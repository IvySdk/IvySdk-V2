package com.ivy.enjoypay;


public class PurchaseTask {

    public int id;
    public Goods goods;
    public String payload;
    public String merchantTransactionId;
    public String payUrl;
    public String order_id;

    public PurchaseTask(int id, Goods goods, String payload) {
        this.id = id;
        this.goods = goods;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "PurchaseTask{" +
                "id=" + id +
                ", goods=" + goods +
                ", payload='" + payload + '\'' +
                ", merchantTransactionId='" + merchantTransactionId + '\'' +
                ", payUrl='" + payUrl + '\'' +
                ", order_id='" + order_id + '\'' +
                '}';
    }
}
