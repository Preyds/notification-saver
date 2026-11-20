package com.example.notificationsaver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object AllowList {
    private const val PREFS = "settings"
    private const val KEY = "allowed_apps"

    fun get(context: Context): Set<String>? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, null)

    fun save(context: Context, set: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, set).apply()
    }

    fun isAllowed(context: Context, pkg: String): Boolean {
        val set = get(context) ?: return true
        return set.contains(pkg)
    }
}

class AppPickerActivity : Activity() {

    private val boxes = mutableListOf<Pair<String, CheckBox>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
        val saved = AllowList.get(this)

         val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 96, 32, 32)
            setBackgroundColor(ThemeManager.bg(this@AppPickerActivity))
                }
        }
        val title = TextView(this).apply {
            text = "Save notifications from:"
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val weight = { LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }

        val allBtn = Button(this).apply {
            text = "All"
            setOnClickListener { boxes.forEach { p -> p.second.isChecked = true } }
        }
        val noneBtn = Button(this).apply {
            text = "None"
            setOnClickListener { boxes.forEach { p -> p.second.isChecked = false } }
        }
        val saveBtn = Button(this).apply {
            text = "Save"
            setOnClickListener {
                val chosen = boxes.filter { p -> p.second.isChecked }
                    .map { p -> p.first }.toSet()
                AllowList.save(this@AppPickerActivity, chosen)
                finish()
            }
        }
        row.addView(allBtn, weight())
        row.addView(noneBtn, weight())
        row.addView(saveBtn, weight())

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for ((pkg, label) in apps) {
            val cb = CheckBox(this).apply {
                text = label
                isChecked = saved == null || saved.contains(pkg)
            }
            list.addView(cb)
            boxes.add(pkg to cb)
        }
        val scroll = ScrollView(this).apply { addView(list) }

        root.addView(title)
        root.addView(row)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)
    }
}
