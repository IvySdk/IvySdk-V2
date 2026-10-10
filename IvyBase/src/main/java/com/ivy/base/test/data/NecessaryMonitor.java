package com.ivy.base.test.data;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class NecessaryMonitor {

    public static class Item {
        public String tag;
        public String info;
        public Event event;

        public Item(String tag, String info) {
            this.tag = tag;
            this.info = info;
        }
    }

    public interface EventListener {
        void onRefresh(List<Item> events, boolean state);
    }

    private EventListener listener = null;
    private Handler handler = new Handler(Looper.getMainLooper());

    private List<Item> list = new ArrayList<>();

    public NecessaryMonitor() {
        list.add(new Item("Facebook", "fb_mobile_purchase"));
        list.add(new Item("Facebook", "AdImpression"));

        list.add(new Item("Firebase", "事件:sdk_iap_purchased"));
        list.add(new Item("Firebase", "事件:gms_ad_paid_event"));
        list.add(new Item("Firebase", "事件:ad_purchase_0_1"));
        list.add(new Item("Firebase", "事件:ad_purchase_0_2"));
        list.add(new Item("Firebase", "事件:ad_purchase_0_3"));
        list.add(new Item("Firebase", "事件:ad_purchase_0_4"));
        list.add(new Item("Firebase", "事件:ad_purchase_0_5"));
        list.add(new Item("Firebase", "事件:ad_purchase_0_6"));
        list.add(new Item("Firebase", "事件:ad_purchase_0_7"));
        list.add(new Item("Firebase", "事件:ad_purchase_0_8"));
        list.add(new Item("Firebase", "事件:ad_purchase_0_9"));
        list.add(new Item("Firebase", "ad_impression"));
        list.add(new Item("Firebase", "ad_impression_revenue"));

        list.add(new Item("Appsflyer", "af_ad_revenue"));
        list.add(new Item("Appsflyer", "af_purchase"));
        list.add(new Item("Appsflyer", "事件:ad_purchase_0_1"));
        list.add(new Item("Appsflyer", "事件:ad_purchase_0_2"));
        list.add(new Item("Appsflyer", "事件:ad_purchase_0_3"));
        list.add(new Item("Appsflyer", "事件:ad_purchase_0_4"));
        list.add(new Item("Appsflyer", "事件:ad_purchase_0_5"));
        list.add(new Item("Appsflyer", "事件:ad_purchase_0_6"));
        list.add(new Item("Appsflyer", "事件:ad_purchase_0_7"));
        list.add(new Item("Appsflyer", "事件:ad_purchase_0_8"));
        list.add(new Item("Appsflyer", "事件:ad_purchase_0_9"));

        list.add(new Item("IVY", "af_ad_revenue"));
        list.add(new Item("IVY", "af_purchase"));

    }

    public void check(Event e) {
        boolean all_collected = true;
        for (Item item : list) {
            if (item.tag.equals(e.tag) && e.info.equals(item.info)) {
                item.event = e;
            }
            if (item.event == null) {
                all_collected = false;
            }
        }
        onStateChanged(all_collected);
    }

    protected void onStateChanged(boolean state){

    }

    private Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            if (listener != null) {
                boolean all_collected = true;
                for (Item item : list) {
                    if (item.event == null) {
                        all_collected = false;
                        break;
                    }
                }
                onStateChanged(all_collected);
                listener.onRefresh(list, all_collected);

                handler.postDelayed(refreshTask, 3000);
            }
        }
    };

    public void setListener(EventListener l) {
        this.listener = l;
        handler.removeCallbacks(refreshTask);
        if (l != null) {
            handler.postDelayed(refreshTask, 1000);
        }
    }

    public void clear() {
        listener = null;
        handler.removeCallbacks(refreshTask);
    }


}
