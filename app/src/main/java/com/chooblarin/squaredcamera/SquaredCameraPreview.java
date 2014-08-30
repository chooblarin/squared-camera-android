package com.chooblarin.squaredcamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * Created by chooblarin on 2014/08/30.
 */
public class SquaredCameraPreview extends SurfaceView
        implements SurfaceHolder.Callback {

    private static final String TAG = "mimic_SquaredCameraPreview";

    private SurfaceHolder mHolder;

    private Camera mCamera;

    private int mCameraId;

    @SuppressWarnings("deprecation")
    public SquaredCameraPreview(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open(mCameraId);

        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setWillNotDraw(false); // needed to onDraw call
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("mimic", "surfaceChanged: w: " + width + ", h: " + height);
        if (mHolder.getSurface() == null || mCamera == null) {
            return; // preview surface does not exist, or camera instance is unavailable.
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            //
        }

        //TODO: set preview size
        setCameraDisplayOrientation();
        //setCameraPreviewSizeOld(width, height);
        setCameraPreviewSize();

        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void setCameraDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        mCamera.setDisplayOrientation(90); // for portrait
    }

    // set optimal preview size
    private void setCameraPreviewSize() {
        Camera.Parameters params = mCamera.getParameters();

        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        if (previewSizes.isEmpty()) {
            Log.d(TAG, "emtry preview sizes");
            return;
        }
        Camera.Size bestSize = previewSizes.get(0);
        for (Camera.Size size : previewSizes) {
            if (size.width * size.height > bestSize.width * bestSize.height) {
                bestSize = size;
            }
        }
        params.setPreviewSize(bestSize.width, bestSize.height);

        mCamera.setParameters(params);
        adjustViewSize(bestSize);
    }

    // Adjust SurfaceView size
    private void adjustViewSize(Camera.Size size) {
        int width = getWidth();

        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        // for portrait
        layoutParams.width = width;
        // float coefficient = (float) width / size.width;
        float coefficient = (float) size.height / width;
        layoutParams.height = (int)(size.width * coefficient);

        this.setLayoutParams(layoutParams);
    }
}
