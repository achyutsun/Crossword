package uk.org.downiesoft.spell;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import uk.org.downiesoft.crossword.MainActivity;
import uk.org.downiesoft.crossword.R;

public class Lexicon {
    private static final String TAG = "uk.org.downiesoft.spell.Lexicon";

    private static final int INDEX_VERSION = 0x10;

    public interface LexiconObserver {
        boolean reportMatch(String aMatch, int aDistance);

        void reportComplete();
    }

    public static final int MAX_LENGTH = 32;
    public static final int DICT_ALL = 0;
    public static final int DICT_BRITISH = 1;
    public static final int DICT_OSPD = 2;
    public static final int DICT_POCKET = 4;

    public static final int MODE_CHECK_INITIAL = 1;
    public static final int MODE_CHECK_FULL = 2;
    public static final int MODE_XWORD = 3;
    public static final int MODE_ANAGRAM = 4;
    public static final int MODE_SCRABBLE = 5;

    public static final int LEVEL_BEGINNER = 1;
    public static final int LEVEL_INTERMEDIATE = 2;
    public static final int LEVEL_EXPERIENCED = 3;
    public static final int LEVEL_CHAMPIONSHIP = 4;
    public static final int LEVEL_COUNT = 5;

    private class IndexEntry {
        public String iPrefix;
        public int iStart;
        public int iCount;

        public IndexEntry() {
            iPrefix = "";
        }

        @SuppressWarnings("unused")
        public IndexEntry(IndexEntry aEntry) {
            iPrefix = aEntry.iPrefix;
            iStart = aEntry.iStart;
            iCount = aEntry.iCount;
        }

        public IndexEntry(String aPrefix, int aStart, int aCount) {
            iPrefix = aPrefix;
            iStart = aStart;
            iCount = aCount;
        }

        public void externalize(DataOutputStream aStream) throws IOException {
            aStream.writeUTF(iPrefix);
            aStream.writeInt(iStart);
            aStream.writeInt(iCount);
        }

        public IndexEntry internalize(DataInputStream aStream) throws IOException {
            iPrefix = aStream.readUTF();
            iStart = aStream.readInt();
            iCount = aStream.readInt();
            return this;
        }

    }

    private class SearchResult {
        public final String iWord;
        public final int iMetric;

        public SearchResult(String aWord, int aMetric) {
            iWord = aWord;
            iMetric = aMetric;
        }
    }

    private class SearchContext {
        public String iLetterSet;
        public String iFixedSet;
        public boolean iUseAll;
        public int iMode;
        public int iDictMask;
        public int iScrabbleLevel;
        public int iMinLength;
        public int iMaxLength;
    }

    private final Context iContext;
    private byte[] iLexicon;
    private ArrayList<IndexEntry> iIndex;
    private ArrayList<IndexEntry> iIndex2;
    private final StringBuffer iBuffer = new StringBuffer(MAX_LENGTH);
    private int iPos = 0;
    // String iLetterSet;
    // boolean iUseAll;
    // int iMode = 0;
    // int iEndPos = 0;
    private SearchContext iSearchContext;
    private LexiconObserver iObserver;

    public Lexicon(Context aContext, LexiconObserver aObserver) throws IOException {
        iContext = aContext;
        iLexicon = bufferLexicon("lexicon.lst");
        iObserver = aObserver;
        loadIndexes();
        iSearchContext = new SearchContext();
    }

    private byte[] bufferLexicon(String aName) throws IOException {
        InputStream is = iContext.getAssets().open(aName);
        ByteArrayOutputStream os = new ByteArrayOutputStream(2500000);
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int length = -1;
        while ((length = is.read(buffer)) > 0) {
            os.write(buffer, 0, length);
        }
        byte[] bytes = os.toByteArray();
        is.close();
        os.close();
        return bytes;
    }

    private boolean eof() {
        return !(iPos < iLexicon.length && iPos >= 0);
    }

    private String readWord(int aPos, int aDict) {
        iPos = aPos;
        if (eof())
            return null;
        aDict = 0;
        int ch = iLexicon[iPos++];
        if (ch < 0)
            ch += 256;
        int len = ch >> 3;
        aDict = ch & 0x07;
        iBuffer.setLength(0);
        for (int i = 0; i < len; i++)
            if (!eof())
                iBuffer.append((char) iLexicon[iPos++]);
        return iBuffer.toString();
    }

