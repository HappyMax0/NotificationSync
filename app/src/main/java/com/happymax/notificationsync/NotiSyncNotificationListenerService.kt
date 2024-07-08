package com.happymax.notificationsync

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.google.auth.oauth2.GoogleCredentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Arrays


class NotiSyncNotificationListenerService : NotificationListenerService() {

    private val MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private val SCOPES = arrayOf(MESSAGING_SCOPE)

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
        val senderID = ""
        if(!token.isNullOrEmpty()){
            Log.d(TAG, "send to $token")

            if(title != null)
                postAsync(token, title, body, this.baseContext)

//            val msgId = AtomicInteger()
//            val RMBuilder = RemoteMessage.Builder("$senderID@gcm.googleapis.com/fcm/send")
//            RMBuilder.apply {
//                //setMessageId(msgId.incrementAndGet().toString())
//                addData("to", token)
//                addData("title", title)
//                addData("body", body)
//            }
//
//            // Send a message to the device corresponding to the provided
//            // registration token.
//            val response = FirebaseMessaging.getInstance().send(RMBuilder.build())
//            Log.d(TAG, "result is $response")
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

    fun postAsync(token:String, title:String, body:String, context: Context) {
        val api_key = "AIzaSyASG_S20l-dnvFlRk5Wj378p0MFXcxT754"
        val sender = ""
        val client = OkHttpClient()

        val obj = JSONObject()
        //data
        val notification = JSONObject()
        notification.put("title", title)
        notification.put("body", body)
        //android
        val android = JSONObject()
        android.put("direct_boot_ok", false)

        val message = JSONObject()
        message.put("token", token)
        message.put("notification", notification)
        //message.put("android", android)

        obj.put("message", message)
//        obj.put("to", token)
//        obj.put("data", data)
        val json = obj.toString()

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

//        val requestBody: RequestBody = FormBody.Builder()
//            .add("to", token)
//            .add("title", title)
//            .add("body", body)
//            .build()
        val url = "https://fcm.googleapis.com/v1/projects/notificationsync-e95aa/messages:send"
        //"https://fcm.googleapis.com/fcm/send"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer  " + getAccessToken(context))
            //.addHeader("Sender", "id=" + sender)
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


        /*val call = client.newCall(request)
        try {
            val response: Response = call.execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { Log.d(TAG, it) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }*/
    }

    @Throws(IOException::class)
    private fun getAccessToken(context:Context): String {

        val internalStorageDir = context.getExternalFilesDir(null)
        val file = File(internalStorageDir, "notificationsync-e95aa-firebase-adminsdk-yuwd4-33db92faa1.json") // 替换为你的文件名
        val googleCredentials: GoogleCredentials = GoogleCredentials
            .fromStream(FileInputStream(file))
            .createScoped(MESSAGING_SCOPE)
        googleCredentials.refreshAccessToken()
        return googleCredentials.getAccessToken().getTokenValue()
    }


    companion object {
        private const val TAG = "NotiSyncNotificationListenerService"
    }
}