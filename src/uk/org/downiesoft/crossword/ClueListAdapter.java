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
	
	private static class ViewHolder {
		TextView mNumberView;
		TextView mTextView;
	}
	
	private int resource;
	private int mSelectedItem=-1;
	private int mSelectedColour;
	private ArrayList<Clue> mItems;

	public ClueListAdapter(Context context, int resource, ArrayList<Clue> items, int selected)
	{
		super(context, resource, items);
		this.resource = resource;
		mItems=items;
		mSelectedColour=context.getResources().getColor(android.R.color.holo_purple);
		mSelectedColour=(mSelectedColour&0xffffff)|0xa0000000;
		mSelectedItem=selected;
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
	public long getItemId(int position)
	{
		Clue clue=mItems.get(position);
		return clue.iType*256+clue.iNumber;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LinearLayout clueItemView;
		ViewHolder holder;
		Clue item = getItem(position);

		String clueNumber = Integer.toString(item.iNumber);

		if (convertView == null)
		{
			clueItemView = new LinearLayout(getContext());
			String inflater = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater li;
			li = (LayoutInflater) getContext().getSystemService(inflater);
			li.inflate(resource, clueItemView, true);
			holder = new ViewHolder();
			holder.mNumberView = (TextView) clueItemView.findViewById(R.id.rowNumber);
			holder.mTextView = (TextView) clueItemView.findViewById(R.id.rowText);
			clueItemView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
			clueItemView = (LinearLayout) convertView;
		}


		holder.mNumberView.setText(clueNumber);
		holder.mTextView.setText(item.iText);
		clueItemView.setBackgroundColor(position==mSelectedItem ?mSelectedColour:0);

		return clueItemView;
	}

	public void setSelectedClue(int aPosition) {
		Log.d(TAG,String.format("setSelectedClue(%d)",aPosition));
		mSelectedItem=aPosition;
		notifyDataSetChanged();
	}

	public int getSelectedCluePosition() {
		return mSelectedItem;
	}
}
