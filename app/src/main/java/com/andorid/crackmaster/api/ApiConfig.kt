package com.andorid.crackmaster.api

object ApiConfig {
    const val BASE_URL: String = "https://crackmaster.shop"
    const val VERIFY_ENDPOINT: String = "/api/verify.php"
    const val RESET_KEY_ENDPOINT: String = "/reseller_api.php?action=reset_key"
    const val KEY_INFO_ENDPOINT: String = "/reseller_api.php?action=key_info"

    const val GAME_TARGET: String = "FreeFire"

    // Binary download server
    const val SMMTOR_BASE_URL: String = "https://ninmods.smmtor.com"
    const val BINARY_PATH: String = "downloads/crack/gemini-cli.zip"
    const val FALLBACK_BINARY_PATH: String = "downloads/crack/gemini-cli.zip"

    const val TIMEOUT_MILLIS: Int = 12000
}
