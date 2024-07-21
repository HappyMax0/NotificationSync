package com.happymax.notificationsync

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log

class Helper {
    companion object{
        private const val TAG = "Helper"
        public fun GetApplicationWithPackageName(packagename: String?, context: Context):Intent? {

            if(packagename == null){
                Log.d(TAG, "return null")
                return  null
            }

            // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
            var packageinfo: PackageInfo? = null

            try {
                packageinfo = context.packageManager.getPackageInfo(packagename, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            if (packageinfo == null) {
                Log.d(TAG, "return currentIntent")
                return null
            }

            // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
            val resolveIntent = Intent(Intent.ACTION_MAIN, null)
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            resolveIntent.setPackage(packageinfo.packageName)

            // 通过getPackageManager()的queryIntentActivities方法遍历
            val resolveinfoList = context.packageManager
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
            return null
        }

    }
}