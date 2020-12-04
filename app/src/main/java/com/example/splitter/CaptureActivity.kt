package com.example.splitter

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.splitter.App.Companion.REQUEST_CAPTURE
import com.example.splitter.App.Companion.REQUEST_RECORD

open class CaptureActivity : AppCompatActivity() {
    companion object {
        val TAG = CaptureActivity::class.qualifiedName

        // actions
        const val REQUEST_CAPTURE_PERMISSION = "request_capture_permission"
    }

    private val intentFilter = IntentFilter().apply {
        addAction(REQUEST_CAPTURE_PERMISSION)
    }

    private val intentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                REQUEST_CAPTURE_PERMISSION -> requestCapturePermission()
            }
        }
    }

    private val mediaProjectionManager: MediaProjectionManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD);
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(intentReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(intentReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CAPTURE && resultCode == RESULT_OK) {
            startCapture(data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            REQUEST_RECORD -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // unable to stream audio
                    Toast.makeText(
                        this,
                        "grant record permission, to stream audio",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    protected fun startCapture(approveIntent: Intent? = null) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD);
        }
        else {
            Intent(this, CaptureService::class.java)
                .setAction(CaptureService.ACTION_START_CAPTURE)
                .putExtra(CaptureService.EXTRA_APPROVE_CAPTURE, approveIntent)
                .also { startService(it) }
        }
    }

    private fun requestCapturePermission() {
        mediaProjectionManager?.also {
            startActivityForResult(it.createScreenCaptureIntent(), REQUEST_CAPTURE)
        }
    }
}