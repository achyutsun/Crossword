package uk.org.downiesoft.crossword;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.widget.LinearLayout;
import java.util.ArrayList;
import android.util.Log;
import android.opengl.Visibility;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.Adapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListAdapter;
import android.widget.ArrayAdapter;
import android.content.res.Configuration;

public class GridFragment extends Fragment implements GridView.GridViewListener, WordEntryDialog.WordEntryListener {

	public static final String TAG=GridFragment.class.getName();

	public interface GridFragmentListener {
		public void onClueButtonPressed(Bundle aCurrentClue);
	}

	private View iView;
	private RelativeLayout iGridLayout;
	private RelativeLayout iEmptyLayout;
	private GridView iGridView;
	private TextView iTextView;
	private ListView[] iClueListViews;
	private Button iEnterButton;
	private Button iEraseButton;
	private Button iCluesButton;
	private CrosswordModel iCrossword;
	private WordEntryDialog iWordEntry;
	private int iCursorX=0;
	private int iCursorY=0;
	private int iCursorDirection=CrosswordModel.CLUE_ACROSS;
	
	private boolean iGuardBackKey=false;
	private long iBackTimestampMillis=0;
	private final int iBackTimeoutMillis=3000;
	private GridFragmentListener iListener;
	private Clue mClickedClue;

