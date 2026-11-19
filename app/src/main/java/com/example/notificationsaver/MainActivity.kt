package com.example.notificationsaver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var listText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 96, 32, 32)
        }
        val button = Button(this).apply {
            text = "Grant notification access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, 16, 0, 16)
        }
        listText = TextView(this).apply { textSize = 16f }
        val scroll = ScrollView(this).apply { addView(listText) }

        root.addView(button)
        root.addView(status)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val fmt = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault())
        val t = getSharedPreferences("status", MODE_PRIVATE).getLong("connected", 0L)
        status.text = if (t == 0L) "Listener: NOT connected yet"
        else "Listener connected: ${fmt.format(Date(t))}"

        val items = NotificationDb(this).all()
        listText.text = if (items.isEmpty()) {
            "No notifications captured yet."
        } else {
            items.joinToString("\n\n") {
                "${fmt.format(Date(it.time))} · ${it.app}\n${it.title}\n${it.text}"
            }
        }
    }
}
