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
package com.programmersbox.forestwoodass.wearable.watchface

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.text.format.DateFormat
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.graphics.withScale
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.*
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.WatchFaceColorPalette.Companion.convertToWatchFaceColorPalette
import com.programmersbox.forestwoodass.wearable.watchface.utils.*
import com.programmersbox.forestwoodass.wearable.watchface.utils.ColorUtils.Companion.darkenColor
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
class ConcentricNativeCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<ConcentricNativeCanvasRenderer.AnalogSharedAssets>(
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
    private var watchFaceData: WatchFaceData = WatchFaceData()

    // Converts resource ids into Colors and ComplicationDrawable.
    private var watchFaceColors = convertToWatchFaceColorPalette(
        context,
        watchFaceData.activeColorStyle,
        watchFaceData.ambientColorStyle
    )


    var is24Format: Boolean = DateFormat.is24HourFormat(context)
    var renderCount: Int = 0

    private val colorBlack = context.resources.getColor(R.color.black, context.theme)
    private var shadowLeft: Bitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.shadow_left
    )

    // Initializes paint object for painting the clock hands with default values.
    private val translucentPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = true
        color = context.resources.getColor(R.color.black_50, context.theme)
    }
    private val outerElementPaint = Paint().apply {
        isAntiAlias = true
    }

    // Used to paint the main hour hand text with the hour pips, i.e., 3, 6, 9, and 12 o'clock.
    private val calendarMonthPaint = Paint().apply {
        isAntiAlias = true
        textSize =
            context.resources.getDimensionPixelSize(R.dimen.calendar_month_font_size).toFloat()
        typeface = context.resources.getFont(R.font.rubik_medium)
    }

    // Used to paint the main hour hand text with the hour pips, i.e., 3, 6, 9, and 12 o'clock.
    private val calendarDayPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.calendar_day_font_size).toFloat()
        typeface = context.resources.getFont(R.font.rubik_bold)
    }

    // Used to paint the main hour hand text with the hour pips, i.e., 3, 6, 9, and 12 o'clock.
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.hour_mark_size).toFloat()
        // typeface = Typeface.createFromAsset(context.resources.assets, "Roboto.ttf");
    }
    private val hourTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_regular)

    }
    private val hourTextAmbientPaint = Paint().apply {
        isAntiAlias = false
        typeface = context.resources.getFont(R.font.rubik_light)

    }
    private val minuteTextAmbientPaint = Paint().apply {
        isAntiAlias = false
        typeface = context.resources.getFont(R.font.rubik_light)

    }
    private val minuteTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_regular)
    }
    private val minuteHighlightPaint = Paint().apply {
        isAntiAlias = true
        // typeface = Typeface.createFromAsset(context.resources.assets, "Roboto.ttf");
    }
    private val minuteDialTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_medium)
    }
    private val secondDialTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = context.resources.getFont(R.font.rubik_regular)
    }

    // Changed when setting changes cause a change in the minute hand arm (triggered by user in
    // updateUserStyle() via userStyleRepository.addUserStyleListener()).
    private var shiftPixelSettingChanged: Boolean = false

    // Default size of watch face drawing area, that is, a no size rectangle. Will be replaced with
    // valid dimensions from the system.
    private var currentWatchFaceSize = Rect(0, 0, 0, 0)

    // Is this expensive?
    private val batteryLevelChanged = BatteryLevelChangeReceiver(this)

    private fun isBatteryLow(): Boolean {
        return batteryLevelChanged.batteryLow
    }

    override fun onBatteryLevelChanged(oldValue: Boolean, newValue: Boolean) {
        interactiveDrawModeUpdateDelayMillis = if (isBatteryLow()) {
            FRAME_PERIOD_MS_LOW_BATTERY
        } else {
            FRAME_PERIOD_MS_DEFAULT
        }
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
    }

    override suspend fun createSharedAssets(): AnalogSharedAssets {
        return AnalogSharedAssets()
    }

    /*
     * Triggered when the user makes changes to the watch face through the settings activity. The
     * function is called by a flow.
     */
    private fun updateWatchFaceData(userStyle: UserStyle) {
        Log.d(TAG, "updateWatchFace(): $userStyle")

        var newWatchFaceData: WatchFaceData = watchFaceData

        // Loops through user style and applies new values to watchFaceData.
        for (options in userStyle) {
            when (options.key.id.toString()) {
                COLOR_STYLE_SETTING -> {
                    val listOption = options.value as
                        UserStyleSetting.ListUserStyleSetting.ListOption

                    newWatchFaceData = newWatchFaceData.copy(
                        activeColorStyle = ColorStyleIdAndResourceIds.getColorStyleConfig(
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
                DRAW_TIME_AOD_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        timeAOD = booleanValue.value
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

            interactiveDrawModeUpdateDelayMillis = if (isBatteryLow()) {
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
                    watchFaceColors.complicationStyleDrawableId
                )?.let {
                    (complication.renderer as CanvasComplicationDrawable).drawable = it
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        scope.cancel("ConcentricNativeCanvasRenderer scope clear() request")
        context.unregisterReceiver(batteryLevelChanged)
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

    private fun drawWatchFace(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean,
        isHalfFace: Boolean,
        scaledImage: Boolean
    ) {
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE) && !isBatteryLow()) {
            drawSecondsDial(
                canvas,
                bounds,
                watchFaceData.numberRadiusFraction,
                watchFaceColors.activePrimaryColor,
                watchFaceColors.activeOuterElementColor,
                zonedDateTime,
                isAmbient
            )
        }
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE) && (!isAmbient ||
                (!isBatteryLow() && isAmbient && watchFaceData.minuteDialAOD))
        ) {
            drawMinutesDial(
                canvas,
                bounds,
                zonedDateTime,
                isAmbient
            )
            drawShadows(canvas, bounds, isAmbient, isHalfFace, scaledImage)
        }
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY) &&
            !isAmbient || watchFaceData.timeAOD
        ) {
            drawDigitalTime(canvas, bounds, zonedDateTime, isHalfFace)
        }

    }


    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        update24Format(isAmbient)
        val scaledImage =
            watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.SCALED_HALFFACE.id
        val isHalfFace = watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.HALFFACE.id ||
            watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.SCALED_HALFFACE.id

        val backgroundColor = if (isAmbient) {
            watchFaceColors.ambientBackgroundColor
        } else {
            watchFaceColors.activeBackgroundColor
        }

        // Zoom the ambient watchface a bit larger, for viewing purposes, in ambient mode.
        // XXX - but maybe not do this or as an option
        if (isAmbient) {
            if (watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.FULLFACE.id) {
                canvas.scale(AOD_ZOOM_LEVEL_1, AOD_ZOOM_LEVEL_1, bounds.exactCenterX(), bounds.exactCenterY())
            } else if (!watchFaceData.compAOD) {
                canvas.scale(AOD_ZOOM_LEVEL_2, AOD_ZOOM_LEVEL_2, 0f, bounds.exactCenterY())
            }
        }

        if (isAmbient &&
            watchFaceData.shiftPixelAmount >= 1.0f
        ) {
            val cx = sin((zonedDateTime.minute % 60f) * 6f) * watchFaceData.shiftPixelAmount
            val cy = -cos((zonedDateTime.minute % 60f) * 6f) * watchFaceData.shiftPixelAmount
            canvas.translate(cx, cy)
        }

        var scaledShiftX = -bounds.width() * LAYOUT_ALT_CLOCK_SHIFT
        var scaledShiftY = 0f

        val restoreCount = canvas.save()
        if (scaledImage) {
            canvas.scale(SCALED_CLOCKFACE_AMOUNT, SCALED_CLOCKFACE_AMOUNT)
            scaledShiftX *= SCALED_WATCHFACE_SHIFTX
            scaledShiftY = -(bounds.height() * SCALED_WATCHFACE_SHIFTY - bounds.height()) / 2.0f
        }

        canvas.drawColor(backgroundColor)

        if (isHalfFace) {
            canvas.translate(scaledShiftX, scaledShiftY)
        }
        drawWatchFace(canvas, bounds, zonedDateTime, isAmbient, isHalfFace, scaledImage)

        canvas.restoreToCount(restoreCount)

        if (!isAmbient) {
            drawDateElement(canvas, bounds, zonedDateTime)
        }

        // CanvasComplicationDrawable already obeys rendererParameters.
        if ((!isAmbient || (!isBatteryLow() && watchFaceData.compAOD))) {
            drawComplications(canvas, zonedDateTime)
        }
    }

    // ----- All drawing functions -----
    private fun drawShadows(
        canvas: Canvas,
        bounds: Rect,
        isAmbient: Boolean,
        isHalfFace: Boolean,
        scaledImage: Boolean
    ) {
        if (isHalfFace) {
            val currentColor = translucentPaint.color
            translucentPaint.color = colorBlack
            val shadowLeftX = if (scaledImage) {
                bounds.width() * LAYOUT_ALT_CLOCK_SHIFT_SCALED
            } else {
                bounds.width() * LAYOUT_ALT_CLOCK_SHIFT
            }
            translucentPaint.isAntiAlias = false
            canvas.drawBitmap(shadowLeft, shadowLeftX, 0f, translucentPaint)
            canvas.drawRect(0f, 0f, shadowLeftX, bounds.height().toFloat(), translucentPaint)
            translucentPaint.isAntiAlias = true
            translucentPaint.color = currentColor
        } else {
            if (watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.FULLFACE.id &&
                (!isAmbient || (isAmbient && watchFaceData.compAOD))
            ) {
                canvas.drawArc(
                    bounds.width().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_OUTTER, bounds.height().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_OUTTER,
                    bounds.width().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_INNER, bounds.height().toFloat() * 0.88f,
                    70f, 40f, true, translucentPaint
                )
                canvas.drawArc(
                    bounds.width().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_OUTTER, bounds.height().toFloat() * 0.11f,
                    bounds.width().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_INNER, bounds.height().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_INNER,
                    250f, 40f, true, translucentPaint
                )
            }
        }
    }


    private fun drawComplications(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime
    ) {
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                val offset =
                    when (watchFaceData.layoutStyle.id) {
                        LayoutStyleIdAndResourceIds.HALFFACE.id -> {
                            0.25f
                        }
                        LayoutStyleIdAndResourceIds.SCALED_HALFFACE.id -> {
                            -0.25f
                        }
                        else -> {
                            0.0f
                        }
                    }

                when (complication.id) {
                    RIGHT_COMPLICATION_ID -> {
                        complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.left =
                            RIGHT_COMPLICATION_LEFT_BOUND + offset
                        complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.right =
                            RIGHT_COMPLICATION_RIGHT_BOUND + offset
                    }
                    LEFT_COMPLICATION_ID -> {
                        complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.left =
                            LEFT_COMPLICATION_LEFT_BOUND + offset
                        complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.right =
                            LEFT_COMPLICATION_RIGHT_BOUND + offset
                    }
                    MIDDLE_COMPLICATION_ID -> {
                        complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.left =
                            MIDDLE_COMPLICATION_LEFT_BOUND + offset
                        complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.right =
                            MIDDLE_COMPLICATION_RIGHT_BOUND + offset
                    }
                }

                if (complication.id == MIDDLE_COMPLICATION_ID &&
                    watchFaceData.layoutStyle.id != LayoutStyleIdAndResourceIds.HALFFACE.id
                ) {
                    complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.left =
                        MIDDLE_COMPLICATION_LEFT_BOUND + offset + MINUTE_HIGHLIGHT_WIDTH_FRACTION
                    complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.right =
                        MIDDLE_COMPLICATION_RIGHT_BOUND + offset + MINUTE_HIGHLIGHT_WIDTH_FRACTION
                    continue
                }

                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun configColors(drawAmbient: Boolean) {
        hourTextPaint.color = if (drawAmbient) {
            watchFaceColors.ambientPrimaryTextColor
        } else {
            watchFaceColors.activePrimaryTextColor
        }
        hourTextAmbientPaint.color = if (drawAmbient) {
            watchFaceColors.ambientPrimaryTextColor
        } else {
            watchFaceColors.activePrimaryTextColor
        }
        minuteTextPaint.color = if (drawAmbient) {
            watchFaceColors.ambientPrimaryTextColor
        } else {
            watchFaceColors.activePrimaryTextColor
        }
        minuteTextAmbientPaint.color = if (drawAmbient) {
            watchFaceColors.ambientPrimaryTextColor
        } else {
            watchFaceColors.activePrimaryTextColor
        }
        minuteHighlightPaint.color = if (drawAmbient) {
            watchFaceColors.ambientPrimaryColor
        } else {
            watchFaceColors.activePrimaryColor
        }
    }

    private fun drawMinuteHighlight(
        canvas: Canvas,
        bounds: Rect,
        drawAmbient: Boolean,
        isHalfFace: Boolean,
        cx: Float,
        cy: Float,
        sizeRadius: Float
    ) {
        minuteHighlightPaint.style = Paint.Style.STROKE
        minuteHighlightPaint.strokeWidth = 3.0f
        minuteHighlightPaint.color = if (!drawAmbient) {
            watchFaceColors.activePrimaryColor
        } else {
            darkenColor(watchFaceColors.activePrimaryColor)
        }

        val rightSide: Float = if (drawAmbient) {
            cx + sizeRadius
        } else {
            if (isHalfFace) {
                bounds.width().toFloat() + 10f
            } else {
                bounds.width() * MINUTE_HIGHLIGHT_WIDTH_FRACTION
            }
        }
        canvas.drawRoundRect(
            cx, cy,
            rightSide, cy + sizeRadius,
            sizeRadius, sizeRadius, minuteHighlightPaint
        )
    }

    private fun update24Format(drawAmbient: Boolean)
    {
        if ( renderCount % ((1000L/interactiveDrawModeUpdateDelayMillis)*10L) == 0L || drawAmbient) {
            is24Format = DateFormat.is24HourFormat(context)
            Log.d(TAG, "Updating 24 hour time")
        }
        renderCount++
    }

    private fun drawDigitalTime(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        isHalfFace: Boolean
    ) {

        if (currentWatchFaceSize != bounds || shiftPixelSettingChanged) {
            shiftPixelSettingChanged = false
            currentWatchFaceSize = bounds
        }

        val hourOfDay = zonedDateTime.toLocalTime().hour
        val minuteOfDay = zonedDateTime.toLocalTime().minute


        canvas.withScale(
            x = 1.0f,
            y = 1.0f,
            pivotX = bounds.exactCenterX(),
            pivotY = bounds.exactCenterY()
        ) {
            val drawAmbient = renderParameters.drawMode == DrawMode.AMBIENT
            configColors(drawAmbient)

            val textBounds = Rect()
            val realTextBounds = Rect()
            val formattedTime = if ( is24Format ) {
                hourOfDay
            } else {
                if (hourOfDay % 12 == 0) {
                    12
                } else {
                    hourOfDay % 12
                }
            }
            val txHour = "%02d".format(formattedTime)
            val biggestText = "88"
            var hourPaintToUse = hourTextPaint
            var minutePaintToUse = minuteTextPaint
            if (drawAmbient) {
                hourPaintToUse = hourTextAmbientPaint
                minutePaintToUse = minuteTextAmbientPaint
            }

            hourPaintToUse.textSize = bounds.height()*(HOUR_FONT_SIZE)
            minutePaintToUse.textSize = bounds.height()*(MINUTE_FONT_SIZE)

            hourTextPaint.getTextBounds(biggestText, 0, biggestText.length, textBounds)
            hourTextPaint.getTextBounds(txHour, 0, txHour.length, realTextBounds)
            val cxHour = bounds.exactCenterX() - realTextBounds.width().toFloat() * 0.63f
            val cyHour = bounds.exactCenterY() + textBounds.height().toFloat() * 0.5f
            val hourOffset = textBounds.width()
            val tx = "%02d".format(minuteOfDay)

            minutePaintToUse.getTextBounds(biggestText, 0, biggestText.length, textBounds)
            minutePaintToUse.getTextBounds(tx, 0, tx.length, realTextBounds)

            val sizeRadius = textBounds.height().toFloat() * 2.5f
            val cx = bounds.exactCenterX() + hourOffset * HOUR_TEXT_CENTER_OFFSET_SHIFT
            val cy = bounds.exactCenterY() - sizeRadius / 2f

            val currentColor = translucentPaint.color
            translucentPaint.color = watchFaceColors.activeBackgroundColor
            translucentPaint.style = Paint.Style.FILL


            // Draw the underside of the highlight
            if (!drawAmbient) {
                canvas.drawArc(
                    bounds.width().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_OUTTER, bounds.height().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_OUTTER,
                    bounds.width().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_INNER, bounds.height().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_INNER,
                    -21f, 42f, true, translucentPaint
                )
                canvas.drawRect(
                    bounds.width().toFloat() * 0.70f, bounds.height().toFloat() * 0.38f,
                    bounds.width().toFloat() * 0.82f, bounds.height().toFloat() * 0.62f,
                    translucentPaint
                )
            } else {
                canvas.drawArc(
                    bounds.width().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_OUTTER, bounds.height().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_OUTTER,
                    bounds.width().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_INNER, bounds.height().toFloat() * FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_INNER,
                    -18f, 36f, true, translucentPaint
                )
                canvas.drawRect(
                    bounds.width().toFloat() * 0.70f, bounds.height().toFloat() * 0.3825f,
                    bounds.width().toFloat() * 0.82f, bounds.height().toFloat() * 0.6175f,
                    translucentPaint
                )
            }

            translucentPaint.color = currentColor

            canvas.drawText(
                txHour,
                cxHour,
                cyHour,
                hourPaintToUse
            )

            canvas.drawText(
                tx,
                bounds.exactCenterX() + hourOffset * (MINUTE_TEXT_CENTER_OFFSET_SHIFT) + (textBounds.width() - realTextBounds.width()) / 2.0f,
                bounds.exactCenterY() + textBounds.height() / 2,
                minutePaintToUse
            )

            drawMinuteHighlight(canvas, bounds, drawAmbient, isHalfFace, cx, cy, sizeRadius)
        }
    }

    private fun drawDateElement(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
            val textBounds = Rect()
            var tx = zonedDateTime.toLocalDate().month.toString().substring(0, 3)
            calendarMonthPaint.getTextBounds(tx, 0, tx.length, textBounds)

            calendarMonthPaint.color = watchFaceColors.activeSecondaryColor
            canvas.drawCircle(
                bounds.exactCenterX(),
                bounds.exactCenterY() * 2 - 15f,
                textBounds.height().toFloat() * CAL_CIRCLE_RADIUS,
                calendarMonthPaint
            )

            calendarMonthPaint.color = if (renderParameters.drawMode == DrawMode.AMBIENT) {
                watchFaceColors.ambientSecondaryColor
            } else {
                watchFaceColors.activePrimaryColor
            }
            canvas.drawText(
                tx,
                bounds.exactCenterX() - (textBounds.width().toFloat() / 2.0f),
                bounds.exactCenterY() * 2 - (textBounds.height().toFloat() / 2.0f) * CAL_TEXT_OFFSET,
                calendarMonthPaint
            )

            tx = zonedDateTime.toLocalDate().dayOfMonth.toString()
            calendarDayPaint.getTextBounds(tx, 0, tx.length, textBounds)
            calendarDayPaint.color = hourTextPaint.color
            canvas.drawText(
                tx,
                bounds.exactCenterX() - (textBounds.width().toFloat() / 2.0f),
                bounds.exactCenterY() * 2 - (textBounds.height().toFloat() / 2.0f),
                calendarDayPaint
            )
        }
    }

    private fun drawSecondsDial(
        canvas: Canvas,
        bounds: Rect,
        numberRadiusFraction: Float,
        primaryColor: Int,
        outerElementColor: Int,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean
    ) {
        if (isAmbient)
            return
        outerElementPaint.color = outerElementColor
        minuteDialTextPaint.color = outerElementColor
        secondDialTextPaint.color = primaryColor
        // Draw and move seconds
        val textBounds = Rect()
        val nano = zonedDateTime.toLocalTime().nano.toFloat() / 1000000f
        val sec = zonedDateTime.toLocalTime().second
        textPaint.color = primaryColor
        for (i in 0 until 60) {
            val rotation =
                ((12.0f / 360.0f) * (i + sec + 15).toFloat() + ((12.0f / 360.0f) * (nano / 1000.0f))) * Math.PI
            if (i % 5 == 0) {
                val dx =
                    sin(rotation).toFloat() * (numberRadiusFraction - 0.01f) * bounds.width()
                        .toFloat()
                val dy =
                    -cos(rotation).toFloat() * (numberRadiusFraction - 0.01f) * bounds.height()
                        .toFloat()
                val tx = "%02d".format(((60 - i) % 60))
                secondDialTextPaint.getTextBounds(tx, 0, tx.length, textBounds)

                val stx = sin(rotation).toFloat() * (numberRadiusFraction) * (bounds.width()
                    .toFloat() + 30f)
                val sty =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction) * (bounds.height()
                        .toFloat() + 30f)

                val stx1 =
                    sin(rotation).toFloat() * (numberRadiusFraction + 0.05f) * bounds.width()
                        .toFloat()
                val sty1 =
                    -cos(rotation).toFloat() * (numberRadiusFraction + 0.05f) * bounds.width()
                        .toFloat()
                outerElementPaint.strokeWidth = 4f
                canvas.drawLine(
                    bounds.exactCenterX() + stx,
                    bounds.exactCenterY() + sty,
                    bounds.exactCenterX() + stx1,
                    bounds.exactCenterY() + sty1,
                    outerElementPaint
                )

                secondDialTextPaint.textSize = bounds.height()*(SECOND_DIAL_FONT_SIZE)
                canvas.drawText(
                    tx,
                    bounds.exactCenterX() + dx - textBounds.width() / 2.0f,
                    bounds.exactCenterY() + dy + textBounds.height() / 2.0f,
                    secondDialTextPaint
                )
            } else {

                val stx = sin(rotation).toFloat() * (numberRadiusFraction) * (bounds.width()
                    .toFloat() + 30f)
                val sty =
                    -cos(rotation).toFloat() * (numberRadiusFraction) * (bounds.height()
                        .toFloat() + 30f)

                val stx1 =
                    sin(rotation).toFloat() * (numberRadiusFraction + 0.05f) * bounds.width()
                        .toFloat()
                val sty1 =
                    -cos(rotation).toFloat() * (numberRadiusFraction + 0.05f) * bounds.width()
                        .toFloat()

                outerElementPaint.strokeWidth = 2f
                canvas.drawLine(
                    bounds.exactCenterX() + stx,
                    bounds.exactCenterY() + sty,
                    bounds.exactCenterX() + stx1,
                    bounds.exactCenterY() + sty1,
                    outerElementPaint
                )

            }
        }
    }


    private fun drawMinutesDial(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean
    ) {
        outerElementPaint.color =
            if (!isAmbient) watchFaceColors.activeOuterElementColor else watchFaceColors.ambientOuterElementColor
        minuteDialTextPaint.color =
            if (!isAmbient) watchFaceColors.activeOuterElementColor else watchFaceColors.ambientOuterElementColor
        secondDialTextPaint.color =
            if (!isAmbient) watchFaceColors.activePrimaryColor else watchFaceColors.ambientPrimaryColor
        // Draw and move seconds
        val numberRadiusFraction = watchFaceData.numberRadiusFraction
        val textBounds = Rect()
        val sec = zonedDateTime.toLocalTime().second


        val minute = zonedDateTime.toLocalTime().minute
        for (i in 0 until 60) {
            val rotation =
                ((12.0f / 360.0f) * (i - minute - 45).toFloat() - ((12.0f / 360.0f) * (sec.toFloat() / 60.0f))) * Math.PI
            if (i % 5 == 0) {

                val dx =
                    sin(rotation).toFloat() * (numberRadiusFraction - FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_OUTTER) * bounds.width()
                        .toFloat()
                val dy =
                    -cos(rotation).toFloat() * (numberRadiusFraction - FULL_WATCHFACE_COMPLICATION_SHADOW_EDGE_OUTTER) * bounds.width()
                        .toFloat()
                val tx = "%02d".format(i % 60)
                minuteDialTextPaint.getTextBounds(tx, 0, tx.length, textBounds)

                val stx =
                    sin(rotation).toFloat() * (numberRadiusFraction - MINUTE_DIAL_MAJOR_MARK_END_OFFSET) * bounds.width()
                        .toFloat()
                val sty =
                    -cos(rotation).toFloat() * (numberRadiusFraction - MINUTE_DIAL_MAJOR_MARK_END_OFFSET) * bounds.width()
                        .toFloat()

                val stx1 =
                    sin(rotation).toFloat() * (numberRadiusFraction - MINUTE_DIAL_START_OFFSET) * bounds.width()
                        .toFloat()
                val sty1 =
                    -cos(rotation).toFloat() * (numberRadiusFraction - MINUTE_DIAL_START_OFFSET) * bounds.width()
                        .toFloat()

                outerElementPaint.strokeWidth = 4f

                canvas.drawLine(
                    bounds.exactCenterX() + stx,
                    bounds.exactCenterY() + sty,
                    bounds.exactCenterX() + stx1,
                    bounds.exactCenterY() + sty1,
                    outerElementPaint
                )

                minuteDialTextPaint.textSize = bounds.height()*(MINUTE_DIAL_FONT_SIZE)
                canvas.drawText(
                    tx,
                    bounds.exactCenterX() + dx - textBounds.width() / 2.0f,
                    bounds.exactCenterY() + dy + textBounds.height() / 2.0f,
                    minuteDialTextPaint
                )
            } else {
                val stx =
                    sin(rotation).toFloat() * (numberRadiusFraction - MINUTE_DIAL_MINOR_MARK_END_OFFSET) * bounds.width()
                        .toFloat()
                val sty =
                    -cos(rotation).toFloat() * (numberRadiusFraction - MINUTE_DIAL_MINOR_MARK_END_OFFSET) * bounds.width()
                        .toFloat()

                val stx1 =
                    sin(rotation).toFloat() * (numberRadiusFraction - MINUTE_DIAL_START_OFFSET) * bounds.width()
                        .toFloat()
                val sty1 =
                    -cos(rotation).toFloat() * (numberRadiusFraction - MINUTE_DIAL_START_OFFSET) * bounds.width()
                        .toFloat()

                outerElementPaint.strokeWidth = 2f

                canvas.drawLine(
                    bounds.exactCenterX() + stx,
                    bounds.exactCenterY() + sty,
                    bounds.exactCenterX() + stx1,
                    bounds.exactCenterY() + sty1,
                    outerElementPaint
                )
            }
        }
    }


    companion object {
        private const val TAG = "ConcentricNativeCanvasRenderer"

    }
}
