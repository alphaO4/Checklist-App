package com.feuerwehr.checklistpackage com.feuerwehr.checklist



import android.os.Bundleimport android.os.Bundle

import androidx.activity.ComponentActivityimport androidx.activity.ComponentActivity

import androidx.activity.compose.setContentimport androidx.activity.compose.setContent

import androidx.compose.foundation.layout.fillMaxSizeimport androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material3.MaterialThemeimport androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Surfaceimport androidx.compose.material3.Surface

import androidx.compose.ui.Modifierimport androidx.compose.ui.Modifier

import com.feuerwehr.checklist.ui.navigation.AppNavigationimport com.feuerwehr.checklist.ui.navigation.AppNavigation

import com.feuerwehr.checklist.ui.theme.ChecklistThemeimport com.feuerwehr.checklist.ui.theme.ChecklistTheme

import dagger.hilt.android.AndroidEntryPointimport dagger.hilt.android.AndroidEntryPoint



@AndroidEntryPoint@AndroidEntryPoint

class MainActivity : ComponentActivity() {class MainActivity : ComponentActivity() {

        

    override fun onCreate(savedInstanceState: Bundle?) {    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)        super.onCreate(savedInstanceState)

                

        setContent {        setContent {

            ChecklistTheme {            ChecklistTheme {

                Surface(                Surface(

                    modifier = Modifier.fillMaxSize(),                    modifier = Modifier.fillMaxSize(),

                    color = MaterialTheme.colorScheme.background                    color = MaterialTheme.colorScheme.background

                ) {                ) {

                    AppNavigation()                    AppNavigation()

                }                }

            }            }

        }        }

    }    }

}}
            )
            webChromeClient = ChecklistWebChromeClient(
                onProgressChanged = { progress -> updateProgress(progress) }
            )
            
            // Enable WebView debugging in debug builds for Android Studio
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                
                // Enable modern web features
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                
                // User agent
                userAgentString = userAgentString + " FeuerwehrChecklistApp/1.0"
                
                // Security settings
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(
                R.color.feuerwehr_red,
                R.color.feuerwehr_red_light,
                R.color.feuerwehr_red_dark
            )
            setOnRefreshListener {
                refreshApp()
            }
        }
    }
    
    private fun setupOfflineManager() {
        offlineManager = OfflineManager(this)
        
        binding.offlineFab.setOnClickListener {
            toggleOfflineMode()
        }
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val url = intent.data?.toString()
                if (url?.startsWith(APP_URL) == true) {
                    binding.webView.loadUrl(url)
                }
            }
        }
    }
    
    private fun loadApp() {
        Log.d(TAG, "Loading app...")
        showLoading()
        updateConnectionStatus(false)
        
        // Check network connectivity
        if (NetworkUtils.isNetworkAvailable(this)) {
            Log.d(TAG, "Network available, loading: $APP_URL")
            binding.webView.loadUrl(APP_URL)
        } else {
            Log.d(TAG, "No network available, switching to offline mode")
            handleOfflineScenario()
        }
    }
    
    private fun refreshApp() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            isOfflineMode = false
            binding.webView.reload()
        } else {
            handleOfflineScenario()
        }
        
        // Stop refresh animation after delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            binding.swipeRefreshLayout.isRefreshing = false
        }, 1000)
    }
    
    private fun handleOfflineScenario() {
        lifecycleScope.launch {
            if (offlineManager.hasCachedContent()) {
                showOfflineContent()
            } else {
                showNoConnectionError()
            }
        }
    }
    
    private fun showOfflineContent() {
        isOfflineMode = true
        updateConnectionStatus(false)
        binding.offlineFab.visibility = View.VISIBLE
        
        // Load offline version or cached content
        offlineManager.loadOfflineContent(binding.webView)
        hideLoading()
    }
    
    private fun showNoConnectionError() {
        hideLoading()
        Toast.makeText(
            this,
            getString(R.string.error_network),
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun toggleOfflineMode() {
        if (isOfflineMode && NetworkUtils.isNetworkAvailable(this)) {
            // Go online
            isOfflineMode = false
            binding.offlineFab.visibility = View.GONE
            loadApp()
        }
    }
    
    private fun handleWebViewError(errorCode: Int) {
        Log.w(TAG, "WebView error occurred: $errorCode")
        when (errorCode) {
            WebViewClient.ERROR_HOST_LOOKUP,
            WebViewClient.ERROR_CONNECT,
            WebViewClient.ERROR_TIMEOUT -> {
                Log.d(TAG, "Network error, switching to offline mode")
                handleOfflineScenario()
            }
            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> {
                Log.e(TAG, "SSL handshake failed")
                Toast.makeText(
                    this,
                    getString(R.string.error_ssl),
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                Log.e(TAG, "Unknown WebView error: $errorCode")
                Toast.makeText(
                    this,
                    getString(R.string.error_loading),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        hideLoading()
    }
    
    private fun updateConnectionStatus(online: Boolean) {
        binding.connectionStatusLayout.visibility = View.VISIBLE
        
        val statusRes = if (online) {
            R.drawable.status_indicator_online
        } else {
            R.drawable.status_indicator_offline
        }
        
        val statusText = if (online) {
            getString(R.string.connected)
        } else {
            getString(R.string.offline)
        }
        
        binding.statusIndicator.setBackgroundResource(statusRes)
        binding.statusText.text = statusText
        
        // Hide status after delay when online
        if (online) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                binding.connectionStatusLayout.visibility = View.GONE
            }, 3000)
        }
    }
    
    private fun updateProgress(progress: Int) {
        if (progress < 100) {
            showLoading()
        } else {
            hideLoading()
        }
    }
    
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }
    
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshApp()
                true
            }
            R.id.action_settings -> {
                // TODO: Open settings
                true
            }
            R.id.action_about -> {
                // TODO: Show about dialog
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}