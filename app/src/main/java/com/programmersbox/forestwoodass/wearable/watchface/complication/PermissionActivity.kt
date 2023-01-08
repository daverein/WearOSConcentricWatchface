package com.programmersbox.forestwoodass.wearable.watchface.complication

import android.Manifest.permission.BODY_SENSORS
import android.app.Activity
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.programmersbox.forestwoodass.wearable.watchface.R


class PermissionActivity : Activity() {

    private val PERMISSION_REQUEST_CODE: Int = 100
    private lateinit var dataRepo: PassiveDataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        Log.d(TAG, "creating PermissionActivity")
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

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "Got the permission results for $requestCode")
        Log.d(TAG, "permissions ${permissions[0]}")
        Log.d(TAG, "grantResults ${grantResults[0]}")
        if ( requestCode == PERMISSION_REQUEST_CODE && grantResults[0] == PERMISSION_DENIED) {
            dataRepo = PassiveDataRepository(this)
            dataRepo.putPermissionDeclined(true)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        finish()
    }

    private fun requestPermission() {
        requestPermissions( arrayOf( BODY_SENSORS ),
            PERMISSION_REQUEST_CODE)
    }

    companion object {
        const val TAG = "PermissionActivity"
    }
}
