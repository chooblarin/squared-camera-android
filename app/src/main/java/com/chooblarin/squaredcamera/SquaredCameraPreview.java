package com.chooblarin.squaredcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by chooblarin on 2014/08/30.
 */
public class SquaredCameraPreview extends SurfaceView
        implements SurfaceHolder.Callback {

    private static final String TAG = "mimic_SquaredCameraPreview";

    private static final String JPEG_FILE_PREFIX = "IMG_";

    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private SurfaceHolder mHolder;

    private Camera mCamera;

    private int mCameraId;

    private boolean mIsTakingPhoto;

    private boolean mHasFocusArea;

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
        setCameraPreviewSize();
        setPictureSize();

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

    public void takePicture() {
        Toast.makeText(getContext(),
                "take picture", Toast.LENGTH_SHORT).show();

        if (mIsTakingPhoto) {
            return;
        }

        // make sure that preview running
        mCamera.startPreview();
        tryAutoFocus();

        Camera.PictureCallback jpegPictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bitmap = null;
                BitmapFactory.Options options = new BitmapFactory.Options();

                // for debug
                options.inSampleSize = 2;

                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                Log.d(TAG, "decoded bitmap size " + width + ", " + height);

                Matrix matrix = new Matrix();
                matrix.postRotate(90); // for portrait
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

                File picFile = getOutputMediaFile();

                try {
                    FileOutputStream fos = new FileOutputStream(picFile);
                    if (bitmap != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    } else {
                        fos.write(data);
                    }
                    fos.close();

                    // oom ...
                    if (bitmap != null) {
                        bitmap.recycle();
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // restart camera
                try {
                    mCamera.startPreview();
                    mIsTakingPhoto = false;
                } catch (Exception e) {
                    Log.d(TAG, "Error starting camera preview after taking photo: " + e.getMessage());
                }

                //sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(picFile)));
            }
        };

        // take picture
        try {
            mCamera.takePicture(null, null, jpegPictureCallback);
            mIsTakingPhoto = true;
            Toast.makeText(getContext(),
                    "Taking a photo...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.d(TAG, "Camera takePicture failed: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(),
                    "Failed to take picture", Toast.LENGTH_SHORT).show();
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

    private void setPictureSize() {
        Camera.Parameters params = mCamera.getParameters();

        // Set picture size
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size currentSize = null;
        for (Camera.Size size : sizes) {
            Log.d(TAG, "Supported size: width -> " + size.width + ", " +
                    "height -> " + size.height);
            if (currentSize == null || size.width > currentSize.width
                    || (size.width == currentSize.width && size.height > currentSize.height)) {
                currentSize = size;
            }
        }

        if (currentSize != null) {
            Log.d(TAG, "Current size: width ->" + currentSize.width + ", " +
                    "height -> " + currentSize.height);
            mCamera.setParameters(params);
        }
    }

    // Adjust SurfaceView size
    private void adjustViewSize(Camera.Size size) {
        int width = getWidth();

        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        // for portrait
        layoutParams.width = width;
        // float coefficient = (float) width / size.width;
        float coefficient = (float) size.height / width;
        layoutParams.height = (int) (size.width * coefficient);

        this.setLayoutParams(layoutParams);
    }

    private void cancelAutoFocus() {
        mCamera.cancelAutoFocus();
    }

    private void tryAutoFocus() {
        Camera.Parameters parameters = mCamera.getParameters();
        String focusMode = parameters.getFocusMode();
        Log.d(TAG, "focusMode is " + focusMode);

        Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Log.d(TAG, "autofocus complete: " + success);
                /*
                mFocusSuccess = success ? FOCUS_SUCCESS : FOCUS_FAILED;
                mFocusCompleteTime = System.currentTimeMillis();
                */
            }
        };
        mCamera.autoFocus(autoFocusCallback);
    }

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "squared"
        );

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
            //activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(mediaStorageDir)));
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(
                mediaStorageDir.getPath() + File.separator
                        + JPEG_FILE_PREFIX + timeStamp + JPEG_FILE_SUFFIX
        );

        return mediaFile;
    }
}
