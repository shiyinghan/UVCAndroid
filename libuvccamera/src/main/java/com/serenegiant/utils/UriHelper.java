package com.serenegiant.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.uvccamera.BuildConfig;

import java.io.File;
import java.util.ArrayList;

/**
 * Uri of external storage for Android10 or later may not work properly
 */
public final class UriHelper {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = UriHelper.class.getSimpleName();

    private UriHelper() {
    }

    /**
     *  convert uri to path
     *
     * @param cr
     * @param uri
     * @return String Null if the path is not found
     */
    @Nullable
    public static String getAbsolutePath(@NonNull final ContentResolver cr, @Nullable final Uri uri) {
        String path = null;
        if (uri != null) {
            try {
                final String[] columns = {MediaStore.Images.Media.DATA};
                final Cursor cursor = cr.query(uri, columns, null, null, null);
                if (cursor != null)
                    try {
                        if (cursor.moveToFirst()) {
                            path = cursor.getString(0);
                        }
                    } finally {
                        cursor.close();
                    }
            } catch (final Exception e) {
//				if (DEBUG) Log.w(TAG, e);
            }
        }
//		Log.v("UriHandler", "getAbsolutePath:" + path);
        return path;
    }

    public static final String[] STANDARD_DIRECTORIES;

    static {
        final ArrayList<String> list = new ArrayList<>();
        list.add(Environment.DIRECTORY_MUSIC);
        list.add(Environment.DIRECTORY_PODCASTS);
        list.add(Environment.DIRECTORY_RINGTONES);
        list.add(Environment.DIRECTORY_ALARMS);
        list.add(Environment.DIRECTORY_NOTIFICATIONS);
        list.add(Environment.DIRECTORY_PICTURES);
        list.add(Environment.DIRECTORY_MOVIES);
        list.add(Environment.DIRECTORY_DOWNLOADS);
        list.add(Environment.DIRECTORY_DCIM);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            list.add(Environment.DIRECTORY_DOCUMENTS);    // API>=19
        }
        STANDARD_DIRECTORIES = list.toArray(new String[0]);
    }

    public static boolean isStandardDirectory(final @NonNull String dir) {
        try {
            for (final String valid : STANDARD_DIRECTORIES) {
                if (valid.equals(dir)) {
                    return true;
                }
            }
        } catch (final Exception e) {
            Log.w(TAG, e);
        }
        return false;
    }

    /**
     * Return path if the Uri can be converted to a local path
     *
     * @param context The context.
     * @param uri     The Uri to query.
     */
    @SuppressLint("NewApi")
    @Nullable
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(@NonNull final Context context, final Uri uri) {
        if (DEBUG) Log.i(TAG, "getPath:uri=" + uri);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            // DocumentProvider
            if (DEBUG) Log.i(TAG, "getPath:isDocumentUri,getAuthority=" + uri.getAuthority());
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                if (DEBUG) Log.i(TAG, "getPath:isDocumentUri,docId=" + docId);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && DEBUG) {
                    Log.i(TAG, "getPath:isDocumentUri,getTreeDocumentId="
                            + DocumentsContract.getTreeDocumentId(uri));
                }
                final String[] split = docId.split(":");
                final String type = split[0];

                if (DEBUG) Log.i(TAG, "getPath:type=" + type);

                if (type != null) {
                    if ("primary".equalsIgnoreCase(type)) {
                        final String path =
                                Environment.getExternalStorageDirectory() + "/";
                        return (split.length > 1) ? path + split[1] : path;
                    } else if ("home".equalsIgnoreCase(type)) {
                        if ((split.length > 1) && isStandardDirectory(split[1])) {
                            return Environment.getExternalStoragePublicDirectory(
                                    split[1]) + "/";
                        }
                        final String path = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOCUMENTS) + "/";
                        return (split.length > 1) ? path + split[1] : path;
                    } else {
                        // search the path from beginning
                        final String primary = Environment.getExternalStorageDirectory().getAbsolutePath();
                        if (DEBUG) Log.i(TAG, "getPath:primary=" + primary);
                        final File[] dirs = context.getExternalFilesDirs(null);
                        final int n = dirs != null ? dirs.length : 0;
                        final StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < n; i++) {
                            final File dir = dirs[i];
                            if (DEBUG) Log.i(TAG, "getPath:" + i + ")dir=" + dir);
                            if ((dir != null) && dir.getAbsolutePath().startsWith(primary)) {
                                // skip primary storage
                                continue;
                            }
                            final String dir_path = dir != null ? dir.getAbsolutePath() : null;
                            if (!TextUtils.isEmpty(dir_path)) {
                                final String[] dir_elements = dir_path.split("/");
                                final int m = dir_elements.length;
                                if ((m > 2) && "storage".equalsIgnoreCase(dir_elements[1])
                                        && type.equalsIgnoreCase(dir_elements[2])) {

                                    boolean found = false;
                                    sb.setLength(0);
                                    sb.append('/').append(dir_elements[1]);
                                    for (int j = 2; j < m; j++) {
                                        if ("Android".equalsIgnoreCase(dir_elements[j])) {
                                            found = true;
                                            break;
                                        }
                                        sb.append('/').append(dir_elements[j]);
                                    }
                                    if (found) {
                                        final File path = new File(new File(sb.toString()), split[1]);
                                        if (DEBUG) Log.i(TAG, "getPath:path=" + path);
                                        // return path directly no matter has permission to access the path or not
                                        return path.getAbsolutePath();
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "unexpectedly type is null");
                }
            } else if (isDownloadsDocument(uri)) {
                // DownloadsProvider
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                // MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                if (contentUri != null) {
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
        } else if (uri != null) {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                // MediaStore (and general)
                if (isGooglePhotosUri(uri)) {
                    return uri.getLastPathSegment();
                }
                return getDataColumn(context, uri, null, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                // File
                return uri.getPath();
            }
        }

        Log.w(TAG, "unexpectedly not found,uri=" + uri);
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    @Nullable
    public static String getDataColumn(@NonNull final Context context,
                                       @NonNull final Uri uri, final String selection, final String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if ((cursor != null) && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(@NonNull final Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(@NonNull final Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(@NonNull final Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(@NonNull final Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

}
