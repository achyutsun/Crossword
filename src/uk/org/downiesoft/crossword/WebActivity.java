package uk.org.downiesoft.crossword;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.ListView;

public class WebActivity extends FragmentActivity
{

	public static final String TAG=WebActivity.class.getName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web_activity);
		Fragment webFragment=new WebViewFragment();
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.webContainer, webFragment, WebViewFragment.TAG).commit();
	}

}
