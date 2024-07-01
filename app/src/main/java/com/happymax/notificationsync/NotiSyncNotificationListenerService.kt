package com.happymax.notificationsync

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger


class NotiSyncNotificationListenerService : NotificationListenerService() {
    lateinit var sharedPreferences:SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)

    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val notifatication = sbn?.notification;
        val extras = notifatication?.extras;

        var extraImage = extras?.getString(Notification.EXTRA_PICTURE)
        val title = extras?.getString(Notification.EXTRA_TITLE, "")
        val body =
            extras?.getCharSequence(Notification.EXTRA_TEXT, "").toString()

        val token = sharedPreferences.getString("Token", "")
        /*how to get sender id:
        1.Sign in to the Firebase console.
        2.Choose your project.
        3.Click the settings icon (gear icon) in the left panel.
        4.Select project settings.
        5.In the Cloud Messaging tab you can find your Sender ID.
        */
        val senderID = "put your sender id here"
        if(!token.isNullOrEmpty()){
            Log.d(TAG, "send to $token")

            val msgId = AtomicInteger()
            val RMBuilder = RemoteMessage.Builder("$senderID@gcm.googleapis.com/fcm/send")
            RMBuilder.apply {
                //setMessageId(msgId.incrementAndGet().toString())
                addData("to", token)
                addData("title", title)
                addData("body", body)
            }

            // Send a message to the device corresponding to the provided
            // registration token.
            val response = FirebaseMessaging.getInstance().send(RMBuilder.build())
            Log.d(TAG, "result is $response")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onListenerDisconnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 通知侦听器断开连接 - 请求重新绑定
            requestRebind(ComponentName(this, NotificationListenerService::class.java))
        }
    }

    companion object {
        private const val TAG = "NotiSyncNotificationListenerService"
    }
}