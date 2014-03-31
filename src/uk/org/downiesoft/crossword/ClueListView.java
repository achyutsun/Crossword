package uk.org.downiesoft.crossword;
import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class ClueListView extends ListView
{
	
	public static final String TAG=ClueListView.class.getName();
	
	private ClueListAdapter iAdapter;
	private Context mContext;
	
	public ClueListView(Context aContext) {
		super(aContext);
		mContext = aContext;
	}
	
	public ClueListView(Context aContext, AttributeSet aAttr) {
		super(aContext, aAttr);
		mContext = aContext;
	}

	public ClueListView(Context aContext, AttributeSet aAttr, int aDefStyle) {
		super(aContext, aAttr, aDefStyle);
		mContext = aContext;
	}

	public void setClueList(ArrayList<Clue> aClueList, int aSelected) {
		if (aClueList != null) {
			iAdapter = new ClueListAdapter(mContext,R.layout.clue_list_item,aClueList, aSelected);
			setAdapter(iAdapter);
		}		
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		try {
			return super.onTouchEvent(ev);
		} catch (Exception e) {
			return true;
		}
	}

	public ClueListAdapter getAdapter() {
		return iAdapter;
	}

}
