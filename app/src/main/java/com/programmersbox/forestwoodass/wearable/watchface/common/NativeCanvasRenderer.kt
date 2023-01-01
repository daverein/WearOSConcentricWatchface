/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.programmersbox.forestwoodass.wearable.watchface.common

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.text.format.DateFormat
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import com.programmersbox.forestwoodass.wearable.watchface.R
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.*
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.WatchFaceColorPalette.Companion.convertToWatchFaceColorPalette
import com.programmersbox.forestwoodass.wearable.watchface.utils.*
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.*

// Default for how long each frame is displayed at expected frame rate.
private const val FRAME_PERIOD_MS_DEFAULT: Long = 64L
private const val FRAME_PERIOD_MS_LOW_BATTERY: Long = 1000L


/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
@Suppress("KotlinConstantConditions")
abstract class NativeCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<NativeCanvasRenderer.AnalogSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
), BatteryLevelChangeReceiver.BatteryLevelChangeListener {
    class AnalogSharedAssets : SharedAssets {
        override fun onDestroy() {
            // Not sure why we are overriding this method
        }
    }

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Represents all data needed to render the watch face. All value defaults are constants. Only
    // three values are changeable by the user (color scheme, ticks being rendered, and length of
    // the minute arm). Those dynamic values are saved in the watch face APIs and we update those
    // here (in the renderer) through a Kotlin Flow.
    var watchFaceData: WatchFaceData = WatchFaceData()

    // Converts resource ids into Colors and ComplicationDrawable.
    var watchFaceColors = convertToWatchFaceColorPalette(
        context,
        watchFaceData.activeColorStyle,
        watchFaceData.ambientColorStyle
    )

    var is24Format: Boolean = DateFormat.is24HourFormat(context)

    val colorBlack = context.resources.getColor(R.color.black, context.theme)

    // Initializes paint object for painting the clock hands with default values.
    val translucentPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        color = context.resources.getColor(R.color.black_50, context.theme)
    }
    val blackPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        style = Paint.Style.FILL
        color = context.resources.getColor(R.color.black, context.theme)
    }
    val outerElementPaint = Paint().apply {
        isAntiAlias = true
    }

    // Used to paint the main hour hand text with the hour pips, i.e., 3, 6, 9, and 12 o'clock.
    val calendarMonthPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_medium)
    }

    // Used to paint the main hour hand text with the hour pips, i.e., 3, 6, 9, and 12 o'clock.
    val calendarDayPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_bold)
    }

    // Used to paint the main hour hand text with the hour pips, i.e., 3, 6, 9, and 12 o'clock.
    val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.hour_mark_size).toFloat()
    }
    val hourTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_regular)

    }
    val hourTextAmbientPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_light)

    }
    val minuteTextAmbientPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_light)

    }
    val minuteTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_regular)
    }
    val minuteHighlightPaint = Paint().apply {
        isAntiAlias = true
    }
    val hourHighlightPaint = Paint().apply {
        isAntiAlias = true
    }
    val secondHighlightPaint = Paint().apply {
        isAntiAlias = true
    }
    val minuteDialTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_medium)
    }
    val secondDialTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_regular)
    }

    // Changed when setting changes cause a change in the minute hand arm (triggered by user in
    // updateUserStyle() via userStyleRepository.addUserStyleListener()).
    var shiftPixelSettingChanged: Boolean = false

    // Default size of watch face drawing area, that is, a no size rectangle. Will be replaced with
    // valid dimensions from the system.
    var currentWatchFaceSize = Rect(0, 0, 0, 0)

    // Is this expensive?
    private val batteryLevelChanged: BatteryLevelChangeReceiver
        get() = BatteryLevelChangeReceiver(this)

    fun isBatteryLow(): Boolean {
        return batteryLevelChanged.batteryLow
    }

    override fun onBatteryLevelChanged(oldValue: Boolean, newValue: Boolean) {
        interactiveDrawModeUpdateDelayMillis = if (isBatteryLow() || watchFaceData.lowPower) {
            FRAME_PERIOD_MS_LOW_BATTERY
        } else {
            FRAME_PERIOD_MS_DEFAULT
        }
    }

    private val timeSetReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            is24Format = DateFormat.is24HourFormat(context)
        }
    }

    private val keyguardManager =
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private var lastLockState = false
    private var lastLockStateInterval: Long = 0
    private fun isDeviceLocked(): Boolean {
        // This is goofy, but the keyguard listener for locked device is not until API33
        // Goofy code expects to be called interactiveDrawModeUpdateDelayMillis times per sec
        lastLockStateInterval++
        if (lastLockStateInterval % (1000L / interactiveDrawModeUpdateDelayMillis) == 0L) {
            lastLockState = keyguardManager.isDeviceLocked
        }
        return lastLockState
    }

    init {
        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                updateWatchFaceData(userStyle)
            }
        }
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(batteryLevelChanged, filter)
        }
        batteryLevelChanged.onReceive(context, batteryStatus)

        IntentFilter("android.intent.action.TIME_SET").let { filter ->
            context.registerReceiver(timeSetReceiver, filter)
        }
    }

    override suspend fun createSharedAssets(): AnalogSharedAssets {
        return AnalogSharedAssets()
    }


    open fun drawComplications(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime
    ): Boolean {
        return !isDeviceLocked()
    }

    /*
     * Triggered when the user makes changes to the watch face through the settings activity. The
     * function is called by a flow.
     */
    open fun updateWatchFaceData(userStyle: UserStyle) {
        Log.d(TAG, "updateWatchFace(): $userStyle")

        var newWatchFaceData: WatchFaceData = watchFaceData

        // Loops through user style and applies new values to watchFaceData.
        for (options in userStyle) {
            when (options.key.id.toString()) {
                COLOR_STYLE_SETTING -> {
                    val listOption = options.value as
                        UserStyleSetting.ListUserStyleSetting.ListOption

                    newWatchFaceData = newWatchFaceData.copy(
                        activeColorStyle = ColorStylesDynamic.getColorStyleConfig(
                            listOption.id.toString()
                        )
                    )
                }
                LAYOUT_STYLE_SETTING -> {
                    val listOption = options.value as
                        UserStyleSetting.ListUserStyleSetting.ListOption

                    newWatchFaceData = newWatchFaceData.copy(
                        layoutStyle = LayoutStyleIdAndResourceIds.getLayoutStyleConfig(
                            listOption.id.toString()
                        )
                    )
                }
                STYLE_ICON_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        styleIcon = booleanValue.value
                    )
                }
                DRAW_TIME_AOD_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        timeAOD = booleanValue.value
                    )
                }
                DRAW_DATE_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        drawDate = booleanValue.value
                    )
                }
                LOW_POWER_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        lowPower = booleanValue.value
                    )
                }
                DRAW_COMP_CIRCLES_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        drawCompCircles = booleanValue.value
                    )
                }
                COMPAOD_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        compAOD = booleanValue.value
                    )
                }
                MINUTEDIALAOD_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        minuteDialAOD = booleanValue.value
                    )
                }
                ACTIVE_AS_AMBIENT_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        activeAsAmbient = booleanValue.value
                    )
                }
                SHIFT_PIXEL_STYLE_SETTING -> {
                    val doubleValue = options.value as
                        UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption

                    // The arm lengths are usually only calculated the first time the watch face is
                    // loaded to reduce the ops in the onDraw(). Because we updated the minute hand
                    // watch length, we need to trigger a recalculation.
                    shiftPixelSettingChanged = true

                    newWatchFaceData = newWatchFaceData.copy(
                        shiftPixelAmount = doubleValue.value.toFloat()
                    )
                }
            }
        }

        // Only updates if something changed.
        if (watchFaceData != newWatchFaceData) {
            watchFaceData = newWatchFaceData

            // Recreates Color and ComplicationDrawable from resource ids.
            watchFaceColors = convertToWatchFaceColorPalette(
                context,
                watchFaceData.activeColorStyle,
                watchFaceData.ambientColorStyle
            )

            interactiveDrawModeUpdateDelayMillis = if (isBatteryLow() || watchFaceData.lowPower) {
                FRAME_PERIOD_MS_LOW_BATTERY
            } else {
                FRAME_PERIOD_MS_DEFAULT
            }

            // Applies the user chosen complication color scheme changes. ComplicationDrawables for
            // each of the styles are defined in XML so we need to replace the complication's
            // drawables.
            for ((_, complication) in complicationSlotsManager.complicationSlots) {
                ComplicationDrawable.getDrawable(
                    context,
                    R.drawable.complication_style
                )?.let {
                    // Set to draw the progress and under circles on the complications or not
                    it.isDrawComplicationCircles = watchFaceData.drawCompCircles
                    it.isStyleIcon = watchFaceData.styleIcon
                    it.isActiveInAmbient = watchFaceData.activeAsAmbient

                    it.activeStyle.iconColor = watchFaceColors.activePrimaryColor
                    it.activeStyle.highlightColor = watchFaceColors.activePrimaryColor
                    it.activeStyle.textColor = watchFaceColors.activePrimaryTextColor
                    it.activeStyle.rangedValuePrimaryColor = watchFaceColors.activeSecondaryColor
                    it.activeStyle.titleColor = watchFaceColors.activePrimaryColor

                    if (watchFaceData.activeAsAmbient) {
                        it.ambientStyle.textColor = watchFaceColors.activePrimaryTextColor
                        it.ambientStyle.iconColor = watchFaceColors.activePrimaryColor
                        it.ambientStyle.highlightColor = watchFaceColors.activePrimaryColor
                        it.ambientStyle.rangedValuePrimaryColor =
                            watchFaceColors.activeSecondaryColor
                        it.ambientStyle.rangedValueSecondaryColor =
                            it.activeStyle.rangedValueSecondaryColor
                        it.ambientStyle.titleColor = watchFaceColors.activePrimaryColor
                    }
                    (complication.renderer as CanvasComplicationDrawable).drawable = it
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        scope.cancel("ConcentricNativeCanvasRenderer scope clear() request")
        context.unregisterReceiver(batteryLevelChanged)
        context.unregisterReceiver(timeSetReceiver)
        super.onDestroy()
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        val isAmbient =
            renderParameters.drawMode == DrawMode.AMBIENT && !watchFaceData.activeAsAmbient

        val backgroundColor = if (isAmbient) {
            watchFaceColors.ambientBackgroundColor
        } else {
            watchFaceColors.activeBackgroundColor
        }
        configColors(isAmbient)

        canvas.drawColor(backgroundColor)

        // Prevent burnin
        if (renderParameters.drawMode == DrawMode.AMBIENT &&
            watchFaceData.shiftPixelAmount >= 1.0f
        ) {
            val cx = sin((zonedDateTime.minute % 60f) * 6f) * watchFaceData.shiftPixelAmount
            val cy = -cos((zonedDateTime.minute % 60f) * 6f) * watchFaceData.shiftPixelAmount
            canvas.translate(cx, cy)
        }
    }

    private fun configColors(drawAmbient: Boolean) {
        hourTextPaint.color = when {
            drawAmbient -> watchFaceColors.ambientPrimaryTextColor
            else -> watchFaceColors.activePrimaryTextColor
        }
        hourTextAmbientPaint.color = when {
            drawAmbient -> watchFaceColors.ambientPrimaryTextColor
            else -> watchFaceColors.activePrimaryTextColor
        }
        minuteTextPaint.color = when {
            drawAmbient -> watchFaceColors.ambientPrimaryTextColor
            else -> watchFaceColors.activePrimaryTextColor
        }
        minuteTextAmbientPaint.color = when {
            drawAmbient -> watchFaceColors.ambientPrimaryTextColor
            else -> watchFaceColors.activePrimaryTextColor
        }
        minuteHighlightPaint.color = when {
            drawAmbient -> watchFaceColors.ambientPrimaryColor
            else -> watchFaceColors.activePrimaryColor
        }
        hourHighlightPaint.color = when {
            drawAmbient -> watchFaceColors.ambientSecondaryColor
            else -> watchFaceColors.activeSecondaryColor
        }
        secondHighlightPaint.color = when {
            drawAmbient -> watchFaceColors.ambientPrimaryColor
            else -> watchFaceColors.activePrimaryColor
        }
        outerElementPaint.color = when {
            drawAmbient -> watchFaceColors.ambientOuterElementColor
            else -> watchFaceColors.activeOuterElementColor
        }
    }

    companion object {
        private const val TAG = "ConcentricNativeCanvasRenderer"
    }
}
