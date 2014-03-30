package uk.org.downiesoft.crossword;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
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
	private ArrayList<Clue> mItems;

	public ClueListAdapter(Context context, int resource, ArrayList<Clue> items)
	{
		super(context, resource, items);
		this.resource = resource;
		mItems=items;
		mSelectedColour=context.getResources().getColor(android.R.color.holo_purple);
		mSelectedColour=(mSelectedColour&0xffffff)|0xa0000000; 
	}
	
	@Override
	public int getCount()
	{
		return mItems.size();
	}

	@Override
	public Clue getItem(int position)
	{
		return mItems.get(position);
	}

	@Override
	public int getItemViewType(int position) {
		return BaseAdapter.IGNORE_ITEM_VIEW_TYPE;
	}
	
	@Override
	public long getItemId(int position)
	{
		return 0;
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
		if (aClue!=null) {
			mSelectedItem=getPosition(aClue);
		} else {
			mSelectedItem = -1;
		}
		notifyDataSetChanged();
	}
	
	public void setSelectedClue(int aPosition) {
		mSelectedItem=aPosition;
		notifyDataSetChanged();
	}

	public int getSelectedCluePosition() {
		return mSelectedItem;
	}
}
