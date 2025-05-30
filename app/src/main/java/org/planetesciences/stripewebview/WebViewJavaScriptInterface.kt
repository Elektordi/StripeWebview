package org.planetesciences.stripewebview

import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Toast
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode

private const val LOG_TAG = "JavaScriptInterface"

class WebViewJavaScriptInterface(private var activity: MainActivity) {
    var terminal: Terminal? = null
    var printer: Printer? = null

    val scanner = activity.registerForActivityResult(ScanQRCode(), ::scannerCallbackHandler)
    var scanner_callback_js_function: String? = null

    @JavascriptInterface
    fun makeToast(message: String, lengthLong: Boolean) {
        Log.d(LOG_TAG, "makeToast called")
        Toast.makeText(activity, message, if (lengthLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun initStripe(location: String, token_js_function: String): Boolean {
        Log.d(LOG_TAG, "initStripe called")
        activity.runOnUiThread {
            activity.binding.settings.visibility = View.GONE
        }
        if(location == "" || token_js_function == "") return false
        if(terminal != null && terminal!!.isReady()) return false
        terminal = Terminal(activity, location, token_js_function)
        return terminal!!.init()
    }

    @JavascriptInterface
    fun stopStripe(): Boolean {
        Log.d(LOG_TAG, "stopStripe called")
        if(terminal == null) return false
        return terminal!!.stop()
    }

    @JavascriptInterface
    fun isStripeReady(): Boolean {
        Log.d(LOG_TAG, "isStripeReady called")
        if(terminal == null) return false
        return terminal!!.isReady()
    }

    @JavascriptInterface
    fun pushToken(token: String) {
        Log.d(LOG_TAG, "pushToken called")
        if(terminal == null) return
        terminal!!.pushToken(token)
    }

    @JavascriptInterface
    fun startPayment(amount: Int, uid: String, callback_js_function: String) {
        Log.d(LOG_TAG, "startPayment called")
        if(amount == 0 || uid == "" || callback_js_function == "") return
        if(terminal == null) return
        terminal!!.startPayment(amount.toLong(), uid, callback_js_function)
    }

    @JavascriptInterface
    fun cancelPayment() {
        Log.d(LOG_TAG, "cancelPayment called")
        if(terminal == null) return
        terminal!!.cancelPayment()
    }

    @JavascriptInterface
    fun scanQrCode(callback_js_function: String) {
        Log.d(LOG_TAG, "scanQrCode called")
        if(callback_js_function == "") return
        scanner_callback_js_function = callback_js_function
        scanner.launch(null)
    }

    private fun scannerCallbackHandler(result: QRResult) {
        Log.d(LOG_TAG, "scannerCallbackHandler: %s".format(result.toString()))
        if(result is QRResult.QRSuccess) {
            val data = result.content.rawValue.replace("'", "\'")
            activity.runOnUiThread {
                activity.binding.webview.evaluateJavascript("$scanner_callback_js_function('$data')") {}
            }
        }
    }

    @JavascriptInterface
    fun initPrinter(target: String) {
        Log.d(LOG_TAG, "initPrinter called")
        if(target == "") return
        printer = Printer(activity, target)
        printer!!.init()
    }

    @JavascriptInterface
    fun printTicket(bytes: ByteArray) {
        Log.d(LOG_TAG, "printTicket called")
        if(printer == null) return
        printer!!.print(bytes)
    }
}