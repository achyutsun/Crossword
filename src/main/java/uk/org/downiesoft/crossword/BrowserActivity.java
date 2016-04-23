package uk.org.downiesoft.crossword;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class BrowserActivity extends FragmentActivity
{

	public static final String TAG=BrowserActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser_activity);
		Fragment browserFragment=BrowserDialog.getInstance(MainActivity.getCrosswordDirectory(), CrosswordModel.getInstance().getCrosswordId());
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.browserContainer, browserFragment, BrowserDialog.TAG).commit();
	}

}
