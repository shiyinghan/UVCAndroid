package com.herohan.uvcapp.utils;

import android.net.Uri;
import android.os.Environment;

import com.serenegiant.utils.UVCUtils;
import com.herohan.uvcapp.R;

import java.io.File;
import java.util.Date;

public class SaveHelper {

    public static String BaseStoragePath = null;

    public static void checkBaseStoragePath() {
        if (BaseStoragePath == null) {
            BaseStoragePath = Environment.getExternalStorageDirectory().getPath() + File.separator + UVCUtils.getApplication().getString(R.string.app_name);
        }
    }

    public static String getSavePhotoPath() {
        checkBaseStoragePath();

        String parentPath = BaseStoragePath + File.separator + TimeFormatter.format_yyyyMMdd(new Date()) + File.separator + "photo";
        File folder = new File(parentPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return parentPath + File.separator + TimeFormatter.format_yyyy_MM_dd_HH_mm_ss(new Date()) + ".jpg";
    }

    public static Uri getSavePhotoUri() {
        return Uri.fromFile(new File(getSavePhotoPath()));
    }

    public static String getSaveVideoPath() {
        checkBaseStoragePath();

        String parentPath = BaseStoragePath + File.separator + TimeFormatter.format_yyyyMMdd(new Date()) + File.separator + "video";
        File folder = new File(parentPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return parentPath + File.separator + TimeFormatter.format_yyyy_MM_dd_HH_mm_ss(new Date()) + ".mp4";
    }

    public static Uri getSaveVideoUri() {
        return Uri.fromFile(new File(getSaveVideoPath()));
    }

}
