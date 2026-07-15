package com.hermesrelay.agent.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermesrelay.agent.data.PreferencesManager
import com.hermesrelay.agent.data.model.WsMessage
import com.hermesrelay.agent.service.SmsRelayService
import com.hermesrelay.agent.websocket.WebSocketClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val gson = Gson()
    private val scope = viewModelScope

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _smsLog = MutableStateFlow<List<SmsLogEntry>>(emptyList())
    val smsLog: StateFlow<List<SmsLogEntry>> = _smsLog

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private var webSocketClient: WebSocketClient? = null

    fun startService() {
        val context = getApplication<Application>()
        val intent = Intent(context, SmsRelayService::class.java).apply {
            action = SmsRelayService.ACTION_START
        }
        context.startForegroundService(intent)
        _isServiceRunning.value = true

        scope.launch {
            val token = preferencesManager.getToken() ?: return@launch
            val serverUrl = preferencesManager.getServerUrl()

            webSocketClient = WebSocketClient(gson, scope).apply {
                connect(serverUrl, token)
                launch {
                    connectionState.collect { state ->
                        _connectionStatus.value = when (state) {
                            WebSocketClient.ConnectionState.DISCONNECTED -> "Disconnected"
                            WebSocketClient.ConnectionState.CONNECTING -> "Connecting..."
                            WebSocketClient.ConnectionState.CONNECTED -> "Connected"
                        }
                    }
                }
                launch {
                    messages.collect { msg ->
                        when (msg.type) {
                            "send_sms" -> {
                                val entry = SmsLogEntry(
                                    jobId = msg.jobId ?: "",
                                    to = msg.to ?: "",
                                    message = msg.message ?: "",
                                    status = "received"
                                )
                                _smsLog.value = _smsLog.value + entry
                                sendSmsResult(msg.jobId ?: "", "sent")
                            }
                        }
                    }
                }
            }
        }
    }

    fun stopService() {
        val context = getApplication<Application>()
        val intent = Intent(context, SmsRelayService::class.java).apply {
            action = SmsRelayService.ACTION_STOP
        }
        context.startService(intent)
        webSocketClient?.disconnect()
        webSocketClient = null
        _isServiceRunning.value = false
        _connectionStatus.value = "Disconnected"
    }

    private fun sendSmsResult(jobId: String, status: String) {
        webSocketClient?.sendSmsResult(jobId, status)
        _smsLog.value = _smsLog.value.map {
            if (it.jobId == jobId) it.copy(status = status) else it
        }
    }

    fun logout() {
        stopService()
        scope.launch {
            preferencesManager.clearToken()
        }
    }

    data class SmsLogEntry(
        val jobId: String,
        val to: String,
        val message: String,
        val status: String
    )
}
