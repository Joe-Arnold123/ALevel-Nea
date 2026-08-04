package com.example.actualcoursework

import android.Manifest
import android.R.attr.bottom
import android.R.attr.text
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.actualcoursework.ui.theme.ActualCourseworkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.Text

class MainActivity : ComponentActivity() {

    private var receivedMessage by mutableStateOf("No message received yet")
    private var receiver: MessageReceiver? = null
    private var sentDeviceName by mutableStateOf("")

    // Hold references to prevent garbage collection
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var activeAdvertisingCallback: AdvertisingSetCallback? = null
    private val messagestate = TextFieldState("Hello")
    private val messageParts = mutableListOf<String>()
    private var expectedPackets=0
    private var recievedPackets=0
    private var alreadyRecievedPackets = mutableListOf<String>()


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startAppLogic()
        } else {
            Toast.makeText(this, "All permissions (Bluetooth + Location) are required", Toast.LENGTH_SHORT).show()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            startAppLogic()
        } else {
            Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ActualCourseworkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Received: $receivedMessage",
                            modifier = Modifier.padding(top = 50.dp, bottom = 20.dp)
                        )
                        Text(
                            text= "Device name: $sentDeviceName",
                            modifier = Modifier.padding(top = 60.dp, bottom = 30.dp)
                        )

                        TextField(
                            state= messagestate,
                            label ={Text("Enter Message")}

                        )
                        Button(onClick = { sendMessage(messagestate.text.toString()) }) {
                            Text("Send message ")

                        }
                    }
                    LaunchedEffect(Unit) {
                        checkPermissionsAndStart()
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startAppLogic()
        } else {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }
        override fun onResume() {
            super.onResume()
            checkPermissionsAndStart()
        }
    private fun startAppLogic() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show()
            return
        }

        if (!adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            return
        }

        // Check Location toggle
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please turn on GPS/Location toggle", Toast.LENGTH_LONG).show()
        }

        // Initialize Advertiser
        bluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser

        // Start Receiving
        if (receiver == null) { // new logic to prevent multiple receivers
            receiver = MessageReceiver(this, { msg ->
                if(!alreadyRecievedPackets.contains(msg.take(2))) {
                    alreadyRecievedPackets.add(msg.take(2)) //new logic to allow multi packet messages
                    expectedPackets = (msg.subSequence(2, 4)).toString().toInt()
                    //messageParts.add(msg.subSequence(4,msg.length).toString())
                    messageParts.add(msg)
                    messageParts.sortBy { it.take(2) }
                    Log.i("BLE_RECV", "Message received: $alreadyRecievedPackets")
                }
                if(messageParts.size==expectedPackets){


                    receivedMessage=messageParts.joinToString(separator = ""){it.drop(4).trim()}
                    messageParts.clear()
                    alreadyRecievedPackets.clear()

                }

            },
            sendingDeviceName = { deviceName ->
                sentDeviceName=deviceName
            })


        }
        receiver?.stopScanning()//prevents it failing silently
        receiver?.startScanning()

        // Auto-send initial message
        sendMessage("Hello")
    }

    @SuppressLint("MissingPermission")


        fun sendMessage(message: String) {
        lifecycleScope.launch {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            val advertiser = bluetoothLeAdvertiser
            if (advertiser == null) {
                Log.e("BLE_SEND", "Advertiser not available")
                return@launch
            }
            var packets = mutableListOf<String>()

            if(message.length>12){
                packets = message.chunked(message.length / 12).toMutableList()
            }
            else{
                packets.add(message)
            }


            var toBeSent = packets.size

            // Stop anything else sending first
            activeAdvertisingCallback?.let {
                advertiser.stopAdvertisingSet(it)
            }

            val parameters = AdvertisingSetParameters.Builder()
                .setLegacyMode(true) // Compatible with all devices
                .setConnectable(false) // Quicker Broadcast
                .setInterval(AdvertisingSetParameters.INTERVAL_LOW) // Fast broadcast
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .setScannable(true)
                .build()


            //val data = AdvertiseData.Builder()
            //.addManufacturerData(0xFFFF, message.toByteArray())
            //.setIncludeDeviceName(false)
            //.build()

            // makes phone see the data sometimes weird buggy workaround
            val scanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

            activeAdvertisingCallback = object : AdvertisingSetCallback() {
                override fun onAdvertisingSetStarted(
                    advertisingSet: AdvertisingSet?,
                    txPower: Int,
                    status: Int
                ) {
                    if (status == ADVERTISE_SUCCESS) {
                        Log.i("BLE_SEND", "Broadcasting message: $message")
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Sending...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Log.e("BLE_SEND", "Failed to start advertising: $status")
                    }
                }
            }

            //advertiser.startAdvertisingSet(
            //parameters, data, scanResponse, null, null, 100
            //,3,activeAdvertisingCallback)
            var index = 0
            var packetnum = ""
            var sendingTotal = ""
            do {

                 if (index < 10) {
                    packetnum = "0$index"
                }
                else {
                    packetnum = index.toString()
                }
                if (toBeSent < 10) {
                    sendingTotal = "0$toBeSent"
                } else {
                    sendingTotal = toBeSent.toString()
                }
                val data = AdvertiseData.Builder()
                    .addManufacturerData(
                        0xFFFF,
                        (packetnum + sendingTotal + packets[index]).toByteArray()
                    )
                    .setIncludeDeviceName(false)
                    .build()

                advertiser.startAdvertisingSet(
                    parameters, data, scanResponse, null, null, 100, 3, activeAdvertisingCallback
                )
                index++
                delay(100)
                bluetoothLeAdvertiser?.stopAdvertisingSet(activeAdvertisingCallback)
            } while (index<toBeSent)

        }
    }


    @SuppressLint("MissingPermission")
    override fun onStop() {
        super.onStop()
        receiver?.stopScanning()
        activeAdvertisingCallback?.let {
            bluetoothLeAdvertiser?.stopAdvertisingSet(it)
        }
    }
}

class MessageReceiver(
    private val activity: MainActivity,
    private val onMessageReceived: (String) -> Unit,
    private val sendingDeviceName: (String) -> Unit
) {
    private val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private var scanning = false

    private val scanCallback = object : ScanCallback() { //what to do when phone gets a message
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE])
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val record = result.scanRecord ?: return



            val data = record.getManufacturerSpecificData(0xFFFF)
            if (data != null) {
                val message = String(data, Charsets.UTF_8)
                Log.i("BLE_RECV", "MESSAGE DECODED: $message")
                activity.runOnUiThread {
                    onMessageReceived(message)
                }
                if(record.deviceName != null){
                    val deviceName = record.deviceName?: "None"
                    activity.runOnUiThread {

                        sendingDeviceName(deviceName)
                    }
                }

                    else{
                        Log.i("BLE_RECV", "Device name is null")
                    }


                }

            }


        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE_RECV", "Scan failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (scanning) return
        val scanner = adapter?.bluetoothLeScanner ?: return
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
        scanning = true
        Log.i("BLE_RECV", "Scanning started (Low Latency)")
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!scanning) return
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        scanning = false
    }
}
