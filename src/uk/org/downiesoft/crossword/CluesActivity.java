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

public class CluesActivity extends FragmentActivity implements ClueListFragment.ClueListListener
{

	public static final String TAG=CluesActivity.class.getName();
	public static final int REQUEST_CLUE=4096;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.clues_activity);
		Intent intent=getIntent();
		Fragment cluesFragment=new CluesFragment();
		Bundle args=intent.getExtras().getBundle("currentClue");
		cluesFragment.setArguments(args);
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.cluesContainer, cluesFragment, CluesFragment.TAG).commit();
	}
	
	@Override
	public void onClueClicked(int aDirection, int aNum, int aPosition) {
		Log.d(TAG, String.format("onClueClicked(%d,%d)",aDirection, aNum));
		Intent intent=new Intent();
		Bundle extras=new Bundle();
		extras.putInt("direction", aDirection);
		extras.putInt("number",aNum);
		extras.putInt("position",aPosition);
		intent.putExtras(extras);
		setResult(REQUEST_CLUE,intent);
		finish();
	}

}
