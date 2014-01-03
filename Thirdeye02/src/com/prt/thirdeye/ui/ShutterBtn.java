package com.prt.thirdeye.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class ShutterBtn extends ImageView {

//	private ShutterBtnListener mListener;

//	private int mBtnMode = BUTTON_MODE_PHOTO;
//	public final static int BUTTON_MODE_PHOTO = 1;
//	public final static int BUTTON_MODE_VIDEO = 2;
	
	public final static String TAG = ShutterBtn.class.getSimpleName();
	
	public ShutterBtn(Context context) {
		super(context);
	}

	public ShutterBtn(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ShutterBtn(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

//	public void setShutterBtnListener(ShutterBtnListener listener) {
//		mListener = listener;
//	}
//	
//	public void setShutterBtnMode(int mode) {
//		mBtnMode = mode;
//	}
//
//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
//			if(mListener == null) {
//				Log.e(TAG, "ShutterBtnListener is not set.");
//			} else {
//				mListener.onShutterBtnPressed();
//			}
//		}
//		return super.onTouchEvent(event);
//	}

	/**
	 * Interface that notifies the CamActivity about the shutter button events.
	 */
//	public interface ShutterBtnListener {
//		public void onShutterBtnPressed();
//	}
}
