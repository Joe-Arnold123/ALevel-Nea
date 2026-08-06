package com.example.actualcoursework


import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@SuppressLint("MissingPermission")
suspend fun sendControlMessage(context: Context,advertising: kotlinx.coroutines.flow.Flow<Boolean>,operation: String) {

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bluetoothManager.adapter
    val advertiser = adapter?.bluetoothLeAdvertiser
    val packet:String="CTRL$operation"

    if (advertiser == null) {
        Log.e("BLE_SEND", "Advertiser not available")
        return
    }


    val parameters = AdvertisingSetParameters.Builder()
        .setLegacyMode(true) // Compatible with all devices
        .setConnectable(false) // Quicker Broadcast
        .setInterval(AdvertisingSetParameters.INTERVAL_LOW) // Fast broadcast
        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
        .setScannable(true)
        .build()


    val data = AdvertiseData.Builder()
        .addManufacturerData(0xFFFF, packet.toByteArray())
        .build()

    val scanResponse = AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .build()

    val callback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            if (status == ADVERTISE_SUCCESS) {
                Log.i("BLE_SEND", "Control signal started")
            } else {
                Log.e("BLE_SEND", "Failed to start control advertising: $status")
            }
        }
    }

advertising.first{isTrue -> !isTrue}
    advertiser.startAdvertisingSet(parameters, data, scanResponse, null, null, callback)
    delay(100)
    advertiser.stopAdvertisingSet(callback)
}

