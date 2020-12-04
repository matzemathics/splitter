package com.example.splitter
import com.google.zxing.Result

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import me.dm7.barcodescanner.zxing.ZXingScannerView
import me.dm7.barcodescanner.zxing.ZXingScannerView.ResultHandler

class QrCodeScanner : AppCompatActivity(), ResultHandler {
    private var mScannerView: ZXingScannerView? = null
    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        mScannerView = ZXingScannerView(this)
        setContentView(mScannerView)
    }

    public override fun onResume() {
        super.onResume()
        mScannerView!!.setResultHandler(this)
        mScannerView!!.startCamera()
    }

    public override fun onPause() {
        super.onPause()
        mScannerView!!.stopCamera()
    }

    override fun handleResult(rawResult: Result) {
        // Do something with the result here
        // Prints scan results
        Log.v("result", rawResult.text)
        // Prints the scan format (qrcode, pdf417 etc.)
        Log.v("result", rawResult.barcodeFormat.toString())
        //If you would like to resume scanning, call this method below:
        //mScannerView.resumeCameraPreview(this);
        val intent = Intent()
        intent.putExtra(App.EXTRA_KEY_QR_CODE, rawResult.text)
        setResult(RESULT_OK, intent)
        finish()
    }
}