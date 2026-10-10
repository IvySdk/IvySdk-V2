package com.ivy.google.billing;

import static com.ivy.base.billing.GoodsType.INAPP;

import android.app.Activity;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsParams.Product;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.UnfetchedProduct;
import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.billing.AbsGoods;
import com.ivy.base.billing.AbsPurchase;
import com.ivy.base.billing.BillingUtils;
import com.ivy.base.billing.GoodsType;
import com.ivy.base.billing.IIPurchaseResult;
import com.ivy.base.billing.IPurchaseResult;
import com.ivy.base.billing.MissedOrder;
import com.ivy.base.billing.PurchaseError;
import com.ivy.base.billing.PurchaseState;
import com.ivy.base.grid.GridManager;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.track.EventIDs;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.base.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class GoogleBillingPurchase extends AbsPurchase<Goods> implements PurchasesUpdatedListener, IUnProguard {


    private final String TAG = "GABilling";
    private BillingUtils billingUtils;
    private final Map<String, Object> clientInfos = new HashMap<>();
    private final BillingClient billingClient = BillingClient.newBuilder(App.getInstance()).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build()).setListener(this).build();

    private int processing_bill_id = 0;


    @Override
    public void setup(final String appId, final String data, final String enjoy_pay_data, final boolean debug, final IIPurchaseResult callback) {
        super.setup(appId, data, enjoy_pay_data, debug, callback);

        try {
            JSONObject json = new JSONObject(data);
            // GP 后台提供的校验 key
            verifyKey = json.optString("key");

            // 校验地址
            verifyServerUrl = json.optString("verify-url");

            if (!TextUtils.isEmpty(verifyServerUrl)) {
                billingUtils = new BillingUtils(appId, verifyServerUrl, debug);
            }

            JSONObject checkout = json.optJSONObject("checkout");
            if (checkout != null) {
                Iterator<String> keys = checkout.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        int payId = Integer.parseInt(key);
                        Goods item = new Goods().decode(payId, checkout.optJSONObject(key));
                        if (item != null) {
                            ids.put(payId, item.getSku());
                            goodsData.put(item.getSku(), item);
                        } else {
                            ILog.e(TAG, "invalid sku for " + key);
                        }
                    } catch (NumberFormatException e) {
                        ILog.e(TAG, "invalid pay id:" + key);
                    }
                }

                connectGoogleBilling();

            } else {
                ILog.e(TAG, "no goods config!!!");
            }

        } catch (Exception e) {
            ILog.e(TAG, "invalid payment config");
        }
    }

    private void connectGoogleBilling() {
        if (billingClient.getConnectionState() == BillingClient.ConnectionState.CONNECTING) {
            return;
        }
        if (billingClient.getConnectionState() == BillingClient.ConnectionState.CONNECTED) {
            return;
        }
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                ILog.i(TAG, "billing store disconnected");
                if (purchaseResultCallback != null) {
                    purchaseResultCallback.onStoreInitialized(false);
                }
            }

            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                ILog.i(TAG, "billing store connect result: " + billingResult.getResponseCode());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryGoods(null, () -> {
                        if (purchaseResultCallback != null) {
                            purchaseResultCallback.onStoreInitialized(true);
                        }
                        // 可在此处查询掉单商品，客户端自行调用 queryPurchase(-1)
                    }, () -> {
                        if (purchaseResultCallback != null) {
                            purchaseResultCallback.onStoreInitialized(false);
                        }
                    });
                }
            }
        });
    }

    /**
     * 查询商品信息
     *
     * @param sku     商品 sku，如为 null，则查询所有商品信息
     * @param success 成功回调
     * @param failure 失败回调
     */
    private void queryGoods(String sku, final Runnable success, final Runnable failure) {
        switch (billingClient.getConnectionState()) {
            case BillingClient.ConnectionState.CONNECTED:
                if (sku != null) {
                    ILog.i(TAG, "billing client connected; start query goods:" + sku);
                    Goods good = goodsData.get(sku);
                    if (good != null) {
                        Product product = Product.newBuilder().setProductId(good.getSku()).setProductType(good.getType() == INAPP ? BillingClient.ProductType.INAPP : BillingClient.ProductType.SUBS).build();
                        queryGoodsDetails(List.of(product), success, failure);
                    } else {
                        ILog.w(TAG, "unable to load goods info for " + sku + "; not configured!!");
                    }
                } else {
                    ILog.i(TAG, "billing client connected; start query all goods");
                    List<Product> inapps = new ArrayList<>();
                    List<Product> subs = new ArrayList<>();
                    for (Goods g : goodsData.values()) {
                        if (Objects.equals(g.getType(), INAPP)) {
                            Product product = Product.newBuilder().setProductId(g.getSku()).setProductType(BillingClient.ProductType.INAPP).build();
                            inapps.add(product);
                        } else {
                            Product product = Product.newBuilder().setProductId(g.getSku()).setProductType(BillingClient.ProductType.SUBS).build();
                            subs.add(product);
                        }
                    }
                    if (!inapps.isEmpty()) {
                        queryGoodsDetails(inapps, () -> {
                            if (!subs.isEmpty()) {
                                queryGoodsDetails(subs, success, failure);
                            } else {
                                if (success != null) {
                                    success.run();
                                }
                            }
                        }, () -> {
                            if (!subs.isEmpty()) {
                                queryGoodsDetails(subs, success, failure);
                            } else {
                                if (failure != null) {
                                    failure.run();
                                }
                            }
                        });
                    } else {
                        if (!subs.isEmpty()) {
                            queryGoodsDetails(subs, success, failure);
                        } else {
                            if (failure != null) {
                                failure.run();
                            }
                        }
                    }
                }
                break;

            case BillingClient.ConnectionState.CLOSED:
            case BillingClient.ConnectionState.DISCONNECTED:
                ILog.w(TAG, "billing client closed or disconnected! start re-connect");
                connectGoogleBilling();
                if (failure != null) {
                    failure.run();
                }
                break;

            case BillingClient.ConnectionState.CONNECTING:
                ILog.i(TAG, "billing client connecting; waiting...");
                if (failure != null) {
                    failure.run();
                }
                break;
        }
    }

    private void queryGoodsDetails(List<Product> list, final Runnable success, final Runnable failure) {
        QueryProductDetailsParams queryParams = QueryProductDetailsParams.newBuilder().setProductList(list).build();

        billingClient.queryProductDetailsAsync(queryParams, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                int responseCode = billingResult.getResponseCode();
                if (responseCode == BillingClient.BillingResponseCode.OK && queryProductDetailsResult != null) {
                    List<ProductDetails> productDetailsList = queryProductDetailsResult.getProductDetailsList();

                    if (debug) {
                        try {
                            List<UnfetchedProduct> unfetched = queryProductDetailsResult.getUnfetchedProductList();
                            for (UnfetchedProduct product : unfetched) {
                                ILog.w(TAG, "未拉取商品:" + product.getProductId() + "; 类型:" + product.getProductType());
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    if (productDetailsList == null || productDetailsList.isEmpty()) {
                        ILog.i(TAG, "empty product details response");
                        if (failure != null) failure.run();
                        return;
                    }

                    for (ProductDetails details : productDetailsList) {
                        Goods good = goodsData.get(details.getProductId());
                        if (good != null) {
                            good.update(details, t -> {
                                good.setSku(t.getProductId());
                                good.setTitle(t.getTitle());
                                good.setDesc(t.getDescription());

                                if (t.getProductType().equals(BillingClient.ProductType.INAPP)) {
                                    good.setType(INAPP);
                                    ProductDetails.OneTimePurchaseOfferDetails oneTime = t.getOneTimePurchaseOfferDetails();
                                    if (oneTime != null) {
                                        good.setPrice(oneTime.getFormattedPrice());
                                        good.setPriceAmountMicros(oneTime.getPriceAmountMicros());
                                        good.setCurrency(oneTime.getPriceCurrencyCode());
                                    }
                                } else if (t.getProductType().equals(BillingClient.ProductType.SUBS)) {
                                    good.setType(GoodsType.SUBS);
                                    if (t.getSubscriptionOfferDetails() != null && !t.getSubscriptionOfferDetails().isEmpty()) {
                                        ProductDetails.SubscriptionOfferDetails offer = t.getSubscriptionOfferDetails().get(0);
                                        good.setOfferId(offer.getOfferId());
                                        good.setOfferToken(offer.getOfferToken());
                                        if (offer.getPricingPhases() != null && !offer.getPricingPhases().getPricingPhaseList().isEmpty()) {
                                            ProductDetails.PricingPhase pricingPhase = offer.getPricingPhases().getPricingPhaseList().get(0);
                                            good.setPrice(pricingPhase.getFormattedPrice());
                                            good.setCurrency(pricingPhase.getPriceCurrencyCode());
                                            good.setPriceAmountMicros(pricingPhase.getPriceAmountMicros());

                                            if (offer.getPricingPhases().getPricingPhaseList().size() > 1) {
                                                ProductDetails.PricingPhase nextPricing = offer.getPricingPhases().getPricingPhaseList().get(1);
                                                good.setOriginalPrice(nextPricing.getFormattedPrice());
                                                good.setOriginalPriceAmountMicros(nextPricing.getPriceAmountMicros());
                                                good.setCurrency(nextPricing.getPriceCurrencyCode());
                                            }
                                        }
                                    }
                                }
                            });
                        } else {
                            ILog.e(TAG, "no good find for " + details.getProductId());
                        }
                    }
                    ILog.i(TAG, "goods info loaded");
                    if (success != null) success.run();
                } else {
                    ILog.w(TAG, "query product details err:" + responseCode);
                    if (failure != null) failure.run();
                }
            }

        });
    }

    @Override
    public boolean isPaymentInitialized() {
        return billingClient.isReady();
    }

    @Override
    public void shippingGoods(String merchantTransactionId) {
        if (billingUtils != null) {
            billingUtils.receiptGoods(merchantTransactionId, new BillingUtils.Callback() {
                @Override
                public void onResult(boolean state) {
                    ILog.i(TAG, "shipping goods result:" + state);
                    if (purchaseResultCallback != null) {
                        purchaseResultCallback.onShippingResult(merchantTransactionId, state);
                    }
                    deletePayload(merchantTransactionId);
                }
            });
        }
    }

    @Override
    public int randomBillId() {
        if (ids.isEmpty()) {
            return -1;
        }
        Set<Integer> keys = ids.keySet();
        int index = new Random().nextInt(keys.size());
        return (Integer) keys.toArray()[index];
    }

    @Override
    public void pay(final int id, final String payload, final String clientInfo) {
        if (billingClient.getConnectionState() != BillingClient.ConnectionState.CONNECTED) {
            TestManager.getInstance().addEvent("Payment", "付费失败，平台连接失败", EventState.ERROR, "billId=" + id);
            connectGoogleBilling();
            ILog.w(TAG, "billing client is not ready");
            if (purchaseResultCallback != null) {
                purchaseResultCallback.payResult(id, IPurchaseResult.PAY_FAILED, payload, null, null);
            }
            return;
        }

        final Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity == null) {
            TestManager.getInstance().addEvent("Payment", "付费失败，Activity=null，检查初始化", EventState.ERROR, "billId=" + id);
            ILog.w(TAG, "invalid activity to pay");
            if (purchaseResultCallback != null) {
                purchaseResultCallback.payResult(id, IPurchaseResult.PAY_FAILED, payload, null, null);
            }
            return;
        }

        final String sku = ids.get(id);
        if (sku == null) {
            TestManager.getInstance().addEvent("Payment", "付费失败，未找到计费点sku", EventState.ERROR, "billId=" + id);
            ILog.w(TAG, "no pay id:" + id + " setup");
            if (purchaseResultCallback != null) {
                purchaseResultCallback.payResult(id, IPurchaseResult.PAY_FAILED, payload, null, null);
            }
            return;
        }

        final Goods goods = goodsData.get(sku);
        if (goods == null) {
            TestManager.getInstance().addEvent("Payment", "付费失败，未找到计费点信息", EventState.ERROR, "billId=" + id);
            ILog.w(TAG, "no goods for pay id:" + id + " & sku:" + sku + " setup! check config file");
            if (purchaseResultCallback != null) {
                purchaseResultCallback.payResult(id, IPurchaseResult.PAY_FAILED, payload, null, null);
            }
            return;
        }

        if (!goods.isHasSync()) {
            TestManager.getInstance().addEvent("Payment", "付费失败，计费点信息未同步", EventState.ERROR, "billId=" + id);
            queryGoods(goods.getSku(), null, null);
            ILog.w(TAG, "has not sync good message:" + id + ";" + sku);
            if (purchaseResultCallback != null) {
                purchaseResultCallback.payResult(id, IPurchaseResult.PAY_FAILED, payload, null, null);
            }
            return;
        }

        setupClientInfo(clientInfo);

        if (billingUtils != null) {
            TestManager.getInstance().addEvent("Payment", "开始预下单", EventState.NORMAL, "billId=" + id);
            billingUtils.preOrder(goods, payload, new BillingUtils.PreOrderCallback<Goods>() {

                @Override
                public void onResult(boolean status, AbsGoods<Goods> _goods, String merchantTransactionId) {
                    if (status) {
                        if (merchantTransactionId == null || merchantTransactionId.isEmpty()) {
                            TestManager.getInstance().addEvent("Payment", "预下单返回成功，但预订单id无效", EventState.ERROR, "billId=" + id);
                            setupPurchaseResult(PurchaseState.PRE_ORDER, false, goods, payload, null, PurchaseError.ERROR_PRE_ORDER_ID, null);
                        } else {
                            TestManager.getInstance().addEvent("Payment", "预下单成功", EventState.NORMAL, "billId=" + id + ";预下单id=" + merchantTransactionId);
                            encodePayload(merchantTransactionId, payload);
                            java.util.Map<String, Object> extra = new java.util.HashMap<>();
                            extra.put("merchantTransactionId", merchantTransactionId);
                            setupPurchaseResult(PurchaseState.PRE_ORDER, true, goods, payload, null, null, extra);
                            doPay(activity, id, goods, payload, merchantTransactionId, merchantTransactionId);
                        }
                    } else {
                        TestManager.getInstance().addEvent("Payment", "预下单失败", EventState.ERROR, "billId=" + id);
                        setupPurchaseResult(PurchaseState.PRE_ORDER, false, goods, payload, null, PurchaseError.ERROR_PRE_ORDER_FAILED, null);
                    }
                }
            });
        } else {
            TestManager.getInstance().addEvent("Payment", "开始付费", EventState.NORMAL, "billId=" + id);
            String localMerchantTransactionId = localOrderId();
            encodePayload(localMerchantTransactionId, payload);
            TestManager.getInstance().addEvent("Payment", "本地订单id:" + localMerchantTransactionId, EventState.NORMAL, "billId=" + id);
            doPay(activity, id, goods, payload, localMerchantTransactionId, null);
        }
    }

    /**
     * 调用支付
     */
    private void doPay(final Activity activity, final int id, final Goods goods, final String payload, final String obfuscatedAccountId, final String merchantTransactionId) {
        if (billingUtils != null) {
            billingUtils.purchasing();
        }

        BillingFlowParams.ProductDetailsParams.Builder productDetailsParamBuilder = BillingFlowParams.ProductDetailsParams.newBuilder();
        if (goods.getOfferToken() != null) {
            productDetailsParamBuilder.setOfferToken(goods.getOfferToken());
        }
        if (goods.getProductDetails() != null) {
            ProductDetails productDetails = goods.getProductDetails();
            productDetailsParamBuilder.setProductDetails(productDetails);
        }

        final String orderProfileId = (merchantTransactionId == null || merchantTransactionId.isEmpty()) ? obfuscatedAccountId : merchantTransactionId;
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(java.util.Collections.singletonList(productDetailsParamBuilder.build()))
                .setIsOfferPersonalized(true)
                .setObfuscatedAccountId(orderProfileId != null ? orderProfileId : "")
                .build();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                processing_bill_id = goods.getId();
                BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
                int responseCode = billingResult.getResponseCode();

                switch (responseCode) {
                    case BillingClient.BillingResponseCode.OK:
                        ILog.i(TAG, "start pay succeed! waiting for check");
                        TestManager.getInstance().addEvent("Payment", "拉起付费平台成功", EventState.NORMAL, "billId=" + id + ";预下单id=" + merchantTransactionId);
                        break;
                    case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                        ILog.i(TAG, "already owned the good" + id + "--" + goods.getSku() + "! waiting for consume");
                        TestManager.getInstance().addEvent("Payment", "拉起付费平台失败，已拥有该商品", EventState.ERROR, "billId=" + id + ";预下单id=" + merchantTransactionId);
                        break;
                    default:
                        ILog.i(TAG, "start pay failed:" + responseCode + " for " + id + "--" + goods.getSku());
                        TestManager.getInstance().addEvent("Payment", "拉起付费平台失败,失败码:" + responseCode + ";" + billingResult.getDebugMessage(), EventState.ERROR, "billId=" + id + ";预下单id=" + merchantTransactionId);
                        if (merchantTransactionId == null || merchantTransactionId.isEmpty()) {
                            if (obfuscatedAccountId != null) deletePayload(obfuscatedAccountId);
                            setupPurchaseResult(PurchaseState.PAY_RESULT, false, goods, payload, null, PurchaseError.ERROR_PULL_UP_SHOP, null);
                        } else {
                            deletePayload(merchantTransactionId);
                            java.util.Map<String, Object> extra = new java.util.HashMap<>();
                            extra.put("merchantTransactionId", merchantTransactionId);
                            setupPurchaseResult(PurchaseState.PAY_RESULT, false, goods, payload, null, PurchaseError.ERROR_PULL_UP_SHOP, extra);
                        }
                        break;
                }
            }
        });
    }

    @Override
    public String getGoodsInfo(int id) {
        try {
            if (id == -1) {
                try {
                    JSONObject data = new JSONObject();
                    for (Map.Entry<String, Goods> item : goodsData.entrySet()) {
                        data.put(String.valueOf(item.getValue().getId()), item.getValue().toJson());
                    }
                    return data.toString();
                } catch (Exception e) {
                    ILog.i(TAG, "error when get good info:" + e.getMessage());
                    return "{}";
                }
            }
            String sku = ids.get(id);
            if (sku != null && goodsData.get(sku) != null) {
                return goodsData.get(sku).toString();
            } else {
                return "{}";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }

    @Override
    public void queryPurchase(int id) {
        if (id == -1) {
            queryUnShippedGoods();
            queryUnConsumedPurchases(false);
            queryUnConsumedPurchases(true);
        } else {
            String sku = ids.get(id);
            if (sku != null) {
                final Goods good = goodsData.get(sku);
                if (good != null) {
                    if (good.isHasSync()) {
                        queryUnConsumedPurchases(!Objects.equals(good.getType(), INAPP));
                    } else {
                        queryGoods(sku, new Runnable() {
                            @Override
                            public void run() {
                                queryUnConsumedPurchases(!Objects.equals(good.getType(), INAPP));
                            }
                        }, null);
                    }
                } else {
                    ILog.i(TAG, "unable to query id! no good found! checkout had it configured");
                }
            } else {
                ILog.i(TAG, "unable to query id! no sku found! checkout had it configured");
            }
        }
    }

    /**
     * 查询已购买，但是后台未记录发货的商品
     */
    private void queryUnShippedGoods() {
        if (billingUtils != null) {
            billingUtils.unShippedGoodsOrders(new BillingUtils.MissedOrderCallback() {
                @Override
                public void onResult(List<MissedOrder> data) {
                    if (data != null) {
                        for (MissedOrder item : data) {
                            String sku = item.productId;
                            int id = 0;
                            if (goodsData.get(sku) != null) {
                                id = goodsData.get(sku).getId();
                            }
                            if (id == 0) continue;
                            String payload = (item.payload == null || item.payload.isEmpty()) ? decodePayload(item.merchantTransactionId) : item.payload;
                            if (purchaseResultCallback != null) {
                                purchaseResultCallback.payResult(id, IPurchaseResult.PAY_SUCCEED, payload, item.merchantTransactionId, null);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * 记录payload
     */
    private void encodePayload(String sku, String payload) {
        if (payload != null) {
            LocalStorage.Instance.encodeString("payload_" + sku, payload);
        } else {
            LocalStorage.Instance.removeValueForKey("payload_" + sku);
        }
    }

    /**
     * 删除payload
     */
    private String decodePayload(String sku) {
        return LocalStorage.Instance.decodeString("payload_" + sku, null);
    }

    private void deletePayload(String sku) {
        String key = "payload_" + sku;
        LocalStorage.Instance.removeValueForKey(key);
    }

    /**
     * 支付结果回调
     */
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        ILog.i(TAG, "received purchase result code:" + billingResult.getResponseCode() + "; " + billingResult);
        if (purchases != null && !purchases.isEmpty()) {
            TestManager.getInstance().addEvent("Payment", "收到付费结果,开始处理", EventState.NORMAL, "");
            for (Purchase purchase : purchases) {
                handlePurchase(billingResult.getResponseCode(), purchase);
            }
        } else {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                TestManager.getInstance().addEvent("Payment", "已拥有当前商品，开始查询", EventState.NORMAL, "");
                ILog.i(TAG, "check unconsumed goods with purchase response code:" + billingResult.getResponseCode());
                queryUnConsumedPurchases(false);
            } else {
                TestManager.getInstance().addEvent("Payment", "付费失败", EventState.ERROR, "response code:" + billingResult.getResponseCode());
                ILog.i(TAG, "default as user canceled purchase response code=" + billingResult.getResponseCode());
                if (purchaseResultCallback != null) {
                    // 在缺失订单的情况下，要求返回bill id, 增加临时记录
                    purchaseResultCallback.payResult(processing_bill_id, IPurchaseResult.PAY_FAILED, null, null, null);
                }
            }
        }
    }

    /**
     * 解析支付结果
     */
    private void handlePurchase(int responseCode, Purchase purchase) {
        String msg = "";
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                msg = "支付成功";
                break;

            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                msg = "已拥有该商品";
                break;

            default:
                String sku = purchase.getProducts().get(0);
                String obfuscatedAccountId = null;
                if (purchase.getAccountIdentifiers() != null) {
                    obfuscatedAccountId = purchase.getAccountIdentifiers().getObfuscatedAccountId();
                }
                msg = "付款失败：失败码：" + responseCode + "sku=" + sku + ";预下单id=" + obfuscatedAccountId;
                break;
        }
        ILog.i(TAG, "purchase end;" + msg);
        TestManager.getInstance().addEvent("Payment", msg, EventState.NORMAL, "");
        consumeGood(purchase);
    }

    /**
     * 消耗或确认商品
     */
    private void consumeGood(Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            String sku = (purchase.getProducts() != null && !purchase.getProducts().isEmpty()) ? purchase.getProducts().get(0) : "unknown";
            ILog.i(TAG, "good:" + sku + " consume failed; had not purchased!!!");
            TestManager.getInstance().addEvent("Payment", "消耗失败，未付费", EventState.ERROR, "sku=" + sku);
            return;
        }

        final String sku = purchase.getProducts().get(0);
        final String obfuscatedAccountId = purchase.getAccountIdentifiers() != null ? purchase.getAccountIdentifiers().getObfuscatedAccountId() : null;
        final String merchantTransactionId = billingUtils != null ? billingUtils.decodeMerchantTransactionId(obfuscatedAccountId) : null;

        final Map<String, Object> params;
        if (merchantTransactionId != null && !merchantTransactionId.isEmpty()) {
            params = new HashMap<>();
            params.put("merchantTransactionId", merchantTransactionId);
        } else {
            params = null;
        }

        final String payload = decodePayload(obfuscatedAccountId);
        final Goods goods = goodsData.get(sku);

        if (goods == null) {
            TestManager.getInstance().addEvent("Payment", "消耗失败，找不到商品信息", EventState.ERROR, "sku=" + sku);
            //找不到商品信息，已验证失败回传，遇到时需要与运营确认是否从配置内删除了计费点
            setupPurchaseResult(PurchaseState.VERIFICATION, false, null, payload, purchase, PurchaseError.ERROR_VERIFY_FAILED, params);
            return;
        }

        switch (goods.getType()) {
            case INAPP:
                TestManager.getInstance().addEvent("Payment", "消耗型商品，开始中台验证订单", EventState.NORMAL, "sku=" + sku);
                verifyPurchase(goods, sku, purchase, payload, params, new Runnable() {
                    @Override
                    public void run() {
                        TestManager.getInstance().addEvent("Payment", "中台订单验证成功，开始消耗", EventState.NORMAL, "sku=" + sku);
                        ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
                        billingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
                            @Override
                            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    TestManager.getInstance().addEvent("Payment", "消耗成功", EventState.NORMAL, "sku=" + sku);
                                    setupPurchaseResult(PurchaseState.PAY_RESULT, true, goods, payload, purchase, null, params);
                                    if (obfuscatedAccountId != null) {
                                        deletePayload(obfuscatedAccountId);
                                    }
                                } else {
                                    TestManager.getInstance().addEvent("Payment", "消耗失败,失败码:" + billingResult.getResponseCode() + ";" + billingResult.getDebugMessage(), EventState.ERROR, "sku=" + sku);
                                    setupPurchaseResult(PurchaseState.PAY_RESULT, false, goods, payload, purchase, PurchaseError.ERROR_CONSUME_FAILED_ + billingResult.getResponseCode(), params);
                                }
                            }
                        });
                    }
                }, new VerifyFailureCallback() {
                    @Override
                    public void onFailure(int code) {
                        ILog.i(TAG, "verify inapp:" + sku + " failed with server; code=" + code);
                        TestManager.getInstance().addEvent("Payment", "中台订单验证失败", EventState.ERROR, "sku=" + sku);
                        setupPurchaseResult(PurchaseState.PAY_RESULT, false, goods, payload, purchase, "verify_failed", params);
                    }
                });
                break;

            default: // 订阅商品
                TestManager.getInstance().addEvent("Payment", "订阅型商品，开始处理", EventState.NORMAL, "sku=" + sku);
                if (purchase.isAcknowledged()) {
                    TestManager.getInstance().addEvent("Payment", "历史订阅，已处理，直接返回成功", EventState.NORMAL, "sku=" + sku);
                    if (purchaseResultCallback != null) {
                        purchaseResultCallback.payResult(goods.getId(), IPurchaseResult.PAY_SUCCEED, payload, merchantTransactionId, purchase.getOriginalJson());
                    }
                } else {
                    TestManager.getInstance().addEvent("Payment", "开始中台验证订单", EventState.NORMAL, "sku=" + sku);
                    verifyPurchase(goods, sku, purchase, payload, params, new Runnable() {
                        @Override
                        public void run() {
                            TestManager.getInstance().addEvent("Payment", "中台订单验证成功，开始标记", EventState.NORMAL, "sku=" + sku);
                            AcknowledgePurchaseParams acknowledgeParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
                            billingClient.acknowledgePurchase(acknowledgeParams, new AcknowledgePurchaseResponseListener() {
                                @Override
                                public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                        TestManager.getInstance().addEvent("Payment", "标记成功", EventState.NORMAL, "sku=" + sku);
                                        setupPurchaseResult(PurchaseState.PAY_RESULT, true, goods, payload, purchase, null, params);
                                    } else {
                                        TestManager.getInstance().addEvent("Payment", "标记失败, 失败码:" + billingResult.getResponseCode() + ";" + billingResult.getDebugMessage(), EventState.NORMAL, "sku=" + sku);
                                        setupPurchaseResult(PurchaseState.PAY_RESULT, true, goods, payload, purchase, PurchaseError.ERROR_CONSUME_FAILED_ + billingResult.getResponseCode(), params);
                                    }
                                }
                            });
                        }
                    }, new VerifyFailureCallback() {
                        @Override
                        public void onFailure(int code) {
                            ILog.i(TAG, "verify subs:" + sku + " failed with server; code=" + code);
                            TestManager.getInstance().addEvent("Payment", "中台订单验证失败", EventState.ERROR, "sku=" + sku);
                            setupPurchaseResult(PurchaseState.PAY_RESULT, false, goods, payload, purchase, "verify_failed", params);
                        }
                    });
                }
                break;
        }
    }

    /**
     * 查询未处理的订单
     */
    private void queryUnConsumedPurchases(boolean isSubs) {
        String type = isSubs ? BillingClient.ProductType.SUBS : BillingClient.ProductType.INAPP;
        QueryPurchasesParams queryParams = QueryPurchasesParams.newBuilder().setProductType(type).build();
        billingClient.queryPurchasesAsync(queryParams, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (Purchase purchase : purchases) {
                        consumeGood(purchase);
                    }
                }
            }
        });
    }

    /**
     * 校验订单
     */
    private void verifyPurchase(final Goods goods, final String sku, final Purchase purchase, final String payload, final Map<String, Object> param, final Runnable success, final VerifyFailureCallback failure) {
        String obfuscatedAccountId = purchase.getAccountIdentifiers() != null ? purchase.getAccountIdentifiers().getObfuscatedAccountId() : null;
        String orderId = purchase.getOrderId();
        String orderPrefix = GridManager.getInstance().getGridConfig("payment.order.prefix", "");
        if (orderPrefix != null && !orderPrefix.isEmpty()) {
            if (orderId != null && !orderId.isEmpty() && !orderId.startsWith(sku) && !orderId.startsWith(orderPrefix)) {
                ILog.e(TAG, "orderId:" + orderId + " for sku:" + sku + " not correct");
                failure.onFailure(0);
                return;
            }
        }

        if (!verifyPurchaseWithKey(purchase)) {
            ILog.i(TAG, "key check failed");
            setupPurchaseResult(PurchaseState.VERIFICATION, false, goods, payload, purchase, PurchaseError.ERROR_KEY_VERIFY_FAILED, param);
            failure.onFailure(0);
            return;
        }

        if (billingUtils != null) {
            String merchantTransactionId = billingUtils.decodeMerchantTransactionId(obfuscatedAccountId);
            if (merchantTransactionId == null || merchantTransactionId.isEmpty()) {
                success.run();
            } else {
                billingUtils.verifyOrder(merchantTransactionId, purchase.getSignature(), purchase.getOriginalJson(), new BillingUtils.Callback() {
                    @Override
                    public void onResult(boolean result) {
                        if (result) {
                            setupPurchaseResult(PurchaseState.VERIFICATION, true, goods, payload, purchase, null, param);
                            success.run();
                        } else {
                            setupPurchaseResult(PurchaseState.VERIFICATION, false, goods, payload, purchase, PurchaseError.ERROR_UNKNOWN, param);
                            failure.onFailure(0);
                        }
                    }
                });
            }
        } else {
            success.run();
        }
    }

    /**
     * 验证失败回调接口
     */
    private interface VerifyFailureCallback {
        void onFailure(int code);
    }

    /**
     * 记录支付结果事件
     */
    private void setupPurchaseResult(int purchaseState, boolean result, Goods goods, String payload, Purchase purchase, String failReason, Map<String, Object> params) {
        String merchantTransactionId = null;
        if (params != null && params.get("merchantTransactionId") instanceof String) {
            merchantTransactionId = (String) params.get("merchantTransactionId");
        }

        Map<String, Object> data = new HashMap<>();
        switch (purchaseState) {
            case PurchaseState.PRE_ORDER:
                data.put("sdk_state", result ? 1 : 0);
                if (failReason != null) {
                    data.put("reason", failReason);
                }
                if (params != null) {
                    data.putAll(params);
                }
                if (goods != null) {
                    data.put("id", goods.getId());
                    data.put("sku", goods.getSku());
                    data.put("type", goods.getType());
                    data.put("conf_price", goods.getUsd());
                    if (goods.getProductDetails() != null) {
                        data.put("title", goods.getProductDetails().getTitle());
                    }
                    data.put("price", goods.getPrice());
                    data.put("currency", goods.getCurrency());
                    data.put("price_amount", goods.getPriceAmount());
                }
                if (merchantTransactionId != null) {
                    data.put("merchantTransactionId", merchantTransactionId);
                }
                data.putAll(clientInfos);
                data.put(EventParams.EVENT_PARAM_PAY_STATE, PurchaseState.PRE_ORDER);

                if (purchaseResultCallback != null) {
                    purchaseResultCallback.logEvent(EventIDs.IAP_PRE_ORDER, EventType.EVENT_TYPE_PURCHASE, EventSrc.EVENT_SRC_SDK, data);
                    if (!result && goods != null) {
                        purchaseResultCallback.payResult(goods.getId(), IPurchaseResult.PAY_FAILED, payload, merchantTransactionId, null);
                    }
                }
                break;

            case PurchaseState.VERIFICATION:
                data.put("sdk_state", result ? 1 : 0);
                if (failReason != null) {
                    data.put("reason", failReason);
                }
                if (params != null) {
                    data.putAll(params);
                }
                if (goods != null) {
                    data.put("id", goods.getId());
                    data.put("sku", goods.getSku());
                    data.put("price", goods.getPrice());
                    data.put("currency", goods.getCurrency());
                    data.put("price_amount", goods.getPriceAmount());
                    data.put("conf_price", goods.getUsd());
                }

                String sku = (purchase != null && purchase.getProducts() != null && !purchase.getProducts().isEmpty()) ? purchase.getProducts().get(0) : (goods != null ? goods.getSku() : null);
                int id = goods != null ? goods.getId() : 0;

                if (purchase != null) {
                    List<String> products = purchase.getProducts();
                    if (products != null && !products.isEmpty()) {
                        data.put("sku", products.get(0));
                    }
                    data.put("purchase_state", purchase.getPurchaseState());
                    data.put("purchase_token", purchase.getPurchaseToken());
                    data.put("purchase_time", purchase.getPurchaseTime());
                    if (purchase.getOrderId() != null) {
                        data.put("order_id", purchase.getOrderId());
                    }
                    data.put("is_acknowledged", purchase.isAcknowledged() ? 1 : 0);
                    data.put("is_auto_renewing", purchase.isAutoRenewing() ? 1 : 0);
                    data.put("package_name", purchase.getPackageName());
                    data.put("signature", purchase.getSignature());
                    data.put("original_json", purchase.getOriginalJson());
                }

                if (merchantTransactionId != null) {
                    data.put("merchantTransactionId", merchantTransactionId);
                }
                data.putAll(clientInfos);
                data.put(EventParams.EVENT_PARAM_PAY_STATE, PurchaseState.VERIFICATION);

                if (purchaseResultCallback != null && !result && sku != null) {
                    purchaseResultCallback.payResult(id, IPurchaseResult.PAY_FAILED, payload, merchantTransactionId, null);
                }
                if (purchaseResultCallback != null) {
                    purchaseResultCallback.logEvent(EventIDs.IAP_VERIFICATION, EventType.EVENT_TYPE_PURCHASE, EventSrc.EVENT_SRC_SDK, data);
                }
                break;

            case PurchaseState.PAY_RESULT:
                int payId = goods != null ? goods.getId() : 0;
                if (purchase != null) {
                    String purchase_token = purchase.getPurchaseToken();
//                    if (purchase.getOrderId() != null) {
//                        data.put("order_id", purchase.getOrderId());
//                    }
                    String order_id = purchase.getOrderId();
                    if (TextUtils.isEmpty(order_id)) return;
                    if (order_id.length() < 5 || order_id.length() > 200) return;
                    if (!claimToken(purchase_token)) return;
                    if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;

                }

                if (purchaseResultCallback != null) {
                    purchaseResultCallback.payResult(payId, result ? IPurchaseResult.PAY_SUCCEED : IPurchaseResult.PAY_FAILED, payload, merchantTransactionId, purchase != null ? purchase.getOriginalJson() : null);
                }

                if (result) {
                    String orderId = purchase != null ? purchase.getOrderId() : null;
                    if (orderId != null && !orderId.isEmpty()) {
                        if (!LocalStorage.Instance.containsKey(orderId + "_logged")) {
                            LocalStorage.Instance.encodeString(orderId + "_logged", orderId);
                            if (goods != null) {
                                int totalOrders = LocalStorage.Instance.decodeInt("total_orders", 0) + 1;
                                double totalRevenue = LocalStorage.Instance.decodeDouble("total_revenue", 0.0) + goods.getPriceAmount();
                                LocalStorage.Instance.encodeInt("total_orders", totalOrders);
                                LocalStorage.Instance.encodeDouble("total_revenue", totalRevenue);
                            }
                        }
                    }
                }

                data.put("sdk_state", result ? 1 : 0);
                if (failReason != null) {
                    data.put("reason", failReason);
                }
                if (params != null) {
                    data.putAll(params);
                }
                if (goods != null) {
                    data.put("id", goods.getId());
                    data.put("sku", goods.getSku());
                    data.put("price", goods.getPrice());
                    data.put("currency", goods.getCurrency());
                    data.put("price_amount", goods.getPriceAmount());
                    data.put("type", goods.getType());
                    data.put("title", goods.getTitle());
                    data.put("conf_price", goods.getUsd());
                }
                if (purchase != null) {
                    List<String> products = purchase.getProducts();
                    if (products != null && !products.isEmpty()) {
                        data.put("sku", products.get(0));
                    }
                    data.put("purchase_state", purchase.getPurchaseState());
                    data.put("purchase_token", purchase.getPurchaseToken());
                    data.put("purchase_time", purchase.getPurchaseTime());
                    if (purchase.getOrderId() != null) {
                        data.put("order_id", purchase.getOrderId());
                    }
                    data.put("is_acknowledged", purchase.isAcknowledged() ? 1 : 0);
                    data.put("is_auto_renewing", purchase.isAutoRenewing() ? 1 : 0);
                    data.put("package_name", purchase.getPackageName());
                    data.put("signature", purchase.getSignature());
                    data.put("original_json", purchase.getOriginalJson());
                }
                if (merchantTransactionId != null) {
                    data.put("merchantTransactionId", merchantTransactionId);
                }
                data.putAll(clientInfos);
                data.put(EventParams.EVENT_PARAM_PAY_CHANNEL, "google_pay");
                data.put(EventParams.EVENT_PARAM_PAY_STATE, PurchaseState.PAY_RESULT);

                if (purchaseResultCallback != null) {
                    purchaseResultCallback.logEvent(EventIDs.IAP_PURCHASED, EventType.EVENT_TYPE_PURCHASE, EventSrc.EVENT_SRC_SDK, data);
                }
                break;
        }
    }

    /**
     * 通过key校验订单
     */
    private boolean verifyPurchaseWithKey(Purchase purchase) {
        if (verifyKey == null || verifyKey.isEmpty()) {
            ILog.w(TAG, "IAP public key is not configured, will NOT verify the purchase");
            return true;
        }
        String signature = purchase.getSignature();
        String purchaseData = purchase.getOriginalJson();
        boolean verified = Security.verifyPurchase(verifyKey, purchaseData, signature);
        if (!verified) {
            ILog.e(TAG, "purchase key verify failed");
            ILog.i(TAG, "OrderID: " + purchase.getOrderId());
            ILog.i(TAG, "Signature: " + purchase.getSignature());
            ILog.i(TAG, "PurchaseData: " + purchase.getOriginalJson());
        } else {
            ILog.i(TAG, "Purchase key Verified success");
        }
        return verified;
    }

    private void setupClientInfo(String clientInfo) {
        if (clientInfo != null && !clientInfo.isEmpty()) {
            try {
                JSONObject json = new JSONObject(clientInfo);
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = json.get(key);
                    if (value instanceof Boolean) {
                        clientInfos.put(key, ((Boolean) value) ? 1 : 0);
                    } else {
                        clientInfos.put(key, value);
                    }
                }
            } catch (Exception e) {
                ILog.e(TAG, "format client info failed:" + e.getMessage());
            }
        } else {
            clientInfos.clear();
        }
    }

    private String localOrderId() {
        String randStr = Util.randomString(28, true);
        try {
            String randStr2 = Util.versionCode() + "-" + Util.versionName() + "-" + Util.availableMemory() + "-" + Util.availableDiskSize();
            String content = System.currentTimeMillis() + "-" + SystemClock.elapsedRealtime() + "-" + randStr + "-" + randStr2;
            String baseString = Base64.encodeToString(content.getBytes(Charset.defaultCharset()), Base64.NO_WRAP);
            String result = Util.md5(baseString);
            if (result.length() > 28) {
                return result.substring(0, 28);
            } else {
                int less = 28 - result.length();
                StringBuilder builder = new StringBuilder(result);
                while (less > 0) {
                    builder.append("a");
                    less--;
                }
                return builder.toString();
            }
        } catch (Exception e) {
            // ignore
        }
        return randStr;
    }

    private boolean claimToken(String token) {
        try {
            if (TextUtils.isEmpty(token)) {
                return false;
            }
            String en_token = Util.hashToken(token);
            String str = LocalStorage.Instance.decodeString("ga_pay_token", "[]");
            JSONArray json = new JSONArray(str);
            int length = json.length();
            for (int i = 0; i < length; i++) {
                String item = json.optString(i, "");
                if (item.equals(en_token)) {
                    return false;
                }
            }
            json.put(en_token);
            if (json.length() > 10) {
                json.remove(0);
            }
            LocalStorage.Instance.encodeString("ga_pay_token", json.toString());
        } catch (Exception ignored) {
        }
        return true;
    }


}

