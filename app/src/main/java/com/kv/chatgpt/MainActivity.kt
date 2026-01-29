package com.kv.chatgpt

import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var deniedGeolocationUptime: Long = 0

    companion object {
        private const val CHATGPT_URL = "https://chat.openai.com"
        private const val GEOLOCATION_COOLDOWN_MS = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        webView = findViewById(R.id.webView)

        setupWebView()

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(CHATGPT_URL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    private fun setupWebView() {
        // Enable hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Disable over-scroll bounce animation (Median optimization)
        webView.overScrollMode = View.OVER_SCROLL_NEVER

        webView.webViewClient = object : ChatGPTWebViewClient(this@MainActivity) {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
            }

            // Geolocation loop prevention (Median optimization)
            // Prevents infinite permission callback loops that degrade performance
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                val elapsed = SystemClock.uptimeMillis() - deniedGeolocationUptime
                if (elapsed < GEOLOCATION_COOLDOWN_MS) {
                    // Recently denied - respond synchronously to prevent loop
                    callback?.invoke(origin, false, false)
                    return
                }
                // Deny geolocation (ChatGPT doesn't need it)
                deniedGeolocationUptime = SystemClock.uptimeMillis()
                callback?.invoke(origin, false, false)
            }
        }

        webView.settings.apply {
            // JavaScript and storage
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            // Standard HTTP caching (avoids serving stale pages after auth)
            cacheMode = WebSettings.LOAD_DEFAULT

            // Rendering optimizations
            @Suppress("DEPRECATION")
            setRenderPriority(WebSettings.RenderPriority.HIGH)

            // Font size fix - prevents CSS rem unit scaling issues (Median optimization)
            minimumFontSize = 1
            minimumLogicalFontSize = 1

            // Zoom controls
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Viewport
            useWideViewPort = true
            loadWithOverviewMode = true

            // Security
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            // Pretend to be a regular browser, not WebView
            userAgentString = userAgentString.replace("; wv", "")

            // Media and content
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true

            // Disable form data saving (security + slight perf gain)
            saveFormData = false
        }

        // Enable cookies for login persistence
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
