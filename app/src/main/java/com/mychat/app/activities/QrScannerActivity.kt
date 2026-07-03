package com.mychat.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.mychat.app.R

class QrScannerActivity : AppCompatActivity() {
    private lateinit var barcodeView: BarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        // Запрашиваем разрешение камеры
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
            }
        }

        barcodeView = findViewById(R.id.barcodeView)
        barcodeView.setDecoderFactory(DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE)))
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Разрешение получено, камера заработает
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
