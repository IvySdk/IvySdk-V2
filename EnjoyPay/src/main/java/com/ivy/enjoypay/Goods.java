package com.ivy.enjoypay;

import com.ivy.base.billing.AbsGoods;

import org.json.JSONException;
import org.json.JSONObject;

public class Goods extends AbsGoods<Item> {

    private Item item;

    public Goods decode(int id, JSONObject data) {
        AbsGoods<Item> decoded = decodeConfig(id, data);
        if (decoded instanceof Goods) {
            return (Goods) decoded;
        }
        return null;
    }

    @Override
    public void update(Item item, UpdateCallback<Item> callback) {
        this.item = item;
        super.update(item, callback);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("pay_id", id);
            json.put("id", sku);
            json.put("type", type);
            json.put("price", price);
            json.put("price_amount", priceAmountMicros);
            json.put("original_price", price);
            json.put("original_price_amount", originalPriceAmountMicros);
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

    public void updatePriceAmount(long p){
        this.priceAmountMicros = p;
    }

    public void updateOriginalPriceAmount(long p){
        this.originalPriceAmountMicros = p;
    }

    @Override
    public double getPriceAmount() {
        if (priceAmountMicros == 0L) return usd;
        return priceAmountMicros / 100.0;
    }

    @Override
    public double getOriginalPriceAmount() {
        if (originalPriceAmountMicros == 0L) return usd;
        return originalPriceAmountMicros / 100.0;
    }
}
