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
	private ArrayList<Clue> mItems;

	public ClueListAdapter(Context context, int resource, ArrayList<Clue> items)
	{
		super(context, resource, items);
		this.resource = resource;
		mItems=items;
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
			LayoutInflater li = (LayoutInflater) getContext().getSystemService(inflater);
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

		return clueItemView;
	}

}
