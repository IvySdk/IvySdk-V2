package com.ivy.track;

import com.ivy.base.track.TrackPlatform;
import com.ivy.base.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class EventChannelUtil {

    private Map<String, List<TrackPlatform>> channels = new HashMap<>();

//    private List<TrackPlatform> allPlatforms = Arrays.asList(
//            TrackPlatform.FACEBOOK,
//            TrackPlatform.FIREBASE,
//            TrackPlatform.APPSFLYER,
//            TrackPlatform.THINkING_DATA
//    );

    public void setup(JSONObject json) {
        loopConfig(json, false);
        if (json.has("country")) {
            JSONArray array = json.optJSONArray("country");
            if (array != null && array.length() > 0) {
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JSONObject item = array.optJSONObject(i);
                        if (item != null) {
                            loopConfig(item, true);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void loopConfig(JSONObject json, boolean checkCountry) {
        boolean needCheck = true;
        if (checkCountry) {
            if (json.has("country")) {
                needCheck = false;
                try {
                    JSONArray countries = json.optJSONArray("country");
                    String country = Util.countryCode();
                    if (countries != null && countries.length() > 0) {
                        for (int i = 0; i < countries.length(); i++) {
                            String value = countries.optString(i);
                            if (value.equalsIgnoreCase(country)) {
                                needCheck = true;
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (!needCheck) {
            return;
        }

        Iterator<String> iter = json.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            if (key.equalsIgnoreCase("country")) {
                continue;
            } else {
                String conf = json.optString(key);
                List<TrackPlatform> list = new ArrayList<>();
                channels.put(key, list);

                String[] confs = conf.split("");
                for (int index = 0; index < confs.length; index++) {
                    String value = confs[index];
                    if ("1".equals(value)) {
                        switch (index) {
                            case 0:
                                list.add(TrackPlatform.APPSFLYER);
                                break;
                            case 1:
                                list.add(TrackPlatform.FIREBASE);
                                break;
                            case 2:
                                list.add(TrackPlatform.FACEBOOK);
                                break;
                            case 3:
                                list.add(TrackPlatform.THINKING_DATA);
                                break;
                            case 4:
                                list.add(TrackPlatform.AXON);
                                break;
                        }
                    }
                }
            }
        }
    }

    public List<TrackPlatform> getTrackPlatform(String event) {
        return channels.get(event);
    }
}
