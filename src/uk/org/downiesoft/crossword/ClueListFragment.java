package uk.org.downiesoft.crossword;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.util.Log;

public class ClueListFragment extends Fragment
{

	public static final String TAG=ClueListFragment.class.getName();
	
	public interface ClueListListener
	{
		int onClueListCreated(ClueListFragment aClueList, int aDirection);
		void onClueClicked(int aDirection, int aNum, int aPosition);
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
		ArrayList<Clue> clueList=iCrossword.iClues.getClueList(iDirection);
		iAdapter = new ClueListAdapter(getActivity(),R.layout.clue_list_item,clueList);
		iListView.setAdapter(iAdapter);
		iListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Clue clue=iAdapter.getItem(position);
				MainActivity.debug(1, TAG, String.format("onItemClick(%d): %s %s", position, iListView, iListView.getChoiceMode()));
				iListener.onClueClicked(iDirection, clue.iNumber, position);
			}});
		int selected = iListener.onClueListCreated(this, iDirection);
		if (selected>=0) {
			iListView.setItemChecked(selected,true);
			iListView.setSelection(selected);
		}
		MainActivity.debug(1, TAG, String.format("onCreateView: %s %s", iListView, iListView.getChoiceMode()));
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		int selected = iCrossword.getClueLists().getSelectedClueIndex(iDirection);
		if (selected >= 0) {
			iListView.setItemChecked(selected, true);
			iListView.setSelection(selected);
		} else {
			iListView.clearChoices();
		}
	}
	
	public void setItemChecked(int position, boolean checked) {
		iListView.setItemChecked(position, checked);
		iListView.setSelection(position);
	}
	
}
