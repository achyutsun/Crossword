package uk.org.downiesoft.crossword;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;
import android.graphics.Color;

public class ClueListAdapter extends ArrayAdapter<Clue>
{
	public static final String TAG=ClueListAdapter.class.getName();
	
	private int resource;
	private int mSelectedItem=-1;
	private int mSelectedColour;

	public ClueListAdapter(Context context, int resource, ArrayList<Clue> items)
	{
		super(context, resource, items);
		this.resource = resource;
		mSelectedColour=context.getResources().getColor(android.R.color.holo_purple);
		mSelectedColour=(mSelectedColour&0xffffff)|0xa0000000; 
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LinearLayout clueItemView;

		Clue item = getItem(position);

		String clueNumber = Integer.toString(item.iNumber);

		if (convertView == null)
		{
			clueItemView = new LinearLayout(getContext());
			String inflater = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater li;
			li = (LayoutInflater) getContext().getSystemService(inflater);
			li.inflate(resource, clueItemView, true);
		} else
		{
			clueItemView = (LinearLayout) convertView;
		}

		TextView numberView = (TextView) clueItemView.findViewById(R.id.rowNumber);
		TextView textView = (TextView) clueItemView.findViewById(R.id.rowText);

		numberView.setText(clueNumber);
		textView.setText(item.iText);
		clueItemView.setBackgroundColor(position==mSelectedItem ?mSelectedColour:0);

		return clueItemView;
	}

	public void setSelectedClue(Clue aClue) {
		Log.d(TAG,String.format("setSelectedClue(%s)",aClue!=null?aClue.toString():"null"));
		if (aClue!=null) {
			mSelectedItem=getPosition(aClue);
		} else {
			mSelectedItem = -1;
		}
		Log.d(TAG,String.format("setSelectedClue=%d",mSelectedItem));
		notifyDataSetChanged();
	}
	
	public void setSelectedClue(int aPosition) {
		Log.d(TAG,String.format("setSelectedClue(%s)",aPosition));
		mSelectedItem=aPosition;
		notifyDataSetChanged();
	}

	public int getSelectedCluePosition() {
		return mSelectedItem;
	}
}
