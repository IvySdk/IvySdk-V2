package com.ivy.core.ab;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.base.track.IKeys;
import com.ivy.base.util.BrotliDecompressor;
import com.ivy.base.util.CheckConfigHelper;
import com.ivy.base.util.Util;
import com.ivy.remote.DataManager;
import com.ivy.remote.EncryptUtil;
import com.ivy.track.IvyTrack;
import com.ivy.track.property.AndroidVersionUtils;
import com.ivy.track.property.DeviceUtil;
import com.ivy.track.property.TimeUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LocalAB {
    private static final String TAG = "AB_LocalAB";
    private static boolean isNewUserSession = false;
    private static JSONObject debugConfig;

    private static class ABHistory {
        private static final String STORAGE_KEY = "sdk_ab_history";
        private static final String FORCED_VARIANT_MARKER = "__forced_variant__";
        private static final String FORCED_VARIANT_PREFIX = "__forced_variant_key__:";
        private static final String NODE_PREFIX = "__ab_node__:";
        private static final String VARIANT_PREFIX = "__ab_variant__:";
        private static final String TYPED_MARKER = "__ab_typed_history_loaded__";
        private static boolean removalsCheckedThisSession;

        private static Set<String> readRemovalIds(JSONArray ids) throws JSONException {
            Set<String> result = new HashSet<>();
            if (ids == null) {
                return result;
            }
            for (int i = 0; i < ids.length(); i++) {
                Object id = ids.get(i);
                if (!(id instanceof String)) {
                    throw new JSONException("AB removal IDs must be strings");
                }
                result.add((String) id);
            }
            return result;
        }

        private static synchronized void prepareSession(Set<String> entries,
                ABTestAllocator.TrafficTreeContext treeContext, String userId) {
            if (removalsCheckedThisSession) {
                return;
            }
            removalsCheckedThisSession = true;
            String userTags = LocalStorage.Instance.decodeString(DataManager.KEY_CACHE_USER_TAGS, null);
            if (userTags != null) {
                try {
                    JSONObject data = new JSONObject(userTags);
                    if (data.has("user_id") && !userId.equals(data.optString("user_id"))) {
                        Log.w(TAG, "AB 清退响应用户不匹配: expected=" + userId + ", actual=" + data.optString("user_id"));
                        return;
                    }
                    Set<String> variants = readRemovalIds(data.optJSONArray("ab_removed_variants"));
                    Set<String> domains = readRemovalIds(data.optJSONArray("ab_removed_domains"));
                    if (!variants.isEmpty() || !domains.isEmpty()) {
                        Log.i(TAG, "检测到 AB 清退指令: variants=" + variants + ", domains=" + domains);
                        Set<String> removedEntries = new HashSet<>();
                        Set<String> retainedEntries = new HashSet<>();
                        collectRemovals(treeContext.root, entries, variants, domains, false,
                                removedEntries, retainedEntries);
                        // 最后一个强制变体被清退后，应恢复常规分流。
                        removedEntries.add(FORCED_VARIANT_MARKER);
                        retainedEntries.retainAll(entries);

                        Set<String> actuallyRemoved = new HashSet<>(entries);
                        actuallyRemoved.retainAll(removedEntries);

                        entries.removeAll(removedEntries);
                        entries.addAll(retainedEntries);
                        save(entries);

                        if (!actuallyRemoved.isEmpty()) {
                            Log.i(TAG, "AB 清退生效，移除历史记录: " + actuallyRemoved);
                        } else {
                            Log.i(TAG, "AB 清退执行完毕，当前用户本地历史未命中待清退项");
                        }
                    } else {
                        Log.d(TAG, "AB 清退检查完成，无待清退配置");
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "读取 AB 清退结果失败，保留历史记录", e);
                }
            } else {
                Log.d(TAG, "AB 清退检查: 暂无缓存的 userTags");
            }
        }

        private static boolean collectRemovals(ABTestAllocator.TrafficNode node, Set<String> entries,
                Set<String> variants, Set<String> domains, boolean removedDomain,
                Set<String> removedEntries, Set<String> retainedEntries) {
            boolean isRemovedDomainNode = "domain".equals(node.type) && domains.contains(node.id);
            if (isRemovedDomainNode) {
                Log.i(TAG, "AB 清退匹配 Domain: " + node.id);
            }
            removedDomain = removedDomain || isRemovedDomainNode;
            boolean retainedHit = false;
            boolean removedVariantHit = false;
            if ("experiment".equals(node.type)) {
                for (ABTestAllocator.TrafficNode variant : node.children) {
                    boolean forcedHit = containsForcedVariant(entries, node.id, variant.id)
                            || (isForced(entries) && !hasScopedForcedVariant(entries)
                            && entries.contains(node.id) && entries.contains(variant.id));
                    boolean hit = forcedHit || containsVariant(entries, node.id, variant.id);
                    if (removedDomain || variants.contains(variant.id)) {
                        if (variants.contains(variant.id)) {
                            Log.i(TAG, "AB 清退匹配 Variant: " + variant.id + " (实验: " + node.id + (hit ? ", 本地历史命中已清除" : "") + ")");
                        } else if (removedDomain && hit) {
                            Log.i(TAG, "AB 清退包含 Variant (因所属 Domain 清退): " + variant.id + " (实验: " + node.id + ")");
                        }
                        removedEntries.add(variantKey(node.id, variant.id));
                        removedEntries.add(forcedVariantKey(node.id, variant.id));
                        if (!hasTypedHistory(entries)) {
                            removedEntries.add(variant.id);
                        }
                        removedVariantHit |= hit;
                    } else if (hit) {
                        retainedHit = true;
                        if (forcedHit) {
                            retainedEntries.add(FORCED_VARIANT_MARKER);
                        }
                    }
                }
                // 只删变体会留下实验保送标记，导致老用户绕过 user_scope=new。
                if (!removedDomain && !removedVariantHit && containsNode(entries, node.type, node.id)) {
                    retainedHit = true;
                }
            } else if (node.children != null) {
                for (ABTestAllocator.TrafficNode child : node.children) {
                    retainedHit |= collectRemovals(child, entries, variants, domains, removedDomain,
                            removedEntries, retainedEntries);
                }
                if (!removedDomain && "domain".equals(node.type)
                        && containsNode(entries, node.type, node.id)) {
                    retainedHit = true;
                }
            }

            if (removedDomain || (removedVariantHit && !retainedHit)) {
                if ("experiment".equals(node.type) && removedVariantHit && !retainedHit) {
                    Log.i(TAG, "AB 清退实验节点: " + node.id);
                }
                removedEntries.add(nodeKey(node.type, node.id));
                if (!hasTypedHistory(entries)) {
                    removedEntries.add(node.id);
                }
            } else if (retainedHit) {
                // 不同域可能使用同名层；保留仍有命中分支所需的共享历史 key。
                retainedEntries.add(nodeKey(node.type, node.id));
                if (!hasTypedHistory(entries)) {
                    retainedEntries.add(node.id);
                }
            }
            return retainedHit;
        }

        private static String forcedVariantKey(String experimentId, String variantId) {
            return FORCED_VARIANT_PREFIX + experimentId + ":" + variantId;
        }

        private static String nodeKey(String type, String id) {
            return NODE_PREFIX + type + ":" + id;
        }

        private static String variantKey(String experimentId, String variantId) {
            return VARIANT_PREFIX + experimentId + ":" + variantId;
        }

        private static boolean hasTypedHistory(Set<String> entries) {
            return entries.contains(TYPED_MARKER);
        }

        private static Set<String> load() {
            Set<String> entries = new HashSet<>();
            try {
                JSONArray historyArr = new JSONArray(LocalStorage.Instance.decodeString(STORAGE_KEY, "[]"));
                for (int i = 0; i < historyArr.length(); i++) {
                    String historyNode = historyArr.getString(i);
                    entries.add(historyNode);
                    if (isTypedEntry(historyNode)) {
                        entries.add(TYPED_MARKER);
                    }
                }
                Log.d(TAG, "加载老用户历史记录完成，个数: " + historyArr.length());
            } catch (Exception e) {
                Log.w(TAG, "无历史记录或解析失败");
            }
            return entries;
        }

        private static boolean isTypedEntry(String entry) {
            return entry != null && (entry.startsWith(NODE_PREFIX) || entry.startsWith(VARIANT_PREFIX));
        }

        private static boolean isForced(Set<String> entries) {
            return entries.contains(FORCED_VARIANT_MARKER);
        }

        private static boolean hasScopedForcedVariant(Set<String> entries) {
            for (String entry : entries) {
                if (entry != null && entry.startsWith(FORCED_VARIANT_PREFIX)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean containsForcedVariant(Set<String> entries, String experimentId, String variantId) {
            return entries.contains(forcedVariantKey(experimentId, variantId));
        }

        private static boolean containsNode(Set<String> entries, String type, String id) {
            if (id == null) {
                return false;
            }
            return entries.contains(nodeKey(type, id))
                    || (!hasTypedHistory(entries) && entries.contains(id));
        }

        private static boolean containsVariant(Set<String> entries, String experimentId, String variantId) {
            if (experimentId == null || variantId == null) {
                return false;
            }
            return entries.contains(variantKey(experimentId, variantId))
                    || (!hasTypedHistory(entries) && entries.contains(variantId));
        }

        private static void addNode(Set<String> entries, String type, String id) {
            if (id != null && !id.isEmpty()) {
                entries.add(nodeKey(type, id));
            }
        }

        private static void addVariant(Set<String> entries, String experimentId, String variantId) {
            if (experimentId != null && !experimentId.isEmpty() && variantId != null && !variantId.isEmpty()) {
                entries.add(variantKey(experimentId, variantId));
            }
        }

        private static Set<String> newForcedEntries() {
            Set<String> entries = new HashSet<>();
            entries.add(FORCED_VARIANT_MARKER);
            return entries;
        }

        private static void addForcedVariant(Set<String> entries, String experimentId, String variantId) {
            if (experimentId != null && !experimentId.isEmpty() && variantId != null && !variantId.isEmpty()) {
                entries.add(forcedVariantKey(experimentId, variantId));
            }
        }

        private static void addResult(Set<String> entries, ABTestAllocator.ExperimentResult result) {
            addNode(entries, "group", result.flowDomainGroupCode);
            addNode(entries, "domain", result.flowDomainCode);
            addNode(entries, "layer", result.flowLayerCode);
            addNode(entries, "experiment", result.experimentId);
            addVariant(entries, result.experimentId, result.variantId);
        }

        private static void migrateLegacy(Set<String> entries, ABTestAllocator.TrafficTreeContext treeContext) {
            if (hasTypedHistory(entries) || isForced(entries)) {
                return;
            }

            Set<String> migratedEntries = new HashSet<>();
            Set<String> legacyVariantIds = new HashSet<>();

            for (ABTestAllocator.TrafficNode exp : treeContext.allExperiments) {
                boolean hasExperiment = entries.contains(exp.id);
                if (hasExperiment) {
                    addNode(migratedEntries, "experiment", exp.id);
                }
                if (exp.children == null) {
                    continue;
                }
                for (ABTestAllocator.TrafficNode var : exp.children) {
                    if (hasExperiment && entries.contains(var.id)) {
                        addVariant(migratedEntries, exp.id, var.id);
                        legacyVariantIds.add(var.id);
                        ABTestAllocator.ExperimentContextInfo ctxInfo = treeContext.experimentContextMap.get(exp.id);
                        if (ctxInfo != null) {
                            addNode(migratedEntries, "group", ctxInfo.flowDomainGroupCode);
                            addNode(migratedEntries, "domain", ctxInfo.flowDomainCode);
                            addNode(migratedEntries, "layer", ctxInfo.flowLayerCode);
                        }
                    }
                }
            }

            migrateLegacyTreeNodes(treeContext.root, entries, legacyVariantIds, migratedEntries);

            if (!migratedEntries.isEmpty()) {
                entries.clear();
                entries.addAll(migratedEntries);
                entries.add(TYPED_MARKER);
            }
        }

        private static void migrateLegacyTreeNodes(ABTestAllocator.TrafficNode node, Set<String> legacyEntries,
                                                   Set<String> legacyVariantIds, Set<String> migratedEntries) {
            if (node == null) {
                return;
            }
            if (legacyEntries.contains(node.id) && !legacyVariantIds.contains(node.id)
                    && ("group".equals(node.type) || "domain".equals(node.type) || "layer".equals(node.type))) {
                addNode(migratedEntries, node.type, node.id);
            }
            if (node.children == null) {
                return;
            }
            for (ABTestAllocator.TrafficNode child : node.children) {
                migrateLegacyTreeNodes(child, legacyEntries, legacyVariantIds, migratedEntries);
            }
        }

        private static void addDomainsFromEntries(Set<String> entries, ABTestAllocator.TrafficTreeContext treeContext,
                                                  Set<String> domains) {
            String domainPrefix = nodeKey("domain", "");
            for (String entry : entries) {
                if (treeContext.allDomainCodes.contains(entry)) {
                    domains.add(entry);
                } else if (entry != null && entry.startsWith(domainPrefix)) {
                    domains.add(entry.substring(domainPrefix.length()));
                }
            }
        }

        private static void save(Set<String> entries) {
            JSONArray newHistory = new JSONArray();
            for (String entry : entries) {
                if (!TYPED_MARKER.equals(entry)) {
                    newHistory.put(entry);
                }
            }
            LocalStorage.Instance.encodeString(STORAGE_KEY, newHistory.toString());
        }
    }

    private static String loadAssets(Context context, String fileName) {
        try {
            InputStream stream = context.getAssets().open(fileName);
            byte[] buffer = new byte[stream.available()];
            stream.read(buffer);
            stream.close();
            String deData = new String(buffer);
            String data = EncryptUtil.decrypt(deData);
            if (data == null) {
                return deData;
            } else {
                return BrotliDecompressor.decompress(DataManager.ENABLE_BROTLI, data);
            }
        } catch (Exception e) {
            Log.e(TAG, "read assets " + fileName + " failed;" + e.getMessage());
        }
        return null;
    }

    public static void initWithContext(Context context) {
        Log.i(TAG, "========== AB测试初始化开始 ==========");

        try {
            if (!LocalStorage.Instance.containsKey("_sdk_first_app_version_")) {
                LocalStorage.Instance.encodeString("_sdk_first_app_version_", Util.versionName());
            }
            if (!LocalStorage.Instance.containsKey("_sdk_first_app_vc_")) {
                LocalStorage.Instance.encodeLong("_sdk_first_app_vc_", Util.versionCode());
            }
            long openTimes = LocalStorage.Instance.decodeLong("_sdk_first_time_", 0L);
            if (System.currentTimeMillis() - openTimes < 86400000) {
                isNewUserSession = true;
            }
            String s = loadAssets(context, "ab_test_config.json");
            JSONObject data = new JSONObject(s);
            debugConfig = data.optJSONObject("debug");

            ProcessABResult processResult = processAB(context, data);
            IvyABManager.getInstance().setDefaultExperimentAbData(processResult.domainDefaultData,
                    processResult.abData, processResult.hitResultStr);

        } catch (Exception e) {
            Log.e(TAG, "parse ab.json failed");
        }
        Log.i(TAG, "========== AB测试初始化完成 ==========");
    }

    public static boolean activateVariant(Context context, String variantIdsStr) {
        try {
            if (variantIdsStr == null || variantIdsStr.trim().isEmpty()) {
                return false;
            }
            Log.i(TAG, "========== 指定激活 Variant (独占模式): " + variantIdsStr + " ==========");

            String[] variantIds = variantIdsStr.split("[,;]");

            String s = loadAssets(context, "ab_test_config.json");
            if (s == null) {
                Log.e(TAG, "加载 ab_test_config.json 失败");
                return false;
            }
            JSONObject data = new JSONObject(s);
            ABTestAllocator.TrafficTreeContext treeContext = ABTestAllocator.buildTreeFromJSON(data);
            if (treeContext == null)
                return false;

            Set<String> historyToAdd = ABHistory.newForcedEntries();
            boolean result = false;
            for (String vid : variantIds) {
                String variantId = vid.trim();
                if (variantId.isEmpty())
                    continue;

                ABTestAllocator.TrafficNode targetVariant = null;
                ABTestAllocator.TrafficNode targetExperiment = null;

                for (ABTestAllocator.TrafficNode exp : treeContext.allExperiments) {
                    if (exp.children != null) {
                        for (ABTestAllocator.TrafficNode var : exp.children) {
                            if (var.id != null && var.id.equals(variantId)) {
                                targetVariant = var;
                                targetExperiment = exp;
                                break;
                            }
                        }
                    }
                    if (targetVariant != null)
                        break;
                }

                if (targetVariant == null) {
                    Log.e(TAG, "未找到指定的 Variant: " + variantId);
                    continue;
                }
                result = true;
                ABHistory.addForcedVariant(historyToAdd, targetExperiment.id, targetVariant.id);
            }

            ABHistory.save(historyToAdd);

            Log.i(TAG, "已清空其它实验并锁定独占 Variant(s): " + variantIdsStr);

            // 重新初始化以应用更新
            initWithContext(context);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "activateVariant 执行报错", e);
        }
        return false;
    }

    private static HashMap<String, Object> m = new HashMap<>();

    public static Map<String, Object> userInfo(Context context, boolean init) {
        if (!init) {
            if (!m.isEmpty()) {
                applyDebugUserInfo(m);
                return m;
            }
        }
        try {
            JSONObject o = new JSONObject(LocalStorage.Instance.decodeString("cache_install_data", "{}"));
            m.put("ad_id", o.optString("ad_id"));
            m.put("ad_name", o.optString("ad_name"));
            m.put("ad_type", o.optString("ad_type"));
            m.put("adset_id", o.optString("adset_id"));
            m.put("adset_name", o.optString("adset_name"));
            m.put("campaign_id", o.optString("campaign_id"));
            m.put("campaign_name", o.optString("campaign_name"));
            m.put("media_channel", o.optString("media_channel"));
            m.put("media_source", o.optString("media_source"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        m.put("ad_value", LocalStorage.Instance.decodeString("_sdk_ad_value_", "unset"));
        m.put("bx", LocalStorage.Instance.decodeString("_bx_id_", ""));
        m.put("old_bx", LocalStorage.Instance.decodeString("_old_bx_id_", ""));
        String country = Util.countryCode();
        Log.d(TAG, "country code " + country);
        m.put("country_code", country);

        if (m.containsKey("ram")) {
            applyDebugUserInfo(m);
            return m;
        }

        try {
            m.put("installed_date",
                    new TimeUtil(
                            context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime)
                            .getUtcTimeStr());
            String s = LocalStorage.Instance.decodeString(DataManager.KEY_CACHE_USER_TAGS, "{}");
            JSONObject o = new JSONObject(s);
            Log.e(TAG, "服务器给的user tags: " + s);
            java.util.Iterator<String> keys = o.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object valueObj = o.opt(key);
                if (valueObj == null) continue;

                if ("lumina_user_group".equals(key)) {
                    // 2. group_condition & event_condition
                    try {
                        JSONArray groupArray;
                        if (valueObj instanceof JSONArray) {
                            groupArray = (JSONArray) valueObj;
                        } else {
                            groupArray = new JSONArray(valueObj.toString());
                        }
                        for (int i = 0; i < groupArray.length(); i++) {
                            String groupId = groupArray.optString(i);
                            if (groupId != null && !groupId.isEmpty()) {
                                m.put(groupId, "1");
                            }
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Parse lumina_user_group failed", ex);
                    }
                } else if ("lumina_user_tag".equals(key)) {
                    // 3. tag_condition
                    try {
                        JSONArray tagArray;
                        if (valueObj instanceof JSONArray) {
                            tagArray = (JSONArray) valueObj;
                        } else {
                            tagArray = new JSONArray(valueObj.toString());
                        }
                        for (int i = 0; i < tagArray.length(); i++) {
                            String item = tagArray.optString(i);
                            if (item != null && !item.isEmpty()) {
                                int splitIdx = item.lastIndexOf('_');
                                if (splitIdx > 0) {
                                    m.put(item.substring(0, splitIdx), item.substring(splitIdx + 1));
                                } else {
                                    m.put(item, "1");
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Parse lumina_user_tag failed", ex);
                    }
                } else {
                    // user_condition 对应的是普通属性 (如 ram, app_version 等)
                    // 无论是整型、浮点型还是布尔，都按字符串存入 m 进行匹配
                    m.put(key, valueObj.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        m.put("android_version", Build.VERSION.RELEASE);
        m.put("android_api_version", Build.VERSION.SDK_INT);
        m.put("app_version", Util.versionName());
        m.put("app_version_code", Util.versionCode());
        m.put("ram", Util.byte2GB(Util.totalMemory()));
        m.put("platform", LocalStorage.Instance.decodeString("_sdk_login_platform_", ""));
        m.put("os_version", AndroidVersionUtils.getAndroidMajorVersion());
        m.put("network_type", CheckConfigHelper.getNetworkType(context));
        m.put("carrier", DeviceUtil.getCarrier(context));
        m.put("language", Locale.getDefault().getLanguage());
        m.put("cores", Runtime.getRuntime().availableProcessors());
        m.put("device_model", Build.MANUFACTURER + "_" + Build.MODEL);
        m.put("paying_user", LocalStorage.Instance.decodeString(IKeys.KEY_PAYING_USER, "0"));
        m.put("first_app_version", LocalStorage.Instance.decodeString("_sdk_first_app_version_", ""));
        m.put("first_app_version_code", LocalStorage.Instance.decodeLong("_sdk_first_app_vc_", 0L));
        m.put("is_new_user", isNewUserSession ? "1" : "0");
        applyDebugUserInfo(m);
        Log.d(TAG, "final country code " + m.get("country_code"));
        return m;
    }

    private static void applyDebugUserInfo(Map<String, Object> userInfo) {
        if (debugConfig != null) {
            for (Iterator<String> it = debugConfig.keys(); it.hasNext(); ) {
                String k = it.next();
                Object v = debugConfig.opt(k);
                userInfo.put(k, v);
                Log.d(TAG, "AB debug " + k + ": " + v);
            }
        }
    }

    private static String getDebugValue(String key) {
        if (debugConfig == null) {
            return "";
        }
        return debugConfig.optString(key, "");
    }

    private static void parseDefaultArray(JSONArray defaultArr, Map<String, Object> uInfo, JSONObject collectedDomainDefaultData) {
        if (defaultArr == null) return;
        for (int i = 0; i < defaultArr.length(); i++) {
            JSONObject item = defaultArr.optJSONObject(i);
            if (item == null) continue;
            String key = item.optString("key");
            if (key == null || key.isEmpty()) continue;

            boolean match = true;
            if (item.has("user_rules") && !item.isNull("user_rules")) {
                match = ABTestAllocator.checkUserRules(item.optJSONObject("user_rules"), uInfo);
            }
            if (match) {
                try {
                    collectedDomainDefaultData.put(key, item.opt("value"));
                } catch (JSONException e) {
                    Log.e(TAG, "解析 default 数据异常", e);
                }
            }
        }
    }

    private static void logAbActivation(String flowDomainGroupCode, String flowDomainCode,
                                        String flowLayerCode, String experimentCode,
                                        String variantCode) {
        Map<String, Object> eventParams = new HashMap<>();
        eventParams.put("flow_domain_group_code", flowDomainGroupCode != null ? flowDomainGroupCode : "");
        eventParams.put("flow_domain_code", flowDomainCode != null ? flowDomainCode : "");
        eventParams.put("flow_layer_code", flowLayerCode != null ? flowLayerCode : "");
        eventParams.put("experiment_code", experimentCode != null ? experimentCode : "");
        eventParams.put("variant_code", variantCode != null ? variantCode : "");

        IvyTrack.getInstance().logEvent("sdk_ab_activation", EventType.EVENT_TYPE_COMMON,
                EventSrc.EVENT_SRC_SDK, eventParams);
    }

    private static void logHitDomainActivations(ABTestAllocator.TrafficTreeContext treeContext) {
        if (treeContext == null || treeContext.root == null || treeContext.root.children == null) {
            return;
        }
        for (ABTestAllocator.TrafficNode group : treeContext.root.children) {
            if (!"group".equals(group.type) || group.children == null) {
                continue;
            }
            for (ABTestAllocator.TrafficNode domain : group.children) {
                if ("domain".equals(domain.type) && domain.hit) {
                    logAbActivation(group.id, domain.id, "", "", "");
                }
            }
        }
    }

    public static class ProcessABResult {
        public JSONObject domainDefaultData;
        public JSONObject abData;
        public String hitResultStr;

        public ProcessABResult(JSONObject domainDefaultData, JSONObject abData, String hitResultStr) {
            this.domainDefaultData = domainDefaultData;
            this.abData = abData;
            this.hitResultStr = hitResultStr;
        }
    }

    private static ProcessABResult processAB(Context context, JSONObject data) {
        JSONObject result = new JSONObject();
        JSONObject collectedDomainDefaultData = new JSONObject();
        String userId = Util.roleId();

        Log.i(TAG, "========== 开始处理AB测试 ==========");
        Log.i(TAG, "用户ID: " + userId);

        // 1. 读取老用户的历史命中记录 (Sticky Bucketing 老用户保持一致性)
        Set<String> targetNodes = ABHistory.load();

        ABTestAllocator.TrafficTreeContext treeContext = ABTestAllocator.buildTreeFromJSON(data);
        if (treeContext == null) {
            throw new IllegalArgumentException("Invalid AB traffic tree");
        }
        ABHistory.migrateLegacy(targetNodes, treeContext);
        ABHistory.prepareSession(targetNodes, treeContext, Util.userId());
        List<ABTestAllocator.ExperimentResult> results = new java.util.ArrayList<>();
        boolean isForced = ABHistory.isForced(targetNodes);
        boolean hasScopedForcedVariant = ABHistory.hasScopedForcedVariant(targetNodes);

        if (isForced) {
            Log.i(TAG, "检测到独占激活标记，跳过常规分流分配");
            // 遍历所有给定的强制激活的 Variant 并生成结果
            for (ABTestAllocator.TrafficNode exp : treeContext.allExperiments) {
                if (exp.children != null) {
                    for (ABTestAllocator.TrafficNode var : exp.children) {
                        boolean isScopedTarget = ABHistory.containsForcedVariant(targetNodes, exp.id, var.id);
                        boolean isLegacyTarget = !hasScopedForcedVariant
                                && targetNodes.contains(exp.id)
                                && targetNodes.contains(var.id);
                        if (isScopedTarget || isLegacyTarget) {
                            ABTestAllocator.ExperimentContextInfo forcedCtxInfo = treeContext.experimentContextMap
                                    .get(exp.id);
                            ABTestAllocator.ExperimentResult forcedResult = new ABTestAllocator.ExperimentResult(
                                    exp.id,
                                    var.id,
                                    var.configParams,
                                    forcedCtxInfo != null ? forcedCtxInfo.flowDomainGroupCode : null,
                                    forcedCtxInfo != null ? forcedCtxInfo.flowDomainCode : null,
                                    forcedCtxInfo != null ? forcedCtxInfo.flowLayerCode : null);
                            results.add(forcedResult);
                        }
                    }
                }
            }
        } else {
            results = ABTestAllocator.allocate(userId, treeContext, userInfo(context, true), targetNodes);
        }

        // hitDomains = 实验命中的 domain + 树遍历走过的 domain（此时已存入 targetNodes）
        Set<String> hitDomains = new HashSet<>();
        ABHistory.addDomainsFromEntries(targetNodes, treeContext, hitDomains);

        Log.i(TAG, "========== AB测试分配结果 ==========");
        Log.i(TAG, "总共命中 " + results.size() + " 个实验/分支" + (isForced ? " (Forced)" : ""));

        // 2. 先收集属于该用户的 Domain 兜底数据（包含 global 和命中的 domain）
        JSONObject allDefaultData = data.optJSONObject("default");
        for (ABTestAllocator.ExperimentResult r : results) {
            if (r.flowDomainCode != null && !r.flowDomainCode.isEmpty()) {
                hitDomains.add(r.flowDomainCode);
            }
        }

        Map<String, Object> uInfo = userInfo(context, true);
        if (allDefaultData != null) {
            // 先加载 global 全局兜底数据作为底色
            parseDefaultArray(allDefaultData.optJSONArray("global"), uInfo, collectedDomainDefaultData);

            // 再用命中的 domain 数据进行覆盖，并且不需要将它设入 result(携带 val/vid 的是变体专属逻辑)
            for (String domainCode : hitDomains) {
                parseDefaultArray(allDefaultData.optJSONArray(domainCode), uInfo, collectedDomainDefaultData);
            }
        }

        // 3. 收集并覆盖实验的 Variant 数据
        try {
            Set<String> hitNodesTracker = new HashSet<>();
            for (ABTestAllocator.ExperimentResult r : results) {
                // 保存命中所有的层级树分支，实现全路径 Sticky Bucketing 一致性
                ABHistory.addResult(hitNodesTracker, r);

                Log.i(TAG, "✅ Experiment: " + r.experimentId);
                Log.i(TAG, "   Variant: " + r.variantId);
                Log.i(TAG, "   Context:");
                Log.i(TAG, "     - DomainGroup: " + (r.flowDomainGroupCode != null ? r.flowDomainGroupCode : "N/A"));
                Log.i(TAG, "     - Domain: " + (r.flowDomainCode != null ? r.flowDomainCode : "N/A"));
                Log.i(TAG, "     - Layer: " + (r.flowLayerCode != null ? r.flowLayerCode : "N/A"));

                logAbActivation(r.flowDomainGroupCode, r.flowDomainCode, r.flowLayerCode,
                        r.experimentId, r.variantId);

                if (r.configParams != null) {
                    Log.i(TAG, "   Config: " + r.configParams.toString());

                    JSONObject configParams = r.configParams;
                    java.util.Iterator<String> keys = configParams.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Object value = configParams.get(key);

                        JSONObject paramObj = new JSONObject();
                        paramObj.put("val", value);
                        paramObj.put("vid", r.variantId);

                        // Experiment 取值会覆盖 default 里的取值 (由于 JsonObject 的相同 Key 会被替换的特性)
                        result.put(key, paramObj);
                    }
                }
            }

            if (results.isEmpty()) {
                logHitDomainActivations(treeContext);
            }

            // 注意：因为 domain ID 已经在 allocateNode 时被加入了 targetNodes，
            // 为了实现全路径一致性，我们也需要将通过 targetNodes 识别出的 domain 存入 history
            for (String id : targetNodes) {
                if (treeContext.allDomainCodes.contains(id)) {
                    ABHistory.addNode(hitNodesTracker, "domain", id);
                }
            }

            if (isForced) {
                Set<String> forcedHistory = ABHistory.newForcedEntries();
                for (ABTestAllocator.ExperimentResult r : results) {
                    ABHistory.addForcedVariant(forcedHistory, r.experimentId, r.variantId);
                }
                ABHistory.save(forcedHistory);
            } else {
                // 更新由于最新命中的覆盖旧的记录
                ABHistory.save(hitNodesTracker);
            }

            Log.i(TAG, "====================================");
            Log.i(TAG, "提取 Domain 兜底配置:");
            Log.i(TAG, collectedDomainDefaultData.toString(2));
            Log.i(TAG, "最终覆盖后实验配置:");
            Log.i(TAG, result.toString(2));

        } catch (JSONException e) {
            Log.e(TAG, "构建返回结果时出错", e);
        }

        List<String> parts = new ArrayList<>();
        Set<String> reportedDomains = new HashSet<>();

        // 1. 先记录有实验命中的情况
        for (ABTestAllocator.ExperimentResult r : results) {
            String domainSerial = treeContext.idToSerialNumberMap.get(r.flowDomainCode);
            String domainDisplay = (domainSerial != null && !domainSerial.isEmpty()) ? domainSerial
                    : (r.flowDomainCode != null && !r.flowDomainCode.isEmpty() ? r.flowDomainCode : "??");

            String expSerial = treeContext.idToSerialNumberMap.get(r.experimentId);
            String expDisplay = (expSerial != null && !expSerial.isEmpty()) ? expSerial
                    : (r.experimentId != null ? r.experimentId : "");

            String varSerial = treeContext.idToSerialNumberMap.get(r.variantId);
            String varDisplay = (varSerial != null && !varSerial.isEmpty()) ? varSerial
                    : (r.variantId != null ? r.variantId : "");

            parts.add(domainDisplay + "-" + expDisplay + "-" + varDisplay);

            if (r.flowDomainCode != null) {
                reportedDomains.add(r.flowDomainCode);
            }
        }

        // 2. 对于命中但没有实验产生的 domain，记录为 domain-??-??
        for (String domainCode : hitDomains) {
            if (!reportedDomains.contains(domainCode)) {
                String domainSerial = treeContext.idToSerialNumberMap.get(domainCode);
                String domainDisplay = (domainSerial != null && !domainSerial.isEmpty()) ? domainSerial : domainCode;
                parts.add(domainDisplay + "-??-??");
            }
        }
        String hitResultStr = android.text.TextUtils.join(",", parts);
        Log.i(TAG, "命中实验的结果: " + hitResultStr);

        return new ProcessABResult(collectedDomainDefaultData, result, hitResultStr);
    }

    public static class ABTestAllocator {
        private static final String TAG = "AB_LocalAB";
        private static int recursionDepth = 0;

        private static String indent() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < recursionDepth; i++) {
                sb.append("  ");
            }
            return sb.toString();
        }

        private static String optSafeString(JSONObject json, String key) {
            if (json == null || json.isNull(key)) {
                return null;
            }
            return json.optString(key, null);
        }

        public static class TrafficNode {
            public String parentId;
            public String id;
            public String type;
            public float flowPercentage;
            public float usedFlowPercentage;
            public List<TrafficNode> children;
            public JSONObject userCondition;
            public JSONObject userRules;
            public JSONObject configParams;
            public List<String> testUserCodes;
            public String userScope;
            public boolean isFunnel;
            public boolean hit;

            public TrafficNode(String id, String type) {
                this.id = id;
                this.type = type;
                this.flowPercentage = 100.0f;
                this.usedFlowPercentage = 0.0f;
                this.isFunnel = false;
                this.hit = false;
                this.children = new ArrayList<>();
                this.testUserCodes = new ArrayList<>();
            }

            @Override
            public String toString() {
                return String.format("[%s] id=%s flow=%.1f%% children=%d",
                        type, id.substring(0, Math.min(20, id.length())),
                        flowPercentage, children.size());
            }
        }

        public static class TrafficTreeContext {
            public TrafficNode root;
            public List<TrafficNode> allExperiments;
            public Map<String, ExperimentContextInfo> experimentContextMap;
            public Set<String> allDomainCodes;

            public TrafficTreeContext() {
                this.allExperiments = new ArrayList<>();
                this.experimentContextMap = new HashMap<>();
                this.allDomainCodes = new HashSet<>();
                this.idToSerialNumberMap = new HashMap<>();
            }

            public Map<String, String> idToSerialNumberMap;
        }

        public static class ExperimentContextInfo {
            public String flowDomainGroupCode;
            public String flowDomainCode;
            public String flowLayerCode;

            public ExperimentContextInfo(String dgCode, String dCode, String lCode) {
                this.flowDomainGroupCode = dgCode;
                this.flowDomainCode = dCode;
                this.flowLayerCode = lCode;
            }
        }

        public static class ExperimentResult {
            public String experimentId;
            public String variantId;
            public JSONObject configParams;
            public String flowDomainGroupCode;
            public String flowDomainCode;
            public String flowLayerCode;

            public ExperimentResult(String experimentId, String variantId, JSONObject params,
                                    String domainGroupCode, String domainCode, String layerCode) {
                this.experimentId = experimentId;
                this.variantId = variantId;
                this.configParams = params;
                this.flowDomainGroupCode = domainGroupCode;
                this.flowDomainCode = domainCode;
                this.flowLayerCode = layerCode;
            }
        }

        public static List<ExperimentResult> allocate(
                String userId,
                TrafficTreeContext treeContext,
                Map<String, Object> userInfo,
                Set<String> targetNodes) {

            Log.d(TAG, "╔════════════════════════════════════════");
            Log.d(TAG, "║ 开始流量分配");
            Log.d(TAG, "║ 用户ID: " + userId);
            Log.d(TAG, "╚════════════════════════════════════════");

            List<ExperimentResult> results = new ArrayList<>();

            // 1. 全局预检：直接通过 `test_user_codes` 将其纳入目标拦截集合中
            Log.d(TAG, "  --> 步骤 1: 扫描测试用户白名单，合并到目标保送集合...");
            for (TrafficNode experiment : treeContext.allExperiments) {
                for (TrafficNode variant : experiment.children) {
                    if (variant.testUserCodes.contains(userId)) {
                        Log.d(TAG, "      🎯 [白名单拦截命中] 用户 " + userId + " 在变体 " + variant.id + " 的白名单中！");
                        ABHistory.addNode(targetNodes, "experiment", experiment.id);
                        ABHistory.addVariant(targetNodes, experiment.id, variant.id);

                        ExperimentContextInfo ctxInfo = treeContext.experimentContextMap.get(experiment.id);
                        if (ctxInfo != null) {
                            if (ctxInfo.flowLayerCode != null && !ctxInfo.flowLayerCode.isEmpty())
                                ABHistory.addNode(targetNodes, "layer", ctxInfo.flowLayerCode);
                            if (ctxInfo.flowDomainCode != null && !ctxInfo.flowDomainCode.isEmpty())
                                ABHistory.addNode(targetNodes, "domain", ctxInfo.flowDomainCode);
                            if (ctxInfo.flowDomainGroupCode != null && !ctxInfo.flowDomainGroupCode.isEmpty())
                                ABHistory.addNode(targetNodes, "group", ctxInfo.flowDomainGroupCode);
                        }
                        break;
                    }
                }
            }

            // 2. 正常走树状分配（通过老记录/名单保证保送拦截）
            Log.d(TAG, "  --> 步骤 2: 进行正常流量树分配（跨越0%比例限制）");
            recursionDepth = 0;
            allocateNode(userId, treeContext.root, 1.0f, results, userInfo, targetNodes, null, null,
                    null);

            Log.d(TAG, "");
            Log.d(TAG, "流量分配完成，命中 " + results.size() + " 个实验");

            return results;
        }

        private static void allocateNode(
                String userId,
                TrafficNode node,
                float parentRatio,
                List<ExperimentResult> results,
                Map<String, Object> userInfo,
                Set<String> targetNodes,
                String domainGroupCode,
                String domainCode,
                String layerCode) {

            Log.d(TAG, indent() + "┌─ 遍历节点: " + node.toString());
            Log.d(TAG, indent() + "│  父流量比: " + String.format("%.2f%%", parentRatio * 100));

            recursionDepth++;

            String currentDomainGroupCode = domainGroupCode;
            String currentDomainCode = domainCode;
            String currentLayerCode = layerCode;

            if ("group".equals(node.type)) {
                currentDomainGroupCode = node.id;
                node.hit = true;
            } else if ("domain".equals(node.type)) {
                currentDomainCode = node.id;
                node.hit = true;
                // 直接存入 targetNodes 实现 domain 级别的 sticky bucketing
                ABHistory.addNode(targetNodes, "domain", node.id);
            } else if ("layer".equals(node.type)) {
                currentLayerCode = node.id;
            }

            if ("experiment".equals(node.type)) {

                Log.d(TAG, indent() + "│  → 这是实验节点，检查是否分配...");
                Log.d(TAG, indent() + "│     上下文: DG=" + currentDomainGroupCode + ", D=" + currentDomainCode + ", L="
                        + currentLayerCode);

                boolean isTarget = ABHistory.containsNode(targetNodes, "experiment", node.id);

                // 处理 user_scope: "new"
                if ("new".equals(node.userScope)) {
                    Object isNewObj = userInfo.get("is_new_user");
                    boolean isNewUser = "1".equals(isNewObj);
                    if (!isNewUser && !isTarget) {
                        Log.d(TAG, indent() + "│  ⚠ user_scope 为 new，但用户不是新用户且无历史记录，跳过该实验");
                        return;
                    }
                }

                if (isTarget || shouldAllocate(userId, node, parentRatio)) {
                    Log.d(TAG, indent() + "│  ✓ 通过流量分配/白名单目标保送");

                    // 如果是被 `activateVariant` 强制指定的 (存在于 targetNodes 里)，则跳过条件检查
                    boolean conditionPass = true;
                    if (!isTarget) {
                        if (node.userRules != null) {
                            conditionPass = checkUserRules(node.userRules, userInfo);
                        } else if (node.userCondition != null) {
                            conditionPass = checkCondition(node.userCondition, userInfo);
                        }
                    }

                    if (isTarget || conditionPass) {
                        if (isTarget) {
                            Log.d(TAG, indent() + "│  ✓ 目标拦截直接放行，跳过条件检查");
                        } else {
                            Log.d(TAG, indent() + "│  ✓ 通过条件检查");
                        }

                        TrafficNode variant = selectVariant(userId, node, targetNodes);
                        if (variant != null) {
                            Log.d(TAG, indent() + "│  🎯 成功分配到变体: " + variant.id);
                            results.add(new ExperimentResult(
                                    node.id,
                                    variant.id,
                                    variant.configParams,
                                    currentDomainGroupCode,
                                    currentDomainCode,
                                    currentLayerCode));
                        } else {
                            Log.w(TAG, indent() + "│  ⚠ 没有可用变体");
                        }
                    } else {
                        Log.d(TAG, indent() + "│  ✗ 未通过条件检查");
                    }
                } else {
                    Log.d(TAG, indent() + "│  ✗ 未通过流量分配");
                }

                recursionDepth--;
                Log.d(TAG, indent() + "└─ 实验节点处理完毕");
                return;
            }

            if (node.children == null || node.children.isEmpty()) {
                Log.d(TAG, indent() + "│  (叶子节点，无子节点)");
                recursionDepth--;
                Log.d(TAG, indent() + "└─");
                return;
            }

            Log.d(TAG, indent() + "│  子节点数量: " + node.children.size());

            String debugDomainGroup = getDebugValue("domain_group");
            if ("root".equals(node.type) && !debugDomainGroup.isEmpty()) {
                boolean foundDomainGroup = false;
                for (int i = 0; i < node.children.size(); i++) {
                    TrafficNode child = node.children.get(i);
                    if ("group".equals(child.type)) {
                        if (!debugDomainGroup.equals(child.id)) {
                            Log.d(TAG, indent() + "│  AB debug skip domain_group: " + child.id);
                            continue;
                        }
                        foundDomainGroup = true;
                        Log.i(TAG, "AB debug force domain_group: " + debugDomainGroup);
                    }

                    allocateNode(userId, child, 1.0f, results, userInfo, targetNodes,
                            currentDomainGroupCode, currentDomainCode, currentLayerCode);
                }

                if (!foundDomainGroup) {
                    Log.w(TAG, "AB debug domain_group not found: " + debugDomainGroup);
                }
                recursionDepth--;
                Log.d(TAG, indent() + "└─");
                return;
            }

            String debugDomain = getDebugValue("domain");
            if ("group".equals(node.type) && !debugDomain.isEmpty()) {
                for (int i = 0; i < node.children.size(); i++) {
                    TrafficNode child = node.children.get(i);
                    if ("domain".equals(child.type) && debugDomain.equals(child.id)) {
                        Log.i(TAG, "AB debug force domain: " + debugDomain);
                        allocateNode(userId, child, parentRatio, results, userInfo, targetNodes,
                                currentDomainGroupCode, currentDomainCode, currentLayerCode);
                        recursionDepth--;
                        Log.d(TAG, indent() + "└─");
                        return;
                    }
                }

                Log.w(TAG, "AB debug domain not found in current group: " + debugDomain);
                recursionDepth--;
                Log.d(TAG, indent() + "└─");
                return;
            }

            if ("group".equals(node.type) || "layer".equals(node.type)) {
                if (node.isFunnel) {
                    Log.d(TAG, indent() + "│  模式: 漏斗分配 (遍历子节点直到命中)");
                    allocateFunnel(userId, node, parentRatio, results, userInfo, targetNodes,
                            currentDomainGroupCode, currentDomainCode, currentLayerCode);
                } else {
                    Log.d(TAG, indent() + "│  模式: 互斥分配 (用户只进一个子节点)");
                    allocateMutuallyExclusive(userId, node, parentRatio, results, userInfo, targetNodes,
                            currentDomainGroupCode, currentDomainCode, currentLayerCode);
                }
            } else {
                Log.d(TAG, indent() + "│  模式: 正交分配 (遍历所有子节点)");
                for (int i = 0; i < node.children.size(); i++) {
                    TrafficNode child = node.children.get(i);
                    Log.d(TAG, indent() + "│  处理第 " + (i + 1) + "/" + node.children.size() + " 个子节点");
                    allocateNode(userId, child, 1.0f, results, userInfo, targetNodes,
                            currentDomainGroupCode, currentDomainCode, currentLayerCode);
                }
            }

            recursionDepth--;
            Log.d(TAG, indent() + "└─");
        }

        private static void allocateFunnel(
                String userId,
                TrafficNode node,
                float parentRatio,
                List<ExperimentResult> results,
                Map<String, Object> userInfo,
                Set<String> targetNodes,
                String domainGroupCode,
                String domainCode,
                String layerCode) {

            for (int i = 0; i < node.children.size(); i++) {
                TrafficNode child = node.children.get(i);
                if (ABHistory.containsNode(targetNodes, child.type, child.id)) {
                    Log.d(TAG, indent() + "│  🎯 漏斗层目标拦截命中 (已通过老用户或白名单保送): " + child.id);
                    allocateNode(userId, child, parentRatio, results, userInfo, targetNodes,
                            domainGroupCode, domainCode, layerCode);
                    return;
                }
            }

            Log.d(TAG, indent() + "│  漏斗分配: 顺序遍历子节点");
            for (int i = 0; i < node.children.size(); i++) {
                TrafficNode child = node.children.get(i);
                Log.d(TAG, indent() + "│    检查子节点: " + child.id);

                int previousResultCount = results.size();
                allocateNode(userId, child, parentRatio, results, userInfo, targetNodes,
                        domainGroupCode, domainCode, layerCode);

                if (results.size() > previousResultCount) {
                    Log.d(TAG, indent() + "│  🎯 漏斗分配: 已进入子节点 " + child.id + "，停止遍历");
                    return;
                }
            }
            Log.w(TAG, indent() + "│  ⚠ 漏斗分配: 未进入任何子节点");
        }

        private static void allocateMutuallyExclusive(
                String userId,
                TrafficNode node,
                float parentRatio,
                List<ExperimentResult> results,
                Map<String, Object> userInfo,
                Set<String> targetNodes,
                String domainGroupCode,
                String domainCode,
                String layerCode) {

            // --- 目标拦截 ---
            // 只要同层有被标记了"白名单/老记录"的存在，无视底层Hash，保证同层只走这条路
            for (int i = 0; i < node.children.size(); i++) {
                TrafficNode child = node.children.get(i);
                if (ABHistory.containsNode(targetNodes, child.type, child.id)) {
                    Log.d(TAG, indent() + "│  🎯 互斥层目标拦截命中 (已通过老用户或白名单保送): " + child.id);
                    allocateNode(userId, child, parentRatio, results, userInfo, targetNodes,
                            domainGroupCode, domainCode, layerCode);
                    return;
                }
            }

            String hashKey = userId + "_" + node.type + "_" + node.id;
            double hashValue = hash(hashKey);

            Log.d(TAG, indent() + "|  hashKey: " + hashKey);
            Log.d(TAG, indent() + "│  互斥分配哈希: " + String.format("%.6f", hashValue));
            Log.d(TAG, indent() + "│  累积分配过程:");

            double hashValue100 = hashValue * 100.0;

            for (int i = 0; i < node.children.size(); i++) {
                TrafficNode child = node.children.get(i);

                float lowerBound = child.usedFlowPercentage;
                float upperBound = child.usedFlowPercentage + child.flowPercentage;

                String range = String.format("[%.4f, %.4f)", lowerBound, upperBound);
                boolean hit = hashValue100 >= lowerBound && hashValue100 < upperBound;

                Log.d(TAG, indent() + "│    " + (i + 1) + ". " + child.type +
                        " 范围=" + range +
                        " 流量=" + String.format("%.1f%%", child.flowPercentage) +
                        (hit ? " ← 命中!" : ""));

                if (hit) {
                    Log.d(TAG, indent() + "│  🎲 哈希值落在此范围，进入该子节点");
                    allocateNode(userId, child, parentRatio, results, userInfo, targetNodes,
                            domainGroupCode, domainCode, layerCode);
                    return;
                }
            }

            Log.w(TAG, indent() + "│  ⚠ 哈希值未命中任何子节点");
        }

        private static boolean shouldAllocate(String userId, TrafficNode node, float parentRatio) {
            // 没有设置配比（flowPercentage <= 0.0f）的全局/停止实验，普通流量自然分配不要进
            // (有测试名单/历史记录的用户早已经在 targetNodes.contains 拦截中保送进去了)
            if (node.flowPercentage <= 0.0f) {
                Log.d(TAG, indent() + "│  流量判断: 无配比实验，正常流量拦截");
                return false;
            }

            if ("default_layer".equals(node.parentId)) {
                String hashKey = userId + "_layer_" + node.parentId;
                double hashValue = hash(hashKey);

                double hashValue100 = hashValue * 100.0;
                float lowerBound = node.usedFlowPercentage;
                float upperBound = node.usedFlowPercentage + (node.flowPercentage * parentRatio);

                boolean result = hashValue100 >= lowerBound && hashValue100 < upperBound;

                Log.d(TAG, indent() + "│  流量判断:");
                Log.d(TAG, indent() + "|    hashKey: " + hashKey);
                Log.d(TAG, indent() + "│    哈希值: " + String.format("%.6f", hashValue));
                Log.d(TAG, indent() + "│    范围:   [" + String.format("%.4f", lowerBound) + ", " +
                        String.format("%.4f", upperBound) + ")");
                Log.d(TAG, indent() + "│    结果:   " + (result ? "✓ 通过" : "✗ 未通过"));

                return result;
            } else {
                return true;
            }
        }

        private static TrafficNode selectVariant(String userId, TrafficNode experiment, Set<String> targetNodes) {
            if (experiment.children == null || experiment.children.isEmpty()) {
                return null;
            }

            // 目标拦截：老记录/白名单锁定了哪个变体，不再随机哈希，直接返回那个固定配置
            for (TrafficNode variant : experiment.children) {
                if (ABHistory.containsVariant(targetNodes, experiment.id, variant.id)) {
                    Log.d(TAG, indent() + "│    🎯 存在老记录/名单命中，强制返回锁定变体: " + variant.id);
                    return variant;
                }
            }

            String hashKey = userId + "_experiment_" + experiment.id;
            double hashValue = hash(hashKey);

            Log.d(TAG, indent() + "│  选择变体:");
            Log.d(TAG, indent() + "|    hashKey: " + hashKey);
            Log.d(TAG, indent() + "│    变体哈希: " + String.format("%.6f", hashValue));

            double hashValue100 = hashValue * 100.0;

            for (int i = 0; i < experiment.children.size(); i++) {
                TrafficNode variant = experiment.children.get(i);

                float lowerBound = variant.usedFlowPercentage;
                float upperBound = variant.usedFlowPercentage + variant.flowPercentage;

                String range = String.format("[%.4f, %.4f)", lowerBound, upperBound);
                boolean hit = hashValue100 >= lowerBound && hashValue100 < upperBound;

                Log.d(TAG, indent() + "│      " + (i + 1) + ". " + variant.id +
                        " 范围=" + range +
                        " 流量=" + String.format("%.1f%%", variant.flowPercentage) +
                        (hit ? " ← 选中!" : ""));

                if (hit) {
                    return variant;
                }
            }

            TrafficNode fallback = experiment.children.get(experiment.children.size() - 1);
            Log.d(TAG, indent() + "│    使用保底变体: " + fallback.id);
            return fallback;
        }

        private static boolean checkUserRules(JSONObject userRules, Map<String, Object> userInfo) {
            if (userRules == null) {
                Log.d(TAG, indent() + "│  UserRules检查: 无规则，默认通过");
                return true;
            }
            try {
                String logicalOp = userRules.optString("logical_operation", "and");
                if (logicalOp.isEmpty()) {
                    logicalOp = "and";
                }
                List<Boolean> results = new ArrayList<>();

                Log.d(TAG, indent() + "│  UserRules检查: 逻辑=" + logicalOp.toUpperCase());

                if (userRules.has("user_condition") && !userRules.isNull("user_condition")) {
                    Log.d(TAG, indent() + "│  -> 检查 user_condition");
                    results.add(checkCondition(userRules.getJSONObject("user_condition"), userInfo));
                }

                if (userRules.has("group_condition") && !userRules.isNull("group_condition")) {
                    Log.d(TAG, indent() + "│  -> 检查 group_condition");
                    results.add(checkCondition(userRules.getJSONObject("group_condition"), userInfo));
                }

                if (userRules.has("tag_condition") && !userRules.isNull("tag_condition")) {
                    Log.d(TAG, indent() + "│  -> 检查 tag_condition");
                    results.add(checkCondition(userRules.getJSONObject("tag_condition"), userInfo));
                }

                if (userRules.has("event_condition") && !userRules.isNull("event_condition")) {
                    Log.d(TAG, indent() + "│  -> 检查 event_condition");
                    String eventCondition = userRules.getString("event_condition");
                    Object eventObj = userInfo.get(eventCondition);
                    boolean eventMatch = (eventObj != null && "1".equals(eventObj.toString()));
                    results.add(eventMatch);
                    Log.d(TAG, indent() + "│    事件匹配(" + eventCondition + ") → " + (eventMatch ? "✓ 通过" : "✗ 失败"));
                }

                if (results.isEmpty()) {
                    return true;
                }

                boolean finalResult;
                if ("and".equalsIgnoreCase(logicalOp)) {
                    finalResult = !results.contains(false);
                } else {
                    finalResult = results.contains(true);
                }

                Log.d(TAG, indent() + "│  UserRules最终结果: " + (finalResult ? "✓ 通过" : "✗ 失败"));
                return finalResult;
            } catch (JSONException e) {
                Log.e(TAG, "Error checking user rules", e);
                return false;
            }
        }

        private static boolean checkCondition(JSONObject condition, Map<String, Object> userInfo) {
            if (condition == null || userInfo == null) {
                Log.d(TAG, indent() + "│  条件检查: 无条件，默认通过");
                return true;
            }

            try {
                JSONArray filters = condition.optJSONArray("filters");
                if (filters == null) {
                    return true;
                }
                String logicalOp = condition.optString("logical_operation", "and");
                if (logicalOp.isEmpty()) {
                    logicalOp = "and";
                }

                Log.d(TAG, indent() + "│  条件检查: " + filters.length() + " 个过滤器, 逻辑=" + logicalOp.toUpperCase());

                List<Boolean> results = new ArrayList<>();

                for (int i = 0; i < filters.length(); i++) {
                    JSONObject filter = filters.getJSONObject(i);
                    boolean matched = checkFilter(filter, userInfo);
                    results.add(matched);

                    Log.d(TAG, indent() + "│    过滤器" + (i + 1) + ": " +
                            filter.getString("key") + " " + filter.getString("calcu_symbol") +
                            " " + (filter.optJSONArray("value") != null ? filter.optJSONArray("value").opt(0) : "N/A") +
                            " → " + (matched ? "✓ 通过" : "✗ 失败"));
                }

                boolean finalResult;
                if ("and".equalsIgnoreCase(logicalOp)) {
                    finalResult = !results.contains(false);
                } else {
                    finalResult = results.contains(true);
                }

                Log.d(TAG, indent() + "│  最终结果: " + (finalResult ? "✓ 通过" : "✗ 失败"));
                return finalResult;

            } catch (JSONException e) {
                Log.e(TAG, "Error checking condition", e);
                return false;
            }
        }

        private static boolean checkFilter(JSONObject filter, Map<String, Object> userInfo) {
            try {
                String key = filter.getString("key");
                String type = filter.optString("type", "string");
                String calcuSymbol = filter.getString("calcu_symbol");
                JSONArray valueArray = filter.optJSONArray("value");

                Object userValue = userInfo.get(key);

                if ("ex".equalsIgnoreCase(calcuSymbol)) {
                    return userValue != null;
                }
                if ("nex".equalsIgnoreCase(calcuSymbol)) {
                    return userValue == null;
                }

                if (userValue == null) {
                    return false;
                }

                if ("float".equals(type) || "int".equals(type)) {
                    return checkNumericFilter(calcuSymbol, userValue, valueArray);
                } else {
                    return checkStringFilter(calcuSymbol, userValue.toString(), valueArray);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking filter", e);
                return false;
            }
        }

        private static boolean checkNumericFilter(String calcuSymbol, Object userValue, JSONArray valueArray) {
            if (valueArray == null || valueArray.length() == 0) {
                return false;
            }
            try {
                float userNum = Float.parseFloat(userValue.toString());
                String symbol = calcuSymbol.toLowerCase(Locale.US);

                switch (symbol) {
                    case "scope":
                        if (valueArray.length() < 2) {
                            return false;
                        }
                        float minNum = Float.parseFloat(valueArray.getString(0));
                        float maxNum = Float.parseFloat(valueArray.getString(1));
                        return userNum >= minNum && userNum <= maxNum;
                    case "eq":
                        for (int i = 0; i < valueArray.length(); i++) {
                            if (userNum == Float.parseFloat(valueArray.getString(i))) {
                                return true;
                            }
                        }
                        return false;
                    case "neq":
                        for (int i = 0; i < valueArray.length(); i++) {
                            if (userNum == Float.parseFloat(valueArray.getString(i))) {
                                return false;
                            }
                        }
                        return true;
                }

                float compareNum = Float.parseFloat(valueArray.getString(0));
                switch (symbol) {
                    case "gt":
                        return userNum > compareNum;
                    case "gteq":
                        return userNum >= compareNum;
                    case "lt":
                        return userNum < compareNum;
                    case "lteq":
                        return userNum <= compareNum;
                    default:
                        return false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking numeric filter", e);
                return false;
            }
        }

        private static boolean checkStringFilter(String calcuSymbol, String userStr, JSONArray valueArray) {
            String symbol = calcuSymbol.toLowerCase(Locale.US);
            boolean isNegative = "neq".equals(symbol) || "nrm".equals(symbol);

            if (valueArray == null || valueArray.length() == 0) {
                return isNegative;
            }

            boolean isRegex = "rm".equals(symbol) || "nrm".equals(symbol);
            boolean matchAny = false;

            if (isRegex) {
                for (int i = 0; i < valueArray.length(); i++) {
                    try {
                        String val = valueArray.getString(i);
                        if (!val.isEmpty() && userStr.matches(val)) {
                            matchAny = true;
                            break;
                        }
                    } catch (Exception e) {
                        // Ignore pattern compile/match errors
                    }
                }
            } else {
                for (int i = 0; i < valueArray.length(); i++) {
                    try {
                        String val = valueArray.getString(i);
                        if (userStr.equalsIgnoreCase(val)) {
                            matchAny = true;
                            break;
                        }
                    } catch (Exception e) {
                        // Ignore json retrieval errors
                    }
                }
            }

            return isNegative ? !matchAny : matchAny;
        }

        public static class FxHasher {
            private static final long SEED64 = 0x517cc1b727220a95L;
            private static final int ROTATE = 5;

            public static long hash64(String input) {
                byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
                long hash = 0L;
                int pos = 0;

                while (pos + 8 <= bytes.length) {
                    long word = bytesToLongLE(bytes, pos);
                    hash = hashWord(hash, word);
                    pos += 8;
                }

                if (pos + 4 <= bytes.length) {
                    long word = bytesToIntLE(bytes, pos) & 0xFFFFFFFFL;
                    hash = hashWord(hash, word);
                    pos += 4;
                }

                while (pos < bytes.length) {
                    long word = bytes[pos] & 0xFFL;
                    hash = hashWord(hash, word);
                    pos++;
                }

                hash = hashWord(hash, 0xFFL);
                return hash;
            }

            private static long hashWord(long hash, long word) {
                hash = Long.rotateLeft(hash, ROTATE) ^ word;
                hash = hash * SEED64;
                return hash;
            }

            private static long bytesToLongLE(byte[] bytes, int offset) {
                return ((long) (bytes[offset] & 0xFF)) |
                        ((long) (bytes[offset + 1] & 0xFF) << 8) |
                        ((long) (bytes[offset + 2] & 0xFF) << 16) |
                        ((long) (bytes[offset + 3] & 0xFF) << 24) |
                        ((long) (bytes[offset + 4] & 0xFF) << 32) |
                        ((long) (bytes[offset + 5] & 0xFF) << 40) |
                        ((long) (bytes[offset + 6] & 0xFF) << 48) |
                        ((long) (bytes[offset + 7] & 0xFF) << 56);
            }

            private static int bytesToIntLE(byte[] bytes, int offset) {
                return (bytes[offset] & 0xFF) |
                        ((bytes[offset + 1] & 0xFF) << 8) |
                        ((bytes[offset + 2] & 0xFF) << 16) |
                        ((bytes[offset + 3] & 0xFF) << 24);
            }

            public static double hashNormalized(String input) {
                long hash = hash64(input);
                if (hash >= 0) {
                    return (double) hash / (double) Long.MAX_VALUE / 2.0;
                } else {
                    return 0.5 + ((double) (hash - Long.MIN_VALUE) / (double) Long.MAX_VALUE / 2.0);
                }
            }
        }

        private static double hash(String input) {
            return FxHasher.hashNormalized(input);
        }

        public static TrafficTreeContext buildTreeFromJSON(JSONObject config) {
            try {
                Log.d(TAG, "开始从JSON构建流量树...");

                TrafficTreeContext contextResponse = new TrafficTreeContext();
                TrafficNode root = new TrafficNode("root", "root");
                contextResponse.root = root;

                JSONArray domainGroups = config.getJSONArray("flow_domain_group");
                Map<String, TrafficNode> domainGroupMap = new HashMap<>();
                Log.d(TAG, "流量域组数量: " + domainGroups.length());

                for (int i = 0; i < domainGroups.length(); i++) {
                    JSONObject dg = domainGroups.getJSONObject(i);
                    TrafficNode groupNode = new TrafficNode(dg.getString("id"), "group");
                    String sn = ABTestAllocator.optSafeString(dg, "serial_number");
                    if (sn != null)
                        contextResponse.idToSerialNumberMap.put(groupNode.id, sn);
                    groupNode.userScope = ABTestAllocator.optSafeString(dg, "user_scope");
                    domainGroupMap.put(groupNode.id, groupNode);
                    root.children.add(groupNode);
                }

                JSONArray domains = config.getJSONArray("flow_domain");
                Map<String, TrafficNode> domainMap = new HashMap<>();
                Map<String, String> domainToGroupMap = new HashMap<>(); // 追溯依赖用
                Log.d(TAG, "流量域数量: " + domains.length());

                for (int i = 0; i < domains.length(); i++) {
                    JSONObject d = domains.getJSONObject(i);
                    TrafficNode domainNode = new TrafficNode(d.getString("id"), "domain");
                    String sn = ABTestAllocator.optSafeString(d, "serial_number");
                    if (sn != null)
                        contextResponse.idToSerialNumberMap.put(domainNode.id, sn);
                    domainNode.userScope = ABTestAllocator.optSafeString(d, "user_scope");
                    domainNode.flowPercentage = (float) d.getDouble("flow_percentage");
                    domainNode.usedFlowPercentage = (float) d.optDouble("used_flow_percentage", 0.0);

                    String groupId = d.getString("domain_group_id");
                    TrafficNode group = domainGroupMap.get(groupId);
                    if (group != null) {
                        group.children.add(domainNode);
                    }

                    domainMap.put(domainNode.id, domainNode);
                    domainToGroupMap.put(domainNode.id, groupId);
                    contextResponse.allDomainCodes.add(domainNode.id);
                }

                JSONArray layers = config.optJSONArray("flow_layer");
                Map<String, TrafficNode> layerMap = new HashMap<>();
                Map<String, String> layerToDomainMap = new HashMap<>(); // 追溯依赖用
                if (layers != null) {
                    Log.d(TAG, "流量层数量: " + layers.length());
                    for (int i = 0; i < layers.length(); i++) {
                        JSONObject l = layers.getJSONObject(i);
                        TrafficNode layerNode = new TrafficNode(l.getString("id"), "layer");
                        String sn = ABTestAllocator.optSafeString(l, "serial_number");
                        if (sn != null)
                            contextResponse.idToSerialNumberMap.put(layerNode.id, sn);

                        layerNode.userScope = ABTestAllocator.optSafeString(l, "user_scope");
                        layerNode.isFunnel = l.optBoolean("is_funnel", false);

                        String domainId = l.optString("flow_domain_id", "");
                        if (!domainId.isEmpty()) {
                            TrafficNode domain = domainMap.get(domainId);
                            if (domain != null) {
                                domain.children.add(layerNode);
                            }
                            layerToDomainMap.put(layerNode.id, domainId);
                        } else {
                            // 全局层级，没有挂载在任何特定 Domain，直接挂在 Root 下
                            root.children.add(layerNode);
                        }

                        layerMap.put(layerNode.id, layerNode);
                    }
                }

                JSONArray experiments = config.getJSONArray("experiment");
                Log.d(TAG, "实验数量: " + experiments.length());

                for (int i = 0; i < experiments.length(); i++) {
                    JSONObject e = experiments.getJSONObject(i);
                    TrafficNode expNode = new TrafficNode(e.getString("id"), "experiment");
                    String sn = ABTestAllocator.optSafeString(e, "serial_number");
                    if (sn != null)
                        contextResponse.idToSerialNumberMap.put(expNode.id, sn);
                    expNode.userScope = ABTestAllocator.optSafeString(e, "user_scope");
                    expNode.flowPercentage = (float) e.optDouble("flow_percentage", 0.0);

                    expNode.usedFlowPercentage = (float) e.optDouble("used_flow_percentage", 0.0);
                    expNode.userCondition = e.optJSONObject("user_condition");
                    expNode.userRules = e.optJSONObject("user_rules");

                    String layerId = e.optString("layer_id", "");
                    String dId = null;
                    String dgId = null;

                    if (layerId.isEmpty()) {
                        Log.d(TAG, "  全局实验(无layer): " + expNode.id);
                        root.children.add(expNode);
                        expNode.parentId = "default_layer";
                    } else {
                        TrafficNode layer = layerMap.get(layerId);
                        if (layer != null) {
                            layer.children.add(expNode);
                            expNode.parentId = layerId;

                            dId = layerToDomainMap.get(layerId);
                            if (dId != null) {
                                dgId = domainToGroupMap.get(dId);
                            }
                        } else {
                            Log.w(TAG, "  实验 " + expNode.id + " 的layer_id无效: " + layerId);
                        }
                    }

                    ExperimentContextInfo expCtx = new ExperimentContextInfo(dgId, dId, layerId);
                    contextResponse.experimentContextMap.put(expNode.id, expCtx);

                    JSONArray variants = e.getJSONArray("variant_group");
                    for (int j = 0; j < variants.length(); j++) {
                        JSONObject v = variants.getJSONObject(j);
                        TrafficNode variantNode = new TrafficNode(v.getString("id"), "variant");
                        String vsn = optSafeString(v, "serial_number");
                        if (vsn != null)
                            contextResponse.idToSerialNumberMap.put(variantNode.id, vsn);
                        variantNode.userScope = ABTestAllocator.optSafeString(v, "user_scope");
                        variantNode.flowPercentage = (float) v.optDouble("flow_percentage", 0.0);

                        variantNode.usedFlowPercentage = (float) v.optDouble("used_flow_percentage", 0.0);
                        variantNode.configParams = v.optJSONObject("strategy");

                        JSONArray testUsers = v.optJSONArray("test_user_codes");
                        if (testUsers != null) {
                            for (int k = 0; k < testUsers.length(); k++) {
                                variantNode.testUserCodes.add(testUsers.getString(k));
                            }
                        }

                        expNode.children.add(variantNode);
                    }

                    contextResponse.allExperiments.add(expNode);
                }

                Log.d(TAG, "流量树构建完成");
                return contextResponse;

            } catch (JSONException e) {
                Log.e(TAG, "Error building tree from JSON", e);
                return null;
            }
        }
    }
}
