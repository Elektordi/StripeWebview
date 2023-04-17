package org.planetesciences.stripewebview

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import org.planetesciences.stripewebview.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.webview.settings.javaScriptEnabled = true
        binding.webview.addJavascriptInterface(WebViewJavaScriptInterface(), "app")
        val url = prefs.getString("start_url", null)
        if(url != null) {
            binding.webview.loadUrl(url)
        } else {
            val reader = assets.open("default_page.html").bufferedReader()
            val html = reader.readText()
            reader.close()
            binding.webview.loadDataWithBaseURL("https://default.localhost", html, "text/html", null, "about:blank")
        }

        binding.settings.setOnClickListener {
            val scanner = GmsBarcodeScanning.getClient(this)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val url = barcode.url?.url
                    if(url != null) {
                        AlertDialog.Builder(this)
                            .setMessage("Définir %s comme url de démarrage ?".format(url))
                            .setPositiveButton("Oui") { _, _ ->
                                prefs.edit().putString("start_url", url).commit()
                                finish();
                                startActivity(intent);
                            }
                            .setNegativeButton("Non", null)
                            .show()
                    }
                }
        }
    }

    inner class WebViewJavaScriptInterface {
        @JavascriptInterface
        fun makeToast(message: String?, lengthLong: Boolean) {
            Toast.makeText(this@MainActivity, message, if (lengthLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }
}