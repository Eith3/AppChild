package com.example.childguard

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.example.childguard.di.AppLocator
import com.example.childguard.worker.LocationTrackerWorker

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Ручной DI: собираем граф зависимостей один раз.
        AppLocator.init(this)

        // Периодическая геолокация, единожды, переживает перезагрузку.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            LocationTrackerWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            LocationTrackerWorker.buildWorkRequest()
        )
    }
}
