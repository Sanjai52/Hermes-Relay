package com.hermesrelay.agent.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hermesrelay.agent.HermesApp
import com.hermesrelay.agent.MainActivity
import com.hermesrelay.agent.R
import com.hermesrelay.agent.data.PreferencesManager
import com.hermesrelay.agent.data.model.WsMessage
import com.hermesrelay.agent.sms.SmsSender
import com.hermesrelay.agent.websocket.WebSocketClient
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class SmsRelayService : Service() {
    companion object {
        const val TAG = "SmsRelayService"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.hermesrelay.agent.START"
        const val ACTION_STOP = "com.hermesrelay.agent.STOP"
    }

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var smsSender: SmsSender
    private lateinit var gson: Gson
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var messageJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        gson = Gson()
        smsSender = SmsSender()
        webSocketClient = WebSocketClient(gson, scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                connectWebSocket()
            }
            ACTION_STOP -> {
                disconnectAndStop()
            }
        }
        return START_STICKY
    }

    private fun connectWebSocket() {
        scope.launch {
            val token = preferencesManager.getToken() ?: return@launch
            val serverUrl = preferencesManager.getServerUrl()

            messageJob?.cancel()
            messageJob = scope.launch {
                webSocketClient.messages.collect { msg ->
                    handleMessage(msg)
                }
            }

            webSocketClient.connect(serverUrl, token)
        }
    }

    private suspend fun handleMessage(msg: WsMessage) {
        when (msg.type) {
            "send_sms" -> {
                val jobId = msg.jobId ?: return
                val to = msg.to ?: return
                val message = msg.message ?: return

                smsSender.sendSms(to, message) { success, status ->
                    val resultStatus = if (success) "sent" else status
                    webSocketClient.sendSmsResult(jobId, resultStatus)
                }
            }
        }
    }

    private fun disconnectAndStop() {
        webSocketClient.disconnect()
        messageJob?.cancel()
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, HermesApp.CHANNEL_ID)
            .setContentTitle("Hermes Relay")
            .setContentText("SMS relay service is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
