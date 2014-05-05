package uk.org.downiesoft.crossword;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import android.content.Context;
import java.io.File;

public class WebManager
{
	private static WebManager sManagerInstance;
	
	private ArrayList<WebInfo> iWebInfo;

	public static WebManager getInstance() {
		if (sManagerInstance==null) {
			sManagerInstance = new WebManager();
		}
		return sManagerInstance;
	}
	
	private WebManager()
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

	public void externalize(DataOutputStream os) throws IOException	{
		os.writeInt(iWebInfo.size());
		for (int i=0; i < iWebInfo.size(); i++)
			iWebInfo.get(i).externalize(os);
		os.flush();
		os.close();
	}
	
	public void internalize(DataInputStream is) throws IOException
	{
		int size=is.readInt();
		iWebInfo = new ArrayList<WebInfo>(size);
		for (int i=0; i < size; i++) {
			WebInfo info=new WebInfo();
			info.internalize(is);
			iWebInfo.add(info);
		}
	}

	public void store(Context aContext)
	{
		File file = new File(aContext.getFilesDir(),"webinfo");
		try
		{
			DataOutputStream os = new DataOutputStream(aContext.openFileOutput("webinfo", Context.MODE_PRIVATE));
			externalize(os);
			os.flush();
			os.close();
		}
		catch (IOException e)
		{
			file.delete();
		}

	}

	public void restore(Context aContext)
	{
		try
		{
			DataInputStream is = new DataInputStream(aContext.openFileInput("webinfo"));
			internalize(is);
			is.close();
		}
		catch (IOException e)
		{
			iWebInfo.clear();
		}

	}

	
}
