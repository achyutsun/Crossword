package uk.org.downiesoft.crossword;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
	public static final int REQUEST_BROWSER = 4098;
	public static final int REQUEST_CLUE=4096;	
	public static final int REQUEST_WEB = 4097;

	private static final int sDebug = 1;

	private CrosswordModel iCrossword;
	private GridFragment iGridFragment;
	private CluesFragment iCluesFragment;
	private SharedPreferences iSettings;
	private WebManager iWebManager;
	private Handler iTitleHandler;
	private Intent iServerIntent;
	private BluetoothManager iBluetoothManager;
	private boolean iCrosswordReceived = false;
	private boolean iBTServer = false;
	private boolean iBTEnabled = false;
	private int mClueDirection = -1;
	private int mCluePosition = -1;


	public static void debug(int aLevel, String aTag, String aText) {
		if (aLevel <= sDebug) {
			Log.d(aTag, aText);
		}
	}
	private class TitleSetter implements Runnable {

		@Override
		public void run() {
			iTitleHandler.removeCallbacks(this);
			setCrosswordTitle();
		}
	};

	private class ImportPuzzleTask extends AsyncTask<String, Void, Void> {
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
		protected Void doInBackground(String... html) {

			try {
				DataInputStream is = new DataInputStream(new ByteArrayInputStream(html[0].getBytes()));
				MainActivity.this.iCrossword.importCrossword(is);
				is.close();
			} catch (Exception e) {
				MainActivity.debug(1, TAG, e.toString() + ":" + e.getMessage());
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mDialog.dismiss();
			iGridFragment.setCrossword(iCrossword);
			iGridFragment.resetClue();
			iTitleHandler.post(new TitleSetter());
		}
	}

	private class ImportSolutionTask extends AsyncTask<String, Void, Boolean> {
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
		protected Boolean doInBackground(String... html) {
			MainActivity.debug(1, TAG, String.format("ImportSolutionTask: %s", html[0]));
			boolean retval = false;
			try {
				DataInputStream is = new DataInputStream(new ByteArrayInputStream(html[0].getBytes()));
				CrosswordModel solution = new CrosswordModel();
				retval = solution.importCrossword(is);
				retval = retval && iCrossword.mapSolution(solution);
				is.close();
			} catch (Exception e) {
				MainActivity.debug(1, TAG, e.toString() + ":" + e.getMessage());
				e.printStackTrace();
			}
			return Boolean.valueOf(retval);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			mDialog.dismiss();
			if (!result) {
				Toast.makeText(MainActivity.this, R.string.text_invalid_solution, Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MainActivity.debug(1, TAG, String.format("onCreate(%s)", iCrossword));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_crossword);
		iCrossword = CrosswordModel.getInstance();
		iSettings = getSharedPreferences("CROSSWORD_SETTINGS", Context.MODE_PRIVATE);
		iTitleHandler = new Handler();
		restore();
		if (iGridFragment == null)
			iGridFragment = new GridFragment();
		if (iCluesFragment == null) {
			iCluesFragment = new CluesFragment();
		}
		restore();
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.fragmentContainer, iGridFragment, GridFragment.TAG).commit();
		fm.beginTransaction().replace(R.id.cluesContainer, iCluesFragment, CluesFragment.TAG).commit();
	}

	@Override
	public void onStart() {
		MainActivity.debug(1, TAG, String.format("onStart(%s)", iCrossword));
		super.onStart();
		// if (iBluetoothManager!=null && iBluetoothManager.isActive())
		// iBluetoothManager.setup();

	}

	@Override
	public void onResume() {
		MainActivity.debug(1, TAG, String.format("onResume(%s)", iCrossword));
		super.onResume();
		if (iCrossword.isValid())
			setCrosswordTitle();
		// if (iBluetoothManager!=null && iBluetoothManager.isActive())
		// iBluetoothManager.listen();
	}

	@Override
	public void onPause() {
		MainActivity.debug(1, TAG, String.format("onPause(%s)", iCrossword));
		super.onPause();
		if (iCrossword.isValid()) {
			iCrossword.saveCrossword(this);
		}
		store();
	}

	@Override
	public void onDestroy() {
		super.onStop();
		if (iBluetoothManager != null)
			iBluetoothManager.stop(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.crossword, menu);
		if (iBluetoothManager != null && iBluetoothManager.isActive()) {
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
		if (iBluetoothManager != null && iBluetoothManager.isActive()) {
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
			case R.id.action_web:
				{
					Intent intent = new Intent(this, WebActivity.class);
					startActivityForResult(intent, MainActivity.REQUEST_WEB);
					return true;
				}
			case R.id.action_clear:
				{

					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setCancelable(true);
					builder.setTitle(R.string.action_clear);
					builder.setMessage(R.string.text_query_continue);
					// builder.setInverseBackgroundForced(true);
					builder.setPositiveButton(R.string.text_ok, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which) {
								iCrossword.clearAnswers();
								iGridFragment.setCrossword(iCrossword);
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
					iServerIntent = new Intent(MainActivity.this, SpellActivity.class);
					iServerIntent.putExtra("uk.org.downiesoft.crossword.wordPattern", iGridFragment.getCluePattern());
					iServerIntent.putExtra("uk.org.downiesoft.crossword.clueText", iGridFragment.getClueText());
					startActivityForResult(iServerIntent, SpellActivity.REQUEST_CROSSWORD);
					return true;
				}
		}
		return false;
	}

	void setCrosswordTitle() {
		String title = getString(R.string.crossword_app_name);
		String subtitle = null;
		if (iCrossword.isValid() && iWebManager != null) {
			title = Integer.toString(iCrossword.crosswordId());
			WebInfo info = iWebManager.getCrossword(iCrossword.crosswordId());
			if (info != null) {
				subtitle = info.dateString();
			}
		}
		getActionBar().setTitle(title);
		getActionBar().setSubtitle(subtitle);
	}

	@Override
	public void onFileSelected(final File aFile) {
		CrosswordModel newCrossword = CrosswordModel.openCrossword(aFile);
		MainActivity.debug(1, TAG, String.format("onFileSelected(%s):%s", aFile, newCrossword));
		if (newCrossword == null) {
			Toast.makeText(MainActivity.this, R.string.open_failed, Toast.LENGTH_LONG).show();
		} else {
			setCrossword(newCrossword);
			Editor editor = iSettings.edit();
			editor.putString("current_file", aFile.toString());
			editor.commit();
		}
	}

	public void setCrossword(CrosswordModel aCrossword) {
		iCrossword = aCrossword;
		MainActivity.debug(1,TAG,String.format("setCrossword(%s)",iCrossword.crosswordId()));
		CrosswordModel.setInstance(iCrossword);
		iCluesFragment.setCrossword(iCrossword);
		iGridFragment.setCrossword(iCrossword);
		iGridFragment.resetClue();
		setCrosswordTitle();
	}

	public void store() {
		if (iCrossword.isValid()) {
			try {
				DataOutputStream os = new DataOutputStream(openFileOutput("current.xwd", Context.MODE_PRIVATE));
				iCrossword.externalize(os);
				os.flush();
				os.close();
				iGridFragment.setCrossword(iCrossword);
			} catch (IOException e) {

			}
		}
		if (iWebManager != null)
			iWebManager.store(this);
	}

	public void restore() {
		iWebManager = WebManager.getInstance();
		iWebManager.restore(this);

		try {
			DataInputStream is = new DataInputStream(openFileInput("current.xwd"));
			iCrossword.internalize(is);
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
					iCrosswordReceived = false;
					iBTServer = false;
					iBluetoothManager = BluetoothManager.getInstance(MainActivity.this, MainActivity.this);
					iBluetoothManager.setup();
					if (!iBluetoothManager.bluetoothEnabled()) {
						iBTEnabled = false;
						Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableIntent, MainActivity.REQUEST_ENABLE_BT);
					} else {
						iBTEnabled = true;
						iServerIntent = new Intent(MainActivity.this, DeviceListActivity.class);
						startActivityForResult(iServerIntent, MainActivity.REQUEST_CONNECT_DEVICE_SECURE);

					}
					dialog.dismiss();
				}
			});
		builder.setNegativeButton(R.string.text_listen, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which) {
					iCrosswordReceived = false;
					iBTServer = true;
					iBluetoothManager = BluetoothManager.getInstance(MainActivity.this, MainActivity.this);
					iBluetoothManager.setup();
					if (!iBluetoothManager.bluetoothEnabled()) {
						Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableIntent, MainActivity.REQUEST_ENABLE_BT);
					} else {
						iBluetoothManager.listen();
					}
					dialog.dismiss();
				}
			});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void stopBluetooth() {
		if (iBluetoothManager != null && iBluetoothManager.isActive())
			if (!iBTEnabled) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setCancelable(true);
				builder.setTitle(R.string.action_bluetooth_off);
				builder.setMessage(R.string.text_disable_bt);
				builder.setInverseBackgroundForced(true);
				builder.setPositiveButton(R.string.text_yes, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which) {
							iBluetoothManager.stop(true);
							dialog.dismiss();
						}
					});
				builder.setNegativeButton(R.string.text_no, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which) {
							iBluetoothManager.stop(false);
							dialog.dismiss();
						}
					});
				AlertDialog alert = builder.create();
				alert.show();
			} else
				iBluetoothManager.stop(false);
	}

	@Override
	public void onCrosswordReceived(final CrosswordModel aCrossword) {
		MainActivity.debug(1,TAG, String.format(">onCrosswordReceived(%s)->%s", iCrossword.crosswordId(), aCrossword.crosswordId()));
		if (iCrossword.isValid()) {
			iCrossword.saveCrossword(this);
		}
		if (iCrossword.crosswordId() != aCrossword.crosswordId()) {
			iCrossword = aCrossword;
			setCrossword(aCrossword);
		} else {
			for (int col = 0; col < CrosswordModel.GRID_SIZE; col++) {
				for (int row = 0; row < CrosswordModel.GRID_SIZE; row++) {
					if (!iCrossword.isBlank(col, row)) {
						if (iCrossword.value(col, row) == CrosswordModel.SQUARE_NONBLANK) {
							iCrossword.setValue(col, row, aCrossword.value(col, row));
						}
					}
				}
			}
		}
		if (!iCrosswordReceived) {
			iBluetoothManager.sendCrossword(iCrossword);
		}
		iCrosswordReceived = true;
		if (iGridFragment != null) {
			iGridFragment.update();
		}
		store();
		MainActivity.debug(1,TAG, String.format("<onCrosswordReceived(%s)", iCrossword.crosswordId()));
		iCrossword.dumpGrid("debug");
	}

	@Override
	public void onClueClicked(int aDirection, int aNum, int aPosition) {
		mClueDirection = aDirection;
		mCluePosition = aPosition;
		if (iGridFragment != null) {
			iGridFragment.clueClicked(aDirection, aNum, aPosition);
		}
	}

	@Override
	public void onClueLongClicked(int aDirection, int aNum, int aPosition) {
		mClueDirection = aDirection;
		mCluePosition = aPosition;
		if (iGridFragment != null) {
			iGridFragment.clueLongClicked(aDirection, aNum, aPosition);
		}
	}


	@Override
	public int onClueListCreated(ClueListFragment aClueList, int aDirection) {
		if (iCluesFragment != null) {
			iCluesFragment.setClueList(aDirection, aClueList);
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
					iBluetoothManager.connectDevice(data);
				}
				break;
			case MainActivity.REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					// Bluetooth is now enabled, so set up a chat session
					if (!iBTServer) {
						iServerIntent = new Intent(this, DeviceListActivity.class);
						startActivityForResult(iServerIntent, MainActivity.REQUEST_CONNECT_DEVICE_SECURE);
					} else {
						iBluetoothManager.listen();
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
					if (word.length() > 0 && iGridFragment != null) {
						iGridFragment.enterWord(word.toUpperCase());
						store();
						BluetoothManager.synch(iCrossword);
					}
				}
				break;
			case MainActivity.REQUEST_WEB:
				if (resultCode == Activity.RESULT_OK) {
					int mode = data.getExtras().getInt("import", 0);
					String html = data.getExtras().getString("html", "");
					switch (mode) {
						case WebViewFragment.IMPORT_PUZZLE:
							importPuzzle(html);
							break;
						case WebViewFragment.IMPORT_SOLUTION:
							importSolution(html);
							break;
					}
				}
				break;
			case MainActivity.REQUEST_BROWSER:
				if (resultCode == Activity.RESULT_OK) {
					File file = new File(data.getExtras().getString("file", ""));
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
			if (iGridFragment != null) {
				if (iGridFragment.offerBackKeyEvent())
					return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

}
