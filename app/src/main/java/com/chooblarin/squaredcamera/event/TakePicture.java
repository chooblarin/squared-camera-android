package com.chooblarin.squaredcamera.event;

import android.graphics.Bitmap;

/**
 * Created by chooblarin on 2014/09/23.
 */
public class TakePicture {

    public Bitmap picture;

    public TakePicture(Bitmap picture) {
        this.picture = picture;
    }

}
