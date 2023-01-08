package com.programmersbox.forestwoodass.wearable.watchface.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.IBinder
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.programmersbox.forestwoodass.wearable.watchface.R
import com.programmersbox.forestwoodass.wearable.watchface.complication.PassiveDataRepository.Companion.NOT_HEART_RATE_CAPABLE
import com.programmersbox.forestwoodass.wearable.watchface.complication.PassiveDataRepository.Companion.NO_HEART_RATE
import com.programmersbox.forestwoodass.wearable.watchface.editor.WatchFaceConfigActivity


class HeartRateComplicationProviderService : CoroutinesComplicationDataSourceService() {

    var context: Context? = null
    private var mShouldUnbind = false

    private lateinit var dataRepo: PassiveDataRepository

    override fun onDestroy() {
        super.onDestroy()
        if (mShouldUnbind) {
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
            mBoundService = (service as HeartRateService.LocalBinder).service
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            mBoundService = null

        }
    }

    override fun onCreate() {
        super.onCreate()
        startService(Intent(this, HeartRateService::class.java))

        if (bindService(Intent(this, HeartRateService::class.java),
                mConnection, BIND_AUTO_CREATE)) {
            mShouldUnbind = true
        }
        dataRepo = PassiveDataRepository(this)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        context = newBase

    }

    override suspend fun onComplicationUpdate(complicationRequest: ComplicationRequest) =
        toComplicationData(
            complicationRequest.complicationType, dataRepo.getHeartRateValue().toInt()
        )

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return toComplicationData(type, 86)
    }

    private fun toComplicationData(
        type: ComplicationType,
        heartRate: Int
    ): ComplicationData {
        Log.d(TAG, "Building complication data for HR $heartRate")
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                getHRComplicationText(heartRate),
                getHRComplicationTitle("BPM")
            ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            when (heartRate) {
                                NO_HEART_RATE.toInt() -> {
                                    R.drawable.noheart_96
                                }
                                NOT_HEART_RATE_CAPABLE.toInt() -> {
                                    R.drawable.notheart_96
                                }
                                else -> {
                                    R.drawable.heart_96
                                }
                            }
                        )
                    ).build()
                ).setTapAction(tapAction()).build()

            else -> throw IllegalArgumentException("Unexpected complication type $type")
        }
    }

    private fun getHRComplicationText(heartRate: Int): ComplicationText {
        return if (heartRate <= 0) {
            PlainComplicationText.Builder("--").build()
        } else {
            PlainComplicationText.Builder(heartRate.toString()).build()
        }
    }

    private fun getHRComplicationTitle(title: String): ComplicationText {
        return PlainComplicationText.Builder(title).build()
    }

    companion object {
        const val TAG = "HeartRateComplicationProviderService"

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
