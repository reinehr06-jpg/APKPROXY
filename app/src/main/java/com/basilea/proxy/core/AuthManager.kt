package com.basilea.proxy.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val churchName: String? = null,
    val relayUrl: String? = null,
    val sessionToken: String? = null
)

class AuthManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("basilea_proxy_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "AuthManager"
        private const val AUTH_ENDPOINT = "https://api.basileavendor.com/api/proxy/auth" // Ajuste conforme necessário
    }

    suspend fun authenticate(whatsapp: String): AuthResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL(AUTH_ENDPOINT)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonOutput = JSONObject().apply {
                put("whatsapp", whatsapp)
            }.toString()

            conn.outputStream.use { it.write(jsonOutput.toByteArray()) }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val auth = AuthResponse(
                    success = true,
                    churchName = json.getString("church_name"),
                    relayUrl = json.getString("relay_url"),
                    sessionToken = json.getString("session_token")
                )
                
                saveCredentials(auth)
                auth
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val message = error?.let { JSONObject(it).optString("message") } ?: "Erro desconhecido"
                AuthResponse(success = false, message = message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auth Error: ${e.message}")
            AuthResponse(success = false, message = "Falha na conexão: ${e.message}")
        }
    }

    private fun saveCredentials(auth: AuthResponse) {
        prefs.edit().apply {
            putString("church_name", auth.churchName)
            putString("relay_url", auth.relayUrl)
            putString("session_token", auth.sessionToken)
            apply()
        }
    }

    fun getChurchName(): String? = prefs.getString("church_name", null)
    fun getRelayUrl(): String? = prefs.getString("relay_url", null)
    fun getSessionToken(): String? = prefs.getString("session_token", null)
    
    fun logout() {
        prefs.edit().clear().apply()
    }
}
