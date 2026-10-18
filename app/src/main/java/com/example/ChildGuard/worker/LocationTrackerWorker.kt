package com.example.childguard.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.childguard.di.AppLocator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

class LocationTrackerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "LocationWorker"
        const val WORK_NAME = "location_tracking"

        fun buildWorkRequest() =
            PeriodicWorkRequestBuilder<LocationTrackerWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    5, TimeUnit.MINUTES
                )
                .build()

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                buildWorkRequest()
            )
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        // Молча выходим, если онбординг не пройден.
        val settings = AppLocator.settings
        val chatId = settings.parentChatId ?: return Result.success()
        if (!settings.isSetupComplete) return Result.success()

        return try {
            val fused = LocationServices
                .getFusedLocationProviderClient(applicationContext)

            // Дешёвый фикс (сеть/Wi-Fi), без будящего GPS.
            @Suppress("MissingPermission")
            val location = Tasks.await(
                fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            ) ?: Tasks.await(fused.lastLocation)

            if (location == null) {
                Log.w(TAG, "No location, retry later")
                return Result.retry()
            }

            val link = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            AppLocator.alertDispatcher.dispatchLocation(
                chatId = chatId,
                lat = location.latitude,
                lon = location.longitude,
                mapsLink = link
            )
            Result.success()
        } catch (security: SecurityException) {
            // Родитель отозвал гео-разрешение — бесконечный retry бессмыслен.
            Log.e(TAG, "Permission revoked", security)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed", e)
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}
