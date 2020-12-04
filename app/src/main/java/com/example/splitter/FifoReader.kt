package com.example.splitter

import android.util.Size
import java.lang.Exception
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

class FifoReader(
    private val channel: ReadableByteChannel,
    private val bufferSize: Int)
{
    var buffer = ByteBuffer.allocate(bufferSize).apply { limit(0) }
        private set

    // fill the buffer, so that there are at least len remaining elements
    // enter in read mode, so
    //      limit = index of last that should be read
    //      position = index of first that should be read
    // leave in read mode
    fun fillTo (len: Int) {
        if (buffer.remaining() > len) return

        if (len > buffer.capacity()) throw Exception("Failed to read $len bytes: Buffer too small")

        var markPosition = buffer.position()

        if (buffer.capacity() - buffer.position() < len) {
            //cannot fit into buffer, so moving data
            val tmp = ByteArray(buffer.remaining())
            buffer.get(tmp)
            buffer.clear()
            buffer.mark()
            markPosition = 0
            buffer.put(tmp)
        }
        else {
            buffer.mark()
            buffer.position(buffer.limit())
            buffer.limit(buffer.capacity())
        }

        do {
            channel.read(buffer)
        } while (buffer.position() - markPosition < len)

        buffer.limit(buffer.position())
        buffer.reset()
    }
}