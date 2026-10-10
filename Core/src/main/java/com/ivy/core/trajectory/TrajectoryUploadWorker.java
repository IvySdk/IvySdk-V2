package com.ivy.core.trajectory;


import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TrajectoryUploadWorker extends Worker {
    private static final String TAG = "TrajectoryUploadWorker";
    private final OkHttpClient httpClient;

    private String url = null;

    private static final MediaType MEDIA_TYPE_PLAIN = MediaType.parse("text/plain; charset=utf-8");
    private static final MediaType MEDIA_TYPE_GZIP = MediaType.parse("application/gzip");

    public TrajectoryUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        url = TrajectoryManager.getUrl(context);
        if (TextUtils.isEmpty(url)){
            return Result.retry();
        }

        File logFile = new File(context.getFilesDir(), "upload_queue.log");
        if (!logFile.exists() || logFile.length() == 0) {
            return Result.success();
        }

        File tempFile = new File(context.getFilesDir(), "upload_queue.log.tmp");
        if (tempFile.exists()) {
            tempFile.delete();
        }
        if (!logFile.renameTo(tempFile)) {
            return Result.retry();
        }

        boolean hasFailure = false;

        // 逐行读取临时文件进行上传
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile));
             FileWriter logWriter = new FileWriter(logFile, true)) { // 用于写回失败的任务

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|", 7);
                if (parts.length < 6) continue;
                String level_id = parts[0];
                String app_id = parts[1];
                String userId = parts[2];
                String userTier = parts[3];
                String appVersion = parts[4];
                String date = parts[5];
                String filePath = parts[6];
                File dataFile = new File(filePath);
                System.out.println("发送结果:" + line);
                // 执行网络上传
                boolean isSuccess = uploadToServer(app_id, level_id, userId, userTier, appVersion, date, dataFile);

                if (isSuccess) {
                    // 上传成功：安全删除本地的大数据 .dat 文件
                    if (dataFile.exists()) {
                        dataFile.delete();
                    }
                } else {
                    // 上传失败：将该行写回主日志文件，等待下一次重试
                    logWriter.write(line + "\n");
                    hasFailure = true;
                }
                System.out.println("发送结果:" + isSuccess);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing upload queue", e);
            hasFailure = true;
        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }

        // 如果有任何一条失败，返回 retry 让 WorkManager 择期重试
        return hasFailure ? Result.retry() : Result.success();
    }

    private boolean uploadToServer(String appId, String levelId, String userId, String userTier, String appVersion, String date, File dataFile) {
        if (!dataFile.exists()) {
            return true; // 文件不存在说明已经被删了，视为成功
        }
        try {
            byte[] compressedData = readFileToBytes(dataFile);
            if (compressedData == null) return false;

            String base64Body = new String(compressedData, StandardCharsets.UTF_8);

//            byte[] gzipBytes = Base64.decode(base64Body, Base64.NO_WRAP);
//            byte[] decompressedBytes = decompressGzip(gzipBytes);
//            String data = new String(decompressedBytes, StandardCharsets.UTF_8);

            RequestBody body = RequestBody.create(base64Body, MEDIA_TYPE_PLAIN);

            Request.Builder builder =  new Request.Builder()
                    .url(url)
                    .header("Content-Type", "text/plain")
//                    .header("Content-Encoding", "gzip")
                    .header("X-App-ID", appId)
                    .header("X-Level-ID", levelId)
                    .header("X-User-ID", userId)
                    .header("X-Date", date)
                    .header("X-App-Version", appVersion)
                    .post(body);

            if (!TextUtils.isEmpty(userTier)){
                builder.header("X-User-Tier", userTier);
            }

            Request request = builder.build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.code() == 200) {
                    return true;
                } else {
                    Log.w(TAG, "Server error code: " + response.code() + ", message: " + response.message());
                }
            }

        } catch (Throwable e) {
            Log.w(TAG, "OkHttp network request failed", e);
        }
        return false;
    }

    public static byte[] decompressGzip(byte[] gzipBytes) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(gzipBytes);
             GZIPInputStream gzipIS = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzipIS.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }

            return bos.toByteArray();
        }
    }

    public byte[] readFileToBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }

    private byte[] compressFileToGzip(File file) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos);
             FileInputStream fis = new FileInputStream(file)) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzip.write(buffer, 0, len);
            }
            gzip.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

}