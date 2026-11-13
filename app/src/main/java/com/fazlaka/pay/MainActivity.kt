package com.fazlaka.pay

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
    override fun onBackPressed() { if (webView.canGoBack()) webView.goBack() else super.onBackPressed() }
    inner class PrinterBridge {
        private fun findPrinter(adapter: BluetoothAdapter): BluetoothDevice? {
            val paired = adapter.bondedDevices?.toList() ?: emptyList()
            if (paired.isEmpty()) return null
            return paired.firstOrNull {
                val n = it.name ?: ""
                n.contains("Razi", true) || n.contains("Printer", true) || n.contains("POS", true) || n.contains("58", true) || n.contains("MPT", true)
            } ?: paired[0]
        }
        private fun doPrint(service: String, invoiceNo: String, num: String, phone: String, due: String, fee: String, total: String, dateStr: String, onDone: (Boolean, String) -> Unit) {
            Thread {
                try {
                    val adapter = BluetoothAdapter.getDefaultAdapter() ?: throw Exception("مفيش بلوتوث")
                    if (!adapter.isEnabled) throw Exception("البلوتوث مقفول")
                    val device = findPrinter(adapter) ?: throw Exception("مفيش طابعة معمولها Pair")
                    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    val socket: BluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                    adapter.cancelDiscovery(); socket.connect()
                    val out: OutputStream = socket.outputStream
                    fun line(s: String = "") { out.write(s.toByteArray(charset("windows-1256"))); out.write(byteArrayOf(0x0A)) }
                    out.write(byteArrayOf(0x1B, 0x40)); out.write(byteArrayOf(0x1B, 0x74, 0x1D)); out.write(byteArrayOf(0x1B, 0x61, 0x01))
                    line("فزلكا - كل يوم معاك"); line("====================")
                    out.write(byteArrayOf(0x1B, 0x61, 0x00))
                    line("فاتورة رقم: $invoiceNo")
                    line("خدمة: $service")
                    if (num.isNotEmpty()) line("رقم العداد / الحساب: $num")
                    if (phone.isNotEmpty()) line("رقم التليفون: $phone")
                    line("المبلغ: $due")
                    line("رسوم الخدمة: $fee")
                    line("الاجمالي: $total")
                    line("الوقت والتاريخ: $dateStr")
                    out.write(byteArrayOf(0x1B, 0x61, 0x01)); line("===================="); line("شكرا لثقتك"); line(""); line("")
                    out.write(byteArrayOf(0x1D, 0x56, 0x41, 0x00)); out.flush(); Thread.sleep(800); socket.close()
                    onDone(true, "تم")
                } catch (e: Exception) { onDone(false, e.message ?: "فشل") }
            }.start()
        }
        @JavascriptInterface
        fun printBill(service: String, invoiceNo: String, num: String, phone: String, due: String, fee: String, total: String, dateStr: String) {
            doPrint(service, invoiceNo, num, phone, due, fee, total, dateStr) { ok, msg ->
                runOnUiThread { Toast.makeText(this@MainActivity, if (ok) "تم الطباعة على الرازي ✅" else "الطباعة فشلت: $msg - دوس اطبع تاني", Toast.LENGTH_LONG).show() }
            }
        }
        @JavascriptInterface
        fun printTest() {
            doPrint("تجربة", "-", "", "", "0", "0", "0", "") { ok, msg ->
                runOnUiThread { Toast.makeText(this@MainActivity, if (ok) "الورقة طلعت فعلا ✅" else "فشلت: $msg", Toast.LENGTH_LONG).show() }
            }
        }
    }
}
