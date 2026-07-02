package com.mychat.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.mychat.app.MainActivity
import com.mychat.app.R

class QrLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_login)

        findViewById<android.widget.TextView>(R.id.btnGoToLogin).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<android.widget.Button>(R.id.btnScanQr).setOnClickListener {
            startQRScanner()
        }
    }

    private fun startQRScanner() {
        IntentIntegrator(this).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Наведите камеру на QR-код")
            setBeepEnabled(true)
            initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val scannedData = result.contents
            val token = if (scannedData.contains("token=")) {
                scannedData.substringAfter("token=").substringBefore("&")
            } else {
                scannedData
            }
            val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit().putString("token", token).apply()
            Toast.makeText(this, "QR отсканирован! Токен сохранён", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
