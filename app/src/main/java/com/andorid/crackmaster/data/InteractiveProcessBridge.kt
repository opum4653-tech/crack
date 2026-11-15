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
    private var pendingInput: String? = null

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
        val downloadResult = BinaryDownloader.downloadBinaryIfNeeded(context, force = false, onProgress = onDownloadProgress)
        if (downloadResult.isFailure) return@withContext downloadResult.map { }

        val binaryFile = downloadResult.getOrNull()!!
        binaryFile.setExecutable(true, false)

        // 2. Spawn root process with stdin/stdout piped
        return@withContext try {
            // Pass HWID, server URL, and saved license key env vars.
            // When NIN_LICENSE_KEY is passed, the binary still emits "[?] Enter API Access Key:"
            // followed immediately by online validation, avoiding fragile stdin race conditions.
            val envBlock = buildString {
                append("export CRACKMASTER_SERVER_URL='https://crackmaster.shop'\n")
                append("export NIN_DEVICE_HWID='$hwid'\n")
                if (savedKey.isNotBlank()) {
                    append("export NIN_LICENSE_KEY='$savedKey'\n")
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

            // Flush any pending stdin input queued before process ready
            pendingInput?.let { pending ->
                stdin!!.writeBytes("$pending\n")
                stdin!!.flush()
                pendingInput = null
            }

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
                _isStarted.value = false
                if (!_isLicenseVerified.value) {
                    BinaryDownloader.deleteCachedBinary(context)
                    _errorEvent.emit("Process terminated before authorization was confirmed.")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Send a line to the binary's stdin (key or "START" signal). */
    fun sendInput(text: String) {
        val line = text.trim()
        scope.launch(Dispatchers.IO) {
            try {
                val out = stdin
                if (out != null) {
                    out.writeBytes("$line\n")
                    out.flush()
                } else {
                    pendingInput = line
                }
            } catch (_: Exception) {}
        }
    }

    /** Kill the binary process and clean up. */
    fun stop() {
        pendingInput = null
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
        val trimmed = line.trim()

        // 1. Success confirmation
        if (trimmed.contains("API Key validated successfully", ignoreCase = true) ||
            trimmed.contains("Core engine authorized", ignoreCase = true)) {
            _isLicenseVerified.value = true
        }

        // 2. Comprehensive error detection: any [-] marker or known failure phrase
        if (trimmed.startsWith("[-]") ||
            trimmed.contains("[-] ") ||
            trimmed.contains("Authentication failed", ignoreCase = true) ||
            trimmed.contains("Invalid or expired", ignoreCase = true) ||
            trimmed.contains("Network error", ignoreCase = true)) {
            val msg = if (trimmed.contains("[-]")) {
                trimmed.substringAfter("[-]").trim()
            } else {
                trimmed
            }
            scope.launch { _errorEvent.emit(if (msg.isNotBlank()) msg else "License verification failed") }
            return
        }

        // 3. Prompt detection: only if line is not an error line
        if (trimmed.contains("[?] Enter API Access Key:", ignoreCase = true)) {
            scope.launch { _licensePromptEvent.emit(Unit) }
        }

        // 4. Start signal handshake
        if (trimmed.contains("Awaiting START signal", ignoreCase = true) ||
            trimmed.contains("Ready to load into memory", ignoreCase = true)) {
            sendInput("START")
        }

        // 5. Active engine confirmation
        if (trimmed.contains("Loading overlay engine into RAM", ignoreCase = true) ||
            trimmed.contains("ninmods starting", ignoreCase = true) ||
            trimmed.contains("daemon active", ignoreCase = true)) {
            _isRunning.value = true
        }
    }
}
