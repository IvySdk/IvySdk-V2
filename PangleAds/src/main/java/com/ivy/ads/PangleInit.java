package com.ivy.ads;

import android.app.Activity;
import android.util.Log;

import com.bytedance.sdk.openadsdk.api.PAGMInitSuccessModel;
import com.bytedance.sdk.openadsdk.api.init.PAGMConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGMSdk;
import com.bytedance.sdk.openadsdk.api.model.PAGErrorModel;
import com.bytedance.sdk.openadsdk.mtestsuite.api.PAGMTestSuite;
import com.ivy.base.IUnProguard;

import org.json.JSONObject;

import java.util.Map;

public class PangleInit implements IPlatformInit, IUnProguard {

    @Override
    public void init(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        // 1. 构建配置对象
        PAGMConfig c = new PAGMConfig.Builder()
                .appId(config.optString("appid")) // 替换为 Pangle 后台申请的 App ID
                .debugLog(debug)    // 测试阶段开启，上线前务必关闭
                .build();

        // 2. 调用初始化接口
        PAGMSdk.init(activity, c, new PAGMSdk.PAGMInitCallback() {
            @Override
            public void success(PAGMInitSuccessModel pagmInitSuccessModel) {
                Log.i("hoho", "PAG SDK Init Success");
                listener.onInitialized("", "pangle");
                if (debug) {
//                    PAGMTestSuite.launchTestSuite(activity);
                }
            }

            @Override
            public void fail(PAGErrorModel pagErrorModel) {
                Log.e("hoho", "PAG SDK Init Failed: code " + pagErrorModel.getErrorCode() + " " + pagErrorModel.getErrorMessage());
                listener.onInitialized("", "pangle");
            }
        });
    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {

    }
}
