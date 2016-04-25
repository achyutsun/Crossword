package uk.org.downiesoft.crossword;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BluetoothManager
{

	public interface BluetoothListener
	{
		void onCrosswordReceived(CrosswordModel aCrossword);
		void setBTStatus(String aSubtitle);
	}

	private static final String TAG = BluetoothManager.class.getName();
	private static final boolean D = true;

	// Message types sent from the BluetoothService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_SYNCH = 6;

	// Key names received from the BluetoothService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	private static BluetoothManager sManagerInstance;
	
	// Layout Views
	private final Activity mActivity;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothService mBTService = null;
	private final BluetoothListener mListener;
	// The Handler that gets information back from the BluetoothService
	private final MessageHandler mHandler = new MessageHandler();
	private boolean mIsServer;
	private String mBTStatus;

	private class MessageHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			CrosswordModel crossword=null;
			if (D)
				MainActivity.debug(1, TAG, "handleMessage: " + msg.what);
			switch (msg.what)
			{
				case MESSAGE_STATE_CHANGE:
					if (D)
						MainActivity.debug(1,TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
					switch (msg.arg1)
					{
						case BluetoothService.STATE_CONNECTED:
							setStatus(mActivity.getString(R.string.title_connected_to, mConnectedDeviceName));
							break;
						case BluetoothService.STATE_CONNECTING:
							mIsServer = false;
							setStatus(R.string.title_connecting);
							break;
						case BluetoothService.STATE_LISTEN:
							mIsServer = true;
							setStatus(R.string.title_not_connected);
							break;
						case BluetoothService.STATE_NONE:
							clearStatus();
							break;
					}
					break;
				case MESSAGE_WRITE:
					if (D)
						MainActivity.debug(1,TAG, String.format("MESSAGE_WRITE: %s bytes",((byte[])msg.obj).length));
					break;
				case MESSAGE_READ:
					byte[] readBuf = (byte[]) msg.obj;
					if (D)
						MainActivity.debug(1,TAG, String.format("MESSAGE_READ: %s bytes",(readBuf.length)));
					crossword=receiveCrossword(readBuf);
					if (mListener != null && crossword!=null)
						mListener.onCrosswordReceived(crossword);
					break;
				case MESSAGE_DEVICE_NAME:
					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
					Toast.makeText(mActivity.getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT)
							.show();
					crossword=CrosswordModel.getInstance();
					if (mIsServer && crossword.isValid()) {
						sendCrossword(crossword);
					}
					break;
				case MESSAGE_TOAST:
					Toast.makeText(mActivity.getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_SYNCH:
					
			}
		}
	};
	
	public static BluetoothManager getInstance(Activity activity, BluetoothListener listener) {
		if (sManagerInstance==null) {
			sManagerInstance = new BluetoothManager(activity,listener);
		}
		return sManagerInstance;
	}

	private BluetoothManager(Activity activity, BluetoothListener listener)
	{
		mActivity = activity;
		mListener = listener;
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	public void setup()
	{
		MainActivity.debug(1, TAG, "setupChat()");
		// Initialize the BluetoothService to perform bluetooth connections
		mBTService = new BluetoothService(mActivity, mHandler);
	}
	
	public void stop(boolean disable)
	{
		clearStatus();
		if (mBTService!=null)
		{
			mBTService.stop(disable);
			mBTService=null;
		}
	}
	
	public void listen()
	{
		if (mBTService!=null)
		{
			mBTService.listen();
		}
	}
	
	public boolean isActive()
	{
		if (mBTService==null)
			return false;
		return mBTService.getState()!=BluetoothService.STATE_NONE;
	}
	
	public void connect(BluetoothDevice device)
	{
		if (mBTService!=null)
		{
			mBTService.connect(device);
		}
	}
	

	public boolean bluetoothEnabled()
	{
		return (mBluetoothAdapter!=null && mBluetoothAdapter.isEnabled());
	}

	public void sendCrossword(CrosswordModel aCrossword)
	{
		// Check that we're actually connected before trying anything
		if (mBTService==null || mBTService.getState() != BluetoothService.STATE_CONNECTED)
		{
			Toast.makeText(mActivity, R.string.not_connected, Toast.LENGTH_SHORT).show();
			return;
		}

		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		try
		{
			aCrossword.saveCrossword(baos);
			byte[] send=baos.toByteArray();
			Log.i(TAG,String.format("sendCrossword(#%s): %s bytes",aCrossword.getCrosswordId(),send.length));
			baos.close();
			// Check that there's actually something to send
			if (send.length>0)
			{
				mBTService.write(send);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private CrosswordModel receiveCrossword(byte[] receive)
	{
		MainActivity.debug(1,TAG, String.format(">receiveCrossword: %s bytes",receive.length));
		ByteArrayInputStream bais=new ByteArrayInputStream(receive);
		CrosswordModel crossword = CrosswordModel.openCrossword(bais);
		MainActivity.debug(1, TAG, String.format("<receiveCrossword: %s", crossword));
		return crossword;
	}
	
	private final void setStatus(int resId)
	{
		mBTStatus = mActivity.getResources().getString(resId);
		setStatus(mBTStatus);
		final ActionBar actionBar = mActivity.getActionBar();
		if (actionBar != null)
			actionBar.setSubtitle(resId);
	}

	private final void setStatus(String subTitle)
	{
		mBTStatus = subTitle;
		if (mListener != null) {
			mListener.setBTStatus(mBTStatus);
		}
	}
	
	public String getStatus() {
		return mBTStatus;
	}

	private final void clearStatus()
	{
		if (mListener != null) {
			mListener.setBTStatus(null);
			mBTStatus = null;
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (D)
			MainActivity.debug(1, TAG, "onActivityResult " + resultCode);
		switch (requestCode)
		{
			case MainActivity.REQUEST_CONNECT_DEVICE_SECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)
				{
					connectDevice(data);
				}
				break;
			case MainActivity.REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK)
				{
					// Bluetooth is now enabled, so set up a chat session
					setup();
				}
				else
				{
					// User did not enable Bluetooth or an error occurred
					MainActivity.debug(1, TAG, "BT not enabled");
					Toast.makeText(mActivity, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
				}
		}
	}

	public void connectDevice(Intent data)
	{
		// Get the device MAC address
		String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mBTService.connect(device);
	}

	public static void synch(CrosswordModel iCrossword)
	{
		if (sManagerInstance!=null && sManagerInstance.isActive())
		{
			sManagerInstance.sendCrossword(iCrossword);
		}
	}

	
}
