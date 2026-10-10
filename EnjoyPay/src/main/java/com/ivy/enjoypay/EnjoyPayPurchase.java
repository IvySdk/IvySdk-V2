package com.ivy.enjoypay;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.ivy.base.ActivityUtil;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.billing.AbsPurchase;
import com.ivy.base.billing.IIPurchaseResult;
import com.ivy.base.billing.IPurchaseResult;
import com.ivy.base.billing.PurchaseState;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.track.EventIDs;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class EnjoyPayPurchase extends AbsPurchase<Goods> implements IUnProguard {

    private final String TAG = "EnjoyPay";

    private BillingManager billingUtils;

    private final Map<String, Object> clientInfos = new HashMap<>();

//    private PurchaseTask processingTask = null;

    private final Map<String, PurchaseTask> taskMap = new HashMap<>();

    @Override
    public void setup(String appId, String data, String enjoy_pay_data, boolean debug, IIPurchaseResult callback) {
        super.setup(appId, data, enjoy_pay_data, debug, callback);
        try {
            JSONObject json = new JSONObject(enjoy_pay_data);
            // GP 后台提供的校验 key
            //  verifyKey = json.optString("key");
            // 校验地址
            verifyServerUrl = json.optString("verify-url");

            if (!TextUtils.isEmpty(verifyServerUrl)) {
                billingUtils = new BillingManager(appId, verifyServerUrl, debug);
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
                            item.updatePriceAmount((long) (item.getUsd() * 100));
                            item.updateOriginalPriceAmount((long) (item.getUsd() * 100));
                            ids.put(payId, item.getSku());
                            goodsData.put(item.getSku(), item); // 最终计费点数据
                        } else {
                            ILog.e(TAG, "invalid sku for " + key);
                        }
                    } catch (NumberFormatException e) {
                        ILog.e(TAG, "invalid pay id:" + key);
                    }
                }

            } else {
                ILog.e(TAG, "no goods config!!!");
            }

        } catch (Exception e) {
            ILog.e(TAG, "invalid payment config");
        }
    }

    @Override
    public boolean isPaymentInitialized() {
        return billingUtils != null;
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
    public void pay(int id, String payload, String clientInfo) {
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
            setupPurchaseResult(PurchaseState.PRE_ORDER, false, new PurchaseTask(id, null, payload));
            return;
        }

        final Goods goods = goodsData.get(sku);
        if (goods == null) {
            TestManager.getInstance().addEvent("Payment", "付费失败，未找到计费点信息", EventState.ERROR, "billId=" + id);
            ILog.w(TAG, "no goods for pay id:" + id + " & sku:" + sku + " setup! check config file");
            setupPurchaseResult(PurchaseState.PRE_ORDER, false, new PurchaseTask(id, null, payload));
            return;
        }
        setupClientInfo(clientInfo);
        if (billingUtils != null) {
            TestManager.getInstance().addEvent("Payment", "开始预下单", EventState.NORMAL, "billId=" + id);
            billingUtils.preOrder(goods, payload, new BillingManager.Action2<PurchaseTask>() {
                @Override
                public void onResult(boolean status, PurchaseTask task) {
                    if (status) {
                        if (task == null) {
                            TestManager.getInstance().addEvent("Payment", "预下单返回成功，但信息无效", EventState.ERROR, "empty task");
                            setupPurchaseResult(PurchaseState.PRE_ORDER, false, task);
                            return;
                        }

                        if (TextUtils.isEmpty(task.merchantTransactionId) || TextUtils.isEmpty(task.payUrl) || TextUtils.isEmpty(task.order_id)) {
                            TestManager.getInstance().addEvent("Payment", "预下单返回成功，但信息无效", EventState.ERROR, task.toString());
                            setupPurchaseResult(PurchaseState.PRE_ORDER, false, task);
                            return;
                        }

                        TestManager.getInstance().addEvent("Payment", "预下单成功", EventState.NORMAL, task.toString());
                        encodePayload(task.merchantTransactionId, payload);
                        java.util.Map<String, Object> extra = new java.util.HashMap<>();
                        extra.put("merchantTransactionId", task.merchantTransactionId);
                        setupPurchaseResult(PurchaseState.PRE_ORDER, true, task);
                        doPay(activity, task);
                    } else {
                        TestManager.getInstance().addEvent("Payment", "预下单失败", EventState.ERROR, "billId=" + id);
                        setupPurchaseResult(PurchaseState.PRE_ORDER, false, task);
                    }
                }

            });
        } else {
            TestManager.getInstance().addEvent("Payment", "付费失败", EventState.ERROR, "enjoy pay校验连接未配置");
            setupPurchaseResult(PurchaseState.PRE_ORDER, false, new PurchaseTask(id, null, payload));
        }
    }

    private void doPay(Activity activity, PurchaseTask task) {
        if (billingUtils != null) {
            billingUtils.purchasing(task, new BillingManager.Action<PurchaseTask>() {
                @Override
                public void onResult(PurchaseTask task) {
                    if (task != null && !TextUtils.isEmpty(task.merchantTransactionId)) {

                        taskMap.put(task.merchantTransactionId, task);
//                        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
//                        intent.launchUrl(activity, Uri.parse(task.payUrl));
                        Intent intent = new Intent(activity, EnjoyPayActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        intent.putExtra("payUrl", task.payUrl);
                        intent.putExtra("merchantTransactionId", task.merchantTransactionId);
                        activity.startActivityForResult(intent, EnjoyPayActivity.REQUEST_CODE);
                    } else {
                        setupPurchaseResult(PurchaseState.PAY_RESULT, false, task);
                    }
                }
            });
        } else {
            setupPurchaseResult(PurchaseState.PAY_RESULT, false, task);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EnjoyPayActivity.REQUEST_CODE) {
            String merchantTransactionId = null;
            if (data == null){
                ILog.e(TAG, "no intent data back");
                return;
            }
            if (data.hasExtra("merchantTransactionId")) {
                merchantTransactionId = data.getStringExtra("merchantTransactionId");
            }
            if (TextUtils.isEmpty(merchantTransactionId)) {
                ILog.e(TAG, "no merchantTransactionId back");
                return;
            }
            PurchaseTask processingTask = taskMap.get(merchantTransactionId);
            if (processingTask == null){
                ILog.e(TAG, "no paying task found for:" + merchantTransactionId);
                return;
            }
            if (resultCode == EnjoyPayActivity.REQUEST_FAILED_CODE) {
                setupPurchaseResult(PurchaseState.PAY_RESULT, false, processingTask);
            } else if (resultCode == EnjoyPayActivity.RESULT_SUCCESS_CODE) {
                verifyPayResult(0, processingTask);
            }
        }
    }

    private void verifyPayResult(int count, PurchaseTask processingTask) {
        if (billingUtils != null && processingTask != null && count < 2) {
            billingUtils.verifyOrder(processingTask, new BillingManager.Action2<PurchaseTask>() {
                @Override
                public void onResult(boolean status, PurchaseTask task) {
                    if (status && task != null) {
                        setupPurchaseResult(PurchaseState.PAY_RESULT, true, task);
                       // shippingGoods(task.merchantTransactionId);
                    } else {
                        ILog.d(TAG, "verify step:" + count + " failed; try next");
                        int c = count + 1;
                        verifyPayResult(c, processingTask);
                    }
                }
            });
        } else {
            setupPurchaseResult(PurchaseState.VERIFICATION, false, processingTask);
        }
    }

    @Override
    public void shippingGoods(String merchantTransactionId) {
        if (billingUtils != null){
            billingUtils.receiptGoods(merchantTransactionId, new BillingManager.Action<Boolean>() {
                @Override
                public void onResult(Boolean state) {
                    ILog.i(TAG, "shipping goods result:" + state);
                    if (purchaseResultCallback != null) {
                        purchaseResultCallback.onShippingResult(merchantTransactionId, state);
                    }
                    deletePayload(merchantTransactionId);
                }
            });
        } else {
            if (purchaseResultCallback != null) {
                purchaseResultCallback.onShippingResult(merchantTransactionId, false);
            }
        }
    }

    @Override
    public void queryPurchase(int id) {
        if (billingUtils != null) {
            billingUtils.unShippedGoodsOrders(new BillingManager.Action<List<UnShippingItem>>() {
                @Override
                public void onResult(List<UnShippingItem> unShippingItems) {
                    if (unShippingItems != null) {
                        if (unShippingItems.size() > 0) {
                            for (UnShippingItem item : unShippingItems) {
                                if (goodsData.containsKey(item.sku)) {
                                    Goods goods = goodsData.get(item.sku);
                                    PurchaseTask purchaseTask = new PurchaseTask(item.payId, goods, item.payload);
                                    purchaseTask.order_id = item.order_id;
                                    purchaseTask.merchantTransactionId = item.merchant_transaction_id;
                                    String payload = item.payload;
                                    if (TextUtils.isEmpty(payload)){
                                        payload = decodePayload(item.sku);
                                    }
                                    purchaseTask.payload = payload;
                                    setupPurchaseResult(PurchaseState.PAY_RESULT, true, purchaseTask);
                                }
                            }
                        }
                    }
                }
            });
        }
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


    /**
     * 记录payload
     */
    private void encodePayload(String sku, String payload) {
        if (payload != null) {
            LocalStorage.Instance.encodeString("payload_enjoy_" + sku, payload);
        } else {
            LocalStorage.Instance.removeValueForKey("payload_enjoy_" + sku);
        }
    }

    /**
     * 删除payload
     */
    private String decodePayload(String sku) {
        return LocalStorage.Instance.decodeString("payload_enjoy_" + sku, null);
    }

    private void deletePayload(String sku) {
        String key = "payload_enjoy_" + sku;
        LocalStorage.Instance.removeValueForKey(key);
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

    /**
     * 记录支付结果事件
     */
    private void setupPurchaseResult(int purchaseState, boolean result, PurchaseTask task) {
        if (task == null) return;
        Map<String, Object> data = new HashMap<>();
        switch (purchaseState) {
            case PurchaseState.PRE_ORDER:
                data.put("sdk_state", result ? 1 : 0);

                if (task.goods != null) {
                    data.put("id", task.goods.getId());
                    data.put("sku", task.goods.getSku());
                    data.put("type", task.goods.getType());
                    data.put("conf_price", task.goods.getUsd());
                    if (!TextUtils.isEmpty(task.goods.getTitle())) {
                        data.put("title", task.goods.getTitle());
                    }
                    data.put("price", task.goods.getPrice());
                    data.put("currency", task.goods.getCurrency());
                    data.put("price_amount", task.goods.getPriceAmount());
                }
                if (!TextUtils.isEmpty(task.order_id)) {
                    data.put("order_id", task.order_id);
                }
                if (task.merchantTransactionId != null) {
                    data.put("merchantTransactionId", task.merchantTransactionId);
                }
                data.putAll(clientInfos);
                data.put(EventParams.EVENT_PARAM_PAY_STATE, PurchaseState.PRE_ORDER);

                if (purchaseResultCallback != null) {
                    purchaseResultCallback.logEvent(EventIDs.IAP_PRE_ORDER, EventType.EVENT_TYPE_PURCHASE, EventSrc.EVENT_SRC_SDK, data);
                    if (!result) {
                        purchaseResultCallback.payResult(task.id, IPurchaseResult.PAY_FAILED, task.payload, task.merchantTransactionId, null);
                    }
                }
                break;

            case PurchaseState.VERIFICATION:
                data.put("sdk_state", result ? 1 : 0);
                if (task.goods != null) {
                    data.put("id", task.goods.getId());
                    data.put("sku", task.goods.getSku());
                    data.put("type", task.goods.getType());
                    data.put("conf_price", task.goods.getUsd());
                    if (!TextUtils.isEmpty(task.goods.getTitle())) {
                        data.put("title", task.goods.getTitle());
                    }
                    data.put("price", task.goods.getPrice());
                    data.put("currency", task.goods.getCurrency());
                    data.put("price_amount", task.goods.getPriceAmount());
                }
                if (!TextUtils.isEmpty(task.order_id)) {
                    data.put("order_id", task.order_id);
                }
                if (task.merchantTransactionId != null) {
                    data.put("merchantTransactionId", task.merchantTransactionId);
                }
                data.putAll(clientInfos);
                data.put(EventParams.EVENT_PARAM_PAY_STATE, PurchaseState.VERIFICATION);

                if (purchaseResultCallback != null && !result) {
                    purchaseResultCallback.payResult(task.id, IPurchaseResult.PAY_FAILED, task.payload, task.merchantTransactionId, null);
                }
                if (purchaseResultCallback != null) {
                    purchaseResultCallback.logEvent(EventIDs.IAP_VERIFICATION, EventType.EVENT_TYPE_PURCHASE, EventSrc.EVENT_SRC_SDK, data);
                }
                break;

            case PurchaseState.PAY_RESULT:
                if (purchaseResultCallback != null) {
                    purchaseResultCallback.payResult(task.id, result ? IPurchaseResult.PAY_SUCCEED : IPurchaseResult.PAY_FAILED, task.payload, task.merchantTransactionId, null);
                }
                data.put("sdk_state", result ? 1 : 0);

                if (task.goods != null) {
                    data.put("id", task.goods.getId());
                    data.put("sku", task.goods.getSku());
                    data.put("type", task.goods.getType());
                    data.put("conf_price", task.goods.getUsd());
                    if (!TextUtils.isEmpty(task.goods.getTitle())) {
                        data.put("title", task.goods.getTitle());
                    }
                    data.put("price", task.goods.getPrice());
                    data.put("currency", task.goods.getCurrency());
                    data.put("price_amount", task.goods.getPriceAmount());
                }
                if (!TextUtils.isEmpty(task.order_id)) {
                    data.put("order_id", task.order_id);
                }
                if (!TextUtils.isEmpty(task.merchantTransactionId)) {
                    data.put("merchantTransactionId", task.merchantTransactionId);

                    taskMap.remove(task.merchantTransactionId);
                }
                data.putAll(clientInfos);
                data.put(EventParams.EVENT_PARAM_PAY_CHANNEL, "enjoy_pay");
                data.put(EventParams.EVENT_PARAM_PAY_STATE, PurchaseState.PAY_RESULT);

                if (purchaseResultCallback != null) {
                    purchaseResultCallback.logEvent(EventIDs.IAP_PURCHASED, EventType.EVENT_TYPE_PURCHASE, EventSrc.EVENT_SRC_SDK, data);
                }
                break;
        }
    }


}
