package uk.org.downiesoft.crossword;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ClueLists
{

	private ArrayList<ArrayList<Clue>> iClues;

	public ClueLists()
	{
		iClues = new ArrayList<ArrayList<Clue>>(2);
		for (int i = 0; i < 2; i++)
		{
			iClues.add(new ArrayList<Clue>(15));
		}
	}

	public void reset()
	{
		for (int i = 0; i < iClues.size(); i++)
		{
			iClues.get(i).clear();
		}
	}

	public Clue getClue(int aDirection, int aNum)
	{
		int i = 0;
		while (i < iClues.get(aDirection).size())
		{
			if (iClues.get(aDirection).get(i).Number() == aNum)
			{
				return (iClues.get(aDirection).get(i));
			}
			i++;
		}
		return null;
	}

	public void addClue(Clue aClue, int aDirection)
	{
		if (aDirection < iClues.size())
			iClues.get(aDirection).add(aClue);
	}

	public void externalize(DataOutputStream aStream) throws IOException
	{
		for (int i = 0; i < 2; i++)
		{
			aStream.writeInt(iClues.get(i).size());
			for (int j = 0; j < iClues.get(i).size(); j++)
			{
				iClues.get(i).get(j).externalize(aStream);
			}
		}
	}

	public void internalize(DataInputStream aStream) throws IOException
	{
		reset();
		for (int i = 0; i < 2; i++)
		{
			int count = aStream.readInt();
			for (int j = 0; j < count; j++)
			{
				Clue clue = new Clue();
				clue.internalize(aStream);
				iClues.get(i).add(clue);
			}
		}
	}

	public int count(int aDirection)
	{
		return iClues.get(aDirection).size();
	}

	public Clue getClueByIndex(int aDirection, int aIndex)
	{
		return iClues.get(aDirection).get(aIndex);
	}
	
	public ArrayList<Clue> getClueList(int aDirection)
	{
		return iClues.get(aDirection%2);
	}

}
