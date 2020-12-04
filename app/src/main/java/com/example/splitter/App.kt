package com.example.splitter

import android.app.*
import android.content.Context
import android.graphics.Color

class App : Application() {
    companion object {
        const val FG_SERVICE_SHARE = 42
        const val FG_SERVICE_RECEIVE = 43

        val HANDLE_CONNECT_FAILED = "splitter.handle_connect_failed"
        val HANDLE_CONNECT_SUCCEEDED = "splitter.handle_connect_succeeded"
        val HANDLE_CONNECTION_CLOSED = "splitter.handle_connection_closed"

        val EXTRA_KEY_QR_CODE = "KEY_QR_CODE"
        val EXTRA_KEY_PORT = "PORT"
        val EXTRA_KEY_HOST = "HOSTNAME"
        val EXTRA_SERVICE_CLASS = "SERVICE_CLASS"

        val ServerPort = 8842

        //request codes
        const val REQUEST_CAPTURE = 1
        const val REQUEST_RECORD = 3
        const val REQUEST_SCAN_QR_CODE = 4
        const val REQUEST_LOCATION = 5

        const val CHANNEL_DEFAULT = "splitter_audio_sharing"
    }

    override fun onCreate() {
        super.onCreate()

        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(service.getNotificationChannel(CHANNEL_DEFAULT) == null) {
            val channel  = NotificationChannel(
                CHANNEL_DEFAULT,
                getString(R.string.general_notification),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.lightColor = Color.GREEN
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            service.createNotificationChannel(channel)
        }
    }
}
