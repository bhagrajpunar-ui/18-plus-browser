package com.bhagraj.browser18

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var pageTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showAgeNotice()
        buildBrowser()
    }

    private fun showAgeNotice() {
        val prefs = getSharedPreferences("browser_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("age_confirmed", false)) return
        AlertDialog.Builder(this)
            .setTitle("18+ Browser")
            .setMessage("आगे बढ़ने के लिए पुष्टि करें कि आपकी उम्र 18 वर्ष या उससे अधिक है। वेबसाइटों की आयु-शर्तों और स्थानीय कानूनों का पालन करें।")
            .setCancelable(false)
            .setPositiveButton("मैं 18+ हूँ") { _, _ -> prefs.edit().putBoolean("age_confirmed", true).apply() }
            .setNegativeButton("बाहर जाएँ") { _, _ -> finish() }
            .show()
    }

    private fun buildBrowser() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(240, 253, 253))
            setPadding(12, 16, 12, 8)
        }
        pageTitle = TextView(this).apply {
            text = "18+"
            textSize = 30f
            setTextColor(Color.rgb(35, 45, 65))
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 12)
        }
        root.addView(pageTitle, LinearLayout.LayoutParams(-1, -2))
        val searchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        address = EditText(this).apply {
            hint = "Google पर खोजें या वेबसाइट लिखें"
            singleLine = true
            textSize = 15f
            setPadding(14, 4, 14, 4)
            setBackgroundColor(Color.WHITE)
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { openAddress(text.toString()); true } else false
            }
        }
        searchRow.addView(address, LinearLayout.LayoutParams(0, 54, 1f))
        searchRow.addView(makeButton("GO") { openAddress(address.text.toString()) })
        root.addView(searchRow)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        actions.addView(makeButton("Home") { web.loadUrl("https://www.google.com") })
        actions.addView(makeButton("Back") { if (web.canGoBack()) web.goBack() })
        actions.addView(makeButton("Next") { if (web.canGoForward()) web.goForward() })
        actions.addView(makeButton("Reload") { web.reload() })
        actions.addView(makeButton("Clear") { clearBrowsingData() })
        root.addView(actions)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.settings.javaScriptCanOpenWindowsAutomatically = false
        web.settings.setSupportMultipleWindows(false)
        web.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        if (android.os.Build.VERSION.SDK_INT >= 26) web.settings.safeBrowsingEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
            override fun onPageFinished(view: WebView?, url: String?) {
                address.setText(url ?: "")
                pageTitle.text = view?.title?.take(38)?.ifBlank { "18+" } ?: "18+"
            }
        }
        web.webChromeClient = WebChromeClient()
        web.setDownloadListener(DownloadListener { url, userAgent, _, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                    .setMimeType(mimeType)
                    .addRequestHeader("User-Agent", userAgent)
                    .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                    .setTitle("18+ Browser download")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(url).lastPathSegment ?: "download")
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "डाउनलोड शुरू हुआ", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this, "डाउनलोड शुरू नहीं हो सका", Toast.LENGTH_SHORT).show() }
        })
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        web.loadUrl("https://www.google.com")
    }

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(this).apply { text = label; textSize = 11f; isAllCaps = false; setOnClickListener { action() } }

    private fun openAddress(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val target = when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.contains(".") && !value.contains(" ") -> "https://$value"
            else -> "https://www.google.com/search?q=" + Uri.encode(value)
        }
        web.loadUrl(target)
    }

    private fun clearBrowsingData() {
        AlertDialog.Builder(this).setTitle("Browsing data साफ़ करें?")
            .setMessage("History, cache और cookies साफ़ होंगे। यह पूर्ण anonymity की गारंटी नहीं है।")
            .setNegativeButton("रद्द करें", null)
            .setPositiveButton("साफ़ करें") { _, _ ->
                web.clearHistory()
                web.clearCache(true)
                CookieManager.getInstance().removeAllCookies {
                    CookieManager.getInstance().flush()
                    Toast.makeText(this, "Browsing data साफ़ किया गया", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() { if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed() }

    override fun onDestroy() { if (::web.isInitialized) web.destroy(); super.onDestroy() }
}
