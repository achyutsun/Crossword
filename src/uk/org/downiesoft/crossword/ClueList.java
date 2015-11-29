package uk.org.downiesoft.crossword;

import android.util.SparseArray;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ClueList {

	private ArrayList<Clue> iClues;
	private SparseArray<Integer> iIndex;

	public ClueList() {
		iClues = new ArrayList<Clue>(15);
		iIndex = new SparseArray<Integer>(15);
	}

	public void reset() {
		iClues.clear();
		iIndex.clear();
	}

	public Clue getClueByNumber(int aNum) {
		Integer index = iIndex.get(aNum);
		if (index != null) {
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
			iIndex.put(clue.iNumber, iClues.size()-1);
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
