package com.happymax.notificationsync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.IOException
import java.net.URL


class NotiSyncFirebaseMessagingService() : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.from)

        // Check if message contains a notification payload.
        remoteMessage.data?.let { data ->
            val packName = data["packageName"]
            packName?.let{ Log.d(TAG, "packName: ${it}") }

            val appName = data["appName"]
            appName?.let{ Log.d(TAG, "appName: ${it}") }

            if(packName != null){
                val intent = Helper.GetApplicationWithPackageName(packName, this)
                if(intent != null){
                    startActivity(intent)
                }
            }
        }

        // Check if message contains a notification payload.
        val notification = remoteMessage.getNotification()
        if (notification != null) {
            if(notification.title != null && notification.body != null){
                val appMsg = AppMsg(null, null, notification.title!!, notification.body!!, notification.imageUrl.toString())
                sendNotification(appMsg)
            }
        }
    }

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
        val channelId = this.packageName
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(appMsg.title)
                .setContentText(appMsg.body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)

        if(appMsg.image != null){
            val bitmap = getBitmapFromUrl(appMsg.image.toString())
            notificationBuilder.setLargeIcon(bitmap)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(
            channelId,
            this.packageName,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    private fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            BitmapFactory.decodeStream(url.openConnection().getInputStream())
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        private const val TAG = "FCM"
    }
}