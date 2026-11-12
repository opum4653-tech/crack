package com.andorid.crackmaster

import android.app.Application
import com.topjohnwu.superuser.Shell

class CrackMasterApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure Libsu official root shell environment
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(15)
        )

        // Request root shell asynchronously on application startup
        Shell.getShell { /* Pre-warm root shell daemon */ }
    }
}
