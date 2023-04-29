package org.planetesciences.stripewebview

import android.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.stripe.stripeterminal.external.callable.*
import com.stripe.stripeterminal.external.models.*
import com.stripe.stripeterminal.log.LogLevel

const val LOG_TAG = "Terminal"
const val SIMULATED = false

class Terminal(var activity: MainActivity, var location: String, var token_js_function: String) {

    private var search: Cancelable? = null
    private var connectedreader: Reader? = null
    private var cancelable: Cancelable? = null
    private var tokenCallback: ConnectionTokenCallback? = null

    private val tokenProvider = object : ConnectionTokenProvider {
        override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
            activity.runOnUiThread {
                Log.i(LOG_TAG, "Requesting token.")
                tokenCallback = callback
                activity.binding.webview.evaluateJavascript("$token_js_function()") {}
            }
        }
    }

    fun pushToken(token: String) {
        if(tokenCallback == null) return
        Log.i(LOG_TAG, "Got token: $token")
        if (token != "ERROR") tokenCallback!!.onSuccess(token.trim('"'))
        else tokenCallback!!.onFailure(ConnectionTokenException("Unable to get token."))
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
            status("Recherche de terminaux en cours")
            search = getInstance().discoverReaders(config, object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    val filteredReaders = readers.filter { r -> r.deviceType.deviceName == "bbpos_wisepad3"}

                    activity.runOnUiThread {
                        if(dialog != null) dialog!!.dismiss()
                        val last = activity.prefs.getString("last_terminal", null)
                        dialog = AlertDialog.Builder(activity)
                            .setTitle("Connexion terminal")
                            .setItems(filteredReaders.map { r ->
                                var s = r.serialNumber
                                if(s == last) s += " (dernier utilisé)"
                                s
                            }.toTypedArray()) { _, which ->
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
                    status("Échec de la recherche des terminaux !")
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
                    activity.prefs.edit().putString("last_terminal", reader.serialNumber).apply()
                }

                override fun onFailure(e: TerminalException) {
                    Log.e(LOG_TAG, "connectBluetoothReader failure", e)
                    status("Échec connexion: %s".format(e.message))
                }
            }
        )
    }

    fun startPayment(amount: Long, uid: String, callbackJsFunction: String) {
        if(connectedreader == null) {
            status("Terminal non connecté !")
            return
        }

        val params = PaymentIntentParameters.Builder()
            .setAmount(amount)
            .setCurrency("eur")
            .setDescription(uid)
            .build()
        getInstance().createPaymentIntent(params, object: PaymentIntentCallback {
            override fun onSuccess(paymentIntent: PaymentIntent) {
                cancelable = getInstance().collectPaymentMethod(paymentIntent,
                    object : PaymentIntentCallback {
                        override fun onSuccess(paymentIntent: PaymentIntent) {

                            getInstance().processPayment(paymentIntent, object : PaymentIntentCallback {
                                override fun onSuccess(paymentIntent: PaymentIntent) {
                                    activity.runOnUiThread {
                                        var data = paymentIntent.id
                                        if(paymentIntent.getCharges().isNotEmpty()) data = paymentIntent.getCharges()[0].id
                                        Log.i(LOG_TAG, "processPayment success! $data")
                                        activity.binding.webview.evaluateJavascript("$callbackJsFunction('$uid', '$data')") {}
                                    }
                                }

                                override fun onFailure(e: TerminalException) {
                                    Log.w(LOG_TAG,"processPayment failed!", e)
                                }
                            } )
                        }

                        override fun onFailure(e: TerminalException) {
                            Log.w(LOG_TAG,"collectPaymentMethod failed!", e)
                            getInstance().cancelPaymentIntent(paymentIntent, object : PaymentIntentCallback {
                                override fun onFailure(e: TerminalException) {
                                    Log.w(LOG_TAG,"cancelPaymentIntent failed!", e)
                                }

                                override fun onSuccess(paymentIntent: PaymentIntent) {
                                    Log.i(LOG_TAG,"cancelPaymentIntent success!")
                                }
                            })
                        }
                    })
            }

            override fun onFailure(e: TerminalException) {
                Log.w(LOG_TAG,"createPaymentIntent failed!", e)
                status("Impossible de créer le paiement.")
            }
        })

    }

    fun cancelPayment() {
        if(cancelable == null) return
        if(cancelable!!.isCompleted) return
        cancelable!!.cancel(object : Callback {
            override fun onFailure(e: TerminalException) {
                Log.w(LOG_TAG,"cancel failed!", e)
            }
            override fun onSuccess() {
                Log.i(LOG_TAG,"cancel success.")
            }
        })
    }
}