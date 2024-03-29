package uk.org.downiesoft.crossword;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
import android.widget.Toast;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;

public class WebManager extends Fragment {
	
	public static final String TAG = WebManager.class.getName();

	public interface WebManagerListener {
		void onWebManagerReady(WebManager aWebManager);
	}
	
	private static WebManager sManagerInstance;

	private ArrayList<WebInfo> mWebInfoList;
	private WebManagerListener mListener;
	private boolean mModified = false;

	public static WebManager getInstance() {
		if (sManagerInstance == null) {
			sManagerInstance = new WebManager();
		}
		return sManagerInstance;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (WebManagerListener)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(String.format("Activity %s must implement %s",activity.getClass().getName(),WebManagerListener.class.getName()));
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		MainActivity.debug(1, TAG, "onCreate");
		setRetainInstance(true);
		new WebInfoLoaderTask(getActivity(), "webinfo").execute();	
		super.onCreate(savedInstanceState);
	}

	public int insert(WebInfo aInfo) {
		return insertInSeq(mWebInfoList, aInfo);
	}

	public ArrayList<WebInfo> getWebInfoList() {
		return mWebInfoList;
	}
	
	public WebInfo getCrossword(int aCrosswordId) {
		if (mWebInfoList != null) {
			for (int i=0; i < mWebInfoList.size(); i++) {
				if (mWebInfoList.get(i).crosswordId() == aCrosswordId) {
					return mWebInfoList.get(i);
				}
			}
		}
		return null;
	}

	public WebInfo getCrossword(Date aDate) {
		if (mWebInfoList != null) {
			for (int i=0; i < mWebInfoList.size(); i++) {
				if (mWebInfoList.get(i).date().compareTo(aDate) == 0) {
					return mWebInfoList.get(i);
				}
			}
		}
		return null;
	}

	public void store(Context aContext) {
		MainActivity.debug(2, TAG, String.format("store %s", mModified));
		if (mModified) {
			try {
				DataOutputStream os = new DataOutputStream(aContext.openFileOutput("webinfo", Context.MODE_PRIVATE));
				os.writeInt(mWebInfoList.size());
				for (WebInfo info: mWebInfoList) {
					info.externalize(os);
				}
				os.flush();
				os.close();
			} catch (IOException e) {
				Toast.makeText(aContext,R.string.save_failed,Toast.LENGTH_SHORT).show();
			}
		}
		mModified = false;
	}

	
	public int insertInSeq(ArrayList<WebInfo> aWebInfoList, WebInfo aInfo) {
		int index = Collections.binarySearch(aWebInfoList, aInfo);
		if (index >= 0) {
			return index;
		} else {
			index = -1 - index;
			aWebInfoList.add(index, aInfo);
			mModified = true;
			return index;			
		}
	}

	private class WebInfoLoaderTask extends AsyncTask<Void, Void, ArrayList<WebInfo>> {

		private final Context mContext;
		private final String mFileName;

		WebInfoLoaderTask(Context aContext, String aFileName) {
			mContext = aContext;
			mFileName = aFileName;
		}

		@Override
		protected ArrayList<WebInfo> doInBackground(Void... voids) {
			MainActivity.debug(1, TAG, ">doInBackground()");
			ArrayList<WebInfo> webInfoList = null;
			File[] xwdFiles=MainActivity.getCrosswordDirectory().listFiles(new FileFilter(){
					@Override
					public boolean accept(File file) {
						return (file.isDirectory() || file.getName().toLowerCase(Locale.ENGLISH).endsWith(".xwd"));
					}});
			SparseBooleanArray deviceFiles = new SparseBooleanArray();
			if (xwdFiles != null) {
				for (File f: xwdFiles) {
					try {
						String name = f.getName();
						int id = Integer.parseInt(name.substring(0, name.lastIndexOf('.')));
						deviceFiles.put(id, true);
					} catch (NumberFormatException e) {
						// ignore non-numeric file names
					}
				}
			}
			DataInputStream is = null;
			try {
				is = new DataInputStream(mContext.openFileInput(mFileName));
				int size=is.readInt();
				webInfoList = new ArrayList<WebInfo>(size);
				for (int i=0; i < size; i++) {
					WebInfo info=new WebInfo(is);
					info.setIsOnDevice(deviceFiles.get(info.crosswordId()));
					MainActivity.debug(2, TAG, String.format("WebManager: on device %s", info.crosswordId()));
					insertInSeq(webInfoList, info);
				}
				mModified = false;
				MainActivity.debug(1, TAG, ">doInBackground()");
				return webInfoList;
			} catch (IOException e) {
				e.printStackTrace();
				if (webInfoList == null) {
					webInfoList = new ArrayList<WebInfo>();
				}
				// scavenge webinfo from files if possible
				for (File xwdFile: xwdFiles) {
					BufferedReader reader = null;
					try {
						reader = new BufferedReader(new InputStreamReader(new FileInputStream(xwdFile)));
						String line = reader.readLine();
						if (line != null && line.matches(CrosswordModel.CROSSWORD_HEADER+".*")) {
							WebInfo info = new WebInfo(line);
							info.setIsOnDevice(true);
							MainActivity.debug(2, TAG, String.format("WebManager: scavenge %s %s", xwdFile.getName(), info));
							if (info.crosswordId() > 0) {
								insertInSeq(webInfoList, info);
							}
						}
					} catch (IOException e2) {
						e.printStackTrace();
					} finally {
						if (reader != null) {
							try {
								reader.close();
							} catch (IOException ignored) {
							}
						}
					}
					
				}
				mModified = true;
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException ignored) {}
				}
			}
			return webInfoList;
		}

		@Override
		protected void onPostExecute(ArrayList<WebInfo> result) {
			WebManager.this.mWebInfoList = result;
			mListener.onWebManagerReady(WebManager.this);
		}

	}
}
