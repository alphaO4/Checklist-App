package com.feuerwehr.checklist.utils

import android.content.Context
import android.webkit.WebView
import androidx.work.*
import com.feuerwehr.checklist.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class OfflineManager(private val context: Context) {
    
    companion object {
        private const val CACHE_DIR = "offline_cache"
        private const val OFFLINE_HTML_FILE = "offline_app.html"
        private const val SYNC_WORK_NAME = "offline_sync"
    }
    
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    suspend fun hasCachedContent(): Boolean = withContext(Dispatchers.IO) {
        val offlineFile = File(cacheDir, OFFLINE_HTML_FILE)
        offlineFile.exists() && offlineFile.length() > 0
    }
    
    suspend fun cacheAppContent(url: String = BuildConfig.APP_URL) = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            
            connection.inputStream.use { input ->
                File(cacheDir, OFFLINE_HTML_FILE).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Also cache essential assets
            cacheEssentialAssets()
            
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    private suspend fun cacheEssentialAssets() = withContext(Dispatchers.IO) {
        val assetsToCache = listOf(
            "https://checklist.svoboda.click/config.js",
            "https://checklist.svoboda.click/web-api-adapter.js",
            "https://checklist.svoboda.click/js/renderer.js",
            "https://checklist.svoboda.click/styles/main.css"
        )
        
        assetsToCache.forEach { assetUrl ->
            try {
                val fileName = assetUrl.substringAfterLast('/')
                val connection = URL(assetUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 10000
                
                connection.inputStream.use { input ->
                    File(cacheDir, fileName).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // Ignore individual asset failures
                e.printStackTrace()
            }
        }
    }
    
    fun loadOfflineContent(webView: WebView) {
        val offlineFile = File(cacheDir, OFFLINE_HTML_FILE)
        if (offlineFile.exists()) {
            webView.loadUrl("file://${offlineFile.absolutePath}")
        } else {
            // Load a basic offline page
            loadBasicOfflinePage(webView)
        }
    }
    
    private fun loadBasicOfflinePage(webView: WebView) {
        val offlineHtml = """
            <!DOCTYPE html>
            <html lang="de">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Offline - Feuerwehr Checklist</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background-color: #f5f5f5;
                        text-align: center;
                    }
                    .container {
                        max-width: 600px;
                        margin: 50px auto;
                        background: white;
                        padding: 40px;
                        border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    .icon {
                        font-size: 64px;
                        margin-bottom: 20px;
                    }
                    h1 {
                        color: #DC143C;
                        margin-bottom: 10px;
                    }
                    p {
                        color: #666;
                        line-height: 1.5;
                        margin-bottom: 20px;
                    }
                    .retry-btn {
                        background-color: #DC143C;
                        color: white;
                        border: none;
                        padding: 12px 24px;
                        border-radius: 4px;
                        font-size: 16px;
                        cursor: pointer;
                        margin-top: 20px;
                    }
                    .retry-btn:hover {
                        background-color: #B71C1C;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">🚒</div>
                    <h1>Offline-Modus</h1>
                    <p>Die Feuerwehr Checklist App ist derzeit nicht verfügbar.</p>
                    <p>Stellen Sie sicher, dass Sie mit dem Internet verbunden sind, und versuchen Sie es erneut.</p>
                    <button class="retry-btn" onclick="window.location.reload()">
                        Erneut versuchen
                    </button>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, offlineHtml, "text/html", "UTF-8", null)
    }
    
    fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncWorkRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
            6, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
    
    fun cancelSyncWork() {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }
}

class OfflineSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val offlineManager = OfflineManager(applicationContext)
            offlineManager.cacheAppContent()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}