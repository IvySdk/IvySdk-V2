package com.ivy.base.notification;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ivy.base.App;
import com.ivy.base.util.Util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class NotificationTaskManager {

    private static NotificationTaskManager _instance = null;
    private Map<String, NotificationTask> tasks = new HashMap<>();
    private Map<String, NotificationTask> bootTasks = new HashMap<>();

    private NotificationTaskManager() {

    }

    public static NotificationTaskManager getInstance() {
        if (_instance == null) {
            synchronized (NotificationTaskManager.class) {
                if (_instance == null) {
                    _instance = new NotificationTaskManager();
                }
            }
        }
        return _instance;
    }

    public void checkNotificationTasks(Context context) {
        boolean _needSave = false;
        try {
            if (tasks.isEmpty()) {
                try {
                    String notify_file_path = context.getCacheDir().getAbsolutePath() + File.separator + "notify.cfb";
                    Util.loadCachedFileData(notify_file_path, data -> {
                        try {
                            Map<String, NotificationTask> _tmp = JSON.parseObject(data, new TypeReference<Map<String, NotificationTask>>() {
                            });
                            if (_tmp != null) {
                                tasks.putAll(_tmp);
                            }
                        } catch (Exception ignored) {
                            Log.e("TAG", ignored.getMessage());
                        }
                    });
                } catch (Exception ignored) {
                    Log.e("TAG", ignored.getMessage());
                    ignored.printStackTrace();
                }
            } else {
                doCheckNotificationTasks(context);
            }
        } catch (Exception ignored) {
            Log.e("TAG", "ALARM Checked:" + ignored.getMessage());
            ignored.printStackTrace();
        }
        if (_needSave) {
            save(context, false);
        }
    }

    private void doCheckNotificationTasks(Context context) {
        boolean _needSave = false;
        try {
            boolean _accept = false;
            String[] keys = tasks.keySet().toArray(new String[0]);
            long current = System.currentTimeMillis();
            Log.e("TAG", "start Checked:" + keys.length);
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                _accept = false;
                if (tasks.containsKey(key)) {
                    NotificationTask task = tasks.get(key);
                    if (task != null) {
                        if (current >= task.nextTime) {
                            _accept = true;
                        } else {
                            _accept = task.nextTime - current <= 5 * 60 * 1000;
                        }
                        if (_accept) {
                            if (task.repeat) {
                                task.nextTime = current + task.interval;
                            } else {
                                tasks.remove(key);
                            }
                            _needSave = true;
                            LocalNotification.show(context, task);
                        }
                        Log.e("TAG", "task Checked:" + key + "; status=" + _accept);
                    } else {
                        tasks.remove(key);
                    }
                }
            }
        } catch (Exception ignored) {
            Log.e("TAG", "ALARM Checked:" + ignored.getMessage());
            ignored.printStackTrace();
        }
        if (_needSave) {
            save(context, false);
        }
    }

    public void checkBootNotificationTasks(Context context) {
        try {
            if (bootTasks.isEmpty()) {
                try {
                    String notify_file_path = context.getCacheDir().getAbsolutePath() + File.separator + "boot_notify.cfb";
                    Util.loadCachedFileData(notify_file_path, data -> {
                        try {
                            Map<String, NotificationTask> _tmp = JSON.parseObject(data, new TypeReference<Map<String, NotificationTask>>() {
                            });
                            if (_tmp != null) {
                                bootTasks.putAll(_tmp);
                            }
                        } catch (Exception ignored) {
                            Log.e("TAG", ignored.getMessage());
                        }
                    });
                } catch (Exception ignored) {
                    Log.e("TAG", ignored.getMessage());
                    ignored.printStackTrace();
                }
            } else {
                doCheckBootNotificationTasks(context);
            }
        } catch (Exception ignored) {
        }
    }

    private void doCheckBootNotificationTasks(Context context) {
        try {
            String[] keys = bootTasks.keySet().toArray(new String[0]);
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                if (bootTasks.containsKey(key)) {
                    NotificationTask task = bootTasks.get(key);
                    LocalNotification.show(context, task);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void addTask(Context context, NotificationTask task, boolean isBoot) {
        try {
            Log.e("TAG", "task added:" + task.tag);
            if (isBoot) {
                bootTasks.put(task.tag, task);
                save(context, true);
            } else {
                tasks.put(task.tag, task);
                save(context, false);
            }
        } catch (Exception ignored) {
        }
    }

    public void removeTask(Context context, String key, boolean isBoot) {
        try {
            if (isBoot) {
                bootTasks.remove(key);
                save(context, true);
            } else {
                tasks.remove(key);
                save(context, false);
            }
        } catch (Exception ignored) {
        }
    }

    public void removeAllTask(Context context, boolean isBoot) {
        try {
            if (isBoot) {
                bootTasks.clear();
                save(context, true);
            } else {
                tasks.clear();
                save(context, false);
            }
        } catch (Exception ignored) {
        }
    }

    private void save(Context context, boolean isBoot) {
        try {
            if (App.getInstance() == null) {

            }
            if (isBoot) {
                String data = JSON.toJSONString(bootTasks);
                Util.cacheDataToFile("boot_notify.cfb", data);
            } else {
                String data = JSON.toJSONString(tasks);
                Util.cacheDataToFile("notify.cfb", data);
            }
        } catch (Exception ignored) {

        }
    }


}
