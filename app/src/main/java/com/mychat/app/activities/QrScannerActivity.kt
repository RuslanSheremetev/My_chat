package com.mychat.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.mychat.app.R

class QrScannerActivity : AppCompatActivity() {
    private lateinit var barcodeView: BarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        barcodeView = findViewById(R.id.barcodeView)
        barcodeView.setDecoderFactory(DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE)))
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: Result?) {
                result?.let {
                    val data = it.text
                    val token = if (data.contains("token=")) {
                        data.substringAfter("token=").substringBefore("&")
                    } else data
                    
                    val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this@QrScannerActivity)
                    prefs.edit().putString("token", token).apply()
                    Toast.makeText(this@QrScannerActivity, "QR отсканирован!", Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent()
                    intent.putExtra("qr_token", token)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
            override fun possibleResultPoints(resultPoints: List<com.google.zxing.ResultPoint>?) {}
        })

        findViewById<ImageButton>(R.id.btnScannerBack).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}
