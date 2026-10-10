package com.ivy.base.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.text.TextUtils;

import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShortcutUtil {

    private static final String TAG = "Shortcut";

    public static void addShortcut(String id, String title, String label, String icon) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                if (TextUtils.isEmpty(id)) {
                    return;
                }
                ShortcutManager shortcutManager = App.getInstance().getSystemService(ShortcutManager.class);
                ShortcutInfo.Builder builder = new ShortcutInfo.Builder(App.getInstance(), id);
                if (!TextUtils.isEmpty(title)) {
                    builder.setShortLabel(title);
                }
                if (!TextUtils.isEmpty(label)) {
                    builder.setLongLabel(label);
                }
                if (!TextUtils.isEmpty(icon)) {
                    Icon _icon = iconFromAssetsFilePath(App.getInstance(), icon, id + ".png");
                    if (_icon != null) {
                        builder.setIcon(_icon);
                    }
                }
                Intent launchIntent = App.getInstance().getPackageManager().getLaunchIntentForPackage(Util.packageName());
                if (launchIntent != null) {
                    launchIntent.setAction(Intent.ACTION_VIEW);
                    launchIntent.putExtra("shortcut_action", "shortcut");
                    launchIntent.putExtra("shortcut_id", id);
                    builder.setIntent(launchIntent);
                }
                ShortcutInfo shortcut = builder.build();
                List<ShortcutInfo> list = shortcutManager.getDynamicShortcuts();
                if (list == null) {
                    list = new ArrayList<>();
                } else {
                    Iterator<ShortcutInfo> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        ShortcutInfo info = iterator.next();
                        if (info.getId().equals(id)) {
                            list.remove(info);
                        }
                    }
                }
                list.add(shortcut);
                shortcutManager.setDynamicShortcuts(list);
            } else {
                ILog.w(TAG, "unable create Shortcut, system version too low; < 7.1");
            }
        } catch (Exception e) {
            ILog.w(TAG, "unable create Shortcut:" + e.getMessage());
        }
    }

    public static void deleteShortcut(String id) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                if (TextUtils.isEmpty(id)) {
                    return;
                }
                ShortcutManager shortcutManager = App.getInstance().getSystemService(ShortcutManager.class);
                List<ShortcutInfo> list = shortcutManager.getDynamicShortcuts();
                if (list == null) {
                    list = new ArrayList<>();
                } else {
                    Iterator<ShortcutInfo> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        ShortcutInfo info = iterator.next();
                        if (info.getId().equals(id)) {
                            list.remove(info);
                        }
                    }
                }
                shortcutManager.setDynamicShortcuts(list);
            }
        } catch (Exception e) {
        }
    }

    public static void setShortcutAction(String action) {
        try {
            if (!TextUtils.isEmpty(action)) {
                LocalStorage.Instance.encodeString("__last_shortcut_action__", action, 15 * 60);
            }
        } catch (Exception e) {
            ILog.w(TAG, "set shortcut action failed:" + e.getMessage());
        }
    }

    public static String getShortcutAction() {
        try {
            String data = LocalStorage.Instance.decodeString("__last_shortcut_action__", "");
            LocalStorage.Instance.encodeString("__last_shortcut_action__", "");
            return data;
        } catch (Exception e) {
            ILog.w(TAG, "get shortcut action failed:" + e.getMessage());
        }
        return "";
    }

    private static Icon iconFromAssetsFilePath(Context context, String assetName, String outName) {
        try {
            File outFile = new File(context.getFilesDir(), outName);

            // 拷贝到内部存储（只复制一次）
            if (!outFile.exists()) {
                InputStream is = context.getAssets().open(assetName);
                FileOutputStream fos = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.close();
                is.close();
            }
            return Icon.createWithFilePath(outFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
