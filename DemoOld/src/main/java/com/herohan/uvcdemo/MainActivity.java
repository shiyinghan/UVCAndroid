package com.herohan.uvcdemo;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.uvcdemo.R;

public class MainActivity extends BaseActivity {
    private static final boolean DEBUG = false;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            if (DEBUG) Log.i(TAG, "onCreate:new");
            showFragment();
        }

        // check and request permissions of writing storage and recording audio
        checkPermissionWriteExternalStorage();
        checkPermissionAudio();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkPermissionCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.v(TAG, "onResume:");
//		updateScreenRotation();
    }

    @Override
    protected void onPause() {
        if (DEBUG) Log.v(TAG, "onPause:isFinishing=" + isFinishing());
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (DEBUG) Log.v(TAG, "onDestroy:");
        super.onDestroy();
    }

    private void showFragment() {
        Fragment fragment = new MainFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.container, fragment).commit();
    }
}
