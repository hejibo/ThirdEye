package com.prt.thirdeyeglass;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.prt.btlib.ConnectionCallback;
import com.prt.btlib.BluetoothData;
import com.prt.btlib.bluetooth.BluetoothConnection;
import com.prt.btlib.bluetooth.ClientBluetoothConnection;

public class BluetoothService extends Service {
	public BluetoothService() {
	}

	private BluetoothConnection mConnection;
	private StreamingListener mStreamingListener;
	// you can define original command(byte)
	public static final byte PREVIEW_DATA = 0;
	public static final int COMMAND_ID_1 = 1;
	public static final int COMMAND_ID_2 = 2;
	public static final String TAG = BluetoothService.class.getSimpleName();
	public static boolean rebound = false;
	// Binder given to clients
	private final IBinder mBinder = new BluetoothServiceBinder();
	private boolean isClientConnected = false;

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		BluetoothDevice device = (BluetoothDevice) intent
				.getParcelableExtra(SelectPairedDeviceActivity.EXTRA_SELECTED_DEVICE);
		mConnection = new ClientBluetoothConnection(
				new ClientConnectionCallback(), true, device);
		mConnection.startConnection();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		rebound = true;
		Log.v(TAG, "onRebind");
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "onUnbind");
		(new Handler()).postDelayed(new Runnable() {
			public void run() {
				if (!rebound)
					stopSelf();
			}
		}, 1000);
		super.onUnbind(intent);
		return true;
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		// stop Bluetooth connection...
		mConnection.stopConnection();
		super.onDestroy();
	}

	public void setStreamingListener(StreamingListener listener) {
		mStreamingListener = listener;
	}

	class ClientConnectionCallback implements ConnectionCallback {

		/**
		 * called when connection established
		 */
		public void onConnected() {
			Log.v(TAG, "connection established ");
			isClientConnected = true;
		}

		/**
		 * called when connection error occurred
		 */
		public void onConnectionFailed() {
			Log.e(TAG, "Connection failed");
			isClientConnected = false;
		}

		/**
		 * called when command received
		 */
		public void onDataReceived(BluetoothData data) {
			Log.e(TAG, "onCommandReceived");
			switch (data.type) {
			case PREVIEW_DATA:
				Log.v(TAG, "command preview!");

				if (data.optionLen > 0) {
					Bitmap bmp = BitmapFactory.decodeByteArray(data.option, 0,
							data.option.length);
					if (mStreamingListener != null)
						mStreamingListener.onDataReceived(bmp);
					// mImageView.setImageBitmap(bmp);
				}
				break;
			default:
				break;
			}
		}

		/**
		 * called when data is sent
		 */
		public void onDataSent(int id) {
			Log.v(TAG, "onDataSent");
		}
	}

	public interface StreamingListener {
		/**
		 * Called when our data is received.
		 */
		// TODO: change from Bitmap
		public void onDataReceived(Bitmap bmp);
	}

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class BluetoothServiceBinder extends Binder {
		public BluetoothService getService() {
			// Return this instance of BluetoothService so clients can call
			// public methods
			return BluetoothService.this;
		}
	}

}
