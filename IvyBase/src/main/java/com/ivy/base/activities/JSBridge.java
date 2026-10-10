package com.ivy.base.activities;

import android.app.Activity;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;

public class JSBridge {
    Activity activity;

    JSBridge(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void triggerKeyDown(int keyCode) {
        activity.runOnUiThread(() -> {
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
            activity.onKeyDown(keyCode, event);
        });
    }
}
