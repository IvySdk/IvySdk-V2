package com.ivy.ads;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import com.ivy.base.ad.AdType;

import org.json.JSONObject;

/**
 * 广告基类，包含广告的基本属性和方法
 */
public abstract class BaseAd implements Runnable {
    // 广告单元ID
    protected String adUnit;
    // 广告类型
    protected AdType adType;
    // 广告平台
    protected String platform;
    // 是否禁用自动重试
    protected boolean disableAutoRetry;
    // 最大无填充次数
    protected int maxNofillCount;
    // 当前无填充次数
    protected int nofillCount;
    // 阻塞开始时间
    protected long blockStartTime;
    // 阻塞持续时间（分钟）
    protected int blockDuration;
    // 延迟秒数
    protected int delaySeconds;
    // 加载失败次数
    protected int loadFailedCount;
    // 价格
    protected double price;
    private int refreshSeconds;
    private long loadStartTime;
    // 货币代码
    protected String currencyCode;
    // 网络名称
    protected String networkName;
    // 分段信息
    protected String segments;
    protected String amazonSlotId;
    protected AdRevenueCallback revenueCallback;
    protected BaseAd.AdLoadCallback loadCallback;
    protected BaseAd.AdShowCallback showCallback;
    protected Activity context;
    // 软超时时间（秒），默认10秒（适用于串行队列等，超时后放行下一层但本层回调仍有效）
    protected int timeoutSeconds = 10;
    protected boolean isLoading = false;
    protected boolean isShowing = false;
    protected boolean isSecondary = false;
    SerialAdLoader serialLoader;
    AdLoadController loadController;
    boolean platformInitialized;
    long serialRetryAtMs;
    boolean loadPending = false;
    boolean destroyed = false;
    
    /**
     * 构造函数
     */
    public BaseAd() {
        this.price = 0;
        this.currencyCode = "USD";
        this.networkName = "max";
        this.nofillCount = 0;
        this.blockStartTime = 0;
        this.delaySeconds = 0;
        this.loadFailedCount = 0;
        this.segments = null;
        this.timeoutSeconds = 10;
    }

    public boolean isSecondary(){
        return isSecondary;
    }

    public void init(AdType adType, JSONObject data, AdRevenueCallback revenueCallback, Activity context){
        try{
            this.context = context;
            this.revenueCallback = revenueCallback;
            this.adType = adType;
            this.adUnit = data.optString("adUnit");
            this.platform = data.getString("platform");
            this.disableAutoRetry = data.optBoolean("disableAutoRetry", false)
                    || (serialLoader != null && "max".equals(platform));
            this.maxNofillCount = data.optInt("maxNofillCount", 5);
            this.blockDuration = data.optInt("blockDuration", 3);
            this.amazonSlotId = data.optString("amazonSlotId", null);
            refreshSeconds = data.optInt("refreshSeconds", 0);
            this.isSecondary = data.optBoolean("fallback", false);
            this.timeoutSeconds = data.optInt("timeoutSeconds", 10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 检查广告是否准备就绪
     */
    public boolean isReady() {
        return true; // 默认实现，子类可以重写
    }

    public boolean delayed = false;
    @Override
    public void run() {
        delayed = false;
        if (loadController != null) {
            loadController.startLoad(this);
        } else {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    load(loadCallback);
                }
            });
        }
    }

    /**
     * 尝试安全加载广告
     */
    public boolean tryLoad(AdLoadCallback callback) {
        if (destroyed || isLoading || isShowing || isBlocked()) {
            return false;
        }
        if (refreshSeconds == 0 && isReady()) {
            return false;
        }
        loadCallback = callback;
        isLoading = true;
        loadStartTime = SystemClock.elapsedRealtime();
        if (callback != null) {
            callback.onLoadStart(this);
        }
        loadAd();
        return true;
    }

