package uk.org.downiesoft.crossword;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BrowserAdapter extends ArrayAdapter<File>
{

	private Context mContext;
	private int mResource;
	private List<File> mItems;
	private WebManager iInfo;

	public BrowserAdapter(Context c, int resource, List<File> items, WebManager info)
	{
		super(c, resource, items);
		mContext=c;
		mResource=resource;
		mItems=items;
		iInfo=info;
	}

	@Override
	public int getCount()
	{
		return mItems.size();
	}

	@Override
	public File getItem(int position)
	{
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LinearLayout browserItemView;
		if (convertView == null)
		{ // if it's not recycled, initialize some attributes
			browserItemView = new LinearLayout(mContext);
			String inflater = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater li;
			li = (LayoutInflater) getContext().getSystemService(inflater);
			li.inflate(mResource, browserItemView, true);
		} else
		{
			browserItemView = (LinearLayout) convertView;
		}

		
		final ImageView imageView=(ImageView)browserItemView.findViewById(R.id.rowImage);
		final TextView textView=(TextView)browserItemView.findViewById(R.id.rowFilename);
		final TextView dateView=(TextView)browserItemView.findViewById(R.id.rowDate);
		
		File item=getItem(position);
		String name=item.getName();
		if (item.isDirectory())
		{
			imageView.setImageResource(R.drawable.folder);
			textView.setText(name.substring(0, name.length()));
		}
		else
		{
			imageView.setImageResource(R.drawable.ic_launcher);
			String base=name.substring(0, name.lastIndexOf('.'));
			textView.setText(base);
			try
			{
				WebInfo info=iInfo.getCrossword(Integer.parseInt(base));
				if (info!=null)
					dateView.setText(info.dateString());
			}
			catch (IllegalArgumentException e)
			{
				dateView.setText("");
			}
		}
		return browserItemView;
	}

}
