package com.andorid.crackmaster.api

// ── /api/verify.php ──────────────────────────────────────────────────────────
// api_config.json §license.response_success / response_error
data class LicenseVerifyResponse(
    val status: Boolean,
    val reason: String? = null,       // error message when status=false
    val data: LicenseData? = null
)

data class LicenseData(
    val modname: String? = null,
    val mod_status: String? = null,
    val credit: String? = null,
    val game: String? = null,
    val expired_date: String? = null, // "Y-m-d H:i:s" UTC+6 (Asia/Dhaka)
    val real: String? = null,         // actual key string
    val token: String? = null         // md5(user_key + serial) lowercase hex
)

// ── /reseller_api.php?action=reset_key ───────────────────────────────────────
// api_config.json §reset_device
data class ResetKeyResponse(
    val status: String,              // "success" | "error"
    val message: String? = null,
    val code: String? = null,        // error code e.g. "DAILY_LIMIT_REACHED"
    val data: ResetKeyData? = null
)

data class ResetKeyData(
    val license_key: String? = null,
    val game: String? = null,
    val resets_used_today: Int? = null,
    val resets_left_today: Int? = null,
    val daily_limit: Int? = null
)

// ── /reseller_api.php?action=key_info ────────────────────────────────────────
// api_config.json §key_info
data class KeyInfoResponse(
    val status: String,              // "success" | "error"
    val data: KeyInfoData? = null
)

data class KeyInfoData(
    val license_key: String? = null,
    val game: String? = null,
    val duration: String? = null,
    val state: String? = null,       // "unused"|"active"|"expired"|"paused"|"banned"
    val device_bound: Boolean? = null,
    val expires_at: String? = null,  // "Y-m-d H:i:s" UTC+6
    val resets_used_today: Int? = null,
    val resets_left_today: Int? = null,
    val daily_limit: Int? = null
)
