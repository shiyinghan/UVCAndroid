package com.serenegiant.common;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;

import xcrash.XCrash;

public class BaseApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // init XCrash to handle crash
        xcrash.XCrash.init(base, new XCrash.InitParameters()
                .setLogDir(getGlobalPath(base)));
    }

    public String getGlobalPath(Context context) {
        String basePath;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            basePath = context.getExternalFilesDir(null).getAbsolutePath();
        } else {
            basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return basePath + File.separator + "crash" + File.separator;
    }
}
