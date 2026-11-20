package com.example.notificationsaver

import android.content.Context
import android.content.res.Configuration

object ThemeManager {
    const val MODE_SYSTEM = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    val modeLabels = listOf("System default", "Light", "Dark")

    private const val PREFS = "settings"
    private const val KEY = "theme_mode"

    fun getMode(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, MODE_SYSTEM)

    fun setMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY, mode).apply()
    }

    fun isDark(context: Context): Boolean = when (getMode(context)) {
        MODE_LIGHT -> false
        MODE_DARK -> true
        else -> {
            val uiMode = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
            uiMode == Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun bg(c: Context) = if (isDark(c)) 0xFF121212.toInt() else 0xFFFFFFFF.toInt()
    fun textPrimary(c: Context) = if (isDark(c)) 0xFFEEEEEE.toInt() else 0xFF1C1C1C.toInt()
    fun textSecondary(c: Context) = if (isDark(c)) 0xFFAAAAAA.toInt() else 0xFF757575.toInt()
    fun hint(c: Context) = if (isDark(c)) 0xFF777777.toInt() else 0xFFAAAAAA.toInt()
}
