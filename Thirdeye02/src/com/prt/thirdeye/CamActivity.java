package com.prt.thirdeye;

import com.prt.thirdeye.bluetooth.BluetoothService;
import com.prt.thirdeye.bluetooth.BluetoothService.BluetoothServiceBinder;
import com.prt.thirdeye.ui.ShutterBtn;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

/**
 * CamActivity shows the camera preview and supports user interactions with our
 * camera.
 */
public class CamActivity extends Activity implements
		CamManager.CameraReadyListener {

	public final static String TAG = CamActivity.class.getSimpleName();
	public final static int CAMERA_MODE_PHOTO = 1;
	public final static int CAMERA_MODE_VIDEO = 2;
	private static int mCameraMode = CAMERA_MODE_PHOTO;
	private CamManager mCamManager;
	private SnapshotManager mSnapshotManager;
	private GLSurfaceView mGLSurfaceView;
	private ShutterBtn mShutterBtn;
	private CameraPreviewListener mCamPreviewListener;
	private CameraOrientationEventListener mOrientationListener;
	private MainSnapshotListener mSnapshotListener;
	private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
	private int mOrientationCompensation = 0;
	private Handler mHandler;
	private boolean mPaused;
	private BluetoothService mService;
	private boolean mIsBTSeviceBound = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LOW_PROFILE);

		setContentView(R.layout.activity_cam);

		// Setup shutter button
		mShutterBtn = (ShutterBtn) findViewById(R.id.btn_shutter);
		mShutterBtn.setOnClickListener(new MainShutterClickListener());
		// mShutterBtn.setShutterBtnListener(l);
		setCameraMode(mCameraMode);

		// Create orientation listener. This should be done first because it
		// takes some time to get first orientation.
		mOrientationListener = new CameraOrientationEventListener(this);
		mOrientationListener.enable();
		setupCamera();
		mHandler = new Handler();
		mPaused = false;

		Intent intent = new Intent(this, BluetoothService.class);
		// our service shouldn't destroy if unbind.
		startService(intent);
		// Bind to LocalService
		bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		// Pause the camera preview
		mPaused = true;
		if (mCamManager != null) {
			mCamManager.pause();
		}
		if (mSnapshotManager != null) {
			mSnapshotManager.onPause();
		}
		if (mOrientationListener != null) {
			mOrientationListener.disable();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		// Restore the camera preview
		mPaused = false;

		if (mCamManager != null) {
			mCamManager.resume();
		}

		super.onResume();

		if (mSnapshotManager != null) {
			mSnapshotManager.onResume();
		}
		mOrientationListener.enable();

	}

	// If you want your activity to receive responses even while it is stopped
	// in the background, then you can bind during onCreate() and unbind during
	// onDestroy().
	// @Override
	protected void onDestroy() {
		// TODO fix orienation change issue
		// Unbind from the service
		if (mIsBTSeviceBound) {
			unbindService(mServiceConnection);
			mIsBTSeviceBound = false;
		}
		super.onDestroy();
	}

	/**
	 * Setup the Camera hardware and preview
	 */
	protected void setupCamera() {
		mCamManager = new CamManager(this);
		((CamApplication) getApplication()).setCamManager(mCamManager);
		setGLRenderer(mCamManager.getRenderer());
		mCamPreviewListener = new CameraPreviewListener();
		mCamManager.setPreviewPauseListener(mCamPreviewListener);
		mCamManager.setCameraReadyListener(this);
		mCamManager.open(Camera.CameraInfo.CAMERA_FACING_BACK);
	}

	/**
	 * Set the GL view using the provided renderer.
	 * 
	 * @param renderer
	 */
	public void setGLRenderer(GLSurfaceView.Renderer renderer) {
		final ViewGroup container = ((ViewGroup) findViewById(R.id.renderer_container));
		// Delete the previous GL Surface View (if any)
		if (mGLSurfaceView != null) {
			container.removeView(mGLSurfaceView);
			mGLSurfaceView = null;
		}
		// Make a new GL view using the provided renderer
		mGLSurfaceView = new GLSurfaceView(this);
		mGLSurfaceView.setEGLContextClientVersion(2);
		mGLSurfaceView.setRenderer(renderer);
		container.addView(mGLSurfaceView);
	}

	/**
	 * Returns the mode of the activity See CameraActivity.CAMERA_MODE_*
	 * 
	 * @return int
	 */
	public static int getCameraMode() {
		return mCameraMode;
	}

	/**
	 * Sets the mode of the activity See CameraActivity.CAMERA_MODE_*
	 * 
	 * @param newMode
	 */
	public void setCameraMode(final int newMode) {
		if (mCameraMode == newMode) {
			return;
		}
		if (mCamManager.getParameters() == null) {
			mHandler.post(new Runnable() {
				public void run() {
					setCameraMode(newMode);
				}
			});
		}
		if (mCamPreviewListener != null) {
			mCamPreviewListener.onPreviewPause();
		}
		mCameraMode = newMode;
		if (newMode == CAMERA_MODE_PHOTO) {
			mShutterBtn.setImageDrawable(getResources().getDrawable(
					R.drawable.btn_photo));
			mCamManager.setStabilization(false);
		} else if (newMode == CAMERA_MODE_VIDEO) {
			// mShutterBtn.setImageDrawable(getResources().getDrawable(R.drawable.btn_video));
			// mCamManager.setStabilization(true);
		}
	}

	public int getOrientation() {
		return mOrientationCompensation;
	}

	/**
	 * Updates the orientation of the whole UI (in place) based on the
	 * calculations given by the orientation listener
	 */
	public void updateInterfaceOrientation() {
		// setViewRotation(mShutterButton, mOrientationCompensation);
		// mReviewDrawer.notifyOrientationChanged(mOrientationCompensation);
	}

	public void updateCapabilities() {
		// Populate the sidebar buttons a little later (so we have camera
		// parameters)
		mHandler.post(new Runnable() {
			public void run() {
				Camera.Parameters params = mCamManager.getParameters();

				// We don't have the camera parameters yet, retry later
				if (params == null) {
					if (!mPaused) {
						mHandler.postDelayed(this, 100);
					}
				} else {
					mCamManager.startParametersBatch();
					// Set orientation
					updateInterfaceOrientation();
					mCamManager.stopParametersBatch();
				}
			}
		});
	}

	@Override
	public void onCameraReady() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Camera.Parameters params = mCamManager.getParameters();

				if (params == null) {
					// Are we too fast? Let's try again.
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							onCameraReady();
						}
					}, 20);
					return;
				}

				mCamManager.updateDisplayOrientation();
				Camera.Size picSize = params.getPictureSize();

				Camera.Size sz = Util.getOptimalPreviewSize(CamActivity.this,
						params.getSupportedPreviewSizes(),
						((float) picSize.width / (float) picSize.height));
				if (sz == null) {
					Log.e(TAG,
							"No preview size!! Something terribly wrong with camera!");
					return;
				}

				// TODO: PRAT
				mCamManager.setPreviewSize(sz.width, sz.height);

				// TODO: PRAT
				// if (mIsCamSwitching) {
				// mCamManager.restartPreviewIfNeeded();
				// mIsCamSwitching = false;
				// }

				if (mSnapshotManager == null) {
					mSnapshotManager = new SnapshotManager(mCamManager,
							CamActivity.this);
					mSnapshotListener = new MainSnapshotListener();
					mSnapshotManager.addListener(mSnapshotListener);
				}

				updateCapabilities();
				// TODO: PRAT
				// mSavePinger.stopSaving();
			}
		});

		if (mIsBTSeviceBound) {
			// wait 1 second and start streaming
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mService.startStreaming();
				}
			}, 1000);
		}
	}

	@Override
	public void onCameraFailed() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// TODO: PRAT
				// Toast.makeText(CamActivity.this,
				// getResources().getString(R.string.cannot_connect_hal),
				// Toast.LENGTH_LONG).show();

			}
		});

		if (mIsBTSeviceBound)
			mService.stopStreaming();
	}

	/**
	 * Listener that is called when the preview pauses or resumes
	 */
	private class CameraPreviewListener implements
			CamManager.PreviewPauseListener {
		@Override
		public void onPreviewPause() {
			// XXX: Do a little animation
		}

		@Override
		public void onPreviewResume() {
			// XXX: Do a little animation
		}
	}

	/**
	 * Handles the orientation changes without turning the actual activity
	 */
	private class CameraOrientationEventListener extends
			OrientationEventListener {
		public CameraOrientationEventListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			// We keep the last known orientation. So if the user first orient
			// the camera then point the camera to floor or sky, we still have
			// the correct orientation.
			if (orientation == ORIENTATION_UNKNOWN) {
				return;
			}
			mOrientation = Util.roundOrientation(orientation, mOrientation);

			// Notify camera of the raw orientation
			mCamManager.setOrientation(mOrientation);

			// Adjust orientationCompensation for the native orientation of the
			// device.
			// Configuration config = getResources().getConfiguration();
			// int rotation =
			// getWindowManager().getDefaultDisplay().getRotation();
			Util.getDisplayRotation(CamActivity.this);

			// boolean nativeLandscape = false;
			//
			// if (((rotation == Surface.ROTATION_0 || rotation ==
			// Surface.ROTATION_180) && config.orientation ==
			// Configuration.ORIENTATION_LANDSCAPE)
			// || ((rotation == Surface.ROTATION_90 || rotation ==
			// Surface.ROTATION_270) && config.orientation ==
			// Configuration.ORIENTATION_PORTRAIT)) {
			// nativeLandscape = true;
			// }

			int orientationCompensation = mOrientation; // + (nativeLandscape ?
														// 0 : 90);
			if (orientationCompensation == 90) {
				orientationCompensation += 180;
			} else if (orientationCompensation == 270) {
				orientationCompensation -= 180;
			}

			// Avoid turning all around
			float angleDelta = orientationCompensation
					- mOrientationCompensation;
			if (angleDelta >= 270) {
				orientationCompensation -= 360;
			}

			if (mOrientationCompensation != orientationCompensation) {
				mOrientationCompensation = orientationCompensation;
				updateInterfaceOrientation();
			}
		}
	}

	/**
	 * Snapshot listener for when snapshots are taken, in SnapshotManager
	 */
	private class MainSnapshotListener implements
			SnapshotManager.SnapshotListener {

		@Override
		public void onSnapshotShutter(final SnapshotManager.SnapshotInfo info) {

			// TODO: PRAT
			// final FrameLayout layout = (FrameLayout)
			// findViewById(R.id.thumb_flinger_container);
			// Fling the preview
			// final ThumbnailFlinger flinger = new ThumbnailFlinger(
			// CameraActivity.this);
			// mHandler.post(new Runnable() {
			// @Override
			// public void run() {
			// layout.addView(flinger);
			// flinger.setRotation(90);
			// flinger.setImageBitmap(info.mThumbnail);
			// flinger.doAnimation();
			// }
			// });

			// Unlock camera auto settings
			mCamManager.setLockSetup(false);
			mCamManager.setStabilization(false);
		}

		@Override
		public void onSnapshotPreview(SnapshotManager.SnapshotInfo info) {
			// Do nothing here
		}

		@Override
		public void onSnapshotProcessing(SnapshotManager.SnapshotInfo info) {
			// TODO: PRAT
			// runOnUiThread(new Runnable() {
			// public void run() {
			// if (mSavePinger != null) {
			// mSavePinger.setPingMode(SavePinger.PING_MODE_ENHANCER);
			// mSavePinger.startSaving();
			// }
			// }
			// });
		}

		@Override
		public void onSnapshotSaved(SnapshotManager.SnapshotInfo info) {
			String uriStr = info.mUri.toString();

			// Add the new image to the gallery and the review drawer
			int originalImageId = Integer.parseInt(uriStr.substring(
					uriStr.lastIndexOf("/") + 1, uriStr.length()));
			Log.v(TAG, "Adding snapshot to gallery: " + originalImageId);
			// TODO: PRAT
			// mReviewDrawer.addImageToList(originalImageId);
			// mReviewDrawer.scrollToLatestImage();
		}

		@Override
		public void onMediaSavingStart() {
			// TODO: PRAT
			// runOnUiThread(new Runnable() {
			// @Override
			// public void run() {
			// mSavePinger.setPingMode(SavePinger.PING_MODE_SAVE);
			// mSavePinger.startSaving();
			// }
			// });
		}

		@Override
		public void onMediaSavingDone() {
			// TODO: PRAT
			// runOnUiThread(new Runnable() {
			// @Override
			// public void run() {
			// mSavePinger.stopSaving();
			// }
			// });
		}

		@Override
		public void onVideoRecordingStart() {
			// TODO: PRAT
		}

		@Override
		public void onVideoRecordingStop() {
			// TODO: PRAT
		}
	}

	/**
	 * When the shutter button is clicked.
	 */
	public class MainShutterClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (mSnapshotManager == null)
				return;
			if (CamActivity.getCameraMode() == CamActivity.CAMERA_MODE_PHOTO) {
				mSnapshotManager.queueSnapshot(true, 0);
			} else if (CamActivity.getCameraMode() == CamActivity.CAMERA_MODE_VIDEO) {
			}
		}

	}

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.v(TAG, "onServiceConnected");
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			BluetoothServiceBinder binder = (BluetoothServiceBinder) service;
			mService = binder.getService();
			mIsBTSeviceBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mIsBTSeviceBound = false;
		}
	};
}
