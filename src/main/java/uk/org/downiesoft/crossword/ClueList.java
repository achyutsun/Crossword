package uk.org.downiesoft.crossword;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import android.util.SparseIntArray;

public class ClueList {

	private final ArrayList<Clue> iClues;
	private final SparseIntArray iIndex;

	public ClueList() {
		iClues = new ArrayList<Clue>(15);
		iIndex = new SparseIntArray(15);
	}

	public void reset() {
		iClues.clear();
		iIndex.clear();
	}

	public Clue getClueByNumber(int aNum) {
		int index = iIndex.get(aNum,-1);
		if (index != -1) {
			return iClues.get(index);
		}
		return null;
	}

	public void addClue(Clue aClue) {
		iClues.add(aClue);
		iIndex.append(aClue.iNumber, iClues.size()-1);
	}

	public void externalize(DataOutputStream aStream) throws IOException {
		aStream.writeInt(iClues.size());
		for (int j = 0; j < iClues.size(); j++) {
			iClues.get(j).externalize(aStream);
		}
	}

	public void internalize(DataInputStream aStream) throws IOException {
		reset();
		int count = aStream.readInt();
		for (int j = 0; j < count; j++) {
			Clue clue = new Clue();
			clue.internalize(aStream);
			iClues.add(clue);
			iIndex.append(clue.iNumber, iClues.size()-1);
		}
	}

	public int count() {
		return iClues.size();
	}

	public Clue getClueByIndex(int aIndex) {
		return iClues.get(aIndex);
	}

	public int getClueIndex(Clue aClue) {
		return iClues.indexOf(aClue);
	}

	public ArrayList<Clue> getClueListArray() {
		return iClues;
	}

}
