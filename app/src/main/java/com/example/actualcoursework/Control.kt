package com.example.actualcoursework


import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.util.Log

import kotlinx.coroutines.delay

@SuppressLint("MissingPermission")
 fun sendControlMessage(context: Context, operation: String,MissingpacketsToBeSent:MutableList<String>?=null) {

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bluetoothManager.adapter
    val advertiser = adapter?.bluetoothLeAdvertiser
    val packet:String="CTRL$operation"

    if (advertiser == null) {//still need to fix why it hides from me
        Log.e("BLE_SEND", "Advertiser not available")
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val advertiser = adapter?.bluetoothLeAdvertiser
        sendControlMessage(context,operation,MissingpacketsToBeSent)

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
                Log.i("BLE_SEND", "sent$operation with packets$MissingpacketsToBeSent")
            } else {
                Log.e("BLE_SEND", "Failed to start control advertising: $status")
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                val advertiser = adapter?.bluetoothLeAdvertiser


            }
        }
    }

    advertiser?.startAdvertisingSet(parameters,
        data,
        scanResponse,
        null,
        null,
        100,
        1,
        callback
    )
    advertiser?.stopAdvertisingSet(callback)

}
suspend fun StartTimer(context:Context,expectedPackets:Int,packetlist: MutableList<String>){//calls dropped packet function after 400ms
    // multiplied by the number of packets
    delay(400L*expectedPackets.toLong())
    if(packetlist.size<expectedPackets) {
        dropped(context,packetlist,expectedPackets)
    }
    else{
        return
    }

}

fun dropped(context: Context,alreadyReceivedPackets: MutableList<String>,expectedPackets: Int){

    val receivedIndices = alreadyReceivedPackets.mapNotNull { packet ->
        packet.take(2).toIntOrNull()
    }.toSet()
    val missingIndices = (0 until expectedPackets).filter { it !in receivedIndices }
    val formattedMissing = missingIndices.map { it.toString().padStart(2, '0') }
    Log.i("BLE_RECV", "Missing Packets: $formattedMissing")
    Log.i("BLE_RECV", "Total missing count: ${missingIndices.size}")
    sendControlMessage(context = context, operation = "DROP",formattedMissing.toMutableList())

}






