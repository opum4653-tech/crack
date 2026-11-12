package com.andorid.crackmaster.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andorid.crackmaster.api.CrackMasterApiClient
import com.andorid.crackmaster.data.GameInfo
import com.andorid.crackmaster.data.InteractiveProcessBridge
import com.andorid.crackmaster.data.SessionManager
import com.andorid.crackmaster.data.ShellExecutor
import com.andorid.crackmaster.data.SystemDetector
import com.andorid.crackmaster.data.SystemStatusInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CrackMasterUiState(
    // ── Stage 1: initial app-launch login ───────────────────────────────────
    val isLoggedIn: Boolean = false,
    val loginKey: String = "",
    val isLoginAuthenticating: Boolean = false,
    val loginError: String? = null,

    // ── Game / System info ──────────────────────────────────────────────────
    val licenseKey: String = "",
    val expiredDate: String = "",
    val gameInfo: GameInfo = GameInfo(
        title = "",
        packageName = SystemDetector.PKG_FF_NORMAL,
        versionName = "",
        versionCode = 0L,
        isInstalled = false,
        architecture = "",
        modeBadge = "EXTERNAL"
    ),
    val systemInfo: SystemStatusInfo = SystemStatusInfo(
        isRootGranted = false,
        rootStatusText = "",
        androidVersion = "",
        deviceName = "",
        architecture = ""
    ),

    // ── Stage 2: START license dialog (InteractiveProcessBridge) ────────────
    val showLicenseDialog: Boolean = false,
    val isAuthenticating: Boolean = false,     // binary is verifying key
    val authError: String? = null,
    val isVerified: Boolean = false,           // binary confirmed key valid
    val terminalLogs: List<String> = emptyList(),

    // ── Engine state ─────────────────────────────────────────────────────────
    val isEngineRunning: Boolean = false,
    val isLaunching: Boolean = false,
    val downloadProgress: Int = 0,
    val launchStatusMessage: String? = null
)

class CrackMasterViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val processBridge = InteractiveProcessBridge(application, viewModelScope)

    private val _uiState = MutableStateFlow(
        CrackMasterUiState(
            isLoggedIn   = sessionManager.isLoggedIn,
            licenseKey   = sessionManager.licenseKey,
            expiredDate  = sessionManager.expiredDate
        )
    )
    val uiState: StateFlow<CrackMasterUiState> = _uiState.asStateFlow()

    init {
        refreshSystemAndGameData()
        collectBridgeSignals()
    }

    // ────────────────────────────────────────────────────────────────────────
    // Stage 1 — Initial app-launch login (direct API call, binary not running)
    // ────────────────────────────────────────────────────────────────────────

    fun onLoginKeyChanged(key: String) {
        _uiState.value = _uiState.value.copy(loginKey = key, loginError = null)
    }

    fun authenticateInitial() {
        val key = _uiState.value.loginKey.trim()
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(loginError = "License key is required!")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoginAuthenticating = true, loginError = null)
            val hwid = sessionManager.getHardwareId()
            val result = CrackMasterApiClient.verifyLicense(key, hwid)
            if (result.isSuccess && result.getOrNull()?.status == true) {
                val resp = result.getOrNull()
                sessionManager.licenseKey  = key
                sessionManager.isLoggedIn  = true
                sessionManager.token       = resp?.data?.token.orEmpty()
                sessionManager.expiredDate = resp?.data?.expired_date.orEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoginAuthenticating = false,
                    isLoggedIn   = true,
                    licenseKey   = key,
                    expiredDate  = sessionManager.expiredDate
                )
            } else {
                val err = result.getOrNull()?.reason ?: result.exceptionOrNull()?.message ?: "Verification failed"
                _uiState.value = _uiState.value.copy(
                    isLoginAuthenticating = false,
                    loginError = err
                )
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Dashboard helpers
    // ────────────────────────────────────────────────────────────────────────

    fun refreshSystemAndGameData() {
        val app = getApplication<Application>()
        val gInfo = SystemDetector.detectGameInfo(app)
        viewModelScope.launch {
            val sInfo   = SystemDetector.detectSystemStatus()
            val running = ShellExecutor.isDaemonRunning()
            _uiState.value = _uiState.value.copy(
                gameInfo        = gInfo,
                systemInfo      = sInfo,
                isEngineRunning = running
            )
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Stage 2 — START button flow (InteractiveProcessBridge stdin pipe)
    // ────────────────────────────────────────────────────────────────────────

    fun onStartClicked() {
        if (_uiState.value.isEngineRunning || _uiState.value.isLaunching) return

        val savedKey = sessionManager.licenseKey

        // Open the dialog IMMEDIATELY with the saved key pre-filled, before bridge starts.
        // Terminal log stream will populate as binary emits lines.
        _uiState.value = _uiState.value.copy(
            terminalLogs        = emptyList(),
            isVerified          = false,
            authError           = null,
            isAuthenticating    = false,
            showLicenseDialog   = true,       // open dialog right away
            isLaunching         = true,
            downloadProgress    = 0,
            licenseKey          = savedKey,   // pre-fill with saved key
            launchStatusMessage = "Preparing..."
        )

        viewModelScope.launch {
            val hwid = sessionManager.getHardwareId()

            val result = processBridge.start(
                hwid       = hwid,
                savedKey   = savedKey,
                onDownloadProgress = { p ->
                    _uiState.value = _uiState.value.copy(
                        downloadProgress    = p,
                        launchStatusMessage = if (p < 100) "Downloading: $p%" else "Launching..."
                    )
                }
            )

            _uiState.value = _uiState.value.copy(isLaunching = false)

            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    showLicenseDialog   = false,
                    launchStatusMessage = result.exceptionOrNull()?.message ?: "Launch failed"
                )
                return@launch
            }

            // Bridge started — wait for "[?] Enter API Access Key:" signal to open dialog
        }
    }

    /** User submitted key in license dialog → pipe to binary stdin */
    fun submitLicenseKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(authError = "License key is required!")
            return
        }
        _uiState.value = _uiState.value.copy(
            isAuthenticating = true,
            authError = null,
            licenseKey = trimmed
        )
        processBridge.sendInput(trimmed)
    }

    fun onStopClicked() {
        processBridge.stop()
        viewModelScope.launch {
            ShellExecutor.stopDaemon()
            _uiState.value = _uiState.value.copy(
                isEngineRunning = false,
                showLicenseDialog = false,
                isVerified = false,
                isAuthenticating = false,
                terminalLogs = emptyList(),
                launchStatusMessage = "Stopped"
            )
        }
    }

    fun dismissLicenseDialog() {
        if (!_uiState.value.isAuthenticating) {
            processBridge.stop()
            _uiState.value = _uiState.value.copy(
                showLicenseDialog = false,
                isVerified = false,
                authError = null,
                terminalLogs = emptyList()
            )
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Bridge signal collectors
    // ────────────────────────────────────────────────────────────────────────

    private fun collectBridgeSignals() {
        // Stdout lines → terminal log view
        viewModelScope.launch {
            processBridge.outputLines.collect { line ->
                val logs = (_uiState.value.terminalLogs + line).takeLast(200)
                _uiState.value = _uiState.value.copy(terminalLogs = logs)
            }
        }

        // "[?] Enter API Access Key:" signal → ensure dialog is open and ready for input
        viewModelScope.launch {
            processBridge.licensePromptEvent.collect {
                _uiState.value = _uiState.value.copy(
                    showLicenseDialog = true,
                    isAuthenticating  = false,
                    authError         = null
                )
            }
        }

        // Error lines from binary
        viewModelScope.launch {
            processBridge.errorEvent.collect { err ->
                _uiState.value = _uiState.value.copy(
                    isAuthenticating = false,
                    authError = err
                )
            }
        }

        // "[+] API Key validated successfully." — binary confirmed
        viewModelScope.launch {
            processBridge.isLicenseVerified.collect { verified ->
                if (verified) {
                    sessionManager.licenseKey = _uiState.value.licenseKey
                    _uiState.value = _uiState.value.copy(
                        isVerified       = true,
                        isAuthenticating = false
                    )
                }
            }
        }

        // "[+] Loading overlay engine into RAM..." — engine active
        viewModelScope.launch {
            processBridge.isRunning.collect { running ->
                _uiState.value = _uiState.value.copy(
                    isEngineRunning   = running,
                    showLicenseDialog = if (running) false else _uiState.value.showLicenseDialog
                )
            }
        }
    }
}
