package com.ivy.ads;

import android.content.Context;
import android.text.TextUtils;

import com.facebook.ads.BidderTokenProvider;
import com.facebook.biddingkit.bidders.BidResponseCallback;
import com.facebook.biddingkit.bidders.BidderWithNotifier;
import com.facebook.biddingkit.facebook.bidder.FacebookBidder;
import com.facebook.biddingkit.gen.BidWithNotification;
import com.facebook.biddingkit.gen.FacebookAdBidFormat;

final class FacebookBK3BidRequester {
    private FacebookBK3BidRequester() {
    }

    static void requestFacebookBid(final Context context,
                                   final String appId,
                                   final String placementId,
                                   final FacebookAdBidFormat format,
                                   final String platformId,
                                   final double bidFloor,
                                   final int timeoutMs,
                                   final boolean testMode,
                                   final boolean coppa,
                                   final boolean limitedDataUse,
                                   final BidCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = BidderTokenProvider.getBidderToken(context);
                    if (TextUtils.isEmpty(token)) {
                        callback.onFailure(-1, "Empty Facebook bidder token");
                        return;
                    }

                    FacebookBidder.Builder builder = new FacebookBidder.Builder(appId, placementId, format, token)
                            .setTestMode(testMode)
                            .setCoppa(coppa)
                            .setLimitedDataUse(limitedDataUse);

//                    if (!TextUtils.isEmpty(platformId)) {
//                        builder.setPlatformId(platformId);
//                    }
                    if (timeoutMs > 0) {
                        builder.setTimeoutMs(timeoutMs);
                    }
                    if (bidFloor > 0) {
                        builder.setBidFloor(bidFloor);
                    }

                    BidderWithNotifier bidder = builder.buildWithNotifier();
                    bidder.retrieveBidWithNotificationCompleted(new BidResponseCallback() {
                        @Override
                        public void handleBidResponse(BidWithNotification bidResponse) {
                            if (bidResponse == null) {
                                callback.onFailure(9, "Facebook BK3 returned empty bid");
                                return;
                            }
                            callback.onSuccess(bidResponse);
                        }

                        @Override
                        public void handleBidResponseFailure(String errorMessage) {
                            callback.onFailure(9, errorMessage);
                        }
                    });
                } catch (Throwable e) {
                    callback.onFailure(-2, "Facebook BK3 request failed: " + e.getMessage());
                }
            }
        }).start();
    }

    interface BidCallback {
        void onSuccess(BidWithNotification bid);

        void onFailure(int code, String message);
    }
}
