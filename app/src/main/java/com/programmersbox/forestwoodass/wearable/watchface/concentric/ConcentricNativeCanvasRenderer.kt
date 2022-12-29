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
package com.programmersbox.forestwoodass.wearable.watchface.concentric

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import androidx.core.graphics.withScale
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.WatchFaceLayer
import com.programmersbox.forestwoodass.wearable.watchface.R
import com.programmersbox.forestwoodass.wearable.watchface.common.NativeCanvasRenderer
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.*
import com.programmersbox.forestwoodass.wearable.watchface.utils.*
import com.programmersbox.forestwoodass.wearable.watchface.utils.ColorUtils.Companion.darkenColor
import java.time.ZonedDateTime
import kotlinx.coroutines.*


/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
@Suppress("KotlinConstantConditions")
class ConcentricNativeCanvasRenderer(
    val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : NativeCanvasRenderer(
    context,
    surfaceHolder,
    watchState,
    complicationSlotsManager,
    currentUserStyleRepository,
    canvasType
){
    private var shadowLeft: Bitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.shadow_left
    )

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
                watchFaceColors.activePrimaryColor,
                watchFaceColors.activeOuterElementColor,
                zonedDateTime
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
            drawShadows(canvas, bounds, isHalfFace, scaledImage)
        }
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY) &&
            !isAmbient || watchFaceData.timeAOD
        ) {
            drawDigitalTime(canvas, bounds, zonedDateTime, isHalfFace)
        }

    }

    private fun scaleIfNeeded(
        canvas : Canvas,
        bounds : Rect,
        isAmbient : Boolean
    ) {
        // Zoom the ambient watchface a bit larger, for viewing purposes, in ambient mode.
        if (isAmbient) {
            if (watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.FULLFACE.id) {
                canvas.scale(AOD_ZOOM_LEVEL_1, AOD_ZOOM_LEVEL_1, bounds.exactCenterX(), bounds.exactCenterY())
            } else if ((!watchFaceData.compAOD && !watchFaceData.activeAsAmbient) || (watchFaceData.activeAsAmbient && watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.HALFFACE.id)) {
                canvas.scale(AOD_ZOOM_LEVEL_2, AOD_ZOOM_LEVEL_2, 0f, bounds.exactCenterY())
            } else if ((!watchFaceData.compAOD && !watchFaceData.activeAsAmbient)|| (watchFaceData.activeAsAmbient && watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.SCALED_HALFFACE.id)) {
                canvas.scale(AOD_ZOOM_LEVEL_4, AOD_ZOOM_LEVEL_4, 0f, bounds.exactCenterY()*1.2f)
            } else if ( watchFaceData.shiftPixelAmount >= 1.0f) {
                canvas.scale(AOD_ZOOM_LEVEL_3, AOD_ZOOM_LEVEL_3, bounds.exactCenterX(), bounds.exactCenterY())
            }
        }
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        super.render(canvas, bounds, zonedDateTime, sharedAssets)
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT && !watchFaceData.activeAsAmbient
        val scaledImage =
            watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.SCALED_HALFFACE.id
        val isHalfFace = watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.HALFFACE.id ||
            watchFaceData.layoutStyle.id == LayoutStyleIdAndResourceIds.SCALED_HALFFACE.id

        val scalingRestoreCount = canvas.save()
        scaleIfNeeded(canvas, bounds, renderParameters.drawMode == DrawMode.AMBIENT)

        var scaledShiftX = -bounds.width() * LAYOUT_ALT_CLOCK_SHIFT
        var scaledShiftY = 0f

        val restoreCount = canvas.save()
        if (scaledImage) {
            canvas.scale(SCALED_CLOCKFACE_AMOUNT, SCALED_CLOCKFACE_AMOUNT)
            scaledShiftX *= SCALED_WATCHFACE_SHIFTX
            scaledShiftY = -(bounds.height() * SCALED_WATCHFACE_SHIFTY - bounds.height()) / 2.0f
        }

        if (isHalfFace) {
            canvas.translate(scaledShiftX, scaledShiftY)
        }
        drawWatchFace(canvas, bounds, zonedDateTime, isAmbient, isHalfFace, scaledImage)

        canvas.restoreToCount(restoreCount)

        if (renderParameters.drawMode != DrawMode.AMBIENT) {
            drawDateElement(canvas, bounds, zonedDateTime, isAmbient)
        }
        if ( watchFaceData.layoutStyle.id != LayoutStyleIdAndResourceIds.FULLFACE.id ) {
            canvas.restoreToCount(scalingRestoreCount)
        }
        // CanvasComplicationDrawable already obeys rendererParameters.
        if ((renderParameters.drawMode != DrawMode.AMBIENT || (!isBatteryLow() && watchFaceData.compAOD))) {
            drawComplications(canvas, zonedDateTime)
        }
    }


    private fun drawShadows(
        canvas: Canvas,
        bounds: Rect,
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
        }
    }

    override fun drawComplications(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime
    ): Boolean {
        if (!super.drawComplications(canvas, zonedDateTime))
            return false
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                val offset =
                    when (watchFaceData.layoutStyle.id) {
                        LayoutStyleIdAndResourceIds.HALFFACE.id -> {
                            0.26f
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
        return true
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
        minuteHighlightPaint.color = when {
            !drawAmbient -> watchFaceColors.activePrimaryColor
            else -> darkenColor(watchFaceColors.activePrimaryColor)
        }

        val rightSide: Float = if (renderParameters.drawMode == DrawMode.AMBIENT || isBatteryLow()) {
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
            val drawAmbient = renderParameters.drawMode == DrawMode.AMBIENT && !watchFaceData.activeAsAmbient

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
            if (renderParameters.drawMode == DrawMode.AMBIENT) {
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
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean
    ) {
        if ( !watchFaceData.drawDate )
            return
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
            calendarMonthPaint.textSize = bounds.height()*(MONTH_FONT_SIZE)
            calendarDayPaint.textSize = bounds.height()*(DAY_FONT_SIZE)
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

            calendarMonthPaint.color = when {
                isAmbient ->watchFaceColors.ambientSecondaryColor
                else -> watchFaceColors.activePrimaryColor
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
        primaryColor: Int,
        outerElementColor: Int,
        zonedDateTime: ZonedDateTime
    ) {
        if (renderParameters.drawMode == DrawMode.AMBIENT)
            return
        outerElementPaint.color = outerElementColor
        minuteDialTextPaint.color = outerElementColor
        secondDialTextPaint.color = primaryColor
        textPaint.color = primaryColor

        // Draw and move seconds
        val textBounds = Rect()
        val nano = zonedDateTime.toLocalTime().nano.toFloat() / 1000000f
        val sec = zonedDateTime.toLocalTime().second
        val pos = canvas.save()

        canvas.rotate(sec*6f+(nano/1000f)*6f, bounds.exactCenterX(), bounds.exactCenterY())
        for (i in 0 until 60) {
            outerElementPaint.strokeWidth = if ( i % 5 == 0 ) {4f}else{2f}
            canvas.drawLine(
                bounds.width()*SECOND_DIAL_TICK_START,
                bounds.exactCenterY(),
                bounds.width().toFloat(),
                bounds.exactCenterY(),
                outerElementPaint
            )
            if ( i % 5 == 0 ) {
                secondDialTextPaint.textSize = bounds.height() * (SECOND_DIAL_FONT_SIZE)
                val tx = "%02d".format(((i) % 60))
                secondDialTextPaint.getTextBounds(tx, 0, tx.length, textBounds)

                val secPos = canvas.save()
                val x = bounds.width() - (textBounds.width() + bounds.width() * SECOND_NUMBER_START)
                val y = bounds.exactCenterY() + textBounds.height().toFloat() / 2
                canvas.rotate(-((sec-i)*6f+(nano/1000f)*6f), x+textBounds.width()/2f, y - textBounds.height().toFloat()/2 )
                canvas.drawText(
                    tx,
                    x, y,
                    secondDialTextPaint
                )
                canvas.restoreToCount(secPos)
            }
            canvas.rotate(-6f, bounds.exactCenterX(), bounds.exactCenterY())
        }
        canvas.restoreToCount(pos)
    }

    private fun drawMinutesDial(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean
    ) {
        outerElementPaint.color = when {
            !isAmbient -> watchFaceColors.activeOuterElementColor
            else -> watchFaceColors.ambientOuterElementColor
        }
        minuteDialTextPaint.color = when {
            !isAmbient -> watchFaceColors.activeOuterElementColor
            else -> watchFaceColors.ambientOuterElementColor
        }

        // Draw and move seconds
        val textBounds = Rect()
        val minute = zonedDateTime.toLocalTime().minute
        val sec = zonedDateTime.toLocalTime().second
        val pos = canvas.save()

        //canvas.rotate(180f, bounds.exactCenterX(), bounds.exactCenterY())
        canvas.rotate(-(minute*6f+(sec/60f)*6f)+180f, bounds.exactCenterX(), bounds.exactCenterY())
        for (i in 0 until 60) {
            val startPoint = when {
                (i%5) == 0 -> MINUTE_DIAL_MAJOR_MARK_END_OFFSET
                else -> MINUTE_DIAL_MINOR_MARK_END_OFFSET
            }
            outerElementPaint.strokeWidth = if ( i % 5 == 0 ) {4f}else{2f}
            canvas.drawLine(
                bounds.width()*startPoint,
                bounds.exactCenterY(),
                bounds.width().toFloat()*MINUTE_DIAL_START_OFFSET,
                bounds.exactCenterY(),
                outerElementPaint
            )
            if ( i % 5 == 0 ) {
                minuteDialTextPaint.textSize = bounds.height() * (MINUTE_DIAL_FONT_SIZE)
                val tx = "%02d".format(((i) % 60))
                minuteDialTextPaint.getTextBounds(tx, 0, tx.length, textBounds)

                val secPos = canvas.save()
                val x = bounds.width() - (textBounds.width() + bounds.width() * MINUTE_NUMBER_START)
                val y = bounds.exactCenterY() + textBounds.height().toFloat() / 2
                canvas.rotate(180f+((minute-i)*6f+(sec/60f)*6f), x+textBounds.width()/2f, y - textBounds.height().toFloat()/2 )
                canvas.drawText(
                    tx,
                    x, y,
                    minuteDialTextPaint
                )
                canvas.restoreToCount(secPos)
            }
            canvas.rotate(6f, bounds.exactCenterX(), bounds.exactCenterY())
        }
        canvas.restoreToCount(pos)
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "ConcentricNativeCanvasRenderer"
    }
}
