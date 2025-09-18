package com.feuerwehr.checklist

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.feuerwehr.checklist.databinding.ActivitySplashBinding
import com.feuerwehr.checklist.utils.NetworkUtils

class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding
    private val splashTimeout: Long = 2000 // 2 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize app and check network
        initializeApp()
    }
    
    private fun initializeApp() {
        binding.splashStatusText.text = getString(R.string.initializing)
        
        Handler(Looper.getMainLooper()).postDelayed({
            // Check network connectivity
            binding.splashStatusText.text = getString(R.string.checking_connection)
            
            Handler(Looper.getMainLooper()).postDelayed({
                // Move to main activity
                binding.splashStatusText.text = getString(R.string.loading_app)
                
                Handler(Looper.getMainLooper()).postDelayed({
                    startMainActivity()
                }, 500)
                
            }, 500)
            
        }, splashTimeout)
    }
    
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}