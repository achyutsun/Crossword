package uk.org.downiesoft.crossword;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CluesFragment extends Fragment
{
	public static final String TAG=CluesFragment.class.getName();
	
	private FragmentTabHost mTabHost;
	Bundle[] iClueBundle;
	CrosswordModel iCrossword;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		iClueBundle = new Bundle[2];
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
		mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.clueList);
		mTabHost.addTab(mTabHost.newTabSpec("Across").setIndicator("Across"), ClueListFragment.class, iClueBundle[CrosswordModel.CLUE_ACROSS]);
		mTabHost.addTab(mTabHost.newTabSpec("Down").setIndicator("Down"), ClueListFragment.class, iClueBundle[CrosswordModel.CLUE_DOWN]);
		mTabHost.setCurrentTab(direction);
		return mTabHost;
	}

}
