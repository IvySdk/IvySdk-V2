package com.ivy.base;

import android.app.Application;

public class App {

    private static Application _instance = null;

    public static Application getInstance() {
        return _instance;
    }

    public static void setInstance(Application instance) {
        if (_instance == null) {
            _instance = instance;
        }
    }
}
