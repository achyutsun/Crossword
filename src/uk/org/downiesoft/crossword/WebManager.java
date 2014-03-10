package uk.org.downiesoft.crossword;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class WebManager
{
	private ArrayList<WebInfo> iWebInfo;

	public WebManager()
	{
		iWebInfo=new ArrayList<WebInfo>();
	}
	
	public int insert(WebInfo aInfo)
	{
		int i=0;
		while (i<iWebInfo.size())
		{
			WebInfo info=iWebInfo.get(i);
			if (info.crosswordId()==aInfo.crosswordId())
			{
				iWebInfo.remove(i);
				iWebInfo.add(i,aInfo);
				return i;
			}
			else if (info.date().after(aInfo.date()))
				i++;
			else
				break;
		}
		iWebInfo.add(i,aInfo);
		return i;
	}
	
	public ArrayList<WebInfo> getWebInfo()
	{
		return iWebInfo;
	}
	
	public WebInfo getCrossword(int aCrosswordId)
	{
		for (int i=0; i<iWebInfo.size(); i++)
			if (iWebInfo.get(i).crosswordId()==aCrosswordId)
				return iWebInfo.get(i);
		return null;
	}
	
	public WebInfo getCrossword(Date aDate)
	{
		for (int i=0; i<iWebInfo.size(); i++)
			if (iWebInfo.get(i).date().compareTo(aDate)==0)
				return iWebInfo.get(i);
		return null;
	}

	public void externalize(DataOutputStream os)
	{
		try
		{
			os.writeInt(iWebInfo.size());
			for (int i=0; i<iWebInfo.size(); i++)
				iWebInfo.get(i).externalize(os);
			os.flush();
			os.close();
		}
		catch (IOException e)
		{			
		}
		
	}

	public void internalize(DataInputStream is)
	{
		try
		{
			int size=is.readInt();
			iWebInfo = new ArrayList<WebInfo>(size);
			for (int i=0; i<size; i++)
			{
				WebInfo info=new WebInfo();
				info.internalize(is);
				iWebInfo.add(info);
			}
		}
		catch (IOException e)
		{
			iWebInfo=new ArrayList<WebInfo>();
		}
	}

}
