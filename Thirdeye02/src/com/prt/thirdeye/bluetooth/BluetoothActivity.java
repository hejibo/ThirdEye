package com.prt.thirdeye.bluetooth;

import com.prt.thirdeye.CamActivity;
import com.prt.thirdeye.R;
import com.prt.thirdeye.bluetooth.BluetoothService;
import com.prt.thirdeye.bluetooth.BluetoothService.BluetoothServiceBinder;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class BluetoothActivity extends Activity {

	private static final String TAG = BluetoothActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);

		final Button testBtn = (Button) findViewById(R.id.btn_test);
		testBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// setup
				Intent intent = new Intent(BluetoothActivity.this,
						CamActivity.class);
				startActivity(intent);
			}

		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluetooth, menu);
		return true;
	}

}
