package com.andorid.crackmaster.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andorid.crackmaster.api.CrackMasterApiClient
import com.andorid.crackmaster.data.BinaryDownloader
import com.andorid.crackmaster.data.GameInfo
import com.andorid.crackmaster.data.SessionManager
import com.andorid.crackmaster.data.ShellExecutor
import com.andorid.crackmaster.data.SystemDetector
import com.andorid.crackmaster.data.SystemStatusInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CrackMasterUiState(
    val licenseKey: String = "",
    val isAuthenticating: Boolean = false,
    val authError: String? = null,
    val isLoggedIn: Boolean = false,
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
    val selectedMode: String = "Bing Driver (FT/VIP & RT Dual)",
    val availableModes: List<String> = listOf(
        "Bing Driver (FT/VIP & RT Dual)",
        "Direct Memory Mode",
        "Virtual Userland Engine"
    ),
    val isEngineRunning: Boolean = false,
    val isLaunching: Boolean = false,
    val launchStatusMessage: String? = null,
    val downloadProgress: Int = 0
)

class CrackMasterViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(
        CrackMasterUiState(
            licenseKey = sessionManager.licenseKey,
            isLoggedIn = sessionManager.isLoggedIn,
            expiredDate = sessionManager.expiredDate,
            selectedMode = sessionManager.selectedMode
        )
    )
    val uiState: StateFlow<CrackMasterUiState> = _uiState.asStateFlow()

    init {
        refreshSystemAndGameData()
        checkExistingSession()
    }

    fun onLicenseKeyChanged(newKey: String) {
        _uiState.value = _uiState.value.copy(
            licenseKey = newKey,
            authError = null
        )
    }

    fun onSelectMode(mode: String) {
        sessionManager.selectedMode = mode
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun refreshSystemAndGameData() {
        val app = getApplication<Application>()
        val gInfo = SystemDetector.detectGameInfo(app)
        viewModelScope.launch {
            val sInfo = SystemDetector.detectSystemStatus()
            val running = ShellExecutor.isDaemonRunning()
            _uiState.value = _uiState.value.copy(
                gameInfo = gInfo,
                systemInfo = sInfo,
                isEngineRunning = running
            )
        }
    }

    private fun checkExistingSession() {
        val savedKey = sessionManager.licenseKey
        if (savedKey.isNotBlank() && sessionManager.isLoggedIn) {
            viewModelScope.launch {
                val hwid = sessionManager.getHardwareId()
                val result = CrackMasterApiClient.verifyLicense(savedKey, hwid)
                if (result.isSuccess && result.getOrNull()?.status == true) {
                    val resp = result.getOrNull()
                    sessionManager.expiredDate = resp?.data?.expired_date ?: sessionManager.expiredDate
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        expiredDate = sessionManager.expiredDate
                    )
                } else {
                    sessionManager.isLoggedIn = false
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = false,
                        authError = result.getOrNull()?.reason ?: "License expired or revoked"
                    )
                }
            }
        }
    }

    fun authenticateInitial(onSuccess: () -> Unit = {}) {
        val key = _uiState.value.licenseKey.trim()
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(authError = "License key is required!")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAuthenticating = true, authError = null)
            val hwid = sessionManager.getHardwareId()
            val result = CrackMasterApiClient.verifyLicense(key, hwid)

            if (result.isSuccess) {
                val resp = result.getOrNull()
                if (resp != null && resp.status) {
                    sessionManager.licenseKey = key
                    sessionManager.isLoggedIn = true
                    sessionManager.token = resp.data?.token.orEmpty()
                    sessionManager.expiredDate = resp.data?.expired_date.orEmpty()

                    _uiState.value = _uiState.value.copy(
                        isAuthenticating = false,
                        isLoggedIn = true,
                        expiredDate = sessionManager.expiredDate,
                        authError = null
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticating = false,
                        authError = resp?.reason ?: "Invalid license key!"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isAuthenticating = false,
                    authError = result.exceptionOrNull()?.message ?: "Server connection failed!"
                )
            }
        }
    }

    fun toggleEngine() {
        if (_uiState.value.isEngineRunning) {
            viewModelScope.launch {
                ShellExecutor.stopDaemon()
                _uiState.value = _uiState.value.copy(
                    isEngineRunning = false,
                    launchStatusMessage = "Stopped"
                )
            }
            return
        }

        viewModelScope.launch {
            val key = sessionManager.licenseKey.ifBlank { _uiState.value.licenseKey.trim() }
            val hwid = sessionManager.getHardwareId()

            _uiState.value = _uiState.value.copy(
                isLaunching = true,
                launchStatusMessage = "Checking key...",
                downloadProgress = 0
            )

            // Step 1: 2nd Verification against crackmaster.shop
            val verifyRes = CrackMasterApiClient.verifyLicense(key, hwid)
            if (verifyRes.isFailure || verifyRes.getOrNull()?.status != true) {
                val err = verifyRes.getOrNull()?.reason ?: "License verification failed"
                _uiState.value = _uiState.value.copy(
                    isLaunching = false,
                    launchStatusMessage = err
                )
                return@launch
            }

            // Step 2: Download binary from ninmods.smmtor.com if needed
            _uiState.value = _uiState.value.copy(launchStatusMessage = "Starting...")
            val context = getApplication<Application>()
            val downloadRes = BinaryDownloader.downloadBinaryIfNeeded(context) { p ->
                _uiState.value = _uiState.value.copy(
                    downloadProgress = p,
                    launchStatusMessage = if (p < 100) "Downloading: $p%" else "Ready"
                )
            }

            if (downloadRes.isFailure) {
                val err = downloadRes.exceptionOrNull()?.message ?: "Download failed"
                _uiState.value = _uiState.value.copy(
                    isLaunching = false,
                    launchStatusMessage = err
                )
                return@launch
            }

            val binaryFile = downloadRes.getOrNull()!!

            // Step 3: Spawn root daemon
            _uiState.value = _uiState.value.copy(launchStatusMessage = "Starting...")
            val startRes = ShellExecutor.startDaemon(binaryFile.absolutePath, key)

            if (startRes.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLaunching = false,
                    isEngineRunning = true,
                    launchStatusMessage = "Active"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLaunching = false,
                    isEngineRunning = false,
                    launchStatusMessage = "Failed"
                )
            }
        }
    }
}
