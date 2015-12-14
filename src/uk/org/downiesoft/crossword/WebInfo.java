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
import java.io.InputStream;

public class WebInfo implements Comparable
{
	
	private int mCrosswordId;
	private int mSearchId;
	private Date mDate;
	private boolean mIsOnDevice;
	
	public WebInfo(int aCrosswordId, int aSearchId, String aDate)
	{
		mCrosswordId = aCrosswordId;
		mSearchId = aSearchId;
		setDate(aDate);
	}
	
	public WebInfo(DataInputStream aStream) throws IOException {
		mCrosswordId = aStream.readInt();
		mSearchId = aStream.readInt();
		mDate = new Date(aStream.readLong());
	}
	
	public void setCrosswordId(int aId)
	{
		mCrosswordId=aId;
	}

	public void setSearchId(int aId)
	{
		mSearchId=aId;
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
		mDate = cal.getTime();
	}

	public int crosswordId()
	{
		return mCrosswordId;
	}

	public int searchId()
	{
		return mSearchId;
	}

	public void setIsOnDevice(boolean aIsOnDevice) {
		mIsOnDevice = aIsOnDevice;
	}

	public boolean isOnDevice() {
		return mIsOnDevice;
	}

	public Date date()
	{
		return mDate;
	}

	public void merge(WebInfo aInfo) {
		mCrosswordId = aInfo.mCrosswordId;
		mSearchId = Math.max(mSearchId,aInfo.mSearchId);
		if (mDate == null) {
			mDate = aInfo.mDate;
		}
		mDate.setTime(aInfo.date().getTime());
		mIsOnDevice |= aInfo.mIsOnDevice;
	}	

	@SuppressLint("SimpleDateFormat")
	public String dateString()
	{
		if (mDate!=null)
		{
			DateFormat df=new SimpleDateFormat("E dd/MM/yyyy");
			return df.format(mDate);
		}
		else
			return "Unknown";
	}
	
	void externalize(DataOutputStream aStream) throws IOException
	{
		aStream.writeInt(mCrosswordId);
		aStream.writeInt(mSearchId);
		aStream.writeLong(mDate.getTime());
	}
	
	@Override
	public int compareTo(Object p1) {
		try {
			WebInfo info = (WebInfo)p1;
			if (info.mCrosswordId == this.mCrosswordId) {
				return 0;
			} else {
				return -this.mDate.compareTo(info.mDate);
			}
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(String.format("Object %s is not of type %s",p1.getClass().getName(),WebInfo.class.getName()));
		}
	}

	@Override
	public String toString() {
		return String.format("WebInfo{%s,%s,%s,%s}",mCrosswordId, mSearchId, dateString(), mIsOnDevice);
	}
	
}
