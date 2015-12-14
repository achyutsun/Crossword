package uk.org.downiesoft.crossword;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import uk.org.downiesoft.crossword.BluetoothManager.BluetoothListener;
import uk.org.downiesoft.spell.SpellActivity;

public class MainActivity extends FragmentActivity implements BluetoothListener, ClueListFragment.ClueListListener {

	public static final String TAG = MainActivity.class.getName();
	public static final String CROSSWORD_DIR = "Crossword";

	public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	public static final int REQUEST_ENABLE_BT = 2;
	
	public static final int REQUEST_CLUE     = 4096;	
	public static final int REQUEST_BROWSER  = 4097;
	public static final int REQUEST_PUZZLE   = 4098;
	public static final int REQUEST_SOLUTION = 4099;
	
	private static final int sDebug = 0;

	private static File sCrosswordRoot;
	
	private CrosswordModel mCrossword;
	private GridFragment mGridFragment;
	private CluesFragment mCluesFragment;
	private WebManager mWebManager;
	private Intent mServerIntent;
	private BluetoothManager mBluetoothManager;
	private boolean mCrosswordReceived = false;
	private boolean mBTServer = false;
	private boolean mBTEnabled = false;
	private int mClueDirection = -1;
	private int mCluePosition = -1;


	public static void debug(int aLevel, String aTag, String aText) {
		if (aLevel <= sDebug) {
			Log.d(aTag, aText);
		}
	}

	private class ImportPuzzleTask extends AsyncTask<String, Void, CrosswordModel> {
		ProgressDialog mDialog;
		CrosswordModel mCrossword;

		@Override
		protected void onPreExecute() {
			mDialog = new ProgressDialog(MainActivity.this);
			mDialog.setMessage("Working...");
			mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mDialog.setCancelable(false);
			mDialog.show();
		}

		@Override
		protected CrosswordModel doInBackground(String... html) {

			try {
				DataInputStream is = new DataInputStream(new ByteArrayInputStream(html[0].getBytes()));
				CrosswordModel crossword = CrosswordModel.importCrossword(is);
				is.close();
				return crossword;
			} catch (Exception e) {
				MainActivity.debug(1, TAG, e.toString() + ":" + e.getMessage());
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(CrosswordModel aCrossword) {
			mDialog.dismiss();
			if (aCrossword != null) {
				setCrossword(aCrossword);
			}
		}
	}

	private class ImportSolutionTask extends AsyncTask<String, Void, CrosswordModel> {
		ProgressDialog mDialog;

		@Override
		protected void onPreExecute() {
			mDialog = new ProgressDialog(MainActivity.this);
			mDialog.setMessage("Working...");
			mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mDialog.setCancelable(false);
			mDialog.show();
		}

		@Override
		protected CrosswordModel doInBackground(String... html) {
			MainActivity.debug(1, TAG, String.format("ImportSolutionTask: %s", html[0]));
			CrosswordModel solution = null;
			try {
				DataInputStream is = new DataInputStream(new ByteArrayInputStream(html[0].getBytes()));
				solution = CrosswordModel.importCrossword(is);
				is.close();
			} catch (Exception e) {
				MainActivity.debug(1, TAG, e.toString() + ":" + e.getMessage());
				e.printStackTrace();
			}
			return solution;
		}

		@Override
		protected void onPostExecute(CrosswordModel solution) {
			mDialog.dismiss();
			if (solution != null) {
				if (mCrossword.mapSolution(solution)) {
					mGridFragment.update();
				}
			} else {
				Toast.makeText(MainActivity.this, R.string.text_invalid_solution, Toast.LENGTH_SHORT).show();
			}
		}
	}

	public static File getCrosswordDirectory() {
		return sCrosswordRoot;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MainActivity.debug(1, TAG, String.format("onCreate(%s)", mCrossword));
		super.onCreate(savedInstanceState);
		MainActivity.sCrosswordRoot = new File(Environment.getExternalStorageDirectory().toString(),getString(R.string.crossword_app_name));
		if (!MainActivity.sCrosswordRoot.exists()) {
			MainActivity.sCrosswordRoot.mkdir();
		}
		setContentView(R.layout.activity_crossword);
		mCrossword = CrosswordModel.getInstance();
		restore();
		if (mGridFragment == null)
			mGridFragment = new GridFragment();
		if (mCluesFragment == null) {
			mCluesFragment = new CluesFragment();
		}
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.fragmentContainer, mGridFragment, GridFragment.TAG).commit();
		fm.beginTransaction().replace(R.id.cluesContainer, mCluesFragment, CluesFragment.TAG).commit();
	}