    private String readWord(int aDict) {
        return readWord(iPos, aDict);
    }

    private void loadIndexes() {
        iIndex = loadIndex("index.idx");
        iIndex2 = loadIndex("index2.idx");

        if (iIndex == null || iIndex2 == null) {
            iIndex = new ArrayList<IndexEntry>();
            iIndex2 = new ArrayList<IndexEntry>();
            int start = 0;
            int dict = 0;
            int count = 0;
            iPos = 0;
            String prev = null;
            String prevword = "";
            int prevPos = 0;
            while (!eof()) {
                prevPos = iPos;
                String word = readWord(dict);
                if (word.length() == 2) {
                    iIndex2.add(new IndexEntry(word, prevPos, 1));
                } else {
                    try {
                        String current = word.substring(0, 3);
                        if (prev == null)
                            prev = current;
                        if (!prev.equalsIgnoreCase(current)) {
                            iIndex.add(new IndexEntry(prev, start, count));
                            start = prevPos;
                            count = 0;
                            prev = current;
                        }
                        prevword = word;
                    } catch (StringIndexOutOfBoundsException e) {
                        MainActivity.debug(1, TAG, String.format("substr failed [%s][%s]", word, prevword));
                    }
                }
                count++;
            }
            saveIndex("index.idx", iIndex);
            saveIndex("index2.idx", iIndex2);
        } else {
            iIndex = loadIndex("index.idx");
            iIndex2 = loadIndex("index2.idx");
        }
    }

