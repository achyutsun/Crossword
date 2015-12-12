package uk.org.downiesoft.crossword;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class WebListActivity extends FragmentActivity
{
	public static final String TAG=WebActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web_list_activity);
		Fragment webInfoList = new WebInfoList();
		getSupportFragmentManager().beginTransaction()
			.replace(R.id.webListContainer, webInfoList, WebInfoList.TAG).commit();
	}

}
