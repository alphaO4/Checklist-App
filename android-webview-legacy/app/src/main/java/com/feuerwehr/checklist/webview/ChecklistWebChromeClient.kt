package com.feuerwehr.checklist.webview

import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView

class ChecklistWebChromeClient(
    private val onProgressChanged: (Int) -> Unit
) : WebChromeClient() {
    
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }
    
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        // Update activity title if needed
    }
    
    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        // Handle JavaScript alerts if needed
        return super.onJsAlert(view, url, message, result)
    }
    
    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        // Handle JavaScript confirms if needed
        return super.onJsConfirm(view, url, message, result)
    }
}