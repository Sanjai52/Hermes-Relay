package com.hermesrelay.agent.websocket

import android.util.Log
import com.google.gson.Gson
import com.hermesrelay.agent.data.model.WsMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketClient(
    private val gson: Gson,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "WebSocketClient"
        private const val PING_INTERVAL_MS = 30_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(PING_INTERVAL_MS, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var ws: WebSocket? = null
    private var reconnectJob: Job? = null
    private var currentToken: String? = null
    private var currentUrl: String? = null
    private var reconnectAttempt = 0

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messages = MutableSharedFlow<WsMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<WsMessage> = _messages

    fun connect(wsUrl: String, token: String) {
        currentUrl = wsUrl
        currentToken = token
        reconnectAttempt = 0
        doConnect(wsUrl, token)
    }

    private fun doConnect(wsUrl: String, token: String) {
        _connectionState.value = ConnectionState.CONNECTING
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened")
                reconnectAttempt = 0
                val authMsg = WsMessage(type = "auth", token = token)
                webSocket.send(gson.toJson(authMsg))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = gson.fromJson(text, WsMessage::class.java)
                    when (msg.type) {
                        "auth_ok" -> {
                            _connectionState.value = ConnectionState.CONNECTED
                            scope.launch {
                                _messages.emit(msg)
                            }
                        }
                        "pong" -> { }
                        else -> {
                            scope.launch { _messages.emit(msg) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    fun sendSmsResult(jobId: String, status: String) {
        val msg = WsMessage(type = "sms_result", jobId = jobId, status = status)
        ws?.send(gson.toJson(msg))
    }

    fun disconnect() {
        reconnectJob?.cancel()
        ws?.close(1000, "Client closing")
        ws = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun scheduleReconnect() {
        if (currentUrl == null || currentToken == null) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = calculateDelay(reconnectAttempt)
            reconnectAttempt++
            Log.d(TAG, "Reconnecting in ${delay}ms (attempt $reconnectAttempt)")
            delay(delay)
            doConnect(currentUrl!!, currentToken!!)
        }
    }

    private fun calculateDelay(attempt: Int): Long {
        val delay = INITIAL_RECONNECT_DELAY_MS * (1 shl attempt.coerceAtMost(5))
        return delay.coerceAtMost(MAX_RECONNECT_DELAY_MS)
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}
