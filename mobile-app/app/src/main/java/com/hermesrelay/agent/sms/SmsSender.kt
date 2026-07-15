package com.hermesrelay.agent.sms

import android.telephony.SmsManager
import android.util.Log
import java.util.concurrent.Executors

class SmsSender {
    companion object {
        private const val TAG = "SmsSender"
    }

    private val executor = Executors.newSingleThreadExecutor()

    fun sendSms(
        phoneNumber: String,
        message: String,
        onResult: (success: Boolean, status: String) -> Unit
    ) {
        executor.execute {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d(TAG, "SMS sent to $phoneNumber")
                onResult(true, "sent")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS", e)
                onResult(false, "failed: ${e.message}")
            }
        }
    }
}
