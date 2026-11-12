package com.luizmarquesarruda.relogio20

import android.app.Application
import android.os.StrictMode

/**
 * Debug-only diagnostics for accidental main-thread I/O and leaked Android resources.
 * Policy violations are logged; they never crash the application and are not enabled in release.
 */
class RelogioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy
                .Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy
                .Builder()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .build(),
        )
    }
}
