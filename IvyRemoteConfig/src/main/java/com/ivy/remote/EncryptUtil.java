package com.ivy.remote;

import android.util.Pair;

import com.ivy.base.App;
import com.ivy.base.util.Util;

import org.json.JSONObject;

public class EncryptUtil {

    private EncryptUtil() {
        // 私有构造，防止实例化
    }

    /**
     * 解密字符串
     */
    public static String decrypt(String src) {
        return Util.decodeParams(App.getInstance(), src);
    }

    /**
     * 判断字符串是否是有效 JSON
     * @param data 待检测字符串
     * @return Pair<Boolean, JSONObject>，第一个为是否有效，第二个为解析后的 JSONObject（无效则为 null）
     */
    public static Pair<Boolean, JSONObject> isValidJson(String data) {
        try {
            if (data == null) return new Pair<>(false, null);
            JSONObject json = new JSONObject(data);
            return new Pair<>(true, json);
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return new Pair<>(false, null);
    }

}
