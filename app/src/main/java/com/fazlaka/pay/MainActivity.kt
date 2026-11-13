package com.fazlaka.pay

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // التعديل بعد المراجعة: طلب إذن البلوتوث للرازي
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(PrinterBridge(), "AndroidPrint")
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class PrinterBridge {
        @JavascriptInterface
        fun printBill(service: String, ref: String, name: String, num: String, due: String, fee: String, total: String, dateStr: String) {
            Thread {
                try {
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                    val device = adapter?.bondedDevices?.firstOrNull()
                        ?: throw Exception("مفيش طابعة متوصلة - اعمل Pair للرازي من بلوتوث الموبايل الأول")
                    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                    adapter.cancelDiscovery()
                    socket.connect()
                    val out: OutputStream = socket.outputStream
                    fun line(s: String = "") { out.write((s + "\n").toByteArray(charset("UTF-8"))) }
                    out.write(byteArrayOf(0x1B, 0x61, 0x01))
                    line("فزلكا - كل يوم معاك")
                    line("====================")
                    line("خدمة: $service")
                    if (ref.isNotEmpty()) line("مرجعي: $ref")
                    if (name.isNotEmpty()) line("العميل: $name")
                    line("رقم: $num")
                    line("صافي: $due")
                    line("خدمة: $fee")
                    line("الاجمالي: $total")
                    line("التاريخ: $dateStr")
                    line("====================")
                    line("شكرا لثقتك")
                    out.write(byteArrayOf(0x1D, 0x56, 0x41, 0x00))
                    out.flush()
                    Thread.sleep(500)
                    socket.close()
                    runOnUiThread { Toast.makeText(this@MainActivity, "تم الطباعة على الرازي ✅", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "الطباعة فشلت: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            }.start()
        }
    }
}
