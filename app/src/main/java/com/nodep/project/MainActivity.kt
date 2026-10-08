package com.nodep.project

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var isBlockedMode = false
    private var blockedHost: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (isBlockedMode && url?.contains("blocked.html") == true) {
                    webView.evaluateJavascript(
                        "if (typeof window.onBlockInfoReceived === 'function') { window.onBlockInfoReceived('$blockedHost'); }",
                        null
                    )
                }
            }
        }

        webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        isBlockedMode = intent?.getBooleanExtra("EXTRA_IS_BLOCKED", false) ?: false
        blockedHost = intent?.getStringExtra("EXTRA_BLOCKED_HOST") ?: ""

        if (isBlockedMode) {
            webView.loadUrl("file:///android_asset/blocked.html")
        } else {
            webView.loadUrl("file:///android_asset/index.html")
        }
    }

    fun closeApp() {
        finishAffinity()
    }
}

class WebAppInterface(private val activity: MainActivity) {
    @JavascriptInterface
    fun isAccessibilityServiceEnabled(): Boolean {
        val am = activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return list.any { 
            it.resolveInfo.serviceInfo.packageName == activity.packageName &&
            it.resolveInfo.serviceInfo.name == CasinoBlockerService::class.java.name 
        }
    }

    @JavascriptInterface
    fun openAccessibilitySettings() {
        activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    @JavascriptInterface
    fun closeTab() {
        activity.runOnUiThread { activity.closeApp() }
    }
}
