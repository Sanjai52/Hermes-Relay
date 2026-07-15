package com.hermesrelay.agent.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hermes_prefs")

class PreferencesManager(private val context: Context) {

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val serverUrl: Flow<String> = context.dataStore.data.map { it[SERVER_URL_KEY] ?: "ws://10.200.243.137:8000" }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(TOKEN_KEY) }
    }

    suspend fun getToken(): String? = context.dataStore.data.first()[TOKEN_KEY]

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL_KEY] = url }
    }

    suspend fun getServerUrl(): String = context.dataStore.data.first()[SERVER_URL_KEY] ?: "ws://10.200.243.137:8000"

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        const val DEFAULT_WS_URL = "ws://10.200.243.137:8000"
        const val DEFAULT_HTTP_URL = "http://10.200.243.137:8000"
    }
}
