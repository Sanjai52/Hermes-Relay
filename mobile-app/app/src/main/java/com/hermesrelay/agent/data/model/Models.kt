package com.hermesrelay.agent.data.model

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user: UserInfo
)

data class UserInfo(
    val id: String,
    val email: String,
    val created_at: String
)

data class WsMessage(
    val type: String,
    val token: String? = null,
    val jobId: String? = null,
    val to: String? = null,
    val message: String? = null,
    val status: String? = null,
    val deviceId: String? = null,
    val device_name: String? = null,
    val error: String? = null
)

data class SmsJob(
    val jobId: String,
    val to: String,
    val message: String
)

data class ApiError(
    val detail: String
)

data class SmsLogEntry(
    val jobId: String,
    val to: String,
    val message: String,
    val status: String
)
