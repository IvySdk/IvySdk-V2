package com.ivy.enjoypay;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.ivy.base.ILog;
import com.ivy.base.billing.GoodsType;
import com.ivy.base.http.HttpUtil;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.util.Util;
import com.ivy.base.util.rx.Func;
import com.ivy.base.util.rx.Observer;
import com.ivy.base.util.rx.RxAndroid;
import com.ivy.base.util.rx.Schedulers;
import com.ivy.base.util.rx.SimpleRxJust;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//https://verify.ivymobile.com/api/external/v1/enjoy_pay/


class UnShippingItem {
    public int payId;
    public String sku;
    public String merchant_transaction_id;
    public String payload;
    public String order_id;

    public UnShippingItem(int payId, String sku, String merchant_transaction_id, String payload, String order_id) {
        this.payId = payId;
        this.sku = sku;
        this.merchant_transaction_id = merchant_transaction_id;
        this.payload = payload;
        this.order_id = order_id;
    }
}

public class BillingManager<T> {

    private final String appId;
    private final String baseUrl;
    private final boolean debug;
    private static final String TAG = "billing";

    public static final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.myLooper());

    private OkHttpClient okHttpClient;


    public BillingManager(String appId, String baseUrl, boolean debug) {
        this.appId = appId;
        this.baseUrl = baseUrl;
        this.debug = debug;
    }

    public String formatUrl(String action) {
        if (baseUrl == null) return "";
        return baseUrl.endsWith("/") ? (baseUrl + action) : (baseUrl + "/" + action);
    }

    public interface Action<T> {
        void onResult(T t);
    }

    public interface Action2<T> {
        void onResult(boolean status, T t);
    }


    public void preOrder(final Goods good, final String payload, final Action2<PurchaseTask> callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                PurchaseTask task = null;
                try {
                    task = new PurchaseTask(good.getId(), good, payload);

                    String payUserId = LocalStorage.Instance.decodeString("_pay_user_id_", Util.roleId());
                    JSONObject param = new JSONObject();
                    param.put("is_encrypt", false);
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("uuid", payUserId);
                    body.put("is_sandbox", debug);

                    body.put("country", debug ? "US" : Util.countryCode());

                    JSONObject skuJson = new JSONObject();
                    skuJson.put("pay_id", good.getId());
                    skuJson.put("id", good.getSku());
                    skuJson.put("price", good.getPrice());
                    skuJson.put("price_amount", good.getPriceAmount() * 100);
                    skuJson.put("currency", good.getCurrency());
                    skuJson.put("name", good.getTitle());
                    skuJson.put("desc", good.getDesc());
                    skuJson.put("usd", good.getUsd());
                    skuJson.put("payload", payload != null ? payload : "");

                    body.put("sku_json", skuJson);
                    param.put("data", body);
                    String url = formatUrl(good.getType().equals(GoodsType.INAPP) ? "pre_enjoy" : "pre_sub_enjoy");
                    String data = postRequest(url, param.toString());


                    if (data != null) {
                        JSONObject json = new JSONObject(data).getJSONObject("data").getJSONObject("data");
                        task.merchantTransactionId = json.optString("mchOrderNo", null);
                        task.payUrl = json.optString("payData", null);
                        task.order_id = json.optString("payOrderId", null);

                        PurchaseTask finalTask2 = task;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ILog.e(TAG, "pre order success:=" + finalTask2.toString());
                                if (callback != null) {
                                    callback.onResult(true, finalTask2);
                                }
                            }
                        });
                    } else {
                        PurchaseTask finalTask1 = task;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ILog.e(TAG, "pre order failed: response data empty");
                                if (callback != null) {
                                    callback.onResult(false, finalTask1);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    ILog.e(TAG, "pre order err for:" + good.getId() + "; err:" + e.getMessage());
                    PurchaseTask finalTask = task;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ILog.e(TAG, "pre order failed: " + e.getMessage());
                            if (callback != null) {
                                callback.onResult(false, finalTask);
                            }
                        }
                    });
                }
            }
        });
    }

    public void purchasing(PurchaseTask task, final Action<PurchaseTask> callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String payUserId = LocalStorage.Instance.decodeString("_pay_user_id_", Util.roleId());
                    JSONObject param = new JSONObject();
                    param.put("is_encrypt", false);
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("uuid", payUserId);
                    body.put("is_sandbox", debug);

                    body.put("country", debug ? "US" : Util.countryCode());

                    body.put("merchant_transaction_id", task.merchantTransactionId);
                    param.put("data", body);
                    String url = formatUrl("pend_enjoy");
                    String data = postRequest(url, param.toString());

                    if (data != null) {
                        boolean result = new JSONObject(data).getBoolean("data");
                        if (!result) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    ILog.e(TAG, "pending failed: response data empty");
                                    if (callback != null) {
                                        callback.onResult(null);
                                    }
                                }
                            });
                            return;
                        }
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ILog.e(TAG, "pending success:" + task.toString());
                                if (callback != null) {
                                    callback.onResult(task);
                                }
                            }
                        });
                    } else {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ILog.e(TAG, "pending failed: response data empty");
                                if (callback != null) {
                                    callback.onResult(null);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    ILog.e(TAG, "pending err:" + e.getMessage());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onResult(null);
                            }
                        }
                    });
                }
            }
        });
    }

    public void verifyOrder(PurchaseTask task, Action2<PurchaseTask> callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String payUserId = LocalStorage.Instance.decodeString("_pay_user_id_", Util.roleId());
                    JSONObject param = new JSONObject();
                    param.put("is_encrypt", false);
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("uuid", payUserId);
                    body.put("is_sandbox", debug);
                    body.put("country", debug ? "US" : Util.countryCode());

                    body.put("merchant_transaction_id", task.merchantTransactionId);
                    body.put("pay_order_id", task.order_id);
                    param.put("data", body);
                    String url = formatUrl(task.goods.getType().equals(GoodsType.INAPP) ? "verify_enjoy" : "verify_sub_enjoy");
                    String data = postRequest(url, param.toString());

                    if (data != null) {
                        boolean result = new JSONObject(data).getBoolean("data");
                        if (!result) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    ILog.e(TAG, "verify result failed: response data empty");
                                    if (callback != null) {
                                        callback.onResult(false, task);
                                    }
                                }
                            });
                            return;
                        }
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ILog.e(TAG, "verify result success:" + task.toString());
                                if (callback != null) {
                                    callback.onResult(true, task);
                                }
                            }
                        });
                    } else {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ILog.e(TAG, "verify result failed: response data empty");
                                if (callback != null) {
                                    callback.onResult(false, task);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    ILog.e(TAG, "verify result err:" + e.getMessage());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onResult(false, task);
                            }
                        }
                    });
                }
            }
        });
    }

    public void receiptGoods(String merchantTransactionId, Action<Boolean> callback) {
        String payUserId = LocalStorage.Instance.decodeString("_pay_user_id_", Util.roleId());
        new SimpleRxJust<>(merchantTransactionId).map(new Func<String, Boolean>() {
            @Override
            public Boolean call(String value) throws Exception {
                try {
                    JSONObject param = new JSONObject();
                    param.put("is_encrypt", false);
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("uuid", payUserId);
                    body.put("merchant_transaction_id", merchantTransactionId);
                    param.put("data", body);

                    String url = formatUrl("consume_enjoy");
                    String data = postRequest(url, param.toString());

                    return data != null && new JSONObject(data).optBoolean("data", false);
                } catch (Exception e) {
                    ILog.e(TAG, "receipt goods failed:" + e.getMessage());
                }
                return false;
            }
        }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                ILog.e(TAG, "receipt goods result:" + aBoolean);
                if (callback != null && aBoolean != null) {
                    callback.onResult(aBoolean);
                }
            }

            @Override
            public void onError(Throwable e) {
                ILog.e(TAG, "receipt goods failed:" + e.getMessage());
                if (callback != null) {
                    callback.onResult(false);
                }
            }
        });
    }

    public void unShippedGoodsOrders(Action<List<UnShippingItem>> callback) {
        String payUserId = LocalStorage.Instance.decodeString("_pay_user_id_", Util.roleId());
        new SimpleRxJust<>("").map(new Func<String, List<UnShippingItem>>() {
            @Override
            public List<UnShippingItem> call(String value) throws Exception {
                try {
                    String url = formatUrl("unconsume_enjoy");
                    String uuid = payUserId;
                    String data = getRequest(url + "?app_id=" + appId + "&uuid=" + uuid);

                    List<UnShippingItem> missedOrders = new ArrayList<>();
                    if (data != null) {
                        JSONArray datas = new JSONObject(data).getJSONArray("data");
                        for (int i = 0; i < datas.length(); i++) {
                            JSONObject json = datas.getJSONObject(i);
                            try {
                                String itemAppId = json.getString("app_id");
                                if (!itemAppId.equals(appId)) continue;
                                String itemUUID = json.getString("uuid");
                                if (!itemUUID.equals(uuid)) continue;
                                String rpDate = json.getString("rp_date");
                                String country = json.getString("country");
                                String merchantTransactionId = json.getString("merchant_transaction_id");
                                if (merchantTransactionId.isEmpty()) continue;
                                String orderId = json.getString("order_id");
                                if (orderId.isEmpty()) continue;
                                String environment = json.getString("environment");
                                int orderStatus = json.getInt("order_status");
                                if (orderStatus != 3) continue;
                                String productId = json.getString("product_id");
                                if (productId.isEmpty()) continue;
                                String name = json.getString("name");
                                double usd = json.getDouble("usd");
                                String payload = json.optString("payload");
                                int payId = json.getInt("pay_id");

                                UnShippingItem unShippingItem = new UnShippingItem(payId, productId, merchantTransactionId, payload, orderId);
                                missedOrders.add(unShippingItem);
                            } catch (Exception e) {
                                ILog.e(TAG, "check missed order err:" + e.getMessage());
                            }
                        }
                    } else {
                        throw new InvalidObjectException("response data invalid");
                    }

                    return missedOrders;
                } catch (Exception e) {
                    ILog.e(TAG, "check unreceived orders failed:" + e.getMessage());
                }
                return null;
            }
        }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<List<UnShippingItem>>() {
            @Override
            public void onSuccess(List<UnShippingItem> missedOrders) {
                if (callback != null) {
                    callback.onResult(missedOrders);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (callback != null) {
                    callback.onResult(null);
                }
            }
        });
    }

    private void encodeMerchantTransactionId(String sku, String merchantTransactionId) {
        if (TextUtils.isEmpty(sku)) return;
        LocalStorage.Instance.encodeString("enjoy_merchant_transaction_" + sku, merchantTransactionId);
    }

    public String decodeMerchantTransactionId(String sku) {
        if (TextUtils.isEmpty(sku)) return "";
        return LocalStorage.Instance.decodeString("enjoy_merchant_transaction_" + sku, "");
    }

    private String getRequest(String url) {
        try {
            OkHttpClient okHttpClient = getOkHttpClient();
            Request request = new Request.Builder().url(url).get().build();
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                assert response.body() != null;
                return response.body().string();
            }
        } catch (Exception e) {
            ILog.e("okhttp", "get url:" + url + " failed:" + e.getMessage());
        }
        return null;
    }

    private String postRequest(String url, String param) {
        try {
            OkHttpClient okHttpClient = getOkHttpClient();
            MediaType mediaType = MediaType.get("application/json; charset=utf-8");
            RequestBody postBody = RequestBody.create(param, mediaType);
            Request request = new Request.Builder().url(url).post(postBody).build();
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                assert response.body() != null;
                return response.body().string();
            }
        } catch (Exception e) {
            ILog.e("okhttp", "post url:" + url + " failed:" + e.getMessage());
        }
        return null;
    }

    private synchronized OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(false)
                    .build();
        }
        return okHttpClient;
    }


}
