package org.planetesciences.stripewebview

import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Toast
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode

class WebViewJavaScriptInterface(private var activity: MainActivity) {
    var terminal: Terminal? = null
    var printer: Printer? = null

    val scanner = activity.registerForActivityResult(ScanQRCode(), ::scannerCallbackHandler)
    var scanner_callback_js_function: String? = null

    @JavascriptInterface
    fun makeToast(message: String, lengthLong: Boolean) {
        Toast.makeText(activity, message, if (lengthLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun initStripe(location: String, token_js_function: String) {
        if(location == "" || token_js_function == "") return
        terminal = Terminal(activity, location, token_js_function)
        terminal!!.init()
        activity.runOnUiThread {
            activity.binding.settings.visibility = View.GONE
        }
    }

    @JavascriptInterface
    fun isStripeReady(): Boolean {
        if(terminal == null) return false
        return terminal!!.isReady()
    }

    @JavascriptInterface
    fun pushToken(token: String) {
        if(terminal == null) return
        terminal!!.pushToken(token)
    }

    @JavascriptInterface
    fun startPayment(amount: Int, uid: String, callback_js_function: String) {
        if(amount == 0 || uid == "" || callback_js_function == "") return
        if(terminal == null) return
        terminal!!.startPayment(amount.toLong(), uid, callback_js_function)
    }

    @JavascriptInterface
    fun cancelPayment() {
        if(terminal == null) return
        terminal!!.cancelPayment()
    }

    @JavascriptInterface
    fun scanQrCode(callback_js_function: String) {
        if(callback_js_function == "") return
        scanner_callback_js_function = callback_js_function
        scanner.launch(null)
    }

    private fun scannerCallbackHandler(result: QRResult) {
        if(result is QRResult.QRSuccess) {
            val data = result.content.rawValue.replace("'", "\'")
            activity.runOnUiThread {
                activity.binding.webview.evaluateJavascript("$scanner_callback_js_function('$data')") {}
            }
        }
    }

    @JavascriptInterface
    fun initPrinter(target: String) {
        if(target == "") return
        printer = Printer(activity, target)
        printer!!.init()
    }

    @JavascriptInterface
    fun printTicket(bytes: ByteArray) {
        if(printer == null) return
        printer!!.print(bytes)
    }
}