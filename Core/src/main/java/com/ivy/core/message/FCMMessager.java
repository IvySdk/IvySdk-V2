package com.ivy.core.message;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.ivy.base.App;
import com.ivy.base.message.FCMMessage;
import com.ivy.base.track.EventIDs;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.core.IvySdk;
import com.ivy.track.IvyTrack;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public class FCMMessager implements FCMMessage.IMessage {
    @Override
    public void onPushReach() {
        reportFCMPushReachEvent();
    }

    public static void reportFCMPushReachEvent() {
        try {
            SharedPreferences sp = App.getInstance().getSharedPreferences("fcm_push_data", Context.MODE_PRIVATE);
            String pushIds = sp.getString("push_ids", "");
            sp.edit().putString("push_ids", "").apply();

            String[] args = pushIds.split(",");
            for (String uuid : args) {
                if (!TextUtils.isEmpty(uuid)) {
                    String data = sp.getString(uuid, null);
                    if (!TextUtils.isEmpty(data)) {
                        try{
                            Bundle bundle = getBundle(data);
                            IvyTrack.getInstance().logEvent(EventIDs.EVENT_PUSH_REACH, bundle);
                        }catch (Exception ignored){}
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    @NonNull
    public static Bundle getBundle(String data) throws JSONException {
        Bundle bundle = new Bundle();
        bundle.putString("event_src", EventSrc.EVENT_SRC_SDK);
        bundle.putString("event_type", EventType.EVENT_TYPE_COMMON);
        JSONObject json = new JSONObject(data);
        Iterator<String> iterator =  json.keys();
        while (iterator.hasNext()){
            String key = iterator.next();
            String value = json.getString(key);
            bundle.putString(key, value);
        }
        return bundle;
    }

}



