package com.andorid.crackmaster.data

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.util.UUID

class SessionManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("crackmaster_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LICENSE = "cm_license_key"
        private const val KEY_TOKEN = "cm_token"
        private const val KEY_EXPIRED_DATE = "cm_expired_date"
        private const val KEY_HWID = "cm_cached_hwid"
        private const val KEY_IS_LOGGED_IN = "cm_is_logged_in"
        private const val KEY_SELECTED_MODE = "cm_selected_mode"
    }

    var licenseKey: String
        get() = prefs.getString(KEY_LICENSE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LICENSE, value.trim()).apply()

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var expiredDate: String
        get() = prefs.getString(KEY_EXPIRED_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EXPIRED_DATE, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var selectedMode: String
        get() = prefs.getString(KEY_SELECTED_MODE, "Bing Driver (FT/VIP & RT Dual)") ?: "Bing Driver (FT/VIP & RT Dual)"
        set(value) = prefs.edit().putString(KEY_SELECTED_MODE, value).apply()

    /**
     * Resolves unique Hardware ID (serial). Prefers ANDROID_ID, with secure fallback.
     */
    fun getHardwareId(): String {
        val cached = prefs.getString(KEY_HWID, null)
        if (!cached.isNullOrBlank()) return cached

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val finalHwid = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            androidId
        } else {
            UUID.randomUUID().toString().replace("-", "").take(16)
        }

        prefs.edit().putString(KEY_HWID, finalHwid).apply()
        return finalHwid
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRED_DATE)
            .apply()
    }
}
