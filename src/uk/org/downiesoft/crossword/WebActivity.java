package uk.org.downiesoft.crossword;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

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
