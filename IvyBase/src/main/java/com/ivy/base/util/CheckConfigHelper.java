package com.ivy.base.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.IKeys;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/* loaded from: classes8.dex */
public class CheckConfigHelper {
    static final List<String> lowLessDevice = Arrays.asList("Lenovo TB-7306F", "M8L", "F-41A", "TECNO BD4i", "TW102", "6102H", "Moto G (5S)", "TECNO BD4a", "Moto G (5) Plus", "6165H", "TECNO BD4h", "G312", "vivo 1606", "Moto G (4)", "Infinix X6511E", "TA-1032", "22071212AG", "220733SL", "TECNO BD4j", "Infinix X6512", "Nokia G11", "moto g play (2021)", "AGS-L03", "TECNO LC7", "vivo 1610", "B140DL", "moto g play (XT2093DL)", "220733SI", "SO-02K", "801SO", "moto g(8) play", "TECNO KG5h", "TA-1038", "vivo 1801", "W_C800", "100011886A", "SM-G800F", "B3-A40", "GM 5 Plus d", "EA1002", "SM-G900W8", "ASUS_X00HD", "Scepter8", "Z982", "F-02H", "TECNO KF6", "Infinix HOT 4", "U705AA", "LG-V521", "Lenovo K33b36", "P028", "Z42", "M760", "A55", "Silver Max Pro", "9024O", "XT1710-02", "Nokia 2 V Tella", "Lenovo TB-8506X", "S7-SH", "TURKCELL T70", "TECNO KB7j", "100044018G", "Lenovo TB-8506F", "REVVLPLUS C3701A", "LGLS755", "100092980", "Power Armor 14", "LAVA Z3 UP 2", "F8331", "SM-J327V", "I3113", "Nokia 3.1 C", "Infinix X626", "X7", "HTC Desire 12", "SM-G800H", "ASUS_X008D", "P13_Blue_Plus_2022", "I4113", "K10", "SM-G900T", "K87CA", "MO-01J", "KT107", "V22", "P00C", "SM-A500M", "vivo 1719", "ASUS_X008DC", "Lenovo TB-X304F", "TA-1028", "A502DL", "901FJ", "U693CL", "Infinix X571", "100003561", "COVET_PRO_LITE", "Venus E3", "LG-K200", "Infinix X559C", "SM-G901F", "Nokia C2 Tennen", "Venus e5", "SAMSUNG-SM-T337A", "X5-LG", "BG2-U01", "GM 5 Plus", "P00L", "Z22", "MAX5", "SM-T377T", "SM-N9208", "Nexus 5X", "SM-J500FN", "TECNO CA7", "Sunshine_T1_Elite", "SUGAR T10", "P024", "Lenovo TB-8505XS", "J5", "SOV35", "HUAWEI CAN-L01", "A55 Pro", "moto g play - 2023", "SM-N910F", "S31", "SM-G925T", "801LV", "SO-04H", "SM-A700F", "SM-G5700", "SM-A300FU");
    static final List<String> lowLess2Device = Arrays.asList("SKY B63", "MEIZU M6", "Note_4CAM", "Infinix HOT 4 Pro", "SL104D", "Elite P55Max", "MAGNO C2", "TECNO CX Air", "VNT_Tab8", "NITRO N62", "STAR5", "X55", "9024W", "HN2-M16Q", "PX12", "P63L", "Digit Glory1", "DVK87 Orion 8 Tab", "F8332", "Mi-4c", "Olax Prime 12", "Lenovo TB-X606XA", "Intel Braswell Chromebook", "DIGIT INFINITY", "Hisense F24", "P1_AMATEUR", "Wildfire E star", "Vodafone Pro 4G", "Logicom La Tab 129", "SH-04H", "NEO 5", "EUT110-11A", "N155DL", "KYV42", "Samsung Chromebook 3", "MT-1011QR", "Acer Chromebook R13 (CB5-312T)", "5516D", "TECNO P904", "KL9878AE", "Dslide_1021", "M4 SS4458-R", "NOVA 6.5", "T10Pro", "HT8WB1RBK02", "X63", "M22", "IRIS", "V23", "N9560", "Doro 8035", "QMobile Evok Power", "P9", "GT-N8000", "Ocean_9", "TA-10-232", "KYV44_u2", "LG-M257", "V9X", "SO-05G", "GA7133", "MAX_2_Ultra", "VZH", "S9_ultra", "M118", "LM-K735", "MAX3", "E0119", "H1", "BLADE V8", "QT2108KB", "i15_ultra", "12 Pro Max", "A5Pro", "SH-L02", "Max80 Plus", "i14proMax", "S16", "MBOX", "GIONEE F205", "A503DL", "Z-T10", "M790", "SATURN", "Vi_Note", "KL628_HL", "F8X", "A7Pro", "M27", "Z965", "M730", "TECNO WX4 Pro", "9097", "meanIT X4", "You L", "H96 PRO+", "P852X", "PAD-7-V6", "Swift 2", "SM-T530", "S6s", "TabletEXO Wave i101H", "Aries_8in", "ASUS Chromebook Flip C302", "C11", "itel A42 Plus", "La_Tab_128", "NID-7022", "ART1", "HT8WB1RBK03", "V10A", "RCT6876Q22N", "Micromax Q4310", "Diamond 2", "Mione Q11", "8CY", "i14 pro Max", "M4 SS4453-R", "MP21", "MBXse8717", "C30 Pro", "Smart 5", "Nokia C300", "M8SPROW", "V1", "K960", "TPM4G_E108", "X713", "KL9878A_EEA", "ZA555", "ZC551KL", "SOT31", "M50A MAX", "Q88D_FUFEN_EEA", "A86W", "NOTE12 PLUS", "D68S", "Y60", "Tab S8 5G", "V2s_GL", "i7_ultra", "CT201", "1804", "SMB-H8009", "V10X", "C81W_EEA", "A08", "Micromax Bharat 5 Plus", "SM-J727A", "Azupik", "M002K-2", "8076_EEA", "pixel 2 xl", "HUAWEI MLA-L01", "Welwitcha-J60", "Masstel Tab10 Edu", "SK5502", "SH-02H", "3207", "QUAD-CORE A133 c3_kt960_216_hongyida_yxx_qc9881", "SMART_W20X", "Tab 14Pro Max", "Note2_PLUS", "B787", "eSTAR BEAUTY 3 HD Quad core", "M60-EEA", "Model T1", "coolpad E2", "Archos T101 HD", "S8001", "B85", "Note4_Plus", "ELUGA Ray 550", "HEAVEN 10.1", "MID7399_WW", "K960N_32_N", "Dbook112_EEA", "MT-750QR", "C-700", "KL2018R", "TAB-001", "SHIELD Tablet K1", "Z104", "T-666", "i100", "X96mini_RP", "Friendly_TH101", "i13 pro Max", "AT701U", "ASUS_Z01BDC", "KAAN_A1", "ECHO_STUDIO", "S119", "prime5", "TK80", "TAB 7 PRO", "SC-06D", "TEO10S", "SC-813", "M121", "ELUGA I7", "TBQG1031");
    static final List<String> lowLess3Device = Arrays.asList("6102H", "6165H", "A55", "A55 Pro", "ASUS_X008D", "ASUS_X00HD", "C30 Pro", "F-02H", "F8332", "F8X", "GM 5 Plus", "GM 5 Plus d", "HT8WB1RBK02", "HTC Desire 12", "Infinix X571", "Infinix X626", "LAVA Z3 UP 2", "Lenovo TB-8506X", "M118", "M121", "M22", "M27", "M730", "M760", "MEIZU M6", "Moto G (4)", "MP21", "NOTE12 PLUS", "P00L", "P13_Blue_Plus_2022", "P9", "Redmi 4X", "Redmi 5A", "S9_ultra", "Silver Max Pro", "SO-04H", "SO-05G", "SOT31", "Tab 14Pro Max", "TECNO BD4i", "TECNO BD4j", "TECNO CA7", "TECNO LC7", "V2s_GL", "vivo 1719", "VNT_Tab8", "W_C800", "Z42");
    static final List<String> lowMoreDevice = Arrays.asList("220333QPG", "Star 5", "Nitro 8", "Infinix X627", "SHV40", "TA-1021", "SL101AE", "A201KC", "704SH", "KYV43", "SH-01K", "N152DL", "Stratus_C5_Elite", "EC1002", "Redmi 4A", "SHV48", "vivo 1724", "TECNO KG5", "TECNO KF6j", "220733SFG", "Schok Volt SV55", "Moto G (5S) Plus", "SH-02M", "Infinix X665B", "moto g(6)", "Infinix X657B", "moto e(7)", "SM-A022M", "moto g pure", "moto e(7) power", "TECNO KG5j", "220733SI", "Redmi 7", "Redmi 5A", "Infinix X6511B", "LS1542QWN", "vivo 1606", "LM-K410", "motorola one", "Infinix X6512", "220733SL", "TECNO BD4a", "TECNO BD4", "moto e5 play", "MRD-LX3", "SM-J700F", "Redmi 5", "220733SH", "itel A661L", "TECNO PR651", "Infinix PR652B", "TECNO KG5h", "moto e5", "Lenovo TB-X306F", "TECNO KC8", "itel A551L", "220743FI", "SM-J700M", "LM-X120", "Redmi 4X", "Redmi Note 5A", "Moto G (5)", "Moto E (4) Plus", "TECNO PR651H", "Lenovo TB-X306X", "Pixel 4 XL", "itel S661L", "220733SPI", "moto e5 plus", "Redmi 4", "Lenovo TB-X505F", "vivo 1817", "Lenovo TB-8505F", "Lenovo TB-8505X", "Nokia 1.4", "9137W", "A509DL", "Moto E (4)", "AMN-LX3", "VOG-TL00", "LYA-AL00", "KYV47", "LAVA Z3 UP 2", "DRA-LX2", "TECNO KF6", "ASUS_X00RD", "ACK2326", "Lenovo TB-8505FS", "CPH2477", "CPH2471", "CPH2269", "vivo 1906", "V2026", "vivo 1904", "CPH2387", "CPH2127", "TECNO BF7");
    static final List<String> lowMoreDevice2 = Arrays.asList("220333QPG", "220733SH", "220733SPI", "220743FI", "Infinix X627", "Infinix X657B", "Infinix X665B", "Lenovo TB-8505X", "Lenovo TB-X306X", "Nokia 1.4", "Redmi 4", "Redmi 4A", "Redmi 4X", "Redmi 5A", "Schok Volt SV55", "Star 5", "TECNO BD4", "TECNO BF7", "TECNO KF6j", "vivo 1724");
    static final List<String> deviceMoreANR = Arrays.asList("Nokia G10", "100011886", "Nokia G20", "Nokia 2.4", "Nokia 2.3", "Nokia 3.4", "Nokia 7.2", "Nokia 3.2", "G300", "TECNO BD2p", "Nokia 6.2", "Nokia C100", "Nokia C10", "Nokia 4.2", "U316AT", "Nokia C1 2nd Edition", "EC211002", "Nokia 3.4", "F-02L", "W-K610-EEA", "W-K610-OPE", "U616AT", "Alpha", "Alpha 950", "Scepter 8 Tablet", "RCT6K03W13", "K87CC", "TYD-108H", "F-03H", "PSP7550DUO", "ES-MAX10", "M653", "Lenovo TB2-X30L", "XT1585", "RCT6A03W13E", "Nexus 5", "Z558VL", "Stratus_C5", "Elite J55US", "NX589J", "TEENO", "RC555L", "SO-02G", "SHV32", "N5502L", "U325AC", "TB-X704A", "Vestel_5000_2gb", "HUAWEI KII-L21", "TURKCELL T80", "Lenovo P1a41", "Alcatel_5098O", "GATA30812", "iQT800", "Nokia 5.1", "F-04H", "Lenovo TB2-X30F", "F-05J", "Moto X Play", "E6633", "VIA F30 Plus", "ASUS_Z010D", "SM-S903VL", "ASUS_Z01BDB", "KIW-L21", "LT C1400", "Gtel Infinity 9", "Hisense F23", "DS-CL1", "C21 Pro", "X2", "Nexus 7", "SH-02J", "SOV32", "Infinix X603", "Mione Q10", "SM-T330NU", "EC211003", "A0001", "F-04J", "C80", "i13 Pro Max", "WP18", "X1030X", "9008A", "i14 Pro Max", "STV100-4", "K7", "Moto C Plus", "TECNO BD2d", "100011885", "Wiko U614AS", "SO-01J", "TECNO BE6", "Lenovo TB-X103F", "Nokia 2.2", "HPP-L55B", "Nokia 3.1", "Core-X3", "Lenovo TB-X104F", "Star 4", "U705AC", "TA-1020", "100071485", "TECNO BD3", "Moto G Play", "Wiko U316AT", "100071483", "TMRVL4G", "TECNO BE6j", "Nokia C2 2nd Edition", "Lenovo TB-7305X", "itel S661W", "itel P552W", "itel A631W", "itel P551W", "A60", "itel A662L", "moto g(6) play", "SM-G530H", "TECNO B1c", "TECNO LC6", "VOG-TL00", "JSN-AL00a", "220733SG", "MAR-AL00", "220733SFG", "RMX3231", "vivo 2015", "V2217", "RMX2185");
    static final List<String> lowDeviceHighCost = Arrays.asList("T602DL", "T603DL", "EA211002", "U318AA", "4188R", "ZTE Blade A3 2020", "4188C", "N1374DL", "U319AA", "WTCKT01", "moto e13", "U304AA", "Infinix X6515", "23026RN54G", "23021RAA2Y", "M2003J15SC", "RMX3830", "Redmi Note 8", "M2004J19C", "M2006C3LG");
    static final List<String> oppoLowDevice = Arrays.asList("CPH2179", "CPH2185", "CPH2239", "CPH2273", "CPH2325", "PFTM20");
    static final List<String> group1Device = Arrays.asList("CPH2139", "CPH2271", "V2254", "23028RN4DG", "itel A665L", "23028RNCAG", "CPH2239", "CPH2325", "CPH2591");
    static final List<String> group2Device = Arrays.asList("RMX3511", "22126RN91Y", "22120RN86G", "23129RN51X", "23053RN02Y", "RMX3834", "V2204", "23124RA7EO", "SM-A155F", "TECNO BG6", "Infinix X6525");
    static final List<String> highCrashDevice = Arrays.asList("SM-J250F", "moto g(6)", "S42", "SM-T380", "SM-T810", "SM-T555", "SM-T815", "SM-T550", "LG-V530", "itel L6502");
    static final String deviceModel = Build.MODEL;
    static final int releaseCode = Build.VERSION.SDK_INT;
    public static boolean isCheckTest = false;
    static long totalMem = 0;
    public static double totalMemG = 0.0d;
    public static double availMemG = 0.0d;
    static boolean isPreload = false;
    static String s_optimize_userwaynum = "normal";

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 2 */
    public static String checkConfigB(Context context) {
        totalMemG = getTotalMemGCache(context);
        double d2 = totalMemG;
        if (d2 < 1.1d || d2 == 1.1d) {
            s_optimize_userwaynum = "lowram";
        } else {
            try {
                int i2 = releaseCode;
                if (i2 < 24) {
                    s_optimize_userwaynum = "lowos6";
                } else {
                    String str = deviceModel;
                    if (deviceMoreANR.contains(str)) {
                        s_optimize_userwaynum = "lowDeviceHighAnr";
                    } else if (lowDeviceHighCost.contains(str)) {
                        s_optimize_userwaynum = "lowDeviceHighCost";
                    } else {
                        double d4 = totalMemG;
                        if (d4 < 2.1d || d4 == 2.1d) {
                            if (lowLessDevice.contains(str) || lowLess2Device.contains(str)) {
                                s_optimize_userwaynum = "lowless";
                            } else if (lowMoreDevice.contains(str)) {
                                s_optimize_userwaynum = "lowmore";
                            } else {
                                s_optimize_userwaynum = "ram1_2";
                            }
                        } else if (lowLessDevice.contains(str) || lowLess2Device.contains(str)) {
                            s_optimize_userwaynum = "lowless";
                        } else if (lowMoreDevice.contains(str)) {
                            s_optimize_userwaynum = "lowmore";
                        } else {
                            double d5 = totalMemG;
                            if (d5 < 3.1d || d5 == 3.1d) {
                                if (i2 < 26) {
                                    s_optimize_userwaynum = "lowos7ram2_3";
                                } else if (i2 < 28) {
                                    s_optimize_userwaynum = "lowos8ram2_3";
                                } else if (i2 < 29) {
                                    s_optimize_userwaynum = "lowos9ram2_3";
                                } else {
                                    s_optimize_userwaynum = "lowram2_3";
                                }
                            } else if (d5 < 4.0d || d5 == 4.0d) {
                                if (oppoLowDevice.contains(str) && !isPreLoad()) {
                                    s_optimize_userwaynum = "lowosoppo3_4";
                                } else if (i2 < 26) {
                                    s_optimize_userwaynum = "lowos7ram3_4";
                                } else if (i2 < 28) {
                                    s_optimize_userwaynum = "lowos8ram3_4";
                                } else {
                                    s_optimize_userwaynum = "normal";
                                }
                            } else {
                                s_optimize_userwaynum = "normal";
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                s_optimize_userwaynum = "normal";
            }
        }
        if (highCrashDevice.contains(deviceModel)) {
            s_optimize_userwaynum = "lowDeviceHighCrash";
        }
        return s_optimize_userwaynum;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 2 */
    public static double formatNumber(double d2) {
        return Math.round(d2 * 10.0d) / 10.0d;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 2 */
    public static long getRAMAvailValue(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 2 */
    public static long getRAMTotalValue(Context context) {
        if (totalMem == 0) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            totalMem = memoryInfo.totalMem;
        }
        return totalMem;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 2 */
    public static boolean isPreLoad() {
        if (isPreload) {
            return true;
        }
        return false;
    }

    static double total_mem_G = 0;

    public static double getTotalMemGCache(Context context) {
        if (total_mem_G == 0) {
            total_mem_G = formatNumber(((getRAMTotalValue(context) / 1024.0d) / 1024.0d) / 1024.0d);
        }
        return total_mem_G;
    }

    private static boolean checkHasPermission(Context context, String permission) {
        try {
            Class<?> contextCompat = null;
            try {
                contextCompat = Class.forName("android.support.v4.content.ContextCompat");
            } catch (Exception e) {
                //ignored
            }
            if (contextCompat == null) {
                try {
                    contextCompat = Class.forName("androidx.core.content.ContextCompat");
                } catch (Exception e) {
                    //ignored
                }
            }

            if (contextCompat == null) {
                return true;
            }

            Method checkSelfPermissionMethod = contextCompat.getMethod("checkSelfPermission",
                    new Class[]{Context.class, String.class});
            int result = (int) checkSelfPermissionMethod.invoke(null, new Object[]{context, permission});
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return true;
        }
    }

   public static String getNetworkType(Context context) {
        try {
            boolean mHasPermission = checkHasPermission(context, "android.permission.ACCESS_NETWORK_STATE");
            if (!mHasPermission) {
                return "NULL";
            }
            // Wifi
            ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = null;
            if (manager != null) {
                networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                    return "WIFI";
                }
            }
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return mobileNetworkType(context, telephonyManager, manager);
        } catch (Exception e) {
            return "NULL";
        }
    }

    @SuppressLint("MissingPermission")
    static private String mobileNetworkType(Context context, TelephonyManager telephonyManager,
                                            ConnectivityManager connectivityManager) {
        // Mobile network
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (telephonyManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        && checkHasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                    networkType = telephonyManager.getDataNetworkType();
                } else {
                    networkType = telephonyManager.getNetworkType();
                }
            } catch (Exception ignored) {
                //ignored
            }
        }

        if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    networkType = networkInfo.getSubtype();
                }
            }
        }

        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
            case 19:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            default:
                return "NULL";
        }
    }

    public static String getRemoteConfigParams(Context context) {
        String opt = checkConfigB(context);

        String campaign = LocalStorage.Instance.decodeString(IKeys.KEY_AF_CAMPAIGN_ID, "");
        String dname = Build.DEVICE;
        String modelname = Build.MODEL;
        String network = getNetworkType(context);
        String isNewUser = "0";
        if (LocalStorage.Instance.decodeBoolean(IKeys.KEY_APP_NEW_USER, false)) {
            isNewUser = "1";
        }
        String bundleId = context.getPackageName();
        String lang = Locale.getDefault().getLanguage();
        String timezone;
        try {
            timezone = TimeZone.getDefault().getDisplayName(false, TimeZone.LONG);
        } catch (Exception e) {
            timezone = TimeZone.getDefault().getID();
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String afid = LocalStorage.Instance.decodeString(IKeys.KEY_AF_ID, "");
        return "&opt=" + opt + "&campaign=" + campaign + "&dname=" + dname + "&modelname=" + modelname + "&network=" + network + "&is_new_user=" + isNewUser + "&bundleId=" + bundleId
                + "&language=" + lang + "&timezone=" + timezone + "&timestamp=" + timestamp + "&afid=" + afid;
    }
}