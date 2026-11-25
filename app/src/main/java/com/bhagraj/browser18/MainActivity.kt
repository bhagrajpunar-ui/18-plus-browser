package com.bhagraj.browser18

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 11, 17)
    private val panel = Color.rgb(24, 26, 37)
    private val purple = Color.rgb(123, 63, 245)
    private val green = Color.rgb(0, 150, 55)
    private val white = Color.WHITE
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var web: WebView
    private lateinit var address: EditText
    private var currentTab = "Home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ageConfirmed()) {
            AlertDialog.Builder(this)
                .setTitle("18+")
                .setMessage("यह ऐप केवल 18 वर्ष या उससे अधिक आयु के वयस्कों के लिए है। आगे बढ़कर आप पुष्टि करते हैं कि आप 18+ हैं। स्थानीय कानूनों और हर वेबसाइट की शर्तों का पालन करें।")
                .setCancelable(false)
                .setPositiveButton("मैं 18+ हूँ") { _, _ ->
                    getSharedPreferences("app", MODE_PRIVATE).edit().putBoolean("age_ok", true).apply()
                    showHome()
                }
                .setNegativeButton("बाहर जाएँ") { _, _ -> finish() }
                .show()
        }
        buildShell()
        showHome()
    }

    private fun ageConfirmed() = getSharedPreferences("app", MODE_PRIVATE).getBoolean("age_ok", false)

    private fun buildShell() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(17, 18, 26))
            setPadding(4, 8, 4, 8)
        }
        listOf("Home", "Browser", "Downloads", "Settings").forEach { tab ->
            val b = Button(this).apply {
                text = tab
                textSize = 11f
                isAllCaps = false
                setTextColor(white)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { when (tab) {
                    "Home" -> showHome()
                    "Browser" -> showBrowser()
                    "Downloads" -> showDownloads()
                    else -> showSettings()
                } }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, 48, 1f))
        }
        root.addView(nav)
        setContentView(root)
        web = WebView(this)
        configureWebView()
    }

    private fun resetContent() { content.removeAllViews() }

    private fun header(title: String, subtitle: String? = null) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 12)
            setBackgroundColor(bg)
        }
        val t = TextView(this).apply {
            text = title
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
        }
        box.addView(t)
        if (subtitle != null) box.addView(TextView(this).apply {
            text = subtitle; textSize = 13f; setTextColor(Color.LTGRAY); setPadding(0, 4, 0, 0)
        })
        content.addView(box)
    }

    private fun showHome() {
        currentTab = "Home"
        resetContent()
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 12, 14, 20) }
        scroll.addView(page)
        val brand = TextView(this).apply {
            text = "18+"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 2)
        }
        page.addView(brand)
        page.addView(TextView(this).apply {
            text = "BROWSER  •  ENTERTAINMENT"
            textSize = 11f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 16)
        })
        val search = EditText(this).apply {
            hint = "Search the web or enter a website"
            setSingleLine(true)
            textSize = 15f
            setTextColor(white)
            setHintTextColor(Color.LTGRAY)
            setPadding(16, 8, 16, 8)
            setBackgroundColor(panel)
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                    openInBrowser(text.toString()); true
                } else false
            }
        }
        page.addView(search, LinearLayout.LayoutParams(-1, 54))
        addSectionTitle(page, "Explore categories")
        val categories = listOf("Movies & Series", "Live TV", "Music & Podcasts", "Sports", "News & Learning", "Web Browser")
        categories.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { name ->
                val card = TextView(this).apply {
                    text = "▶\n$name"
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(white)
                    gravity = Gravity.CENTER
                    includeFontPadding = true
                    minHeight = dp(88)
                    setPadding(dp(10), dp(14), dp(10), dp(14))
                    setBackgroundColor(panel)
                    setOnClickListener {
                        if (name == "Web Browser") showBrowser() else showCategory(name)
                    }
                }
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(dp(4), dp(4), dp(4), dp(4))
                row.addView(card, lp)
            }
            page.addView(row)
        }
        addSectionTitle(page, "Your entertainment")
        val note = TextView(this).apply {
            text = "Add your own licensed video, TV and streaming links here. This starter app does not host or scrape third-party content."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(16, 16, 16, 16)
            setBackgroundColor(panel)
        }
        page.addView(note)
        val browser = actionButton("Open Browser", purple) { showBrowser() }
        page.addView(browser)
        val downloads = actionButton("My Downloads", green) { showDownloads() }
        page.addView(downloads)
        content.addView(scroll)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun addSectionTitle(parent: LinearLayout, text: String) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            setPadding(4, 22, 4, 8)
        })
    }

    private fun showCategory(name: String) {
        resetContent()
        header(name, "Licensed content and links you choose to open")
        val msg = TextView(this).apply {
            text = "इस सेक्शन में केवल आपके स्वामित्व वाले या अधिकृत वीडियो/स्ट्रीम लिंक जोड़ें। अभी यह शुरुआती कैटलॉग स्क्रीन है।"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(20, 16, 20, 16)
        }
        content.addView(msg)
        content.addView(actionButton("Open a website", purple) { showBrowser() })
    }

    private fun showBrowser() {
        currentTab = "Browser"
        resetContent()
        val top = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 10, 12, 8) }
        top.addView(TextView(this).apply {
            text = "18+  |  Browser"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 10)
        })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        address = EditText(this).apply {
            hint = "Search or enter URL"
            setSingleLine(true)
            textSize = 14f
            setTextColor(white)
            setHintTextColor(Color.LTGRAY)
            setPadding(12, 4, 12, 4)
            setBackgroundColor(panel)
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { openInBrowser(text.toString()); true } else false
            }
        }
        row.addView(address, LinearLayout.LayoutParams(0, 50, 1f))
        row.addView(actionButton("GO", purple) { openInBrowser(address.text.toString()) })
        top.addView(row)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf("Back", "Next", "Reload", "Video", "Clear").forEach { label ->
            controls.addView(actionButton(label, panel) { when(label) {
                "Back" -> if (web.canGoBack()) web.goBack()
                "Next" -> if (web.canGoForward()) web.goForward()
                "Reload" -> web.reload()
                "Video" -> downloadCurrentDirectVideo()
                "Clear" -> clearBrowsingData()
            } })
        }
        top.addView(controls)
        content.addView(top)
        if (web.parent != null) (web.parent as android.view.ViewGroup).removeView(web)
        content.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        if (web.url.isNullOrBlank()) web.loadUrl("https://www.google.com")
    }

    private fun configureWebView() {
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
                if (::address.isInitialized) address.setText(url ?: "")
            }
        }
        web.webChromeClient = WebChromeClient()
        web.setDownloadListener(DownloadListener { url, userAgent, disposition, mime, _ ->
            enqueueDownload(url, userAgent, disposition, mime)
        })
    }

    private fun openInBrowser(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        showBrowser()
        val target = when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.contains(".") && !value.contains(" ") -> "https://$value"
            else -> "https://www.google.com/search?q=" + Uri.encode(value)
        }
        address.setText(target)
        web.loadUrl(target)
    }

    // Only downloads ordinary direct media files. It does not extract protected streams or bypass DRM.
    private fun downloadCurrentDirectVideo() {
        web.evaluateJavascript("(function(){var v=Array.from(document.querySelectorAll('video')).find(x=>x.currentSrc||x.src);return JSON.stringify(v?{url:v.currentSrc||v.src}:null);})()") { result ->
            try {
                val decoded = org.json.JSONTokener(result ?: "null").nextValue()
                if (decoded == null || decoded.toString() == "null") {
                    toast("इस पेज पर direct video link नहीं मिला")
                    return@evaluateJavascript
                }
                val url = (decoded as org.json.JSONObject).optString("url")
                val path = Uri.parse(url).lastPathSegment?.lowercase() ?: ""
                val allowed = listOf(".mp4", ".webm", ".mov", ".m4v", ".3gp").any { path.endsWith(it) }
                if (!allowed || url.startsWith("blob:")) {
                    toast("यह direct downloadable file नहीं है। साइट का अधिकृत download विकल्प इस्तेमाल करें।")
                    return@evaluateJavascript
                }
                enqueueDownload(url, web.settings.userAgentString, null, "video/*")
            } catch (_: Exception) { toast("वीडियो लिंक नहीं पढ़ सके") }
        }
    }

    private fun enqueueDownload(url: String, userAgent: String?, disposition: String?, mime: String?) {
        try {
            val uri = Uri.parse(url)
            if (uri.scheme != "https" && uri.scheme != "http") { toast("यह लिंक डाउनलोड योग्य नहीं है"); return }
            val name = URLUtil.guessFileName(url, disposition, mime).replace(Regex("[^A-Za-z0-9._-]"), "_")
            val req = DownloadManager.Request(uri)
                .setTitle(name)
                .setDescription("18+ Browser download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .addRequestHeader("User-Agent", userAgent ?: web.settings.userAgentString)
            CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }?.let { req.addRequestHeader("Cookie", it) }
            web.url?.let { req.addRequestHeader("Referer", it) }
            if (!mime.isNullOrBlank()) req.setMimeType(mime)
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            toast("डाउनलोड शुरू: $name")
        } catch (_: Exception) { toast("डाउनलोड शुरू नहीं हो सका") }
    }

    private fun showDownloads() {
        currentTab = "Downloads"
        resetContent()
        header("Downloads", "Files are saved in your device Downloads folder")
        content.addView(TextView(this).apply {
            text = "Android के Downloads ऐप/फोल्डर में अपने डाउनलोड देखें। डाउनलोड की स्थिति notification panel में दिखाई देगी।"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(20, 18, 20, 18)
        })
        content.addView(actionButton("Open Downloads", green) {
            try {
                val intent = android.content.Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                startActivity(intent)
            } catch (_: Exception) { toast("Downloads ऐप उपलब्ध नहीं है") }
        })
        content.addView(actionButton("Back to Home", purple) { showHome() })
    }

    private fun showSettings() {
        currentTab = "Settings"
        resetContent()
        header("Settings", "Privacy and app preferences")
        content.addView(actionButton("Clear browsing data", panel) { clearBrowsingData() })
        content.addView(actionButton("Reset 18+ confirmation", panel) {
            getSharedPreferences("app", MODE_PRIVATE).edit().putBoolean("age_ok", false).apply()
            toast("अगली बार ऐप खोलने पर आयु-पुष्टि आएगी")
        })
        content.addView(actionButton("About 18+", panel) {
            AlertDialog.Builder(this).setTitle("18+").setMessage("यह ब्राउज़र और मनोरंजन ऐप का शुरुआती संस्करण है। केवल वैध और अधिकृत सामग्री का उपयोग करें।").setPositiveButton("OK", null).show()
        })
    }

    private fun clearBrowsingData() {
        AlertDialog.Builder(this).setTitle("Browsing data साफ़ करें?")
            .setMessage("History, cache और cookies साफ़ होंगे। इससे पूर्ण anonymity की गारंटी नहीं मिलती।")
            .setNegativeButton("रद्द करें", null)
            .setPositiveButton("साफ़ करें") { _, _ ->
                web.clearHistory()
                web.clearCache(true)
                CookieManager.getInstance().removeAllCookies {
                    CookieManager.getInstance().flush()
                    toast("Browsing data साफ़ किया गया")
                }
            }.show()
    }

    private fun actionButton(label: String, color: Int, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setTextColor(white)
            setBackgroundColor(color)
            setOnClickListener { action() }
        }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (::web.isInitialized && web.parent != null && web.canGoBack()) web.goBack()
        else showHome()
    }

    override fun onDestroy() {
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }
}
