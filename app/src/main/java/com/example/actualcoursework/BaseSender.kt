package com.example.actualcoursework
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import java.security.SecureRandom
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
        duration: Int = 200
    ) {
        try {


            advertiser?.startAdvertisingSet(
                parameters,
                packet,
                scanResponse,
                null,
                null,
                duration,
                0,
                advertisingSetCallback,
            )

        } catch (e: Exception) {

            Log.e("Advertise Error", "Error $e")
        }


    }

    private val sRandom = SecureRandom()
    protected fun nextId(sRandom: SecureRandom):String{

        val bytes = ByteArray(4)
        sRandom.nextBytes(bytes)
        return bytes.joinToString { "%02x".format(it)}


    }
    protected fun packetBuilder(message: String,ttl: Int = 7,recieverId:String ):MutableList<AdvertiseData> {//overrides in child class based on packet type
       val nextId = nextId(sRandom)
        val preppedPackets:MutableList<AdvertiseData> = mutableListOf()
        val ciphermessage = Encryption ().encrypt(message,recieverId,nextId,KeyManagement())
        val chunkSize =10;
        val packetsToAdd: List<ByteArray> = (0 until ciphermessage.size step chunkSize).map { start ->
            ciphermessage.copyOfRange(start, minOf(start + chunkSize, ciphermessage.size))
        }
        val numofpackets =packetsToAdd.size-1
       val totalPackets =if(numofpackets<10) "0$numofpackets" else numofpackets.toString()

        packetsToAdd.forEachIndexed { index, byte ->
            val currentIndex = if (index >= 10) { "$index" } else { "0$index" }
            preppedPackets.add(AdvertiseData.Builder()
                .addManufacturerData(
                    0xFFFF,
                    (currentIndex + totalPackets).toByteArray()+ byte +  (ttl.toString() + nextId).toByteArray()
                )
                .setIncludeDeviceName(false)
                .build())
        }
        return preppedPackets

    }


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_ADVERTISE])
    protected fun processSendQueue(
        context: Context,
        sendQueue: Queue<AdvertiseData>,
        scanResponse: AdvertiseData,
        advertisingSetCallback: AdvertisingSetCallback,
        parameters: AdvertisingSetParameters,

        ) {
        try {
            val packet: AdvertiseData = sendQueue.remove()


            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw Exception("Permission not granted")

            }
            advertise(packet, scanResponse, advertisingSetCallback, parameters)




        }
        catch (e : Exception){
            Log.e("Advertise Error", "Error $e")
        }
        }  //removes a packet from the send queue and calls advertise
    }












