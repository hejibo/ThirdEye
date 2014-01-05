package com.prt.thirdeyeglass;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;

import com.google.android.glass.app.Card;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardScrollView;

public class SelectPairedDeviceActivity extends Activity implements
		GestureDetector.BaseListener {

	private static final String TAG = SelectPairedDeviceActivity.class
			.getSimpleName();

	private GestureDetector mDetector;
	private CardScrollView mView;
	private SelectDeviceCardScrollAdapter mAdapter;
	private List<BluetoothDevice> mDevices;

	public static final String EXTRA_INITIAL_VALUE = "initial_value";
	public static final String EXTRA_SELECTED_DEVICE = "selected_device";

	private AudioManager mAudioManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		mDevices = new ArrayList<BluetoothDevice>(pairedDevices);

		// If there are paired devices
		if (mDevices.size() > 0) {
			mAdapter = new SelectDeviceCardScrollAdapter(this, mDevices);
			mDetector = new GestureDetector(this).setBaseListener(this);
			mView = new CardScrollView(this) {
				@Override
				public final boolean dispatchGenericFocusedEvent(
						MotionEvent event) {
					if (mDetector.onMotionEvent(event)) {
						return true;
					}
					return super.dispatchGenericFocusedEvent(event);
				}
			};
			mView.setAdapter(mAdapter);
			setContentView(mView);
		} else {
			Card card = new Card(this);
			card.setText("No Paired Device.");
			card.setInfo("Please pair your phone and Glass with MyGlass app.");
			setContentView(card.toView());
		}

		// If there are paired devices
		// if (mDevices.size() > 0) {
		// // Loop through paired devices
		// for (BluetoothDevice device : mDevices) {
		// Log.v(TAG, "Device : " + device.getName() + ", Address "
		// + device.getAddress());
		// }
		// } else {
		// Log.v(TAG, "Make sure the user turns on Bluetooth on both devices.");
		// }
		// DiscoverableSwitch.enableDiscoverable(this, 300);
		// BluetoothHelper helper = new BluetoothHelper();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onGesture(Gesture gesture) {
		if (gesture == Gesture.TAP && mDevices.size() > 0) {
			Intent resultIntent = new Intent();
			setResult(RESULT_OK, resultIntent);
			int pos = mView.getSelectedItemPosition();
			BluetoothDevice d = (BluetoothDevice) mView.getItemAtPosition(pos);
			Log.v(TAG, "selected " + d.getName());
			resultIntent.putExtra(EXTRA_SELECTED_DEVICE, d);
			mAudioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
			finish();
			return true;
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();
		mView.activate();
		mView.setSelection(getIntent().getIntExtra(EXTRA_INITIAL_VALUE, 0));
	}

	@Override
	public void onPause() {
		super.onPause();
		mView.deactivate();
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		return mDetector.onMotionEvent(event);
	}
}
