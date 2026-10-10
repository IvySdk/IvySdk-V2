package com.ivy.base.util;

import android.widget.Toast;

import com.ivy.base.ActivityUtil;

public class IToast {

    public static boolean debug = false;

    public static void toast(String msg) {
        if (debug) {
            if (ActivityUtil.getInstance().getActivity() != null) {
                Toast.makeText(ActivityUtil.getInstance().getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void toastAnyway(String msg) {
        if (ActivityUtil.getInstance().getActivity() != null) {
            Toast.makeText(ActivityUtil.getInstance().getActivity(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}