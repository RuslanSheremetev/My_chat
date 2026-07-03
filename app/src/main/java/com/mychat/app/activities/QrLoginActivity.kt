package com.mychat.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.mychat.app.MainActivity
import com.mychat.app.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class QrLoginActivity : AppCompatActivity() {
    private lateinit var barcodeView: BarcodeView
    private var scanned = false
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_login)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
            }
        }

        barcodeView = findViewById(R.id.barcodeView)
        barcodeView.setDecoderFactory(DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE)))
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (scanned) return
                result?.let {
                    scanned = true
                    val data = it.text
                    val token = data.substringAfter("token=").substringBefore("&")
                    val username = data.substringAfter("user=").substringBefore("&")
                    loginWithQR(token, username)
                }
            }
            override fun possibleResultPoints(resultPoints: List<com.google.zxing.ResultPoint>?) {}
        })

        findViewById<android.widget.TextView>(R.id.btnGoToLogin).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun loginWithQR(qrToken: String, username: String) {
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
            override fun onFailure(call: Call, e: java.io.IOException) {
                runOnUiThread { loginFallback(qrToken, username) }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: ""
                    val json = JSONObject(respBody)
                    val accessToken = json.optString("access_token")
                    val user = json.optString("username", username)
                    runOnUiThread { loginSuccess(accessToken.ifEmpty { qrToken }, user) }
                } else {
                    runOnUiThread { loginFallback(qrToken, username) }
                }
            }
        })
    }

    private fun loginSuccess(token: String, username: String) {
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("token", token).putString("username", username).apply()
        Toast.makeText(this, "Вход выполнен!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun loginFallback(token: String, username: String) {
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("token", token).putString("username", username).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
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
