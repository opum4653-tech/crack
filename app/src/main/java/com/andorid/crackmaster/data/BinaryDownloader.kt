package com.andorid.crackmaster.data

import android.content.Context
import com.andorid.crackmaster.api.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

object BinaryDownloader {

    fun getBinaryFile(context: Context): File {
        val binDir = File(context.filesDir, "bin").also { if (!it.exists()) it.mkdirs() }
        return File(binDir, "gemini-cli")
    }

    suspend fun downloadBinaryIfNeeded(
        context: Context,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val binFile = getBinaryFile(context)
        if (binFile.exists() && binFile.length() > 1024) {
            binFile.setExecutable(true, false)
            onProgress(100)
            return@withContext Result.success(binFile)
        }

        downloadBinaryFromServer(context, onProgress)
    }

    suspend fun downloadBinaryFromServer(
        context: Context,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val binDir = File(context.filesDir, "bin").also { if (!it.exists()) it.mkdirs() }
        val targetBin = File(binDir, "gemini-cli")

        val candidateUrls = listOf(
            "${ApiConfig.SMMTOR_BASE_URL}/${ApiConfig.BINARY_PATH}",
            "${ApiConfig.SMMTOR_BASE_URL}/downloads/crack/gemini-cli.zip"
        )

        var conn: HttpURLConnection? = null
        var inputStream: InputStream? = null
        val tempZip = File(binDir, "download_temp.zip")

        try {
            var connected = false
            for (urlStr in candidateUrls) {
                try {
                    val url = URL(urlStr)
                    val candidateConn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = ApiConfig.TIMEOUT_MILLIS
                        readTimeout = ApiConfig.TIMEOUT_MILLIS
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "CrackMaster-Launcher/2.0")
                        setRequestProperty("Accept", "*/*")
                    }
                    if (candidateConn.responseCode in 200..299) {
                        conn = candidateConn
                        connected = true
                        break
                    } else {
                        candidateConn.disconnect()
                    }
                } catch (_: Exception) {}
            }

            if (!connected || conn == null) {
                return@withContext Result.failure(Exception("Unable to reach mod binary server at ninmods.smmtor.com"))
            }

            inputStream = conn.inputStream
            val totalLength = conn.contentLength.toLong()
            val out = FileOutputStream(tempZip)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (totalLength > 0) {
                    val progress = ((totalRead * 100) / totalLength).toInt().coerceIn(0, 100)
                    onProgress(progress)
                }
            }
            out.flush()
            out.close()

            // Extract binary if zip, or rename directly if raw executable
            if (isZipFile(tempZip)) {
                unzipBinary(tempZip, binDir)
                tempZip.delete()
            } else {
                if (targetBin.exists()) targetBin.delete()
                tempZip.renameTo(targetBin)
            }

            if (targetBin.exists()) {
                targetBin.setExecutable(true, false)
                targetBin.setReadable(true, false)
                ShellExecutor.execute("chmod 755 '${targetBin.absolutePath}'")
                onProgress(100)
                Result.success(targetBin)
            } else {
                Result.failure(Exception("Binary gemini-cli could not be located in package."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            conn?.disconnect()
        }
    }

    private fun isZipFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { stream ->
                val b = ByteArray(4)
                stream.read(b) == 4 && b[0] == 0x50.toByte() && b[1] == 0x4B.toByte() && b[2] == 0x03.toByte() && b[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun unzipBinary(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val cleanName = File(entry.name).name
                    val outFile = if (cleanName.contains("gemini-cli") || cleanName.endsWith(".sh")) {
                        File(targetDir, "gemini-cli")
                    } else {
                        File(targetDir, cleanName)
                    }
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                    outFile.setExecutable(true, false)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
