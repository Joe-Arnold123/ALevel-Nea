package com.example.actualcoursework

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.bluetooth.le.AdvertisingSetCallback.ADVERTISE_SUCCESS
import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Sender(
    private val context: Context,
    private val serviceScope: CoroutineScope,
    private val onRestartRequired: () -> Unit,
    private val onPacketsUpdated: (List<String>) -> Unit
) {

    @SuppressLint("MissingPermission")
    fun send(message: String, isMissing: Boolean = false) {
        serviceScope.launch {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            val advertiser = adapter?.bluetoothLeAdvertiser
            
            if (advertiser == null) {
                Log.e("BLE_SEND", "Advertiser not available")
                return@launch
            }

            var packets = if (message.length > 4) {
                message.chunked(10).toMutableList()
            } else {
                mutableListOf(message)
            }

            val toBeSent = packets.size
            val parameters = AdvertisingSetParameters.Builder()
                .setLegacyMode(true)
                .setConnectable(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .setScannable(true)
                .build()

            val scanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

            packets.forEachIndexed { index, content ->
                val localAdvertisingCallback = object : AdvertisingSetCallback() {
                    override fun onAdvertisingSetStarted(
                        advertisingSet: AdvertisingSet?,
                        txPower: Int,
                        status: Int
                    ) {
                        if (status == ADVERTISE_SUCCESS) {
                            Log.i("BLE_SEND", "Broadcasting message: $message")
                            (context as? Activity)?.runOnUiThread {
                                Toast.makeText(context, "Sending...", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("BLE_SEND", "Failed to start advertising: $status")
                            onRestartRequired()
                        }
                    }
                }

                val packetnum = if (index < 10) "0$index" else index.toString()
                val sendingTotal = if (toBeSent < 10) "0$toBeSent" else toBeSent.toString()
                val hash = message.hashCode().toString().take(2)

                val data = AdvertiseData.Builder()
                    .addManufacturerData(
                        0xFFFF,
                        (packetnum + sendingTotal + packets[index] + hash).toByteArray()
                    )
                    .setIncludeDeviceName(false)
                    .build()

                advertiser.startAdvertisingSet(
                    parameters, data, scanResponse, null, null, localAdvertisingCallback
                )

                delay(200)
                advertiser.stopAdvertisingSet(localAdvertisingCallback)
                delay(60)
            }
            onPacketsUpdated(packets)
        }
    }
}
