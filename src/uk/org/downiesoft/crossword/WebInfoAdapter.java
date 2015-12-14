package uk.org.downiesoft.crossword;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class WebInfoAdapter extends ArrayAdapter<WebInfo>
{
	
	public static final String TAG = WebInfoAdapter.class.getName();
	
	private int mResource;
	private int mColourAvailable;
	private int mColourOnDevice;

	public WebInfoAdapter(Context context, int resource, ArrayList<WebInfo> items)
	{
		super(context, resource, items);
		this.mResource = resource;
		mColourAvailable = context.getResources().getColor(android.R.color.primary_text_dark);
		mColourOnDevice = context.getResources().getColor(android.R.color.tertiary_text_dark);
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
			li.inflate(mResource, infoItemView, true);
		} else
		{
			infoItemView = (LinearLayout) convertView;
		}

		ImageView imageView = (ImageView) infoItemView.findViewById(R.id.webImage);
		TextView idView = (TextView) infoItemView.findViewById(R.id.webId);
		TextView dateView = (TextView) infoItemView.findViewById(R.id.webDate);

		idView.setText(Integer.toString(item.crosswordId()));
		dateView.setText(item.dateString());
		if (item.isOnDevice()) {
			imageView.setImageResource(R.drawable.ic_launcher);
			idView.setTextColor(mColourOnDevice);
			dateView.setTextColor(mColourOnDevice);
		} else {
			imageView.setImageDrawable(null);
			idView.setTextColor(mColourAvailable);
			dateView.setTextColor(mColourAvailable);
		}
		
		return infoItemView;
	}


}