	private class OnClueListItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> adapterView, View viewGroup, int position, long id) {
			Log.d(TAG,String.format("onItemClick(%d)",position));
			ClueListAdapter adapter = (ClueListAdapter)adapterView.getAdapter();
			Clue clue=adapter.getItem(position);
//			adapter.setSelectedClue(position);
			GridFragment.this.clueClicked(clue.type(), clue.number(), position);
		}
	}

	private class OnClueListItemSelectedListener implements ListView.OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> adapterView, View viewGroup, int position, long id) {
			Log.d(TAG,String.format("onItemSelected(%d)",position));
			ClueListAdapter adapter = (ClueListAdapter)adapterView.getAdapter();
			Clue clue=adapter.getItem(position);
			int direction=clue.type();
			iClueListViews[1-direction].setItemChecked(-1,false);
			iClueListViews[1-direction].clearChoices();
			iClueListViews[1-direction].invalidate();
			iClueListViews[direction].setItemChecked(position,true);
			iClueListViews[direction].requestFocus();
			Point pos=iCrossword.locateClue(clue.type(),clue.number());
			if (pos!=null) {
				iGridView.setCursor(pos.x,pos.y,direction,false);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> p1) {
			// TODO: Implement this method
		}
		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			iListener = (GridFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement " + GridFragmentListener.class.getName());
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		iView = inflater.inflate(R.layout.crossword_grid, container, false);
		iGridLayout = (RelativeLayout)iView.findViewById(R.id.gridLayout);
		iEmptyLayout = (RelativeLayout)iView.findViewById(R.id.emptyLayout);
		iGridView = (GridView) iView.findViewById(R.id.gridView);
		iTextView = (TextView) iView.findViewById(R.id.clueText);
		iClueListViews = new ListView[2];
		iClueListViews[0] = (ListView) iView.findViewById(R.id.cluesListAcross);
		iClueListViews[1] = (ListView) iView.findViewById(R.id.cluesListDown);
		iEnterButton = (Button) iView.findViewById(R.id.gridEnterButton);
		iEraseButton = (Button) iView.findViewById(R.id.gridEraseButton);
		iCluesButton = (Button) iView.findViewById(R.id.gridCluesButton);
		iCrossword = ((MainActivity)getActivity()).getCrossword();

		if (iCrossword == null || !iCrossword.isValid()) {
			iGridLayout.setVisibility(View.GONE);
			iEmptyLayout.setVisibility(View.VISIBLE);
		} else {
			iGridLayout.setVisibility(View.VISIBLE);
			iEmptyLayout.setVisibility(View.GONE);
		}
		if (iGridView != null) {
			iGridView.setObserver(this);
			iGridView.setCrossword(iCrossword);
		}
		iEnterButton.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View arg0) {
					String hint = iCrossword.getCluePattern(new Point(iCursorX, iCursorY), iCursorDirection);
					FragmentManager fm = getActivity().getSupportFragmentManager();
					iWordEntry = WordEntryDialog.getInstance(iGridView.getCurrentClue(), "", hint, GridFragment.this);
					iWordEntry.show(fm, WordEntryDialog.TAG);
//					FragmentTransaction ft = fm.beginTransaction();
//					ft.replace(R.id.fragmentContainer, iWordEntry, "word_entry");
//					ft.addToBackStack("word_entry");
//					ft.commit();
				}
			});
		iEraseButton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View arg0) {
					iCrossword.eraseWord(new Point(iCursorX, iCursorY), iCursorDirection);
					iGridView.invalidate();
				}
			});
		if (iTextView != null) {
			iCluesButton.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View arg0) {
						Clue clue=iCrossword.clueAt(new Point(iCursorX, iCursorY), iCursorDirection);
						Bundle args=new Bundle();
						args.putInt("direction", clue.iType);
						args.putInt("number", clue.iNumber);
						iListener.onClueButtonPressed(args);
					}
				});
		} else {
			iCluesButton.setVisibility(View.GONE);
		}
		return iView;
	}

	@Override 
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (savedInstanceState != null) {
			iCursorX = savedInstanceState.getInt("cursorX", 0);
			iCursorY = savedInstanceState.getInt("cursorY", 0);
			iCursorDirection = savedInstanceState.getInt("cursorDirection", CrosswordModel.CLUE_ACROSS);
		} else {
			getSavedState();
		}
		Clue clue=iCrossword.clueAt(new Point(iCursorX,iCursorY), iCursorDirection);
		int position = iCrossword.clues().getClueList(iCursorDirection).indexOf(clue);		
		if (iClueListViews[0] != null) {
			for (int i=0; i < 2; i++) {
				iClueListViews[i].setAdapter(new ClueListAdapter(getActivity(),R.layout.clue_list_item,iCrossword.clues().getClueList(i),
					i==iCursorDirection?position:-1));
				iClueListViews[i].setOnItemClickListener(new OnClueListItemClickListener());
				iClueListViews[i].setOnItemSelectedListener(new OnClueListItemSelectedListener());
//					iClueListViews[i].setOnItemSelectedListener(new OnClueListItemSelectedListener());
			}
			iClueListViews[iCursorDirection].setItemChecked(position,true);
			iClueListViews[iCursorDirection].setSelection(position);
		}
		
		if (iCrossword != null && iCrossword.isValid()) {
			iGridView.setCursor(iCursorX, iCursorY, iCursorDirection, iTextView!=null);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		iGuardBackKey = true;
		iGridView.setObserver(this);
		iGridView.setCrossword(iCrossword);
		if (iCrossword != null && iCrossword.isValid()) {
			iGridView.setCursor(iCursorX, iCursorY, iCursorDirection, false);
		}	
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (iGridView != null) {
			iCursorX = iGridView.getCursorX();
			iCursorY = iGridView.getCursorY();
			iCursorDirection = iGridView.getCursorDirection();
			outState.putInt("cursorX", iCursorX);
			outState.putInt("cursorY", iCursorY);
			outState.putInt("cursorDirection", iCursorDirection);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		saveState();
		iGuardBackKey = false;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// only called for keyboard visibility changes on word entry dialog
	}

	
	public void getSavedState() {
		SharedPreferences settings=getActivity().getSharedPreferences("CROSSWORD_SETTING", Context.MODE_PRIVATE);
		iCursorX = settings.getInt("cursorX", 0);
		iCursorY = settings.getInt("cursorY", 0);
		iCursorDirection = settings.getInt("cursorDirection", CrosswordModel.CLUE_ACROSS);		
	}

	public void saveState() {
		SharedPreferences settings=getActivity().getSharedPreferences("CROSSWORD_SETTING", Context.MODE_PRIVATE);
		Editor editor=settings.edit();
		editor.putInt("cursorX", iCursorX);
		editor.putInt("cursorY", iCursorY);
		editor.putInt("cursorDirection", iCursorDirection);
		editor.commit();
	}

	public void setCrossword(CrosswordModel aCrossword) {
		iCrossword = aCrossword;
		if (iGridView != null) {
			iGridView.setCrossword(iCrossword);
		}
		if (iCrossword == null || !iCrossword.isValid()) {
			iGridLayout.setVisibility(View.GONE);
			iEmptyLayout.setVisibility(View.VISIBLE);
		} else {
			iGridLayout.setVisibility(View.VISIBLE);
			iEmptyLayout.setVisibility(View.GONE);
		}
	}

	public void resetClue() {
		for (int row=0; row < CrosswordModel.GRID_SIZE; row++)
			for (int col=0; col < CrosswordModel.GRID_SIZE; col++)
				if (!iCrossword.isBlank(col, row)) {
					Point pos=new Point(col, row);
					for (int dir=CrosswordModel.CLUE_ACROSS; dir <= CrosswordModel.CLUE_DOWN; dir++) {
						Clue clue=iCrossword.clueAt(pos, dir);
						if (clue != null) {
							iGridView.setCursor(pos.x, pos.y, dir, false);
							if (iTextView != null)
								iTextView.setText(clue.toString());
							return;
						}
					}
				}
	}

	@Override
	public void onClueSelected(Clue aClue, int aCursorX, int aCursorY, int aCursorDirection) {
		if (aClue != null && iTextView!=null) {
			iTextView.setText(aClue.toString());
		}
		iCursorX = aCursorX;
		iCursorY = aCursorY;
		iCursorDirection = aCursorDirection;
		saveState();
		if (aClue!=null && iClueListViews[aCursorDirection]!=null) {
			ClueListAdapter adapter = (ClueListAdapter)iClueListViews[aCursorDirection].getAdapter();
			int index=adapter.getPosition(aClue);
			iClueListViews[1-iCursorDirection].setItemChecked(-1,false);
			iClueListViews[1-iCursorDirection].clearChoices();
			iClueListViews[1-iCursorDirection].invalidate();
			iClueListViews[iCursorDirection].setItemChecked(index,true);
			iClueListViews[iCursorDirection].setSelection(index);
			iClueListViews[aCursorDirection].smoothScrollToPosition(index);
			iClueListViews[aCursorDirection].requestFocus();
		}
	}

	@Override
	public void onWordEntered(String aWord) {
		enterWord(aWord);
		((MainActivity)getActivity()).synch();
		//FragmentManager fm = getActivity().getSupportFragmentManager();
		//fm.popBackStack("word_entry", FragmentManager.POP_BACK_STACK_INCLUSIVE);
		if (iCrossword.crosswordCompleted()) {
			Toast.makeText(getActivity(), R.string.text_congrats, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onWordEntryCancelled() {
		FragmentManager fm = getActivity().getSupportFragmentManager();
		fm.popBackStack("word_entry", FragmentManager.POP_BACK_STACK_INCLUSIVE);
		Fragment frag=fm.findFragmentByTag("word_entry");
		if (frag != null)
			fm.beginTransaction().remove(frag).commit();
	}

	public void clueClicked(int aDirection, int aNum, int aPosition) {
		if (aPosition >= 0) {
			Point pos=iCrossword.locateClue(aDirection, aNum);
			iCursorX = pos.x;
			iCursorY = pos.y;
			iCursorDirection = aDirection;
			if (iClueListViews[0]!=null) {
				iClueListViews[1-iCursorDirection].setItemChecked(-1,false);
				iClueListViews[1-iCursorDirection].clearChoices();
				iClueListViews[1-iCursorDirection].invalidate();
				
					Clue clue=iCrossword.clueAt(pos, aDirection);
					if ((mClickedClue != null && clue.equals(mClickedClue)) || (aPosition == iClueListViews[iCursorDirection].getSelectedItemPosition() 
						&& aPosition == iClueListViews[iCursorDirection].getCheckedItemPosition())) {
						String hint = iCrossword.getCluePattern(new Point(iCursorX, iCursorY), iCursorDirection);
						FragmentManager fm = getActivity().getSupportFragmentManager();
						iWordEntry = WordEntryDialog.getInstance(iGridView.getCurrentClue(), "", hint, GridFragment.this);
						iWordEntry.show(fm, WordEntryDialog.TAG);
					} else {
						mClickedClue = clue;
						iClueListViews[iCursorDirection].setSelection(aPosition);					
						iClueListViews[iCursorDirection].setItemChecked(aPosition, true);
					}
			}
			iGridView.setCursor(pos.x, pos.y, aDirection,false);			
		}
	}

	public boolean offerBackKeyEvent() {
		if (iGuardBackKey) {
	    	long now=System.currentTimeMillis();
	    	if (now - iBackTimestampMillis > iBackTimeoutMillis) {
	    		Toast.makeText(getActivity(), R.string.text_press_back_again, Toast.LENGTH_SHORT).show();
	    		iBackTimestampMillis = now;
		        return true;
	    	}
		}
		return false;
	}

	public void update() {
		if (iGridView != null)
			iGridView.invalidate();
	}

	public String getClueText() {
		return iCrossword.clueAt(new Point(iCursorX, iCursorY), iCursorDirection).iText;
	}

	public String getCluePattern() {
		return iCrossword.getCluePattern(new Point(iCursorX, iCursorY), iCursorDirection).replace('*', '.');
	}

	public void enterWord(String aWord) {
		if (iCrossword.enterWord(new Point(iCursorX, iCursorY), iCursorDirection, aWord)) {
			iGridView.invalidate();
		}
	}

}