    private void saveIndex(String aName, ArrayList<IndexEntry> aIndex) {
        try {
            DataOutputStream os = new DataOutputStream(iContext.openFileOutput(aName, Context.MODE_PRIVATE));
            os.write(INDEX_VERSION);
            os.writeInt(aIndex.size());
            for (IndexEntry entry : aIndex)
                entry.externalize(os);
            os.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private ArrayList<IndexEntry> loadIndex(String aName) {
        try {
            DataInputStream is = new DataInputStream(iContext.openFileInput(aName));
            int version = is.read();
            if (version == INDEX_VERSION) {
                int size = is.readInt();
                ArrayList<IndexEntry> index = new ArrayList<IndexEntry>(size);
                for (int i = 0; i < size; i++) {
                    IndexEntry entry = new IndexEntry();
                    index.add(entry.internalize(is));
                }
                is.close();
                return index;
            } else {
                is.close();
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    public IndexEntry searchIndex(String aWord) {
        if (aWord.length() >= 3) {
            int max = iIndex.size();
            int min = 0;
            int test;
            while (max > min) {
                test = ((max + min) / 2);
                IndexEntry entry = iIndex.get(test);
                int cmp = entry.iPrefix.compareToIgnoreCase(aWord.substring(0, 3));
                if (cmp < 0)
                    min = test + 1;
                else if (cmp > 0)
                    max = test;
                else
                    return entry;
            }
        }
        return null;
    }

    private int findIndex(int aStartIndex, String aLetterSet, String aFixed, int aMode) {
        boolean found = false;
        IndexEntry thisIndex = null;
        int i = 0;
        for (i = aStartIndex; !found && i < iIndex.size(); i++) {
            thisIndex = iIndex.get(i);
            switch (aMode) {
                case MODE_ANAGRAM:
                case MODE_SCRABBLE:
                    // found=Contained(aLetterSet, aFixed, iThisIndex.iPrefix);
                    break;
                case MODE_XWORD:
                    found = thisIndex.iPrefix.matches(aLetterSet.substring(0, 3));
                    break;
                case MODE_CHECK_INITIAL:
                    found = thisIndex.iPrefix.substring(0, 1).compareToIgnoreCase(aLetterSet.substring(0, 1)) == 0;
                    break;
                case MODE_CHECK_FULL:
                default:
                    found = (i + 1 > iIndex.size()) ? true : iIndex.get(i + 1).iPrefix.compareToIgnoreCase(aLetterSet) > 0;
                    break;
            }
			if (found) {
				return i;
			}
        }
		return -1;
    }

    int levenshtein(String aWord1, String aWord2) {
        int matrix[][] = new int[aWord1.length() + 1][aWord2.length() + 1];
        final int KCostIns = 1;
        final int KCostDel = 1;
        final int KCostSub = 2;
        final int KCostTra = 0;
        int cost;
        int l1 = aWord1.length();
        int l2 = aWord2.length();
        for (int i = 0; i <= l1; i++)
            matrix[i][0] = i * (1 << KCostDel);
        for (int j = 0; j <= l2; j++)
            matrix[0][j] = j * (1 << KCostIns);
        for (int i = 0; i < l1; i++)
            for (int j = 0; j < l2; j++) {
                cost = aWord1.charAt(i) == aWord2.charAt(j) ? 0 : 1;
                int costIns = matrix[i][j + 1] + (cost << KCostIns);
                int costDel = matrix[i + 1][j] + (cost << KCostDel);
                int costSub = matrix[i][j] + (cost << KCostSub);
                int newCost = Math.min(costSub, Math.min(costDel, costIns));
                if (i > 0 && j > 0 && aWord1.charAt(i) == aWord2.charAt(j - 1) && aWord1.charAt(i - 1) == aWord2.charAt(j))
                    newCost = Math.min(newCost, matrix[i - 1][j - 1] + (cost << KCostTra));
                matrix[i + 1][j + 1] = (short) newCost;
            }
        return matrix[l1][l2];
    }

    public boolean contained(String aLetterSet, String aCandidate) {
        int[] letters = new int[32];
        char[] candidate = aCandidate.toCharArray();
        for (int i = 0; i < 32; i++)
            letters[i] = 0;
        for (int i = 0; i < aLetterSet.length(); i++)
            letters[aLetterSet.charAt(i) & 0x1f]++;

        for (int i = 0; i < aCandidate.length(); i++) {
            if (--letters[candidate[i] & 0x1f] < 0) {
                if (--letters[0x1F] < 0)
                    return false;
                else
                    candidate[i] |= 0x20;
            } else
                candidate[i] &= 0xDF;
        }
        return true;
    }

    public boolean contained(String aLetterSet, String aFixed, String aCandidate, boolean aAllFixed) {
        char[] letters = new char[32];
        char[] candidate = aCandidate.toCharArray();
        char[] fixed = aFixed.toCharArray();
        for (int i = 0; i < 32; i++)
            letters[i] = 0;
        for (int i = 0; i < aLetterSet.length(); i++)
            letters[aLetterSet.charAt(i) & 0x1f]++;

        int i = 0;
        int f = 0;
        int cLen = aCandidate.length();
        int fLen = aFixed.length();
        while (i < cLen) {
            if (f != 0) // see if we can match the fixed part
            {
                while (f < fLen && i + f < cLen && (fixed[f] & 0xDF) == (candidate[i + f] & 0xDF))
                    f++;
                if (f == fLen || (!aAllFixed && (i + f == cLen))) {
                    // yes, so skip ahead and mark tiles as non-blank
                    int skip = i + f;
                    while (i < skip) {
                        candidate[i++] &= 0xDF;
                    }
                } else
                    f = 0; // no, so try again next time
            }
            if (i < cLen) {
                if (--letters[candidate[i] & 0x1F] < 0) {
                    if (--letters[0x1F] < 0) // is there a blank?
                        return false; // no, so match has failed
                    else
                        candidate[i] |= 0x20; // yes, so mark the candidate
                    // letter as blank
                } else
                    candidate[i] &= 0xDF; // not a blank
                i++;
            }
        }
        return !aAllFixed || f == fLen;
    }

    boolean check(String aCandidate, int aDictionary, int aDictMask, boolean aFuzzy) {
        iPos = 0;
        aDictionary = 0;
        if (aCandidate.length() == 2) {
//            String buffer;
//            String word;
//            i2Letters.Seek(ESeekStart, startPos);
//            do {
//                i2Letters.Read(buffer);
//                if (buffer.Length() < 3) return
//                        EFalse;
//                word.Copy(buffer.Left(2));
//            } while
//                    (word.CompareF(aCandidate) < 0);
//            aDictionary = buffer[2];
//            return
//                    (word.CompareF(aCandidate) == 0 && (aDictMask == EDictAll ||
//                            aDictionary & aDictMask));

        } else {
            if (!aFuzzy) {
                IndexEntry entry = null;
                if ((entry = searchIndex(aCandidate)) != null) {
                    String word;
                    int dict = 0;
                    iPos = entry.iStart;
                    int ret = 0;
                    do {
                        word = readWord(dict);
                        if (word == null)
                            return false;
                    } while ((ret = word.compareToIgnoreCase(aCandidate)) < 0);
                    if (ret == 0)
                        aDictionary = dict;
                    return ret == 0 && (aDictMask == DICT_ALL || (dict & aDictMask) != 0);
                }
            } else {
                // fuzzy search
                iSearchContext.iLetterSet = aCandidate.toLowerCase();
                iSearchContext.iUseAll = true;
                iSearchContext.iMode = MODE_CHECK_FULL;
                new SearchTask().execute(aCandidate.toLowerCase());
            }
        }
        return false;
    }

    public void xWordMatch(String aPattern, boolean aUseAll) {
        if (iObserver != null) {
            iSearchContext.iLetterSet = aPattern.toLowerCase();
            iSearchContext.iMode = MODE_XWORD;
            iSearchContext.iUseAll = aUseAll;
            new SearchTask().execute(aPattern.toLowerCase());
        }
    }

    public void anagram(String aLetterSet, boolean aUseAll, int aDictMask, int aLevel) {
        iSearchContext.iDictMask = aDictMask;
        iSearchContext.iScrabbleLevel = aLevel;
        if (iObserver != null) {
            iSearchContext.iLetterSet = aLetterSet;
            iSearchContext.iMode = MODE_ANAGRAM;
            iSearchContext.iUseAll = aUseAll;
            new SearchTask().execute(aLetterSet.toLowerCase());
        }
    }

    public void scrabbleAnagram(String aLetterSet, String aFixedSet, int aMinLength, int aMaxLength, int aDictMask, int aLevel,
                                int aWaitNoteId) {
        iSearchContext.iDictMask = aDictMask;
        iSearchContext.iScrabbleLevel = aLevel;
        if (iObserver != null) {
            iSearchContext.iLetterSet = aLetterSet.toLowerCase();
            iSearchContext.iFixedSet = aFixedSet.toLowerCase();

            iSearchContext.iMode = MODE_SCRABBLE;
            iSearchContext.iUseAll = false;
            iSearchContext.iMinLength = aMinLength;
            iSearchContext.iMaxLength = aMaxLength;
            new SearchTask().execute(iSearchContext.iLetterSet);
        }
    }

    private class SearchTask extends AsyncTask<String, SearchResult, Boolean> {

        private String iWord;
        private boolean iFinished = false;
        ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            mDialog = new ProgressDialog(iContext);
            mDialog.setMessage("Working...");
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDialog.setCancelable(false);
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            iWord = params[0];
            String cand = null;
            int indexPtr = 0;
            int wordCount = 0;
            Random random = new Random();
            switch (iSearchContext.iMode) {
                case MODE_CHECK_INITIAL:
                case MODE_XWORD: {
                    indexPtr = findIndex(indexPtr, iWord.substring(0, 3), "", iSearchContext.iMode);
                    if (indexPtr == -1)
                        return false;
                    break;
                }
                case MODE_SCRABBLE: {
                    String fixLeft3 = iSearchContext.iFixedSet.substring(Math.min(3, iSearchContext.iFixedSet.length()));
                    indexPtr = findIndex(indexPtr, iWord, fixLeft3, iSearchContext.iMode);
                    if (indexPtr >= 0 && random.nextInt(LEVEL_COUNT) > iSearchContext.iScrabbleLevel)
                        return false;
                    break;
                }
                case MODE_ANAGRAM:
                case MODE_CHECK_FULL: {
                    indexPtr = 0;
                    break;
                }
                default:
                    return false;
            }
            IndexEntry entry = iIndex.get(indexPtr);
            iPos = entry.iStart;
            wordCount = entry.iCount;
            while (!iFinished && wordCount > 0) {
                int dict = 0;
                cand = readWord(dict);
                wordCount--;
                if (cand != null) {
                    int wLen = cand.length();
                    int lLen = iSearchContext.iLetterSet.length();
                    switch (iSearchContext.iMode) {
                        case MODE_CHECK_INITIAL:
                        case MODE_CHECK_FULL:
                            if (iSearchContext.iMode == MODE_CHECK_FULL
                                    || cand.substring(0, 1).compareToIgnoreCase(iSearchContext.iLetterSet.substring(0, 1)) == 0) {
                                if (Math.abs(lLen - wLen) <= 3) {
                                    int dist = levenshtein(iWord, cand);
                                    if (dist < 5) {
                                        publishProgress(new SearchResult(cand, dist));
                                    }
                                }
                            }
                            if (wordCount == 0 && ++indexPtr < iIndex.size()) {
                                entry = iIndex.get(indexPtr);
                                if (iSearchContext.iMode == MODE_CHECK_FULL || entry.iPrefix.substring(0, 1).compareToIgnoreCase(iWord.substring(0, 1)) == 0)
                                    wordCount = entry.iCount;
                            }
                            break;
                        case MODE_XWORD: {
                            if (!iSearchContext.iUseAll || wLen == lLen) {
                                if (cand.matches(iSearchContext.iLetterSet)) {
                                    publishProgress(new SearchResult(cand, lLen - wLen));
                                }
                            }
                            if (wordCount == 0 && indexPtr < iIndex.size()) {
                                indexPtr = findIndex(indexPtr + 1, iWord.substring(0, 3), "", iSearchContext.iMode);
                                if (indexPtr != -1) {
                                    entry = iIndex.get(indexPtr);
                                    iPos = entry.iStart;
                                    wordCount = entry.iCount;
                                }
                            }
                            break;
                        }
                        case MODE_ANAGRAM: {
                            if (((!iSearchContext.iUseAll && (lLen - wLen) >= 0) || wLen == lLen)
                                    && (iSearchContext.iDictMask == DICT_ALL || (dict & iSearchContext.iDictMask) != 0)) {
                                if (contained(iSearchContext.iLetterSet, cand)) {
                                    MainActivity.debug(1, TAG, String.format("publish %s %d %d %s %d %d %d", cand, iPos, indexPtr, entry.iPrefix,
                                            entry.iStart, entry.iCount, wordCount));
                                    publishProgress(new SearchResult(cand, lLen - wLen));
                                }
                            }
                            if (wordCount == 0 && ++indexPtr < iIndex.size()) {
                                entry = iIndex.get(indexPtr);
                                iPos = entry.iStart;
                                wordCount = entry.iCount;
                            }
                            break;
                        }
                        case MODE_SCRABBLE: {
                            if (wLen >= iSearchContext.iMinLength && wLen <= iSearchContext.iMaxLength
                                    && (iSearchContext.iDictMask == DICT_ALL || (dict & iSearchContext.iDictMask) != 0)) {
                                int fLen = iSearchContext.iFixedSet.length();
                                if (contained(iSearchContext.iLetterSet, iSearchContext.iFixedSet, cand, true))
                                    publishProgress(new SearchResult(cand, lLen + fLen - wLen));
                            }
                            String fixLeft3 = iSearchContext.iFixedSet.substring(Math.min(3, iSearchContext.iFixedSet.length()));
                            indexPtr = findIndex(++indexPtr, iWord, fixLeft3, iSearchContext.iMode);
                            if (indexPtr >= 0 && random.nextInt(LEVEL_COUNT) > iSearchContext.iScrabbleLevel)
                                return false;
                            wordCount = iIndex.get(indexPtr).iCount;
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
            return true;
        }

        @Override
        public void onProgressUpdate(SearchResult... results) {
            if (iObserver != null) {
                for (SearchResult result : results)
                    iFinished = iFinished || iObserver.reportMatch(result.iWord, result.iMetric);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mDialog.dismiss();
            if (!result) {
                Toast.makeText(iContext, R.string.text_not_found, Toast.LENGTH_SHORT).show();
            }
        }

    }
}
