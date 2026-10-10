package com.ivy.base.message;

public class FCMMessage {

    public interface IMessage {
        void onPushReach();
    }

    private static volatile IMessage messager;

    public static void setMessager(IMessage message) {
        messager = message;
    }

    public static void onPushReach() {
        try {
            if (messager != null) {
                messager.onPushReach();
            }
        } catch (Exception e) {
        }
    }

}
