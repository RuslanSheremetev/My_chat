package com.mychat.app.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R
import org.webrtc.*

class CallActivity : AppCompatActivity() {
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    
    private var peerConnection: PeerConnection? = null
    private var factory: PeerConnectionFactory? = null
    
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
        
        initWebRTC()
        
        findViewById<ImageButton>(R.id.btnEndCall).setOnClickListener { endCall() }
        findViewById<ImageButton>(R.id.btnSpeaker).setOnClickListener { t("Динамик") }
        findViewById<ImageButton>(R.id.btnMic).setOnClickListener { t("Микрофон") }
    }
    
    private fun initWebRTC() {
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(this)
                    .setFieldTrials("")
                    .createInitializationOptions()
            )
            
            val options = PeerConnectionFactory.Options()
            factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(this).createAudioDeviceModule())
                .createPeerConnectionFactory()
            
            val audioSource = factory?.createAudioSource(MediaConstraints())
            
            val iceServers = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
            
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            }
            
            val audioTrack = factory?.createAudioTrack("audio", audioSource)
            peerConnection = factory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                init {
                    audioTrack?.let { peerConnection?.addTrack(it) }
                }
                override fun onIceCandidate(candidate: IceCandidate?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    if (state == PeerConnection.IceConnectionState.CONNECTED) {
                        runOnUiThread {
                            findViewById<TextView>(R.id.callStatus).text = "Соединено"
                            handler.postDelayed({
                                findViewById<TextView>(R.id.callStatus).visibility = android.view.View.GONE
                                findViewById<TextView>(R.id.callTimer).visibility = android.view.View.VISIBLE
                                isRunning = true
                                handler.post(timerRunnable)
                            }, 500)
                        }
                    }
                }
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onRemoveStream(stream: MediaStream?) {}
            })
            
            startCall()
        } catch (e: Exception) {
            t("WebRTC: ${e.message}")
        }
    }
    
    private fun startCall() {
        findViewById<TextView>(R.id.callStatus).text = "Соединение..."
        findViewById<TextView>(R.id.callStatus).visibility = android.view.View.VISIBLE
    }
    
    private fun endCall() {
        isRunning = false
        peerConnection?.close()
        factory?.dispose()
        finish()
    }
    
    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        peerConnection?.close()
        factory?.dispose()
        super.onDestroy()
    }
    
    private fun t(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
