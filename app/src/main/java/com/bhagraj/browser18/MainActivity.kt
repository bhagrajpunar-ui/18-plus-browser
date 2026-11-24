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
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONTokener

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
            setSingleLine(true)
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
        actions.addView(makeButton("Video") { downloadCurrentVideo() })
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

        // Download direct media files when a website exposes a normal downloadable URL.
        web.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            downloadMedia(url, userAgent, contentDisposition, mimeType)
        })

        // Long-press handling: use the tapped link when available; otherwise
        // offer the currently playing direct video URL from the page.
        web.setOnLongClickListener {
            val hit = web.hitTestResult
            val linkUrl = hit.extra
            if (hit.type == WebView.HitTestResult.SRC_ANCHOR_TYPE && !linkUrl.isNullOrBlank()) {
                AlertDialog.Builder(this)
                    .setItems(arrayOf("Download link")) { _, _ ->
                        downloadMedia(linkUrl, web.settings.userAgentString, null, null)
                    }
                    .show()
            } else {
                downloadCurrentVideo()
            }
            true
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        web.loadUrl("https://www.google.com")
    }

    // Aloha-style convenience: tap Video while a normal, directly downloadable video is playing.
    // This intentionally does not extract or bypass DRM, encrypted streams, HLS/DASH manifests, or blob URLs.
    private fun downloadCurrentVideo() {
        web.evaluateJavascript(
            "(function(){var v=Array.from(document.querySelectorAll('video')).find(x=>x.currentSrc||x.src);return JSON.stringify(v?{url:(v.currentSrc||v.src),mime:(v.currentType||v.type||'')} : null);})()"
        ) { result ->
            try {
                val decoded = JSONTokener(result ?: "null").nextValue()
                if (decoded == null || decoded.toString() == "null") {
                    Toast.makeText(this, "इस पेज पर वीडियो लिंक नहीं मिला", Toast.LENGTH_LONG).show()
                    return@evaluateJavascript
                }
                val video = decoded as org.json.JSONObject
                val url = video.optString("url")
                val mime = video.optString("mime").takeIf { it.isNotBlank() }
                val path = Uri.parse(url).lastPathSegment?.lowercase() ?: ""
                val directFile = listOf(".mp4", ".webm", ".mov", ".m4v", ".3gp", ".ogv").any { path.endsWith(it) }
                if (url.startsWith("blob:") || path.endsWith(".m3u8") || path.endsWith(".mpd") || !directFile) {
                    Toast.makeText(this, "यह वीडियो direct file नहीं है; साइट का अपना Download विकल्प इस्तेमाल करें", Toast.LENGTH_LONG).show()
                    return@evaluateJavascript
                }
                downloadMedia(url, web.settings.userAgentString, null, mime)
            } catch (_: Exception) {
                Toast.makeText(this, "वीडियो लिंक पढ़ा नहीं जा सका", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun downloadMedia(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        try {
            val uri = Uri.parse(url)
            if (uri.scheme != "https" && uri.scheme != "http") {
                Toast.makeText(this, "यह media link सीधे डाउनलोड नहीं हो सकता", Toast.LENGTH_LONG).show()
                return
            }
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "18plus_video" }
            val request = DownloadManager.Request(uri)
                .setTitle(fileName)
                .setDescription("18+ Browser media download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .addRequestHeader("User-Agent", userAgent ?: web.settings.userAgentString)
            val cookie = CookieManager.getInstance().getCookie(url)
            if (!cookie.isNullOrBlank()) request.addRequestHeader("Cookie", cookie)
            val referer = web.url
            if (!referer.isNullOrBlank()) request.addRequestHeader("Referer", referer)
            if (!mimeType.isNullOrBlank()) request.setMimeType(mimeType)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "डाउनलोड शुरू हुआ: $fileName", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "डाउनलोड शुरू नहीं हो सका", Toast.LENGTH_LONG).show()
        }
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
