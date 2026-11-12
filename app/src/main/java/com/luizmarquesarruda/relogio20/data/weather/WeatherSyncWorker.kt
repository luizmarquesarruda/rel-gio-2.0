package com.luizmarquesarruda.relogio20.data.weather

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.luizmarquesarruda.relogio20.domain.weather.WeatherRefreshPolicy
import kotlinx.coroutines.CancellationException
import java.io.IOException

class WeatherSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val preferences = WeatherPreferences(applicationContext)
        val location = preferences.readLocation() ?: return Result.success()
        val cached = preferences.readSnapshot()
        if (WeatherRefreshPolicy.shouldSkipPeriodicRefresh(
                cached?.fetchedAtEpochMillis,
                System.currentTimeMillis()
            )
        ) return Result.success()
        return try {
            val snapshot = OpenMeteoWeatherRepository().fetchForecast(location)
            preferences.saveSnapshot(snapshot)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            Result.retry()
        } catch (error: Exception) {
            Result.failure()
        }
    }

}
