package com.andorid.crackmaster.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class GameInfo(
    val title: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isInstalled: Boolean,
    val icon: Drawable? = null,
    val architecture: String,
    val modeBadge: String = "EXTERNAL"
)

data class SystemStatusInfo(
    val isRootGranted: Boolean,
    val rootStatusText: String,
    val androidVersion: String,
    val deviceName: String,
    val architecture: String
)

object SystemDetector {

    const val PKG_FF_NORMAL = "com.dts.freefireth"
    const val PKG_FF_MAX = "com.dts.freefiremax"

    /**
     * Dynamically reads installed game information purely from Android PackageManager.
     * Zero hardcoded versions or codes.
     */
    fun detectGameInfo(context: Context): GameInfo {
        val pm = context.packageManager
        val arch = resolvePrimaryAbi()

        // 1. Check Free Fire (com.dts.freefireth)
        try {
            val pkgInfo = pm.getPackageInfo(PKG_FF_NORMAL, 0)
            val appInfo = pkgInfo.applicationInfo
            val iconDrawable = appInfo?.loadIcon(pm)
            val label = appInfo?.loadLabel(pm)?.toString() ?: "Free Fire"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            val vName = pkgInfo.versionName ?: "Unknown"

            return GameInfo(
                title = label,
                packageName = PKG_FF_NORMAL,
                versionName = vName,
                versionCode = vCode,
                isInstalled = true,
                icon = iconDrawable,
                architecture = arch,
                modeBadge = "EXTERNAL"
            )
        } catch (_: PackageManager.NameNotFoundException) {}

        // 2. Check Free Fire Max (com.dts.freefiremax)
        try {
            val pkgInfo = pm.getPackageInfo(PKG_FF_MAX, 0)
            val appInfo = pkgInfo.applicationInfo
            val iconDrawable = appInfo?.loadIcon(pm)
            val label = appInfo?.loadLabel(pm)?.toString() ?: "Free Fire MAX"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            val vName = pkgInfo.versionName ?: "Unknown"

            return GameInfo(
                title = label,
                packageName = PKG_FF_MAX,
                versionName = vName,
                versionCode = vCode,
                isInstalled = true,
                icon = iconDrawable,
                architecture = arch,
                modeBadge = "EXTERNAL"
            )
        } catch (_: PackageManager.NameNotFoundException) {}

        // 3. Not installed on current device
        return GameInfo(
            title = "Free Fire",
            packageName = PKG_FF_NORMAL,
            versionName = "Not Installed",
            versionCode = 0L,
            isInstalled = false,
            icon = null,
            architecture = arch,
            modeBadge = "EXTERNAL"
        )
    }

    /**
     * Dynamically launches the target game package via launch intent.
     */
    fun launchGame(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Dynamically reads real hardware model, manufacturer, OS release, and root authorization.
     */
    suspend fun detectSystemStatus(): SystemStatusInfo = withContext(Dispatchers.IO) {
        // Query root privilege dynamically via Libsu
        val rootGranted = try {
            Shell.isAppGrantedRoot() == true || Shell.cmd("id -u").exec().out.any { it.trim() == "0" }
        } catch (_: Exception) {
            false
        }

        val rootText = if (rootGranted) "Granted" else "Denied"

        // Dynamic OS Version (e.g. "14", "13", "12")
        val osVersion = Build.VERSION.RELEASE.ifBlank { Build.VERSION.SDK_INT.toString() }

        // Dynamic Device Name resolved from system properties (e.g. "Xiaomi 2201116SG", "Samsung SM-S918B")
        val manufacturer = Build.MANUFACTURER.orEmpty().trim().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty().trim()
        val deviceName = when {
            model.startsWith(manufacturer, ignoreCase = true) -> model
            manufacturer.isNotEmpty() && model.isNotEmpty() -> "$manufacturer $model"
            model.isNotEmpty() -> model
            else -> Build.DEVICE ?: "Android Device"
        }

        val arch = resolvePrimaryAbi()

        SystemStatusInfo(
            isRootGranted = rootGranted,
            rootStatusText = rootText,
            androidVersion = osVersion,
            deviceName = deviceName,
            architecture = arch
        )
    }

    private fun resolvePrimaryAbi(): String {
        val supportedAbis = Build.SUPPORTED_ABIS
        val primaryAbi = if (!supportedAbis.isNullOrEmpty()) supportedAbis[0] else System.getProperty("os.arch").orEmpty()
        return primaryAbi.uppercase().ifBlank { "ARM64-V8A" }
    }
}
