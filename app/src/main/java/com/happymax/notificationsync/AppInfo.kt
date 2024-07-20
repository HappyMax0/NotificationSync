package com.happymax.notificationsync

import android.graphics.drawable.Drawable

data class AppInfo(val appName:String, val packageName:String, val icon: Drawable?, var enable:Boolean)
