package com.herohan.uvcapp;

import android.app.Application;
import android.graphics.Color;

import jp.wasabeef.takt.Seat;
import jp.wasabeef.takt.Takt;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
//        Takt.stock(this)
//                .seat(Seat.BOTTOM_LEFT)
//                .interval(250)
//                .color(Color.WHITE)
//                .size(14f)
//                .alpha(0.5f)
//                .listener(fps -> {
////                    Log.d("uvcdemo", (int) fps + " fps");
//                });
    }
}
