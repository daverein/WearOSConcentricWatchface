package com.programmersbox.forestwoodass.wearable.watchface.complication

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices.getClient
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM
import androidx.health.services.client.data.PassiveListenerConfig

class HeartRateService : Service() {
    inner class LocalBinder : Binder() {
        val service: HeartRateService
            get() = this@HeartRateService
    }

    private var connectionCount = 0
    private var hasPermissions: Boolean = false
    private var listenerStarted: Boolean = false
    private lateinit var dataRepo: PassiveDataRepository

    override fun onCreate() {
        super.onCreate()
        dataRepo = PassiveDataRepository(this)
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) -> {
                hasPermissions = true
                Log.d(TAG, "We have body_sensor permissions")
                startHealthListener(baseContext)
            }
            else -> {
                hasPermissions = false
                Log.d(TAG, "We do NOT have body_sensor permissions")
                val dialogIntent = Intent(this, PermissionActivity::class.java)
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(dialogIntent)
            }
        }
    }

    private fun havePermission(): Boolean
    {
        Log.d(TAG, "havePermission: $hasPermissions")
        if ( hasPermissions )
            return true
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) -> {
                hasPermissions = true
            }
            else -> {
                hasPermissions = false
                Log.d(TAG, "We do NOT have body_sensor permissions")
            }
        }
        return hasPermissions
    }
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return if (hasPermissions) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }
    override fun onBind(intent: Intent): IBinder? {
        connectionCount++
        Log.d(TAG, "bind Clients connected: $connectionCount")
        havePermission()
        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        connectionCount--
        Log.d(TAG, "unbind Clients connected: $connectionCount")
        if ( connectionCount <= 0 && !hasPermissions) {
            stopSelf()
        }
        return super.onUnbind(intent)
    }
    private fun startHealthListener(context: Context) {
        if ( listenerStarted || !hasPermissions)
            return
        val healthClient = getClient(context /*context*/)
        val passiveMonitoringClient = healthClient.passiveMonitoringClient

        val passiveListenerConfig: PassiveListenerConfig = PassiveListenerConfig.builder()
            .setDataTypes(
                setOf(
                    HEART_RATE_BPM
                )
            )
            .build()
        val rc = passiveMonitoringClient.setPassiveListenerServiceAsync(
            PassiveDataService::class.java,
            passiveListenerConfig
        )
        rc.addListener({
            Log.d(TAG, "future completed")
            Log.d(TAG, "rc = $rc, done = ${rc.isDone} and cancelled = ${rc.isCancelled}")
        }, ContextCompat.getMainExecutor(this))
        listenerStarted = true
    }

    companion object {
        var TAG = "HeartRateService"
    }
}
