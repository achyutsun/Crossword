package uk.org.downiesoft.crossword;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import java.io.File;

public class BrowserActivity extends FragmentActivity
{

	public static final String TAG=BrowserActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser_activity);
		File path = new File(Environment.getExternalStorageDirectory().toString(),MainActivity.CROSSWORD_DIR);
		if (!path.exists())
			path.mkdir();
		Fragment browserFragment=BrowserDialog.getInstance(path, "*.*");
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.browserContainer, browserFragment, BrowserDialog.TAG).commit();
	}

}
