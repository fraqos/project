package com.dhyra.browser

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.view.*
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import android.content.ClipData
import android.content.ClipboardManager
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("dhyra", MODE_PRIVATE) }
    private lateinit var base: LinearLayout
    private lateinit var address: EditText
    private lateinit var web: WebView
    private lateinit var status: TextView
    private val tabs = mutableListOf<String>()
    private var current = 0
    private var privateMode = false
    private val btc = "bc1qrdlrhldck8pz6mtcaths86htz3g3hjyhsa44q5"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(229, 240, 250)
        window.navigationBarColor = Color.rgb(229, 240, 250)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        tabs.add(prefs.getString("last_url", "https://www.google.com") ?: "https://www.google.com")
        buildBrowser()
        load(tabs[0])
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Float = 14f) = TextView(this).apply {
        text = s; textSize = size; setTextColor(Color.rgb(30, 48, 66)); gravity = Gravity.CENTER
    }
    private fun button(s: String, action: () -> Unit) = Button(this).apply {
        text = s; textSize = 12f; isAllCaps = false
        setTextColor(Color.rgb(33, 62, 87))
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(239, 247, 253)))
        setOnClickListener { action() }
    }
    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(8), dp(12), dp(8))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.argb(238, 255, 255, 255))
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), Color.rgb(216, 232, 244))
        }
    }
    private fun buildBrowser() {
        base = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(6))
            setBackgroundColor(Color.rgb(232, 242, 250))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(text("DHYRA", 22f), LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(button("Tabs ${tabs.size}") { showTabs() })
        header.addView(button("⋮") { showMenu() })
        base.addView(header)

        val search = panel()
        address = EditText(this).apply {
            hint = "Search or enter website"; singleLine = true; textSize = 15f
            setTextColor(Color.rgb(25, 43, 60)); setHintTextColor(Color.GRAY)
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setBackgroundColor(Color.TRANSPARENT)
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                    navigate(text.toString()); true
                } else false
            }
        }
        search.addView(address)
        base.addView(search, LinearLayout.LayoutParams(-1, dp(54)))
        status = text("Browse Freely. Explore More. Support Innovation.", 11f)
        base.addView(status, LinearLayout.LayoutParams(-1, dp(32)))

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.useWideViewPort = true
        web.settings.loadWithOverviewMode = true
        web.settings.javaScriptCanOpenWindowsAutomatically = false
        web.settings.setSupportMultipleWindows(false)
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                status.text = "Loading…"
                if (url != null) address.setText(url)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                status.text = url ?: "Ready"
                if (url != null && !privateMode) {
                    tabs[current] = url
                    prefs.edit().putString("last_url", url).apply()
                    addHistory(url)
                }
            }
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler, error: android.net.http.SslError) {
                handler.cancel()
            }
        }
        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val req = DownloadManager.Request(Uri.parse(url))
                req.setMimeType(mimeType)
                req.addRequestHeader("User-Agent", userAgent)
                req.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                req.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType))
                (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                toast("Download started")
            } catch (_: Exception) { toast("Unable to start download") }
        }
        base.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = panel()
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        row.addView(button("‹") { if (web.canGoBack()) web.goBack() })
        row.addView(button("›") { if (web.canGoForward()) web.goForward() })
        row.addView(button("↻") { web.reload() })
        row.addView(button("⌂") { showHome() })
        row.addView(button("＋") { newTab() })
        row.addView(button("☆") { bookmark() })
        nav.addView(row)
        base.addView(nav)
        setContentView(base)
    }
    private fun navigate(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val url = if (value.contains(" ") || (!value.contains(".") && !value.startsWith("http")))
            "https://www.google.com/search?q=" + Uri.encode(value)
        else if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
        web.loadUrl(url)
    }
    private fun load(url: String) { address.setText(url); web.loadUrl(url) }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun addHistory(url: String) {
        val a = JSONArray(prefs.getString("history", "[]"))
        a.put(JSONObject().put("url", url).put("time", System.currentTimeMillis()))
        while (a.length() > 300) a.remove(0)
        prefs.edit().putString("history", a.toString()).apply()
    }
    private fun bookmark() {
        val a = JSONArray(prefs.getString("bookmarks", "[]"))
        val url = web.url ?: return
        for (i in 0 until a.length()) if (a.optString(i) == url) { toast("Already bookmarked"); return }
        a.put(url); prefs.edit().putString("bookmarks", a.toString()).apply(); toast("Bookmark saved")
    }
    private fun showHome() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(24), dp(24), dp(24), dp(24)); setBackgroundColor(Color.rgb(232,242,250)) }
        box.addView(text("DHYRA BROWSER", 26f))
        box.addView(text("Browse Freely. Explore More.", 14f))
        val input = EditText(this).apply { hint = "Search the web"; singleLine = true }
        box.addView(input, LinearLayout.LayoutParams(-1, dp(56)))
        box.addView(button("Search") { val q=input.text.toString(); buildBrowser(); navigate(q) })
        box.addView(button("Bookmarks") { showList("Bookmarks", "bookmarks") })
        box.addView(button("History") { showList("History", "history") })
        box.addView(button("Support DHYRA") { showDonate() })
        box.addView(button("Back to browser") { buildBrowser(); load(tabs[current]) })
        setContentView(box)
    }
    private fun showTabs() {
        val choices = tabs.mapIndexed { i, u -> "${i+1}. $u" }.toMutableList()
        choices.add("＋ New tab")
        AlertDialog.Builder(this).setTitle("Tabs (${tabs.size})").setItems(choices.toTypedArray()) { _, which ->
            if (which == tabs.size) newTab() else { current = which; buildBrowser(); load(tabs[current]) }
        }.setNegativeButton("Close current") { _, _ ->
            if (tabs.size > 1) { tabs.removeAt(current); current = (current - 1).coerceAtLeast(0); buildBrowser(); load(tabs[current]) }
            else toast("At least one tab must remain")
        }.show()
    }
    private fun newTab() {
        tabs.add("https://www.google.com"); current = tabs.lastIndex
        buildBrowser(); load(tabs[current])
    }
    private fun showList(title: String, key: String) {
        val a = JSONArray(prefs.getString(key, "[]"))
        val items = (0 until a.length()).map { i ->
            if (key == "history") a.optJSONObject(i)?.optString("url") ?: "" else a.optString(i)
        }.filter { it.isNotBlank() }.distinct().reversed()
        AlertDialog.Builder(this).setTitle(title).setItems(items.toTypedArray()) { _, which ->
            buildBrowser(); navigate(items[which])
        }.setPositiveButton("Clear") { _, _ -> prefs.edit().remove(key).apply(); toast("$title cleared") }
            .setNegativeButton("Close", null).show()
    }
    private fun showDonate() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(18), dp(24), dp(12)) }
        layout.addView(text("SUPPORT DHYRA BROWSER", 20f))
        layout.addView(text("Your voluntary support helps development.", 14f))
        val wallet = TextView(this).apply { text = btc; textSize = 13f; setTextIsSelectable(true); setPadding(0, dp(18), 0, dp(18)) }
        layout.addView(wallet)
        AlertDialog.Builder(this).setTitle("Bitcoin Donation").setView(layout)
            .setPositiveButton("Copy address") { _, _ ->
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("BTC address", btc))
                toast("Wallet address copied")
            }
            .setNeutralButton("Open wallet") { _, _ ->
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("bitcoin:$btc"))) } catch (_: Exception) { toast("No Bitcoin wallet app found") }
            }.setNegativeButton("Close", null).show()
    }
    private fun showMenu() {
        val items = arrayOf("Home", "New tab", "Tabs", "Bookmarks", "History", "Find in page", "Share page", "Desktop mode", "Private mode", "Support / Donate", "Clear browsing data")
        AlertDialog.Builder(this).setTitle("DHYRA Menu").setItems(items) { _, i ->
            when (i) {
                0 -> showHome()
                1 -> newTab()
                2 -> showTabs()
                3 -> showList("Bookmarks", "bookmarks")
                4 -> showList("History", "history")
                5 -> { val e = EditText(this); AlertDialog.Builder(this).setTitle("Find in page").setView(e).setPositiveButton("Find") { _, _ -> web.findAllAsync(e.text.toString()) }.setNegativeButton("Cancel", null).show() }
                6 -> { val u=web.url ?: return@setItems; startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,u), "Share page")) }
                7 -> { web.settings.userAgentString = if (web.settings.userAgentString.contains("Mobile")) web.settings.userAgentString.replace("Mobile", "X11") else web.settings.userAgentString + " Desktop"; web.reload() }
                8 -> { privateMode = !privateMode; if (privateMode) { web.clearHistory(); web.clearCache(true); toast("Private mode enabled for this session") } else toast("Private mode disabled") }
                9 -> showDonate()
                10 -> AlertDialog.Builder(this).setMessage("Clear history and cached files?").setPositiveButton("Clear") { _, _ -> prefs.edit().remove("history").apply(); web.clearHistory(); web.clearCache(true); web.clearFormData(); toast("Browsing data cleared") }.setNegativeButton("Cancel", null).show()
            }
        }.show()
    }
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
    override fun onDestroy() { if (!privateMode && tabs.isNotEmpty()) prefs.edit().putString("last_url", web.url ?: tabs[current]).apply(); super.onDestroy() }
}
