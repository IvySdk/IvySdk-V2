package com.ivy.google.billing;

import com.android.billingclient.api.ProductDetails;
import com.ivy.base.billing.AbsGoods;

import org.json.JSONException;
import org.json.JSONObject;

public class Goods extends AbsGoods<ProductDetails> {

    private String offerId;       // google billing中订阅型消费品校验id
    private String offerToken;    // google billing中订阅型消费品校验key
    private ProductDetails productDetails;

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getOfferToken() {
        return offerToken;
    }

    public void setOfferToken(String offerToken) {
        this.offerToken = offerToken;
    }

    public ProductDetails getProductDetails() {
        return productDetails;
    }

    public void setProductDetails(ProductDetails productDetails) {
        this.productDetails = productDetails;
    }

    // decode
    public Goods decode(int id, JSONObject data) {
        AbsGoods<ProductDetails> decoded = decodeConfig(id, data);
        if (decoded instanceof Goods) {
            return (Goods) decoded;
        }
        return null;
    }

    @Override
    public void update(ProductDetails t, UpdateCallback<ProductDetails> callback) {
        this.productDetails = t;
        super.update(t, callback);
    }


    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("pay_id", id);
            json.put("id", sku);
            json.put("type", type);
            json.put("price", price);
            json.put("price_amount", priceAmountMicros / 1000000.0f);
            json.put("original_price", originalPrice);
            json.put("original_price_amount", originalPriceAmountMicros / 1000000.0f);
            json.put("currency", currency);
            json.put("title", title);
            json.put("desc", desc);
            json.put("usd", usd);
            json.put("v25_count", v25Count);
            json.put("v25_event", v25Event == null ? "" : v25Event);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public double getPriceAmount() {
        if (priceAmountMicros == 0L) return usd;
        return priceAmountMicros / 1000000.0;
    }

    @Override
    public double getOriginalPriceAmount() {
        if (originalPriceAmountMicros == 0L) return usd;
        return originalPriceAmountMicros / 1000000.0;
    }

}
