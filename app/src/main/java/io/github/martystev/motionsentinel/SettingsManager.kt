package io.github.martystev.motionsentinel

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "motionsentinel_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getTelegramToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""
    fun getTelegramChatId(): String = prefs.getString(KEY_CHAT_ID, "") ?: ""

    fun setTelegramToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token.trim()).apply()
    }

    fun setTelegramChatId(chatId: String) {
        prefs.edit().putString(KEY_CHAT_ID, chatId.trim()).apply()
    }

    fun getSensitivity(): Int = prefs.getInt(KEY_SENSITIVITY, 60)

    fun setSensitivity(value: Int) {
        prefs.edit().putInt(KEY_SENSITIVITY, value).apply()
    }

    fun getRepeatIntervalSec(): Int = prefs.getInt(KEY_REPEAT_INTERVAL, 5)

    fun setRepeatIntervalSec(value: Int) {
        prefs.edit().putInt(KEY_REPEAT_INTERVAL, value.coerceIn(1, 60)).apply()
    }

    fun getGpsDelaySec(): Int = prefs.getInt(KEY_GPS_DELAY, 60)

    fun setGpsDelaySec(value: Int) {
        prefs.edit().putInt(KEY_GPS_DELAY, value.coerceIn(5, 300)).apply()
    }

    fun getMonitoringIntervalSec(): Int = prefs.getInt(KEY_MONITOR_INTERVAL, 10)

    fun setMonitoringIntervalSec(value: Int) {
        prefs.edit().putInt(KEY_MONITOR_INTERVAL, value.coerceIn(1, 60)).apply()
    }

    fun isHighFreqInAlert(): Boolean = prefs.getBoolean(KEY_HIGH_FREQ_ALERT, true)

    fun setHighFreqInAlert(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGH_FREQ_ALERT, enabled).apply()
    }

    fun getActivationDelaySec(): Int = prefs.getInt(KEY_ACTIVATION_DELAY, 10)

    fun setActivationDelaySec(value: Int) {
        prefs.edit().putInt(KEY_ACTIVATION_DELAY, value.coerceIn(0, 120)).apply()
    }

    fun isSirenEnabled(): Boolean = prefs.getBoolean(KEY_SIREN, false)

    fun setSirenEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIREN, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    companion object {
        private const val KEY_TOKEN = "telegram_token"
        private const val KEY_CHAT_ID = "telegram_chat_id"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_REPEAT_INTERVAL = "repeat_interval_sec"
        private const val KEY_GPS_DELAY = "gps_delay_sec"
        private const val KEY_MONITOR_INTERVAL = "monitor_interval_sec"
        private const val KEY_HIGH_FREQ_ALERT = "high_freq_alert"
        private const val KEY_ACTIVATION_DELAY = "activation_delay_sec"
        private const val KEY_SIREN = "siren_enabled"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
