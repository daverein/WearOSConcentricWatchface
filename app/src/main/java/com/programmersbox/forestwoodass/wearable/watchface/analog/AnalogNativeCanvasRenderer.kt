package com.programmersbox.forestwoodass.wearable.watchface.analog

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.WatchFaceLayer
import com.programmersbox.forestwoodass.wearable.watchface.common.NativeCanvasRenderer
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.*
import com.programmersbox.forestwoodass.wearable.watchface.utils.*
import java.time.ZonedDateTime

private const val DIAL_TICKS_MAJOR_STROKE = 4f
private const val DIAL_TICKS_MINOR_STROKE = 2f
private const val DIAL_TICKS_MAJOR_LENGTH = 0.94f
private const val DIAL_TICKS_MINOR_LENGTH = 0.98f
private const val HOUR_HAND_WIDTH = 0.05f
private const val HOUR_HAND_EXTENT = 0.40f
private const val MINUTE_HAND_WIDTH = 0.04f
private const val MINUTE_HAND_EXTENT = 0.10f
private const val HOUR_MINUTE_HAND_STROKE = 1f
private const val HOUR_MINUTE_HAND_RADIUS = 10f
private const val SECONDS_CIRCLE_RADIUS = 0.08f
private const val SECONDS_CIRCLE_STROKE = 3f
private const val SECONDS_MAJOR_FONT_SIZE = (SECOND_DIAL_FONT_SIZE * 1.6f)
private const val SECONDS_MINOR_FONT_SIZE = (SECOND_DIAL_FONT_SIZE * 0.62f)
private const val SECONDS_EDGE_MINOR_PADDING = 0.99f
private const val SECONDS_EDGE_MAJOR_PADDING = 0.96f
private const val SECOND_MARK_LENGTH = 0.03f
private const val HOUR_MINUTE_HANDLE_LENGTH = 0.10f
private const val SECONDS_CIRCLE_OFFSET = 0.004f


private const val DIGITAL_TIME_POSITION = 0.47f
private const val DIGITAL_DATE_POSITION = 0.20f
private const val ANALOG_DATE_POSITION = 0.75f
private const val TIME_FONT_SIZE = 0.27f

