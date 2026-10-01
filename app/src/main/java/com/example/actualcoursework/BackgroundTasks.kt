package com.example.actualcoursework

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log

import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class BackgroundTasks : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default+ SupervisorJob())
    private var heartbeatJob: Job? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val textTitle ="Bluetooth messenger"
        val textContent  ="keeping alive"
        val notification = NotificationCompat.Builder(this, "my_channel_id")
            .setContentText(textContent)
            .setContentTitle(textTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager 
        
        val channel = NotificationChannel("my_channel_id", "My Channel", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
        ServiceCompat.startForeground(this@BackgroundTasks,1,notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)



        val flow = flow {
            while (true) {
                emit(Unit)
                delay(15000

                )
            }
        }
        heartbeatJob?.cancel()
    heartbeatJob=serviceScope.launch {
        flow.collect {
            sendControlMessage(this@BackgroundTasks, "HRBT")
            Log.i("Heartbeat","Sent connected message ")
        }
    }




        return START_STICKY
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    


}

