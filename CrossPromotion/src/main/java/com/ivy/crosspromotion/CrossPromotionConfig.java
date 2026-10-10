package com.ivy.crosspromotion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable CrossPromotion configuration. */
public final class CrossPromotionConfig {
    public enum Format {
        BANNER("banner"),
        INTERSTITIAL("interstitial"),
        REWARDED("rewarded");

        private final String value;

        Format(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static Format parse(String value) {
            if ("banner".equals(value)) {
                return BANNER;
            }
            if ("rewarded".equals(value)) {
                return REWARDED;
            }
            return INTERSTITIAL;
        }
    }

    /** One promoted app and all media/text needed to render it. */
    public static final class Ad {
        private final String id;
        private final boolean enabled;
        private final int weight;
        private final String targetPackage;
        private final String title;
        private final String description;
        private final String callToAction;
        private final String icon;
        private final String banner;
        private final String image;
        private final String video;
        private final String image2;
        private final String video2;

        private Ad(JSONObject json) {
            id = string(json, "id");
            enabled = json.optBoolean("enabled", true);
            weight = Math.max(1, json.optInt("weight", 1));
            targetPackage = string(json, "targetPackage");
            title = string(json, "title");
            description = string(json, "description");
            callToAction = defaultString(string(json, "callToAction"), "Install");
            icon = string(json, "icon");
            banner = string(json, "banner");
            image = string(json, "image");
            video = string(json, "video");
            image2 = string(json, "image2");
            video2 = string(json, "video2");
        }

        public String getId() {
            return id;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getWeight() {
            return weight;
        }

        public String getTargetPackage() {
            return targetPackage;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getCallToAction() {
            return callToAction;
        }

        public String getIcon() {
            return icon;
        }

        public String getBanner() {
            return banner;
        }

        public String getImage() {
            return image;
        }

        public String getVideo() {
            return video;
        }

        public String getImage2() {
            return image2;
        }

        public String getVideo2() {
            return video2;
        }

        public boolean hasSecondFullscreenCreative() {
            return !image2.isEmpty() || !video2.isEmpty();
        }

        public String getImage(boolean landscape) {
            return landscape && hasSecondFullscreenCreative() ? image2 : image;
        }

        public String getVideo(boolean landscape) {
            return landscape && hasSecondFullscreenCreative() ? video2 : video;
        }

        public boolean supports(Format format) {
            if (format == Format.BANNER) {
                return !banner.isEmpty();
            }
            if (format == Format.REWARDED) {
                return !video.isEmpty() || !image.isEmpty()
                        || !video2.isEmpty() || !image2.isEmpty();
            }
            return !video.isEmpty() || !image.isEmpty()
                    || !video2.isEmpty() || !image2.isEmpty();
        }
    }

    private final String version;
    private final boolean enabled;
    private final List<Ad> ads;
    private final Map<String, Ad> adsById;

    private CrossPromotionConfig(String version, boolean enabled, List<Ad> ads) {
        this.version = version;
        this.enabled = enabled;
        this.ads = Collections.unmodifiableList(new ArrayList<>(ads));
        Map<String, Ad> indexed = new HashMap<>();
        for (Ad ad : ads) {
            indexed.put(ad.id, ad);
        }
        adsById = Collections.unmodifiableMap(indexed);
    }

    public static CrossPromotionConfig empty() {
        return new CrossPromotionConfig("", false, Collections.emptyList());
    }

    public static CrossPromotionConfig fromJson(String json) throws JSONException {
        if (json == null || json.trim().isEmpty()) {
            return empty();
        }
        return fromJson(new JSONObject(json));
    }

    public static CrossPromotionConfig fromJson(JSONObject root) throws JSONException {
        if (root == null || root.length() == 0) {
            return empty();
        }
        JSONArray array = root.optJSONArray("ads");
        return parseAds(
                root.optString("version", ""),
                root.optBoolean("enabled", root.has("ads") || root.optBoolean("enabled", false)),
                array == null ? new JSONArray() : array);
    }

    private static CrossPromotionConfig parseAds(
            String version,
            boolean enabled,
            JSONArray array
    ) throws JSONException {
        List<Ad> ads = new ArrayList<>();
        Map<String, Boolean> ids = new HashMap<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            Ad ad = new Ad(item);
            if (ad.id.isEmpty()) {
                throw new JSONException("ad.id must not be empty");
            }
            if (ids.put(ad.id, true) != null) {
                throw new JSONException("Duplicate ad.id: " + ad.id);
            }
            if (ad.targetPackage.isEmpty()) {
                throw new JSONException("ad.targetPackage must not be empty: " + ad.id);
            }
            if (ad.banner.isEmpty()
                    && ad.image.isEmpty()
                    && ad.video.isEmpty()
                    && ad.image2.isEmpty()
                    && ad.video2.isEmpty()) {
                throw new JSONException("Ad has no media URL: " + ad.id);
            }
            ads.add(ad);
        }
        return new CrossPromotionConfig(version, enabled, ads);
    }

    public String getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Ad> getAds() {
        return ads;
    }

    public Ad getAd(String id) {
        return adsById.get(id);
    }

    private static String string(JSONObject json, String key) {
        Object value = json.opt(key);
        return value == null || value == JSONObject.NULL ? "" : String.valueOf(value).trim();
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
