package com.example.actualcoursework
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MessageReceiver2(
    private val context: Context,
    private val onMessageReceived: (String) -> Unit,
    private val onDeviceNameFound: (String) -> Unit,
    private val onSendMessageRequest: (String) -> Unit,
    private val previousmessage: String

) {
    private val messageCache = MessageCache()

    private val messageParts = mutableSetOf<String>()
    private val alreadyReceivedPackets = mutableListOf<String>()
    private var expectedPackets: Int = 0
    private var receivedMessage: String = ""
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private var scanning = false
    private var timer: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var connected: Job? = null


    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord
            val manufacturerData = scanRecord?.getManufacturerSpecificData(0xFFFF) //need to request an id

            manufacturerData?.let {
                val message = String(it)
                //filter for this app

                    val deviceName = result.device.name ?: "Unknown Device"
                    onDeviceNameFound(deviceName)

                    messageHandler(message)


            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE_RECEIVE", "Scan failed with error: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (scanning) return
        val scanner = adapter?.bluetoothLeScanner ?: return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Filter for the app
        val filter = ScanFilter.Builder()
            .setManufacturerData(0xFFFF, byteArrayOf(), byteArrayOf())
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
        scanning = true
        Log.i("BLE_RECEIVE", "Started Scanning")
    }
     fun messageHandler(msg: String) {
         if(dupplicate(msg, cache = messageCache.cache )){
             Log.i("BLE_RECV", "Duplicate message received")
             return
         }


        if (msg.take(4) == "CTRL") {
            if (msg.startsWith("CTRLHRBT")) {
                Toast.makeText(context, "Heartbeat received", Toast.LENGTH_SHORT).show()
                MainActivity.status.isConnected.value=true
                connected?.cancel()
                connected = scope.launch {
                    delay(30000)
                    MainActivity.status.isConnected.value=false
                    Log.i("BLE_RECV", "Disconnected")
                }
                return
            }
            else if (msg.startsWith("CTRLDROP")) {
                //send message parts contained in message
                Log.i("BLE_RECV", "Dropped packets")
                onSendMessageRequest(previousmessage)
                Log.i("BLE_RECV", "trying to resend message ")




            }

        }





            if  (!alreadyReceivedPackets.contains(msg.take(2)) || alreadyReceivedPackets.isEmpty()) {
                alreadyReceivedPackets.add(msg.take(2)) //new logic to allow multi packet messages
                try {
                    expectedPackets = (msg.subSequence(2, 4)).toString().toInt()
                    if(alreadyReceivedPackets.size==1){
                        timer=scope.launch {
                            StartTimer(context,expectedPackets,alreadyReceivedPackets)//wait for message to end and count packets
                        }
                    }

                } catch (e: Exception) {
                    Log.e("BLE_RECV", "Error parsing packet: $msg")
                    return
                }
                Log.i("BLE_RECV", "Expected packets: $expectedPackets")
                messageParts.add(msg)
                messageParts.sortedBy{ it.take(2) }

                Log.i("BLE_RECV", "Message received: $alreadyReceivedPackets")
            }
        else {
            if (msg.takeLast(2) != messageParts.last().takeLast(2)
            ) {


                alreadyReceivedPackets.clear()
                alreadyReceivedPackets.add(msg.take(2))
                messageParts.clear()
                expectedPackets = msg.subSequence(2,4).toString().toInt()
                messageParts.add(msg)
                timer?.cancel()
                timer=scope.launch {
                    StartTimer(context,expectedPackets,alreadyReceivedPackets)
                }



            }
        }

        if (alreadyReceivedPackets.size == expectedPackets) {
                receivedMessage =
                    messageParts.joinToString(separator = "") { it.drop(4).trim().dropLast(3) }
                onMessageReceived(receivedMessage)
            addMessageToCache(messageParts.last().takeLast(2),messageCache.cache)


            timer?.cancel()
                messageParts.clear()
                alreadyReceivedPackets.clear()
            } else if (alreadyReceivedPackets.size > expectedPackets) {
                onMessageReceived("Error")
                messageParts.clear()
                alreadyReceivedPackets.clear()
            }

    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!scanning) return
        val scanner = adapter?.bluetoothLeScanner ?: return
        scanner.stopScan(scanCallback)
        scanning = false
        Log.i("BLE_RECEIVE", "Stopped Scanning")
    }
}



