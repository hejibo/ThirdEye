package com.prt.thirdeyeglass;

import com.prt.thirdeyeglass.BluetoothService.BluetoothServiceBinder;
import com.prt.thirdeyeglass.BluetoothService.StreamingListener;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;

public class MainActivity extends Activity implements StreamingListener {

	private static final String TAG = MainActivity.class.getSimpleName();
	static final int SELECT_PAIRED_DEVICE_REQUEST = 0;
	// private BluetoothConnection mConnection;
	private BluetoothService mService;
	private boolean mIsBTSeviceBound = false;

	private ImageView mImageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mImageView = (ImageView) MainActivity.this.findViewById(R.id.imageView);
		mImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		startActivityForResult(new Intent(this,
				SelectPairedDeviceActivity.class), SELECT_PAIRED_DEVICE_REQUEST);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (mIsBTSeviceBound) {
			unbindService(mServiceConnection);
			mIsBTSeviceBound = false;
		}
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SELECT_PAIRED_DEVICE_REQUEST) {
			if (resultCode == RESULT_OK) {
				BluetoothDevice device = (BluetoothDevice) data
						.getParcelableExtra(SelectPairedDeviceActivity.EXTRA_SELECTED_DEVICE);
				Log.v(TAG, "onActivityResult : " + device.getName());
				Intent intent = new Intent(this, BluetoothService.class);
				intent.putExtra(
						SelectPairedDeviceActivity.EXTRA_SELECTED_DEVICE,
						device);
				// our service shouldn't destroy if unbind.
				startService(intent);
				// Bind to LocalService
				bindService(intent, mServiceConnection,
						Context.BIND_AUTO_CREATE);
			}
		}
	}

	/**
	 * Defines callbacks for service binding, passed to bindService()
	 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.v(TAG, "onServiceConnected");
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			BluetoothServiceBinder binder = (BluetoothServiceBinder) service;
			mService = binder.getService();
			mIsBTSeviceBound = true;
			mService.setStreamingListener(MainActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mIsBTSeviceBound = false;
		}
	};

	@Override
	public void onDataReceived(Bitmap bmp) {
		mImageView.setImageBitmap(bmp);
	}

}
