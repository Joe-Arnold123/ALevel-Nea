package com.example.actualcoursework

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.actualcoursework.ui.theme.ActualCourseworkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {

    private var receivedMessage by mutableStateOf("No message received yet")
    private var receiver: MessageReceiver2? = null
    private var sentDeviceName by mutableStateOf("")


    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var activeAdvertisingCallback: AdvertisingSetCallback? = null
    private val messagestate = TextFieldState("Hello")
    private val messageParts = mutableListOf<String>()
    private var previousMessage: String =""

    object status {
        var isConnected = mutableStateOf(false)
    }
    private val serviceScope = CoroutineScope(Dispatchers.Default+ SupervisorJob())
    private var messageJob: Job? = null


    private var heartbeatJob: Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startAppLogic()
        } else {
            Toast.makeText(
                this,
                "All permissions (Bluetooth + Location) are required",
                Toast.LENGTH_SHORT
            ).show()
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
                            text = "Device name: $sentDeviceName",
                            modifier = Modifier.padding(top = 60.dp, bottom = 30.dp)
                        )

                        TextField(
                            state = messagestate,
                            label = { Text("Enter Message") },
                            modifier = Modifier.width(300.dp).height(300.dp)

                        )
                        Button(onClick = { sender.send(messagestate.text.toString(),false) }) {
                            Text("Send message ")

                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if(status.isConnected==mutableStateOf(false)){
                            drawCircle(//add connected device num asw

                                color=Color.Red,
                                radius = 100f


                            )}
                            else{
                                drawCircle(//add connected device num asw

                                    color=Color.Green,
                                    radius = 100f


                                )



                            }
                        }
                    }
                }
            }
        }
    }
    val sender=Sender(this,serviceScope,onRestartRequired = {startAppLogic()},onPacketsUpdated = {packets ->previousMessage=packets})



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
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter


            super.onResume()

            checkPermissionsAndStart()




        }
    private  fun startAppLogic() {
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
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please turn on GPS/Location toggle", Toast.LENGTH_LONG).show()
        }

        // Initialize Advertiser
        bluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser

        // Start Receiving
        if (receiver == null) {
            receiver = MessageReceiver2(
                context = this,
                onMessageReceived = { msg -> receivedMessage = msg },
                onDeviceNameFound = { name -> sentDeviceName = name },
                onSendMessageRequest = { msg -> sender.send(msg) },
                previousmessage = previousMessage
            )
        }

        receiver?.startScanning()




val serviceIntent = Intent(this, BackgroundTasks::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    @SuppressLint("MissingPermission")
    override fun onStop() {
        super.onStop()
        receiver?.stopScanning()
        activeAdvertisingCallback?.let {
            bluetoothLeAdvertiser?.stopAdvertisingSet(it)
        }
    }
    @SuppressLint("MissingPermission") //permission are properly handled but linter is stupid
    override fun onPause() {
        receiver?.stopScanning()
        super.onPause()
        activeAdvertisingCallback?.let {
            bluetoothLeAdvertiser?.stopAdvertisingSet(it)
        }
    }
}



