package org.planetesciences.stripewebview

import android.R
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.planetesciences.stripewebview.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.webview.settings.javaScriptEnabled = true
        binding.webview.addJavascriptInterface(WebViewJavaScriptInterface(), "app")
        binding.webview.loadUrl("http://192.168.2.185/test_stripe_webview/")
    }

    inner class WebViewJavaScriptInterface {
        @JavascriptInterface
        fun makeToast(message: String?, lengthLong: Boolean) {
            Toast.makeText(this@MainActivity, message, if (lengthLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }
}