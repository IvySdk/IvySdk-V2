package com.ivy.ads;

import android.os.Build;
import android.util.Log;

import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.IKeys;
import com.ivy.base.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class BxSelector {
    private static final String TEST_BX_ID_KEY = "_test_bx_id_";
    private static final Random RANDOM = new Random();

    private static String isLowDevice(JSONArray lowDevices){
        if (lowDevices == null){
            return "no";
        }
        if(LocalStorage.Instance.containsKey("low_device")){
            return LocalStorage.Instance.decodeString("low_device", "no");
        }

//        String[] lowDevices = {
//                "801SO","901SO","1918N","8505F","100011886A","100071483","100071485A","100092980","A12","A13_EEA","A70","ACTAB1422","AKT","Active_T1","Aquaman_10_Smart_WIFI","B2N_sprout","B140DL","B170D","BOL","BP","BUZZ_5_Prime","BV4800","Bremerton","C36","Cascabel","DDV_sprout","Doha_TMO","E7LT","Elite_D65","F11","F52B","G10_EEA","G518","G0892","G0892_GMOB","G0892_MXT","G0892_SP","G0892_SYN","G0894","Gevo_V10","H1","H9493","HPPL63A","HSSDM450QC","HWALP","HWBLN-H","HWFRD","HWJSN-HM","HWNXT","HWRVL","HWTRT-Q","HWVIE","HydrogenONE","I3113","IT_701A1","IT_701A_EEA","Infinix-X655","K7Pro","KY21L-RG100","L-03K","LAVIET88HD1","LMX04","LYNX","LYV","M1","M10_R01","MATRIX","MAXIMUS_P1","MX-A65","MX-NN62WW01","N6001LA","NEW_10","NEXG_N71","NOA_P1","ORO","OT5","OT11","OUKITEL_P1","OnePlus7TProNR","OnePlusN100","OrbitM20","P1","P2a42","P027","P30","P30_B","P30_EEA","P30_EUR","P30_ROW","P30_US","P60Pro","PAD10_PRO","PCZ","PCZ-u","PGN507","PRIME_P1","Pad70_NEU","Panther_00EEE","Power_Armor_16S","RNNA_sprout","RNN_sprout","S5","S11","S11_EEA","S11_ROW","S22_EEA","S42","S42G","S0480LL_ALTAN","SC-01L","SG907SH","SH-03J","SH-RM12","SLD_sprout","SMART_W32","STO","SV67Q","Stratus_C7","Sunshine_T1_Elite","T10P","T10P_A","T10P_B","T10P_ROW","T10P_US","T12-EEA","T13_EEA","T22-EEA","TAB10","TAB10_1","TAB10_EEA","TAB10_EU","TAB10_EUR","TAB10_ROW","TAB-7887-32G3GB","TECNO-KF6","TECNO-Mobile-KJ6","TL10","U319AA","V10","V10S","VIA_M30_Plus","VILLAON-V30","VILLAON-V652L","Wildfire_E6_Star","X6C6F","X12","X50","X95","X306F","X306FA","X306V","X306X","X306XA","X704F","Y103_EEA","Z8888S","a6plte","a6pltechn","aka","albus","angelica","angelicain","borneo","cereus","conquest-S11","cv5a","ellis","elsa","evert_n","geralt_cheets","griffin","gta2xlwifichn","gtactive5","gts9fe","gts9fepwifi","gts9fewifi","htc_ocnuhl","htc_ocnuhljapan","htc_ocnwhl","hwbeethoven","itel-A632W","itel-A667L","j6primelte","j8y18lte","jadelte","jadeltechn","jasmine_sprout","kukui_cheets","mcv5a","mdh10lm","mdh15lm","meh15lm","meh20lm","messi","mhn","nautilus_cheets","nikel","noblelte","nobleltecmcc","nobleltetmo","olivelite","on7xreflte","pad6_EEA","potter","potter_nt","sabahl","staryu_cheets","ysl","zenlte","zenltetmo",
//        };

        String tt = Build.DEVICE;
        String ts = Build.MODEL;
        for (int i = lowDevices.length()-1; i >= 0; i--) {
            String lowDevice = lowDevices.optString(i);
            if (tt.equals(lowDevice) || ts.equals(lowDevice)){
                LocalStorage.Instance.encodeString("low_device", "yes");
                return "yes";
            }
        }
        LocalStorage.Instance.encodeString("low_device", "no");
        return "no";
    }

    private static Map<String, Object> getContext(JSONArray lowDevices){
        Map<String, Object> map = new HashMap<>();
        map.put("country", Util.countryCode());
        map.put("media_source", LocalStorage.Instance.decodeString(IKeys.KEY_AF_MEDIA_SOURCE, "Organic").toLowerCase());
        map.put("campaign_id", LocalStorage.Instance.decodeString(IKeys.KEY_AF_CAMPAIGN_ID, ""));
        map.put("ad_value", AdValueTagProcessor.getInstance().getAdValue());
        map.put("low_device", isLowDevice(lowDevices));
        map.put("device_ram", Util.byte2GB(Util.totalMemory()));
        map.put("app_version", versionName2Number());
        map.put("api_version", Build.VERSION.SDK_INT);
        map.put("install_days", installDays());
        for(Map.Entry<String, Object> e : map.entrySet()){
            Log.d("hoho", e.getKey() + " " + e.getValue());
        }
        return map;
    }

    private static long installDays(){
        try {
            long firstInstallTime = LocalStorage.Instance.decodeLong("_sdk_first_time_", 0L);
            long duration = System.currentTimeMillis() - firstInstallTime;
            if (duration <= 0) return 0;
            return duration / 86400000L;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static long versionName2Number(){
        String version = Util.versionName();
        if (version == null) version = "";

        String[] parts = version.split("\\.");
        StringBuilder sb = new StringBuilder();

        // 强制循环 5 次
        for (int i = 0; i < 5; i++) {
            int number = 0;
            if (i < parts.length) {
                try {
                    number = Integer.parseInt(parts[i]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            sb.append(String.format(Locale.ENGLISH, "%03d", number));
        }

        return Long.parseLong(sb.toString());
    }

    public static String pickBx(JSONObject root) throws Exception {
        JSONArray rulesArr = root.optJSONArray("rules");
        if (rulesArr == null) return "default";
        Map<String, Object> ctx = getContext(root.optJSONArray("low_devices"));

        // 转为列表并按 priority 降序
        List<JSONObject> rules = new ArrayList<>();
        for (int i = 0; i < rulesArr.length(); i++) rules.add(rulesArr.getJSONObject(i));
        Collections.sort(rules, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                return Integer.compare(a.optInt("priority", 0), b.optInt("priority", 0));
            }
        });

        String lastExp = LocalStorage.Instance.decodeString("_last_exp_", "");
        String lastBx = LocalStorage.Instance.decodeString("_bx_id_", "");
        boolean sticky = LocalStorage.Instance.decodeBoolean("_last_sticky_", false);

        // 规则是: 1. 如果没有分配过, 那么只要满足条件就直接进入
        //        2. 如果已经分配过, 并且是粘滞的, 那么就需要直接进入这个, 不需要再分配, 除非这个exp已经丢失了
        //        3. 如果已经分配过, 并且这一次分配的还是这个实验, 那么也不需要再分配实验和变体

        if (sticky){
            for(JSONObject r : rules){
                String expName = r.optString("expName");
                if (expName.equals(lastExp)){
                    Log.d("hoho", "sticky " + lastExp + " " + lastBx);
                    return lastBx;
                }
            }
        }

        for (JSONObject rule : rules) {
            if (!matchRule(rule.optJSONArray("condition"), ctx)) continue;
            String expName = rule.optString("expName");
            if (expName.equals(lastExp)){
                return lastBx;
            }
            LocalStorage.Instance.encodeString("_last_exp_", expName);
            LocalStorage.Instance.encodeBoolean("_last_sticky_", rule.optBoolean("sticky", false));

            Object bxNode = rule.opt("bx");
            JSONArray percentArr = rule.optJSONArray("ratio");

            // bx 是字符串
            if (bxNode instanceof String) return (String) bxNode;

            // bx 是数组
            if (bxNode instanceof JSONArray) {
                JSONArray bxArr = (JSONArray) bxNode;
                List<String> candidates = new ArrayList<>();
                for (int i = 0; i < bxArr.length(); i++) candidates.add(bxArr.getString(i));

                double[] weights = buildWeights(percentArr, candidates.size());
                return weightedPick(candidates, weights);
            }
        }
        return "default";
    }

    static String getTestBxId(boolean debug) {
        if (!debug) return "";
        String testBxId = LocalStorage.Instance.decodeString(TEST_BX_ID_KEY, "");
        if (testBxId == null) return "";
        if (!testBxId.isEmpty()) {
            Log.d("hoho", "use test bx id " + testBxId);
        }
        return testBxId;
    }

    // condition 外层 OR，内层 AND
    private static boolean matchRule(JSONArray condition, Map<String, Object> ctx) {
        if (condition == null) return false;
        for (int i = 0; i < condition.length(); i++) {
            JSONArray andGroup = condition.optJSONArray(i);
            if (andGroup == null) continue;
            boolean andOk = true;
            for (int j = 0; j < andGroup.length(); j++) {
                JSONObject cond = andGroup.optJSONObject(j);
                if (cond == null || !matchCond(cond, ctx)) { andOk = false; break; }
            }
            if (andOk) return true; // OR 命中
        }
        return false;
    }

    // include / exclude；区间用 [min,max]，-1 表示无界
    private static boolean matchCond(JSONObject cond, Map<String, Object> ctx) {
        String key = cond.optString("key", "");
        Object val = ctx.get(key);
        if (val == null) return false;

        JSONArray include = cond.optJSONArray("include");
        if (include != null && !inSetOrRange(val, include)) return false;

        JSONArray exclude = cond.optJSONArray("exclude");
        if (exclude != null && inSetOrRange(val, exclude)) return false;

        return true;
    }

    private static boolean inSetOrRange(Object val, JSONArray arr) {
        // 区间 [min,max]；-1 表示无界
        if (arr.length() == 2 && arr.opt(0) instanceof Number && arr.opt(1) instanceof Number) {
            double v = toDouble(val);
            double min = arr.optDouble(0, -1);
            double max = arr.optDouble(1, -1);
            if (min != -1 && v < min) return false;
            if (max != -1 && v >= max) return false;
            return true;
        }
        // 列表包含
        for (int i = 0; i < arr.length(); i++) {
            if (String.valueOf(val).equalsIgnoreCase(arr.optString(i))) return true;
        }
        return false;
    }

    private static double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return Double.NaN; }
    }

    private static double[] buildWeights(JSONArray percentArr, int size) {
        double[] w = new double[size];
        if (percentArr != null && percentArr.length() == size) {
            for (int i = 0; i < size; i++) w[i] = percentArr.optDouble(i, 0);
        } else {
            Arrays.fill(w, 1.0 / size); // 均分
        }
        return w;
    }

    private static String weightedPick(List<String> items, double[] weights) {
        double r = RANDOM.nextDouble();
        double acc = 0;
        for (int i = 0; i < items.size(); i++) {
            acc += weights[i];
            if (r <= acc) return items.get(i);
        }
        return items.get(items.size() - 1);
    }
}
