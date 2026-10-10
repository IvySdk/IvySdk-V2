package com.ivy.track.property;

import android.text.TextUtils;

public class EmulatorDetector {

    public boolean isEmulator() {
        return mayOnEmulatorViaQEMU() || isEmulatorFromAbi();
    }

    private boolean mayOnEmulatorViaQEMU() {
        return "1".equals(getProp("ro.kernel.qemu"));
    }

    private boolean isEmulatorFromAbi() {
        String var0 = "";
        String propValue = getProp("ro.product.cpu.abi");
        if (propValue == null) {
            return false;
        }
        var0 = propValue;
        return !TextUtils.isEmpty(var0) && var0.contains("x86");
    }

    private String getProp(String var0) {
        try {
            Class<?> var10000;
            boolean var10001;
            try {
                var10000 = Class.forName("android.os.SystemProperties");
            } catch (Exception var4) {
                var10001 = false;
                return null;
            }

            Class<?> var1 = var10000;
            String var5 = "get";

            Class<?>[] var10002;
            try {
                var10002 = new Class[1];
            } catch (Exception var3) {
                var10001 = false;
                return null;
            }

            Class<?>[] var10003 = var10002;
            byte var10004 = 0;

            try {
                var10003[var10004] = String.class;
                return (String) var10000.getMethod(var5, var10002).invoke(var1, var0);
            } catch (Exception var2) {
                var10001 = false;
                return null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}