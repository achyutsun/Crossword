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
	private int resource;

	public ClueListAdapter(Context context, int resource, ArrayList<Clue> items)
	{
		super(context, resource, items);
		this.resource = resource;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LinearLayout ClueItemView;

		Clue item = getItem(position);

		String clueNumber = Integer.toString(item.iNumber);

		if (convertView == null)
		{
			ClueItemView = new LinearLayout(getContext());
			String inflater = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater li;
			li = (LayoutInflater) getContext().getSystemService(inflater);
			li.inflate(resource, ClueItemView, true);
		} else
		{
			ClueItemView = (LinearLayout) convertView;
		}

		TextView numberView = (TextView) ClueItemView.findViewById(R.id.rowNumber);
		TextView textView = (TextView) ClueItemView.findViewById(R.id.rowText);

		numberView.setText(clueNumber);
		textView.setText(item.iText);

		return ClueItemView;
	}

}
