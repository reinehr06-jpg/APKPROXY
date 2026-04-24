package com.basilea.proxy.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.basilea.proxy.data.model.CheckinSessionEntity

class CheckinSessionManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(session: CheckinSessionEntity) {
        prefs.edit().apply {
            putString(KEY_ACCOUNT_CODE, session.accountCode)
            putString(KEY_EVENT_ID, session.eventId)
            putString(KEY_SECURE_EVENT_ID, session.secureEventId)
            putString(KEY_EVENT_NAME, session.eventName)
            putString(KEY_OPERATOR_NAME, session.operatorName)
            putLong(KEY_EXPIRES_AT, session.expiresAt)
            putLong(KEY_CREATED_AT, session.createdAt)
            apply()
        }
    }

    fun getSession(): CheckinSessionEntity? {
        val accountCode = prefs.getString(KEY_ACCOUNT_CODE, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        
        if (isExpired(expiresAt)) {
            clearSession()
            return null
        }

        return CheckinSessionEntity(
            accountCode = accountCode,
            eventId = prefs.getString(KEY_EVENT_ID, "") ?: "",
            secureEventId = prefs.getString(KEY_SECURE_EVENT_ID, "") ?: "",
            eventName = prefs.getString(KEY_EVENT_NAME, "") ?: "",
            operatorName = prefs.getString(KEY_OPERATOR_NAME, "") ?: "",
            expiresAt = expiresAt,
            createdAt = prefs.getLong(KEY_CREATED_AT, System.currentTimeMillis())
        )
    }

    fun isSessionValid(): Boolean {
        val session = getSession() ?: return false
        return !isExpired(session.expiresAt)
    }

    fun getDaysUntilExpiry(): Int {
        val session = getSession() ?: return 0
        val now = System.currentTimeMillis()
        val diff = session.expiresAt - now
        return (diff / (24 * 60 * 60 * 1000)).toInt()
    }

    fun shouldShowExpiryWarning(): Boolean {
        val days = getDaysUntilExpiry()
        return days in 1..3
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun isExpired(expiresAt: Long): Boolean {
        return System.currentTimeMillis() > expiresAt
    }

    companion object {
        private const val PREFS_NAME = "checkin_session"
        private const val KEY_ACCOUNT_CODE = "account_code"
        private const val KEY_EVENT_ID = "event_id"
        private const val KEY_SECURE_EVENT_ID = "secure_event_id"
        private const val KEY_EVENT_NAME = "event_name"
        private const val KEY_OPERATOR_NAME = "operator_name"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_CREATED_AT = "created_at"

        const val SESSION_DURATION_MS = 15L * 24 * 60 * 60 * 1000
    }
}