package com.happymax.notificationsync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationCompat
import com.google.auth.oauth2.GoogleCredentials
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.get
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Objects


class NotiSyncNotificationListenerService : NotificationListenerService() {

    private val MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private val SCOPES = arrayOf(MESSAGING_SCOPE)

    lateinit var sharedPreferences:SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

        val mChannel: NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = NotificationChannel(
                "Foreground",
                "前台服务",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            Objects.requireNonNull<NotificationManager>(
                getSystemService<NotificationManager>(
                    NotificationManager::class.java
                )
            ).createNotificationChannel(mChannel)
        }
        val foregroundNotice: Notification = NotificationCompat.Builder(this, "Foreground")
            .setContentTitle(getString(R.string.notification_running))
            .setContentText(getString(R.string.notification_forwarding))
            .build()
        startForeground(1, foregroundNotice)
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)

    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        var enabledPackages = mutableListOf<String>()
        val notifatication = sbn?.notification
        val extras = notifatication?.extras
        val packageName = sbn?.packageName
        var appName = ""
        if(packageName != null)
        {
            appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString()
        }

        val title = extras?.getString(Notification.EXTRA_TITLE, "")
        val body =
            extras?.getCharSequence(Notification.EXTRA_TEXT, "").toString()
        val image = extras?.getString(Notification.EXTRA_LARGE_ICON)

        val token = sharedPreferences.getString("Token", "")

        val json = sharedPreferences.getString("EnabledPackages", null)
        if(json != null){
            val type = object : TypeToken<List<String>>() {}.type
            if (json != null) {
                val gson = Gson()
                enabledPackages = gson.fromJson(json, type)

            }
        }

        /*how to get sender id:
        1.Sign in to the Firebase console.
        2.Choose your project.
        3.Click the settings icon (gear icon) in the left panel.
        4.Select project settings.
        5.In the Cloud Messaging tab you can find your Sender ID.
        */
        if(!token.isNullOrEmpty() && enabledPackages.contains(packageName)){
            Log.d(TAG, "send to $token")

            if(title != null) {
                val context = this.baseContext
                GlobalScope.launch(Dispatchers.IO) {
                    // 在这里执行耗时操作
                    PostToFCMServer(AppMsg(appName, packageName, title, body, image), token, context)
                    withContext(Dispatchers.Main) {
                        // 在这里更新 UI
                    }
                }
            }
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

    fun PostToFCMServer(appMsg: AppMsg, token:String, context: Context) {
        val client = OkHttpClient()

        val obj = JSONObject()
        //notification
        val notification = JSONObject()
        notification.put("title", "[${appMsg.appName}] ${appMsg.title}")
        notification.put("body", appMsg.body)
        if(appMsg.image != null){
            notification.put("imageUrl", appMsg.image)
        }
        //data
        val data = JSONObject()
        data.put("packageName", appMsg.packageName)
        data.put("appName", appMsg.appName)
        //android
        val android = JSONObject()
        android.put("direct_boot_ok", false)

        val message = JSONObject()
        message.put("token", token)
        message.put("notification", notification)
        message.put("data", data)
        //message.put("android", android)

        obj.put("message", message)
        val json = obj.toString()

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        //You can find myproject-ID in the General tab of your project settings in the Firebase console.
        val url = "https://fcm.googleapis.com/v1/projects/notificationsync-e95aa/messages:send"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer " + getAccessToken(context))
            .addHeader("Content-Type", "application/json; UTF-8")
            .build()

        val call = client.newCall(request)
        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { Log.d(TAG, it) }
                }
            }
        })
    }

    @Throws(IOException::class)
    private fun getAccessToken(context:Context): String {

        val internalStorageDir = context.getExternalFilesDir(null)
        val file = File(internalStorageDir, "notificationsync-e95aa-firebase-adminsdk-yuwd4-33db92faa1.json") // 替换为你的文件名
        val googleCredentials: GoogleCredentials = GoogleCredentials
            .fromStream(FileInputStream(file))
            .createScoped(SCOPES.toList())

        val token = googleCredentials.refreshAccessToken().getTokenValue()
        if(token != null){
            Log.d(TAG, "result is $token")
            return token
        }

        Log.d(TAG, "can not get token")
        return String()
    }


    companion object {
        private const val TAG = "Listener"
    }
}