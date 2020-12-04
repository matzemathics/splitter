package com.example.splitter

import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder

class ReceiveService : Service() {
    companion object {
        val TAG = ReceiveService::class.qualifiedName

        init {
            System.loadLibrary("opusjni")
        }
    }
    private external fun connect(port: Int, host: String) : Int
    private external fun nativeRecvLoop (sock: Int)

    var enabled = true
    var started = false
        private set

    inner class AudioReceiver(
        private val host: String,
        private val port: Int) : Thread()
    {
        override fun run() {
            started = true
            while (enabled) {
                while (!started) sleep(200);
                val sock = connect(port, host)

                if (sock != -1) {
                    val it = Intent(App.HANDLE_CONNECT_SUCCEEDED).apply {
                        putExtra(App.EXTRA_SERVICE_CLASS,
                            ComponentName(this@ReceiveService, ReceiveService::class.java))
                        sendBroadcast(this)
                    }
                    nativeRecvLoop(sock)
                } else {
                    this@ReceiveService.sendBroadcast(Intent(App.HANDLE_CONNECT_FAILED))
                    enabled = false
                }
            }
            stopSelf()
        }
    }

    private var receiveThread : AudioReceiver? = null

    private fun startForegroundService() {
        Notification.Builder(this, App.CHANNEL_DEFAULT)
            .setSmallIcon(R.drawable.splitter_simple)
            .setContentTitle("receiving audio")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build().let {
                startForeground(
                    App.FG_SERVICE_RECEIVE, it,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return object : Binder(), AudioServiceBinder {
            override val isStarted: Boolean
                get() = started

            override fun play() {
                started = true
            }

            override fun pause() {
                started = false
            }

            override fun close() {
                enabled = false
                started = false
            }

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (receiveThread == null)
        {
            val port = intent?.getIntExtra(App.EXTRA_KEY_PORT, 8842)
            val host = intent?.getStringExtra(App.EXTRA_KEY_HOST)

            if (port != null && host != null) {
                receiveThread = AudioReceiver(host, port)
                startForegroundService()
                receiveThread?.start()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        enabled = false
        started = false
    }
}