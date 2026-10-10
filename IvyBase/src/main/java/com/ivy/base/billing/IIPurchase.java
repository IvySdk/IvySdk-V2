package com.ivy.base.billing;

public interface IIPurchase extends IPurchase {

    void setup(String appId, String data, String enjoy_pay_data, boolean debug, IIPurchaseResult callback);

    int randomBillId();

}
