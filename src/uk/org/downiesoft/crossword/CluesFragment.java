package uk.org.downiesoft.crossword;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class CluesFragment extends Fragment
{
	public static final String TAG=CluesFragment.class.getName();
	
	private FragmentTabHost mTabHost;
	Bundle[] iClueBundle;
	ClueListFragment[] iClueLists;
	CrosswordModel iCrossword;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		iClueBundle = new Bundle[2];
		if (iClueLists==null) {
			iClueLists = new ClueListFragment[2];
		}
		iCrossword = CrosswordModel.getInstance();
		for (int i=CrosswordModel.CLUE_ACROSS; i<=CrosswordModel.CLUE_DOWN; i++)
		{
			iClueBundle[i]=new Bundle();
			iClueBundle[i].putInt("direction", i);
		}
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Bundle args=getArguments();
		int direction=0;
		int number=-1;
		if (args!=null)
		{
			direction=args.getInt("direction",-1);
			number=args.getInt("number",-1);
			if ((direction==CrosswordModel.CLUE_ACROSS || direction==CrosswordModel.CLUE_DOWN) && number>=0)
				iClueBundle[direction].putInt("number",number);
		}
		mTabHost = new FragmentTabHost(getActivity());
		mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.cluesContainer);
		mTabHost.addTab(mTabHost.newTabSpec("Across").setIndicator("Across"), ClueListFragment.class, iClueBundle[CrosswordModel.CLUE_ACROSS]);
		mTabHost.addTab(mTabHost.newTabSpec("Down").setIndicator("Down"), ClueListFragment.class, iClueBundle[CrosswordModel.CLUE_DOWN]);
		mTabHost.setCurrentTab(direction);
		return mTabHost;
	}

	public void setClueList(int direction, ClueListFragment clueList) {
		if (iClueLists==null) {
			iClueLists = new ClueListFragment[2];
		}
		iClueLists[direction] = clueList;
	}
	
	public void setClue(Clue clue, int index, int direction) {
		mTabHost.setCurrentTab(direction);
		ClueListFragment clueList = iClueLists[direction];
		MainActivity.debug(1,TAG, String.format("setClue(%s,%s): %s",index,direction,clue));
		if (clueList != null) {
			clueList.setItemChecked(index,true);
		}
		iCrossword.getClueLists().setSelectedClue(direction, index);
	}

	public void setCrossword(CrosswordModel aCrossword) {
		iCrossword = aCrossword;
		MainActivity.debug(1, TAG, String.format("setCrossword(%s)", aCrossword));
		if (iClueLists[CrosswordModel.CLUE_ACROSS] != null) {
			iClueLists[CrosswordModel.CLUE_ACROSS].setCrossword(iCrossword);		
		}
		if (iClueLists[CrosswordModel.CLUE_DOWN] != null) {
			iClueLists[CrosswordModel.CLUE_DOWN].setCrossword(iCrossword);
		}
	}
}
