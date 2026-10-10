package com.ivy.playgames;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.GameStatsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.playergameevent.PlayerGameEvent;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.Callback;
import com.ivy.base.Callback2;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.games.IPlayGameEvents;
import com.ivy.base.games.auth.AuthPlatforms;
import com.ivy.base.games.auth.IAuthResponse;
import com.ivy.base.games.auth.IAuthResult;
import com.ivy.base.games.auth.IPlayGameAuth;
import com.ivy.base.http.HttpUtil;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.util.rx.Func;
import com.ivy.base.util.rx.Observer;
import com.ivy.base.util.rx.RxAndroid;
import com.ivy.base.util.rx.Schedulers;
import com.ivy.base.util.rx.SimpleRxJust;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.Iterator;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class PlayGamesAuth implements IPlayGameAuth, IUnProguard {

    private static final String TAG = "PlayGames";

    private String appId = "";
    private String web_client_id = null;
    private IAuthResult globalAuthResult = null;
    private boolean isAuthenticated = false;
    private String userInfo = "{}";
    private String serverAuthCode = "";
    private String verifyUrl = null;
    private boolean autoSignIn = false;
    private boolean hasInitialized = false;

    private IPlayGameEvents iPlayGameEvents = null;

    private static final String ARCHIVE_KEY = "sdk_logic_ts";

    @Override
    public void setup(String appId, JSONObject jsonObject, boolean debug, IAuthResult authResult) {
        this.appId = appId;
        this.globalAuthResult = authResult;
        this.web_client_id = jsonObject.optString("web_client_id");
        this.autoSignIn = jsonObject.optBoolean("auto_login", false);
        if (web_client_id == null || web_client_id.isEmpty()) {
            ILog.e(TAG, "web_client_id can not be empty!!!");
        }
        this.verifyUrl = jsonObject.optString("verify-url");

        if (autoSignIn) {
            initialize(globalAuthResult);
        }
    }

    @Override
    public boolean isServiceAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(App.getInstance());
        if (resultCode == ConnectionResult.SUCCESS) {
            // 1. 服务正常，继续执行业务逻辑
            return true;
        } else if (apiAvailability.isUserResolvableError(resultCode)) {
            // 2. 可修复错误（如未安装、需更新、被禁用）：弹出系统标准对话框引导用户修复
            // apiAvailability.getErrorDialog(activity, resultCode, REQUEST_CODE).show();
            return false;
        } else {
            // 3. 不可修复错误（如签名无效、设备不支持）
            ILog.e(TAG, "此设备不支持 Google Play 服务，状态码: " + resultCode);
            return false;
        }
    }

    @Override
    public void setListener(IPlayGameEvents listener) {
        this.iPlayGameEvents = listener;
    }

    @Override
    public void initialize(IAuthResponse authCallback, boolean checkLoginState) {
        if (!isServiceAvailable()) {
            if (authCallback != null) {
                authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, false, null, "service not available");
            }
            return;
        }
        if (!hasInitialized) {
            hasInitialized = true;
            PlayGamesSdk.initialize(App.getInstance());
        }
        if (checkLoginState) {
//            doLoadLoginStatus(authCallback);
            login(authCallback);
        }
    }

    private void initialize(IAuthResponse authCallback) {
        initialize(authCallback, true);
    }

    @Override
    public String requireServerSideAuthCode() {
        return serverAuthCode;
    }

    @Override
    public String getPlayGamesUserInfo() {
        return userInfo;
    }

    // ===================== 成就 =====================
    @Override
    public void unlockAchievement(String achievementId) {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getAchievementsClient(activity).unlock(achievementId);
            TestManager.getInstance().addEvent("PlayGames", "解锁成就,achievementId=" + achievementId, EventState.NORMAL, "achievementId=" + achievementId);
        } else {
            ILog.w(TAG, "unable to unlock achievement! invalid activity instance");
            TestManager.getInstance().addEvent("PlayGames", "解锁成就失败，无效Activity", EventState.ERROR, "achievementId=" + achievementId);
        }
    }

    @Override
    public void increaseAchievement(String achievementId, int step) {
        if (step < 0) return;
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getAchievementsClient(activity).increment(achievementId, step);
            TestManager.getInstance().addEvent("PlayGames", "更新成就进度，achievementId=" + achievementId + "增量=" + step, EventState.NORMAL, "achievementId=" + achievementId + ";step=" + step);
        } else {
            ILog.w(TAG, "unable to increase achievement! invalid activity instance");
            TestManager.getInstance().addEvent("PlayGames", "更新成就进度失败，无效Activity", EventState.ERROR, "achievementId=" + achievementId + ";step=" + step);
        }
    }

    @Override
    public void setAchievementStep(String achievementId, int step) {
        if (step < 0) return;
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getAchievementsClient(activity).setSteps(achievementId, step);
            TestManager.getInstance().addEvent("PlayGames", "更新成就step，achievementId=" + achievementId + "step=" + step, EventState.NORMAL, "achievementId=" + achievementId + ";step=" + step);
        } else {
            ILog.w(TAG, "unable to increase achievement! invalid activity instance");
            TestManager.getInstance().addEvent("PlayGames", "更新成就step失败，无效Activity", EventState.ERROR, "achievementId=" + achievementId + ";step=" + step);
        }
    }

    @Override
    public void showAchievement() {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getAchievementsClient(activity).getAchievementsIntent().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    try {
                        activity.startActivityForResult(task.getResult(), 99992);
                    } catch (Exception e) {
                        ILog.w(TAG, "show achievement failed:" + e.getMessage());
                    }
                } else {
                    ILog.w(TAG, "show achievement failed; " + task.getException().getMessage());
                }
            });
        } else {
            ILog.w(TAG, "unable to show achievement! invalid activity instance");
        }
    }

    // ===================== 排行榜 =====================
    @Override
    public void showLeaderboards() {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getLeaderboardsClient(activity).getAllLeaderboardsIntent().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    try {
                        activity.startActivityForResult(task.getResult(), 99991);
                    } catch (Exception e) {
                        ILog.w(TAG, "show leaderboard failed:" + e.getMessage());
                    }
                } else {
                    ILog.w(TAG, "show leaderboard failed; " + task.getException().getMessage());
                }
            });
        } else {
            ILog.w(TAG, "unable to show leaderboards! invalid activity instance");
        }
    }

    @Override
    public void showLeaderboard(String leaderboardId) {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getLeaderboardsClient(activity).getLeaderboardIntent(leaderboardId).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    try {
                        activity.startActivityForResult(task.getResult(), 99991);
                    } catch (Exception e) {
                        ILog.w(TAG, "show leaderboard" + leaderboardId + " failed:" + e.getMessage());
                    }
                } else {
                    ILog.w(TAG, "show leaderboard" + leaderboardId + " failed; " + task.getException().getMessage());
                }
            });
        } else {
            ILog.w(TAG, "unable to show leaderboard:" + leaderboardId + "! invalid activity instance");
        }
    }

    @Override
    public void updateLeaderboard(String leaderboardId, long score) {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getLeaderboardsClient(activity).submitScore(leaderboardId, score);
        } else {
            ILog.w(TAG, "unable to update leaderboard:" + leaderboardId + "! invalid activity instance");
        }
    }

    @Override
    public void loadLeaderboardScore(String leaderboardId) {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getLeaderboardsClient(activity).loadCurrentPlayerLeaderboardScore(leaderboardId, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC).addOnCompleteListener(new OnCompleteListener<AnnotatedData<LeaderboardScore>>() {
                @Override
                public void onComplete(@NonNull Task<AnnotatedData<LeaderboardScore>> task) {
                    if (task.isSuccessful()) {
                        LeaderboardScore score = task.getResult().get();
                        if (iPlayGameEvents != null && score != null) {
                            iPlayGameEvents.onLeaderBoardScore(leaderboardId, score.getDisplayScore(), score.getDisplayRank());
                        } else {
                            ILog.w(TAG, "unable to load leaderboard:" + leaderboardId + " score! data empty");
                        }
                    } else {
                        try {
                            ILog.w(TAG, "load leaderboard:" + leaderboardId + " score failed!" + task.getException().getMessage());
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        } else {
            ILog.w(TAG, "unable to update leaderboard:" + leaderboardId + "! invalid activity instance");
        }
    }

    // ===================== 存档 =====================
    public void showArchives() {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            int maxNumberOfSavedGamesToShow = 100;
            PlayGames.getSnapshotsClient(activity).getSelectSnapshotIntent("See My Saves", true, true, maxNumberOfSavedGamesToShow).addOnCompleteListener(new OnCompleteListener<Intent>() {
                @Override
                public void onComplete(@NonNull Task<Intent> task) {
                    if (task.isSuccessful()) {
                        try {
                            activity.startActivityForResult(task.getResult(), 99993);
                        } catch (Exception e) {
                            ILog.w(TAG, "show archives failed:" + e.getMessage());
                        }
                    } else {
                        ILog.w(TAG, "show leaderboard archives failed; " + task.getException().getMessage());
                    }
                }
            });
        } else {
            ILog.w(TAG, "unable to show archives! invalid activity instance");
        }
    }

    @Override
    public void save(String archiveName, String actionId, String payload) {
        if (TextUtils.isEmpty(archiveName)) {
            return;
        }
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(activity);
            snapshotsClient.open(archiveName, true).addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                @Override
                public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {
                    if (task.isSuccessful()) {
                        SnapshotsClient.DataOrConflict<Snapshot> r = task.getResult();
                        try {
                            resolve(snapshotsClient, r, snapshot -> {
                                ILog.i(TAG, "resolve archives success");
                                try {
                                    long nextTs = loadLocalTs(activity, archiveName) + 1;
                                    byte[] wrapped = wrapPayload(payload.getBytes(StandardCharsets.UTF_8), nextTs);
                                    snapshot.getSnapshotContents().writeBytes(wrapped);
                                    SnapshotMetadataChange meta = new SnapshotMetadataChange.Builder().build();
                                    snapshotsClient.commitAndClose(snapshot, meta).addOnCompleteListener(new OnCompleteListener<SnapshotMetadata>() {
                                        @Override
                                        public void onComplete(@NonNull Task<SnapshotMetadata> task) {
                                            if (task.isSuccessful()) {
                                                saveLocalTs(activity, archiveName, nextTs);
                                                if (iPlayGameEvents != null) {
                                                    iPlayGameEvents.onWriteArchive(archiveName, actionId, true);
                                                }
                                                ILog.i(TAG, "archive save success:" + archiveName + "  " + actionId);
                                            } else {
                                                if (iPlayGameEvents != null) {
                                                    iPlayGameEvents.onWriteArchive(archiveName, actionId, false);
                                                }
                                                ILog.i(TAG, "archive save failed:" + (task.getException() != null ? task.getException().getMessage() : ""));
                                            }
                                        }
                                    });
                                } catch (Exception e) {

                                }
                            });
                        } catch (Exception e) {
                            ILog.w(TAG, "unable to open archives! " + e.getMessage());
                            if (iPlayGameEvents != null) {
                                iPlayGameEvents.onWriteArchive(archiveName, actionId, false);
                            }
                        }
                    } else {
                        ILog.w(TAG, "unable to open archives! read failed:" + (task.getException() != null ? task.getException().getMessage() : ""));
                        if (iPlayGameEvents != null) {
                            iPlayGameEvents.onWriteArchive(archiveName, actionId, false);
                        }
                    }
                }
            });
        } else {
            ILog.w(TAG, "unable to read archives! invalid activity instance");
            if (iPlayGameEvents != null) {
                iPlayGameEvents.onWriteArchive(archiveName, actionId, false);
            }
        }
    }

    @Override
    public void read(String archiveName) {
        if (TextUtils.isEmpty(archiveName)) {
            return;
        }
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(activity);
            snapshotsClient.open(archiveName, true).addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                @Override
                public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {
                    if (task.isSuccessful()) {
                        SnapshotsClient.DataOrConflict<Snapshot> r = task.getResult();
                        try {
                            resolve(snapshotsClient, r, snapshot -> {
                                ILog.i(TAG, "resolve archives success");
                                try {
                                    Meta meta = readMeta(snapshot);
                                    if (iPlayGameEvents != null) {
                                        iPlayGameEvents.onReadArchive(archiveName, meta == null ? "" : meta.payload);
                                    }
                                } catch (Exception e) {
                                    ILog.w(TAG, "unable to parse archives content" + e.getMessage());
                                    if (iPlayGameEvents != null) {
                                        iPlayGameEvents.onReadArchive(archiveName, "");
                                    }
                                }
                            });
                        } catch (Exception e) {
                            ILog.w(TAG, "unable to open archives! " + e.getMessage());
                            if (iPlayGameEvents != null) {
                                iPlayGameEvents.onReadArchive(archiveName, "");
                            }
                        }
                    } else {
                        ILog.w(TAG, "unable to open archives! read failed:" + (task.getException() != null ? task.getException().getMessage() : ""));
                        if (iPlayGameEvents != null) {
                            iPlayGameEvents.onReadArchive(archiveName, "");
                        }
                    }
                }
            });
        } else {
            ILog.w(TAG, "unable to read archives! invalid activity instance");
            if (iPlayGameEvents != null) {
                iPlayGameEvents.onReadArchive(archiveName, "");
            }
        }
    }

    public void resolve(SnapshotsClient sc, SnapshotsClient.DataOrConflict<Snapshot> r, IPG<Snapshot> ipg) throws Exception {
        if (!r.isConflict()) {
            if (ipg != null) {
                ipg.call(r.getData());
            }
            return;
        }
        SnapshotsClient.SnapshotConflict conflict = r.getConflict();

        Snapshot a = conflict.getConflictingSnapshot();
        Snapshot b = conflict.getSnapshot();

        Meta ma = readMeta(a);
        Meta mb = readMeta(b);

        Snapshot chosen = chooseByMeta(a, b, ma, mb);
        sc.resolveConflict(conflict.getConflictId(), chosen).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (ipg != null) {
                    ipg.call(chosen);
                }
            } else {
                if (ipg != null) {
                    ipg.call(null);
                }
            }
        });
    }

    private Meta readMeta(Snapshot s) throws Exception {
        byte[] raw = s.getSnapshotContents().readFully();
        JSONObject root = new JSONObject(new String(raw, StandardCharsets.UTF_8));

        Meta m = new Meta();
        m.logicalTs = root.optLong(ARCHIVE_KEY, 0);
        m.payload = root.optString("payload", "");
        return m;
    }

    private Snapshot chooseByMeta(Snapshot a, Snapshot b, Meta ma, Meta mb) {
        if (ma == null && mb != null) return b;
        if (mb == null && ma != null) return a;
        if (ma != null && mb != null) {
            if (ma.schema != mb.schema) return ma.schema > mb.schema ? a : b;
            if (ma.logicalTs != mb.logicalTs) return ma.logicalTs > mb.logicalTs ? a : b;
        }
        // 兜底：云端版本
        return b;
    }

    private byte[] wrapPayload(byte[] payload, long ts) throws Exception {
        JSONObject root = new JSONObject();
        root.put("logical_ts", ts);
        root.put("payload", new String(payload, StandardCharsets.UTF_8));
        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    private long loadLocalTs(Context context, String prefix) {
        return context.getSharedPreferences(prefix + "_sdk_pg_logic_ts", Context.MODE_PRIVATE).getLong("ts", 0);
    }

    private void saveLocalTs(Context context, String prefix, long ts) {
        context.getSharedPreferences(prefix + "_sdk_pg_logic_ts", Context.MODE_PRIVATE).edit().putLong("ts", ts).apply();
    }

    private static class Meta {
        int schema;
        long logicalTs;
        String payload;
    }

    // ===================== 活动 =====================
    @Override
    public void submitEvent(String eventId, int increment) {
        if (TextUtils.isEmpty(eventId)) {
            return;
        }
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getEventsClient(activity).increment(eventId, increment);
            ILog.i(TAG, "submit event " + eventId + "");
        } else {
            ILog.w(TAG, "submit event " + eventId + " failed! invalid activity");
        }

    }

    @Override
    public void loadEvent(String eventId) {
        if (TextUtils.isEmpty(eventId)) {
            if (iPlayGameEvents != null) {
                iPlayGameEvents.onLoadEvents("[]");
            }
            return;
        }
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getEventsClient(activity).loadByIds(true, eventId).addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
                @Override
                public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
                    if (task.isSuccessful()) {
                        try {
                            JSONArray array = new JSONArray();
                            for (Event event : task.getResult().get()) {
                                JSONObject obj = new JSONObject();
                                obj.put("id", event.getEventId());
                                obj.put("name", event.getName());
                                obj.put("value", event.getValue());
                                obj.put("icon", event.getIconImageUri() == null ? event.getIconImageUri().toString() : "");
                                array.put(obj);
                            }
                            if (iPlayGameEvents != null) {
                                iPlayGameEvents.onLoadEvents(array.toString());
                            }
                        } catch (Exception e) {
                            ILog.w(TAG, "load events failed! " + e.getMessage());
                        }
                    } else {
                        ILog.w(TAG, "load event" + eventId + " failed! " + (task.getException() != null ? task.getException().getMessage() : ""));
                        if (iPlayGameEvents != null) {
                            iPlayGameEvents.onLoadEvents("[]");
                        }
                    }
                }
            });
        } else {
            ILog.w(TAG, "submit event " + eventId + " failed! invalid activity");
            if (iPlayGameEvents != null) {
                iPlayGameEvents.onLoadEvents("[]");
            }
        }
    }

    @Override
    public void loadEvents() {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getEventsClient(activity).loadByIds(true).addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
                @Override
                public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
                    if (task.isSuccessful()) {
                        try {
                            JSONArray array = new JSONArray();
                            for (Event event : task.getResult().get()) {
                                JSONObject obj = new JSONObject();
                                obj.put("id", event.getEventId());
                                obj.put("name", event.getName());
                                obj.put("value", event.getValue());
                                obj.put("icon", event.getIconImageUri() == null ? event.getIconImageUri().toString() : "");
                                array.put(obj);
                            }
                            if (iPlayGameEvents != null) {
                                iPlayGameEvents.onLoadEvents(array.toString());
                            }
                        } catch (Exception e) {
                            ILog.w(TAG, "load events failed! " + e.getMessage());
                        }
                    } else {
                        ILog.w(TAG, "load events failed! " + (task.getException() != null ? task.getException().getMessage() : ""));
                        if (iPlayGameEvents != null) {
                            iPlayGameEvents.onLoadEvents("[]");
                        }
                    }
                }
            });
        } else {
            ILog.w(TAG, "load events failed! invalid activity");
            if (iPlayGameEvents != null) {
                iPlayGameEvents.onLoadEvents("[]");
            }
        }
    }

    //游戏统计数据
    @Override
    public void submitGameEvent(String eventId, JSONObject property) {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayerGameEvent.Builder builder = new PlayerGameEvent.Builder(eventId);
            if (property != null) {
                Iterator<String> iter = property.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    try {
                        Object value = property.get(key);
                        if (value instanceof Long || value instanceof Integer) {
                            long v = ((Number) value).longValue();
                            builder.addProperty(key, v);
                        } else if (value instanceof Double || value instanceof Float) {
                            double v = ((Number) value).doubleValue();
                            builder.addProperty(key, v);
                        } else if (value instanceof Boolean) {
                            boolean v = Boolean.parseBoolean(value.toString());
                            builder.addProperty(key, v);
                        } else if (value instanceof String) {
                            String v = String.valueOf(value);
                            builder.addProperty(key, v);
                        } else {
                            throw new InvalidParameterException("invalid data type; only double、long、string、boolean supported");
                        }
                    } catch (Exception e) {
                        ILog.e(TAG, "event property:" + key + " add failed;check type");
                    }
                }
            }
            TestManager.getInstance().addEvent("PlayGames", "数据已记录", EventState.NORMAL, "activity对象无效");
            GameStatsClient client = PlayGames.getGameStatsClient(activity);
            client.recordEvent(builder.build());
            client.requestEventsUpload();
            TestManager.getInstance().addEvent("PlayGames", "数据已上传", EventState.NORMAL, "事件名:" + eventId + ";\n数据:\n" + property);
        } else {
            TestManager.getInstance().addEvent("PlayGames", "上传数据失败", EventState.ERROR, "activity对象无效");
        }
    }

    // ===================== 好友 =====================

    // ===================== 玩家数据 =====================

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
//        if (intent != null) {
//            if (requestCode == 99993) {
//                if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)) {
//                    // Load a snapshot.
//                    SnapshotMetadata snapshotMetadata = intent.getParcelableExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA);
//                    String mCurrentSaveName = snapshotMetadata.getUniqueName();
//                    if (iPlayGameEvents != null) {
//                        iPlayGameEvents.onArchiveSelected(mCurrentSaveName);
//                    }
//                } else if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)) {
//                    // Create a new snapshot named with a unique string
//                    String unique = new BigInteger(281, new Random()).toString(13);
//                    String mCurrentSaveName = "snapshot-" + unique;
//                    createArchive(mCurrentSaveName);
//                }
//            }
//        }
    }

    @Override
    public void autoLogin() {
        // play games 在启动时主动触发登录
    }

    @Override
    public void loadLoginStatus() {
        // 这里在需要时触发 doLoadLoginStatus
    }

    /**
     * isAuthenticated 仅能查询play games本地缓存登录状态
     * 如果在PlayGames软件中删除登录历史，此方法会返回错误状态，需要在检测结果中再次调用登录确保状态一致
     *
     * @param authCallback
     */
    @Deprecated
    private void doLoadLoginStatus(IAuthResponse authCallback) {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getGamesSignInClient(activity).isAuthenticated().addOnCompleteListener(task -> {
                boolean localIsAuthenticated = task.isSuccessful() && task.getResult().isAuthenticated();
                ILog.i(TAG, "load sign status:" + localIsAuthenticated);
                ILog.i(TAG, "re-call sign in from load sign status,avoid inconsistencies between PlayGames local login status and the server");
                if (localIsAuthenticated) {
                    PlayGames.getGamesSignInClient(activity).signIn().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            ILog.i(TAG, "re-call sign in success");
                            loadServerAuthCode((state, msg) -> {
                                isAuthenticated = state;
                                if (state) {
                                    authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, true, null, null);
                                } else {
                                    authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, false, null, msg);
                                }
                            });
                        } else {
                            isAuthenticated = false;
                            ILog.i(TAG, "re-call sign in failed:" + (task1.getException() != null ? task1.getException().getMessage() : ""));
                            authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, false, null, "");
                        }
                    });
                } else {
                    authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, false, null, task.getException() != null ? task.getException().getMessage() : "failed");
                }
            });
        } else {
            ILog.w(TAG, "unable to log sign status! invalid activity instance");
            authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, false, null, "invalid activity");
        }
    }

    @Override
    public void login(IAuthResponse authCallback) {
        IAuthResponse callback = (authCallback != null) ? authCallback : globalAuthResult;
        if (!hasInitialized) {
            TestManager.getInstance().addEvent("PlayGames_Auth", "初始化", EventState.WARNING, "");
            initialize(authCallback, false);
        }
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getGamesSignInClient(activity).signIn().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ILog.i(TAG, "sign in response success");
                    TestManager.getInstance().addEvent("PlayGames_Auth", "登录成功，开始换取auth code", EventState.NORMAL, "");
                    loadServerAuthCode((state, msg) -> {
                        isAuthenticated = state;
                        TestManager.getInstance().addEvent("PlayGames_Auth", state ? "登录成功" : "登录失败", state ? EventState.NORMAL : EventState.ERROR, msg == null ? "" : msg);
                        if (state) {
                            if (callback != null)
                                callback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, true, null, null);
                        } else {
                            if (callback != null)
                                callback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, false, null, msg);
                        }
                    });
                } else {
                    TestManager.getInstance().addEvent("PlayGames_Auth", "登录失败", EventState.ERROR, (task.getException() != null ? task.getException().getMessage() : ""));
                    isAuthenticated = false;
                    ILog.i(TAG, "sign in failed:" + (task.getException() != null ? task.getException().getMessage() : ""));
                    if (callback != null) {
                        callback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, false, null, task.getException() != null ? task.getException().getMessage() : "failed");
                    }
                }
            });
        } else {
            ILog.w(TAG, "unable to log sign status! invalid activity instance");
            TestManager.getInstance().addEvent("PlayGames_Auth", "登录失败，Activity对象为null，检查初始化逻辑", EventState.ERROR, "Activity=null");
            isAuthenticated = false;
            if (callback != null) {
                callback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, false, null, "invalid activity");
            }
        }
    }

    @Override
    public void logout() {
        ILog.w(TAG, "play games can not sign out");
    }

    @Override
    public void logout(Callback callback) {

    }

    @Override
    public boolean isTokenValid() {
        return false;
    }

    @Override
    public boolean getLoginStatus() {
        return isAuthenticated;
    }

    @Override
    public String getUserInfo() {
        if ("{}".equals(userInfo)) {
            loadUserInfo(null);
        }
        return userInfo;
    }

    @Override
    public String getUserId() {
        try {
            return new JSONObject(userInfo).getString("id");
        } catch (Exception e) {
            return "";
        }
    }

    private void loadUserInfo(UserInfoCallback callback) {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            PlayGames.getPlayersClient(activity).getCurrentPlayer().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    try {
                        Player player = task.getResult();
                        JSONObject json = new JSONObject();
                        json.put("id", player.getPlayerId());
                        json.put("name", player.getDisplayName());
                        Uri iconUri = player.getIconImageUri();
                        json.put("photo", iconUri != null ? iconUri.toString() : "");
                        json.put("token", serverAuthCode);
                        userInfo = json.toString();
                        ILog.i(TAG, "get player success:" + userInfo);
                        if (callback != null) callback.onResult(true);
                    } catch (Exception e) {
                        ILog.e(TAG, "get player err:" + e.getMessage());
                        userInfo = "{}";
                        if (callback != null) callback.onResult(false);
                    }
                } else {
                    userInfo = "{}";
                    ILog.e(TAG, "get player err:" + (task.getException() != null ? task.getException().getMessage() : ""));
                    if (callback != null) callback.onResult(false);
                }
            });
        } else {
            ILog.i(TAG, "require server auth code failed:invalid activity instance");
            if (callback != null) callback.onResult(false);
        }
    }

    @Override
    public void refreshAuthCode(Callback2<Boolean> callback) {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            if (web_client_id != null && !web_client_id.isEmpty()) {
                PlayGames.getGamesSignInClient(activity).requestServerSideAccess(web_client_id, true).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        serverAuthCode = task.getResult();
                        ILog.i(TAG, "require server auth code success:" + serverAuthCode);
                        callback.onResult(true);
                    } else {
                        ILog.i(TAG, "require server auth code failed:" + (task.getException() != null ? task.getException().getMessage() : ""));
                        callback.onResult(false);
                    }
                });
            } else {
                ILog.i(TAG, "require server auth code failed:invalid web client id");
                callback.onResult(false);
            }
        } else {
            ILog.i(TAG, "require server auth code failed:invalid activity instance");
            callback.onResult(false);
        }
    }

    private void loadServerAuthCode(ServerAuthCallback callback) {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            if (web_client_id != null && !web_client_id.isEmpty()) {
                PlayGames.getGamesSignInClient(activity).requestServerSideAccess(web_client_id, false).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        serverAuthCode = task.getResult();
                        TestManager.getInstance().addEvent("PlayGames_Auth", "换取auth code 成功", EventState.NORMAL, "" + serverAuthCode);
                        ILog.i(TAG, "require server auth code success:" + serverAuthCode);
                        if (verifyUrl == null || verifyUrl.isEmpty()) {
                            loadUserInfo(state -> callback.onResult(state, null));
                        } else {
                            TestManager.getInstance().addEvent("PlayGames_Auth", "开始中台校验", EventState.NORMAL, "" + serverAuthCode);
                            new SimpleRxJust<>(serverAuthCode).map(new Func<String, Boolean>() {
                                @Override
                                public Boolean call(String s) throws Exception {
                                    return verifyLogin(verifyUrl, s);
                                }
                            }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<Boolean>() {
                                @Override
                                public void onSuccess(Boolean result) {
                                    TestManager.getInstance().addEvent("PlayGames_Auth", result ? "中台校验成功" : "中台校验失败", result ? EventState.NORMAL : EventState.ERROR, "" + serverAuthCode);
                                    if (result) {
                                        loadUserInfo(state -> callback.onResult(state, null));
                                    } else {
                                        if (callback != null) {
                                            callback.onResult(false, "verify failed");
                                        }
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {
                                    TestManager.getInstance().addEvent("PlayGames_Auth", "中台校验失败", EventState.ERROR, "" + serverAuthCode);
                                    if (callback != null) {
                                        callback.onResult(false, "verify failed");
                                    }
                                }
                            });
                        }
                    } else {
                        TestManager.getInstance().addEvent("PlayGames_Auth", "换取auth code 失败，无效的web client id，检查配置", EventState.ERROR, (task.getException() != null ? task.getException().getMessage() : ""));
                        ILog.i(TAG, "require server auth code failed:" + (task.getException() != null ? task.getException().getMessage() : ""));
                        callback.onResult(false, task.getException() != null ? task.getException().getMessage() : "failed");
                    }
                });
            } else {
                TestManager.getInstance().addEvent("PlayGames_Auth", "换取auth code 失败，无效的web client id，检查配置", EventState.ERROR, "无效的web client id");
                ILog.i(TAG, "require server auth code failed:invalid web client id");
                callback.onResult(false, "invalid web client id");
            }
        } else {
            ILog.i(TAG, "require server auth code failed:invalid activity instance");
            callback.onResult(false, "invalid activity instance");
        }
    }

    private boolean verifyLogin(String url, String token) {
        try {
            JSONObject json = new JSONObject();
            json.put("is_encrypt", false);
            JSONObject data = new JSONObject();
            data.put("app_id", appId);
            data.put("token", token);
            json.put("data", data);

            OkHttpClient okHttpClient = HttpUtil.getInstance().getOkHttpClient();
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody postBody = RequestBody.create(json.toString(), mediaType);
            Request request = new Request.Builder().url(url).post(postBody).build();
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                String str = response.body().string();
                String userId = new JSONObject(str).getString("data");
                return userId != null && !userId.isEmpty();
            }
        } catch (Exception e) {
            ILog.e(TAG, "verify login failed:" + e.getMessage());
        }
        return false;
    }

    // callback interfaces
    private interface UserInfoCallback {
        void onResult(boolean state);
    }

    private interface ServerAuthCallback {
        void onResult(boolean state, String msg);
    }

}
