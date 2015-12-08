package uk.org.downiesoft.crossword;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Environment;
import android.widget.Toast;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

class CrosswordModel
{
	public static final String TAG="uk.org.downiesoft.crossword.CrosswordModel";
	
	public static final int GRID_SIZE = 15;
	public static final int VERSION_ANDROID = 0x00020000;
	public static final int VERSION_CURRENT = 0x00020000;

	public static final int SQUARE_BLANK = 0;
	public static final int SQUARE_NONBLANK = 1;
	public static final int CLUE_ACROSS = 0;
	public static final int CLUE_DOWN = 1;

	public static final String CROSSWORD_HEADER = "#!Crossword";

	private static CrosswordModel sCrossword;
	
	int iCrosswordId;
	ClueLists iClues;
	boolean iCrosswordValid;
	int iGrid[][] = new int[GRID_SIZE][GRID_SIZE];
	int iSolution[][] = new int[GRID_SIZE][GRID_SIZE];

	public static CrosswordModel getInstance()
	{
		if (sCrossword==null) {
			sCrossword = new CrosswordModel();
		}
		return sCrossword;
	}
	
	public static final void setInstance(CrosswordModel aCrossword) {
		sCrossword = aCrossword;
	}
	
	public CrosswordModel()
	{
		iClues = new ClueLists();
		reset();
	}

	public int value(Point aPos)
	{
		return iGrid[aPos.x][aPos.y];
	}

	public int value(int aX, int aY)
	{
		return iGrid[aX][aY];
	}

	public int solution(Point aPos)
	{
		return iSolution[aPos.x][aPos.y];
	}

	public int solution(int aX, int aY)
	{
		return iSolution[aX][aY];
	}

	public void setValue(Point aPos, int aValue)
	{
		iGrid[aPos.x][aPos.y] = aValue;
	}

	public void setValue(int aX, int aY, int aValue)
	{
		iGrid[aX][aY] = aValue;
	}

	public boolean crosswordStatus()
	{
		return iCrosswordValid;
	}

	public boolean isBlank(Point aPos)
	{
		return iGrid[aPos.x][aPos.y] == SQUARE_BLANK;
	}

	public boolean isBlank(int aX, int aY)
	{
		return iGrid[aX][aY] == SQUARE_BLANK;
	}

	public int crosswordId()
	{
		return iCrosswordId;
	}

	public ClueLists getClueLists()
	{
		return iClues;
	};

	public boolean isValid()
	{
		return iCrosswordValid;
	}

