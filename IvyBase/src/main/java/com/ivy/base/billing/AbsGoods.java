package com.ivy.base.billing;

import com.ivy.base.track.IEvent;

import org.json.JSONObject;


/**
 * 商品抽象类
 */
public abstract class AbsGoods<T> {

    protected int id = -1; // pay id
    protected String title = ""; // 标题
    protected String desc = ""; // 商品描述
    protected String sku = ""; // 商品在计费平台唯一id
    protected String type = GoodsType.INAPP; // 商品计费类型
    protected double usd = 0.0; // 商品价格美元
    protected String currency = "USD"; // 默认货币单位
    protected String price = ""; // 商品价格，包含货币单位
    protected long priceAmountMicros = 0L; // 商品价格，无货币单位，单位：微米

    // 二次事件
    protected int v25Count = 0;
    protected String v25Event = null;

    // 商品原价
    protected String originalPrice = "";
    protected long originalPriceAmountMicros = 0L;

    // 是否已同步线上计费点信息
    protected boolean hasSync = false;

    /**
     * 解析配置
     */
    public AbsGoods<T> decodeConfig(int id, JSONObject data) {
        if (data == null) return null;
        this.id = id;
        this.sku = data.optString("feename");
        if (this.sku.isEmpty()) return null;
        this.title = data.optString("title");
        this.desc = data.optString("desc");
        this.type = data.optInt("repeat", 1) == 1 ? GoodsType.INAPP : GoodsType.SUBS;
        this.usd = data.optDouble("usd", 0.0);
        this.price = "US$" + usd;
        this.v25Count = data.optInt("v25_count", 0);
        this.v25Event = data.optString("v25_event", "in_ivyapp_purchase");
        return this;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getUsd() {
        return usd;
    }

    public void setUsd(double usd) {
        this.usd = usd;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public long getPriceAmountMicros() {
        return priceAmountMicros;
    }

    public void setPriceAmountMicros(long priceAmountMicros) {
        this.priceAmountMicros = priceAmountMicros;
    }

    public int getV25Count() {
        return v25Count;
    }

    public void setV25Count(int v25Count) {
        this.v25Count = v25Count;
    }

    public String getV25Event() {
        return v25Event;
    }

    public void setV25Event(String v25Event) {
        this.v25Event = v25Event;
    }

    public String getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice;
    }

    public long getOriginalPriceAmountMicros() {
        return originalPriceAmountMicros;
    }

    public void setOriginalPriceAmountMicros(long originalPriceAmountMicros) {
        this.originalPriceAmountMicros = originalPriceAmountMicros;
    }

    public boolean isHasSync() {
        return hasSync;
    }

    public void setHasSync(boolean hasSync) {
        this.hasSync = hasSync;
    }

    /**
     * 更新商品信息
     */
    public void update(T t, UpdateCallback<T> callback) {
        callback.invoke(t);
        this.hasSync = true;
    }

    public interface UpdateCallback<T> {
        void invoke(T t);
    }

    public abstract JSONObject toJson();

    @Override
    public String toString() {
        return toJson().toString();
    }

    public abstract double getPriceAmount();

    public abstract double getOriginalPriceAmount();
}

