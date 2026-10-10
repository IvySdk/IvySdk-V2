package com.ivy.aihelp;

import android.os.Handler;
import android.os.Looper;

import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.helper.IHelperCallback;
import com.ivy.base.helper.IIHelper;
import com.ivy.base.util.Util;

import net.aihelp.config.ApiConfig;
import net.aihelp.config.UserConfig;
import net.aihelp.config.enums.ShowConversationMoment;
import net.aihelp.event.Acknowledgment;
import net.aihelp.event.AsyncEventListener;
import net.aihelp.event.EventType;
import net.aihelp.init.AIHelpSupport;

import org.json.JSONObject;

public class AIHelp implements IIHelper, IUnProguard {

    private static final String TAG = "AIHelp";

    private IHelperCallback callback;
    private boolean hasInitialized = false;
    private boolean listenUnreadMessage = false;
    private int currentMessageCount = 0;
    private long unreadMessageLoopDuration = 5 * 60 * 1000;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable unreadMessageLoopRunnable;

    private final AsyncEventListener initEventListener = new AsyncEventListener() {
        @Override
        public void onAsyncEventReceived(String jsonEventData, Acknowledgment acknowledgment) {
            handler.post(() -> {
                try {
                    JSONObject json = new JSONObject(jsonEventData);
                    hasInitialized = json.getBoolean("isSuccess");
                    if (!hasInitialized) {
                        String message = json.optString("message");
                        ILog.w(TAG, "init err:" + message);
                    } else {
                        login();
                        AIHelpSupport.fetchUnreadMessageCount();
                    }
                    unreadMessageLoopDuration = json.optLong("unreadMessageLoopDuration", 300) * 1000;
                } catch (Exception e) {
                    ILog.e(TAG, "init response err:" + e.getMessage());
                }
                AIHelpSupport.unregisterAsyncEventListener(EventType.INITIALIZATION);
            });
        }
    };

    private final AsyncEventListener messageEventListener = new AsyncEventListener() {
        @Override
        public void onAsyncEventReceived(String jsonEventData, Acknowledgment acknowledgment) {
            handler.post(() -> {
                try {
                    JSONObject json = new JSONObject(jsonEventData);
                    currentMessageCount = json.optInt("msgCount", 0);
                    ILog.i(TAG, "current unread message count:" + currentMessageCount);
                    if (callback != null) {
                        callback.onUnreadHelperMessageCount(currentMessageCount);
                    }
                } catch (Exception e) {
                    ILog.e(TAG, "listen message err:" + e.getMessage());
                }
            });
        }
    };

    private final AsyncEventListener sessionOpenEventListener = new AsyncEventListener() {
        @Override
        public void onAsyncEventReceived(String jsonEventData, Acknowledgment acknowledgment) {
            handler.post(() -> {
                ILog.i(TAG, "session open!");
                if (callback != null) {
                    callback.onHelperStatus(1);
                }
            });
        }
    };

    private final AsyncEventListener sessionClosedEventListener = new AsyncEventListener() {
        @Override
        public void onAsyncEventReceived(String jsonEventData, Acknowledgment acknowledgment) {
            handler.post(() -> {
                ILog.i(TAG, "session closed! fetch unread message count");
                if (callback != null) {
                    callback.onHelperStatus(0);
                }
                AIHelpSupport.fetchUnreadMessageCount();
            });
        }
    };

    @Override
    public void setup(String data, boolean debug, IHelperCallback callback) {
        this.callback = callback;
        try {
            AIHelpSupport.enableLogging(debug);

            JSONObject config = new JSONObject(data);
            String appId = config.getString("appId");
            String domain = config.getString("domain");
            unreadMessageLoopDuration = config.optLong("unreadMessageLoopDuration", 5 * 60 * 1000L);

            AIHelpSupport.registerAsyncEventListener(EventType.INITIALIZATION, initEventListener);
            AIHelpSupport.registerAsyncEventListener(EventType.MESSAGE_ARRIVAL, messageEventListener);
            AIHelpSupport.registerAsyncEventListener(EventType.SESSION_OPEN, sessionOpenEventListener);
            AIHelpSupport.registerAsyncEventListener(EventType.SESSION_CLOSE, sessionClosedEventListener);

            AIHelpSupport.initialize(App.getInstance(), domain, appId);
        } catch (Exception e) {
            ILog.e(TAG, "setup failed:" + e.getMessage());
        }
    }

    @Override
    public boolean isHelperInitialized() {
        return hasInitialized;
    }

    @Override
    public boolean hasNewHelperMessage() {
        return currentMessageCount > 0;
    }

    @Override
    public void showHelper(String entranceId, String meta, String tags, String welcomeMessage) {
        updateHelperUserInfo(meta, tags);
        ApiConfig.Builder builder = new ApiConfig.Builder();
        builder.setEntranceId(entranceId);
        if (welcomeMessage != null) {
            builder.setWelcomeMessage(welcomeMessage);
        }
        AIHelpSupport.show(builder.build());
    }

    @Override
    public void showHelperSingleFAQ(String faqId, int moment) {
        ShowConversationMoment showMoment;
        if (moment == 0) {
            showMoment = ShowConversationMoment.NEVER;
        } else if (moment == 1) {
            showMoment = ShowConversationMoment.AFTER_MARKING_UNHELPFUL;
        } else if (moment == 2) {
            showMoment = ShowConversationMoment.ONLY_IN_ANSWER_PAGE;
        } else {
            showMoment = ShowConversationMoment.ALWAYS;
        }
        AIHelpSupport.showSingleFAQ(faqId, showMoment);
    }

    @Override
    public void listenHelperUnreadMessageCount(boolean onlyOnce) {
        listenUnreadMessage = true;
        AIHelpSupport.fetchUnreadMessageCount();
        if (!onlyOnce) {
            startUnreadMessageLoop();
        }
    }

    @Override
    public void stopListenHelperUnreadMessageCount() {
        listenUnreadMessage = false;
        if (unreadMessageLoopRunnable != null) {
            handler.removeCallbacks(unreadMessageLoopRunnable);
        }
    }

    @Override
    public void updateHelperUserInfo(String data, String tags) {
        UserConfig.Builder builder = new UserConfig.Builder();
        if (data != null) builder.setCustomData(data);
        if (tags != null) builder.setUserTags(tags);
        AIHelpSupport.updateUserInfo(builder.build());
    }

    @Override
    public void resetHelperUserInfo() {
        AIHelpSupport.resetUserInfo();
    }

    @Override
    public void closeHelper() {
        AIHelpSupport.unregisterAsyncEventListener(EventType.MESSAGE_ARRIVAL);
        AIHelpSupport.close();
    }

    private void startUnreadMessageLoop() {
        unreadMessageLoopRunnable = new Runnable() {
            @Override
            public void run() {
                if (listenUnreadMessage) {
                    AIHelpSupport.fetchUnreadMessageCount();
                    handler.postDelayed(this, unreadMessageLoopDuration);
                }
            }
        };
        handler.postDelayed(unreadMessageLoopRunnable, unreadMessageLoopDuration);
    }

    @Override
    public void updateHelperLanguage(String l) {
        try {
            AIHelpSupport.updateSDKLanguage(l);
        } catch (Exception ignored) {
        }
    }

    private void login() {
        AIHelpSupport.login(Util.roleId());
    }
}
