package com.feuerwehr.checklist.webview

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class ChecklistWebViewClient(
    private val appUrl: String,
    private val onPageLoaded: () -> Unit,
    private val onError: (Int) -> Unit,
    private val onConnectionStatusChanged: (Boolean) -> Unit
) : WebViewClient() {
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        // Page loading started
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageLoaded()
        
        // Check if we're connected by trying to inject a connectivity check
        view?.evaluateJavascript("""
            (function() {
                return navigator.onLine;
            })();
        """) { result ->
            val isOnline = result == "true"
            onConnectionStatusChanged(isOnline)
        }
    }
    
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        
        // Only handle errors for the main frame
        if (request?.isForMainFrame == true) {
            error?.errorCode?.let { onError(it) }
            onConnectionStatusChanged(false)
        }
    }
    
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString()
        
        return when {
            url?.startsWith(appUrl) == true -> {
                // Allow navigation within the app
                false
            }
            url?.startsWith("tel:") == true || url?.startsWith("mailto:") == true -> {
                // Handle tel: and mailto: links externally
                true
            }
            else -> {
                // Block external navigation or handle as needed
                true
            }
        }
    }
}