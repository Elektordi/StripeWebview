package org.planetesciences.stripewebview

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode

import org.planetesciences.stripewebview.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var prefs: SharedPreferences

    val scanner = registerForActivityResult(ScanQRCode(), ::scannerCallbackHandler)

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.domStorageEnabled = true
        binding.webview.settings.loadWithOverviewMode = true
        binding.webview.settings.useWideViewPort = true
        binding.webview.addJavascriptInterface(WebViewJavaScriptInterface(this@MainActivity), "app")
        binding.webview.webChromeClient = WebChromeClient()
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
            scanner.launch(null)
        }

        binding.settings.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setMessage("Effacer url de démarrage ?")
                .setPositiveButton("Oui") { _, _ ->
                    prefs.edit().remove("start_url").apply()
                    finish()
                    startActivity(intent)
                }
                .setNegativeButton("Non", null)
                .show()
            true
        }
    }

    override fun onStart() {
        super.onStart()

        this.requestPermissions(arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        ), 1)
    }

    private fun scannerCallbackHandler(result: QRResult) {
        if(result is QRResult.QRSuccess) {
            val newUrl = result.content.rawValue
            AlertDialog.Builder(this)
                .setMessage("Définir \"%s\" comme url de démarrage ?".format(newUrl))
                .setPositiveButton("Oui") { _, _ ->
                    prefs.edit().putString("start_url", newUrl).apply()
                    finish()
                    startActivity(intent)
                }
                .setNegativeButton("Non", null)
                .show()

        } else if(result is QRResult.QRError) {
            Toast.makeText(this, result.exception.toString(), Toast.LENGTH_LONG).show()
        }
    }
}