package com.example.actualcoursework

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission

class MessageReceiver2(
    private val context: Context,
    private val onMessageReceived: (String) -> Unit,
    private val onDeviceNameFound: (String) -> Unit
) {
    private val messageParts = mutableListOf<String>()
    private val alreadyReceivedPackets = mutableListOf<String>()
    private var expectedPackets: Int = 0
    private var receivedMessage: String = ""
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private var scanning = false

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name ?: "Unknown Device"
            onDeviceNameFound(deviceName)

            val scanRecord = result.scanRecord
            val manufacturerData = scanRecord?.getManufacturerSpecificData(0xFFFF)

            manufacturerData?.let {
                val message = String(it)
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

        scanner.startScan(null, settings, scanCallback)
        scanning = true
        Log.i("BLE_RECEIVE", "Started Scanning")
    }
    fun messageHandler(msg: String) {
        if (!alreadyReceivedPackets.contains(msg.take(2))) {
            alreadyReceivedPackets.add(msg.take(2)) //new logic to allow multi packet messages
            try {
                expectedPackets = (msg.subSequence(2, 4)).toString().toInt()
            } catch (e: Exception) {
                Log.e("BLE_RECV", "Error parsing packet: $msg")
                return
            }
            Log.i("BLE_RECV", "Expected packets: $expectedPackets")
            messageParts.add(msg)
            messageParts.sortBy { it.take(2) }

            Log.i("BLE_RECV", "Message received: $alreadyReceivedPackets")
        }

        if (alreadyReceivedPackets.size == expectedPackets) {
            receivedMessage = messageParts.joinToString(separator = "") { it.drop(4).trim().dropLast(2) }
            onMessageReceived(receivedMessage)
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

