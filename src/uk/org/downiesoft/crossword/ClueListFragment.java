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

public class ClueListFragment extends Fragment
{
	
	public interface ClueListListener
	{
		void onClueClicked(int aDirection, int aNum);
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
		iCrossword = ((MainActivity)getActivity()).getCrossword();
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
		int number=0;
		Bundle args=getArguments();
		if (args!=null)
		{
			iDirection=args.getInt("direction",0);
			number=args.getInt("number", -1);
		}
		ArrayList<Clue> clueList=iCrossword.iClues.getClueList(iDirection);
		iAdapter = new ClueListAdapter(getActivity(),R.layout.clue_list_item,clueList);
		iListView.setAdapter(iAdapter);
		if (number>=1)
		{
			for (int i=0; i<clueList.size(); i++)
			{
				if (clueList.get(i).iNumber==number)
				{
					iListView.setSelection(i);
					break;
				}
			}
		}
		iListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Clue clue=iAdapter.getItem(position);
				if (iListener!=null)
					iListener.onClueClicked(iDirection, clue.iNumber);
			}});
		return view;
	}
	
}
