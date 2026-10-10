package com.ivy.base.billing;

import android.text.TextUtils;

import com.ivy.base.ILog;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BillingUtils<T> {

    private final String appId;
    private final String baseUrl;
    private final boolean debug;
    private static final String TAG = "billing";


    public BillingUtils(String appId, String baseUrl, boolean debug) {
        this.appId = appId;
        this.baseUrl = baseUrl;
        this.debug = debug;
    }

    private String formatUrl(String action) {
        if (baseUrl == null) return "";
        return baseUrl.endsWith("/") ? (baseUrl + action) : (baseUrl + "/" + action);
    }

    public interface PreOrderCallback<T> {
        void onResult(boolean status, AbsGoods<T> goods, String merchantTransactionId);
    }

    public interface Callback {
        void onResult(boolean state);
    }

    public interface MissedOrderCallback {
        void onResult(List<MissedOrder> data);
    }

    public void preOrder(final AbsGoods<T> good, final String payload, final PreOrderCallback<T> callback) {
        String payUserId = LocalStorage.Instance.decodeString("_pay_user_id_", Util.roleId());

        new SimpleRxJust<>("").map(s -> {
            String merchantTransactionId = null;
            try {
                JSONObject param = new JSONObject();
                param.put("is_encrypt", false);
                JSONObject body = new JSONObject();
                body.put("app_id", appId);
                body.put("uuid", payUserId);
                body.put("is_sandbox", debug);
                body.put("country", Locale.getDefault().getCountry());

                JSONObject skuJson = new JSONObject();
                skuJson.put("pay_id", good.id);
                skuJson.put("id", good.sku);
                skuJson.put("price", good.price);
                skuJson.put("price_amount", good.getPriceAmount());
                skuJson.put("currency", good.currency);
                skuJson.put("name", good.title);
                skuJson.put("desc", good.desc);
                skuJson.put("usd", good.usd);
                skuJson.put("payload", payload != null ? payload : "");

                body.put("sku_json", skuJson);
                param.put("data", body);
                String url = formatUrl("pre_google");
                String data = postRequest(url, param.toString());

                if (data != null) {
                    JSONObject json = new JSONObject(data);
                    merchantTransactionId = json.getJSONObject("data").optString("merchant_transaction_id", null);
                }
            } catch (Exception e) {
                ILog.e(TAG, "pre order err for:" + good.id + "; err:" + e.getMessage());
            }
            return merchantTransactionId;
        }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<String>() {
            @Override
            public void onSuccess(String merchantTransactionId) {
                ILog.e(TAG, "pre order result:" + merchantTransactionId);
                if (merchantTransactionId == null || merchantTransactionId.isEmpty()) {
                    if (callback != null) {
                        callback.onResult(false, good, null);
                    }
                } else {
                    if (callback != null) {
                        encodeMerchantTransactionId(merchantTransactionId, merchantTransactionId);
                        callback.onResult(true, good, merchantTransactionId);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                ILog.e(TAG, "pre order failed:" + e.getMessage());
                if (callback != null) {
                    callback.onResult(false, good, null);
                }
            }
        });
    }

    public void purchasing() {
    }

    public void verifyOrder(String merchantTransactionId, String signature, String purchaseData, Callback callback) {
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
                    body.put("inapp_data_signature", signature);
                    body.put("merchant_transaction_id", merchantTransactionId);
                    body.put("inapp_purchase_data", new JSONObject(purchaseData));
                    param.put("data", body);

                    String url = formatUrl("verify_google");
                    String data = postRequest(url, param.toString());
                    return data != null && new JSONObject(data).optBoolean("data", false);
                } catch (Exception e) {
                    ILog.e(TAG, "verify order failed:" + e.getMessage());
                }
                return false;
            }
        }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                ILog.e(TAG, "verify order result:" + aBoolean);
                if (callback != null && aBoolean != null) {
                    callback.onResult(aBoolean);
                }
            }

            @Override
            public void onError(Throwable e) {
                ILog.e(TAG, "verify order failed:" + e.getMessage());
                if (callback != null) {
                    callback.onResult(false);
                }
            }
        });
    }

    public void receiptGoods(String merchantTransactionId, Callback callback) {
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

                    String url = formatUrl("consume_google");
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

    public void unShippedGoodsOrders(MissedOrderCallback callback) {
        String payUserId = LocalStorage.Instance.decodeString("_pay_user_id_", Util.roleId());
        new SimpleRxJust<>("").map(new Func<String, List<MissedOrder>>() {
            @Override
            public List<MissedOrder> call(String value) throws Exception {
                try {
                    String url = formatUrl("unconsume_google");
                    String uuid = payUserId;
                    String data = getRequest(url + "?app_id=" + appId + "&uuid=" + uuid);

                    List<MissedOrder> missedOrders = new ArrayList<>();
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
                                MissedOrder item = new MissedOrder(itemAppId, itemUUID, rpDate, country, merchantTransactionId, orderId, environment, orderStatus, productId, name, usd, payload);
                                missedOrders.add(item);
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
        }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<List<MissedOrder>>() {
            @Override
            public void onSuccess(List<MissedOrder> missedOrders) {
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
        LocalStorage.Instance.encodeString("merchant_transaction_" + sku, merchantTransactionId);
    }

    public String decodeMerchantTransactionId(String sku) {
        if (TextUtils.isEmpty(sku)) return "";
        return LocalStorage.Instance.decodeString("merchant_transaction_" + sku, "");
    }

    private String getRequest(String url) {
        try {
            OkHttpClient okHttpClient = HttpUtil.getInstance().getOkHttpClient();
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
            OkHttpClient okHttpClient = HttpUtil.getInstance().getOkHttpClient();
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
}
