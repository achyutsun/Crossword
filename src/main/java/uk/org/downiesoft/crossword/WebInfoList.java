package uk.org.downiesoft.crossword;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;


public class WebInfoList extends Fragment
{

	public static final String TAG = WebInfoList.class.getName();
	
	public interface WebInfoListListener
	{
		void onWebInfoListItemSelected(WebInfo aWebInfo);
	}
	
	private View iView;
	private ListView iListView;
	private WebInfoAdapter iAdapter;
	private WebInfoListListener iListener;
	
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		iView = inflater.inflate(R.layout.web_list, container, false);
		iListView = (ListView) iView.findViewById(R.id.webInfoList);
		iListView.setEmptyView(iView.findViewById(R.id.emptyWebInfoList));
		iListView.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				WebInfo item=iAdapter.getItem(position);
				if (iListener!=null)
					iListener.onWebInfoListItemSelected(item);
			}
		});
		iAdapter = new WebInfoAdapter(getActivity(), R.layout.web_info_item, WebManager.getInstance().getWebInfoList());
		iListView.setAdapter(iAdapter);
		return iView;
	}
	
    public void setListener(WebInfoListListener aListener)
    {
    	iListener = aListener;
		if (iAdapter != null) {
			iAdapter.notifyDataSetChanged();
		}
    }
}
