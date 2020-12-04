package com.example.splitter

import android.os.IBinder

interface AudioServiceBinder : IBinder {
    val isStarted : Boolean
    fun play()
    fun pause()
    fun close()
}