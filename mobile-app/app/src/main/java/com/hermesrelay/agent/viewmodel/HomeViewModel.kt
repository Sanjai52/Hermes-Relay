package com.hermesrelay.agent.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermesrelay.agent.data.PreferencesManager
import com.hermesrelay.agent.data.model.SmsLogEntry
import com.hermesrelay.agent.service.SmsRelayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _smsLog = MutableStateFlow<List<SmsLogEntry>>(emptyList())
    val smsLog: StateFlow<List<SmsLogEntry>> = _smsLog

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private var boundService: SmsRelayService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SmsRelayService.LocalBinder
            boundService = binder.getService()
            boundService?.setConnectionCallback(object : SmsRelayService.ConnectionCallback {
                override fun onConnectionStateChanged(state: String) {
                    _connectionStatus.value = state
                }
                override fun onSmsLogEntry(entry: SmsLogEntry) {
                    _smsLog.value = _smsLog.value + entry
                }
            })
            _connectionStatus.value = boundService?.currentState ?: "Disconnected"
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    fun startService() {
        val context = getApplication<Application>()
        val intent = Intent(context, SmsRelayService::class.java).apply {
            action = SmsRelayService.ACTION_START
        }
        context.startForegroundService(intent)
        context.bindService(intent, serviceConnection, android.content.Context.BIND_AUTO_CREATE)
        _isServiceRunning.value = true
    }

    fun stopService() {
        val context = getApplication<Application>()
        context.unbindService(serviceConnection)
        boundService = null
        val intent = Intent(context, SmsRelayService::class.java).apply {
            action = SmsRelayService.ACTION_STOP
        }
        context.startService(intent)
        _isServiceRunning.value = false
        _connectionStatus.value = "Disconnected"
    }

    fun logout() {
        stopService()
        viewModelScope.launch {
            preferencesManager.clearToken()
        }
    }
}
