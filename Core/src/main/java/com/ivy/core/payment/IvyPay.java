package com.ivy.core.payment;

import android.app.Dialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import com.ivy.base.ActivityUtil;
import com.ivy.base.ILog;
import com.ivy.base.billing.IIPurchase;
import com.ivy.base.billing.IIPurchaseResult;
import com.ivy.base.billing.IPurchaseResult;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.core.util.LoadingDialog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IvyPay implements IIPurchase {

    public static final String TAG = "pay";

    private IIPurchase purchaseImpl;
    private IIPurchase enjoyPayPurchaseImpl;
    private IPurchaseResult callback;

//    private boolean isPurchasing = false;
    private Dialog loadingDialog;
//
//    private BottomSheetDialog platformSelectDialog = null;
//    private IIPurchase activePurchaseImpl = null;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private long last_pay_time = 0L;

    @Override
    public void setup(final String appId, final String data, final String enjoy_pay_data, final boolean debug, final IIPurchaseResult callback) {
        this.callback = callback;
        executor.execute(() -> {
            try {
                if (!TextUtils.isEmpty(data)) {
                    ILog.d(TAG, "start instantiate google billing impl");
                    Class<?> cls = Class.forName("com.ivy.google.billing.GoogleBillingPurchase");
                    Object instance = cls.getDeclaredConstructor().newInstance();
                    if (instance instanceof IIPurchase) {
                        purchaseImpl = (IIPurchase) instance;
                        mainHandler.post(() -> {
                            if (purchaseImpl != null) {
                                purchaseImpl.setup(appId, data, null, debug, callback);
                            } else {
                                ILog.w(TAG, "google billing impl setup failed");
                            }
                        });
                    }
                } else {
                    ILog.w(TAG, "instantiate google billing impl failed: no config data");
                }
            } catch (Exception e) {
                ILog.e(TAG, "instantiate google billing impl failed:" + e.getMessage());
            }
            try {
                if (!TextUtils.isEmpty(enjoy_pay_data)) {
                    ILog.d(TAG, "start instantiate enjoy pay impl");
                    Class<?> cls = Class.forName("com.ivy.enjoypay.EnjoyPayPurchase");
                    Object instance = cls.getDeclaredConstructor().newInstance();
                    if (instance instanceof IIPurchase) {
                        enjoyPayPurchaseImpl = (IIPurchase) instance;
                        mainHandler.post(() -> {
                            if (enjoyPayPurchaseImpl != null) {
                                enjoyPayPurchaseImpl.setup(appId, null, enjoy_pay_data, debug, callback);
                            } else {
                                ILog.w(TAG, "enjoy pay impl setup failed");
                            }
                        });
                    }
                } else {
                    ILog.w(TAG, "instantiate enjoy pay impl failed: no config data");
                }
            } catch (Exception e) {
                ILog.e(TAG, "instantiate enjoy pay impl failed:" + e.getMessage());
            }
        });
    }

    @Override
    public int randomBillId() {
        return purchaseImpl != null ? purchaseImpl.randomBillId() : -1;
    }

    @Override
    public void shippingGoods(String merchantTransactionId) {
        if (merchantTransactionId.startsWith("ENJOY")) {
            if (enjoyPayPurchaseImpl != null)
                enjoyPayPurchaseImpl.shippingGoods(merchantTransactionId);
            return;
        }
        if (purchaseImpl != null) {
            purchaseImpl.shippingGoods(merchantTransactionId);
        }
    }

    @Override
    public boolean isPaymentInitialized() {
        return purchaseImpl != null && purchaseImpl.isPaymentInitialized();
    }

    @Override
    public void pay(int id, String payload, String clientInfo) {
        if (SystemClock.elapsedRealtime() - last_pay_time < 5000) {
            TestManager.getInstance().addEvent("Payment", "当前正在付费，无法开始新的付费任务", EventState.ERROR, "billId=" + id);
            return;
        }
        last_pay_time = SystemClock.elapsedRealtime();
        try {
            if (ActivityUtil.getInstance().getActivity() != null) {
                try {
                    if (loadingDialog != null) {
                        if (loadingDialog.isShowing()) loadingDialog.dismiss();
                        loadingDialog = null;
                    }
                } catch (Throwable ignored) {
                }
                loadingDialog = new LoadingDialog(ActivityUtil.getInstance().getActivity());
                loadingDialog.show();
            }

            if (purchaseImpl != null) {
                TestManager.getInstance().addEvent("Payment", "开始付费任务", EventState.NORMAL, "billId=" + id);
                purchaseImpl.pay(id, payload, clientInfo);
            } else {
                TestManager.getInstance().addEvent("Payment", "付费平台未初始化", EventState.ERROR, "billId=" + id);
                if (callback != null) {
                    callback.payResult(id, IPurchaseResult.PAY_FAILED, payload, null, null);
                }
            }
        } catch (Exception e) {
            TestManager.getInstance().addEvent("Payment", "付费出错", EventState.ERROR, e.getMessage());
            endPay();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (enjoyPayPurchaseImpl != null) {
            enjoyPayPurchaseImpl.onActivityResult(requestCode, resultCode, data);
        }
    }

//    private LinearLayout buildPlatformSelectDialogContent(Context context, String title, View.OnClickListener listener, String... items) {
//        LinearLayout root = new LinearLayout(context);
//        root.setOrientation(LinearLayout.VERTICAL);
//        root.setPadding((int) Util.dp2Px(context, 24),
//                (int) Util.dp2Px(context, 16),
//                (int) Util.dp2Px(context, 24),
//                (int) Util.dp2Px(context, 24));
//        root.setBackgroundColor(Color.WHITE);
//
//        if (!TextUtils.isEmpty(title)) {
//            TextView titleView = new TextView(context);
//            titleView.setTextSize(20);
//            titleView.setTextColor(Color.BLACK);
//            titleView.setGravity(Gravity.CENTER);
//            titleView.setText(title);
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) Util.dp2Px(context, 40));
//            root.addView(titleView, params);
//        }
//
//        if (items != null && items.length > 0) {
//            for (String s : items) {
//                TextView textView = new TextView(context);
//                textView.setTextSize(16);
//                textView.setTextColor(Color.BLACK);
//                textView.setGravity(Gravity.CENTER_VERTICAL);
//                textView.setText(s);
//                textView.setTag(s);
//                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) Util.dp2Px(context, 36));
//                root.addView(textView, params);
//                if (listener != null) textView.setOnClickListener(listener);
//            }
//        }
//        return root;
//    }

