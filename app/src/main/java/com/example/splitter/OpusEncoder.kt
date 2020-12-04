package com.example.splitter

class OpusEncoder private constructor (val enc: Long, val frameSize: Int) {
    companion object {
        fun create (sampleRate: Int, channels: Int, frameSize: Int) : OpusEncoder {
            return OpusEncoder(init_encoder(sampleRate, channels), frameSize)
        }

        private external fun init_encoder(sampleRate: Int, i: Int) : Long
        private external fun native_encode (
            enc: Long, frameSize: Int, input: ShortArray, output: ByteArray, len: Int) : Int
        private external fun native_close (enc: Long)
    }

    fun encode (input: ShortArray, output: ByteArray, len: Int) : Int {
        return native_encode(enc, frameSize, input, output, len)
    }

    fun close () {
        native_close(enc)
    }
}