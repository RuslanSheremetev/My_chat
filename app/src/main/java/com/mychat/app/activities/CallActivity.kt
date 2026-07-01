package com.mychat.app.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R
import okhttp3.*
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule

class CallActivity : AppCompatActivity() {
    companion object {
        var onSignalingMessage: ((String) -> Unit)? = null
    }
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    
    private var peerConnection: PeerConnection? = null
    private var factory: PeerConnectionFactory? = null
    private var ws: WebSocket? = null
    private var ringtonePlayer: android.media.MediaPlayer? = null
    private var isCaller = false
    private var incomingActions: android.view.View? = null
    private var btnAccept: ImageButton? = null
    private var btnDecline: ImageButton? = null
    private var callTimer: TextView? = null
    private var callStatus: TextView? = null
    
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                seconds++
                val mins = seconds / 60
                val secs = seconds % 60
                findViewById<TextView>(R.id.callTimer).text = "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
                handler.postDelayed(this, 1000)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        
        val name = intent.getStringExtra("name") ?: "Пользователь"
        findViewById<TextView>(R.id.callName).text = name
        findViewById<TextView>(R.id.callAvatar).text = name.take(1).uppercase()
        isCaller = intent.getBooleanExtra("caller", true)
        
        initWebRTC()
        
        incomingActions = findViewById(R.id.incomingActions)
        btnAccept = findViewById(R.id.btnAccept)
        btnDecline = findViewById(R.id.btnDecline)
        callTimer = findViewById(R.id.callTimer)
        callStatus = findViewById(R.id.callStatus)
        
        if (!isCaller) {
            // Входящий звонок
            callStatus?.text = "Входящий звонок..."
            incomingActions?.visibility = android.view.View.VISIBLE
            btnAccept?.setOnClickListener { acceptCall() }
            btnDecline?.setOnClickListener { endCall() }
        }
        
        findViewById<ImageButton>(R.id.btnEndCall).setOnClickListener { endCall() }
    }
    
        
    private fun initWebRTC() {
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions()
            )
            
            factory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(this).createAudioDeviceModule())
                .createPeerConnectionFactory()
            
            val iceServers = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("turn:2.26.71.102:3478")
                    .setUsername("user").setPassword("password").createIceServer()
            )
            
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
            
            peerConnection = factory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        ws?.send(JSONObject().apply {
                            put("type", "ice_candidate")
                            put("candidate", JSONObject().apply {
                                put("sdp", it.sdp)
                                put("sdpMid", it.sdpMid)
                                put("sdpMLineIndex", it.sdpMLineIndex)
                            })
                        }.toString())
                    }
                }
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    if (state == PeerConnection.IceConnectionState.CONNECTED) {
                        stopRingtone()
                        runOnUiThread {
                            findViewById<TextView>(R.id.callStatus).visibility = android.view.View.GONE
                            findViewById<TextView>(R.id.callTimer).visibility = android.view.View.VISIBLE
                            isRunning = true
                            handler.post(timerRunnable)
                        }
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
            })
            
            if (isCaller) {
                createOffer()
                playRingtone(true)  // Гудки
            } else {
                playRingtone(false)  // Рингтон
            }
            
        } catch (e: Exception) {
            t("WebRTC: ${e.message}")
        }
    }
    
    private fun acceptCall() {
        incomingActions?.visibility = android.view.View.GONE
        callStatus?.text = "Соединение..."
        initWebRTC()
        // Отправляем answer (после того как создан peerConnection)
        handler.postDelayed({
            peerConnection?.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    peerConnection?.setLocalDescription(this, sdp)
                    ws?.send(JSONObject().apply {
                        put("type", "call_answer")
                        put("to", intent.getStringExtra("name"))
                        put("sdp", JSONObject().apply {
                            put("type", sdp?.type?.canonicalForm())
                            put("description", sdp?.description)
                        })
                    }.toString())
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) { t("Answer: $error") }
                override fun onSetFailure(error: String?) {}
            }, MediaConstraints())
        }, 500)
    }
    
    private fun playRingtone(isOutgoing: Boolean) {
        try {
            val resId = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            ringtonePlayer = android.media.MediaPlayer().apply {
                setDataSource(this@CallActivity, resId)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {}
    }
    
    private fun stopRingtone() {
        ringtonePlayer?.apply {
            stop()
            release()
        }
        ringtonePlayer = null
    }
    
        
    private fun connectSignaling() {
        val client = OkHttpClient()
        val prefs = getSharedPreferences("mychat_prefs", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val me = prefs.getString("username", "") ?: ""
        val request = Request.Builder().url("ws://2.26.71.102:8000/ws/$me?token=$token").build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = JSONObject(text)
                runOnUiThread {
                    when (msg.optString("type")) {
                        "call_answer" -> onAnswerReceived(msg.optJSONObject("sdp"))
                        "ice_candidate" -> onIceCandidate(msg.optJSONObject("candidate"))
                        "call_end" -> endCall()
                    }
                }
            }
        })
    }
    
    private fun createOffer() {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(this, sdp)
                ws?.send(JSONObject().apply {
                    put("type", "call_offer")
                    put("to", intent.getStringExtra("name"))
                    put("sdp", JSONObject().apply {
                        put("type", sdp?.type?.canonicalForm())
                        put("description", sdp?.description)
                    })
                }.toString())
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { t("Offer: $error") }
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }
    
    private fun onAnswerReceived(sdp: JSONObject?) {
        sdp ?: return
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                runOnUiThread { callStatus?.text = "Соединение..." }
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, SessionDescription(
            SessionDescription.Type.fromCanonicalForm(sdp.optString("type")),
            sdp.optString("description")
        ))
    }
    
    private fun onAnswerReceivedOLD(sdp: JSONObject?) {
        sdp ?: return
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, SessionDescription(
            SessionDescription.Type.fromCanonicalForm(sdp.optString("type")),
            sdp.optString("description")
        ))
    }
    
    private fun onIceCandidate(candidate: JSONObject?) {
        candidate ?: return
        peerConnection?.addIceCandidate(IceCandidate(
            candidate.optString("sdpMid"),
            candidate.optInt("sdpMLineIndex"),
            candidate.optString("sdp")
        ))
    }
    
    private fun endCall() {
        stopRingtone()
        isRunning = false
        ws?.send(JSONObject().apply { put("type", "call_end") }.toString())
        peerConnection?.close()
        factory?.dispose()
        finish()
    }
    
    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        stopRingtone()
        super.onDestroy()
    }
    
    private fun t(msg: String) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
}
