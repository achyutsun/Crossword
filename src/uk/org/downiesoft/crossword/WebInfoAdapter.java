package uk.org.downiesoft.crossword;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WebInfoAdapter extends ArrayAdapter<WebInfo>
{
	private int resource;

	public WebInfoAdapter(Context context, int resource, ArrayList<WebInfo> items)
	{
		super(context, resource, items);
		this.resource = resource;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LinearLayout infoItemView;

		WebInfo item = getItem(position);

		if (convertView == null)
		{
			infoItemView = new LinearLayout(getContext());
			String inflater = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater li;
			li = (LayoutInflater) getContext().getSystemService(inflater);
			li.inflate(resource, infoItemView, true);
		} else
		{
			infoItemView = (LinearLayout) convertView;
		}

		TextView idView = (TextView) infoItemView.findViewById(R.id.webId);
		TextView dateView = (TextView) infoItemView.findViewById(R.id.webDate);

		idView.setText(Integer.toString(item.crosswordId()));
		dateView.setText(item.dateString());

		return infoItemView;
	}


}
