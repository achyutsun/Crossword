package uk.org.downiesoft.crossword;
import android.widget.ListView;
import java.util.ArrayList;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.drawable.StateListDrawable;

public class ClueListView extends ListView
{
	
	public static final String TAG=ClueListView.class.getName();
	
	private ArrayList<Clue> mClueList;
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

	public void setClueList(ArrayList<Clue> aClueList) {
		mClueList = aClueList;
		if (aClueList != null) {
			iAdapter = new ClueListAdapter(mContext,R.layout.clue_list_item,aClueList);
			setAdapter(iAdapter);
		}		
	}
	
	public ClueListAdapter getAdapter() {
		return iAdapter;
	}

}
