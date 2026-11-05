package com.luizmarquesarruda.relogio20.data.weather

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal object WeatherSyncScheduler {
    private const val PERIODIC_WORK_NAME = "weather-refresh-periodic-v1"
    private const val IMMEDIATE_WORK_NAME = "weather-refresh-now-v1"

    fun ensurePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
            6L,
            TimeUnit.HOURS,
            1L,
            TimeUnit.HOURS
        ).setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
