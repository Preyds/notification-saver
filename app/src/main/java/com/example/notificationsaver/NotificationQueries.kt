package com.example.notificationsaver

data class NoteRow(
    val id: Long,
    val app: String,
    val title: String,
    val text: String,
    val time: Long
)

fun NotificationDb.allRows(): List<NoteRow> {
    val list = mutableListOf<NoteRow>()
    readableDatabase.rawQuery(
        "SELECT id, app, title, text, time FROM notes ORDER BY time DESC", null
    ).use { c ->
        while (c.moveToNext()) {
            list.add(
                NoteRow(
                    c.getLong(0),
                    c.getString(1) ?: "",
                    c.getString(2) ?: "",
                    c.getString(3) ?: "",
                    c.getLong(4)
                )
            )
        }
    }
    return list
}

fun NotificationDb.deleteById(id: Long) {
    writableDatabase.delete("notes", "id = ?", arrayOf(id.toString()))
}

fun NotificationDb.deleteAll() {
    writableDatabase.delete("notes", null, null)
}
