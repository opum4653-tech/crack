package com.andorid.crackmaster.api

data class LicenseVerifyResponse(
    val status: Boolean,
    val reason: String? = null,
    val data: LicenseData? = null
)

data class LicenseData(
    val modname: String? = null,
    val mod_status: String? = null,
    val credit: String? = null,
    val game: String? = null,
    val expired_date: String? = null,
    val real: String? = null,
    val token: String? = null
)

data class ResetKeyResponse(
    val status: String,
    val message: String? = null,
    val code: String? = null
)
