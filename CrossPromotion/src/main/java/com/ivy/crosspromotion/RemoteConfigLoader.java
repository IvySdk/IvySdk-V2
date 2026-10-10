package com.ivy.crosspromotion;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Loads and persists one last-known-good remote configuration. */
final class RemoteConfigLoader {
    private static final String TAG = "CrossPromotion";

    interface Callback {
        void onSuccess(CrossPromotionConfig config, boolean notModified);

        void onError(Exception error);
    }

    private static final String PREFERENCES = "ivy_cross_promotion_config";
    private static final String KEY_URL = "url";
    private static final String KEY_JSON = "json";
    private static final String KEY_ETAG = "etag";
    private static final int MAX_CONFIG_BYTES = 1024 * 1024;

    private final SharedPreferences preferences;
    private final String configUrl;
    private final Callback callback;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private volatile CrossPromotionConfig currentConfig;

    RemoteConfigLoader(
            Context context,
            String configUrl,
            CrossPromotionConfig initialConfig,
            Callback callback
    ) {
        this.preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        this.configUrl = configUrl;
        this.currentConfig = initialConfig;
        this.callback = callback;
    }

    void setCurrentConfig(CrossPromotionConfig currentConfig) {
        this.currentConfig = currentConfig;
    }

    CrossPromotionConfig loadLastGood() {
        if (!configUrl.equals(preferences.getString(KEY_URL, ""))) {
            return null;
        }
        String json = preferences.getString(KEY_JSON, "");
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return CrossPromotionConfig.fromJson(json);
        } catch (Exception ignored) {
            preferences.edit().clear().apply();
            return null;
        }
    }

    void refresh() {
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        executor.execute(() -> {
            try {
                loadRemote();
            } catch (Exception error) {
                Log.w(TAG, "Failed to load remote config from " + configUrl + ": " + error.getMessage());
                callback.onError(error);
            } finally {
                loading.set(false);
            }
        });
    }

    private void loadRemote() throws Exception {
        Log.d(TAG, "Fetching remote config: " + configUrl);
        URL url = new URL(configUrl);
        String protocol = url.getProtocol();
        if (!"https".equalsIgnoreCase(protocol) && !"http".equalsIgnoreCase(protocol)) {
            throw new IOException("Unsupported config URL: " + configUrl);
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/json");
        String etag = "";
        if (configUrl.equals(preferences.getString(KEY_URL, ""))) {
            etag = preferences.getString(KEY_ETAG, "");
        }
        if (etag == null || etag.isEmpty()) {
            etag = formatVersionEtag(currentConfig == null ? "" : currentConfig.getVersion());
        }
        if (!etag.isEmpty()) {
            connection.setRequestProperty("If-None-Match", etag);
        }

        try {
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.d(TAG, "Remote config not modified (HTTP 304), using cached config");
                CrossPromotionConfig unchanged = preferSaved(loadLastGood(), currentConfig);
                if (unchanged == null) {
                    throw new IOException("Server returned 304 without an active config");
                }
                currentConfig = unchanged;
                callback.onSuccess(unchanged, true);
                return;
            }
            if (status < 200 || status >= 300) {
                throw new IOException("Config request failed with HTTP " + status);
            }
            int declaredLength = connection.getContentLength();
            if (declaredLength > MAX_CONFIG_BYTES) {
                throw new IOException("Config exceeds 1 MiB");
            }

            String json;
            try (InputStream input = connection.getInputStream()) {
                json = readUtf8(input);
            }
            CrossPromotionConfig parsed = CrossPromotionConfig.fromJson(json);
            currentConfig = parsed;
            Log.d(TAG, "Remote config fetched successfully: version=" + parsed.getVersion() + ", ads=" + parsed.getAds().size() + ", enabled=" + parsed.isEnabled());
            String responseEtag = connection.getHeaderField("ETag");
            preferences.edit()
                    .putString(KEY_URL, configUrl)
                    .putString(KEY_JSON, json)
                    .putString(KEY_ETAG, responseEtag == null ? "" : responseEtag)
                    .apply();
            callback.onSuccess(parsed, false);
        } finally {
            connection.disconnect();
        }
    }

    static String formatVersionEtag(String version) {
        if (version == null || version.trim().isEmpty()) {
            return "";
        }
        String safeVersion = version.trim().replace("\\", "").replace("\"", "");
        return safeVersion.isEmpty() ? "" : "\"" + safeVersion + "\"";
    }

    static CrossPromotionConfig preferSaved(
            CrossPromotionConfig saved,
            CrossPromotionConfig current
    ) {
        return saved == null ? current : saved;
    }

    private String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8 * 1024];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > MAX_CONFIG_BYTES) {
                throw new IOException("Config exceeds 1 MiB");
            }
            output.write(buffer, 0, count);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    void shutdown() {
        executor.shutdownNow();
    }
}
