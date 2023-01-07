package com.programmersbox.forestwoodass.wearable.watchface.complication

import android.Manifest.permission.BODY_SENSORS
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class PermissionActivity : Activity() {

    private val PERMISSION_REQUEST_CODE: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                BODY_SENSORS
            ) -> {
                finish()
            }
            else -> {
                requestPermission()
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "Got the permission results for $requestCode")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        finish()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(BODY_SENSORS),
            PERMISSION_REQUEST_CODE
        )
    }

    companion object {
        const val TAG = "PermissionActivity"
    }
}
