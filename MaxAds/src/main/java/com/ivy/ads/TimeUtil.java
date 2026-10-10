package com.ivy.ads;

import com.ivy.base.local.LocalStorage;

public class TimeUtil {
    private static final long ONE_DAY_MILLIS = 86400000;
    private static final long ONE_HOUR_MILLIS = 3600000;
    private static volatile int cachedDaysSinceInstall = 0;
    private static volatile int cachedHourSinceInstall = 0;
    private static volatile long installTime = -1;
    private static volatile long nextDayUpdateTime = -1;
    private static volatile long nextHourUpdateTime = -1;

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 2 */
    private static int calculateDaysSinceInstall(long j2) {
        return (int) ((j2 - installTime) / 86400000);
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 2 */
    private static int calculateHoursSinceInstall(long j2) {
        return (int) ((j2 - installTime) / 3600000);
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 2 */
    private static long getInstallTime() {
        if (installTime == -1 || installTime == 1) {
            if (!LocalStorage.Instance.containsKey("s_is_first_open_time")){
                LocalStorage.Instance.encodeLong("s_is_first_open_time", System.currentTimeMillis());
            }
            installTime = LocalStorage.Instance.decodeLong("s_is_first_open_time", 0L);
        }
        return installTime;
    }

    public static int getCurrentDaySinceInstall() {
        long currentTimeMillis = System.currentTimeMillis();
        if (installTime == -1) {
            installTime = getInstallTime();
            nextDayUpdateTime = installTime + 86400000;
        }
        if (currentTimeMillis >= nextDayUpdateTime) {
            cachedDaysSinceInstall = calculateDaysSinceInstall(currentTimeMillis);
            nextDayUpdateTime = installTime + ((cachedDaysSinceInstall + 1L) * 86400000);
        }
        return cachedDaysSinceInstall;
    }

    public static int getCurrentHourSinceInstall() {
        long currentTimeMillis = System.currentTimeMillis();
        if (installTime == -1) {
            installTime = getInstallTime();
            nextHourUpdateTime = installTime + 3600000;
        }
        if (currentTimeMillis >= nextHourUpdateTime) {
            cachedHourSinceInstall = calculateHoursSinceInstall(currentTimeMillis);
            nextHourUpdateTime = installTime + ((cachedHourSinceInstall + 1L) * 3600000);
        }
        return cachedHourSinceInstall;
    }
}
