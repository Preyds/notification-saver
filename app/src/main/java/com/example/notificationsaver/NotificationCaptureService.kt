package com.example.notificationsaver

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

data class SavedNotification(
    val app: String,
    val title: String,
    val text: String,
    val time: Long
)

object NotificationStore {
    val items = mutableListOf<SavedNotification>()
}

class NotificationCaptureService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (title.isEmpty() && text.isEmpty()) return

        NotificationStore.items.add(
            0, SavedNotification(sbn.packageName, title, text, sbn.postTime)
        )
    }
}
