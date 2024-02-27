package org.planetesciences.stripewebview

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.PrintWriter
import java.net.Socket
import java.util.*

const val PRINTER_LOG_TAG = "Printer"

// From: https://developer.android.com/reference/android/bluetooth/BluetoothDevice
// "If you are connecting to a Bluetooth serial board then try using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB."
private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

class Printer(val activity: MainActivity, val target: String) {

    private var thread: PrintThread? = null

    fun init() {
        // TODO: List dialog like terminals...
    }

    fun print(ticket: String) {
        if(thread != null && thread!!.isAlive) {
            status("Une impression est déjà en cours !")
            return
        }
        status("Impression envoyée...")
        thread = PrintThread(ticket)
        thread!!.start()
    }

    private inner class PrintThread(val ticket: String) : Thread() {
        @SuppressLint("MissingPermission")
        override fun run() {
            try {
                val parts = target.split(":").size
                if (parts == 2) {
                    // TCP host (IP:PORT)
                    val hostport = target.split(":")
                    val socket = Socket(hostport[0], hostport[1].toInt())
                    socket.outputStream.write(ticket.encodeToByteArray())
                    socket.close()
                } else if(parts == 6) {
                    // Bluetooth MAC (AA:BB:CC:DD:EE:FF)
                    val btadapter = (activity.baseContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                    btadapter.cancelDiscovery()
                    val btdev = btadapter.getRemoteDevice(target)
                    val uuid = UUID.fromString(SPP_UUID)
                    val btsock = btdev.createRfcommSocketToServiceRecord(uuid)
                    btsock.connect()
                    Thread.sleep(100)
                    btsock.outputStream.write(ticket.encodeToByteArray())
                    btsock.close()
                } else {
                    Log.w(PRINTER_LOG_TAG, "Invalid printer target: %s".format(target))
                    status("Imprimante invalide.")
                    return
                }
                status("Impression OK !")

            } catch (e: Exception) {
                Log.e(PRINTER_LOG_TAG, "exception", e)
                status("Erreur: %s".format(e.message))
            }
        }
    }

    fun status(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }
}