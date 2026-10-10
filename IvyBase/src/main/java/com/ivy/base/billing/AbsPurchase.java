package com.ivy.base.billing;

import android.content.Intent;

import java.util.HashMap;
import java.util.Map;

// 抽象类
public abstract class AbsPurchase<T> implements IIPurchase {

    protected String TAG;

    protected Map<String, T> goodsData = new HashMap<>(); // <sku, AbsGood>
    protected Map<Integer, String> ids = new HashMap<>(); // <payId, sku>
    protected IIPurchaseResult purchaseResultCallback;
    protected boolean initializeStatus = false;
    protected String appId = "";
    protected boolean debug = false;

    // 支付结果校验
    protected String verifyKey;
    protected String verifyServerUrl;

    @Override
    public void setup(String appId, String data, String enjoy_pay_dta, boolean debug, IIPurchaseResult callback) {
        this.appId = appId;
        this.debug = debug;
        this.purchaseResultCallback = callback;
    }

    @Override
    public boolean isPaymentInitialized() {
        return initializeStatus;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public boolean isEnjoyPayInitialized() {
        return isPaymentInitialized();
    }

    @Override
    public void enjoyPay(int id, String payload, String clientInfo) {

    }

    @Override
    public String getEnjoyPayGoodsInfo(int id) {
        return "";
    }

    @Override
    public void queryEnjoyPurchase(int id) {

    }

}
