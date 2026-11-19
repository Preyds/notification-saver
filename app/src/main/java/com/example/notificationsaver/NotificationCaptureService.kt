package com.example.notificationsaver

import android.app.Notification
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

data class SavedNotification(
    val app: String,
    val title: String,
    val text: String,
    val time: Long
)

class NotificationDb(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "notifications.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "app TEXT, title TEXT, text TEXT, time INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun add(n: SavedNotification) {
        val v = ContentValues().apply {
            put("app", n.app)
            put("title", n.title)
            put("text", n.text)
            put("time", n.time)
        }
        writableDatabase.insert("notes", null, v)
    }

    fun all(): List<SavedNotification> {
        val list = mutableListOf<SavedNotification>()
        readableDatabase.rawQuery(
            "SELECT app, title, text, time FROM notes ORDER BY time DESC", null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    SavedNotification(
                        c.getString(0), c.getString(1), c.getString(2), c.getLong(3)
                    )
                )
            }
        }
        return list
    }
}

class NotificationCaptureService : NotificationListenerService() {

    override fun onListenerConnected() {
        getSharedPreferences("status", MODE_PRIVATE).edit()
            .putLong("connected", System.currentTimeMillis()).apply()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (title.isEmpty() && text.isEmpty()) return

        NotificationDb(this).add(
            SavedNotification(sbn.packageName, title, text, sbn.postTime)
        )
    }
}
