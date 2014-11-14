package com.chooblarin.squaredcamera;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by chooblarin on 2014/08/30.
 */
public class SquaredCameraActivity extends ActionBarActivity {

    private static final String TAG = "mimic_SquaredCameraActivity";

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    public static Intent createIntent(Context context) {
        return new Intent(context, SquaredCameraActivity.class);
    }

    private static final int MODE_CAMERA_PREVIEW = 0;
    private static final int MODE_PICTURE_EDIT = 1;

    private int mCurrentMode;

    private SquaredCameraPreview mPreview;
    private SquaredView mSquaredView;
    private Button mShutterButton;

    private Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_squared_camera);

        // 画面をスリープ状態にさせない
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        Log.d(TAG, "standard max memory = " + activityManager.getMemoryClass() + "MB");
        //Log.d(TAG, "large max memory = " + activityManager.getLargeMemoryClass() + "MB");

        mPreview = new SquaredCameraPreview(this);
        ((FrameLayout) findViewById(R.id.squared_camera_preview)).addView(mPreview);

        mSquaredView = (SquaredView) findViewById(R.id.squared_image_view);
        mShutterButton = (Button) findViewById(R.id.shutter_button);
        mShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.takePicture(new SquaredCameraPreview.Callback() {
                    @Override
                    public void onPictureTaken(Bitmap bitmap) {
                        mBitmap = bitmap;
                        mSquaredView.setImageBitmap(mBitmap);
                        mCurrentMode = MODE_PICTURE_EDIT;
                        supportInvalidateOptionsMenu();
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mCurrentMode == MODE_CAMERA_PREVIEW) {
            getMenuInflater().inflate(R.menu.home, menu);
        } else if (mCurrentMode == MODE_PICTURE_EDIT) {
            getMenuInflater().inflate(R.menu.picture_edit, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_item_cancel) {
            mCurrentMode = MODE_CAMERA_PREVIEW;
            mSquaredView.setImageBitmap(null);
            mBitmap.recycle();
            mPreview.retake();
        } else if (itemId == R.id.action_item_save) {
            mCurrentMode = MODE_CAMERA_PREVIEW;
            mSquaredView.setImageBitmap(null);
            saveBitmapToFile();
            mPreview.retake();
        }

        return super.onOptionsItemSelected(item);
    }

    // bitmapをファイルに保存
    private void saveBitmapToFile() {
        File picFile = getOutputMediaFile();

        try {
            FileOutputStream fos = new FileOutputStream(picFile);
            if (mBitmap != null) {
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            }
            fos.close();

            Toast.makeText(getApplicationContext(),
                    "Save picture", Toast.LENGTH_SHORT).show();

            // oom ...
            if (mBitmap != null) {
                mBitmap.recycle();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
