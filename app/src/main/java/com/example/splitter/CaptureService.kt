package com.example.splitter

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel

class CaptureService : Service() {
    companion object {
        private val TAG = CaptureService::class.qualifiedName

        init {
            System.loadLibrary("opusjni")
        }
        // actions
        const val ACTION_START_CAPTURE = "start_capture"
        const val ACTION_STOP_CAPTURE = "stop_capture"
        const val EXTRA_APPROVE_CAPTURE = "approve_capture"

        private const val SAMPLE_RATE = 48000
        private val FRAME_SIZE : Int
            get() = (0.02 * SAMPLE_RATE).toInt()
    }

    private var mProjection: MediaProjection? = null
    private val mTcpListener = ServerSocket(App.ServerPort)

    var started = false
        private set

    private val mRecorder = object : Thread() {
        private var mClosed = false

        fun close() {
            mClosed = true
        }

        override fun run() {
            val conf = mProjection?.let{ projection ->
                AudioPlaybackCaptureConfiguration
                    .Builder(projection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .build()
            } ?: return

            val recorder = AudioRecord.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                    .setSampleRate(48000)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build()
                )
                .setAudioPlaybackCaptureConfig(conf)
                .build()

            recorder?.startRecording()

            val encoder = OpusEncoder.create(SAMPLE_RATE, 2, FRAME_SIZE)
            val buffer = ShortArray(FRAME_SIZE*2)
            val bufSize = 2000
            val outBuf = ByteArray(bufSize)
            val bytes = ByteBuffer.allocate(bufSize+4)

            var socket : Socket? = null
            var stream : WritableByteChannel? = null


            while (!mClosed) {
                mTcpListener.soTimeout = 60000
                while (started && socket?.isConnected != true)
                {
                    try {
                        socket = mTcpListener.accept()
                    } catch (e: IOException) {
                        sendBroadcast(Intent(App.HANDLE_CONNECT_FAILED))
                        return
                    }
                    Intent(App.HANDLE_CONNECT_SUCCEEDED).apply {
                        putExtra(App.EXTRA_SERVICE_CLASS, ComponentName(
                            this@CaptureService, CaptureService::class.java))
                        sendBroadcast(this)
                    }

                    stream = Channels.newChannel(socket.getOutputStream())
                }

                while (started) {
                    if(socket?.isConnected != true) break

                    try {
                        val length = recorder!!.read(
                            buffer,
                            0,
                            FRAME_SIZE * 2,
                            AudioRecord.READ_BLOCKING
                        )

                        if (length != 0) {
                            val len = encoder.encode(buffer, outBuf, bufSize)
                            bytes.clear()
                            bytes.putInt(len)
                            bytes.put(outBuf.sliceArray(0 until len))
                            bytes.flip()

                            stream?.write(bytes)
                        }
                    }
                    catch (e: IOException) {
                        break
                    }
                }
                socket?.close()
            }

            encoder.close()
            mTcpListener.close()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CAPTURE -> {
                enableCapture(intent.getParcelableExtra(EXTRA_APPROVE_CAPTURE))
            }
            ACTION_STOP_CAPTURE -> {
                disableCapture()
            }
        }

        return START_STICKY
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
                mRecorder.close()
                if (started) disableCapture()
            }

        }
    }

    private fun startForegroundService() {
        Notification.Builder(this, App.CHANNEL_DEFAULT)
            .setSmallIcon(R.drawable.splitter_simple)
            .setContentTitle("sharing your audio output")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build().let {
                startForeground(
                    App.FG_SERVICE_SHARE, it,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            }
    }

    private fun enableCapture(approveIntent: Intent?) {
        if (mProjection == null) {
            if (approveIntent != null) {
                if (foregroundServiceType !=
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                {
                    startForegroundService()
                }

                val mediaProjectionManager = applicationContext
                    .getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

                mProjection = mediaProjectionManager.getMediaProjection(
                    Activity.RESULT_OK,
                    approveIntent
                )
            }
            else {
                val intent = Intent(CaptureActivity.REQUEST_CAPTURE_PERMISSION)
                sendBroadcast(intent)
                return
            }
        }
        mProjection?.also {
            started = true
            mRecorder.start()
        }
    }

    private fun disableCapture() {
        started = false
        mProjection = null
        stopForeground(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        mRecorder.close()
        if (started) disableCapture()
        mTcpListener.close()
    }
}