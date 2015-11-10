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
import android.os.Handler;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.view.Gravity;
import android.content.Intent;
import android.view.inputmethod.*;

public class GridFragment extends Fragment implements GridView.GridViewListener {

	public static final String TAG=GridFragment.class.getName();

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
	private int iCursorX=0;
	private int iCursorY=0;
	private int iCursorDirection=CrosswordModel.CLUE_ACROSS;
	
	private boolean iGuardBackKey=false;
	private long iBackTimestampMillis=0;
	private final int iBackTimeoutMillis=3000;
	private Clue mClickedClue;
	private Handler mHandler = new Handler();
	
	private class CancelChoice implements Runnable {
		
		private ListView mListView;
		
		public CancelChoice(ListView aList) {
			mListView = aList;	
		}
		
		@Override
		public void run() {
			if (mListView.getCheckedItemCount()>0) {
				mListView.setItemChecked(mListView.getCheckedItemPosition(),false);
				mListView.invalidate();
			}
		}
		
	}

	private class OnClueListItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> adapterView, View viewGroup, int position, long id) {
			MainActivity.debug(1, TAG,String.format("onItemClick(%d)",position));
			ClueListAdapter adapter = (ClueListAdapter)adapterView.getAdapter();
			Clue clue=adapter.getItem(position);
			GridFragment.this.clueClicked(clue.type(), clue.number(), position);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MainActivity.debug(1, TAG,String.format("onCreateView %s",this));
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
		iCrossword = CrosswordModel.getInstance();

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
					wordEntryDialog(iGridView.getCurrentClue(), "", hint);
				}
			});
		iEraseButton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View arg0) {
					iCrossword.eraseWord(new Point(iCursorX, iCursorY), iCursorDirection);
					iGridView.highlightCurrentClue(true);
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
						Intent intent = new Intent(getActivity(), CluesActivity.class);
						intent.putExtra("currentClue",args);
						startActivityForResult(intent, CluesActivity.REQUEST_CLUE);
					}
				});
		} else {
			iCluesButton.setVisibility(View.GONE);
		}
		return iView;
	}

	@Override 
	public void onActivityCreated(Bundle savedInstanceState) {
		MainActivity.debug(1, TAG,String.format("onActivityCreated(%s)",savedInstanceState));
		super.onActivityCreated(savedInstanceState);
		iCrossword = CrosswordModel.getInstance();
		iGridView.setCrossword(iCrossword);
		getSavedState();
		if (savedInstanceState != null) {
			iCursorX = savedInstanceState.getInt("cursorX", iCursorX);
			iCursorY = savedInstanceState.getInt("cursorY", iCursorY);
			iCursorDirection = savedInstanceState.getInt("cursorDirection", iCursorDirection);
		}
		Clue clue=iCrossword.clueAt(new Point(iCursorX,iCursorY), iCursorDirection);
		if (clue == null) {
			resetClue();
			clue=iCrossword.clueAt(new Point(iCursorX,iCursorY), iCursorDirection);
		}
		int position = iCrossword.clues().getClueList(iCursorDirection).indexOf(clue);		
		if (iClueListViews[0] != null) {
			for (int i=0; i < 2; i++) {
				iClueListViews[i].setAdapter(new ClueListAdapter(getActivity(),R.layout.clue_list_item,iCrossword.clues().getClueList(i)));
				iClueListViews[i].setOnItemClickListener(new OnClueListItemClickListener());
				iClueListViews[i].setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//				iClueListViews[i].setOnItemSelectedListener(new OnClueListItemSelectedListener());
//					iClueListViews[i].setOnItemSelectedListener(new OnClueListItemSelectedListener());
			}
			iClueListViews[iCursorDirection].setItemChecked(position,true);
			iClueListViews[iCursorDirection].setSelection(position);
		}
		if (iGridView != null) {
			iGridView.setCursor(iCursorX,iCursorY,iCursorDirection,true);
			iGridView.redraw();
		}
		MainActivity.debug(1, TAG,String.format("<onActivityCreated(%s,%s,%s) %s",iCursorX, iCursorY, iCursorDirection,clue));
	}

	@Override
	public void onResume() {
		super.onResume();
		iGuardBackKey = true;
		iGridView.setObserver(this);
//		iGridView.setCrossword(iCrossword);
//		if (iCrossword != null && iCrossword.isValid()) {
//			iGridView.setCursor(iCursorX, iCursorY, iCursorDirection, iTextView!=null);
//		}
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
		MainActivity.debug(1, TAG,String.format("setCrossword(%s): %s",aCrossword, this));
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
							MainActivity.debug(1, TAG,String.format("resetClue(%s)",clue));
							if (iGridView != null) {
								iCursorX = pos.x;
								iCursorY = pos.y;
								iCursorDirection = dir;
								saveState();
								iGridView.setCursor(pos.x, pos.y, dir, false);
								iGridView.redraw();
							}
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
			mClickedClue = aClue;
			iClueListViews[iCursorDirection].setSelection(index);
			iClueListViews[aCursorDirection].requestFocus();
		}
	}

	public void clueClicked(int aDirection, int aNum, int aPosition) {
		if (aPosition >= 0) {
			Point pos=iCrossword.locateClue(aDirection, aNum);
			iCursorX = pos.x;
			iCursorY = pos.y;
			iCursorDirection = aDirection;
			if (iClueListViews[0]!=null) {
				ListView clueList=iClueListViews[1-iCursorDirection];
				if (clueList.getCheckedItemCount()>0) {
					mHandler.post(new CancelChoice(clueList));
//					clueList.setItemChecked(clueList.getCheckedItemPosition(),false);
				}

				Clue clue=iCrossword.clueAt(pos, aDirection);
				if ((mClickedClue != null && clue.equals(mClickedClue)) || (aPosition == iClueListViews[iCursorDirection].getSelectedItemPosition() 
					&& aPosition == iClueListViews[iCursorDirection].getCheckedItemPosition())) {
					String hint = iCrossword.getCluePattern(new Point(iCursorX, iCursorY), iCursorDirection);
					wordEntryDialog(iGridView.getCurrentClue(), "", hint);
				} else {
					mClickedClue = clue;
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
			iGridView.redraw();
	}

	public String getClueText() {
		return iCrossword.clueAt(new Point(iCursorX, iCursorY), iCursorDirection).iText;
	}

	public String getCluePattern() {
		return iCrossword.getCluePattern(new Point(iCursorX, iCursorY), iCursorDirection).replace('*', '.');
	}

	public void enterWord(String aWord) {
		if (iCrossword.enterWord(new Point(iCursorX, iCursorY), iCursorDirection, aWord)) {
			iGridView.highlightCurrentClue(true);
		}
	}

	private void wordEntryDialog(Clue aClue, String aWord, final String aHint) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setCancelable(true);
		builder.setTitle(R.string.text_enter_word);
		View dialog = getActivity().getLayoutInflater().inflate(R.layout.word_entry, null);
		final TextView clue = (TextView)dialog.findViewById(R.id.entryClue);
		final EditText word = (EditText)dialog.findViewById(R.id.entryEditText);
		clue.setText(aClue.toString());
		word.setText(aWord);
		word.setHint(aHint);
		builder.setView(dialog);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton(R.string.text_ok, null);
		builder.setNegativeButton(R.string.text_cancel, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		final AlertDialog alert = builder.create();
		alert.show();
		alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View p1) {
					String entered = word.getText().toString().toUpperCase().replace(" ", "");
					String hint = aHint.toUpperCase().replace('*', '.');
					if (entered.matches(hint)) {
						enterWord(entered);
						BluetoothManager.synch(iCrossword);
						if (iCrossword.crosswordCompleted()) {
							Toast.makeText(getActivity(), R.string.text_congrats, Toast.LENGTH_LONG).show();
						}
						if (alert.getCurrentFocus()!=null) {
							InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
							inputMethodManager.hideSoftInputFromWindow(alert.getCurrentFocus().getWindowToken(), 0);
						}
						alert.dismiss();
					} else {
						Toast toast = Toast.makeText(getActivity(), R.string.text_bad_fit, Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();						
					}
				}
			});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case CluesActivity.REQUEST_CLUE:
				if (data!=null && data.getExtras()!=null) {
					int direction = data.getExtras().getInt("direction");
					int number = data.getExtras().getInt("number");
					int position = data.getExtras().getInt("position",-1);
					clueClicked(direction, number, position);
				}
				break;
			default:
				super.onActivityResult(requestCode,resultCode,data);
		}
	}

	
}
