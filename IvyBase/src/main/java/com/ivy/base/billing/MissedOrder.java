package com.ivy.base.billing;

public class MissedOrder {
    public String appId;
    public String uuid;
    public String rpDate;
    public String country;
    public String merchantTransactionId;
    public String orderId;
    public String environment;
    public int orderStatus;
    public String productId;
    public String name;
    public double usd;
    public String payload;

    public MissedOrder(String appId, String uuid, String rpDate, String country, String merchantTransactionId, String orderId, String environment, int orderStatus, String productId, String name, double usd, String payload) {
        this.appId = appId;
        this.uuid = uuid;
        this.rpDate = rpDate;
        this.country = country;
        this.merchantTransactionId = merchantTransactionId;
        this.orderId = orderId;
        this.environment = environment;
        this.orderStatus = orderStatus;
        this.productId = productId;
        this.name = name;
        this.usd = usd;
        this.payload = payload;
    }
}
