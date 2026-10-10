package com.ivy.core.helper;

import com.ivy.base.ILog;
import com.ivy.base.helper.IHelperCallback;
import com.ivy.base.helper.IIHelper;

import org.json.JSONObject;

public class IvyHelper implements IIHelper {

    public static final String TAG = "Customer";

    private IIHelper helper;

    @Override
    public void setup(final String data, final boolean debug, final IHelperCallback callback) {
        try {
            JSONObject json = new JSONObject(data);
            if (json.has("AIHelp")) {
                String config = json.getString("AIHelp");
                helper = (IIHelper) Class.forName("com.ivy.aihelp.AIHelp").getDeclaredConstructor().newInstance();
                if (helper != null) {
                    helper.setup(config, debug, callback);
                }
            } else {
                ILog.e(TAG, "helper init failed: no AIHelp config");
            }
        } catch (Exception e) {
            ILog.e(TAG, "helper init failed:" + e.getMessage());
        }
    }

    @Override
    public boolean isHelperInitialized() {
        if (helper != null) {
            return helper.isHelperInitialized();
        } else {
            ILog.w(TAG, "customer service not setup!!!");
            return false;
        }
    }

    @Override
    public boolean hasNewHelperMessage() {
        if (helper != null) {
            return helper.hasNewHelperMessage();
        } else {
            ILog.w(TAG, "customer service not setup!!!");
            return false;
        }
    }

    @Override
    public void showHelper(String entranceId, String meta, String tags, String welcomeMessag) {
        if (helper != null) {
            helper.showHelper(entranceId, meta, tags, welcomeMessag);
        } else {
            ILog.w(TAG, "customer service not setup!!!");
        }
    }

    @Override
    public void showHelperSingleFAQ(String faqId, int moment) {
        if (helper != null) {
            helper.showHelperSingleFAQ(faqId, moment);
        } else {
            ILog.w(TAG, "customer service not setup!!!");
        }
    }

    @Override
    public void listenHelperUnreadMessageCount(boolean onlyOnce) {
        if (helper != null) {
            helper.listenHelperUnreadMessageCount(onlyOnce);
        } else {
            ILog.w(TAG, "customer service not setup!!!");
        }
    }

    @Override
    public void stopListenHelperUnreadMessageCount() {
        if (helper != null) {
            helper.stopListenHelperUnreadMessageCount();
        } else {
            ILog.w(TAG, "customer service not setup!!!");
        }
    }

    @Override
    public void updateHelperUserInfo(String data, String tags) {
        if (helper != null) {
            helper.updateHelperUserInfo(data, tags);
        } else {
            ILog.w(TAG, "customer service not setup!!!");
        }
    }

    @Override
    public void updateHelperLanguage(String l) {
        if (helper != null) {
            helper.updateHelperLanguage(l);
        } else {
            ILog.w(TAG, "customer service not setup!!!");
        }
    }

    @Override
    public void resetHelperUserInfo() {
        if (helper != null) {
            helper.resetHelperUserInfo();
        } else {
            ILog.w(TAG, "customer service not setup!!!");
        }
    }

    @Override
    public void closeHelper() {
        if (helper != null) {
            helper.closeHelper();
        } else {
            ILog.w(TAG, "customer service not setup!!!");
        }
    }
}
