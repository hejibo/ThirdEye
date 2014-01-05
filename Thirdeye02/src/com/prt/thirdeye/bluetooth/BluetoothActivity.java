package com.prt.thirdeye.bluetooth;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.prt.thirdeye.CamActivity;
import com.prt.thirdeye.R;

public class BluetoothActivity extends Activity {

	private static final String TAG = BluetoothActivity.class.getSimpleName();
	private TextView mInfoTV;
	private StringBuilder mInfo = new StringBuilder();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);

		mInfoTV = (TextView) findViewById(R.id.tv_info);
		
		if(isMyGlassRunning())
			mInfo.append("MyGlass is running.");
		else
			mInfo.append("MyGlass is NOT running.");
		mInfoTV.setText(mInfo);
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
	
	private boolean isMyGlassRunning() {
		boolean running = false;
		ActivityManager activityManager = (ActivityManager) this
				.getSystemService(ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> procInfos = activityManager
				.getRunningAppProcesses();
		for (int i = 0; i < procInfos.size(); i++) {
			if (procInfos.get(i).processName.equals("com.google.glass.companion")) {
				running = true;
			}
		}
		return running;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluetooth, menu);
		return true;
	}

}
