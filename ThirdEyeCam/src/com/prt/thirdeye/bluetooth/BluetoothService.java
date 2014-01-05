package com.prt.thirdeye.bluetooth;

import java.io.ByteArrayOutputStream;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.prt.btlib.ConnectionCallback;
import com.prt.btlib.BluetoothData;
import com.prt.btlib.bluetooth.BluetoothConnection;
import com.prt.btlib.bluetooth.ServerBluetoothConnection;
import com.prt.thirdeye.CamApplication;
import com.prt.thirdeye.CamManager;
import com.prt.thirdeye.SnapshotManager;
import com.prt.thirdeye.SnapshotManager.PreviewCaptureListener;
import com.prt.thirdeye.SnapshotManager.SnapshotInfo;

public class BluetoothService extends Service {
	public BluetoothService() {
	}

	private BluetoothConnection mConnection;
	// you can define original command(byte)
	public static final byte COMMAND_PREVIEW = 0;
	public static final int COMMAND_ID_1 = 1;
	public static final int COMMAND_ID_2 = 2;
	public static final String TAG = BluetoothService.class.getSimpleName();
	public static boolean rebound = false;
	// Binder given to clients
	private final IBinder mBinder = new BluetoothServiceBinder();
	private boolean isClientConnected = false;
	private static volatile boolean mIsStreaming = false;
	private static final Object streamingLock = new Object();

	private CamManager mCamMan;
	private SnapshotManager mSnapshotManager;
	private Handler mHandler;

	@Override
	public void onCreate() {
		super.onCreate();
		// you can start Bluetooth Connection
		// by call Server(Client)BluetoothConnection.startConnection()
		mConnection = new ServerBluetoothConnection(
				new ServerConnectionCallback(), true);
		mConnection.startConnection();

		mCamMan = ((CamApplication) getApplication()).getCamManager();
		mSnapshotManager = new SnapshotManager(mCamMan, BluetoothService.this);
		mHandler = new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {
		// TODO Auto-generated method stub
		super.onRebind(intent);
		rebound = false;
		Log.v(TAG, "onRebind");
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "onUnbind");
		(new Handler()).postDelayed(new Runnable() {
			public void run() {
				if (rebound)
					stopSelf();
			}
		}, 1000);
		super.onUnbind(intent);
		// check if everything's good...last pic is sent?
		// stopSelf();
		return true;
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		// stop bluetooth connection...
		mConnection.stopConnection();
		super.onDestroy();
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

	/**
	 * Implements ConnectionCallback (first argument of
	 * Server(Client)BluetoothConnection Contructor)
	 */
	class ServerConnectionCallback implements ConnectionCallback {

		/*
		 * this method called when connection established
		 */
		public void onConnected() {
			Log.e(TAG, "onConnected");
			isClientConnected = true;
		}

		/*
		 * this method called when connection error occurred
		 */
		public void onConnectionFailed() {
			// finish();
			Log.e(TAG, "Connection failed!");
			isClientConnected = false;
		}

		/*
		 * this method called when command received
		 */
		public void onDataReceived(BluetoothData command) {

		}

		/**
		 * this method called when data is sent
		 * 
		 * @param id
		 *            indicates which data was sent.
		 */
		public void onDataSent(int id) {

			// If you want to send a data, use sendData(int command, byte[]
			// data, int id).
			// byte[] data = new byte[] { 'f', 'r', 'm', 's', 'r', 'v', 'r'
			// };
			// String str = "hello, world!";
			// byte[] data = str.getBytes();
			// mConnection.sendData(COMMAND_PREVIEW, data, COMMAND_ID_2);
			// break;
		}
	}

	public void startStreaming() {
		synchronized (streamingLock) {
			if (!mIsStreaming) {
				if (mSnapshotManager != null)
					mSnapshotManager
							.setPreviewCaptureListener(mPreviewCaptureListener);
				if (mHandler != null)
					mHandler.postDelayed(mCapturePreviewRannable, 1000);
				mIsStreaming = true;
			}
		}
	}

	public void stopStreaming() {
		synchronized (streamingLock) {
			if (mIsStreaming) {
				if (mHandler != null)
					mHandler.removeCallbacks(mCapturePreviewRannable);
				if (mSnapshotManager != null)
					mSnapshotManager.removePreviewCaptureListener();
				mIsStreaming = false;
			}
		}
	}

	private Runnable mCapturePreviewRannable = new Runnable() {

		@Override
		public void run() {
			mSnapshotManager.captureStreamingPreview();
			mHandler.postDelayed(mCapturePreviewRannable, 1000);
		}

	};

	private void sendPreview(byte[] data) {
		mConnection.sendData(COMMAND_PREVIEW, data, COMMAND_ID_2);
	}

	private PreviewCaptureListener mPreviewCaptureListener = new PreviewCaptureListener() {

		@Override
		public void onPreviewCaptured(final SnapshotInfo info) {
			// TODO Auto-generated method stub
			Log.v(TAG, " --- --- onPreviewCaptured --- --- ");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			info.mThumbnail.compress(Bitmap.CompressFormat.JPEG, 10, baos);
			final byte[] jpegData = baos.toByteArray();

			// Log.v(TAG, "jpegData.length " + data.length);

			new Thread(new Runnable() {

				@Override
				public void run() {
					Log.v(TAG, "#run mIsStreaming " + mIsStreaming);
					Log.v(TAG, "#run isClientConnected " + isClientConnected);
					if (mIsStreaming && isClientConnected) {
						try {
							Log.v(TAG, "about to send...");
							Thread.sleep(500);
							sendPreview(jpegData);
							Log.v(TAG, " --- --- TEST --- --- ");
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				}

			}).start();

		}

	};
}
