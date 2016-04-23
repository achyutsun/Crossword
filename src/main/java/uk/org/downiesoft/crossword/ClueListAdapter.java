package uk.org.downiesoft.crossword;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ClueListAdapter extends ArrayAdapter<Clue>
{
	public static final String TAG=ClueListAdapter.class.getName();
	
	private static class ViewHolder {
		TextView mNumberView;
		TextView mTextView;
	}
	
	private static final int CLUE_LIST_ITEM = R.layout.clue_list_item;
	private final ArrayList<Clue> mItems;

	public ClueListAdapter(Context context, ArrayList<Clue> items)
	{
		super(context, CLUE_LIST_ITEM, items);
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
			li.inflate(CLUE_LIST_ITEM, clueItemView, true);
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
