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
    val isVerified: Boolean = false,
    val showLicenseDialog: Boolean = false,
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
            expiredDate = sessionManager.expiredDate
        )
    )
    val uiState: StateFlow<CrackMasterUiState> = _uiState.asStateFlow()

    init {
        refreshSystemAndGameData()
    }

    fun onLicenseKeyChanged(newKey: String) {
        _uiState.value = _uiState.value.copy(
            licenseKey = newKey,
            authError = null
        )
    }

    fun openLicenseDialog() {
        val saved = sessionManager.licenseKey
        _uiState.value = _uiState.value.copy(
            licenseKey = if (saved.isNotBlank()) saved else _uiState.value.licenseKey,
            showLicenseDialog = true,
            authError = null,
            isAuthenticating = false,
            isVerified = false
        )
    }

    fun dismissLicenseDialog() {
        if (!_uiState.value.isAuthenticating) {
            _uiState.value = _uiState.value.copy(showLicenseDialog = false)
        }
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

    fun onStartClicked() {
        if (_uiState.value.isEngineRunning) return
        val savedKey = sessionManager.licenseKey.ifBlank { _uiState.value.licenseKey.trim() }
        if (savedKey.isBlank()) {
            openLicenseDialog()
        } else {
            // Already has key saved, but user wants NinMods-style check or confirmation:
            // Open license dialog prefilled with saved key so user can submit / start directly
            openLicenseDialog()
        }
    }

    fun onStopClicked() {
        viewModelScope.launch {
            ShellExecutor.stopDaemon()
            _uiState.value = _uiState.value.copy(
                isEngineRunning = false,
                launchStatusMessage = "Stopped"
            )
        }
    }

    fun submitLicenseKeyAndStart(keyInput: String) {
        val key = keyInput.trim()
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(authError = "License key is required!")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAuthenticating = true,
                authError = null
            )
            val hwid = sessionManager.getHardwareId()

            // 1. Verify license with crackmaster.shop
            val verifyRes = CrackMasterApiClient.verifyLicense(key, hwid)
            if (verifyRes.isFailure || verifyRes.getOrNull()?.status != true) {
                val err = verifyRes.getOrNull()?.reason ?: "License verification failed"
                _uiState.value = _uiState.value.copy(
                    isAuthenticating = false,
                    authError = err
                )
                return@launch
            }

            val resp = verifyRes.getOrNull()
            sessionManager.licenseKey = key
            sessionManager.isLoggedIn = true
            sessionManager.token = resp?.data?.token.orEmpty()
            sessionManager.expiredDate = resp?.data?.expired_date.orEmpty()

            _uiState.value = _uiState.value.copy(
                isAuthenticating = false,
                isVerified = true,
                expiredDate = sessionManager.expiredDate,
                showLicenseDialog = false,
                isLaunching = true,
                launchStatusMessage = "Starting...",
                downloadProgress = 0
            )

            // 2. Download binary if needed
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

            // 3. Spawn root daemon into memory
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
