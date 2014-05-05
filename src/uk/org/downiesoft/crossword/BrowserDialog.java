package uk.org.downiesoft.crossword;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class BrowserDialog extends DialogFragment
{
	public static final String FRAGMENT_TAG = "BROWSER_FRAGMENT";

	public interface BrowserDialogListener
	{
		public void onFileSelected(File aFile);
	}

	public static BrowserDialog getInstance(String aFolder, String aName)
	{
		BrowserDialog self=new BrowserDialog();
		Bundle args = new Bundle();
		args.putString("folder", aFolder);
		args.putString("name", aName);
		self.setArguments(args);
		return self;
	}
	
	private View iView;
	private ListView iListView;
	private ArrayList<File> iFileList;
	private File iRootDir;
	private BrowserAdapter iBrowserAdapter;
	private BrowserDialogListener iListener;
	
	
    @Override
    public void onAttach(Activity activity)
    {
            super.onAttach(activity);
            try
            {
                    iListener = (BrowserDialogListener) activity;
            } catch (ClassCastException e)
            {
                    throw new ClassCastException(activity.toString() + " must implement BrowserDialogListener");
            }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getDialog()!=null)
        	getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    }
    
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		iView = inflater.inflate(R.layout.browser_dialog, container, false);
		iListView = (ListView) iView.findViewById(R.id.file_list);
		iListView.setEmptyView(iView.findViewById(R.id.empty_file_list));
		iListView.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				File item=iBrowserAdapter.getItem(position);
				iListener.onFileSelected(item);
				if (getDialog()!=null)
					getDialog().dismiss();
			}
		});
		//if (getDialog()!=null)
		//	getDialog().setTitle(R.string.text_openfile);
		Bundle args=getArguments();
		setRootDir(new File(args.getString("folder")),args.getString("name"));
		return iView;
	}
	
	public BrowserAdapter getAdapter()
	{
		return iBrowserAdapter;
	}
	
	private void setRootDir(File aRootDir, String aName)
	{
		if (aRootDir!=null && aRootDir.exists())
			iRootDir=aRootDir;
		else
			iRootDir=getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		File[] files=iRootDir.listFiles(new FileFilter(){
			@Override
			public boolean accept(File file)
			{
				return (file.isDirectory() || file.getName().toLowerCase(Locale.ENGLISH).endsWith(".xwd"));
			}});
		iFileList=new ArrayList<File>();
		int selected=0;
		for (int i=0; i<files.length; i++)
		{
			iFileList.add(files[i]);
			if (aName!=null && files[i].getName().compareTo(aName)==0)
				selected=i;
		}
		iBrowserAdapter = new BrowserAdapter(getActivity(), R.layout.browser_item, iFileList, WebManager.getInstance());
		iListView.setAdapter(iBrowserAdapter);
		iListView.setSelection(selected);
	}
	
}
