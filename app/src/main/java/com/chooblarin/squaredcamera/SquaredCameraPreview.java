package com.chooblarin.squaredcamera;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

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
}
