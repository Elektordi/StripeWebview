package org.planetesciences.stripewebview

import android.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.stripe.stripeterminal.external.callable.*
import com.stripe.stripeterminal.external.models.*
import com.stripe.stripeterminal.log.LogLevel

const val LOG_TAG = "Terminal"
const val SIMULATED = true

class Terminal(var activity: MainActivity, var location: String, var token_js_function: String) {

    private var search: Cancelable? = null
    private var connectedreader: Reader? = null
    private var cancelable: Cancelable? = null

    private val tokenProvider = object : ConnectionTokenProvider {
        override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
            activity.runOnUiThread {
                activity.binding.webview.evaluateJavascript("$token_js_function()") {
                    if (it != null) callback.onSuccess(it.trim('"'))
                    else callback.onFailure(ConnectionTokenException("Token is null"))
                }
            }
        }
    }

    private val listener = object : TerminalListener {
        override fun onUnexpectedReaderDisconnect(reader: Reader) {
            status("Terminal déconnecté !")
        }
    }

    private val reconnectlistener = object : BluetoothReaderReconnectionListener {
        override fun onReaderReconnectFailed(reader: Reader) {
            status("Échec de la reconnexion du terminal !")
        }

        override fun onReaderReconnectStarted(cancelReconnect: Cancelable) {
            status("Reconnexion du terminal en cours...")
        }

        override fun onReaderReconnectSucceeded() {
            status("Reconnexion du terminal réussie !")
        }

    }

    private val btlistener = object : BluetoothReaderListener {
        override fun onReportLowBatteryWarning() {
            status("Batterie du terminal faible.")
        }

        override fun onReportReaderEvent(event: ReaderEvent) {
            status("Terminal: Évènement %s".format(event.toString()))
        }

        override fun onRequestReaderInput(options: ReaderInputOptions) {
            status("Terminal: Insérez %s".format(options.toString()))
        }

        override fun onRequestReaderDisplayMessage(message: ReaderDisplayMessage) {
            status("Terminal: %s".format(message))
        }

        override fun onStartInstallingUpdate(update: ReaderSoftwareUpdate, cancelable: Cancelable?) {
            status("Mise à jour du terminal en cours, veuillez patienter.")
        }

        override fun onReportReaderSoftwareUpdateProgress(progress: Float) {
            status("Mise à jour du terminal en cours, %.0f%%.".format(progress*100))
        }

        override fun onFinishInstallingUpdate(update: ReaderSoftwareUpdate?, e: TerminalException?) {
            if(e==null) status("Mise à jour du terminal réussie !")
            else status("Mise à jour du terminal échouée: %s".format(e))
        }
    }

    fun status(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun getInstance() : com.stripe.stripeterminal.Terminal {
        if (!com.stripe.stripeterminal.Terminal.isInitialized()) {
            com.stripe.stripeterminal.Terminal.initTerminal(activity, LogLevel.VERBOSE, tokenProvider, listener)
        }
        return com.stripe.stripeterminal.Terminal.getInstance()
    }

    fun init() {
        try {
            val config = DiscoveryConfiguration(
                isSimulated = SIMULATED,
                discoveryMethod = DiscoveryMethod.BLUETOOTH_SCAN,
            )

            var dialog: AlertDialog? = null

            search = getInstance().discoverReaders(config, object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    var filteredReaders = readers.filter { r -> r.deviceType.deviceName == "bbpos_wisepad3"}

                    activity.runOnUiThread {
                        if(dialog != null) dialog!!.dismiss()
                        dialog = AlertDialog.Builder(activity)
                            .setTitle("Connexion terminal")
                            .setItems(filteredReaders.map { r -> r.serialNumber }.toTypedArray()) { _, which ->
                                connect(filteredReaders[which])
                            }
                            .setNegativeButton("Annuler") { _, _ ->
                                search!!.cancel(object: Callback {
                                    override fun onFailure(e: TerminalException) {
                                    }
                                    override fun onSuccess() {
                                    }
                                })
                            }
                            .show()
                    }
                }
            }, object : Callback {
                override fun onSuccess() {
                    Log.i(LOG_TAG, "Terminal search success.")
                }

                override fun onFailure(e: TerminalException) {
                    Log.e(LOG_TAG, "StripeTerminal discoverReaders Failure", e)
                }
            })
        } catch (e: Exception) {
            Log.e(LOG_TAG, "init exception", e)
            status("Error: %s".format(e.message))
        }
    }

    private fun connect(reader: Reader) {
        val connectionConfig = ConnectionConfiguration.BluetoothConnectionConfiguration(location, true, reconnectlistener)

        getInstance().connectBluetoothReader(
            reader,
            connectionConfig,
            btlistener,
            object : ReaderCallback {
                override fun onSuccess(reader: Reader) {
                    connectedreader = reader
                    status("Terminal connecté !")
                }

                override fun onFailure(e: TerminalException) {
                    Log.e(LOG_TAG, "connectBluetoothReader failure", e)
                    status("Connect failed: %s".format(e.message))
                }
            }
        )
    }
}