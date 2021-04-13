package ru.adminmk.mydashboard.model.database

import android.app.Application

class DashboardApplication :Application() {
    override fun onCreate() {
        super.onCreate()

        IndicatorsRepository.initialize(this)
    }
}