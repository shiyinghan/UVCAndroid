package com.herohan.uvcapp.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility class for video recording related operations.
 */
public final class VideoUtil {
    private static final String TAG = "VideoUtil";

    private VideoUtil(){}

    /** Gets the absolute path from a Uri. */
    @Nullable
    public static String getAbsolutePathFromUri(@NonNull ContentResolver resolver,
                                                @NonNull Uri contentUri) {
        Cursor cursor = null;
        try {
            // We should include in any Media collections.
            String[] proj;
            int columnIndex;
            proj = new String[]{MediaStore.Video.Media.DATA};
            cursor = resolver.query(contentUri, proj, null, null, null);
            if (cursor == null) {
                throw new NullPointerException();
            }
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } catch (RuntimeException e) {
            Log.e(TAG, String.format(
                    "Failed in getting absolute path for Uri %s with Exception %s",
                    contentUri.toString(), e.toString()));
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
