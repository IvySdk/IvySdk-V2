package com.ivy.ads.events;

import android.os.Bundle;

import com.ivy.base.track.ITrack;
import com.tencent.mmkv.MMKV;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


class AdPurchase {
    public double value;
    public String event;

    public AdPurchase(String event, double value) {
        this.event = event;
        this.value = value;
    }

}

public class AdPurchaseEventHandler {


    private boolean repeat = true;
    private List<AdPurchase> list = new ArrayList<>();

    public AdPurchaseEventHandler(JSONObject jsonObject) {
        try {
            repeat = jsonObject.optBoolean("repeat", true);
            JSONArray array = jsonObject.getJSONArray("step");
            if (array.length() > 0) {
                for (int i = 0; i < array.length(); i++) {
                    double value = array.getDouble(i);
                    String eventName = "ad_purchase_" + value;
                    eventName = eventName.replaceAll("\\.", "_");
                    AdPurchase item = new AdPurchase(eventName, value);
                    list.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void check(ITrack logger, MMKV mmkv, float revenue) {
        if (mmkv != null && list.size() > 0) {
            Iterator<AdPurchase> iterator = list.iterator();
            while (iterator.hasNext()) {
                AdPurchase item = iterator.next();
                float value = mmkv.decodeFloat(item.event, 0.0f) + revenue;
                if (value >= item.value) {
//                    logger.logToConversation(item.event, new Bundle());
                    if (repeat) {
                        value = 0.0f;
                    }
                }
                mmkv.encode(item.event, value);
            }
        }
    }


}
