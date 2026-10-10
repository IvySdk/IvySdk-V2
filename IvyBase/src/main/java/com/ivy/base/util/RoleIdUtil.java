package com.ivy.base.util;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class RoleIdUtil {

    public static void write(Context context, String roleId) {
        writeMediaFile(context, roleId);
        writeDownloadFile(context, roleId);
    }

    private static void writeMediaFile(Context context, String content) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, ".install_id.dat");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Android/media/" + context.getPackageName());
            Uri uri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }
            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable t) {

        }
    }

    private static void writeDownloadFile(Context context, String content) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, ".install_id.dat");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }
            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable t) {

        }
    }

    public static String read(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }
        Uri mediaUri = findMediaFile(context);
        String result = readContent(context, mediaUri);
        if (!TextUtils.isEmpty(result)) {
            return result;
        }
        Uri downloadUri = findDownloadFile(context);
        return readContent(context, downloadUri);
    }

    private static String readContent(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) {
                return null;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString("UTF-8");
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }

    private static Uri findMediaFile(Context context) {
        try {
            String relativePath = "Android/media/" + context.getPackageName() + "/";
            String[] projection = {MediaStore.Downloads._ID};
            String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?" + " AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?";
            String[] args = {".install_id.dat", relativePath};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try (Cursor cursor = context.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, args, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID));
                        return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    private static Uri findDownloadFile(Context context) {
        try {
            String[] projection = {MediaStore.Downloads._ID};
            String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
            String[] args = {".install_id.dat"};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try (Cursor cursor = context.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, args, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID));
                        return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }

}
