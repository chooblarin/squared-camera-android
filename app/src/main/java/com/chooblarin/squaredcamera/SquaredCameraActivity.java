package com.chooblarin.squaredcamera;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

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

        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        Log.d(TAG, "standard max memory = " + activityManager.getMemoryClass() + "MB");
        Log.d(TAG, "large max memory = " + activityManager.getLargeMemoryClass() + "MB");

        mPreview = new SquaredCameraPreview(this);
        ((FrameLayout) findViewById(R.id.squared_camera_preview)).addView(mPreview);
    }

    public void onClickTakePhoto(View view) {
        Toast.makeText(getApplicationContext(),
                "take photo", Toast.LENGTH_SHORT).show();
    }
}