	@Override
	public void onStart() {
		MainActivity.debug(1, TAG, String.format("onStart(%s)", mCrossword));
		super.onStart();
	}

	@Override
	public void onResume() {
		MainActivity.debug(1, TAG, String.format("onResume(%s)", mCrossword));
		super.onResume();
		if (mCrossword.isValid())
			setCrosswordTitle();
	}

	@Override
	public void onPause() {
		MainActivity.debug(1, TAG, String.format("onPause(%s)", mCrossword));
		super.onPause();
		if (mCrossword.isValid() && mCrossword.isModified()) {
			if (!mCrossword.saveCrossword(sCrosswordRoot)) {
				Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
			}
		}
		store();
	}

	@Override
	public void onDestroy() {
		super.onStop();
		if (mBluetoothManager != null)
			mBluetoothManager.stop(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.crossword, menu);
		if (mBluetoothManager != null && mBluetoothManager.isActive()) {
			menu.removeItem(R.id.action_bluetooth_on);
		} else {
			menu.removeItem(R.id.action_bluetooth_off);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean on = false;
		boolean off = false;
		for (int i = 0; i < menu.size(); i++) {
			off = off || menu.getItem(i).getItemId() == R.id.action_bluetooth_off;
			on = on || menu.getItem(i).getItemId() == R.id.action_bluetooth_on;
		}
		if (mBluetoothManager != null && mBluetoothManager.isActive()) {
			if (!off)
				menu.add(Menu.NONE, R.id.action_bluetooth_off, 100, R.string.action_bluetooth_off);
			if (on)
				menu.removeItem(R.id.action_bluetooth_on);
		} else {
			if (!on)
				menu.add(Menu.NONE, R.id.action_bluetooth_on, 101, R.string.action_bluetooth_on);
			if (off)
				menu.removeItem(R.id.action_bluetooth_off);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_open:
				{
					Intent intent = new Intent(this, BrowserActivity.class);
					startActivityForResult(intent, REQUEST_BROWSER);
					return true;
				}
			case R.id.action_import:
				{
					Intent intent = new Intent(this, WebActivity.class);
					intent.putExtra(WebViewFragment.ARG_MODE,WebViewFragment.MODE_PUZZLE);
					startActivityForResult(intent, MainActivity.REQUEST_PUZZLE);
					return true;
				}
			case R.id.action_solution:
				{
					Intent intent = new Intent(this, WebActivity.class);
					intent.putExtra(WebViewFragment.ARG_MODE,WebViewFragment.MODE_SOLUTION);
					startActivityForResult(intent, MainActivity.REQUEST_SOLUTION);
					return true;
				}
			case R.id.action_clear:
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setCancelable(true);
					builder.setTitle(R.string.action_clear);
					builder.setMessage(R.string.text_query_continue);
					builder.setPositiveButton(R.string.text_ok, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mCrossword.clearAnswers();
								mGridFragment.setCrossword(mCrossword);
								dialog.dismiss();
							}
						});
					builder.setNegativeButton(R.string.text_cancel, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
					AlertDialog alert = builder.create();
					alert.show();
					return true;
				}
			case R.id.action_bluetooth_on:
				{
					startBluetooth();
					return true;
				}
			case R.id.action_bluetooth_off:
				{
					stopBluetooth();
					return true;
				}
			case R.id.action_spell:
				{
					mServerIntent = new Intent(MainActivity.this, SpellActivity.class);
					mServerIntent.putExtra("uk.org.downiesoft.crossword.wordPattern", mGridFragment.getCluePattern());
					mServerIntent.putExtra("uk.org.downiesoft.crossword.clueText", mGridFragment.getClueText());
					startActivityForResult(mServerIntent, SpellActivity.REQUEST_CROSSWORD);
					return true;
				}
		}
		return false;
	}

