package uk.org.downiesoft.crossword;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Collections;

public class BrowserDialog extends DialogFragment
{
	public static final String TAG = BrowserDialog.class.getName();

	public static BrowserDialog getInstance(File aFolder, String aName)
	{
		BrowserDialog self=new BrowserDialog();
		Bundle args = new Bundle();
		args.putString("folder", aFolder.toString());
		args.putString("name", aName);
		self.setArguments(args);
		return self;
	}
	
	private View iView;
	private ListView iListView;
	private ArrayList<WebInfo> iFileList;
	private File iRootDir;
	private WebInfoAdapter iBrowserAdapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getDialog()!=null)
        	getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    }
    
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		iView = inflater.inflate(R.layout.web_list, container, false);
		iListView = (ListView) iView.findViewById(R.id.webInfoList);
		iListView.setEmptyView(iView.findViewById(R.id.emptyWebInfoList));
		iListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		iListView.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				WebInfo item=iBrowserAdapter.getItem(position);
				Intent result = new Intent();
				result.putExtra("file",item.crosswordId());
				getActivity().setResult(Activity.RESULT_OK, result);
				getActivity().finish();
			}
		});
		iListView.setOnItemLongClickListener(new OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
					iListView.setMultiChoiceModeListener(new ListViewMultiChoiceModeListener());
					iListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
					iListView.setItemChecked(position, true);
					return true;
				}
			});
		Bundle args=getArguments();
		setRootDir(new File(args.getString("folder")),args.getInt("name"));
		return iView;
	}
	
	public WebInfoAdapter getAdapter()
	{
		return iBrowserAdapter;
	}
	
	private void setRootDir(File aRootDir, int aCrosswordId)
	{
		if (aRootDir!=null && aRootDir.exists())
			iRootDir=aRootDir;
		else
			iRootDir=MainActivity.getCrosswordDirectory();
		File[] files=iRootDir.listFiles(new FileFilter(){
			@Override
			public boolean accept(File file)
			{
				String name = file.getName();
				if (name.toLowerCase(Locale.ENGLISH).endsWith(".xwd")) {
					try {
						int id = Integer.parseInt(name.substring(0,name.lastIndexOf('.')));
						return id > 0;
					} catch (NumberFormatException e) {
						// ignore non-numeric files
					}
				}
				return false;
			}});
		iFileList=new ArrayList<WebInfo>(files.length);
		int selected=0;
		WebManager webManager = WebManager.getInstance();
		for (int i=0; i<files.length; i++) {
			File f = files[i];
			String name = f.getName();
			int id = Integer.parseInt(name.substring(0,name.lastIndexOf('.')));
			WebInfo info = webManager.getCrossword(id);
			if (info == null) {
				info = new WebInfo(id,0,null);
			} else {
				info.setIsOnDevice(true);
			}
			iFileList.add(info);
			if (id == aCrosswordId) { 
				selected=i;
			}
		}
		Collections.sort(iFileList);
		iBrowserAdapter = new WebInfoAdapter(getActivity(), R.layout.web_info_item, iFileList);
		iListView.setAdapter(iBrowserAdapter);
		iListView.setSelection(selected);
	}

	private class ListViewMultiChoiceModeListener implements MultiChoiceModeListener {

		private boolean mSelectAll=false;

		@Override
		public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
			MainActivity.debug(1,TAG, String.format("onItemCheckedStateChanged(%s,%s)",position,checked));
			actionMode.invalidate();
		}


		@Override
		public boolean onCreateActionMode(ActionMode p1, Menu p2) {

			p1.getMenuInflater().inflate(R.menu.browser_dialog, p2);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return true;
		}

		@Override
		public boolean onActionItemClicked(final ActionMode actionMode, MenuItem item) {
			switch (item.getItemId()) {
				case R.id.action_select_all:
					mSelectAll = true;
					for (int i=0; i<iListView.getCount(); i++) {
						iListView.setItemChecked(i,true);
					}
					return true;
				case R.id.action_delete:
					final ArrayList<WebInfo> names=new ArrayList<WebInfo>(iListView.getCheckedItemCount());
					SparseBooleanArray checked=iListView.getCheckedItemPositions();
					for (int i=0; i<iBrowserAdapter.getCount(); i++) {
						if (checked.get(i,false)) {
							names.add(iBrowserAdapter.getItem(i));
						}
					}
					final int count = names.size();
					if (count > 0) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setTitle(R.string.text_confirm_delete);
						String format = getActivity().getResources().getString(names.size()==1? R.string.text_delete_format_single: R.string.text_delete_format_plural);
						builder.setMessage(String.format(format,names.size()));
						builder.setPositiveButton(R.string.text_ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface p1, int p2) {
									int deleted = 0;
									for (WebInfo info: names) {
										File f = new File(MainActivity.getCrosswordDirectory(),String.format("%s.xwd",info.crosswordId()));
										if (f.exists() && f.delete()) {
											deleted++;
											iFileList.remove(f);
										}
									}
									if (deleted > 0) {
										iBrowserAdapter.notifyDataSetChanged();
									}
									String format = getActivity().getResources().getString(count==1? R.string.text_deleted_format_single: R.string.text_deleted_format_plural);
									Toast.makeText(getActivity(), String.format(format, deleted, count),Toast.LENGTH_SHORT).show();
									actionMode.finish();
								}
							});
						builder.create().show();
					}
					return true;
			}
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode p1) {
			iListView.clearChoices();
		}

	}
	
}
