package org.planetesciences.stripewebview

import android.webkit.JavascriptInterface
import android.widget.Toast

class WebViewJavaScriptInterface(private var activity: MainActivity) {
    var terminal: Terminal? = null

    @JavascriptInterface
    fun makeToast(message: String, lengthLong: Boolean) {
        Toast.makeText(activity, message, if (lengthLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun initStripe(location: String, token_js_function: String) {
        if(location == "" || token_js_function == "") return
        terminal = Terminal(activity, location, token_js_function)
        terminal!!.init()
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

}