	void clearAnswers()
	{
		for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++)
		{
			int square = iGrid[i % GRID_SIZE][i / GRID_SIZE];
			if (square != SQUARE_BLANK)
				iGrid[i % GRID_SIZE][i / GRID_SIZE] = SQUARE_NONBLANK;
		}
	}

	void reset()
	{
		iClues.reset();
		for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++)
		{
			iSolution[i % GRID_SIZE][i / GRID_SIZE] = SQUARE_BLANK;
			iGrid[i % GRID_SIZE][i / GRID_SIZE] = SQUARE_BLANK;
		}
		iCrosswordId = 0;
		iCrosswordValid = false;
	}

	public void saveCrosswordId(BufferedWriter aWriter) throws IOException
	{
		aWriter.write(CROSSWORD_HEADER + "\t" + Integer.toString(iCrosswordId));
		aWriter.newLine();
	}

	public void saveGrid(BufferedWriter aWriter) throws IOException
	{
		for (int row = 0; row < GRID_SIZE; row++)
		{
			for (int col = 0; col < GRID_SIZE; col++)
			{
				int val = value(col, row);
				aWriter.write(val == SQUARE_BLANK ? '*' : (val == SQUARE_NONBLANK ? ' ' : val));
			}
			aWriter.newLine();
		}		
	}
	
	public void saveClues(BufferedWriter aWriter) throws IOException
	{
		for (int i = 0; i < 2; i++)
		{
			if (i == 0) {
				aWriter.write("Across");
				aWriter.newLine();
			}
			if (i == 1) {
				aWriter.write("Down");
				aWriter.newLine();
			}
			for (int j = 0; j < iClues.count(i); j++)
			{
				aWriter.write(iClues.getClueByIndex(i, j).toString());
				aWriter.newLine();
			}
		}
		
	}
	
	public void saveCrossword(OutputStream aStream) throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(aStream));
		saveCrosswordId(writer);
		saveGrid(writer);
		saveClues(writer);
		writer.newLine();
		writer.close();
	}
	
	public boolean saveCrossword(Context aContext)
	{
		String path = Environment.getExternalStorageDirectory().toString() + File.separatorChar + "Crossword";
		File dir = new File(path);
		dir.mkdir();
		path+=File.separatorChar+Integer.toString(iCrosswordId)+".xwd";
		try
		{
			DataOutputStream is = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(path))));
			saveCrossword(is);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(aContext, R.string.save_failed, Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	public void readGrid(BufferedReader aReader, CrosswordModel aCrossword) throws IOException
	{
		String line;
		for (int row = 0; row < GRID_SIZE; row++)
		{
			line = aReader.readLine();
			MainActivity.debug(2, TAG, String.format("OpenCrossword: row=[%s]", line));
			for (int col = 0; col < GRID_SIZE && col < line.length(); col++)
			{
				char ch = line.charAt(col);
				if (ch == '*')
					aCrossword.iGrid[col][row] = SQUARE_BLANK;
				else
					aCrossword.iGrid[col][row] = (ch == ' ' ? SQUARE_NONBLANK : (int) ch);
			}
		}		
	}
	
	public void readCrosswordId(BufferedReader aReader) throws IOException
	{
		String line = aReader.readLine();
		MainActivity.debug(1, TAG, String.format("OpenCrossword: id=[%s]", line));
		if (line == null || !line.regionMatches(0, CROSSWORD_HEADER, 0, CROSSWORD_HEADER.length()))
			iCrosswordId=-1;
		String[] id = line.split("\t");
		iCrosswordId=Integer.parseInt(id[1]);
	}
	
	public static CrosswordModel openCrossword(InputStream is) throws IOException
	{
		CrosswordModel crossword=new CrosswordModel();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		crossword.readCrosswordId(reader);
		crossword.readGrid(reader,crossword);
		String line = reader.readLine();
		if (!line.equalsIgnoreCase("Across"))
			return null;
		int direction = CLUE_ACROSS;
		line = reader.readLine();
		try {
			while (line != null && line.length() > 0) {
				MainActivity.debug(2, TAG, String.format("OpenCrossword: clue=[%s]", line));
				if (line.equalsIgnoreCase("Down")) {
					direction = CLUE_DOWN;
					line = reader.readLine();
					if (line == null) {
						return null;
					}
				}
				Clue clue = new Clue();
				clue.setType(direction);
				String[] words = line.split("\t");
				words[0] = words[0].replaceFirst("[AD]:", "");
				clue.setNumber(Integer.parseInt(words[0]));
				clue.setText(words[1]);
				crossword.iClues.addClue(clue, direction);
				line = reader.readLine();
			}
		} catch (NumberFormatException e) {
			// end of input
		}
		reader.close();
		return crossword;
	}
	
	public static CrosswordModel openCrossword(File aFile)
	{
		CrosswordModel crossword=null;
		try
		{
			InputStream is = new FileInputStream(aFile);
			crossword=CrosswordModel.openCrossword(is);
			is.close();
			crossword.iCrosswordValid = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return crossword;
	}

	@SuppressWarnings("deprecation")
	public boolean importCrossword(DataInputStream aStream)
	{
		final String KXwordIdHdr1 = "<td width=\"450\" class=\"telegraph\" bgcolor=\"#ffffff\">&nbsp;puzzles.<b>telegraph</b>.co.uk&nbsp;&nbsp;&nbsp;CRYPTIC CROSSWORD NO: ";
		final String KAcrossHdr1 = "<td width=\"290\"><font face=\"arial,helvetica\" size=\"2\"><b>Across</b></font><br>";
		final String KDownHdr1 =  "<td width=\"290\"><font face=\"arial,helvetica\" size=\"2\"><b>Down</b></font><br>";
		final String KClueNumHdr1 = "<td valign=\"top\"><b><font face=\"arial,helvetica\" size=\"2\">";
		final String KClueTxtHdr1 = "<td width=\"85%\"><font face=\"arial,helvetica\" size=\"2\"><clue>";
		final String KSolutionHdr1 = "<td width=\"85%\"><font face=\"arial,helvetica\" size=\"2\"><solution>";
		final String KCellBlack = "black_cell";
		final String KCellWhite = "white_cell";
		final String KCellNumber = "_number";
		final String KQuoteToken = "&quot;";
		final String KAmpersandToken = "&amp;";
		
		int id=0;
		int grid[][] = new int[GRID_SIZE][GRID_SIZE];
		int col,row;
		col = row = 0;
		ClueLists clues = new ClueLists();
		Clue clue=new Clue();
		try
		{
			String line=aStream.readLine();
			while (line!=null)
			{
				if (line.length()==0)
					line=aStream.readLine();
				if (line==null)
					break;
//				line=line.replace("\"", "");
//				line=line.replace(" ","");
				if (line.contains(KXwordIdHdr1))
				{
					line=line.substring(line.indexOf(KXwordIdHdr1)+KXwordIdHdr1.length());
					String idStr=line.substring(0,line.indexOf('<'));
					line=line.substring(idStr.length());
					idStr=idStr.replace(",", "");
					id=Integer.parseInt(idStr);
				}
				else if (line.contains(KAcrossHdr1))
				{
					clue.iType=CLUE_ACROSS;
					line="";
				}
				else if (line.contains(KDownHdr1))
				{
					clue.iType=CLUE_DOWN;
					line="";
				}
				else if (line.contains(KClueNumHdr1))
				{
					line=line.substring(line.indexOf(KClueNumHdr1)+KClueNumHdr1.length());
					String numStr=line.substring(0,line.indexOf('<'));
					line=line.substring(numStr.length());
					clue.iNumber=Integer.parseInt(numStr);
				}
				else if (line.contains(KClueTxtHdr1))
				{
					line=line.substring(line.indexOf(KClueTxtHdr1)+KClueTxtHdr1.length());
					clue.iText=line.substring(0,line.indexOf('<'));
					line=line.substring(clue.iText.length());
					clue.iText=clue.iText.replace(KAmpersandToken, "&");
					clue.iText=clue.iText.replace(KQuoteToken, "\"");
					clues.addClue(new Clue(clue), clue.iType);
				}
				else if (line.contains(KSolutionHdr1))
				{
					line=line.substring(line.indexOf(KSolutionHdr1)+KSolutionHdr1.length());
					clue.iText=line.substring(0,line.indexOf('<'));
					line=line.substring(clue.iText.length());
					clue.iText=clue.iText.replace(KAmpersandToken, "&");
					clue.iText=clue.iText.replace(KQuoteToken, "\"");
					clues.addClue(new Clue(clue), clue.iType);
				}
				else if (line.contains(".gif"))
				{
					int black=line.indexOf(KCellBlack);
					int min=black;
					int white=line.indexOf(KCellWhite);
					if (white>=0 && (black<0 || white<black))
						min = white;
					int number=line.indexOf(KCellNumber);
					if (number>=0 && (black<0 || number<black) && (white<0 || number<white))
						min = number;
					if (min>=0 && min==black)
					{
						grid[col][row]=SQUARE_BLANK;
						col=(col+1)%GRID_SIZE;
						if (col==0) row++;
						line=line.substring(black+KCellBlack.length());
					}
					else if (min>=0 && min==white)
					{
						grid[col][row]=SQUARE_NONBLANK;
						col=(col+1)%GRID_SIZE;
						if (col==0) row++;
						line=line.substring(white+KCellWhite.length());
					}
					else if (min>=0 && min==number)
					{
						grid[col][row]=SQUARE_NONBLANK;
						col=(col+1)%GRID_SIZE;
						if (col==0) row++;
						line=line.substring(number+KCellNumber.length());
					}
					else
						line="";
				}
				else
					line="";
			}
			aStream.close();
			boolean crosswordValid = col == 0 && row == 15 && clues.count(CLUE_ACROSS) > 0 && clues.count(CLUE_DOWN) > 0 && id > 0;
			if (!crosswordValid)
				return false;
			iClues = clues;
			iCrosswordId = id;
			iCrosswordValid = true;
			for (int i = 0; i < 15 * 15; i++) {
				iGrid[i % 15][i / 15] = grid[i % 15][i / 15];
				iSolution[i % 15][i / 15] = 0;
			}
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	
	public boolean mapSolution(CrosswordModel aSolution)
	{
		int clueNum = 1;
		if (iCrosswordId!=aSolution.iCrosswordId)
			return false;
		for (int col=0; col<GRID_SIZE; col++)
			for (int row=0; row<GRID_SIZE; row++)
			{
				if (isBlank(col,row) && !aSolution.isBlank(col, row))
					return false;
				if (!isBlank(col,row) && aSolution.isBlank(col,row))
					return false;
				iSolution[col][row]=aSolution.iGrid[col][row];
			}
		boolean valid=true;
		for (int i = 0; i < GRID_SIZE*GRID_SIZE; i++)
		{
			int row=i/GRID_SIZE;
			int col=i%GRID_SIZE;
				if (!isBlank(col, row))
				{
					Point pos = new Point(col, row);
					Rect extA = getClueExtent(pos, CLUE_ACROSS);
					Rect extD = getClueExtent(pos, CLUE_DOWN);
					boolean incClue=false;
					if (extA.width() > 0 && (col == 0 || isBlank(col - 1, row)))
					{
						incClue=true;
						Clue solution = aSolution.getClue(CLUE_ACROSS, clueNum);
						if (solution==null || !enterSolution(pos, CLUE_ACROSS, solution.text()))
							valid=false;
					}
					if (extD.height() > 0 && (row == 0 || isBlank(col, row - 1)))
					{
						incClue=true;
						Clue solution = aSolution.getClue(CLUE_DOWN, clueNum);
						if (solution==null || !enterSolution(pos, CLUE_DOWN, solution.text()))
							valid=false;
					}
					if (incClue)
						clueNum++;
					if (!valid)
						break;
				}
		}
		if (!valid)
			iSolution=new int[GRID_SIZE][GRID_SIZE];
		return valid;
	}
	
	public Clue getClue(int aDirection, int aNum)
	{
		return iClues.getClueByNumber(aDirection, aNum);
	}

	public Rect getClueExtent(Point aPos, int aDirection)
	{
		Point delta = new Point(1 - aDirection, aDirection);
		Point start = new Point(aPos);
		do
		{
			start.x -= delta.x;
			start.y -= delta.y;
		} while (withinGrid(start) && !isBlank(start));
		start.x += delta.x;
		start.y += delta.y;
		Point end = new Point(start);
		while (withinGrid(end) && !isBlank(end))
		{
			end.x += delta.x;
			end.y += delta.y;
		}
		end.x -= delta.x;
		end.y -= delta.y;
		return new Rect(start.x, start.y, end.x, end.y);
	}

	public int clueLength(Point aPos, int aDirection)
	{
		Rect extent = getClueExtent(aPos, aDirection);
		return Math.max(extent.width(), extent.height()) + 1;
	}

	
	public boolean clueCompleted(Point aPos, int aDirection)
	{
		Point delta = new Point(1 - aDirection, aDirection);
		Point newpos = new Point(aPos);
		int len = 0;
		do
		{
			newpos.x -= delta.x;
			newpos.y -= delta.y;
		} while (newpos.x >= 0 && newpos.y >= 0 && !isBlank(newpos));
		boolean complete = true;
		newpos.x += delta.x;
		newpos.y += delta.y;
		while (complete && withinGrid(newpos) && !isBlank(newpos))
		{
			complete = value(newpos) != SQUARE_NONBLANK;
			newpos.x += delta.x;
			newpos.y += delta.y;
			len++;
		}
		return complete && len > 1;
	}

	public Point locateClue(int aDirection, int aNum)
	{
		int clueNum = 0;
		for (int y = 0; y < GRID_SIZE; y++)
		{
			for (int x = 0; x < GRID_SIZE; x++)
			{
				if (iGrid[x][y] != SQUARE_BLANK)
				{
					boolean down = (y == 0 || isBlank(x, y - 1));
					boolean across = (x == 0 || isBlank(x - 1, y));
					if (down && (y == GRID_SIZE - 1 || isBlank(x, y + 1)))
						down = false;
					if (across && (x == GRID_SIZE - 1 || isBlank(x + 1, y)))
						across = false;
					if (down || across)
					{
						clueNum++;
						if (clueNum == aNum && ((down && aDirection == CLUE_DOWN) || (across && aDirection == CLUE_ACROSS)))
						{
							return new Point(x, y);
						}
					}
				}
			}
		}
		return null;
	}

	public boolean clueCompleted(int aDirection, int aNum)
	{
		Point startPos = locateClue(aDirection, aNum); 
		if ( startPos==null)
			return false;
		Point delta = new Point(1 - aDirection, aDirection);
		Point newpos = new Point(startPos);
		boolean completed = true;
		;
		while (completed && withinGrid(newpos) && !isBlank(newpos))
		{
			completed = value(newpos) != SQUARE_NONBLANK;
			newpos.x += delta.x;
			newpos.y += delta.y;
		}
		return completed;
	}

	public boolean withinGrid(Point aPos)
	{
		return withinGrid(aPos.x,aPos.y);
	}

	public boolean withinGrid(int aCol, int aRow)
	{
		return aCol >= 0 && aRow >= 0 && aCol < GRID_SIZE && aRow < GRID_SIZE;
	}


	public Clue clueAt(Point aPos, int aDirection)
	{
		if (isBlank(aPos))
			return null;
		Rect extent=getClueExtent(aPos,aDirection);
		Point cluestart = new Point(extent.left,extent.top);
		int clueNum = 0;
		for (int y = 0; y < GRID_SIZE; y++)
		{
			for (int x = 0; x < GRID_SIZE; x++)
			{
				if (iGrid[x][y] != SQUARE_BLANK)
				{
					boolean down = (y == 0 || isBlank(x, y - 1));
					boolean across = (x == 0 || isBlank(x - 1, y));
					if (down && (y == GRID_SIZE - 1 || isBlank(x, y + 1)))
						down = false;
					if (across && (x == GRID_SIZE - 1 || isBlank(x + 1, y)))
						across = false;
					if (down || across)
					{
						clueNum++;
						if (cluestart.equals(x, y))
						{
							int direction = down ? (across ? aDirection : CLUE_DOWN) : CLUE_ACROSS;
							return getClue(direction, clueNum);
						}
					}
				}
			}
		}
		return null;
	}

	
	public boolean crosswordCompleted()
	{
		boolean xwordComplete = true;
		for (int y = 0; y < GRID_SIZE; y++)
		{
			for (int x = 0; x < GRID_SIZE; x++)
			{
				xwordComplete = xwordComplete && (value(x, y) != SQUARE_NONBLANK);
			}
		}
		return xwordComplete;
	}	 

	public boolean enterSolution(Point aPos, int aDirection, String aWord)
	{
		return enterWord(aPos, aDirection, aWord, iSolution);
	}

	public boolean enterWord(Point aPos, int aDirection, String aWord)
	{
		return enterWord(aPos, aDirection, aWord, iGrid);
	}

	private boolean enterWord(Point aPos, int aDirection, String aWord, int[][] aGrid)
	{
		Rect extent=getClueExtent(aPos,aDirection);
		if (aGrid[aPos.x][aPos.y]==SQUARE_BLANK) return false;
		Point delta=new Point(1-aDirection,aDirection);
		int len=extent.width()+extent.height()+1;
		if (len!=aWord.length()) return false;
		Point pos=new Point(extent.left,extent.top);
		for (int i=0; i<len; i++)
		{
			int val=aGrid[pos.x][pos.y];
			if (val!=SQUARE_NONBLANK && val!=(aWord.charAt(i)&0xDF))
				return false;
			pos.x+=delta.x;
			pos.y+=delta.y;
		}
		pos.set(extent.left,extent.top);
		for (int i=0; i<len; i++)
		{
			aGrid[pos.x][pos.y]=aWord.charAt(i)&0xDF;
			pos.x+=delta.x;
			pos.y+=delta.y;
		}		
		return true;
	}
	
	public String getCluePattern(Point aPos, int aDirection)
	{
		Point delta = new Point(1 - aDirection, aDirection);
		Rect extent=getClueExtent(aPos,aDirection);
		Point pos=new Point(extent.left,extent.top);
		int len=extent.width()+extent.height()+1;
		StringBuffer buffer=new StringBuffer(len);
		for (int i=0; i<len; i++)
		{
			int val=value(pos);
			if (val==SQUARE_NONBLANK)
				buffer.append('*');
			else
				buffer.append((char)val);
			pos.x+=delta.x;
			pos.y+=delta.y;
		}
		return buffer.toString();
	}

	public void eraseWord(Point aPos, int aDirection)
	{
		if (value(aPos)==SQUARE_BLANK) return;
		Rect extent=this.getClueExtent(aPos, aDirection);
		if (extent==null || extent.width()+extent.height()==0) return;
		Point delta=new Point(1-aDirection,aDirection);
		Point pos=new Point(extent.left,extent.top);
		extent.set(extent.left,extent.top,extent.right+1,extent.bottom+1);
		while (extent.contains(pos.x,pos.y))
		{
			Rect extentX=getClueExtent(pos,1-aDirection);
			if (extentX.width()+extentX.height()==0 || !clueCompleted(pos,1-aDirection))
			{
				setValue(pos,SQUARE_NONBLANK);
			}
			pos.x+=delta.x;
			pos.y+=delta.y;
		}
	}

	public void externalize(DataOutputStream aStream) throws IOException
	{
		aStream.writeInt(VERSION_CURRENT);
		aStream.writeInt(iCrosswordId);
		for (int x = 0; x < GRID_SIZE; x++)
		{
			for (int y = 0; y < GRID_SIZE; y++)
			{
				aStream.writeByte(iGrid[x][y]);
			}
		}
		iClues.externalize(aStream);
	}

	public void internalize(DataInputStream aStream) throws IOException
	{
		int version;
		iCrosswordValid = false;
		version = aStream.readInt();
		if (version < VERSION_ANDROID)
		{
			iCrosswordId = version;
			version = -1;
		}
		else
			iCrosswordId = aStream.readInt();
		for (int x = 0; x < GRID_SIZE; x++)
		{
			for (int y = 0; y < GRID_SIZE; y++)
			{
				iGrid[x][y] = aStream.readByte();
			}
		}
		iClues.internalize(aStream);
		iCrosswordValid = true;
	}

	public void dumpGrid(String aTag)
	{
		for (int row=0; row<GRID_SIZE; row++)
		{
			StringBuffer rowBuff=new StringBuffer(GRID_SIZE);
			for (int col=0; col<GRID_SIZE; col++)
			{
				int val=value(col,row);
				rowBuff.append(isBlank(col,row)?'*':(val==CrosswordModel.SQUARE_NONBLANK?' ':(char)val));
			}
			MainActivity.debug(1, aTag,String.format("Row %2d %s", row, rowBuff.toString()));
		}
	}

	@Override
	public String toString() {
		return String.format("CrosswordModel{%s}",this.iCrosswordId);
	}

}
