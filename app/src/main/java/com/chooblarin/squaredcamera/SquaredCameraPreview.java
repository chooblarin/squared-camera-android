package com.chooblarin.squaredcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private Camera.Size mSurfaceSize;

    private Camera.Size mPictureSize;

    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();

    private Matrix mCameraToPreviewMatrix = new Matrix();

    private Matrix mPreviewToCameraMatrix = new Matrix();

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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() != 1) {
            Log.d(TAG, "multi touch");
            return true;
        }

        if (event.getAction() != MotionEvent.ACTION_UP) {
            Log.d(TAG, "ACTION -> " + event.getAction());
            return true;
        }

        if (mIsTakingPhoto) {
            return true;
        }

        mCamera.startPreview();
        cancelAutoFocus();

        Camera.Parameters parameters = mCamera.getParameters();
        String focusMode = parameters.getFocusMode();

        Log.d(TAG, "FocusMode -> " + focusMode);
        if (parameters.getMaxNumFocusAreas() != 0 && focusMode != null &&
                (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO)
                        || focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO)
                        || focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))) {
            Log.d(TAG, "set focus (and metering?) area");
            float x = event.getX();
            float y = event.getY();
            Log.d(TAG, "x => " + x + ", y => " + y);

            ArrayList<Camera.Area> areas = getAreas(event.getX(), event.getY());
            parameters.setFocusAreas(areas);

            if (parameters.getMaxNumMeteringAreas() != 0) { // also set metering areas
                parameters.setMeteringAreas(areas);
            }

            mCamera.setParameters(parameters);
        }

        tryAutoFocus();
        return true;
    }

    private ArrayList<Camera.Area> getAreas(float x, float y) {
        float[] coords = {x, y};
        // calculatePreviewToCameraMatrix();
        // previewToCameraMatrix.MapPoints(coords);
        mCameraToPreviewMatrix.reset();
        //mCamera.getCameraInfo(mCameraId, mCameraInfo);
        mCameraToPreviewMatrix.postRotate(90);
        mCameraToPreviewMatrix.postScale(getWidth() / 2000f, getHeight() / 2000f);
        mCameraToPreviewMatrix.postTranslate(getWidth() / 2f, getHeight() / 2f);
        Log.d(TAG, "CameraToPreviewMatrix " + mCameraToPreviewMatrix.toString());

        if (!mCameraToPreviewMatrix.invert(mPreviewToCameraMatrix)) {
            Log.d(TAG, "failed to invert matrix !");
        }
        Log.d(TAG, "mPreviewToCameraMatrix " + mPreviewToCameraMatrix.toString());

        Log.d(TAG, "x => " + coords[0] + ", y => " + coords[1]);
        mPreviewToCameraMatrix.mapPoints(coords);
        Log.d(TAG, "x => " + coords[0] + ", y => " + coords[1]);

        x = coords[0];
        y = coords[1];

        int focusSize = 50;
        Rect rect = new Rect();
        rect.left = (int) x - focusSize;
        rect.right = (int) x + focusSize;
        rect.top = (int) y - focusSize;
        rect.bottom = (int) y + focusSize;

        if (rect.left < -1000) {
            rect.left = -1000;
            rect.right = rect.left + 2 * focusSize;
        } else if (rect.right > 1000) {
            rect.right = 1000;
            rect.left = rect.right - 2 * focusSize;
        }

        if (rect.top < -1000) {
            rect.top = -1000;
            rect.bottom = rect.top + 2 * focusSize;
        } else if (rect.bottom > 1000) {
            rect.bottom = 1000;
            rect.top = rect.bottom - 2 * focusSize;
        }

        ArrayList<Camera.Area> areas = new ArrayList<Camera.Area>();
        areas.add(new Camera.Area(rect, 1000));
        return areas;
    }

    public void takePicture() {
        Toast.makeText(getContext(),
                "take picture", Toast.LENGTH_SHORT).show();

        if (mIsTakingPhoto) {
            return;
        }

        // make sure that preview running
        mCamera.startPreview();
        //tryAutoFocus();

        Camera.PictureCallback jpegPictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                int maxSize = 1024;
                mCamera.stopPreview();

                BitmapFactory.Options options = new BitmapFactory.Options();

                // 所望のbitmapをつくる前にサイズを調べる．(OOM対策)
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, options);

                int width = options.outWidth; // 4128
                int height = options.outHeight; // 3096

                Log.d(TAG, "scanned size .. options.size " + width + ", " + height);

                // サイズを小さくして読み込み
                int srcSize = Math.max(width, height); // 4128
                options.inSampleSize = maxSize < srcSize ? (srcSize / maxSize) : 1; // 4
                Log.d(TAG, "sample size " + options.inSampleSize);
                options.inJustDecodeBounds = false;
                Bitmap tmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                Log.d(TAG, "risezed size .. options.size " + options.outWidth + ", " + options.outHeight);

                int size = Math.min(options.outWidth, options.outHeight); // 774
                float previewRatio = (float) mSurfaceSize.height / (float) mSurfaceSize.width;
                float cameraRatio = (float) options.outHeight / (float) options.outWidth;
                Log.d(TAG, "preview ratio: " + previewRatio + ", camera ratio: " + cameraRatio);

                Matrix matrix = new Matrix();
                matrix.postRotate(90); // for portrait

                int length = (int) (size * (previewRatio / cameraRatio)); // 1辺の長さ
                Log.d(TAG, "rid length -> " + length);
                int rid = size - length; // 切り捨てる部分の長さ

                // 所望のBitmapを生成
                // 座標系は回転する前のモノっぽい（?）
                Bitmap source = Bitmap.createBitmap(tmp,
                        0,  // x
                        (int) (rid * 0.5), // y
                        length,
                        length,
                        matrix, true);

                tmp.recycle();

                // bitmapをファイルに保存
                File picFile = getOutputMediaFile();

                try {
                    FileOutputStream fos = new FileOutputStream(picFile);
                    if (source != null) {
                        source.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    } else {
                        fos.write(data);
                    }
                    fos.close();

                    // oom ...
                    if (source != null) {
                        source.recycle();
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

        Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
            }
        };

        try {
            mCamera.takePicture(shutterCallback, null, jpegPictureCallback);
            mIsTakingPhoto = true;
            Toast.makeText(getContext(),
                    "Taking a photo...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.d(TAG, "Camera takePicture failed: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(),
                    "Failed to take picture", Toast.LENGTH_SHORT).show();
        }
        /*
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    Log.d(TAG, "autofocus complete: " + success);
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
            }
        });
        */
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

        mSurfaceSize = bestSize;

        Log.d(TAG, "preview sizes -> w : " + bestSize.width
                + ", h: " + bestSize.height + ", ratio: " + (float) bestSize.height / bestSize.width);
        params.setPreviewSize(mSurfaceSize.width, mSurfaceSize.height);

        mCamera.setParameters(params);
        adjustViewSize(mSurfaceSize);
    }

    private void setPictureSize() {
        Camera.Parameters params = mCamera.getParameters();

        // Set picture size
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size currentSize = null;
        for (Camera.Size size : sizes) {
            if (currentSize == null || size.width > currentSize.width
                    || (size.width == currentSize.width && size.height > currentSize.height)) {
                currentSize = size;
            }
        }

        if (currentSize != null) {
            Log.d(TAG, "picture size: width ->" + currentSize.width + ", " +
                    "height -> " + currentSize.height + ", ratio -> " + (float) currentSize.height / currentSize.width);
            mPictureSize = currentSize;
            params.setPictureSize(mPictureSize.width, mPictureSize.height);
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d("mimic SquareCameraPreview", "getMeasuredWidth -> " + getMeasuredWidth()
                + ", getMeasuredHeight()" + getMeasuredHeight());
    }
}
