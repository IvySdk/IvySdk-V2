package com.ivy.base.billing;

import android.content.Intent;

public interface IPurchase {

    boolean isPaymentInitialized();

    void pay(int id, String payload, String clientInfo);

    void shippingGoods(String merchantTransactionId);

    String getGoodsInfo(int id);

    void queryPurchase(int id);

    void onActivityResult(int requestCode, int resultCode, Intent data);

    //enjoypay 接口
    boolean isEnjoyPayInitialized();

    void enjoyPay(int id, String payload, String clientInfo);

    String getEnjoyPayGoodsInfo(int id);

    void queryEnjoyPurchase(int id);

}
