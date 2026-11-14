package com.fazlaka.pay

import android.os.Bundle
import android.webkit.WebView
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    lateinit var webView: WebView
    lateinit var executor: Executor
    var pendingEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        executor = ContextCompat.getMainExecutor(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(BiometricBridge(), "AndroidBiometric")
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class BiometricBridge {
        @JavascriptInterface
        fun authenticate() { pendingEnable = false; runOnUiThread { showBiometric() } }
        @JavascriptInterface
        fun authenticateForEnable() { pendingEnable = true; runOnUiThread { showBiometric() } }
    }

    fun showBiometric() {
        try {
            val mgr = BiometricManager.from(this)
            if (mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
                if (pendingEnable) webView.evaluateJavascript("onBiometricEnableResult(false)", null)
                else webView.evaluateJavascript("onBiometricResult(false)", null)
                return
            }
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fazlaka Pay")
                .setSubtitle("حط بصمتك للدخول")
                .setNegativeButtonText("إلغاء")
                .build()
            val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    runOnUiThread {
                        if (pendingEnable) webView.evaluateJavascript("onBiometricEnableResult(true)", null)
                        else webView.evaluateJavascript("onBiometricResult(true)", null)
                    }
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    runOnUiThread {
                        if (pendingEnable) webView.evaluateJavascript("onBiometricEnableResult(false)", null)
                        else webView.evaluateJavascript("onBiometricResult(false)", null)
                    }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    runOnUiThread {
                        if (pendingEnable) webView.evaluateJavascript("onBiometricEnableResult(false)", null)
                        else webView.evaluateJavascript("onBiometricResult(false)", null)
                    }
                }
            })
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            try {
                if (pendingEnable) webView.evaluateJavascript("onBiometricEnableResult(false)", null)
                else webView.evaluateJavascript("onBiometricResult(false)", null)
            } catch (_: Exception) {}
        }
    }
}
