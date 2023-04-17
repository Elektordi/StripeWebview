package org.planetesciences.stripewebview

import android.webkit.JavascriptInterface
import android.widget.Toast

class WebViewJavaScriptInterface(private var activity: MainActivity) {
    var terminal: Terminal? = null

    @JavascriptInterface
    fun makeToast(message: String?, lengthLong: Boolean) {
        Toast.makeText(activity, message, if (lengthLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun initStripe(location: String?, token_js_function: String?) {
        if(location == null || token_js_function == null) return
        terminal = Terminal(activity, location, token_js_function)
        terminal!!.init()
    }
}