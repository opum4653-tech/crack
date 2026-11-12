package com.andorid.crackmaster.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * HTTP client aligned to api_config.json.
 *
 * All endpoints: POST application/x-www-form-urlencoded.
 * Error response field: "reason" (not "error") — per api_config.json §license.response_error.
 * Timeouts: 8s connect, 12s read — matching binary curl config.
 */
object CrackMasterApiClient {

    // ── /api/verify.php ──────────────────────────────────────────────────────
    /**
     * POST user_key + serial + game → { status: true/false, data: {...}, reason: "..." }
     * Used for Stage 1 initial app login (binary not running yet).
     */
    suspend fun verifyLicense(
        userKey: String,
        serial: String,
        game: String = ApiConfig.GAME_TARGET
    ): Result<LicenseVerifyResponse> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("${ApiConfig.BASE_URL}${ApiConfig.VERIFY_ENDPOINT}")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod      = "POST"
                connectTimeout     = ApiConfig.CONNECT_TIMEOUT_MILLIS
                readTimeout        = ApiConfig.TIMEOUT_MILLIS
                doInput            = true
                doOutput           = true
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("User-Agent",   "CrackMaster-Launcher/2.0")
                setRequestProperty("Accept",       "application/json")
            }

            // POST body: user_key=&serial=&game=   (api_config.json §license.request)
            val body = buildString {
                append(ApiConfig.FIELD_USER_KEY).append("=").append(URLEncoder.encode(userKey.trim(), "UTF-8"))
                append("&").append(ApiConfig.FIELD_SERIAL).append("=").append(URLEncoder.encode(serial.trim(), "UTF-8"))
                append("&").append(ApiConfig.FIELD_GAME).append("=").append(URLEncoder.encode(game.trim(), "UTF-8"))
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body); it.flush() }

            val code    = conn.responseCode
            val stream  = if (code in 200..299) conn.inputStream else conn.errorStream
            val text    = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            if (text.isBlank()) {
                return@withContext Result.failure(Exception("Empty response (HTTP $code)"))
            }

            val json   = JSONObject(text)
            val status = json.optBoolean(ApiConfig.RESP_STATUS, false)
            // Error field is "reason" per api_config.json §license.response_error
            val reason = json.optString(ApiConfig.RESP_REASON, null)

            val licData = if (json.has(ApiConfig.RESP_DATA) && !json.isNull(ApiConfig.RESP_DATA)) {
                val d = json.getJSONObject(ApiConfig.RESP_DATA)
                LicenseData(
                    modname      = d.optString("modname",      null),
                    mod_status   = d.optString("mod_status",   null),
                    credit       = d.optString("credit",       null),
                    game         = d.optString("game",         null),
                    expired_date = d.optString("expired_date", null), // "Y-m-d H:i:s" UTC+6
                    real         = d.optString("real",         null),
                    token        = d.optString("token",        null)
                )
            } else null

            Result.success(LicenseVerifyResponse(status = status, reason = reason, data = licData))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    // ── /reseller_api.php?action=reset_key ───────────────────────────────────
    /**
     * POST license_key → { status: "success"/"error", message, code, data: {...} }
     * api_config.json §reset_device
     */
    suspend fun resetDeviceLock(licenseKey: String): Result<ResetKeyResponse> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("${ApiConfig.BASE_URL}${ApiConfig.RESET_KEY_ENDPOINT}")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod  = "POST"
                connectTimeout = ApiConfig.CONNECT_TIMEOUT_MILLIS
                readTimeout    = ApiConfig.TIMEOUT_MILLIS
                doInput        = true
                doOutput       = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("User-Agent",   "CrackMaster-Launcher/2.0")
            }

            val body = "${ApiConfig.FIELD_LICENSE_KEY}=" + URLEncoder.encode(licenseKey.trim(), "UTF-8")
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body); it.flush() }

            val code  = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text  = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            val json    = JSONObject(text)
            val status  = json.optString("status",  "error")
            val message = json.optString("message", null)
            val errCode = json.optString("code",    null)

            val resetData = if (json.has("data") && !json.isNull("data")) {
                val d = json.getJSONObject("data")
                ResetKeyData(
                    license_key        = d.optString("license_key",        null),
                    game               = d.optString("game",               null),
                    resets_used_today  = if (d.has("resets_used_today"))  d.getInt("resets_used_today")  else null,
                    resets_left_today  = if (d.has("resets_left_today"))  d.getInt("resets_left_today")  else null,
                    daily_limit        = if (d.has("daily_limit"))         d.getInt("daily_limit")        else null
                )
            } else null

            Result.success(ResetKeyResponse(status = status, message = message, code = errCode, data = resetData))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    // ── /reseller_api.php?action=key_info ────────────────────────────────────
    /**
     * POST license_key → { status: "success"/"error", data: { state, expires_at, ... } }
     * api_config.json §key_info  (optional — for showing expiry in app)
     */
    suspend fun getKeyInfo(licenseKey: String): Result<KeyInfoResponse> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("${ApiConfig.BASE_URL}${ApiConfig.KEY_INFO_ENDPOINT}")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod  = "POST"
                connectTimeout = ApiConfig.CONNECT_TIMEOUT_MILLIS
                readTimeout    = ApiConfig.TIMEOUT_MILLIS
                doInput        = true
                doOutput       = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("User-Agent",   "CrackMaster-Launcher/2.0")
            }

            val body = "${ApiConfig.FIELD_LICENSE_KEY}=" + URLEncoder.encode(licenseKey.trim(), "UTF-8")
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body); it.flush() }

            val code   = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text   = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            val json   = JSONObject(text)
            val status = json.optString("status", "error")

            val infoData = if (json.has("data") && !json.isNull("data")) {
                val d = json.getJSONObject("data")
                KeyInfoData(
                    license_key        = d.optString("license_key",        null),
                    game               = d.optString("game",               null),
                    duration           = d.optString("duration",           null),
                    state              = d.optString("state",              null),
                    device_bound       = if (d.has("device_bound"))        d.getBoolean("device_bound")   else null,
                    expires_at         = d.optString("expires_at",         null),
                    resets_used_today  = if (d.has("resets_used_today"))   d.getInt("resets_used_today")  else null,
                    resets_left_today  = if (d.has("resets_left_today"))   d.getInt("resets_left_today")  else null,
                    daily_limit        = if (d.has("daily_limit"))         d.getInt("daily_limit")        else null
                )
            } else null

            Result.success(KeyInfoResponse(status = status, data = infoData))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }
}
