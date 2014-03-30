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
	private ClueListView[] iClueListViews;
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

	private class OnClueListItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> adapterView, View viewGroup, int position, long id) {
			ClueListAdapter adapter =((ClueListView)adapterView).getAdapter();
			Clue clue=adapter.getItem(position);
//			adapter.setSelectedClue(position);
			GridFragment.this.clueClicked(clue.type(), clue.number(), position);
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
		iClueListViews = new ClueListView[2];
		iClueListViews[0] = (ClueListView) iView.findViewById(R.id.cluesListAcross);
		iClueListViews[1] = (ClueListView) iView.findViewById(R.id.cluesListDown);
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
			for (int i=0; i < 2; i++) {
				if (iClueListViews[i] != null) {
					iClueListViews[i].setClueList(iCrossword.clues().getClueList(i));
					iClueListViews[i].setOnItemClickListener(new OnClueListItemClickListener());
//					iClueListViews[i].setOnItemSelectedListener(new OnClueListItemSelectedListener());
					iClueListViews[i].invalidate();
				}
			}
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
		Log.d(TAG,String.format("onActivityCreated"));
		
		if (savedInstanceState != null) {
			iCursorX = savedInstanceState.getInt("cursorX", 0);
			iCursorY = savedInstanceState.getInt("cursorY", 0);
			iCursorDirection = savedInstanceState.getInt("cursorDirection", CrosswordModel.CLUE_ACROSS);
		} else {
			getSavedState();
		}
		if (iCrossword != null && iCrossword.isValid()) {
			iGridView.setCursor(iCursorX, iCursorY, iCursorDirection);
			Clue clue=iCrossword.clueAt(new Point(iCursorX,iCursorY), iCursorDirection);
			if (clue!=null && iClueListViews[iCursorDirection]!=null) {
				iClueListViews[iCursorDirection].getAdapter().setSelectedClue(clue);
				iClueListViews[1-iCursorDirection].getAdapter().setSelectedClue(null);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG,String.format("start onResume"));
		iGuardBackKey = true;
		iGridView.setObserver(this);
		iGridView.setCrossword(iCrossword);
		if (iCrossword != null && iCrossword.isValid()) {
			iGridView.setCursor(iCursorX, iCursorY, iCursorDirection);
			Clue clue=iCrossword.clueAt(new Point(iCursorX,iCursorY), iCursorDirection);
			if (clue!=null && iClueListViews[iCursorDirection]!=null) {
				iClueListViews[iCursorDirection].getAdapter().setSelectedClue(clue);
				iClueListViews[1-iCursorDirection].getAdapter().setSelectedClue(null);
			}
		}	
		Log.d(TAG,String.format("end onResume"));
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
							iGridView.setCursor(pos.x, pos.y, dir);
							if (iTextView != null)
								iTextView.setText(clue.toString());
							return;
						}
					}
				}
	}

	@Override
	public void onClueSelected(Clue aClue, int aCursorX, int aCursorY, int aCursorDirection) {
		Log.d(TAG,String.format("onClueSelected(%s)",aClue.toString()));
		if (aClue != null && iTextView!=null) {
			iTextView.setText(aClue.toString());
		}
		iCursorX = aCursorX;
		iCursorY = aCursorY;
		iCursorDirection = aCursorDirection;
		saveState();
		if (aClue!=null && iClueListViews[aCursorDirection]!=null) {
			ClueListAdapter adapter = iClueListViews[aCursorDirection].getAdapter();
			int index=adapter.getPosition(aClue);
			iClueListViews[iCursorDirection].getAdapter().setSelectedClue(index);
			iClueListViews[1-iCursorDirection].getAdapter().setSelectedClue(null);
			iClueListViews[aCursorDirection].smoothScrollToPosition(index);
		}
		iGridView.invalidate();
	}

	@Override
	public void onWordEntered(String aWord) {
		enterWord(aWord);
		((MainActivity)getActivity()).synch();
		FragmentManager fm = getActivity().getSupportFragmentManager();
		fm.popBackStack("word_entry", FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
		Log.d(TAG,String.format("clueClicked(%d)",aPosition));
		if (aPosition >= 0) {
			Point pos=iCrossword.locateClue(aDirection, aNum);
			iCursorX = pos.x;
			iCursorY = pos.y;
			iCursorDirection = aDirection;
			if (iClueListViews[iCursorDirection]!=null) {
				ClueListAdapter adapter = iClueListViews[iCursorDirection].getAdapter();
				if (aPosition!=adapter.getSelectedCluePosition()) {
					iClueListViews[iCursorDirection].getAdapter().setSelectedClue(aPosition);
					iClueListViews[1-iCursorDirection].getAdapter().setSelectedClue(null);
					iClueListViews[iCursorDirection].smoothScrollToPosition(aPosition);
				} else {
					String hint = iCrossword.getCluePattern(new Point(iCursorX, iCursorY), iCursorDirection);
					FragmentManager fm = getActivity().getSupportFragmentManager();
					iWordEntry = WordEntryDialog.getInstance(iGridView.getCurrentClue(), "", hint, GridFragment.this);
					iWordEntry.show(fm, WordEntryDialog.TAG);
				}
			}
			iGridView.setCursor(pos.x, pos
			.y, aDirection);			
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