	void setCrosswordTitle() {
		String title = getString(R.string.crossword_app_name);
		String subtitle = null;
		if (mBluetoothManager != null && mBluetoothManager.isActive() && mBluetoothManager.getStatus() != null) {
			subtitle = mBluetoothManager.getStatus();			
		} else if (mCrossword.isValid() && mWebManager != null) {
			title = Integer.toString(mCrossword.getCrosswordId());
			WebInfo info = mWebManager.getCrossword(mCrossword.getCrosswordId());
			if (info != null) {
				subtitle = info.dateString();
			}
		}
		getActionBar().setTitle(title);
		getActionBar().setSubtitle(subtitle);
	}

	@Override
	public void setBTStatus(String aSubtitle) {
		if (aSubtitle != null) {
			getActionBar().setSubtitle(aSubtitle);			
		} else {
			setCrosswordTitle();
		}
	}


	@Override
	public void onFileSelected(final File aFile) {
		CrosswordModel newCrossword = CrosswordModel.openCrossword(aFile);
		MainActivity.debug(1, TAG, String.format("onFileSelected(%s):%s", aFile, newCrossword));
		if (newCrossword == null) {
			Toast.makeText(MainActivity.this, R.string.open_failed, Toast.LENGTH_LONG).show();
		} else {
			newCrossword.setModified(false);
			setCrossword(newCrossword);
		}
	}

	public void setCrossword(CrosswordModel aCrossword) {
		mCrossword = aCrossword;
		MainActivity.debug(1,TAG,String.format("setCrossword(%s)",mCrossword.getCrosswordId()));
		CrosswordModel.setInstance(mCrossword);
		mCluesFragment.setCrossword(mCrossword);
		mGridFragment.setCrossword(mCrossword);
		mGridFragment.resetClue();
		setCrosswordTitle();
	}

	public void store() {
		if (mCrossword.isValid()) {
			try {
				DataOutputStream os = new DataOutputStream(openFileOutput("current.xwd", Context.MODE_PRIVATE));
				mCrossword.externalize(os);
				os.flush();
				os.close();
				mGridFragment.setCrossword(mCrossword);
			} catch (IOException e) {

			}
		}
		if (mWebManager != null)
			mWebManager.store(this);
	}

	public void restore() {
		mWebManager = WebManager.getInstance();
		mWebManager.restore(this);

		try {
			DataInputStream is = new DataInputStream(openFileInput("current.xwd"));
			mCrossword.internalize(is);
			is.close();
			setCrosswordTitle();
		} catch (IOException e) {
		}
	}

	public void importPuzzle(String html) {
		new ImportPuzzleTask().execute(html);
	}

	public void importSolution(String html) {
		new ImportSolutionTask().execute(html);
	}

