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
import android.graphics.*
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.graphics.withScale
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.ColorStyleIdAndResourceIds
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.LayoutStyleIdAndResourceIds
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.WatchFaceColorPalette.Companion.convertToWatchFaceColorPalette
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.WatchFaceData
import com.programmersbox.forestwoodass.wearable.watchface.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

// Default for how long each frame is displayed at expected frame rate.
private const val FRAME_PERIOD_MS_DEFAULT: Long = 16L

/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
class AnalogWatchCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<AnalogWatchCanvasRenderer.AnalogSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {
    class AnalogSharedAssets : SharedAssets {
        override fun onDestroy() {
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

    // Initializes paint object for painting the clock hands with default values.
    private val clockHandPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth =
            context.resources.getDimensionPixelSize(R.dimen.clock_hand_stroke_width).toFloat()
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
        textSize = context.resources.getDimensionPixelSize(R.dimen.hour_text_size).toFloat()
        typeface = context.resources.getFont(R.font.rubik_regular)

    }
    private val hourTextAmbientPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.hour_text_size).toFloat()
        typeface = context.resources.getFont(R.font.rubik_light)

    }
    private val minuteTextAmbientPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.minute_text_size).toFloat()
        typeface = context.resources.getFont(R.font.rubik_light)

    }
    private val minuteTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.minute_text_size).toFloat()
        typeface = context.resources.getFont(R.font.rubik_regular)
    }
    private val minuteHightlightPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.minute_text_size).toFloat()
        // typeface = Typeface.createFromAsset(context.resources.assets, "Roboto.ttf");
    }
    private val minuteDialTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.minute_dial_text_size).toFloat()
        typeface = context.resources.getFont(R.font.rubik_medium)
    }
    private val secondDialTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.second_dial_text_size).toFloat()
        typeface = context.resources.getFont(R.font.rubik_regular)
    }

    // Changed when setting changes cause a change in the minute hand arm (triggered by user in
    // updateUserStyle() via userStyleRepository.addUserStyleListener()).
    private var armLengthChangedRecalculateClockHands: Boolean = false

    // Default size of watch face drawing area, that is, a no size rectangle. Will be replaced with
    // valid dimensions from the system.
    private var currentWatchFaceSize = Rect(0, 0, 0, 0)

    init {
        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                updateWatchFaceData(userStyle)
            }
        }
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
                        drawHourPips = booleanValue.value
                    )
                }
                COMPAOD_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        compAOD = booleanValue.value
                    )
                }
                SHIFT_PIXEL_STYLE_SETTING -> {
                    val doubleValue = options.value as
                        UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption

                    // The arm lengths are usually only calculated the first time the watch face is
                    // loaded to reduce the ops in the onDraw(). Because we updated the minute hand
                    // watch length, we need to trigger a recalculation.
                    armLengthChangedRecalculateClockHands = true

                    // Updates length of minute hand based on edits from user.
                    val newMinuteHandDimensions = newWatchFaceData.copy(
                        lengthFraction = doubleValue.value.toFloat()
                    )

                    newWatchFaceData = newWatchFaceData.copy(
                        lengthFraction = doubleValue.value.toFloat()
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
        scope.cancel("AnalogWatchCanvasRenderer scope clear() request")
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
        val backgroundColor = if (renderParameters.drawMode == DrawMode.AMBIENT) {
            watchFaceColors.ambientBackgroundColor
        } else {
            watchFaceColors.activeBackgroundColor
        }

        if (renderParameters.drawMode == DrawMode.AMBIENT &&
            watchFaceData.lengthFraction >= 1.0f
        ) {
            var cx = sin((zonedDateTime.minute % 60f) * 6f) * watchFaceData.lengthFraction
            var cy = -cos((zonedDateTime.minute % 60f) * 6f) * watchFaceData.lengthFraction
            canvas.translate(cx, cy)
        }

        canvas.drawColor(backgroundColor)
        val LAYOUT_ALT_CLOCK_SHIFT = 0.30f

        if (watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.HALFFACE.id) {
            canvas.translate(-bounds.width() * LAYOUT_ALT_CLOCK_SHIFT, 0f)
        }
        // if (renderParameters.drawMode == DrawMode.INTERACTIVE &&
        //     renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)
        ) {
            drawSecondsDial(
                canvas,
                bounds,
                watchFaceData.numberRadiusFraction,
                watchFaceData.numberStyleOuterCircleRadiusFraction,
                watchFaceColors.activePrimaryColor,
                watchFaceColors.activeOuterElementColor,
                watchFaceData.numberStyleOuterCircleRadiusFraction,
                watchFaceData.gapBetweenOuterCircleAndBorderFraction,
                zonedDateTime
            )
        }
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY) &&
            (renderParameters.drawMode != DrawMode.AMBIENT || watchFaceData.drawHourPips)
        ) {
            drawMinutesDial(
                canvas,
                bounds,
                watchFaceData.numberRadiusFraction,
                watchFaceData.numberStyleOuterCircleRadiusFraction,
                if (renderParameters.drawMode != DrawMode.AMBIENT) watchFaceColors.activePrimaryColor else watchFaceColors.ambientPrimaryColor,
                if (renderParameters.drawMode != DrawMode.AMBIENT) watchFaceColors.activeOuterElementColor else watchFaceColors.ambientOuterElementColor,
                watchFaceData.numberStyleOuterCircleRadiusFraction,
                watchFaceData.gapBetweenOuterCircleAndBorderFraction,
                zonedDateTime
            )
            if (renderParameters.drawMode != DrawMode.AMBIENT || watchFaceData.drawHourPips) {
                drawDigitalTime(canvas, bounds, zonedDateTime)
            }
        }

        //  canvas.translate(-bounds.width()*0.60f, 0f)
        // CanvasComplicationDrawable already obeys rendererParameters.
        if (renderParameters.drawMode != DrawMode.AMBIENT || watchFaceData.compAOD) {
            if (watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.HALFFACE.id) {
                canvas.translate(bounds.width() * LAYOUT_ALT_CLOCK_SHIFT, 0f)
                canvas.translate(bounds.width() * 0.25f, 0f)
            }
            drawComplications(canvas, zonedDateTime)
        }
    }

    // ----- All drawing functions -----
    private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                if ((complication.id == MIDDLE_COMPLICATION_ID &&
                        watchFaceData.layoutStyle.id != LayoutStyleIdAndResourceIds.HALFFACE.id)
                ) {
                    continue
                }
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun drawDigitalTime(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {

        if (currentWatchFaceSize != bounds || armLengthChangedRecalculateClockHands) {
            armLengthChangedRecalculateClockHands = false
            currentWatchFaceSize = bounds
        }

        val hourOfDay = zonedDateTime.toLocalTime().hour
        val minuteOfDay = zonedDateTime.toLocalTime().minute


        canvas.withScale(
            x = WATCH_HAND_SCALE,
            y = WATCH_HAND_SCALE,
            pivotX = bounds.exactCenterX(),
            pivotY = bounds.exactCenterY()
        ) {
            val drawAmbient = renderParameters.drawMode == DrawMode.AMBIENT

            clockHandPaint.style = if (drawAmbient) Paint.Style.STROKE else Paint.Style.FILL
            clockHandPaint.color = if (drawAmbient) {
                watchFaceColors.ambientSecondaryColor
            } else {
                watchFaceColors.activePrimaryColor
            }
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
            minuteHightlightPaint.color = if (drawAmbient) {
                watchFaceColors.ambientPrimaryColor
            } else {
                watchFaceColors.activePrimaryColor
            }

            val textBounds = Rect()
            val realTextBounds = Rect()
            val formattedTime = if (hourOfDay % 12 == 0) {
                12
            } else {
                hourOfDay % 12
            }
            var txHour = "%02d".format(formattedTime)
            val biggestText = "88"
            var hourPaintToUse = if (drawAmbient) {
                hourTextAmbientPaint
            } else {
                hourTextPaint
            }
            hourTextPaint.getTextBounds(biggestText, 0, biggestText.length, textBounds)
            hourTextPaint.getTextBounds(txHour, 0, txHour.length, realTextBounds)
            val cxHour = bounds.exactCenterX() - realTextBounds.width().toFloat() * 0.6f
            val cyHour = bounds.exactCenterY() + textBounds.height().toFloat() * 0.5f
//            canvas.drawText(
//                tx,
//                bounds.exactCenterX() - realTextBounds.width().toFloat() * 0.6f,
//                bounds.exactCenterY() + textBounds.height().toFloat() * 0.5f,
//                hourPaintToUse
//            )

            val hourOffset = textBounds.width()

            var tx = "%02d".format(minuteOfDay)
            var paintToUse = if (drawAmbient) {
                minuteTextAmbientPaint
            } else {
                minuteTextPaint
            }
            paintToUse.getTextBounds(biggestText, 0, biggestText.length, textBounds)

            val sizeRadius = textBounds.height().toFloat() * 1.9f
            val cx = bounds.exactCenterX() + hourOffset * 0.5f
            val cy = bounds.exactCenterY() - sizeRadius / 2;
            val focusModeWidth =
                if (watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.HALFFACE.id) {
                    1.5f
                } else {
                    3.0f
                }
            val ambientOffset = if (!drawAmbient) {
                focusModeWidth
            } else {
                1.0f
            }

            minuteHightlightPaint.style = Paint.Style.FILL
            minuteHightlightPaint.color = watchFaceColors.activeBackgroundColor


            // Draw the underside of the hightlight
            canvas.drawArc(bounds.width().toFloat()*0.15f, bounds.height().toFloat()*0.15f,
                bounds.width().toFloat()*0.85f, bounds.height().toFloat()*0.85f,
                -17f, 34f, true, minuteHightlightPaint)
            canvas.drawRect(bounds.width().toFloat()*0.70f, bounds.height().toFloat()*0.39f,
                bounds.width().toFloat()*0.82f, bounds.height().toFloat()*0.61f,
                minuteHightlightPaint )


            canvas.drawText(
                txHour,
                cxHour,
                cyHour,
                hourPaintToUse
            )

            canvas.drawText(
                tx,
                bounds.exactCenterX() + hourOffset * (0.55f),
                bounds.exactCenterY() + textBounds.height() / 2,
                paintToUse
            )

            minuteHightlightPaint.style = Paint.Style.STROKE
            minuteHightlightPaint.strokeWidth = 3.0f
            minuteHightlightPaint.color = clockHandPaint.color

            canvas.drawRoundRect(
                cx, cy,
                cx + (sizeRadius * ambientOffset), cy + sizeRadius,
                sizeRadius, sizeRadius, minuteHightlightPaint
            )

            if (!drawAmbient) {
                drawDateElement(canvas, bounds, zonedDateTime)
            }
        }
    }

    private fun drawDateElement(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        val textBounds = Rect()
        var tx = zonedDateTime.toLocalDate().month.toString().substring(0, 3)
        calendarMonthPaint.getTextBounds(tx, 0, tx.length, textBounds)

        calendarMonthPaint.color = watchFaceColors.activeSecondaryColor
        canvas.drawCircle(
            bounds.exactCenterX(),
            bounds.exactCenterY() * 2 - 15f,
            textBounds.height().toFloat() * 1.75f,
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
            bounds.exactCenterY() * 2 - (textBounds.height().toFloat() / 2.0f) * 4.5f,
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

    private fun drawSecondsDial(
        canvas: Canvas,
        bounds: Rect,
        numberRadiusFraction: Float,
        outerCircleStokeWidthFraction: Float,
        primaryColor: Int,
        outerElementColor: Int,
        numberStyleOuterCircleRadiusFraction: Float,
        gapBetweenOuterCircleAndBorderFraction: Float,
        zonedDateTime: ZonedDateTime
    ) {
        val drawAmbient = renderParameters.drawMode == DrawMode.AMBIENT

        if (drawAmbient)
            return
        outerElementPaint.strokeWidth = outerCircleStokeWidthFraction * bounds.width()
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
                (((12.0f / 360.0f)) * (i + sec + 15).toFloat() + ((12.0f / 360.0f) * (nano.toFloat() / 1000.0f))) * Math.PI
            if (i % 5 == 0) {
                val dx =
                    sin(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.01f) * bounds.width()
                        .toFloat()
                val dy =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.01f) * bounds.height()
                        .toFloat()
                val tx = "%02d".format(((60 - i) % 60))
                secondDialTextPaint.getTextBounds(tx, 0, tx.length, textBounds)

                val stx = sin(rotation * 1.0f).toFloat() * (numberRadiusFraction) * (bounds.width()
                    .toFloat() + 30f)
                val sty =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction) * (bounds.height()
                        .toFloat() + 30f)

                val stx1 =
                    sin(rotation * 1.0f).toFloat() * (numberRadiusFraction + 0.05f) * bounds.width()
                        .toFloat()
                val sty1 =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction + 0.05f) * bounds.width()
                        .toFloat()
                outerElementPaint.strokeWidth = 3f
                canvas.drawLine(
                    bounds.exactCenterX() + stx,
                    bounds.exactCenterY() + sty,
                    bounds.exactCenterX() + stx1,
                    bounds.exactCenterY() + sty1,
                    outerElementPaint
                )

                if (!drawAmbient) {
                    canvas.drawText(
                        tx,
                        bounds.exactCenterX() + dx - textBounds.width() / 2.0f,
                        bounds.exactCenterY() + dy + textBounds.height() / 2.0f,
                        secondDialTextPaint
                    )
                }
            } else {

                val stx = sin(rotation * 1.0f).toFloat() * (numberRadiusFraction) * (bounds.width()
                    .toFloat() + 30f)
                val sty =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction) * (bounds.height()
                        .toFloat() + 30f)

                val stx1 =
                    sin(rotation * 1.0f).toFloat() * (numberRadiusFraction + 0.05f) * bounds.width()
                        .toFloat()
                val sty1 =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction + 0.05f) * bounds.width()
                        .toFloat()

                outerElementPaint.strokeWidth = 1f
                canvas.drawLine(
                    bounds.exactCenterX() + stx,
                    bounds.exactCenterY() + sty,
                    bounds.exactCenterX() + stx1,
                    bounds.exactCenterY() + sty1,
                    outerElementPaint
                )

            }
        }

        // Draw seconds ticks
        outerElementPaint.strokeWidth = outerCircleStokeWidthFraction * bounds.width()
        outerElementPaint.color = outerElementColor
        canvas.save()
    }


    private fun drawMinutesDial(
        canvas: Canvas,
        bounds: Rect,
        numberRadiusFraction: Float,
        outerCircleStokeWidthFraction: Float,
        primaryColor: Int,
        outerElementColor: Int,
        numberStyleOuterCircleRadiusFraction: Float,
        gapBetweenOuterCircleAndBorderFraction: Float,
        zonedDateTime: ZonedDateTime
    ) {
        val drawAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        outerElementPaint.strokeWidth = outerCircleStokeWidthFraction * bounds.width()
        outerElementPaint.color = outerElementColor
        minuteDialTextPaint.color = outerElementColor
        secondDialTextPaint.color = primaryColor
        // Draw and move seconds
        val textBounds = Rect()
        val nano = zonedDateTime.toLocalTime().nano.toFloat() / 1000000f
        val sec = zonedDateTime.toLocalTime().second
        textPaint.color = primaryColor


        val minute = zonedDateTime.toLocalTime().minute
        textPaint.color = outerElementColor
        for (i in 0 until 60) {
            val rotation =
                (((12.0f / 360.0f)) * (i - minute - 45).toFloat() - ((12.0f / 360.0f) * (sec.toFloat() / 60.0f))) * Math.PI
            if (i % 5 == 0) {

                val dx =
                    sin(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.15f) * bounds.width()
                        .toFloat()
                val dy =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.15f) * bounds.width()
                        .toFloat()
                val tx = "%02d".format(i % 60)
                minuteDialTextPaint.getTextBounds(tx, 0, tx.length, textBounds)

                val stx =
                    sin(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.095f) * bounds.width()
                        .toFloat()
                val sty =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.095f) * bounds.width()
                        .toFloat()

                val stx1 =
                    sin(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.07f) * bounds.width()
                        .toFloat()
                val sty1 =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.07f) * bounds.width()
                        .toFloat()

                outerElementPaint.strokeWidth = 4f

                canvas.drawLine(
                    bounds.exactCenterX() + stx,
                    bounds.exactCenterY() + sty,
                    bounds.exactCenterX() + stx1,
                    bounds.exactCenterY() + sty1,
                    outerElementPaint
                )

                canvas.drawText(
                    tx,
                    bounds.exactCenterX() + dx - textBounds.width() / 2.0f,
                    bounds.exactCenterY() + dy + textBounds.height() / 2.0f,
                    minuteDialTextPaint
                )
            } else {
                val stx =
                    sin(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.085f) * bounds.width()
                        .toFloat()
                val sty =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.085f) * bounds.width()
                        .toFloat()

                val stx1 =
                    sin(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.07f) * bounds.width()
                        .toFloat()
                val sty1 =
                    -cos(rotation * 1.0f).toFloat() * (numberRadiusFraction - 0.07f) * bounds.width()
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
        private const val TAG = "AnalogWatchCanvasRenderer"

        // Used to canvas.scale() to scale watch hands in proper bounds. This will always be 1.0.
        private const val WATCH_HAND_SCALE = 1.0f
    }
}