//    private void showPayChannelDialog(Callback callback) {
//        if (platformSelectDialog != null) platformSelectDialog.dismiss();
//        LinearLayout root = buildPlatformSelectDialogContent(ActivityUtil.getInstance().getActivity(), "Select payment method", new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String tag = v.getTag().toString();
//                if (tag.equalsIgnoreCase("Google Billing")) {
//                    activePurchaseImpl = purchaseImpl;
//                } else if (tag.equalsIgnoreCase("Enjoy Pay")) {
//                    activePurchaseImpl = enjoyPayPurchaseImpl;
//                }
//                if (platformSelectDialog != null) platformSelectDialog.dismiss();
//                if (callback != null) callback.onResult();
//            }
//        }, "Google Billing", "Enjoy Pay");
//
//        platformSelectDialog = new BottomSheetDialog(ActivityUtil.getInstance().getActivity());
//        platformSelectDialog.setContentView(root);
//        platformSelectDialog.show();
//
//    }

//    private void startPayTask(int id, String payload, String clientInfo) {
//
//    }

    public void endPay() {
        try {
            if (loadingDialog != null) {
                loadingDialog.dismiss();
                loadingDialog = null;
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getGoodsInfo(int id) {
        if (purchaseImpl != null) {
            return purchaseImpl.getGoodsInfo(id);
        } else {
            return "{}";
        }
    }

    @Override
    public void queryPurchase(int id) {
        if (purchaseImpl != null) {
            purchaseImpl.queryPurchase(id);
        }
    }

    @Override
    public boolean isEnjoyPayInitialized() {
        return enjoyPayPurchaseImpl != null && enjoyPayPurchaseImpl.isPaymentInitialized();
    }

    @Override
    public void enjoyPay(int id, String payload, String clientInfo) {
        if (SystemClock.elapsedRealtime() - last_pay_time < 5000) {
            TestManager.getInstance().addEvent("Payment", "当前正在付费，无法开始新的付费任务--enjoypay", EventState.ERROR, "billId=" + id);
            return;
        }
        last_pay_time = SystemClock.elapsedRealtime();
        try {
            if (ActivityUtil.getInstance().getActivity() != null) {
                try {
                    if (loadingDialog != null) {
                        if (loadingDialog.isShowing()) loadingDialog.dismiss();
                        loadingDialog = null;
                    }
                } catch (Throwable ignored) {
                }
                loadingDialog = new LoadingDialog(ActivityUtil.getInstance().getActivity());
                loadingDialog.show();
            }

            if (enjoyPayPurchaseImpl != null) {
                TestManager.getInstance().addEvent("Payment", "开始付费任务--enjoypay", EventState.NORMAL, "billId=" + id);
                enjoyPayPurchaseImpl.pay(id, payload, clientInfo);
            } else {
                TestManager.getInstance().addEvent("Payment", "付费平台未初始化--enjoypay", EventState.ERROR, "billId=" + id);
                if (callback != null) {
                    callback.payResult(id, IPurchaseResult.PAY_FAILED, payload, null, null);
                }
            }
        } catch (Exception e) {
            TestManager.getInstance().addEvent("Payment", "付费出错--enjoypay", EventState.ERROR, e.getMessage());
            endPay();
        }
    }

    @Override
    public String getEnjoyPayGoodsInfo(int id) {
        if (enjoyPayPurchaseImpl != null) {
            return enjoyPayPurchaseImpl.getGoodsInfo(id);
        } else {
            return "{}";
        }
    }

    @Override
    public void queryEnjoyPurchase(int id) {
        if (enjoyPayPurchaseImpl != null) {
            enjoyPayPurchaseImpl.queryPurchase(id);
        }
    }
}
