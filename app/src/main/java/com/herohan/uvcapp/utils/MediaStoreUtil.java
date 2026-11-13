package com.herohan.uvcapp.utils;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MediaStoreUtil {
    private static final String TAG = "MediaStoreUtil";
    private static final List<String> MEDIA_DIRECTORY_LIST = Arrays.asList(
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_DOWNLOADS);

    public static String getFileMimeType(File file) {
        String extension = "";
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            extension = name.substring(lastDot + 1).toLowerCase(Locale.getDefault());
        }
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimeType == null) throw new RuntimeException("no find mimeType");
        return mimeType;
    }

    public static String dateFileName(String prefix, String extension) {
        Locale loc = Locale.getDefault();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS", loc);
        return prefix + sdf.format(System.currentTimeMillis()) + "." + extension;
    }

    public static String dateFileName(String prefix) {
        Locale loc = Locale.getDefault();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS", loc);
        return prefix + sdf.format(System.currentTimeMillis());
    }

    public static File createMediaFile(Context context, String fileName) {
        String mimeType = getFileMimeType(new File(fileName));
        String relativePath;
        if (mimeType.startsWith("video")) {
            relativePath = Environment.DIRECTORY_MOVIES;
        } else if (mimeType.startsWith("image")) {
            relativePath = Environment.DIRECTORY_PICTURES;
        } else {
            relativePath = Environment.DIRECTORY_DOWNLOADS;
        }
        File docs = context.getExternalFilesDir(relativePath);
        return new File(docs, fileName);
    }

    public static Map.Entry<String, Uri> createPublicMediaFile(Context context, String fileName) {
        String mimeType = getFileMimeType(new File(fileName));
        String relativePath;
        if (mimeType.startsWith("video")) {
            relativePath = Environment.DIRECTORY_MOVIES;
        } else if (mimeType.startsWith("image")) {
            relativePath = Environment.DIRECTORY_PICTURES;
        } else {
            relativePath = Environment.DIRECTORY_DOWNLOADS;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            int p = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (p != PackageManager.PERMISSION_GRANTED) {
                throw new RuntimeException("no WRITE_EXTERNAL_STORAGE permission");
            }
            File storePath = Environment.getExternalStoragePublicDirectory(relativePath);
            File appDir = new File(storePath, context.getPackageName());
            if (!appDir.exists()) appDir.mkdirs();
            File targetFile = new File(appDir, fileName);
            return new AbstractMap.SimpleImmutableEntry<>(targetFile.getAbsolutePath(), Uri.fromFile(targetFile));
        }

        Uri contentUri;
        if (mimeType.startsWith("video")) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType.startsWith("image")) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else {
            contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath + "/" + context.getPackageName());
        Uri insertedUri = context.getContentResolver().insert(contentUri, contentValues);

        File storePath = Environment.getExternalStoragePublicDirectory(relativePath);
        File appDir = new File(storePath, context.getPackageName());
        if (!appDir.exists()) appDir.mkdirs();
        String fullPath = new File(appDir, fileName).getAbsolutePath();
        return new AbstractMap.SimpleImmutableEntry<>(fullPath, insertedUri);
    }

    public static void saveMediaStore(Context context, File file, @androidx.annotation.Nullable MediaScannerConnection.OnScanCompletedListener callback) {
        if (!file.exists()) {
            throw new RuntimeException("no find file");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            int p = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (p != PackageManager.PERMISSION_GRANTED) {
                throw new RuntimeException("no WRITE_EXTERNAL_STORAGE permission");
            }
        }

        String mimeType = getFileMimeType(file);
        if (isInMediaDirectoryList(file)) {
            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, new String[]{mimeType}, (path, uri) -> {
                Log.d(TAG, "saveMediaStore success: isInMediaDirectory path-" + path + " mediaUri-" + uri);
                if (callback != null) callback.onScanCompleted(path, uri);
            });
            return;
        }

        Map.Entry<String, Uri> mediaUriData = createPublicMediaFile(context, file.getName());
        String finalPath = mediaUriData.getKey();
        Uri finalUri = mediaUriData.getValue();
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(finalUri); FileInputStream fileInputStream = new FileInputStream(file)) {
            if (outputStream == null) {
                throw new RuntimeException("Failed to open output stream for uri: " + finalUri);
            }
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (Exception e) {
            throw new RuntimeException("Error copying file to MediaStore: " + e);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            MediaScannerConnection.scanFile(context, new String[]{finalPath}, new String[]{mimeType}, (path, uri) -> {
                Log.d(TAG, "saveMediaStore success: scanFile path-" + path + " mediaUri-" + uri);
                if (callback != null) callback.onScanCompleted(path, uri);
            });
            return;
        }
        Log.d(TAG, "saveMediaStore success: path-" + finalPath + " mediaUri-" + finalUri);
        if (callback != null) callback.onScanCompleted(finalPath, finalUri);
    }

    @androidx.annotation.Nullable
    public static Map.Entry<Long, Uri> findRecentMedia(Context context, Boolean isImage) {
        Uri uri = isImage ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED};
        String sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC";
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                long time = cursor.getLong(1);
                return new AbstractMap.SimpleImmutableEntry<>(time, ContentUris.withAppendedId(uri, id));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @androidx.annotation.Nullable
    public static Map.Entry<Long, Uri> findRecentMedia(Context context) {
        Map.Entry<Long, Uri> imagePath = findRecentMedia(context, true);
        Map.Entry<Long, Uri> videoPath = findRecentMedia(context, false);
        if (imagePath == null) {
            return videoPath;
        }
        if (videoPath == null) {
            return imagePath;
        }
        if (imagePath.getKey() >= videoPath.getKey()) {
            return imagePath;
        }
        return videoPath;
    }

    private static boolean isInMediaDirectoryList(File file) {
        String filePath = file.getAbsolutePath();
        for (String s : MEDIA_DIRECTORY_LIST) {
            String dir = Environment.getExternalStoragePublicDirectory(s).getAbsolutePath();
            if (filePath.startsWith(dir)) {
                return true;
            }
        }
        return false;
    }
}