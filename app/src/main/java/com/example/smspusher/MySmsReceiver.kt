package com.example.smspusher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class MySmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LogManager.addLog(context, "onReceive triggered")
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.originatingAddress
                val message = sms.messageBody
                LogManager.addLog(context, "SMS received from: $sender, message: $message")

                val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                val endpointUrl = prefs.getString(MainActivity.ENDPOINT_URL_KEY, "https://pusherworker.example.com") ?: "https://pusherworker.example.com"
                val authToken = prefs.getString(MainActivity.AUTH_TOKEN_KEY, "") ?: ""

                sendSmsDataToEndpoint(context, endpointUrl, authToken, sender, message)
            }
        }
    }

    private fun sendSmsDataToEndpoint(context: Context, endpointUrl: String, authToken: String, sender: String?, message: String) {
        val client = OkHttpClient()

        val json = JSONObject().apply {
            put("sender", sender)
            put("message", message)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpointUrl)
            .post(requestBody)
            .header("Authorization", "Bearer $authToken")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    LogManager.addLog(context, "Successfully sent SMS data to endpoint")
                } else {
                    val errorBody = response.body?.string() ?: "No error body"
                    LogManager.addLog(context, "Failed to send SMS data. Response code: ${response.code}, Error body: $errorBody")
                }
            } catch (e: IOException) {
                LogManager.addLog(context, "Error sending SMS data: ${e.message}")
            } catch (e: Exception) {
                LogManager.addLog(context, "Unexpected error sending SMS data: ${e.message}")
            }
        }
    }
}
