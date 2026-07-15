package com.hermesrelay.agent.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermesrelay.agent.data.PreferencesManager
import com.hermesrelay.agent.data.model.LoginRequest
import com.hermesrelay.agent.data.model.LoginResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val preferencesManager = PreferencesManager(application)
    private val gson = Gson()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun updateEmail(value: String) { _email.value = value }
    fun updatePassword(value: String) { _password.value = value }

    fun login(httpUrl: String) {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _error.value = "Email and password are required"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    performLogin(httpUrl.trimEnd('/') + "/api/v1/auth/login")
                }
                if (result != null) {
                    preferencesManager.saveToken(result.access_token)
                    _isLoggedIn.value = true
                } else {
                    _error.value = "Login failed"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                _error.value = e.message ?: "Connection failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun performLogin(url: String): LoginResponse? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val body = gson.toJson(LoginRequest(_email.value, _password.value))
        conn.outputStream.write(body.toByteArray())

        val code = conn.responseCode
        if (code == 200) {
            val response = conn.inputStream.bufferedReader().readText()
            return gson.fromJson(response, LoginResponse::class.java)
        }
        val errorBody = try { conn.errorStream?.bufferedReader()?.readText() } catch (e: Exception) { null }
        _error.value = errorBody ?: "Server returned $code"
        return null
    }

    fun clearError() {
        _error.value = null
    }
}
