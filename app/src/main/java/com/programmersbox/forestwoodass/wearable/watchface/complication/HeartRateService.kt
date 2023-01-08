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
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM
import androidx.health.services.client.data.PassiveListenerConfig
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.programmersbox.forestwoodass.wearable.watchface.complication.PassiveDataRepository.Companion.NOT_HEART_RATE_CAPABLE

class HeartRateService : Service() {
    inner class LocalBinder : Binder() {
        val service: HeartRateService
            get() = this@HeartRateService
    }

    private var connectionCount = 0
    private var hasPermissions: Boolean = false
    private var listenerStarted: Boolean = false
    private var notCapable: Boolean = false
    private  var healthClient: HealthServicesClient? = null
    private var passiveMonitoringClient: PassiveMonitoringClient? = null
    private lateinit var dataRepo: PassiveDataRepository

    override fun onCreate() {
        super.onCreate()
        if (android.os.Build.VERSION.SDK_INT  < android.os.Build.VERSION_CODES.R) {
            stopSelf()
            return
        }
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
                if (!dataRepo.getPermissionDeclined()) {
                    Log.d(TAG, "Starting permissions dialog activity")
                    val dialogIntent = Intent(this, PermissionActivity::class.java)
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(dialogIntent)
                } else {
                    Log.d(TAG, "User declined permissions")
                }
            }
        }
    }

    private fun havePermission(): Boolean
    {
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
                Log.d(TAG, "havePermission: We do NOT have body_sensor permissions")
            }
        }
        return hasPermissions
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (hasPermissions) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        if ( passiveMonitoringClient != null ) {
            passiveMonitoringClient?.clearPassiveListenerServiceAsync()
        }
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
        // This really should be unnecessary, but I'm OCD
        if (android.os.Build.VERSION.SDK_INT  < android.os.Build.VERSION_CODES.R) {
            return
        }
        dataRepo.putPermissionDeclined(false)
        healthClient = healthClient ?: getClient(context /*context*/)
        passiveMonitoringClient = healthClient?.passiveMonitoringClient

        val passiveListenerConfig: PassiveListenerConfig = PassiveListenerConfig.builder()
            .setDataTypes(
                setOf(
                    HEART_RATE_BPM
                )
            )
            .build()
        val rc = passiveMonitoringClient?.setPassiveListenerServiceAsync(
            PassiveDataService::class.java,
            passiveListenerConfig
        )
        if ( rc != null ) {
            Futures.addCallback(rc, object : FutureCallback<Void?> {
                override fun onSuccess(result: Void?) {
                    //handle on all success and combination success
                    Log.d(TAG, "future onSuccess")
                    listenerStarted = true
                    notCapable = false
                }

                override fun onFailure(t: Throwable) {
                    //handle on either task fail or combination failed
                    Log.d(TAG, "future onFailure $t")
                    passiveMonitoringClient?.clearPassiveListenerServiceAsync()
                    notCapable = true

                    dataRepo.putHeartRateValue(NOT_HEART_RATE_CAPABLE)
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }

    companion object {
        var TAG = "HeartRateService"
    }
}
