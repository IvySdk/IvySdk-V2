package com.android.client;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;

import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.core.IvySdk;

public class UnityPlayerActivity extends com.unity3d.player.UnityPlayerActivity {

    @Override
    protected void onCreate(Bundle bundle) {

        super.onCreate(bundle);
        App.setInstance(getApplication());
        ActivityUtil.getInstance().setActivity(this);
        try {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        } catch (Throwable ignored) {
        }
        IvySdk.getInstance().onCreate(this);
//        AndroidSdk.displayInNotch();

        boolean ivySplash = false;
        try {
            ApplicationInfo applicationInfo = App.getInstance().getPackageManager().getApplicationInfo(App.getInstance().getPackageName(), PackageManager.GET_META_DATA);
            ivySplash = applicationInfo.metaData.getBoolean("ivy.splash", false);
        } catch (Exception ignored) {
        }
        if (ivySplash) {
            SplashImageLoader.loadAndAddSplash(this, "splash", ((ViewGroup) getWindow().getDecorView()));
        }
    }

    public void hideSplash() {
        ILog.d("unity", "call hide splash");
        SplashImageLoader.removeSplash(((ViewGroup) getWindow().getDecorView()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        AndroidSdk.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidSdk.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AndroidSdk.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        AndroidSdk.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AndroidSdk.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AndroidSdk.onNewIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        AndroidSdk.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        AndroidSdk.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


}
