package com.happymax.notificationsync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.media.RingtoneManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class NotiSyncFirebaseMessagingService() : FirebaseMessagingService() {
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.from)

        // Check if message contains a notification payload.
        val notification = remoteMessage.getNotification()
        if (notification != null) {
            if(notification.title != null && notification.body != null){
                val appMsg = AppMsg(notification.title!!, notification.body!!)
                sendNotification(appMsg)
            }
        }

        // 提取额外的数据
        remoteMessage.data?.let { data ->
            // 处理额外的数据
            val packName = data["packageName"]
            if(packName != null)
                Log.d(TAG, "packName: " + packName)
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
            this, 0 , intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = this.packageName
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
            this.packageName,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    private fun doStartApplicationWithPackageName(packagename: String?):Intent {
        val currentIntent = Intent(this, MainActivity::class.java)
        currentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        var packageinfo: PackageInfo? = null
        if(packagename == null){
            Log.d(TAG, "return currentIntent")
            return  currentIntent
        }

        try {
            packageinfo = packageManager.getPackageInfo(packagename, 0)
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
        }
        if (packageinfo == null) {
            Log.d(TAG, "return currentIntent")
            return currentIntent
        }

        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
        val resolveIntent = Intent(Intent.ACTION_MAIN, null)
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        resolveIntent.setPackage(packageinfo.packageName)

        // 通过getPackageManager()的queryIntentActivities方法遍历
        val resolveinfoList = packageManager
            .queryIntentActivities(resolveIntent, 0)
        val resolveinfo = resolveinfoList.iterator().next()
        if (resolveinfo != null) {
            // packagename = 参数packname
            val packageName = resolveinfo.activityInfo.packageName
            // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
            val className = resolveinfo.activityInfo.name
            // LAUNCHER Intent
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)

            // 设置ComponentName参数1:packagename参数2:MainActivity路径
            val cn = ComponentName(packageName, className)
            intent.setComponent(cn)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            //startActivity(intent)
            Log.d(TAG, "return targetIntent")
            return  intent
        }

        Log.d(TAG, "return currentIntent")
        return currentIntent
    }

    companion object {
        private const val TAG = "NotiSyncFirebaseMessagingService"
    }
}