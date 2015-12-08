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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class GridFragment extends Fragment implements GridView.GridViewListener {

	public static final String TAG=GridFragment.class.getName();

	private View iView;
	private RelativeLayout iGridLayout;
	private TextView iEmptyView;
	private GridView iGridView;
	private TextView iTextView;
	private CrosswordModel iCrossword;
	private int iCursorX=0;
	private int iCursorY=0;
	private int iCursorDirection=CrosswordModel.CLUE_ACROSS;
	
	private boolean iGuardBackKey=false;
	private long iBackTimestampMillis=0;
	private final int iBackTimeoutMillis=3000;
	private CluesFragment mCluesFragment;
	
	
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
		iEmptyView = (TextView)iView.findViewById(R.id.emptyLayout);
		iGridView = (GridView) iView.findViewById(R.id.gridView);
		iTextView = (TextView) iView.findViewById(R.id.clueText);
		iCrossword = CrosswordModel.getInstance();

		if (iCrossword == null || !iCrossword.isValid()) {
			iGridLayout.setVisibility(View.GONE);
			iEmptyView.setVisibility(View.VISIBLE);
		} else {
			iGridLayout.setVisibility(View.VISIBLE);
			iEmptyView.setVisibility(View.GONE);
		}
		if (iGridView != null) {
			iGridView.setObserver(this);
			iGridView.setCrossword(iCrossword);
		}
		setHasOptionsMenu(true);
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
		if (iCrossword.isValid() && iGridView != null) {
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
		Clue clue=iCrossword.clueAt(new Point(iCursorX,iCursorY), iCursorDirection);
		int position = iCrossword.getClueLists().getClueIndex(iCursorDirection,clue);
		mCluesFragment = (CluesFragment)getActivity().getSupportFragmentManager().findFragmentByTag(CluesFragment.TAG);
		if (mCluesFragment != null) {
			mCluesFragment.setClue(clue, position, iCursorDirection);
		}
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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.grid_fragment,menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO: Implement this method
		switch (item.getItemId()) {
			case R.id.action_enter:
				String hint = iCrossword.getCluePattern(new Point(iCursorX, iCursorY), iCursorDirection);
				wordEntryDialog(iGridView.getCurrentClue(), "", hint);
				return true;
			case R.id.action_erase:
				iCrossword.eraseWord(new Point(iCursorX, iCursorY), iCursorDirection);
				iGridView.highlightCurrentClue(true);
				return true;
		}
		return super.onOptionsItemSelected(item);
		
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
		MainActivity.debug(1, TAG,String.format("setCrossword(%s): %s",aCrossword, iCrossword.isValid()));
		iCrossword = aCrossword;
		if (iGridView != null) {
			iGridView.setCrossword(iCrossword);
		}
		if (iCrossword == null || !iCrossword.isValid()) {
			iGridLayout.setVisibility(View.GONE);
			iEmptyView.setVisibility(View.VISIBLE);
		} else {
			iGridLayout.setVisibility(View.VISIBLE);
			iEmptyView.setVisibility(View.GONE);
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
	public void onGridClueSelected(Clue aClue, int aCursorX, int aCursorY, int aCursorDirection) {
		MainActivity.debug(1,TAG,String.format("onClueSelected(%s,%s,%s,%s): %s",aCursorX,aCursorY,aCursorDirection,aClue,mCluesFragment));
		if (aClue != null && iTextView!=null) {
			iTextView.setText(aClue.toString());
		}
		iCursorX = aCursorX;
		iCursorY = aCursorY;
		iCursorDirection = aCursorDirection;
		saveState();
		if (aClue!=null && mCluesFragment != null) {
			int index = iCrossword.getClueLists().getClueIndex(aCursorDirection, aClue);
			mCluesFragment.setClue(aClue, index, aCursorDirection);
		}
	}

	@Override
	public void onGridClueLongPress(Clue aClue, int aCursorX, int aCursorY, int aCursorDirection) {
		MainActivity.debug(1,TAG,String.format("onClueLongPress(%s,%s,%s,%s): %s",aCursorX,aCursorY,aCursorDirection,aClue,mCluesFragment));
		iCursorX = aCursorX;
		iCursorY = aCursorY;
		iCursorDirection = aCursorDirection;
		saveState();
		if (aClue != null) {
			String hint = iCrossword.getCluePattern(new Point(iCursorX, iCursorY), iCursorDirection);
			wordEntryDialog(aClue, "", hint);
		}
	}

	public void clueClicked(int aDirection, int aNum, int aPosition) {
		if (aPosition >= 0) {
			Point pos=iCrossword.locateClue(aDirection, aNum);
			iCursorX = pos.x;
			iCursorY = pos.y;
			iCursorDirection = aDirection;
			Clue clue=iCrossword.clueAt(pos, aDirection);
			if (mCluesFragment != null) {
				int index = iCrossword.getClueLists().getClueIndex(iCursorDirection, clue);
				mCluesFragment.setClue(clue, index, iCursorDirection);
			}
			iGridView.setCursor(pos.x, pos.y, aDirection,false);
			if (iTextView != null) {
				iTextView.setText(clue.toString());
			}
			
		}
	}

	public void clueLongClicked(int aDirection, int aNum, int aPosition) {
		if (aPosition >= 0) {
			Point pos=iCrossword.locateClue(aDirection, aNum);
			iCursorX = pos.x;
			iCursorY = pos.y;
			iCursorDirection = aDirection;
			String hint = iCrossword.getCluePattern(new Point(iCursorX, iCursorY), iCursorDirection);
			wordEntryDialog(iGridView.getCurrentClue(), "", hint);
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
		iGridView.requestFocusFromTouch();
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
		builder.setNegativeButton(R.string.text_cancel,null);
		final AlertDialog alert = builder.create();
		alert.show();
		alert.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View p1) {
					if (alert.getCurrentFocus() != null) {
						InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
						inputMethodManager.hideSoftInputFromWindow(alert.getCurrentFocus().getWindowToken(), 0);
					}
					alert.dismiss();
				}
			});
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
			case MainActivity.REQUEST_CLUE:
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
