package com.fazlaka.pay

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(PrintBridge(this), "AndroidPrint")
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/index.html")
    }

    class PrintBridge(val activity: AppCompatActivity) {
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        @JavascriptInterface
        fun printReceipt(text: String) {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                val device = adapter?.bondedDevices?.firstOrNull {
                    it.name.contains("Razi", true) ||
                    it.name.contains("رازي", true) ||
                    it.name.contains("58", true) ||
                    it.name.contains("Printer", true) ||
                    it.name.contains("POS", true)
                } ?: adapter?.bondedDevices?.firstOrNull()
                if (device == null) return
                val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val out: OutputStream = socket.outputStream
                fun send(bytes: ByteArray) { out.write(bytes); out.flush() }
                fun sendStr(s: String) { out.write(s.toByteArray(Charsets.UTF_8)); out.flush() }
                send(byteArrayOf(0x1B, 0x40))
                send(byteArrayOf(0x1B, 0x61, 0x01))
                send(byteArrayOf(0x1D, 0x21, 0x11))
                sendStr("Fazlaka\n")
                send(byteArrayOf(0x1D, 0x21, 0x00))
                sendStr("للدفع الالكتروني\n")
                sendStr("--------------------------------\n")
                send(byteArrayOf(0x1B, 0x61, 0x02))
                sendStr(text + "\n")
                send(byteArrayOf(0x1B, 0x61, 0x01))
                sendStr("--------------------------------\n")
                sendStr("فزلكا - كل يوم معاك\n")
                sendStr("\n\n\n")
                send(byteArrayOf(0x1D, 0x56, 0x41, 0x00))
                Thread.sleep(500)
                socket.close()
            } catch (e: Exception) { e.printStackTrace() }
        }

        @JavascriptInterface
        fun printBill(service: String, ref: String, name: String, num: String, due: String, fee: String, total: String, date: String) {
            val body = "الخدمة: $service\n" +
                       "الرقم المرجعي: $ref\n" +
                       "اسم العميل: $name\n" +
                       "رقم العداد: $num\n" +
                       "المبلغ: $due\n" +
                       "تكلفة الخدمة: $fee\n" +
                       "الاجمالي: $total\n" +
                       "التاريخ: $date\n"
            printReceipt(body)
        }
    }
}
