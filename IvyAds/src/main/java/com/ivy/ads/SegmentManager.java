package com.ivy.ads;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SegmentManager {

    private static final String TAG = "SegmentManager";

    private int session = 0;

    private final Map<AdType, List<SegmentAdUnit>> adUnits = new HashMap<>();
    private final Map<Integer, List<Integer>> segments = new HashMap<>();
//    private MMKV mmkv = null;

//    public SegmentManager(MMKV mmkv) {
//        this.mmkv = mmkv;
//        if (mmkv != null) {
//            session = mmkv.decodeInt("blockads_session", 0) + 1;
//        }
//    }

    public String segmentStr() {
        StringBuilder str = new StringBuilder();
        try {
            for (Map.Entry<Integer, List<Integer>> entry : segments.entrySet()) {
                for (Integer value : entry.getValue()) {
                    str.append(",");
                    str.append(entry.getKey().toString());
                    str.append(":");
                    str.append(value.toString());
                }
            }
            if (str.length() > 0) {
                str.deleteCharAt(0);
            }
        } catch (Exception ignored) {
        }
        return str.toString();
    }

    public void parse(String bxId, String configStr) {
        try {
            JSONObject json = new JSONObject(configStr);
            JSONObject config = json.optJSONObject(bxId);
            if (config == null) {
                config = json.optJSONObject("default");
                if (config != null) {
                    parseAdConfig(config);
                }
            } else {
                parseAdConfig(config);
            }
        } catch (Exception e) {
            Log.i(TAG, "block ads parse failed:" + e.getMessage());
        }
    }

    private void parseAdConfig(JSONObject bxConfig) {
        try {
            JSONObject adConfig = bxConfig.getJSONObject("max");
            if (adConfig.has("inter")) {
                JSONArray c = adConfig.optJSONArray("inter");
                if (c != null) {
                    adUnits.put(AdType.INTERSTITIAL, parseAdItem(AdType.INTERSTITIAL, c));
                }
            }
            if (adConfig.has("video")) {
                JSONArray c = adConfig.optJSONArray("video");
                if (c != null) {
                    adUnits.put(AdType.REWARDED, parseAdItem(AdType.REWARDED, c));
                }
            }
            if (adConfig.has("banner")) {
                JSONArray c = adConfig.optJSONArray("banner");
                if (c != null) {
                    adUnits.put(AdType.BANNER, parseAdItem(AdType.BANNER, c));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<SegmentAdUnit> parseAdItem(AdType adType, JSONArray json) throws Exception {
        List<SegmentAdUnit> result = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            JSONObject item = json.getJSONObject(i);
            String name = item.optString("name", "1");
            String adUnit = item.getString("adUnit");
            if (adUnit == null || adUnit.trim().isEmpty()) {
                continue;
            }
            String amazonSlotId = null;
            if (item.has("amazonSlotId")) {
                amazonSlotId = item.optString("amazonSlotId");
            }
            if (item.has("segmentConfigs")) {
                JSONArray segmentConfigs = item.getJSONArray("segmentConfigs");
                parseSegmentConfigs(adType, segmentConfigs);
            }
            result.add(new SegmentAdUnit(name, adUnit, amazonSlotId));
        }
        return result;
    }

    private void parseSegmentConfigs(AdType adType, JSONArray json) throws Exception {
        for (int i = 0; i < json.length(); i++) {
            JSONObject item = json.getJSONObject(i);
            if (item.has("segment") && item.has("installHours")) {
                // 限时segment
                int configInstallHours = item.optInt("installHours", 0);
                long installTime = System.currentTimeMillis();
                try {
//                    installTime = IvySdk.getActivity().getPackageManager().getPackageInfo(IvySdk.getActivity().getPackageName(), 0).firstInstallTime;
                } catch (Exception ignore) {
                }
                int installHours = (int) ((System.currentTimeMillis() - installTime) / 1000.0 / 60.0 / 60.0);
                if (installHours <= configInstallHours) {
                    JSONArray segment = item.getJSONArray("segment");
                    for (int i1 = 0; i1 < segment.length(); i1++) {
                        String[] segmentItem = segment.getString(i1).split(":");
                        if (segmentItem.length == 2) {
                            int segmentKey = Integer.parseInt(segmentItem[0]);
                            if (segments.containsKey(segmentKey)) {
                                List<Integer> list = segments.get(segmentKey);
                                if (list == null) {
                                    list = new ArrayList<>();
                                }
                                list.add(Integer.parseInt(segmentItem[1]));
                                segments.put(segmentKey, list);
                            } else {
                                List<Integer> list = new ArrayList<Integer>();
                                list.add(Integer.parseInt(segmentItem[1]));
                                segments.put(segmentKey, list);
                            }
                        }
                    }
                    return;
                }
            } else {
                // ecpm segment
                JSONObject segment = item.getJSONObject("segment");
                if (segment.has("tag") && segment.has("ecpm") && segment.has("index")) {
                    int tag = segment.getInt("tag");
                    JSONArray ecpms = segment.getJSONArray("ecpm");
                    JSONArray indexs = segment.getJSONArray("index");
                    if (ecpms.length() == indexs.length()) {
                        int avgEcpmLastNSession = item.optInt("avgEcpmLastNSession", 0);
                        double ecpm = getEcpm(adType, avgEcpmLastNSession);
                        double preEcpm = -1.0;
                        for (int i1 = 0; i1 < ecpms.length(); i1++) {
                            double curEcpm = ecpms.getDouble(i1);
                            if (ecpm > preEcpm && ecpm <= curEcpm) {
                                int segmentValue = indexs.getInt(i1);
                                if (segments.containsKey(tag)) {
                                    List<Integer> list = segments.get(tag);
                                    if (list == null) {
                                        list = new ArrayList<>();
                                    }
                                    list.add(segmentValue);
                                    segments.put(tag, list);
                                } else {
                                    List<Integer> list = new ArrayList<Integer>();
                                    list.add(segmentValue);
                                    segments.put(tag, list);
                                }
                            }
                            preEcpm = curEcpm;
                        }
                    }
                }
            }
        }
    }

    public void updateEcpm(String adType, double value) {
//        if (mmkv != null) {
//            String key = "blockads_" + session + "_" + adType;
//            String revenueKey = key + "_revenue";
//            double newValue = mmkv.decodeDouble(revenueKey, 0.0) + value;
//            mmkv.encode(revenueKey, newValue);
//
//            String countKey = key + "_times";
//            long newValueTimes = mmkv.decodeLong(countKey, 0) + 1;
//            mmkv.encode(countKey, newValueTimes);
//        }
    }

    private double getEcpm(AdType adType, int count) {
        long times = 0;
        double revenue = 0.0;
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                int _session = session - i - 1;
                if (_session >= 0) {
                    String revenueKey = "blockads_" + _session + "_" + adType.value() + "_revenue";
                    String countKey = "blockads_" + _session + "_" + adType.value() + "_times";
//                    revenue += mmkv != null ? mmkv.decodeDouble(revenueKey, 0.0) : 0;
//                    times += mmkv != null ? mmkv.decodeLong(countKey, 0) : 0;
                }
            }
            if (times > 0) {
                return revenue / times;
            }
        }
        return 0.0;
    }

    public boolean isValid(){
        return !adUnits.isEmpty();
    }

    public List<SegmentAdUnit> getAdUnits(AdType adType){
        return adUnits.get(adType);
    }

    public Map<Integer, List<Integer>> getSegments() {
        return segments;
    }

//    public JSONObject format() {
//        try {
//            JSONObject parent = new JSONObject();
//
//            //interstitial
//            List<SegmentAdUnit> interstitials = adUnits.get(AdType.INTERSTITIAL);
//            if (interstitials != null){
//                JSONArray full = new JSONArray();
//                for (SegmentAdUnit segmentAdUnit : interstitials) {
//                    JSONObject item = new JSONObject();
//                    item.put("provider", "applovinmax");
//                    JSONObject p = new J
//                }
//
//            }
//            //rewarded
//
//
//            //banner
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }


}