    /**
     * 加载广告
     */
    public void load(AdLoadCallback callback) {
        if (loadController != null) {
            loadController.request(this);
            return;
        }
        if (serialLoader != null && !serialLoader.ownsLoad(this)) {
            serialLoader.request();
            return;
        }
        loadCallback = callback;
        if (isLoading || isShowing || ((serialLoader != null || refreshSeconds == 0) && isReady())) {
            if (serialLoader != null && !isLoading) {
                serialLoader.onLoadSkipped(this);
            }
            return;
        }
        if (isBlocked()) {
            if (callback != null) {
                callback.onLoadFailed(this, -1, "blocked by nofill limit");
            }
            return;
        }
        isLoading = true;
        loadStartTime = SystemClock.elapsedRealtime();
        if (callback != null) {
            callback.onLoadStart(this);
        }
        loadAd();
    }

    protected abstract void loadAd();

    protected void close(){}

    public void destroy() {
        close();
    }

    final void destroyForShutdown() {
        destroyed = true;
        try {
            destroy();
        } finally {
            loadCallback = null;
            showCallback = null;
            revenueCallback = null;
            isLoading = false;
            isShowing = false;
            delayed = false;
            loadPending = false;
        }
    }
    
    /**
     * 加载失败处理
     */
    public void onLoadFailed(int errorCode, String errorMsg) {
        delaySeconds = (int) Math.pow(2, Math.min(loadFailedCount, 5));
        loadFailedCount++;
        
        if (isNofillError(errorCode, errorMsg)) { // 无填充错误码
            nofillCount++;
            if (nofillCount >= maxNofillCount) {
                blockStartTime = System.currentTimeMillis();
            }
        }
    }

    protected abstract boolean isNofillError(int errorCode, String errorMsg);

    public void onLoss(String bidder, float price){}
    
    /**
     * 加载成功处理
     */
    public void onLoadSuccess() {
        nofillCount = 0;
        blockStartTime = 0;
        loadFailedCount = 0;
        delaySeconds = 1;
    }
    
    /**
     * 检查是否被阻塞
     */
    public boolean isBlocked() {
        if (nofillCount >= maxNofillCount) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - blockStartTime <= (long) blockDuration * 60 * 1000) {
                return true;
            } else {
                nofillCount = 0;
                blockStartTime = 0;
                loadFailedCount = 0;
            }
        }
        return false;
    }
    
    /**
     * 显示广告
     */
    public abstract void show(AdShowCallback callback);
    
    // Getter 方法
    public String getAdUnit() { return adUnit; }
    public AdType getAdType() { return adType; }
    public String getPlatform() { return platform; }
    public long getLoadStartTime() { return loadStartTime; }
    public boolean isDisableAutoRetry() { return disableAutoRetry; }
    public int getMaxNofillCount() { return maxNofillCount; }
    public int getNofillCount() { return nofillCount; }
    public long getBlockStartTime() { return blockStartTime; }
    public int getBlockDuration() { return blockDuration; }
    public int getDelaySeconds() { return delaySeconds; }
    public int getLoadFailedCount() { return loadFailedCount; }
    public double getPrice() { return price; }
    public String getCurrencyCode() { return currencyCode; }
    public String getNetworkName() { return networkName; }
    public String getSegments() { return segments; }
    public String getAmazonSlotId() { return amazonSlotId; }
    public int getRefreshSeconds() { return refreshSeconds; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public View getView() { return null; }
    
    /**
     * 广告加载回调接口
     */
    public interface AdLoadCallback {
        void onLoadStart(BaseAd ad);
        void onLoadFailed(BaseAd ad, int errorCode, String message);
        void onLoadSuccess(BaseAd ad);
    }
    
    /**
     * 广告显示回调接口
     */
    public interface AdShowCallback {
        void onAdClosed(BaseAd ad, boolean rewarded);
        void onAdDisplayed(BaseAd ad);
        void onAdShowFailed(BaseAd ad, int errorCode, String message);
        void onAdClicked(BaseAd ad);
    }

    public interface AdRevenueCallback {
        /**
         * Ad revenue callback
         * @param adConfig Ad configuration
         */
        void onAdRevenuePaid(BaseAd adConfig);
    }
}
