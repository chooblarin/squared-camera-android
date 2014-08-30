package com.chooblarin.squaredcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

        mPreview = new SquaredCameraPreview(this);
        ((FrameLayout) findViewById(R.id.squared_camera_preview)).addView(mPreview);
    }
}
