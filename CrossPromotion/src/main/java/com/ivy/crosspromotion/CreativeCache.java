package com.ivy.crosspromotion;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class CreativeCache {
    private static final String TAG = "CrossPromotion";

    enum Type {
        IMAGE,
        VIDEO
    }

    private static final int MAX_IMAGE_BYTES = 20 * 1024 * 1024;
    private static final int MAX_VIDEO_BYTES = 100 * 1024 * 1024;
    private static final int MAX_RETRIES = 3;
    private static final String ASSET_PREFIX = "asset:///";

    private final Context context;
    private final File cacheDirectory;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Set<String> activeDownloads = Collections.newSetFromMap(
            new ConcurrentHashMap<>());
    private final Map<String, Entry> pendingEntries = new ConcurrentHashMap<>();

    private ConnectivityManager.NetworkCallback networkCallback;

    CreativeCache(Context context) {
        this.context = context.getApplicationContext();
        cacheDirectory = new File(context.getCacheDir(), "ivy_cross_promotion");
        registerNetworkListener();
    }

    private void registerNetworkListener() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        Log.d(TAG, "Network became available, checking pending creative downloads (" + pendingEntries.size() + " pending)");
                        retryPending();
                    }
                };
                cm.registerNetworkCallback(request, networkCallback);
            }
        } catch (Exception error) {
            Log.w(TAG, "Cannot register network callback: " + error.getMessage());
        }
    }

    private void unregisterNetworkListener() {
        if (networkCallback != null) {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    cm.unregisterNetworkCallback(networkCallback);
                }
            } catch (Exception ignored) {
            }
            networkCallback = null;
        }
    }

    void cache(String url, String configVersion, Type type) {
        if (url == null || url.isEmpty()) {
            return;
        }
        File cached = getCachedFile(url, configVersion, type);
        if (cached != null) {
            return;
        }
        String key = type.name() + ":" + url;
        pendingEntries.put(key, new Entry(url, configVersion, type));
        if (!activeDownloads.add(key)) {
            return;
        }
        enqueue(url, configVersion, type, key, 0, 0);
    }

    private void retryPending() {
        for (Map.Entry<String, Entry> entryMap : pendingEntries.entrySet()) {
            String key = entryMap.getKey();
            Entry entry = entryMap.getValue();
            File cached = getCachedFile(entry.url, entry.configVersion, entry.type);
            if (cached != null) {
                pendingEntries.remove(key);
                continue;
            }
            if (activeDownloads.add(key)) {
                Log.d(TAG, "Re-enqueuing creative download upon network recovery: " + entry.url);
                enqueue(entry.url, entry.configVersion, entry.type, key, 0, 0);
            }
        }
    }

    private void enqueue(
            String url,
            String configVersion,
            Type type,
            String key,
            int attempt,
            long delaySeconds
    ) {
        Runnable task = () -> {
            try {
                File file = cachedFile(url, type);
                if (!file.exists() || file.length() == 0L) {
                    File legacy = legacyCachedFile(url, configVersion, type);
                    if (legacy != null && legacy.exists() && legacy.length() > 0L) {
                        pendingEntries.remove(key);
                        activeDownloads.remove(key);
                        return;
                    }

                    Log.d(TAG, "Start caching creative (attempt " + (attempt + 1) + "): type=" + type + ", url=" + url);

                    // Tier 2: Check APK Assets first before network download
                    String assetPath = resolveAssetPath(url);
                    boolean resolvedFromAsset = false;
                    if (assetPath != null && assetExists(assetPath)) {
                        try {
                            Log.d(TAG, "Found creative in APK assets: " + assetPath + ", copying to disk cache...");
                            copyAsset(assetPath, file, type);
                            resolvedFromAsset = true;
                            Log.d(TAG, "Creative cached from APK assets successfully: " + file.getAbsolutePath() + " (" + file.length() + " bytes)");
                        } catch (Exception assetError) {
                            Log.w(TAG, "Failed to cache from APK asset (" + assetPath + "), will fallback to network: " + assetError.getMessage());
                        }
                    }

                    // Tier 3: Network download (if not present in APK assets or copy failed)
                    if (!resolvedFromAsset) {
                        if (url.startsWith("https://") || url.startsWith("http://")) {
                            Log.d(TAG, "Downloading creative from network: " + url);
                            download(url, file, type);
                            Log.d(TAG, "Creative cached from network successfully: " + file.getAbsolutePath() + " (" + file.length() + " bytes)");
                        } else if (url.startsWith(ASSET_PREFIX)) {
                            throw new IOException("APK asset not found: " + url);
                        } else {
                            throw new IOException("Unsupported creative URL: " + url);
                        }
                    }
                }
                pendingEntries.remove(key);
                activeDownloads.remove(key);
            } catch (Exception error) {
                Log.e(TAG, "Failed to cache creative (" + type + ", attempt " + (attempt + 1) + "): " + url, error);
                if (attempt + 1 < MAX_RETRIES) {
                    long nextDelay = (attempt + 1) * 15L;
                    Log.d(TAG, "Retrying cache for " + url + " in " + nextDelay + "s");
                    enqueue(url, configVersion, type, key, attempt + 1, nextDelay);
                } else {
                    activeDownloads.remove(key);
                    Log.w(TAG, "Creative download exhausted " + MAX_RETRIES + " attempts, waiting for network change: " + url);
                }
            }
        };

        if (delaySeconds <= 0) {
            executor.execute(task);
        } else {
            executor.schedule(task, delaySeconds, TimeUnit.SECONDS);
        }
    }

    File getCachedFile(String url, String configVersion, Type type) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            // Tier 1: Check local disk cache
            File file = cachedFile(url, type);
            if (file.exists() && file.length() > 0L) {
                return file;
            }
            File legacy = legacyCachedFile(url, configVersion, type);
            if (legacy != null && legacy.exists() && legacy.length() > 0L) {
                return legacy;
            }

            // Tier 2: Check APK Assets synchronously for cold-start readiness
            String assetPath = resolveAssetPath(url);
            if (assetPath != null && assetExists(assetPath)) {
                try {
                    copyAsset(assetPath, file, type);
                    if (file.exists() && file.length() > 0L) {
                        return file;
                    }
                } catch (Exception error) {
                    Log.w(TAG, "Failed to copy APK asset synchronously in getCachedFile: " + assetPath, error);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    File getCachedFile(String url, Type type) {
        return getCachedFile(url, "", type);
    }

    static String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        int queryIndex = url.indexOf('?');
        String cleanUrl = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        int slashIndex = cleanUrl.lastIndexOf('/');
        String name = slashIndex >= 0 ? cleanUrl.substring(slashIndex + 1) : cleanUrl;
        return name.trim();
    }

    private boolean isSafeFileName(String name) {
        if (name == null || name.isEmpty() || name.length() > 128) {
            return false;
        }
        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            return false;
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 && dot < name.length() - 1;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format(Locale.US, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private boolean assetExists(String assetPath) {
        if (assetPath == null || assetPath.isEmpty()) {
            return false;
        }
        try (InputStream is = context.getAssets().open(assetPath)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private String resolveAssetPath(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        if (url.startsWith(ASSET_PREFIX)) {
            String sub = url.substring(ASSET_PREFIX.length());
            return (sub.isEmpty() || sub.startsWith("/") || sub.contains("..")) ? null : sub;
        }
        String fileName = extractFileName(url);
        if (isSafeFileName(fileName)) {
            return "crosspromotion/" + fileName;
        }
        return null;
    }

    private File cachedFile(String url, Type type) {
        String fileName = extractFileName(url);
        if (isSafeFileName(fileName)) {
            return new File(cacheDirectory, fileName);
        }
        return new File(cacheDirectory, sha256(type.name() + ":" + url) + ".creative");
    }

    private File legacyCachedFile(String url, String configVersion, Type type) {
        if (configVersion == null || configVersion.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    (configVersion + ":" + type.name() + ":" + url)
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder name = new StringBuilder();
            for (byte value : hash) {
                name.append(String.format(Locale.US, "%02x", value));
            }
            File legacy = new File(cacheDirectory, name + ".creative");
            if (legacy.exists() && legacy.length() > 0L) {
                return legacy;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void copyAsset(String assetPath, File destination, Type type) throws IOException {
        if (assetPath.isEmpty() || assetPath.startsWith("/") || assetPath.contains("..")) {
            throw new IOException("Invalid APK asset path: " + assetPath);
        }
        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs() && !cacheDirectory.exists()) {
            throw new IOException("Cannot create creative cache directory");
        }
        File temporary = temporaryFile(destination);
        try (InputStream input = context.getAssets().open(assetPath)) {
            copyToFile(input, temporary, maxBytes(type));
            validate(temporary, type);
            moveIntoPlace(temporary, destination);
        } finally {
            clearIncomplete(temporary, destination);
        }
    }

    private void download(String source, File destination, Type type) throws IOException {
        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs() && !cacheDirectory.exists()) {
            throw new IOException("Cannot create creative cache directory");
        }

        File temporary = temporaryFile(destination);

        HttpURLConnection connection = (HttpURLConnection) new URL(source).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(type == Type.VIDEO ? 30_000 : 15_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", type == Type.VIDEO ? "video/*" : "image/*");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Creative request failed with HTTP " + status);
            }
            int declaredLength = connection.getContentLength();
            int maxBytes = maxBytes(type);
            if (declaredLength > maxBytes) {
                throw new IOException(
                        "Creative exceeds " + (maxBytes / 1024 / 1024) + " MiB");
            }

            try (InputStream input = connection.getInputStream()) {
                copyToFile(input, temporary, maxBytes);
            }
            validate(temporary, type);
            moveIntoPlace(temporary, destination);
        } finally {
            connection.disconnect();
            clearIncomplete(temporary, destination);
        }
    }

    private File temporaryFile(File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new IOException("Cannot create creative cache directory");
        }
        String tmpName = destination.getName() + "." + System.currentTimeMillis() + "." + Thread.currentThread().getId() + ".tmp";
        File temporary = new File(parent != null ? parent : cacheDirectory, tmpName);
        if (temporary.exists() && !temporary.delete()) {
            throw new IOException("Cannot clear stale creative download: " + temporary.getAbsolutePath());
        }
        return temporary;
    }

    private int maxBytes(Type type) {
        return type == Type.VIDEO ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES;
    }

    private void copyToFile(InputStream input, File destination, int maxBytes) throws IOException {
        try (FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > maxBytes) {
                    throw new IOException(
                            "Creative exceeds " + (maxBytes / 1024 / 1024) + " MiB");
                }
                output.write(buffer, 0, count);
            }
            output.getFD().sync();
        }
    }

    private void validate(File file, Type type) throws IOException {
        if (type == Type.IMAGE && !isImage(file)) {
            throw new IOException("Creative is not a decodable image");
        }
        if (type == Type.VIDEO && !isVideo(file)) {
            throw new IOException("Creative is not a playable video");
        }
    }

    private synchronized void moveIntoPlace(File source, File destination) throws IOException {
        if (destination.exists() && destination.length() > 0L) {
            source.delete();
            return;
        }
        if (!source.renameTo(destination)) {
            copy(source, destination);
            if (!source.delete()) {
                source.deleteOnExit();
            }
        }
    }

    private void clearIncomplete(File temporary, File destination) {
        if (temporary != null && temporary.exists()) {
            try {
                if (!temporary.delete()) {
                    temporary.deleteOnExit();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isImage(File file) {
        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return options.outWidth > 0 && options.outHeight > 0;
    }

    private boolean isVideo(File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION);
            return duration != null && Long.parseLong(duration) > 0L;
        } catch (Exception ignored) {
            return false;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
                // Nothing else to release.
            }
        }
    }

    private void copy(File source, File destination) throws IOException {
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.getFD().sync();
        }
    }

    void shutdown() {
        unregisterNetworkListener();
        executor.shutdownNow();
    }

    private static final class Entry {
        private final String url;
        private final String configVersion;
        private final Type type;

        private Entry(String url, String configVersion, Type type) {
            this.url = url;
            this.configVersion = configVersion;
            this.type = type;
        }
    }
}
