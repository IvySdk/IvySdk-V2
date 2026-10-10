package com.ivy.core.trajectory;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.time.TimeUtil;
import com.ivy.base.util.Util;
import com.ivy.core.IvySdk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

public class TrajectoryManager {
    private static final String TAG = "TrajectoryManager";
    private static final String DIR_TRAJECTORIES = "trajectories";
    private static final String FILE_QUEUE = "upload_queue.log";

    private static final int MAX_DATA_SIZE_BYTES = 40 * 1024;
    private static final int MAX_LOG_SIZE_BYTES = 5 * 1024 * 1024;

    private static volatile TrajectoryManager instance;
    private final Context context;
    private final ExecutorService diskExecutor;

    private String appsflyer_id = null;
    private String app_id = null;

    private List<Integer> blockList = new ArrayList();


    private TrajectoryManager() {
        this.context = App.getInstance();
        this.diskExecutor = Executors.newSingleThreadExecutor();
        this.app_id = IvySdk.getInstance().getAppId();
        String data = LocalStorage.Instance.decodeString("_trajectory_blocked_", "");
        blockList = formatBlockLevelList(data);
        saveUrl();
    }

    public static TrajectoryManager getInstance() {
        if (instance == null) {
            synchronized (TrajectoryManager.class) {
                if (instance == null) {
                    instance = new TrajectoryManager();
                }
            }
        }
        return instance;
    }

    public static void saveUrl() {
        try {
            String trajectoryUrl = IvySdk.getInstance().getGridConfig("trajectory.url", null);
            if (TextUtils.isEmpty(trajectoryUrl)) {
                ILog.w("trajectory", "had not set trajectory upload url");
                return;
            }
            SharedPreferences preferences = App.getInstance().getSharedPreferences("trajectory.share", Context.MODE_PRIVATE);
            preferences.edit().putString("trajectory.url", trajectoryUrl).apply();
        } catch (Throwable ignored) {
        }
    }

    public static String getUrl(Context context) {
        try {
            SharedPreferences preferences = context.getSharedPreferences("trajectory.share", Context.MODE_PRIVATE);
            String url = preferences.getString("trajectory.url", null);
            return url;
        } catch (Throwable ignored) {
        }
        return null;
    }


    /**
     * 屏蔽指定关卡轨迹上传
     *
     * @param data 关卡列表，ex:  1,2,3
     */
    public void updateBlockTrajectoryLevels(String data) {
        List<Integer> temp = formatBlockLevelList(data);
        blockList.clear();
        blockList.addAll(temp);
        LocalStorage.Instance.encodeString("_trajectory_blocked_", data);
    }

    private List<Integer> formatBlockLevelList(String str) {
        if (TextUtils.isEmpty(str)) return new ArrayList<>();
        String[] args = str.split(",");
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String item = args[i];
            if (TextUtils.isEmpty(item)) continue;
            try {
                list.add(Integer.valueOf(args[i]));
            } catch (Exception ignored) {
            }
        }
        return list;
    }


    /**
     *
     * @param userTier 用户分层
     * @param data     关卡数据
     */
    public void uploadTrajectory(int level_id, String userTier, String data) {
        if (blockList.contains(level_id)) {
            ILog.d(TAG, "level:" + level_id + " blocked!");
            return;
        }
        byte[] compressedData = compressStringToGzip(data);
        if (compressedData == null) {
            ILog.d(TAG, "level:" + level_id + " compress failed");
            return;
        }

        String base64Body = Base64.encodeToString(compressedData, Base64.NO_WRAP);

        int dataSizeBytes = base64Body.getBytes(StandardCharsets.UTF_8).length;
        ILog.d(TAG, "Trajectory data size=" + dataSizeBytes + "B");
        //如果超过 40KB（40960字节），直接拦截不记录
        if (dataSizeBytes > MAX_DATA_SIZE_BYTES) {
            ILog.w(TAG, "Trajectory data is too large (" + (dataSizeBytes / 1024) + " KB), dropped.");
            return;
        }
        if (TextUtils.isEmpty(appsflyer_id)) {
            appsflyer_id = LocalStorage.Instance.decodeString("appsflyer_id", "");
        }
        if (TextUtils.isEmpty(appsflyer_id)) {
            Log.w(TAG, "can not find appsflyer id, reject upload");
            return;
//            appsflyer_id = "000000";
        }
        if (TextUtils.isEmpty(userTier)) {
            userTier = "";
        }
        String finalUserTier = userTier;
        diskExecutor.execute(() -> {
            try {
                String fileName = "traj_" + System.currentTimeMillis() + "_" + Math.random() + ".dat";
                File dir = new File(context.getFilesDir(), DIR_TRAJECTORIES);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File dataFile = new File(dir, fileName);
                writeStringToFile(dataFile, base64Body);
                String date = TimeUtil.getInstance().getFormattedUTCTime("yyyy-MM-dd");
                String app_version = Util.versionName();
                String logEntry = level_id + "|" + app_id + "|" + appsflyer_id + "|" + finalUserTier + "|" + app_version + "|" + date + "|" + dataFile.getAbsolutePath() + "\n";
                File logFile = new File(context.getFilesDir(), FILE_QUEUE);
                if (logFile.exists()) {
                    if (logFile.length() > MAX_LOG_SIZE_BYTES) {
                        try {
                            if (dataFile.exists()) {
                                dataFile.delete();
                            }
                        } catch (Throwable ignored) {
                        }
                        return;
                    }
                }
                appendStringToFile(logFile, logEntry);

                // 触发后台 Worker 进行上传
                triggerUploadWorker();

            } catch (Exception e) {
                Log.e(TAG, "Failed to save trajectory locally", e);
            }
        });
    }

    private void writeStringToFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private synchronized void appendStringToFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private byte[] compressStringToGzip(String data) {
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length() / 4);
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            byte[] inputBytes = data.getBytes(StandardCharsets.UTF_8);
            gzip.write(inputBytes, 0, inputBytes.length);
            gzip.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void triggerUploadWorker() {
        // 有网络时才触发上传
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(TrajectoryUploadWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork("TrajectoryUploadWorker", ExistingWorkPolicy.KEEP, uploadWorkRequest);
    }
}