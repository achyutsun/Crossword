package uk.org.downiesoft.crossword;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;

public class WebInfo
{
	private int iCrosswordId;
	private int iSearchId;
	private Date iDate;

	public WebInfo()
	{	
	}
	
	public WebInfo(int aCrosswordId, int aSearchId, String aDate)
	{
		iCrosswordId = aCrosswordId;
		iSearchId = aSearchId;
		setDate(aDate);
	}

	public void setCrosswordId(int aId)
	{
		iCrosswordId=aId;
	}

	public void setSearchId(int aId)
	{
		iSearchId=aId;
	}
	
	@SuppressLint("SimpleDateFormat")
	public void setDate(String aDateStr)
	{
		final String months="JanFebMarAprMayJunJulAugSepOctNovDec";
		String[] elem=aDateStr.split(" ");
		int d=Integer.parseInt(elem[1]);
		int m=months.indexOf(elem[2])/3;
		int y=Integer.parseInt(elem[3])+2000;
		Calendar cal=Calendar.getInstance(Locale.UK);
		cal.set(y, m, d);
		iDate = cal.getTime();
	}

	public int crosswordId()
	{
		return iCrosswordId;
	}

	public int searchId()
	{
		return iSearchId;
	}

	public Date date()
	{
		return iDate;
	}
	
	@SuppressLint("SimpleDateFormat")
	public String dateString()
	{
		if (iDate!=null)
		{
			DateFormat df=new SimpleDateFormat("E dd/MM/yyyy");
			return df.format(iDate);
		}
		else
			return "Unknown";
	}
	
	void externalize(DataOutputStream aStream) throws IOException
	{
		aStream.writeInt(iCrosswordId);
		aStream.writeInt(iSearchId);
		aStream.writeLong(iDate.getTime());
	}
	
	void internalize(DataInputStream aStream) throws IOException
	{
		iCrosswordId=aStream.readInt();
		iSearchId=aStream.readInt();
		iDate=new Date(aStream.readLong());
	}
	
}
