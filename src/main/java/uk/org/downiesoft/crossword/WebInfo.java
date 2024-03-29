package uk.org.downiesoft.crossword;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.io.BufferedReader;

public class WebInfo implements Comparable<WebInfo>
{
	
	private int mCrosswordId;
	private int mSearchId;
	private Date mDate;
	private boolean mIsOnDevice;
	
	public WebInfo() {
	}
	
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
	
	public WebInfo(String aHeaderLine) {
		String[] elements = aHeaderLine.split("\t");
		int id = Integer.parseInt(elements[1]);
		mCrosswordId = id;
		if (elements.length >= 3) {
			mSearchId = Integer.parseInt(elements[2]);
		}
		if (elements.length >= 4) {
			setDate(elements[3]);
		} else {
			mDate = estimatedDate(mCrosswordId);
		}
	}
	
	public void setCrosswordId(int aId)
	{
		mCrosswordId=aId;
	}

	public void setSearchId(int aId)
	{
		mSearchId=aId;
	}
	
	public void setDate(String aDateStr)
	{
		DateFormat df;
		try {
			if (aDateStr != null) {
				if (aDateStr.contains("/")) {
					df = new SimpleDateFormat("E dd/MM/yyyy");
					mDate = df.parse(aDateStr);
				} else {
					df = new SimpleDateFormat("E dd MMM yy");
					mDate = df.parse(aDateStr);
				}
			} else {
				mDate = WebInfo.estimatedDate(mCrosswordId);
			}
		} catch (ParseException e) {
			mDate = WebInfo.estimatedDate(mCrosswordId);
		}
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
		if (mDate != null) {
			return mDate;
		} else {
			return WebInfo.estimatedDate(mCrosswordId);
		}
	}

	public static Date estimatedDate(int aId) {
		Calendar cal = Calendar.getInstance(Locale.UK);
		final long oneDayMillis = 24 * 3600 * 1000;
		if (aId>=10000) {
			cal.set(2015,Calendar.DECEMBER,14,0,0,0);
			long time=cal.getTimeInMillis();
			long weeks = (aId - 27984) / 6;
			long days = (aId - 27984) % 6;
			time = time + (7 * weeks + days) * oneDayMillis;
			return new Date(time);
		} else {
			cal.set(2015,Calendar.DECEMBER,13,0,0,0);
			long time=cal.getTimeInMillis();
			long weeks = (aId - 2826);
			time = time + (7 * weeks) * oneDayMillis;
			return new Date(time);
		}
	}
	
	public void merge(WebInfo aInfo) {
		mCrosswordId = aInfo.mCrosswordId;
		mSearchId = Math.max(mSearchId,aInfo.mSearchId);
		if (mDate == null) {
			mDate = aInfo.mDate;
		}
		if (aInfo.mDate != null) {
			mDate.setTime(aInfo.date().getTime());
		} else {
			mDate = WebInfo.estimatedDate(mCrosswordId);
		}
		mIsOnDevice |= aInfo.mIsOnDevice;
	}	

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
	
	public void externalize(DataOutputStream aStream) throws IOException
	{
		aStream.writeInt(mCrosswordId);
		aStream.writeInt(mSearchId);
		aStream.writeLong(mDate.getTime());
	}
	
	@Override
	public int compareTo(WebInfo info) {
		if (info.mCrosswordId == this.mCrosswordId) {
			return 0;
		} else if (mDate != null && info.mDate != null) {
			return -this.mDate.compareTo(info.mDate);
		} else if (mDate == null && info.mDate != null) {
			return -WebInfo.estimatedDate(this.mCrosswordId).compareTo(info.mDate);
		} else if (mDate != null) {
			return -this.mDate.compareTo(estimatedDate(info.mCrosswordId));
		} else {
			return -WebInfo.estimatedDate(this.mCrosswordId).compareTo(estimatedDate(info.mCrosswordId));
		}
	}
	
	@Override
	public String toString() {
		return String.format("WebInfo{%s,%s,%s,%s}",mCrosswordId, mSearchId, dateString(), mIsOnDevice);
	}
	
}