	private void startBluetooth() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		builder.setTitle(R.string.action_bluetooth_on);
		builder.setMessage(R.string.text_select_connection);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton(R.string.text_connect, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mCrosswordReceived = false;
					mBTServer = false;
					mBluetoothManager = BluetoothManager.getInstance(MainActivity.this, MainActivity.this);
					mBluetoothManager.setup();
					if (!mBluetoothManager.bluetoothEnabled()) {
						mBTEnabled = false;
						Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableIntent, MainActivity.REQUEST_ENABLE_BT);
					} else {
						mBTEnabled = true;
						mServerIntent = new Intent(MainActivity.this, DeviceListActivity.class);
						startActivityForResult(mServerIntent, MainActivity.REQUEST_CONNECT_DEVICE_SECURE);

					}
					dialog.dismiss();
				}
			});
		builder.setNegativeButton(R.string.text_listen, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mCrosswordReceived = false;
					mBTServer = true;
					mBluetoothManager = BluetoothManager.getInstance(MainActivity.this, MainActivity.this);
					mBluetoothManager.setup();
					if (!mBluetoothManager.bluetoothEnabled()) {
						Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableIntent, MainActivity.REQUEST_ENABLE_BT);
					} else {
						mBluetoothManager.listen();
					}
					dialog.dismiss();
				}
			});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void stopBluetooth() {
		if (mBluetoothManager != null && mBluetoothManager.isActive())
			if (!mBTEnabled) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setCancelable(true);
				builder.setTitle(R.string.action_bluetooth_off);
				builder.setMessage(R.string.text_disable_bt);
				builder.setInverseBackgroundForced(true);
				builder.setPositiveButton(R.string.text_yes, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mBluetoothManager.stop(true);
							dialog.dismiss();
						}
					});
				builder.setNegativeButton(R.string.text_no, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mBluetoothManager.stop(false);
							dialog.dismiss();
						}
					});
				AlertDialog alert = builder.create();
				alert.show();
			} else
				mBluetoothManager.stop(false);
	}

	@Override
	public void onCrosswordReceived(final CrosswordModel aCrossword) {
		MainActivity.debug(1,TAG, String.format(">onCrosswordReceived(%s)->%s", mCrossword.getCrosswordId(), aCrossword.getCrosswordId()));
		if (mCrossword.isValid()) {
			if (!mCrossword.saveCrossword(sCrosswordRoot)) {
				Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
			}
		}
		if (mCrossword.getCrosswordId() != aCrossword.getCrosswordId()) {
			mCrossword = aCrossword;
			setCrossword(aCrossword);
		} else {
			for (int col = 0; col < CrosswordModel.GRID_SIZE; col++) {
				for (int row = 0; row < CrosswordModel.GRID_SIZE; row++) {
					if (!mCrossword.isBlank(col, row)) {
						if (mCrossword.value(col, row) == CrosswordModel.SQUARE_NONBLANK) {
							mCrossword.setValue(col, row, aCrossword.value(col, row));
						}
					}
				}
			}
		}
		if (!mCrosswordReceived) {
			mBluetoothManager.sendCrossword(mCrossword);
		}
		mCrosswordReceived = true;
		if (mGridFragment != null) {
			mGridFragment.update();
		}
		store();
		MainActivity.debug(1,TAG, String.format("<onCrosswordReceived(%s)", mCrossword.getCrosswordId()));
	}

	@Override
	public void onClueClicked(int aDirection, int aNum, int aPosition) {
		mClueDirection = aDirection;
		mCluePosition = aPosition;
		if (mGridFragment != null) {
			mGridFragment.clueClicked(aDirection, aNum, aPosition);
		}
	}

	@Override
	public void onClueLongClicked(int aDirection, int aNum, int aPosition) {
		mClueDirection = aDirection;
		mCluePosition = aPosition;
		if (mGridFragment != null) {
			mGridFragment.clueLongClicked(aDirection, aNum, aPosition);
		}
	}


	@Override
	public int onClueListCreated(ClueListFragment aClueList, int aDirection) {
		if (mCluesFragment != null) {
			mCluesFragment.setClueList(aDirection, aClueList);
			if (aDirection == mClueDirection) {
				return mCluePosition;
			}
		}
		return -1;
	}



	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case MainActivity.REQUEST_CONNECT_DEVICE_SECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK) {
					mBluetoothManager.connectDevice(data);
				}
				break;
			case MainActivity.REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					// Bluetooth is now enabled, so set up a chat session
					if (!mBTServer) {
						mServerIntent = new Intent(this, DeviceListActivity.class);
						startActivityForResult(mServerIntent, MainActivity.REQUEST_CONNECT_DEVICE_SECURE);
					} else {
						mBluetoothManager.listen();
					}
				} else {
					// User did not enable Bluetooth or an error occurred
					Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
					finish();
				}
				break;
			case SpellActivity.REQUEST_CROSSWORD:
				if (resultCode == Activity.RESULT_OK) {
					String word = data.getExtras().getString("uk.org.downiesoft.spell.wordResult", "");
					if (word.length() > 0 && mGridFragment != null) {
						mGridFragment.enterWord(word.toUpperCase());
						store();
						BluetoothManager.synch(mCrossword);
					}
				}
				break;
			case MainActivity.REQUEST_PUZZLE:
				if (resultCode == Activity.RESULT_OK) {
					String html = data.getExtras().getString("html", "");
					importPuzzle(html);
				}
				break;
			case MainActivity.REQUEST_SOLUTION:
				if (resultCode == Activity.RESULT_OK) {
					String html = data.getExtras().getString("html", "");
					importSolution(html);
				}
				break;
			case MainActivity.REQUEST_BROWSER:
				if (resultCode == Activity.RESULT_OK) {
					File file = new File(sCrosswordRoot,String.format("%s.xwd",data.getExtras().getInt("file")));
					onFileSelected(file);
				}
				break;				
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (mGridFragment != null) {
				if (mGridFragment.offerBackKeyEvent())
					return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

}
