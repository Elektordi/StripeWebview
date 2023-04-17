package org.planetesciences.stripewebview

import android.webkit.JavascriptInterface
import android.widget.Toast

class WebViewJavaScriptInterface(private var context: MainActivity) {
    @JavascriptInterface
    fun makeToast(message: String?, lengthLong: Boolean) {
        Toast.makeText(context, message, if (lengthLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}