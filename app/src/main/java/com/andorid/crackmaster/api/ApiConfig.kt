package com.andorid.crackmaster.api

object ApiConfig {
    // ── License server (from api_config.json) ───────────────────────────────
    const val BASE_URL: String                = "https://crackmaster.shop"
    const val VERIFY_ENDPOINT: String         = "/api/verify.php"
    const val RESET_KEY_ENDPOINT: String      = "/reseller_api.php?action=reset_key"
    const val KEY_INFO_ENDPOINT: String       = "/reseller_api.php?action=key_info"

    // POST fields (api_config.json §license.request)
    const val FIELD_USER_KEY: String          = "user_key"
    const val FIELD_SERIAL: String            = "serial"
    const val FIELD_GAME: String              = "game"
    const val FIELD_LICENSE_KEY: String       = "license_key"

    // Game target value
    const val GAME_TARGET: String             = "FreeFire"

    // Response keys (api_config.json §license.response_success / response_error)
    const val RESP_STATUS: String             = "status"
    const val RESP_REASON: String             = "reason"
    const val RESP_DATA: String               = "data"

    // Network timeouts matching binary (12s connect, 12s read)
    const val TIMEOUT_MILLIS: Int             = 12_000
    const val CONNECT_TIMEOUT_MILLIS: Int     = 8_000

    // ── Binary download server ──────────────────────────────────────────────
    const val SMMTOR_BASE_URL: String         = "https://ninmods.smmtor.com"
    const val BINARY_PATH: String             = "downloads/crack/gemini-cli.zip"
}
