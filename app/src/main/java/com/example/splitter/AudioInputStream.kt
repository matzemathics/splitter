package com.example.splitter

import android.util.Log
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.ByteBuffer

class AudioInputStream(
    private val stream: InputStream,
    private val maxFrameSize: Int
) : BufferedInputStream(stream) {
    private val lenBuffer = ByteBuffer.allocate(4)
    private val frameBuffer = ByteArray(maxFrameSize)

    fun readFrame () : ByteArray {
        var rem = 4
        var offs = 0
        do {
            read(lenBuffer.array(), offs, rem).also {
                offs += it
                rem -= it
            }
        } while (rem != 0)

        val len = lenBuffer.int
        lenBuffer.rewind()

        if (len == 0) {
            return ByteArray(0)
        }

        rem = len
        offs = 0
        do {
            read(frameBuffer, offs, rem).also {
                offs += it
                rem -= it
            }
        } while (rem != 0)

        return frameBuffer.sliceArray(0 until len)
    }
}