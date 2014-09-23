package com.chooblarin.squaredcamera.event;

import com.squareup.otto.Bus;

/**
 * Created by chooblarin on 2014/09/23.
 */
public class BusHolder {
    private static Bus sBus = new Bus();

    public static Bus getInstance() {
        return sBus;
    }

    private BusHolder() {
    }
}
