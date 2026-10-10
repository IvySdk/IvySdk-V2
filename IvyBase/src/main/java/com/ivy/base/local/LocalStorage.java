package com.ivy.base.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.ivy.base.App;
import com.tencent.mmkv.MMKV;

public class LocalStorage {

    public static LocalStorage Instance = new LocalStorage();
    private MMKV mmkv = null;

    private LocalStorage() {

    }

    private void importFromSharedPreferences(Context context, String title, int mode) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(title, mode);
        if (sharedPreferences.getAll() != null) {
            if (!sharedPreferences.getAll().isEmpty()) {
                mmkv.importFromSharedPreferences(sharedPreferences);
                sharedPreferences.edit().clear().apply();
            }
        }
    }

    public void init(){
        MMKV.initialize(App.getInstance());
        mmkv = MMKV.mmkvWithID("ev");

        importFromSharedPreferences(App.getInstance(), "prefs", Context.MODE_PRIVATE);
        importFromSharedPreferences(App.getInstance(), "_cyj_promotion", Context.MODE_PRIVATE);
        importFromSharedPreferences(App.getInstance(), "pref", Context.MODE_PRIVATE);
        importFromSharedPreferences(App.getInstance(), "pays", Context.MODE_PRIVATE);
    }

    public void checkInit(){
        if (mmkv == null && App.getInstance() != null){
            init();
        }
    }

    public boolean containsKey(String key) {
        if (mmkv != null) {
            return mmkv.containsKey(key);
        } else {
            return false;
        }
    }

    
    public void removeValueForKey(String key) {
        if (mmkv != null) {
            mmkv.removeValueForKey(key);
        } else {
        }
    }

    
    public void removeValuesForKey(String[] keys) {
        if (mmkv != null) {
            mmkv.removeValuesForKeys(keys);
        } else {
        }
    }

    
    public String[] allKeys() {
        if (mmkv != null) {
            return mmkv.allKeys();
        } else {
            return null;
        }
    }

    
    public String[] allKeysNotExpired() {
        if (mmkv != null) {
            return mmkv.allNonExpireKeys();
        } else {
            return null;
        }
    }

    
    public void clearAll() {
        if (mmkv != null) {
            mmkv.clearAll();
        } else {
        }
    }
    public void encodeInt(String key, int value) {
        if (mmkv != null) {
            mmkv.encode(key, value);
        } else {
        }
    }
    
    public void encodeInt(String key, int value, int expireDurationInSecond) {
        if (mmkv != null) {
            if (expireDurationInSecond > 0) {
                mmkv.encode(key, value, expireDurationInSecond);
            } else {
                mmkv.encode(key, value);
            }
        } else {
        }
    }

    
    public int decodeInt(String key, Integer defaultValue) {
        if (mmkv != null) {
            return mmkv.decodeInt(key, defaultValue != null ? defaultValue : 0);
        } else {
            return defaultValue != null ? defaultValue : 0;
        }
    }
    public void encodeLong(String key, long value) {
        if (mmkv != null) {
            mmkv.encode(key, value);
        } else {
        }
    }
    
    public void encodeLong(String key, long value, int expireDurationInSecond) {
        if (mmkv != null) {
            if (expireDurationInSecond > 0) {
                mmkv.encode(key, value, expireDurationInSecond);
            } else {
                mmkv.encode(key, value);
            }
        } else {
        }
    }

    
    public long decodeLong(String key, Long defaultValue) {
        if (mmkv != null) {
            return mmkv.decodeLong(key, defaultValue != null ? defaultValue : 0L);
        } else {
            return defaultValue != null ? defaultValue : 0L;
        }
    }

    public void encodeString(String key, String value) {
        if (mmkv != null) {
            mmkv.encode(key, value);
        } else {
        }
    }
    
    public void encodeString(String key, String value, int expireDurationInSecond) {
        if (mmkv != null) {
            if (expireDurationInSecond > 0) {
                mmkv.encode(key, value, expireDurationInSecond);
            } else {
                mmkv.encode(key, value);
            }
        } else {
        }
    }

    
    public String decodeString(String key, String defaultValue) {
        if (mmkv == null) {
            return defaultValue;
        }
        return mmkv.decodeString(key, defaultValue);
    }

    public void encodeBoolean(String key, boolean value) {
        if (mmkv != null) {
            mmkv.encode(key, value);
        } else {
        }
    }
    
    public void encodeBoolean(String key, boolean value, int expireDurationInSecond) {
        if (mmkv != null) {
            if (expireDurationInSecond > 0) {
                mmkv.encode(key, value, expireDurationInSecond);
            } else {
                mmkv.encode(key, value);
            }
        } else {
        }
    }

    public boolean decodeBoolean(String key, Boolean defaultValue) {
        if (mmkv != null) {
            return mmkv.decodeBool(key, defaultValue != null ? defaultValue : false);
        } else {
            return defaultValue != null ? defaultValue : false;
        }
    }
    
    public void encodeFloat(String key, float value, int expireDurationInSecond) {
        if (mmkv != null) {
            if (expireDurationInSecond > 0) {
                mmkv.encode(key, value, expireDurationInSecond);
            } else {
                mmkv.encode(key, value);
            }
        } else {
        }
    }
    
    public float decodeFloat(String key, Float defaultValue) {
        if (mmkv != null) {
            return mmkv.decodeFloat(key, defaultValue != null ? defaultValue : 0.0f);
        } else {
            return defaultValue != null ? defaultValue : 0.0f;
        }
    }

    public void encodeDouble(String key, double value) {
        if (mmkv != null) {
            mmkv.encode(key, value);
        } else {
        }
    }

    public void encodeDouble(String key, double value, int expireDurationInSecond) {
        if (mmkv != null) {
            if (expireDurationInSecond > 0) {
                mmkv.encode(key, value, expireDurationInSecond);
            } else {
                mmkv.encode(key, value);
            }
        } else {
        }
    }

    public double decodeDouble(String key, Double defaultValue) {
        if (mmkv != null) {
            return mmkv.decodeDouble(key, defaultValue != null ? defaultValue : 0.0);
        } else {
            return defaultValue != null ? defaultValue : 0.0;
        }
    }



}
