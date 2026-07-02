package com.mychat.app.activities

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.MainActivity
import com.mychat.app.R

class MapsActivity : AppCompatActivity() {
    private var selectedLat = 55.7558
    private var selectedLng = 37.6173

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val webView = findViewById<WebView>(R.id.mapWebView)
        val locationInfo = findViewById<TextView>(R.id.locationInfo)
        val btnSend = findViewById<Button>(R.id.btnSendLocation)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // OpenStreetMap с возможностью выбора точки
        val mapHtml = """
            <!DOCTYPE html><html><head>
            <meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>body{margin:0}#map{height:100vh;width:100vw}</style>
            </head><body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$selectedLat, $selectedLng], 15);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap'
                }).addTo(map);
                var marker = L.marker([$selectedLat, $selectedLng], {draggable: true}).addTo(map);
                marker.on('dragend', function(e) {
                    var pos = marker.getLatLng();
                    window.android.onLocationSelected(pos.lat, pos.lng);
                });
                map.on('click', function(e) {
                    marker.setLatLng(e.latlng);
                    window.android.onLocationSelected(e.latlng.lat, e.latlng.lng);
                });
            </script></body></html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, mapHtml, "text/html", "UTF-8", null)

        // JavaScript interface
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onLocationSelected(lat: Double, lng: Double) {
                selectedLat = lat
                selectedLng = lng
                runOnUiThread {
                    locationInfo.text = "📍 %.6f, %.6f".format(lat, lng)
                }
            }
        }, "android")

        btnBack.setOnClickListener { finish() }
        btnSend.setOnClickListener {
            val text = "📍 https://maps.google.com/?q=$selectedLat,$selectedLng"
            MainActivity.sendCallSignal("""{"type":"location","lat":$selectedLat,"lng":$selectedLng}""")
            // Отправляем как обычное сообщение через MainActivity
            val intent = android.content.Intent().apply {
                putExtra("location_text", text)
            }
            setResult(RESULT_OK, intent)
            finish()
        }

        locationInfo.text = "📍 %.6f, %.6f".format(selectedLat, selectedLng)
    }
}
