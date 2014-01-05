package com.prt.thirdeyeglass;

import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.google.android.glass.widget.CardScrollAdapter;

public class SelectDeviceCardScrollAdapter extends CardScrollAdapter {

	private final Context mContext;
	private final List<BluetoothDevice> mPairedDevices;
	private final int mCount;
	private final String TAG = SelectDeviceCardScrollAdapter.class
			.getSimpleName();

	public SelectDeviceCardScrollAdapter(Context context,
			List<BluetoothDevice> pairedDevices) {
		this.mContext = context;
		this.mPairedDevices = pairedDevices;
		this.mCount = pairedDevices.size();
	}

	/**
	 * Finds the position of the item with the specified key identifier.
	 */
	@Override
	public int findIdPosition(Object id) {
		if (id instanceof Integer) {
			int idInt = (Integer) id;
			if (idInt >= 0 && idInt < mCount) {
				return idInt;
			}
		}
		return AdapterView.INVALID_POSITION;
	}

	/**
	 * Finds the position of the specified value item.
	 */
	@Override
	public int findItemPosition(Object item) {
		if (mPairedDevices.contains(item))
			return mPairedDevices.indexOf(item);
		return AdapterView.INVALID_POSITION;
	}

	@Override
	public int getCount() {
		return mCount;
	}

	/**
	 * Get the data item associated with the specified position in the data set.
	 */
	@Override
	public Object getItem(int pos) {
		if (0 <= pos && pos < mPairedDevices.size())
			return mPairedDevices.get(pos);
		return null;
	}

	@Override
	public View getView(int pos, View convertView, ViewGroup parent) {
		Log.v(TAG, "getView");
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.card_select_paired_device, parent);
			Log.v(TAG, "convertView == null");
		}
		final TextView view = (TextView) convertView
				.findViewById(R.id.device_name);
		Log.v(TAG, "name " + mPairedDevices.get(pos).getName());
		view.setText(mPairedDevices.get(pos).getName());
		return setItemOnCard(this, convertView);
	}

}
