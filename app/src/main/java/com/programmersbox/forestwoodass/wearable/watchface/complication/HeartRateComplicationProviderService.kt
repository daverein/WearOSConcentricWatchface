package com.programmersbox.forestwoodass.wearable.watchface.complication

import android.Manifest
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.IBinder
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.programmersbox.forestwoodass.wearable.watchface.R
import com.programmersbox.forestwoodass.wearable.watchface.editor.WatchFaceConfigActivity


class HeartRateComplicationProviderService : CoroutinesComplicationDataSourceService() {

    var context: Context? = null
    private var mShouldUnbind = false

    private lateinit var dataRepo: PassiveDataRepository

    override fun onDestroy() {
        super.onDestroy()
        if ( mShouldUnbind ) {
            unbindService(mConnection)
        }
    }
    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        Log.d(TAG, "onComplicationActivated...")
        startService(Intent(this, HeartRateService::class.java))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand...")
        return START_STICKY
    }

    private var mBoundService: HeartRateService? = null

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = (service as HeartRateService.LocalBinder).service


        }

        override fun onServiceDisconnected(className: ComponentName?) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null

        }
    }

    override fun onCreate() {
        super.onCreate()
        startService(Intent(this, HeartRateService::class.java))

        if (bindService(
                Intent(this, HeartRateService::class.java),
                mConnection, BIND_AUTO_CREATE
            )
        ) {
            mShouldUnbind = true
        } else {
            // TODO
        }
        dataRepo = PassiveDataRepository(this)
    }
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        context = newBase

    }

    override suspend fun onComplicationUpdate(complicationRequest: ComplicationRequest) =
        toComplicationData(
            complicationRequest.complicationType, dataRepo.getHeartRateValue().toInt(), "BPM")

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return toComplicationData(type, 86, "BPM")
    }

    private fun toComplicationData(
        type: ComplicationType,
        heartRate: Int,
        units: String
    ): ComplicationData {
        Log.d(TAG, "Building complication data for HR $heartRate")
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                getHRComplicationText(heartRate),
                getHRComplicationTitle(units)
            )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            if(heartRate==-100){R.drawable.noheart_96}else{R.drawable.heart_96}
                        )
                    ).build()
                )
                .setTapAction(tapAction())
                .build()

            else -> throw IllegalArgumentException("Unexpected complication type $type")
        }
    }

    private fun getHRComplicationText(heartRate: Int): ComplicationText {
        return if ( heartRate <= 0 ) {
            PlainComplicationText.Builder("--").build()
        } else {
            PlainComplicationText.Builder("$heartRate").build()
        }
    }

    private fun getHRComplicationTitle(title: String): ComplicationText {
        return PlainComplicationText.Builder(title).build()
    }

    companion object {
        const val TAG = "HeartRateComplicationProviderService"
        fun Context.forceComplicationUpdate() {
            if (applicationContext.checkCallingOrSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
                val request =
                    ComplicationDataSourceUpdateRequester.create(
                        applicationContext, ComponentName(
                            applicationContext, HeartRateComplicationProviderService::class.java
                        )
                    )
                request.requestUpdateAll()
            }
        }

        fun Context.tapAction(): PendingIntent? {
            val intent = Intent(this, WatchFaceConfigActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

}
