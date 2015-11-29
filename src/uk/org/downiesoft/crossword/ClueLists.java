package uk.org.downiesoft.crossword;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import android.util.SparseArray;

public class ClueLists
{

	private ClueList[] iClues;
	private int mSelectedDirection;
	private int mSelectedIndex;
	
	public ClueLists()
	{
		iClues = new ClueList[2];
		for (int i = 0; i < 2; i++)
		{
			iClues[i] = new ClueList();
		}
	}

	public void reset()
	{
		for (ClueList l: iClues) {
			l.reset();
		}
	}

	public Clue getClueByNumber(int aDirection, int aNum)
	{
		return iClues[aDirection].getClueByNumber(aNum);
	}

	public void addClue(Clue aClue, int aDirection)
	{
		iClues[aDirection].addClue(aClue);
	}

	public void externalize(DataOutputStream aStream) throws IOException
	{
		for (ClueList l: iClues) {
			l.externalize(aStream);
		}
	}

	public void internalize(DataInputStream aStream) throws IOException
	{
		for (ClueList l: iClues) {
			l.internalize(aStream);
		}
	}

	public int count(int aDirection)
	{
		return iClues[aDirection].count();
	}

	public Clue getClueByIndex(int aDirection, int aIndex)
	{
		return iClues[aDirection].getClueByIndex(aIndex);
	}
	
	public int getClueIndex(int aDirection, Clue aClue) {
		return iClues[aDirection].getClueIndex(aClue);
	}
	
	public ArrayList<Clue> getClueListArray(int aDirection)
	{
		return iClues[aDirection].getClueListArray();
	}
	
	public void setSelectedClue(int aDirection, int aIndex) {
		mSelectedDirection = aDirection;
		mSelectedIndex = aIndex;
	}

	public int getSelectedClueIndex(int aDirection) {
		if (aDirection == mSelectedDirection) {
			return mSelectedIndex;
		}
		return -1;
	}
}
