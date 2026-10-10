//package com.ivy.base.notification;
//
//import android.content.Intent;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//
//import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.messaging.RemoteMessage;
//
///**
// * Receives messages from your server (FCM). This service acts as a fallback: your server sends a data-message
// * when the server-side reminder time arrives. The app will then show a local notification (so behavior matches local alarms).
// *
// * NOTE: To test, send a data message with 'title' and 'body' fields to the device token.
// */
//public class ReminderFirebaseMessagingService extends FirebaseMessagingService {
//    private static final String TAG = "ReminderFMS";
//
//    @Override
//    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
//        Log.d(TAG, "onMessageReceived: " + remoteMessage.getData());
//        String title = remoteMessage.getData().getOrDefault("title", "学习提醒");
//        String body = remoteMessage.getData().getOrDefault("body", "回来继续你的目标吧！");
//
//        // Reuse local notification code path for unified behavior
//        Intent i = new Intent(this, NotificationReceiver.class);
//        i.putExtra("title", title);
//        i.putExtra("body", body);
//        sendBroadcast(i);
//    }
//
//    @Override
//    public void onNewToken(@NonNull String token) {
//        super.onNewToken(token);
//        Log.d(TAG, "FCM token: " + token);
//        // TODO: upload token to your server for later push
//    }
//}
