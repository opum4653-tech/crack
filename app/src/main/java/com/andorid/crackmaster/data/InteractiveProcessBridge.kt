package com.andorid.crackmaster.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File

/**
 * Starts the crack_master binary as a root interactive process (stdin/stdout piped).
 * License key is passed via stdin — the binary calls crackmaster.shop internally.
 * App reads stdout line-by-line to detect state transitions.
 *
 * Stdout signal protocol (from binary src/main.cpp):
 *   "[?] Enter API Access Key:"            → emit licensePromptEvent, wait for key via sendInput()
 *   "[-] Authentication failed: ..."       → emit errorEvent
 *   "[+] API Key validated successfully."  → isLicenseVerified = true
 *   "[?] Ready to load into memory. Awaiting START signal..."  → auto-send "START"
 *   "[+] Loading overlay engine into RAM..." → isRunning = true
 */
class InteractiveProcessBridge(
    private val context: Context,
    private val scope: CoroutineScope
) {
    // ── State flows ──────────────────────────────────────────────────────────
    private val _outputLines   = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 128)
    val outputLines: SharedFlow<String> = _outputLines.asSharedFlow()

    private val _licensePromptEvent = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val licensePromptEvent: SharedFlow<Unit> = _licensePromptEvent.asSharedFlow()

    private val _errorEvent = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 8)
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private val _isLicenseVerified = MutableStateFlow(false)
    val isLicenseVerified: StateFlow<Boolean> = _isLicenseVerified.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isStarted = MutableStateFlow(false)
    val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    // ── Process handles ──────────────────────────────────────────────────────
    private var process: Process? = null
    private var stdin: DataOutputStream? = null
    private var readJob: Job? = null

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Download binary (if needed), then spawn it as a root interactive process.
     * @param onDownloadProgress 0-100 progress for download phase, -1 = error
     */
    suspend fun start(
        hwid: String,
        savedKey: String,
        onDownloadProgress: (Int) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // 1. Download
        val downloadResult = BinaryDownloader.downloadBinaryIfNeeded(context, onDownloadProgress)
        if (downloadResult.isFailure) return@withContext downloadResult.map { }

        val binaryFile = downloadResult.getOrNull()!!
        binaryFile.setExecutable(true, false)

        // 2. Spawn root process with stdin/stdout piped
        return@withContext try {
            val envBlock = buildString {
                append("export CRACKMASTER_SERVER_URL='https://crackmaster.shop'\n")
                append("export NIN_DEVICE_HWID='$hwid'\n")
                if (savedKey.isNotBlank()) {
                    append("export NIN_LICENSE_KEY='${savedKey.trim()}'\n")
                }
            }
            val cmd = "${envBlock}exec '${binaryFile.absolutePath}'\n"

            val pb = ProcessBuilder("su")
                .redirectErrorStream(true)
                .start()

            process = pb
            stdin = DataOutputStream(pb.outputStream)

            // Write env + exec command
            stdin!!.writeBytes(cmd)
            stdin!!.flush()

            _isStarted.value = true

            // 3. Read stdout line-by-line in background
            readJob = scope.launch(Dispatchers.IO) {
                try {
                    pb.inputStream.bufferedReader().use { reader ->
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            _outputLines.emit(line)
                            handleLine(line)
                        }
                    }
                } catch (_: Exception) {}
                _isRunning.value = false
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Send a line to the binary's stdin (key or "START" signal). */
    fun sendInput(text: String) {
        scope.launch(Dispatchers.IO) {
            try {
                stdin?.writeBytes("${text.trim()}\n")
                stdin?.flush()
            } catch (_: Exception) {}
        }
    }

    /** Kill the binary process and clean up. */
    fun stop() {
        try { stdin?.writeBytes("exit\n"); stdin?.flush() } catch (_: Exception) {}
        try { stdin?.close() } catch (_: Exception) {}
        try { process?.destroy() } catch (_: Exception) {}
        readJob?.cancel()
        process = null
        stdin = null
        _isRunning.value = false
        _isStarted.value = false
        _isLicenseVerified.value = false
    }

    // ── Stdout signal handler ────────────────────────────────────────────────

    private fun handleLine(line: String) {
        when {
            line.contains("[?] Enter API Access Key:", ignoreCase = true) -> {
                scope.launch { _licensePromptEvent.emit(Unit) }
            }

            line.contains("Authentication failed", ignoreCase = true) ||
            line.contains("Invalid or expired", ignoreCase = true) ||
            line.contains("[-]", ignoreCase = false) && line.contains("fail", ignoreCase = true) -> {
                val msg = line.removePrefix("[-]").trim()
                scope.launch { _errorEvent.emit(msg) }
            }

            line.contains("API Key validated successfully", ignoreCase = true) ||
            line.contains("Core engine authorized", ignoreCase = true) -> {
                _isLicenseVerified.value = true
            }

            line.contains("Awaiting START signal", ignoreCase = true) ||
            line.contains("Ready to load into memory", ignoreCase = true) -> {
                // Auto-send START
                sendInput("START")
            }

            line.contains("Loading overlay engine into RAM", ignoreCase = true) ||
            line.contains("ninmods starting", ignoreCase = true) ||
            line.contains("daemon active", ignoreCase = true) -> {
                _isRunning.value = true
            }
        }
    }
}
