package uk.org.downiesoft.crossword;

import android.content.Context;
import android.os.AsyncTask;
import android.util.SparseBooleanArray;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class WebManager {
	
	public static final String TAG = WebManager.class.getName();

	private static WebManager sManagerInstance;

	private ArrayList<WebInfo> mWebInfoList;
	private WebInfoAdapter mWebInfoAdapter;
	private boolean mModified = false;

	public static WebManager getInstance() {
		if (sManagerInstance == null) {
			sManagerInstance = new WebManager();
		}
		return sManagerInstance;
	}

	private WebManager() {
	}

	public int insert(WebInfo aInfo) {
		int index = insertInSeq(mWebInfoList, aInfo);
		if (mWebInfoAdapter != null) {
			mWebInfoAdapter.notifyDataSetChanged();
		}
		return index;
	}

	public WebInfoAdapter getAdapter(Context aContext) {
		if (mWebInfoAdapter == null) {
			mWebInfoAdapter = new WebInfoAdapter(aContext, R.layout.web_info_item, mWebInfoList);
		}
		return mWebInfoAdapter;
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
				File file = new File(aContext.getFilesDir(), "webinfo");
				file.delete();
			}
		}
		mModified = false;
	}

	public void restore(Context aContext) {
		new WebInfoLoaderTask(aContext, "webinfo").execute();	
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

		private Context mContext;
		private String mFileName;

		WebInfoLoaderTask(Context aContext, String aFileName) {
			mContext = aContext;
			mFileName = aFileName;
		}

		@Override
		protected ArrayList<WebInfo> doInBackground(Void... voids) {
			ArrayList<WebInfo> webInfoList = null;
			File[] xwdFiles=MainActivity.getCrosswordDirectory().listFiles(new FileFilter(){
					@Override
					public boolean accept(File file) {
						return (file.isDirectory() || file.getName().toLowerCase(Locale.ENGLISH).endsWith(".xwd"));
					}});
			SparseBooleanArray deviceFiles = new SparseBooleanArray(xwdFiles.length);
			for (File f: xwdFiles) {
				try {
					String name = f.getName();
					int id = Integer.parseInt(name.substring(0, name.lastIndexOf('.')));
					deviceFiles.put(id, true);
				} catch (NumberFormatException e) {
					// ignore non-numeric file names
				}
			}
			try {
				DataInputStream is = new DataInputStream(mContext.openFileInput(mFileName));
				int size=is.readInt();
				webInfoList = new ArrayList<WebInfo>(size);
				for (int i=0; i < size; i++) {
					WebInfo info=new WebInfo(is);
					info.setIsOnDevice(deviceFiles.get(info.crosswordId()));
					insertInSeq(webInfoList, info);
				}
				is.close();
				return webInfoList;
			} catch (IOException e) {
				if (webInfoList != null) {
					webInfoList.clear();
				}
			}
			return webInfoList;
		}

		@Override
		protected void onPostExecute(ArrayList<WebInfo> result) {
			WebManager.this.mWebInfoList = result;
			mModified = false;
		}

	}
}
