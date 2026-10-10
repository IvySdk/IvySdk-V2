package com.ivy.base.billing;

/**
 * 支付结果接口
 */
public interface IPurchaseResult {
    int UNSUPPORTED = 0;
    int PAY_SUCCEED = 1;
    int PAY_FAILED = 2;

    void payResult(int payId, int status, String payload, String merchantTransactionId, String purchase);

    void onShippingResult(String merchantTransactionId, boolean status);

    void onStoreInitialized(boolean initState);
}
