package com.example.notificationsaver

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

object Reliability {

    fun isListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":").any {
            ComponentName.unflattenFromString(it)?.packageName == context.packageName
        }
    }

    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun summary(context: Context): String {
        val accessOk = isListenerEnabled(context)
        val batteryOk = isBatteryUnrestricted(context)
        val line = "Access: ${if (accessOk) "ON" else "OFF"} · " +
            "Battery: ${if (batteryOk) "unrestricted" else "restricted"}"
        return if (accessOk && batteryOk) line
        else "$line\n⚠ Tap \"Fix background issues\""
    }

    fun showFixDialog(activity: Activity) {
        val items = arrayOf(
            "Allow unrestricted battery use",
            "Open Autostart settings",
            "Open app info"
        )
        AlertDialog.Builder(activity)
            .setTitle("Fix background issues")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> requestBatteryExemption(activity)
                    1 -> openAutostart(activity)
                    else -> openAppInfo(activity)
                }
            }
            .show()
    }

    private fun requestBatteryExemption(activity: Activity) {
        try {
            activity.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${activity.packageName}")
                )
            )
        } catch (e: Exception) {
            openAppInfo(activity)
        }
    }

    private fun openAutostart(activity: Activity) {
        try {
            activity.startActivity(
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                )
            )
        } catch (e: Exception) {
            openAppInfo(activity)
        }
    }

    private fun openAppInfo(activity: Activity) {
        activity.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${activity.packageName}")
            )
        )
    }
}
