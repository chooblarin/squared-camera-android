package com.chooblarin.squaredcamera;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Created by chooblarin on 2014/08/30.
 */
public class SquaredCameraActivity extends Activity {

    private static final String TAG = "mimic_SquaredCameraActivity";

    public static Intent createIntent(Context context) {
        return new Intent(context, SquaredCameraActivity.class);
    }

    private SquaredCameraPreview mPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_squared_camera);

        // 画面をスリープ状態にさせない
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        Log.d(TAG, "standard max memory = " + activityManager.getMemoryClass() + "MB");
        Log.d(TAG, "large max memory = " + activityManager.getLargeMemoryClass() + "MB");

        mPreview = new SquaredCameraPreview(this);
        ((FrameLayout) findViewById(R.id.squared_camera_preview)).addView(mPreview);
    }

    public void onClickTakePicture(View view) {
        mPreview.takePicture();
    }
}
