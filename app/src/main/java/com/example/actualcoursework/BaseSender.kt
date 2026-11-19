package com.example.actualcoursework

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import java.util.Queue

abstract class BaseSender(context: Context) {
    protected val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    protected val adapter: BluetoothAdapter? = bluetoothManager.adapter
    protected var advertiser = adapter?.bluetoothLeAdvertiser
    protected var advertising: Boolean = false

    val advertisingSetCallback = object : AdvertisingSetCallback() {

        override fun onAdvertisingSetStarted(
            advertisingSet: AdvertisingSet?,
            txPower: Int,
            status: Int
        ) {
            if (status == ADVERTISE_SUCCESS) {
                advertising = true
                Log.i("Advertise ", "Great Success ")
            } else {
                Log.e("Advertise ", "Error $status")
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            advertising = false
            Log.i("Advertise ", "Stopped advertising")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    protected fun advertise(
        packet: AdvertiseData,  //abstract function to send whatever its given
        scanResponse: AdvertiseData, //each one of these has an overrideable func
        advertisingSetCallback: AdvertisingSetCallback,//meaning child classes can change settings if they want
        parameters: AdvertisingSetParameters,//not that they are sentient
        duration: Int = 100
    ) {
        try {


            advertiser?.startAdvertisingSet(
                parameters,
                packet,
                scanResponse,
                null,
                null,
                200,
                0,
                advertisingSetCallback,
            )
            advertiser?.stopAdvertisingSet(advertisingSetCallback)
        } catch (e: Exception) {

            Log.e("Advertise Error", "Error $e")
        }


    }

    protected fun packetBuilder(message: String) {//overrides in child class based on packet type


    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    protected fun processSendQueue(
        sendQueue: Queue<AdvertiseData>,
        scanResponse: AdvertiseData,
        advertisingSetCallback: AdvertisingSetCallback,
        parameters: AdvertisingSetParameters,

        ) {
        try {
            val packet: AdvertiseData = sendQueue.remove()


            advertise(packet, scanResponse, advertisingSetCallback, parameters)


            Log.e("Advertise Error", "Error $e")


    }
}











