package com.happymax.notificationsync

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.util.Log;
import android.widget.Toast
import androidx.compose.ui.res.stringResource

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

class NotiSyncFirebaseMessagingService() : FirebaseMessagingService() {
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.from)

        // Check if message contains a notification payload.
        val notification = remoteMessage.getNotification()
        if (notification != null) {
            /*Log.d(
                TAG, "Message Notification Body: " + remoteMessage.getNotification()!!
                    .body
            )*/

            if(notification.title != null && notification.body != null){
                val appMsg = AppMsg(notification.title!!, notification.body!!)
                sendNotification(appMsg)
            }

        }
    }
    // [END receive_message]

    // [START on_new_token]
    /**
     * There are two scenarios when onNewToken is called:
     * 1) When a new token is generated on initial app startup
     * 2) Whenever an existing token is changed
     * Under #2, there are three scenarios when the existing token is changed:
     * A) App is restored to a new device
     * B) User uninstalls/reinstalls the app
     * C) User clears app data
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }
    // [END on_new_token]


    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement this method to send token to your app server.
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val isInited = sharedPreferences.getBoolean("IsInited", false)
        val isClient = sharedPreferences.getBoolean("IsClient", false)

        if(isInited){
            if(isClient){
                val editor = sharedPreferences.edit()
                editor.putString("Token", token)
            }
        }

        Toast.makeText(this, R.string.toast_token_updated, Toast.LENGTH_LONG).show()
    }

    private fun sendNotification(appMsg: AppMsg) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = appMsg.title
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(appMsg.title)
                .setContentText(appMsg.msg)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(
            channelId,
            appMsg.title,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "NotiSyncFirebaseMessagingService"
    }
}