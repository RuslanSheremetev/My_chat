package com.mychat.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.mychat.app.MainActivity
import com.mychat.app.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType

class QrLoginActivity : AppCompatActivity() {
    private val client = OkHttpClient()

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
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val scannedData = result.contents
            // Извлекаем токен из URL mychat://login?token=...
            val qrToken = if (scannedData.contains("token=")) {
                scannedData.substringAfter("token=").substringBefore("&")
            } else {
                scannedData
            }
            loginWithQR(qrToken)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun loginWithQR(qrToken: String) {
        val json = JSONObject().apply {
            put("qr_token", qrToken)
            put("device_name", android.os.Build.MODEL)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("http://2.26.71.102:8000/api/qr/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@QrLoginActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val respJson = JSONObject(response.body?.string() ?: "")
                    val accessToken = respJson.optString("access_token")
                    val username = respJson.optString("username")
                    // Сохраняем токен и открываем чат
                    val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this@QrLoginActivity)
                    prefs.edit()
                        .putString("token", accessToken)
                        .putString("username", username)
                        .apply()
                    runOnUiThread {
                        startActivity(Intent(this@QrLoginActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@QrLoginActivity, "QR-код недействителен", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
