package uk.org.downiesoft.crossword;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import java.util.ArrayList;

public class ClueListFragment extends Fragment
{

	public static final String TAG=ClueListFragment.class.getName();
	
	public interface ClueListListener
	{
		int onClueListCreated(ClueListFragment aClueList, int aDirection);
		void onClueClicked(int aDirection, int aNum, int aPosition);
		void onClueLongClicked(int aDirection, int aNum, int aPosition);
	}
	
	ClueListAdapter iAdapter;
	ListView iListView;
	CrosswordModel iCrossword;
	ClueListListener iListener;
	int iDirection;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		iCrossword = CrosswordModel.getInstance();
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			iListener = (ClueListListener)activity;
		} catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString() + " must implement ClueListListener");
		}
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.clue_list, container, false);
		iListView = (ListView) view.findViewById(R.id.clueList);
		iDirection=0;
		Bundle args=getArguments();
		if (args!=null)
		{
			iDirection=args.getInt("direction",0);
		}
		ArrayList<Clue> clueList=iCrossword.getClueLists().getClueListArray(iDirection);
		iAdapter = new ClueListAdapter(getActivity(),R.layout.clue_list_item,clueList);
		iListView.setAdapter(iAdapter);
		iListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Clue clue=iAdapter.getItem(position);
				MainActivity.debug(1, TAG, String.format("onItemClick(%d): %s", position, iListView.isItemChecked(position)));
				iListener.onClueClicked(iDirection, clue.iNumber, position);
				view.invalidate();
			}});
		iListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
				{
					Clue clue=iAdapter.getItem(position);
					MainActivity.debug(1, TAG, String.format("onItemLongClick(%d): %s", position, iListView.isItemChecked(position)));
					if (iListView.isItemChecked(position)) {
						iListener.onClueLongClicked(iDirection, clue.iNumber, position);
					} else {
						iListener.onClueClicked(iDirection, clue.iNumber, position);
						iListener.onClueLongClicked(iDirection, clue.iNumber, position);
					}
					return true;
				}});
		int selected = iListener.onClueListCreated(this, iDirection);
		if (selected>=0) {
			iListView.setItemChecked(selected,true);
			iListView.setSelection(selected);
		}
		MainActivity.debug(1, TAG, String.format("onCreateView: %s %s %s", iDirection, iCrossword.getCrosswordId(), this.getId()));
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		iCrossword = CrosswordModel.getInstance();
		int selected = iCrossword.getClueLists().getSelectedClueIndex(iDirection);
		MainActivity.debug(1, TAG, String.format("onResume: %s %s %s %s", iDirection, selected, iCrossword.getCrosswordId(), this.getId()));
		if (selected >= 0) {
			iListView.setItemChecked(selected, true);
			iListView.setSelection(selected);
		} else {
			iListView.clearChoices();
		}
	}
	
	public void setItemChecked(int position, boolean checked) {
		iListView.setItemChecked(position, checked);
		iListView.smoothScrollToPosition(position);
	}
	
	public void setCrossword(CrosswordModel aCrossword) {
		MainActivity.debug(1, TAG, String.format(">setCrossword(%s): %s %s", aCrossword.getCrosswordId(), iDirection, this.getId()));
		iCrossword = aCrossword;
		iAdapter = new ClueListAdapter(getActivity(),R.layout.clue_list_item,iCrossword.getClueLists().getClueListArray(iDirection));
		iListView.setAdapter(iAdapter);
		iListView.invalidate();
		iAdapter.notifyDataSetChanged();
		MainActivity.debug(1, TAG, String.format("<setCrossword(%s): %s %s", iCrossword.getCrosswordId(), iDirection, this.getId()));
	}
	
}
