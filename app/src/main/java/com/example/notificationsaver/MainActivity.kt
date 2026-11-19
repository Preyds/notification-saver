package com.example.notificationsaver

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val exportRequest = 1001

    private lateinit var status: TextView
    private lateinit var emptyText: TextView
    private lateinit var search: EditText
    private lateinit var adapter: NoteAdapter

    private var allItems: List<NoteRow> = emptyList()
    private var shown: List<NoteRow> = emptyList()
    private val nameCache = HashMap<String, String>()
    private val iconCache = HashMap<String, Drawable>()
    private val rowFmt = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun appName(pkg: String): String = nameCache.getOrPut(pkg) {
        try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun appIcon(pkg: String): Drawable = iconCache.getOrPut(pkg) {
        try {
            packageManager.getApplicationIcon(pkg)
        } catch (e: Exception) {
            packageManager.defaultActivityIcon
        }
    }

    private fun weighted() =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(16), dp(12), dp(8))
        }

        val accessBtn = Button(this).apply {
            text = "Grant access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        val appsBtn = Button(this).apply {
            text = "Choose apps"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, AppPickerActivity::class.java))
            }
        }
        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        topRow.addView(accessBtn, weighted())
        topRow.addView(appsBtn, weighted())

        status = TextView(this).apply {
            textSize = 13f
            setPadding(0, dp(8), 0, dp(4))
        }

        val label = TextView(this).apply {
            text = "Keep notifications for:"
            textSize = 13f
        }
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Retention.labels
        )
        spinner.setSelection(Retention.index(this))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (position != Retention.index(this@MainActivity)) {
                    Retention.setIndex(this@MainActivity, position)
                    refresh()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        search = EditText(this).apply {
            hint = "Search notifications or apps"
            setSingleLine(true)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                    applyFilter()
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        val exportBtn = Button(this).apply {
            text = "Export CSV"
            setOnClickListener { startExport() }
        }
        val clearBtn = Button(this).apply {
            text = "Clear all"
            setOnClickListener { confirmClear() }
        }
        val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actionRow.addView(exportBtn, weighted())
        actionRow.addView(clearBtn, weighted())

        emptyText = TextView(this).apply {
            textSize = 15f
            setPadding(0, dp(16), 0, dp(16))
        }

        adapter = NoteAdapter()
        val listView = ListView(this).apply {
            this.adapter = this@MainActivity.adapter
        }

        root.addView(topRow)
        root.addView(status)
        root.addView(label)
        root.addView(spinner)
        root.addView(search)
        root.addView(actionRow)
        root.addView(emptyText)
        root.addView(
            listView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        Retention.cleanup(this)
        val full = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault())
        val t = getSharedPreferences("status", MODE_PRIVATE).getLong("connected", 0L)
        status.text = if (t == 0L) "Listener: NOT connected yet"
        else "Listener connected: ${full.format(Date(t))}"

        allItems = NotificationDb(this).allRows()
        applyFilter()
    }

    private fun applyFilter() {
        val q = search.text.toString().trim().lowercase()
        shown = if (q.isEmpty()) allItems else allItems.filter {
            it.title.lowercase().contains(q) ||
                it.text.lowercase().contains(q) ||
                appName(it.app).lowercase().contains(q)
        }
        adapter.notifyDataSetChanged()
        emptyText.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        emptyText.text = if (allItems.isEmpty()) "No notifications captured yet."
        else "No matches."
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle("Clear all?")
            .setMessage("This deletes every saved notification.")
            .setPositiveButton("Delete") { _, _ ->
                NotificationDb(this).deleteAll()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startExport() {
        if (shown.isEmpty()) {
            Toast.makeText(this, "Nothing to export", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "notifications.csv")
        }
        startActivityForResult(intent, exportRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == exportRequest && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            contentResolver.openOutputStream(uri)?.use {
                it.write(buildCsv().toByteArray())
            }
            Toast.makeText(this, "Exported ${shown.size} notifications", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun csvCell(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    private fun buildCsv(): String {
        val f = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder("time,app,title,text\n")
        for (n in shown) {
            sb.append(csvCell(f.format(Date(n.time)))).append(',')
                .append(csvCell(appName(n.app))).append(',')
                .append(csvCell(n.title)).append(',')
                .append(csvCell(n.text)).append('\n')
        }
        return sb.toString()
    }

    inner class NoteAdapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = shown[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val n = shown[position]
            val ctx = this@MainActivity

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8), 0, dp(8))
            }

            val icon = ImageView(ctx).apply {
                setImageDrawable(appIcon(n.app))
            }
            row.addView(icon, LinearLayout.LayoutParams(dp(40), dp(40)))

            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(4), 0)
            }
            col.addView(TextView(ctx).apply {
                text = "${appName(n.app)} · ${rowFmt.format(Date(n.time))}"
                textSize = 12f
                setTextColor(0xFF757575.toInt())
            })
            if (n.title.isNotEmpty()) {
                col.addView(TextView(ctx).apply {
                    text = n.title
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                })
            }
            if (n.text.isNotEmpty()) {
                col.addView(TextView(ctx).apply {
                    text = n.text
                    textSize = 14f
                })
            }
            row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            val del = TextView(ctx).apply {
                text = "✕"
                textSize = 20f
                setPadding(dp(12), dp(4), dp(8), dp(4))
                setOnClickListener {
                    NotificationDb(ctx).deleteById(n.id)
                    refresh()
                }
            }
            row.addView(del)
            return row
        }
    }
}
