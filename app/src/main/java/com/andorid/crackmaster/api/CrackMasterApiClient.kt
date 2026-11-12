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

object CrackMasterApiClient {

    /**
     * Verifies license key with CrackMaster server (/api/verify.php).
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
                requestMethod = "POST"
                connectTimeout = ApiConfig.TIMEOUT_MILLIS
                readTimeout = ApiConfig.TIMEOUT_MILLIS
                doInput = true
                doOutput = true
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("User-Agent", "CrackMaster-Launcher/2.0")
                setRequestProperty("Accept", "application/json")
            }

            val postParams = buildString {
                append("user_key=").append(URLEncoder.encode(userKey.trim(), "UTF-8"))
                append("&serial=").append(URLEncoder.encode(serial.trim(), "UTF-8"))
                append("&game=").append(URLEncoder.encode(game.trim(), "UTF-8"))
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(postParams)
                writer.flush()
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

            if (responseText.isBlank()) {
                return@withContext Result.failure(Exception("Empty response from server (HTTP $responseCode)"))
            }

            val json = JSONObject(responseText)
            val status = json.optBoolean("status", false)
            val reason = json.optString("reason", null)

            val licenseData = if (json.has("data") && !json.isNull("data")) {
                val dataObj = json.getJSONObject("data")
                LicenseData(
                    modname = dataObj.optString("modname", null),
                    mod_status = dataObj.optString("mod_status", null),
                    credit = dataObj.optString("credit", null),
                    game = dataObj.optString("game", null),
                    expired_date = dataObj.optString("expired_date", null),
                    real = dataObj.optString("real", null),
                    token = dataObj.optString("token", null)
                )
            } else null

            Result.success(LicenseVerifyResponse(status = status, reason = reason, data = licenseData))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Resets device lock for a license key.
     */
    suspend fun resetDeviceLock(licenseKey: String): Result<ResetKeyResponse> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("${ApiConfig.BASE_URL}${ApiConfig.RESET_KEY_ENDPOINT}")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = ApiConfig.TIMEOUT_MILLIS
                readTimeout = ApiConfig.TIMEOUT_MILLIS
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("User-Agent", "CrackMaster-Launcher/2.0")
            }

            val postParams = "license_key=" + URLEncoder.encode(licenseKey.trim(), "UTF-8")
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(postParams); it.flush() }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

            val json = JSONObject(responseText)
            val status = json.optString("status", "error")
            val message = json.optString("message", null)
            val code = json.optString("code", null)

            Result.success(ResetKeyResponse(status = status, message = message, code = code))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }
}
