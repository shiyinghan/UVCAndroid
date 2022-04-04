package com.serenegiant.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.serenegiant.uvccamera.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import cn.hutool.core.io.FileUtil;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    private static final String MIME_PREFIX_IMAGE = "image/";
    private static final String MIME_PREFIX_VIDEO = "video/";
    private static final String MIME_PREFIX_AUDIO = "audio/";

    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    @NonNull
    public static String getAppName() {
        return UVCUtils.getApplication().getString(R.string.app_name);
    }

    /**
     * generate output file
     * use media store to create media file directly, updating media store manually by MediaScannerConnection.scanFile is unnecessary.
     *
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
     * @param ext  .mp4(.m4a for audio) or .png
     * @return return null when this app has no writing permission to external storage.
     */
    public static Uri getCaptureUri(Context context, final String type, final String ext) {
        return getCaptureUri(context, type, null, ext);
    }

    public static Uri getCaptureUri(final Context context,
                                          final String type, final String prefix, final String ext) {
        String fileName = getDateTimeString() + ext;
        if (!TextUtils.isEmpty(prefix)) {
            fileName = prefix + fileName;
        }
        String mimeType = FileUtil.getMimeType(ext);

        // Add a specific media item.
        ContentResolver resolver = context.getContentResolver();

        Uri mediaTable = null;
        String volumeName = null;
        // Find all media files on the primary external storage device.
        // On API <= 28, use VOLUME_EXTERNAL instead.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            volumeName = MediaStore.VOLUME_EXTERNAL;
        } else {
            volumeName = MediaStore.VOLUME_EXTERNAL_PRIMARY;
        }
        if (mimeType.startsWith(MIME_PREFIX_IMAGE)) {
            mediaTable = MediaStore.Images.Media.getContentUri(
                    volumeName);
        } else if (mimeType.startsWith(MIME_PREFIX_VIDEO)) {
            mediaTable = MediaStore.Video.Media.getContentUri(
                    volumeName);
        } else if (mimeType.startsWith(MIME_PREFIX_AUDIO)) {
            mediaTable = MediaStore.Audio.Media.getContentUri(
                    volumeName);
        } else {
            return null;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            final File dir = getCaptureDir(context, type);
            if (dir == null) {
                return null;
            }
            String absolutePath = dir.getPath() + File.separator + fileName;
            contentValues.put(MediaStore.MediaColumns.DATA, absolutePath);
        } else {
            String relativePath = type + File.separator + getAppName();
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
        }

        Uri uri = resolver.insert(mediaTable, contentValues);
        return uri;
    }

    /**
     * create file for saving movie / still image file
     *
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM
     * @param ext  .mp4 .png or .jpeg
     * @return  return null when this app has no writing permission to external storage.
     */
    public static File getCaptureFile(final Context context,
                                            final String type, final String ext) {

        return getCaptureFile(context, type, null, ext);
    }

    /**
     *  need WRITE_EXTERNAL_STORAGE permission
     */
    public static File getCaptureFile(final Context context,
                                            final String type, final String prefix, final String ext) {
        File result = null;
        final String file_name = (TextUtils.isEmpty(prefix) ? getDateTimeString() : prefix + getDateTimeString()) + ext;
        final File dir = getCaptureDir(context, type);
        if (dir != null) {
            dir.mkdirs();
            if (dir.canWrite()) {
                result = dir;
            }
        }
        if (result != null) {
            result = new File(result, file_name);
        }
        return result;
    }

    public static File getCaptureDir(final Context context,
                                           final String type) {
        final File dir = new File(Environment.getExternalStoragePublicDirectory(type), getAppName());
        dir.mkdirs();
        if (dir.canWrite()) {
            return dir;
        }
        return null;
    }

    /**
     * @return formatted string of current time
     */
    public static String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }

    public static String getExternalMounts() {
        String externalpath = null;
        String internalpath = "";

        final Runtime runtime = Runtime.getRuntime();
        try {
            String line;
            final Process proc = runtime.exec("mount");
            final BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            while ((line = br.readLine()) != null) {
//    			Log.i(TAG, "getExternalMounts:" + line);
                if (line.contains("secure")) continue;
                if (line.contains("asec")) continue;

                if (line.contains("fat")) {//external card
                    final String columns[] = line.split(" ");
                    if (columns != null && (columns.length > 1) && !TextUtils.isEmpty(columns[1])) {
                        externalpath = columns[1];
                        if (!externalpath.endsWith("/")) {
                            externalpath = externalpath + "/";
                        }
                    }
                } else if (line.contains("fuse")) {//internal storage
                    final String columns[] = line.split(" ");
                    if (columns != null && columns.length > 1) {
                        internalpath = internalpath.concat("[" + columns[1] + "]");
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
//		Log.i(TAG, "Path of sd card external: " + externalpath);
//		Log.i(TAG, "Path of internal memory: " + internalpath);
        return externalpath;
    }
}
