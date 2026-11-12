package com.andorid.crackmaster.data

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShellExecutor {

    suspend fun execute(command: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = Shell.cmd(command).exec()
            res.isSuccess
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isDaemonRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = Shell.cmd("pgrep -f 'gemini-cli' || pidof 'gemini-cli'").exec()
            res.isSuccess && res.out.any { it.trim().isNotEmpty() && it.trim().all { c -> c.isDigit() } }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun startDaemon(binPath: String, licenseKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val safePath = binPath.replace("'", "'\\''")
            val safeKey = licenseKey.replace("'", "'\\''")
            val cmd = "chmod 755 '$safePath' && nohup '$safePath' --key '$safeKey' > /dev/null 2>&1 &"
            val res = Shell.cmd(cmd).exec()
            if (res.isSuccess) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to spawn process (code ${res.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopDaemon(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.cmd("pkill -9 -f 'gemini-cli'").exec()
            true
        } catch (_: Exception) {
            false
        }
    }
}
