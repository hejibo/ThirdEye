package com.prt.thirdeye;

import com.prt.thirdeye.ui.ShutterBtn;
import com.prt.thirdeye.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * A main activity shows the camera preview.
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

	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_cam);

		// final View controlsView =
		// findViewById(R.id.fullscreen_content_controls);
		// TODO: PRAT
		final View contentView = findViewById(R.id.renderer_container);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Setup shutter button
		mShutterBtn = (ShutterBtn) findViewById(R.id.btn_shutter);
		mShutterBtn.setOnClickListener(new MainShutterClickListener());
		// mShutterBtn.setShutterBtnListener(l);
		setCameraMode(mCameraMode);

		// findViewById(R.id.btn_shutter).setOnTouchListener(
		// mDelayHideTouchListener);

		// Create orientation listener. This should be done first because it
		// takes some time to get first orientation.
		mOrientationListener = new CameraOrientationEventListener(this);
		mOrientationListener.enable();
		setupCamera();
		mHandler = new Handler();
		mPaused = false;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
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

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	protected void setupCamera() {
		// Setup the Camera hardware and preview
		mCamManager = new CamManager(this);
		((CamApplication) getApplication()).setCameraManager(mCamManager);
		setGLRenderer(mCamManager.getRenderer());
		mCamPreviewListener = new CameraPreviewListener();
		mCamManager.setPreviewPauseListener(mCamPreviewListener);
		mCamManager.setCameraReadyListener(this);
		mCamManager.open(Camera.CameraInfo.CAMERA_FACING_BACK);
	}

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
			Configuration config = getResources().getConfiguration();
			int rotation = getWindowManager().getDefaultDisplay().getRotation();
			Util.getDisplayRotation(CamActivity.this);

			boolean nativeLandscape = false;

			if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && config.orientation == Configuration.ORIENTATION_LANDSCAPE)
					|| ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
				nativeLandscape = true;
			}

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
		private long mRecordingStartTimestamp;
		private TextView mTimerTv;
		private boolean mIsRecording;

		private Runnable mUpdateTimer = new Runnable() {
			@Override
			public void run() {
				long recordingDurationMs = System.currentTimeMillis()
						- mRecordingStartTimestamp;
				int minutes = (int) Math.floor(recordingDurationMs / 60000.0);
				int seconds = (int) recordingDurationMs / 1000 - minutes * 60;

				mTimerTv.setText(String.format("%02d:%02d", minutes, seconds));

				// Loop infinitely until recording stops
				if (mIsRecording) {
					mHandler.postDelayed(this, 500);
				}
			}
		};

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
}