class AnalogNativeCanvasRenderer(
    val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int,
    drawDigital: Boolean
) : NativeCanvasRenderer(
    context,
    surfaceHolder,
    watchState,
    complicationSlotsManager,
    currentUserStyleRepository,
    canvasType
) {
    private val drawDigitalWatchFace = drawDigital
    private var hiddenSecondsBitmap: Bitmap? = null
    private var hiddenSecondsCutoutMaskBitmap: Bitmap? = null
    private var hiddenSecondsResultBitmap: Bitmap? = null
    private val xferMode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

    private val secondPainter = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        xfermode = xferMode
    }

    override fun onDestroy() {
        hiddenSecondsBitmap?.recycle()
        hiddenSecondsBitmap = null
        hiddenSecondsCutoutMaskBitmap?.recycle()
        hiddenSecondsCutoutMaskBitmap = null
        hiddenSecondsResultBitmap?.recycle()
        hiddenSecondsResultBitmap = null
        super.onDestroy()
    }

    override fun updateWatchFaceData(userStyle: UserStyle) {
        hiddenSecondsBitmap?.recycle()
        hiddenSecondsBitmap = null
        super.updateWatchFaceData(userStyle)
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        super.render(canvas, bounds, zonedDateTime, sharedAssets)
        hiddenSecondsBitmap = hiddenSecondsBitmap ?: createSecondsBitmap(bounds)
        hiddenSecondsCutoutMaskBitmap = hiddenSecondsCutoutMaskBitmap ?: createSecondsMaskBitmap(bounds)
        hiddenSecondsResultBitmap = hiddenSecondsResultBitmap ?: createResultsBitmap(bounds)

        // Prevent burn-in
        if (renderParameters.drawMode == DrawMode.AMBIENT &&
            watchFaceData.shiftPixelAmount >= 1.0f
        ) {
            val scaleValue =
                0.97f - (0.05f * (watchFaceData.shiftPixelAmount / SHIFT_PIXEL_AOD_FRACTION_MAXIMUM))
            canvas.scale(scaleValue, scaleValue, bounds.exactCenterX(), bounds.exactCenterY())
        }

        if (!drawDigitalWatchFace) {
            drawDial(canvas, bounds)
        }

        // CanvasComplicationDrawable already obeys rendererParameters.
        if ((renderParameters.drawMode != DrawMode.AMBIENT || (!isBatteryLow() && watchFaceData.compAOD))) {
            drawComplications(canvas, zonedDateTime)
        }

        drawDateElement(
            canvas,
            bounds,
            zonedDateTime,
            (renderParameters.drawMode == DrawMode.AMBIENT || isBatteryLow()) && !watchFaceData.activeAsAmbient
        )

        if (!drawDigitalWatchFace) {
            drawSecondsCircle(canvas, bounds, zonedDateTime)
        }
        drawTime(canvas, bounds, zonedDateTime)
        if (drawDigitalWatchFace) {
            drawSecondsCircle(canvas, bounds, zonedDateTime)
        }
    }

    private fun drawTime(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        if (renderParameters.drawMode != DrawMode.AMBIENT || watchFaceData.timeAOD) {
            if (!drawDigitalWatchFace) {
                drawClockHands(canvas, bounds, zonedDateTime)
            } else {
                drawDigitalTime(canvas, bounds, zonedDateTime)
            }
        }
    }
    private fun drawDigitalTime(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
            val fontSize = bounds.height() * TIME_FONT_SIZE
            val textBounds = Rect()
            val maxTextBounds = Rect()

            minuteHighlightPaint.textSize = fontSize * 0.7f
            var tx = ":"
            minuteHighlightPaint.getTextBounds(tx, 0, tx.length, textBounds)
            canvas.drawText(
                tx,
                bounds.exactCenterX() - (textBounds.width().toFloat()),
                bounds.height() * (DIGITAL_TIME_POSITION - 0.05f),
                minuteHighlightPaint
            )
            val colonWidth = textBounds.width().toFloat()
            val maxSize = "8"
            minuteHighlightPaint.textSize = fontSize
            minuteHighlightPaint.getTextBounds(maxSize, 0, maxSize.length, maxTextBounds)

            val formattedTime = if (is24Format) {
                zonedDateTime.toLocalTime().hour
            } else {
                if (zonedDateTime.toLocalTime().hour % 12 == 0) {
                    12
                } else {
                    zonedDateTime.toLocalTime().hour % 12
                }
            }

            tx = String.format("%02d", formattedTime)
            minuteHighlightPaint.getTextBounds(tx, 0, 1, textBounds)
            canvas.drawText(
                tx.subSequence(0, 1).toString(),
                (bounds.exactCenterX() - ((maxTextBounds.width()
                    .toFloat() + colonWidth * 1f) * 2 + colonWidth * 1.5f)) + (maxTextBounds.width() - textBounds.width()) / 2,
                bounds.height() * DIGITAL_TIME_POSITION,
                minuteHighlightPaint
            )
            minuteHighlightPaint.getTextBounds(tx, 1, 2, textBounds)
            canvas.drawText(
                tx.subSequence(1, 2).toString(),
                (bounds.exactCenterX() - ((maxTextBounds.width()
                    .toFloat() + colonWidth * 1f) + colonWidth * 1f)) + (maxTextBounds.width() - textBounds.width()) / 2,
                bounds.height() * DIGITAL_TIME_POSITION,
                minuteHighlightPaint
            )

            tx = String.format("%02d", zonedDateTime.toLocalTime().minute)
            minuteHighlightPaint.getTextBounds(tx, 0, tx.length, textBounds)
            canvas.drawText(
                tx,
                bounds.exactCenterX() + colonWidth,
                bounds.height() * DIGITAL_TIME_POSITION,
                minuteHighlightPaint
            )
        }
    }

    private fun drawDateElement(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean
    ) {
        if (!watchFaceData.drawDate || (renderParameters.drawMode == DrawMode.AMBIENT && !watchFaceData.timeAOD))
            return
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
            calendarMonthPaint.textSize = bounds.height() * (DAY_FONT_SIZE)
            calendarDayPaint.textSize = bounds.height() * (DAY_FONT_SIZE)
            val textBounds = Rect()
            val tx = "${zonedDateTime.toLocalDate().dayOfWeek.toString().substring(0, 3)}, ${
                zonedDateTime.toLocalDate().month.toString().substring(0, 3)
            } ${zonedDateTime.toLocalDate().dayOfMonth}"
            calendarMonthPaint.getTextBounds(tx, 0, tx.length, textBounds)
            calendarMonthPaint.color = when {
                isAmbient -> watchFaceColors.ambientSecondaryColor
                else -> watchFaceColors.activePrimaryTextColor
            }
            val positionY = if (drawDigitalWatchFace) {
                DIGITAL_DATE_POSITION
            } else {
                ANALOG_DATE_POSITION
            }
            canvas.drawText(
                tx,
                bounds.exactCenterX() - (textBounds.width().toFloat() / 2.0f),
                bounds.height() * positionY,
                calendarMonthPaint
            )
        }
    }

    private fun adjustComplicationsForAnalog(complication: ComplicationSlot) {
        val offsetLeft = 0.06f
        when (complication.id) {
            RIGHT_COMPLICATION_ID -> {
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.top =
                    MIDDLE_COMPLICATIONS_TOP_BOUND
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.bottom =
                    MIDDLE_COMPLICATIONS_BOTTOM_BOUND
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.left =
                    RIGHT_COMPLICATION_LEFT_BOUND - 0.25f
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.right =
                    RIGHT_COMPLICATION_RIGHT_BOUND - 0.25f
            }
            LEFT_COMPLICATION_ID -> {
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.top =
                    LEFT_COMPLICATIONS_TOP_BOUND + offsetLeft
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.bottom =
                    LEFT_COMPLICATIONS_BOTTOM_BOUND + offsetLeft
            }
            MIDDLE_COMPLICATION_ID -> {
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.left =
                    MIDDLE_COMPLICATION_LEFT_BOUND + 0.15f
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.right =
                    MIDDLE_COMPLICATION_RIGHT_BOUND + 0.15f
            }
        }
    }

    private fun adjustComplicationsForDigital(complication: ComplicationSlot) {
        val offsetLeft = 0.55f
        when (complication.id) {
            RIGHT_COMPLICATION_ID -> {
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.top =
                    MIDDLE_COMPLICATIONS_TOP_BOUND + 0.15f
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.bottom =
                    MIDDLE_COMPLICATIONS_BOTTOM_BOUND + 0.15f
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.left =
                    RIGHT_COMPLICATION_LEFT_BOUND - 0.25f
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.right =
                    RIGHT_COMPLICATION_RIGHT_BOUND - 0.25f
            }
            LEFT_COMPLICATION_ID -> {
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.top =
                    LEFT_COMPLICATIONS_TOP_BOUND + offsetLeft
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.bottom =
                    LEFT_COMPLICATIONS_BOTTOM_BOUND + offsetLeft
            }
            MIDDLE_COMPLICATION_ID -> {
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.left =
                    MIDDLE_COMPLICATION_LEFT_BOUND + 0.15f
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.right =
                    MIDDLE_COMPLICATION_RIGHT_BOUND + 0.15f
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.top =
                    MIDDLE_COMPLICATIONS_TOP_BOUND + 0.15f
                complication.complicationSlotBounds.perComplicationTypeBounds[ComplicationType.RANGED_VALUE]?.bottom =
                    MIDDLE_COMPLICATIONS_BOTTOM_BOUND + 0.15f
            }
        }
    }

    private fun drawComplications(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime
    ) {
        // A bit goofy, but readjust the complications from their initial positions to ones that's best
        // for this watchface
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                if (drawDigitalWatchFace) {
                    adjustComplicationsForDigital(complication)
                } else {
                    adjustComplicationsForAnalog(complication)
                }

                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun drawClockHands(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        val hour = zonedDateTime.toLocalTime().hour
        val minute = zonedDateTime.toLocalTime().minute
        val sec = zonedDateTime.toLocalTime().second
        val nano = zonedDateTime.toLocalTime().nano / 1000000L

        minuteHighlightPaint.color =
            (minuteHighlightPaint.color and 0x00ffffff) or 0xe7000000.toInt()

        minuteHighlightPaint.strokeWidth = HOUR_MINUTE_HAND_STROKE * 2f
        minuteHighlightPaint.strokeCap = Paint.Cap.ROUND
        minuteTextPaint.strokeWidth = HOUR_MINUTE_HAND_STROKE * 2f
        minuteTextPaint.strokeCap = Paint.Cap.ROUND
        minuteTextPaint.style = Paint.Style.STROKE
        translucentPaint.strokeWidth = HOUR_MINUTE_HAND_STROKE
        translucentPaint.style = Paint.Style.STROKE
        translucentPaint.strokeCap = Paint.Cap.ROUND
        hourTextPaint.style = Paint.Style.STROKE
        hourTextPaint.strokeWidth = HOUR_MINUTE_HAND_STROKE

        // Draw Minute hand
        canvas.save()
        canvas.rotate(
            (minute * 6f + (sec / 60f) * 6f + (nano / 1000L) * 6f),
            bounds.exactCenterX(),
            bounds.exactCenterY()
        )
        canvas.drawRoundRect(
            bounds.exactCenterX() - (bounds.width() * MINUTE_HAND_WIDTH) / 2f,
            bounds.exactCenterY() - (bounds.height() * HOUR_MINUTE_HANDLE_LENGTH),
            bounds.exactCenterX() + (bounds.width() * MINUTE_HAND_WIDTH) / 2f,
            (bounds.height() / 2) * MINUTE_HAND_EXTENT,
            HOUR_MINUTE_HAND_RADIUS,
            HOUR_MINUTE_HAND_RADIUS,
            minuteHighlightPaint
        )
        if (HOUR_MINUTE_HAND_STROKE != 0f) {
            canvas.drawRoundRect(
                bounds.exactCenterX() - (bounds.width() * MINUTE_HAND_WIDTH) / 2f,
                bounds.exactCenterY() - (bounds.height() * HOUR_MINUTE_HANDLE_LENGTH),
                bounds.exactCenterX() + (bounds.width() * MINUTE_HAND_WIDTH) / 2f,
                (bounds.height() / 2) * MINUTE_HAND_EXTENT,
                HOUR_MINUTE_HAND_RADIUS,
                HOUR_MINUTE_HAND_RADIUS,
                minuteTextPaint
            )
        }
        minuteTextPaint.strokeWidth = HOUR_MINUTE_HAND_STROKE * 5f
        minuteTextPaint.strokeCap = Paint.Cap.BUTT
        canvas.drawLine(
            bounds.exactCenterX(),
            bounds.exactCenterY(),
            bounds.exactCenterX(),
            bounds.exactCenterY() - (bounds.height() * HOUR_MINUTE_HANDLE_LENGTH),
            minuteTextPaint
        )
        minuteTextPaint.strokeWidth = HOUR_MINUTE_HAND_STROKE * 2f
        minuteTextPaint.strokeCap = Paint.Cap.ROUND
        canvas.restore()

        // Draw hour hand
        canvas.save()
        canvas.rotate(
            (hour * 30f + (minute / 60f) * 30f),
            bounds.exactCenterX(),
            bounds.exactCenterY()
        )
        canvas.drawRoundRect(
            bounds.exactCenterX() - (bounds.width() * HOUR_HAND_WIDTH) / 2f,
            bounds.exactCenterY() - (bounds.height() * HOUR_MINUTE_HANDLE_LENGTH),
            bounds.exactCenterX() + (bounds.width() * HOUR_HAND_WIDTH) / 2f,
            (bounds.height() / 2) * HOUR_HAND_EXTENT,
            HOUR_MINUTE_HAND_RADIUS,
            HOUR_MINUTE_HAND_RADIUS,
            minuteHighlightPaint
        )
        if (HOUR_MINUTE_HAND_STROKE != 0f) {
            canvas.drawRoundRect(
                bounds.exactCenterX() - (bounds.width() * HOUR_HAND_WIDTH) / 2f,
                bounds.exactCenterY() - (bounds.height() * HOUR_MINUTE_HANDLE_LENGTH),
                bounds.exactCenterX() + (bounds.width() * HOUR_HAND_WIDTH) / 2f,
                (bounds.height() / 2) * HOUR_HAND_EXTENT,
                HOUR_MINUTE_HAND_RADIUS,
                HOUR_MINUTE_HAND_RADIUS,
                minuteTextPaint
            )
        }
        minuteTextPaint.strokeWidth = HOUR_MINUTE_HAND_STROKE * 5f
        minuteTextPaint.strokeCap = Paint.Cap.BUTT
        canvas.drawLine(
            bounds.exactCenterX(),
            bounds.exactCenterY(),
            bounds.exactCenterX(),
            bounds.exactCenterY() - (bounds.height() * HOUR_MINUTE_HANDLE_LENGTH),
            minuteTextPaint
        )
        canvas.restore()

        // Draw the button in the center
        minuteTextPaint.strokeWidth = 0f
        minuteTextPaint.style = Paint.Style.FILL
        canvas.drawCircle(
            bounds.exactCenterX(),
            bounds.exactCenterY(),
            bounds.width() * 0.02f,
            minuteTextPaint
        )
        canvas.drawCircle(
            bounds.exactCenterX(),
            bounds.exactCenterY(),
            bounds.width() * 0.005f,
            blackPaint
        )
    }

    private fun createCurrentSecondsMaskCircle(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        val sec = zonedDateTime.toLocalTime().second
        val nano = zonedDateTime.toLocalTime().nano.toFloat() / 1000000f

        canvas.save()
        canvas.rotate(
            90f + sec * 6f + (nano / 1000f) * 6f,
            bounds.exactCenterX(),
            bounds.exactCenterY()
        )
        // Create the second/nano-time specific bitmap
        val maskC = Canvas()
        hiddenSecondsResultBitmap!!.eraseColor(Color.TRANSPARENT)
        maskC.setBitmap(hiddenSecondsResultBitmap!!)

        secondPainter.xfermode = null
        maskC.drawBitmap(hiddenSecondsBitmap!!, 0f, 0f, secondPainter)

        maskC.rotate(
            90f + sec * 6f + (nano / 1000f) * 6f,
            bounds.exactCenterX(),
            bounds.exactCenterY()
        )
        secondPainter.xfermode = xferMode
        secondPainter.color = 0xffffffff.toInt()
        secondPainter.style = Paint.Style.FILL

        maskC.drawBitmap(
            hiddenSecondsCutoutMaskBitmap!!,
            bounds.exactCenterX() - hiddenSecondsCutoutMaskBitmap!!.width / 2,
            0f,
            secondPainter
        )
        maskC.rotate(
            -sec * 6f - (nano / 1000f) * 6f,
            bounds.exactCenterX(),
            bounds.exactCenterY()
        )
        secondPainter.xfermode = null
        canvas.restore()
    }

    private fun drawSecondsCircle(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        val sec = zonedDateTime.toLocalTime().second
        val nano = zonedDateTime.toLocalTime().nano.toFloat() / 1000000f

        // Draw second bullet if NOT in ambient mode
        if (renderParameters.drawMode != DrawMode.AMBIENT) {
            createCurrentSecondsMaskCircle(canvas, bounds, zonedDateTime)

            // Draw the resulting cut out imaged into the bigger canvas
            canvas.drawBitmap(hiddenSecondsResultBitmap!!, 0f, 0f, minuteTextPaint)

            // Draw the seconds focus circle and the little tick mark pointer
            canvas.save()
            canvas.rotate(
                90f + sec * 6f + (nano / 1000f) * 6f,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )
            minuteTextPaint.strokeWidth = SECONDS_CIRCLE_STROKE
            minuteTextPaint.strokeCap = Paint.Cap.ROUND
            minuteTextPaint.style = Paint.Style.STROKE
            canvas.drawCircle(
                bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET),
                bounds.exactCenterY(),
                bounds.height() * SECONDS_CIRCLE_RADIUS,
                minuteTextPaint
            )
            canvas.drawLine(
                0f, bounds.exactCenterY(),
                bounds.width() * SECOND_MARK_LENGTH, bounds.exactCenterY(), minuteTextPaint
            )
            canvas.restore()
        }
    }

    private fun drawDial(
        canvas: Canvas,
        bounds: Rect
    ) {
        if (renderParameters.drawMode == DrawMode.AMBIENT && !watchFaceData.minuteDialAOD)
            return

        val pos = canvas.save()

        for (i in 0 until 60) {
            // Change paintToUse to secondHighlightPaint for the highlight colored dial
            val paintToUse = when {
                (i % 15) == 0 -> hourTextPaint
                else -> outerElementPaint
            }
            // Change these to be the same, if you want that look
            paintToUse.strokeWidth = when {
                (i % 5) == 0 -> DIAL_TICKS_MAJOR_STROKE
                else -> DIAL_TICKS_MINOR_STROKE
            }
            paintToUse.strokeCap = Paint.Cap.ROUND

            val startPoint = when {
                (i % 5) == 0 -> DIAL_TICKS_MAJOR_LENGTH
                else -> DIAL_TICKS_MINOR_LENGTH
            }
            canvas.drawLine(
                bounds.width() * startPoint,
                bounds.exactCenterY(),
                bounds.width().toFloat(),
                bounds.exactCenterY(),
                paintToUse
            )
//            if (i % 5 == 0) {
//                val textBounds = Rect()
//                val tx = "%d".format(((i) / 5))
//                hourTextPaint.getTextBounds(tx, 0, tx.length, textBounds)
//                val x = bounds.centerX() - (textBounds.width() / 2f)
//                val y = bounds.height() * (1f - startPoint) + textBounds.height() + 6f
//                canvas.save()
//                canvas.rotate(
//                    -i * 6f,
//                    x + textBounds.width() / 2f,
//                    y - textBounds.height().toFloat() / 2
//                )
//                hourTextPaint.textSize = bounds.height() * (SECOND_DIAL_FONT_SIZE)
//                canvas.drawText(
//                    tx,
//                    x, y,
//                    outerElementPaint
//                )
//                canvas.restore()
//            }
            canvas.rotate(6f, bounds.exactCenterX(), bounds.exactCenterY())
        }
        canvas.restoreToCount(pos)
    }

    private fun createSecondsMaskBitmap(bounds: Rect): Bitmap {
        val result: Bitmap =
            Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        paint.color = 0xffffffff.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawCircle(
            bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET),
            bounds.exactCenterY(),
            bounds.height() * SECONDS_CIRCLE_RADIUS,
            paint
        )
        return result
    }

    private fun createResultsBitmap(bounds: Rect): Bitmap {
        return Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
    }

    private fun createSecondsBitmap(bounds: Rect): Bitmap {
        val result: Bitmap =
            Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        drawSecondsDialBitmap(canvas, bounds)
        return result
    }

    private fun drawSecondsDialBitmap(
        canvas: Canvas,
        bounds: Rect
    ) {
        canvas.drawColor(colorBlack)
        val pos = canvas.save()

        for (i in 0 until 60) {
            val startPoint = when {
                (i % 5) == 0 -> SECONDS_EDGE_MAJOR_PADDING
                else -> SECONDS_EDGE_MINOR_PADDING
            }
            val fontSize = when {
                (i % 5) == 0 -> SECONDS_MAJOR_FONT_SIZE
                else -> SECONDS_MINOR_FONT_SIZE
            }
            outerElementPaint.color = when {
                (i % 5) == 0 -> watchFaceColors.activePrimaryColor
                else -> watchFaceColors.activeOuterElementColor
            }

            outerElementPaint.textSize = bounds.height() * (fontSize)
            val textBounds = Rect()
            val tx = "%02d".format(i)
            outerElementPaint.getTextBounds(tx, 0, tx.length, textBounds)
            val x = bounds.centerX() - (textBounds.width() / 2f)
            val y = bounds.height() * (1f - startPoint) + textBounds.height() + 6f
            canvas.save()
            canvas.rotate(
                -i * 6f,
                x + textBounds.width() / 2f,
                y - textBounds.height().toFloat() / 2
            )
            canvas.drawText(
                tx,
                x, y,
                outerElementPaint
            )
            canvas.restore()
            canvas.rotate(6f, bounds.exactCenterX(), bounds.exactCenterY())
        }
        canvas.restoreToCount(pos)
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "AnalogNativeCanvasRenderer"
    }
}
