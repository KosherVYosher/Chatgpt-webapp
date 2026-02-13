package com.kv.chatgpt

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

open class ChatGPTWebViewClient(private val context: Context) : WebViewClient() {

    companion object {
        private val INTERNAL_DOMAINS = listOf(
            "chat.openai.com",
            "chatgpt.com",
            "openai.com",
            "googleapis.com",
            "gstatic.com",
            "auth0.openai.com",
            "auth.openai.com",
            "accounts.google.com"
        )
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false

        // Check if URL should be handled internally
        if (shouldLoadInternally(url)) {
            return false // Load in WebView
        }

        // Open external URLs in browser
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    private fun shouldLoadInternally(url: String): Boolean {
        return INTERNAL_DOMAINS.any { domain ->
            url.contains(domain, ignoreCase = true)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        CookieManager.getInstance().flush()
    }
}
