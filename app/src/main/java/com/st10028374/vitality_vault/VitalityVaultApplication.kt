package com.st10028374.vitality_vault

import android.app.Application
import com.st10028374.vitality_vault.sync.NetworkMonitor

class VitalityVaultApplication : Application() {

    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate() {
        super.onCreate()

        // Initialize network monitoring for automatic sync
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()

        // Schedule periodic sync - runs every 6 hours when device is online
        NetworkMonitor.schedulePeriodicSync(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        // Stop network monitoring when app terminates
        networkMonitor.stopMonitoring()
    }
}