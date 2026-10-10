package com.ivy.track.property;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class IPUtil {
    private String ip;
    private int requestCount = 0;
    private final List<String> servers = Arrays.asList(
            "https://api.ipify.org",
            "https://icanhazip.com"
    );
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentTask;

    public String getIPAddress() {
        return ip;
    }

    public void startLoadIP() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }

        currentTask = executor.submit(() -> {
            try {
                String ipAddress = loadIPTask();
                if (ipAddress == null || ipAddress.isEmpty()) {
                    TimeUnit.SECONDS.sleep(10);
                    startLoadIP(); // Retry after delay
                } else {
                    ip = ipAddress;
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    TimeUnit.SECONDS.sleep(10);
                    startLoadIP(); // Retry after error
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void shutdown() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        executor.shutdown();
    }

    private String loadIPTask() throws Exception {
        try {
            requestCount++;
            int index = requestCount % servers.size();
            String serverUrl = servers.get(index);

            HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10 * 1000);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                return reader.readLine();
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            throw e; // Rethrow to handle in calling method
        }
    }
}
