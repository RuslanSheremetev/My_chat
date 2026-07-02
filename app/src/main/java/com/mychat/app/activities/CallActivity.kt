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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.concurrent.thread
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
    private var me = ""
    private var remoteSdp: JSONObject? = null
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
        t("CallActivity started")
        setContentView(R.layout.activity_call)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 100)
            }
        }
        
        val name = intent.getStringExtra("name") ?: "Пользователь"
        val sdpStr = intent.getStringExtra("sdp")
        if (!sdpStr.isNullOrEmpty()) {
            try {
                remoteSdp = JSONObject(sdpStr)
            } catch (_: Exception) {}
        }
        findViewById<TextView>(R.id.callName).text = name
        findViewById<TextView>(R.id.callAvatar).text = name.take(1).uppercase()
        isCaller = intent.getBooleanExtra("caller", true)
        
        connectSignaling()
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
                try {
                    val audioSource = factory?.createAudioSource(MediaConstraints())
                    val audioTrack = factory?.createAudioTrack("audio0", audioSource)
                    peerConnection?.addTrack(audioTrack)
                    logToServer("audio track added")
                } catch (e: Exception) {
                    logToServer("audio error: ${e.message}")
                }
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        com.mychat.app.MainActivity.sendCallSignal(JSONObject().apply {
                            put("type", "ice_candidate")
                            put("from", me)
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
                handler.postDelayed({
                    if (isRunning && peerConnection?.connectionState() != PeerConnection.PeerConnectionState.CONNECTED) {
                        logToServer("call timeout")
                        endCall()
                    }
                }, 30000)
            } else {
                playRingtone(false)  // Рингтон
            }
            
        } catch (e: Exception) {
            logToServer("WebRTC error: ${e.message}")
            t("WebRTC: ${e.message}")
        }
    }
    
    private fun acceptCall() {
        logToServer("accepted")
        incomingActions?.visibility = android.view.View.GONE
        callStatus?.text = "Соединение..."
        // Устанавливаем remote SDP из offer
        remoteSdp?.let { sdp ->
            val type = SessionDescription.Type.fromCanonicalForm(sdp.optString("type", "offer"))
            val desc = sdp.optString("description", "")
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() {
                    logToServer("remote desc set, creating answer")
                    peerConnection?.createAnswer(object : SdpObserver {
                        override fun onCreateSuccess(sdp: SessionDescription?) {
                            logToServer("answer created")
                            peerConnection?.setLocalDescription(this, sdp)
                            com.mychat.app.MainActivity.sendCallSignal(JSONObject().apply {
                                put("type", "call_answer")
                                put("from", me)
                                put("to", intent.getStringExtra("name"))
                                put("sdp", JSONObject().apply {
                                    put("type", sdp?.type?.canonicalForm())
                                    put("description", sdp?.description)
                                })
                            }.toString())
                        }
                        override fun onSetSuccess() {}
                        override fun onCreateFailure(error: String?) { logToServer("Answer error: $error") }
                        override fun onSetFailure(error: String?) {}
                    }, MediaConstraints())
                }
                override fun onCreateFailure(error: String?) {}
                override fun onSetFailure(error: String?) {}
            }, SessionDescription(type, desc))
        }
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
        logToServer("connecting")
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        me = prefs.getString("username", "") ?: ""
        ws = com.mychat.app.MainActivity.mainWs
        com.mychat.app.MainActivity.onSignalingMessage = { text ->
            try {
                val msg = JSONObject(text)
                runOnUiThread {
                    when (msg.optString("type")) {
                        "call_offer" -> {
                            remoteSdp = msg.optJSONObject("sdp")
                            logToServer("got remote offer")
                        }
                        "call_answer" -> onAnswerReceived(msg.optJSONObject("sdp"))
                        "ice_candidate" -> onIceCandidate(msg.optJSONObject("candidate"))
                        "call_end" -> endCall()
                    }
                }
            } catch (_: Exception) {}
        }
    }
    
    private fun createOffer() {
        logToServer("creating offer")
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                logToServer("offer created")
                peerConnection?.setLocalDescription(this, sdp)
                com.mychat.app.MainActivity.sendCallSignal(JSONObject().apply {
                    put("type", "call_offer")
                    put("from", me)
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
        logToServer("ending")
        stopRingtone()
        isRunning = false
        com.mychat.app.MainActivity.sendCallSignal(JSONObject().apply { put("type", "call_end"); put("from", me); put("to", intent.getStringExtra("name")) }.toString())
        handler.postDelayed({
            peerConnection?.close()
            factory?.dispose()
        }, 500)
        finish()
    }
    
    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        stopRingtone()
        super.onDestroy()
    }
    
    private fun logToServer(msg: String) {
        thread {
            try {
                val json = JSONObject().apply {
                    put("logs", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("timestamp", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
                            put("message", "CALL: $msg")
                            put("level", "INFO")
                        })
                    })
                }
                val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
                val token = prefs.getString("token", "") ?: ""
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url("http://2.26.71.102:8000/api/logs?token=$token").post(body).build()
                OkHttpClient().newCall(request).execute()
            } catch (e: Exception) {}
        }
    }
    
    private fun t(msg: String) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
}
