package uk.org.downiesoft.crossword;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import uk.org.downiesoft.crossword.BluetoothManager.BluetoothListener;
import uk.org.downiesoft.crossword.BrowserDialog.BrowserDialogListener;
import uk.org.downiesoft.crossword.GridFragment.GridFragmentListener;
import uk.org.downiesoft.crossword.WebViewFragment.WebViewFragmentListener;
import uk.org.downiesoft.spell.SpellActivity;
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
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements BrowserDialogListener, WebViewFragmentListener,
		BluetoothListener,GridFragmentListener
{

	public static final String TAG = "uk.org.downiesoft.crossword.MainActivity";
	private static final String CROSSWORD_DIR = "Crossword";

	private CrosswordModel iCrossword;
	private GridFragment iGridFragment;
	private SharedPreferences iSettings;
	private WebManager iWebManager;
	private Handler iTitleHandler;
	private Intent iServerIntent;
	private BluetoothManager iBluetoothManager;
	private boolean iCrosswordReceived = false;
	private boolean iBTServer = false;
	private boolean iBTEnabled = false;

	private class TitleSetter implements Runnable
	{

		@Override
		public void run()
		{
			iTitleHandler.removeCallbacks(this);
			setCrosswordTitle();
		}
	};

	private class ImportPuzzleTask extends AsyncTask<String, Void, Void>
	{
		ProgressDialog mDialog;

		@Override
		protected void onPreExecute()
		{
			mDialog = new ProgressDialog(MainActivity.this);
			mDialog.setMessage("Working...");
			mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mDialog.setCancelable(false);
			mDialog.show();
		}

		@Override
		protected Void doInBackground(String... html)
		{

			try
			{
				DataInputStream is = new DataInputStream(new ByteArrayInputStream(html[0].getBytes()));
				MainActivity.this.iCrossword.importCrossword(is);
				is.close();
			}
			catch (Exception e)
			{
				Log.d("uk.org.downiesoft.crossword.ImportPuzzle", e.toString() + ":" + e.getMessage());
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			mDialog.dismiss();
			iGridFragment.setCrossword(iCrossword);
			iGridFragment.resetClue();
			iTitleHandler.post(new TitleSetter());
			FragmentManager fm = getSupportFragmentManager();
			fm.popBackStack("main_activity", FragmentManager.POP_BACK_STACK_INCLUSIVE);
			Fragment frag = fm.findFragmentByTag("web_info_list");
			if (frag != null)
				fm.beginTransaction().remove(frag).commit();
			frag = fm.findFragmentByTag("web_view");
			if (frag != null)
				fm.beginTransaction().remove(frag).commit();
		}
	}

	private class ImportSolutionTask extends AsyncTask<String, Void, Boolean>
	{
		ProgressDialog mDialog;

		@Override
		protected void onPreExecute()
		{
			mDialog = new ProgressDialog(MainActivity.this);
			mDialog.setMessage("Working...");
			mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mDialog.setCancelable(false);
			mDialog.show();
		}

		@Override
		protected Boolean doInBackground(String... html)
		{
			Log.d("uk.org.downiesoft.crossword.ImportSolutionTask", html[0]);
			boolean retval = false;
			try
			{
				DataInputStream is = new DataInputStream(new ByteArrayInputStream(html[0].getBytes()));
				CrosswordModel solution = new CrosswordModel();
				retval = solution.importCrossword(is);
				retval = retval && iCrossword.mapSolution(solution);
				is.close();
			}
			catch (Exception e)
			{
				Log.d("uk.org.downiesoft.crossword.ImportSolution", e.toString() + ":" + e.getMessage());
				e.printStackTrace();
			}
			return Boolean.valueOf(retval);
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			mDialog.dismiss();
			if (!result)
			{
				Toast.makeText(MainActivity.this, R.string.text_invalid_solution, Toast.LENGTH_SHORT).show();
			}
			FragmentManager fm = getSupportFragmentManager();
			fm.popBackStack("main_activity", FragmentManager.POP_BACK_STACK_INCLUSIVE);
			Fragment frag = fm.findFragmentByTag("web_info_list");
			if (frag != null)
				fm.beginTransaction().remove(frag).commit();
			frag = fm.findFragmentByTag("web_view");
			if (frag != null)
				fm.beginTransaction().remove(frag).commit();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_crossword);
		iCrossword = CrosswordModel.getInstance();
		iSettings = getSharedPreferences("CROSSWORD_SETTINGS", Context.MODE_PRIVATE);
		iTitleHandler = new Handler();
		restore();
		if (iGridFragment == null)
			iGridFragment = new GridFragment();
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.fragmentContainer, iGridFragment, "GridFragment").commit();
	}

	@Override
	public void onStart()
	{
		super.onStart();
		// if (iBluetoothManager!=null && iBluetoothManager.isActive())
		// iBluetoothManager.setup();

	}

	@Override
	public void onResume()
	{
		super.onResume();
		restore();
		if (iCrossword.isValid())
			setCrosswordTitle();
		// if (iBluetoothManager!=null && iBluetoothManager.isActive())
		// iBluetoothManager.listen();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		store();
	}

	@Override
	public void onDestroy()
	{
		super.onStop();
		if (iBluetoothManager != null)
			iBluetoothManager.stop(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.crossword, menu);
		if (iBluetoothManager != null && iBluetoothManager.isActive())
		{
			menu.removeItem(R.id.action_bluetooth_on);
		}
		else
		{
			menu.removeItem(R.id.action_bluetooth_off);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		boolean on = false;
		boolean off = false;
		for (int i = 0; i < menu.size(); i++)
		{
			off = off || menu.getItem(i).getItemId() == R.id.action_bluetooth_off;
			on = on || menu.getItem(i).getItemId() == R.id.action_bluetooth_on;
		}
		if (iBluetoothManager != null && iBluetoothManager.isActive())
		{
			if (!off)
				menu.add(Menu.NONE, R.id.action_bluetooth_off, 100, R.string.action_bluetooth_off);
			if (on)
				menu.removeItem(R.id.action_bluetooth_on);
		}
		else
		{
			if (!on)
				menu.add(Menu.NONE, R.id.action_bluetooth_on, 101, R.string.action_bluetooth_on);
			if (off)
				menu.removeItem(R.id.action_bluetooth_off);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		BrowserDialog browserDialog;
		switch (item.getItemId())
		{
			case R.id.action_open:
			{
				String path = Environment.getExternalStorageDirectory().toString() + File.separatorChar + CROSSWORD_DIR;
				File currentFile = new File(path);
				if (!currentFile.exists())
					currentFile.mkdir();
				browserDialog = BrowserDialog.getInstance(path, "*.*");
				FragmentManager fm = getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.replace(R.id.fragmentContainer, browserDialog, "browser_dialog");
				ft.addToBackStack("browser_dialog");
				ft.commit();
				return true;
			}
			case R.id.action_save:
			{
				if (iCrossword.isValid() && iCrossword.saveCrossword(this))
				{
					String format = this.getString(R.string.save_successful);
					Toast.makeText(this, String.format(format, iCrossword.crosswordId()), Toast.LENGTH_SHORT).show();
				}
				return true;
			}
			case R.id.action_web:
			{
				FragmentManager fm = getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.replace(R.id.fragmentContainer, new WebViewFragment(), "web_view");
				ft.addToBackStack("main_activity");
				ft.commit();
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
					public void onClick(DialogInterface dialog, int which)
					{
						iCrossword.clearAnswers();
						iGridFragment.setCrossword(iCrossword);
						dialog.dismiss();
					}
				});
				builder.setNegativeButton(R.string.text_cancel, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
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

	void setCrosswordTitle()
	{
		String title = getString(R.string.crossword_app_name);
		String subtitle = null;
		if (iCrossword.isValid() && iWebManager != null)
		{
			title = Integer.toString(iCrossword.crosswordId());
			WebInfo info = iWebManager.getCrossword(iCrossword.crosswordId());
			if (info != null)
			{
				subtitle = info.dateString();
			}
		}
		getActionBar().setTitle(title);
		if (subtitle!=null) {
			getActionBar().setSubtitle(subtitle);
		}
	}

	@Override
	public void onFileSelected(final File aFile)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		builder.setTitle(R.string.action_open);
		builder.setMessage(R.string.text_query_continue);
		// builder.setInverseBackgroundForced(true);
		builder.setPositiveButton(R.string.text_ok, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				FragmentManager fm = getSupportFragmentManager();
				fm.popBackStack("browser_dialog", FragmentManager.POP_BACK_STACK_INCLUSIVE);
				Fragment frag = fm.findFragmentByTag("browser_dialog");
				if (frag != null)
					fm.beginTransaction().remove(frag).commit();
				String currentFile = aFile.toString();
				CrosswordModel newCrossword = CrosswordModel.openCrossword(aFile);
				if (newCrossword == null)
				{
					Toast.makeText(MainActivity.this, R.string.open_failed, Toast.LENGTH_LONG).show();
				}
				else
				{
					iCrossword = newCrossword;
					currentFile = aFile.toString();
					iGridFragment.setCrossword(iCrossword);
					iGridFragment.resetClue();
					setCrosswordTitle();
					Editor editor = iSettings.edit();
					editor.putString("current_file", currentFile);
					editor.commit();
				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.text_cancel, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();

	}

	public void store()
	{
		if (iCrossword.isValid())
		{
			try
			{
				DataOutputStream os = new DataOutputStream(openFileOutput("current.xwd", Context.MODE_PRIVATE));
				iCrossword.externalize(os);
				os.flush();
				os.close();
				iGridFragment.setCrossword(iCrossword);
			}
			catch (IOException e)
			{

			}
		}
		try
		{
			DataOutputStream os = new DataOutputStream(openFileOutput("webinfo", Context.MODE_PRIVATE));
			if (iWebManager != null)
				iWebManager.externalize(os);
			os.flush();
			os.close();
		}
		catch (IOException e)
		{

		}

	}

	public void restore()
	{
		try
		{
			iWebManager = new WebManager();
			DataInputStream is = new DataInputStream(openFileInput("webinfo"));
			iWebManager.internalize(is);
			is.close();
		}
		catch (IOException e)
		{
		}

		try
		{
			DataInputStream is = new DataInputStream(openFileInput("current.xwd"));
			iCrossword.internalize(is);
			is.close();
			setCrosswordTitle();
		}
		catch (IOException e)
		{

		}
	}

	public CrosswordModel getCrossword()
	{
		return iCrossword;
	}

	public WebManager getWebManager()
	{
		return iWebManager;
	}

	@Override
	public void importPuzzle(String html)
	{
		new ImportPuzzleTask().execute(html);
	}

	@Override
	public void importSolution(String html)
	{
		new ImportSolutionTask().execute(html);
	}

	private void startBluetooth()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		builder.setTitle(R.string.action_bluetooth_on);
		builder.setMessage(R.string.text_select_connection);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton(R.string.text_connect, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				iCrosswordReceived = false;
				iBTServer = false;
				iBluetoothManager = new BluetoothManager(MainActivity.this, MainActivity.this);
				iBluetoothManager.setup();
				if (!iBluetoothManager.bluetoothEnabled())
				{
					iBTEnabled = false;
					Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableIntent, BluetoothManager.REQUEST_ENABLE_BT);
				}
				else
				{
					iBTEnabled = true;
					iServerIntent = new Intent(MainActivity.this, DeviceListActivity.class);
					startActivityForResult(iServerIntent, BluetoothManager.REQUEST_CONNECT_DEVICE_SECURE);

				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.text_listen, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				iCrosswordReceived = false;
				iBTServer = true;
				iBluetoothManager = new BluetoothManager(MainActivity.this, MainActivity.this);
				iBluetoothManager.setup();
				if (!iBluetoothManager.bluetoothEnabled())
				{
					Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableIntent, BluetoothManager.REQUEST_ENABLE_BT);
				}
				else
				{
					iBluetoothManager.listen();
				}
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void stopBluetooth()
	{
		if (iBluetoothManager != null && iBluetoothManager.isActive())
			if (!iBTEnabled)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setCancelable(true);
				builder.setTitle(R.string.action_bluetooth_off);
				builder.setMessage(R.string.text_disable_bt);
				builder.setInverseBackgroundForced(true);
				builder.setPositiveButton(R.string.text_yes, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						iBluetoothManager.stop(true);
						dialog.dismiss();
					}
				});
				builder.setNegativeButton(R.string.text_no, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						iBluetoothManager.stop(false);
						dialog.dismiss();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
			else
				iBluetoothManager.stop(false);
	}

	@Override
	public void onCrosswordReceived(CrosswordModel aCrossword)
	{
		if (iCrossword.crosswordId() == aCrossword.crosswordId())
		{
			for (int col = 0; col < CrosswordModel.GRID_SIZE; col++)
				for (int row = 0; row < CrosswordModel.GRID_SIZE; row++)
					if (!iCrossword.isBlank(col, row))
						if (iCrossword.value(col, row) == CrosswordModel.SQUARE_NONBLANK)
							iCrossword.setValue(col, row, aCrossword.value(col, row));
			if (!iCrosswordReceived)
				iBluetoothManager.sendCrossword(iCrossword);
			iCrosswordReceived = true;
			if (iGridFragment != null)
				iGridFragment.update();
		}
		else
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setCancelable(true);
			builder.setTitle(R.string.text_synch_error);
			builder.setMessage(R.string.text_mismatch);
			builder.setInverseBackgroundForced(true);
			builder.setNegativeButton(R.string.text_ok, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case BluetoothManager.REQUEST_CONNECT_DEVICE_SECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)
				{
					iBluetoothManager.connectDevice(data);
				}
				break;
			case BluetoothManager.REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK)
				{
					// Bluetooth is now enabled, so set up a chat session
					if (!iBTServer)
					{
						iServerIntent = new Intent(this, DeviceListActivity.class);
						startActivityForResult(iServerIntent, BluetoothManager.REQUEST_CONNECT_DEVICE_SECURE);
					}
					else
					{
						iBluetoothManager.listen();
					}
				}
				else
				{
					// User did not enable Bluetooth or an error occurred
					Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
					finish();
				}
				break;
			case SpellActivity.REQUEST_CROSSWORD:
				if (resultCode == Activity.RESULT_OK)
				{
					String word = data.getExtras().getString("uk.org.downiesoft.spell.wordResult", "");
					if (word.length() > 0 && iGridFragment != null)
					{
						iGridFragment.enterWord(word.toUpperCase());
						store();
						synch();
					}
				}
				break;
			case CluesActivity.REQUEST_CLUE:
				if (data!=null && data.getExtras()!=null) {
					int direction = data.getExtras().getInt("direction");
					int number = data.getExtras().getInt("number");
					int position = data.getExtras().getInt("position",-1);
					iGridFragment.clueClicked(direction, number, position);
				}
				break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
		{
			if (iGridFragment != null)
			{
				if (iGridFragment.offerBackKeyEvent())
					return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	public void synch()
	{
		if (iBluetoothManager != null && iBluetoothManager.isActive())
		{
			iBluetoothManager.sendCrossword(iCrossword);
		}
	}
	
	@Override
	public void onClueButtonPressed(Bundle aCurrentClue) {
		Intent intent = new Intent(this, CluesActivity.class);
		intent.putExtra("currentClue",aCurrentClue);
		startActivityForResult(intent, CluesActivity.REQUEST_CLUE);
	}

}
