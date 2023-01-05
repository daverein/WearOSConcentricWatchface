package com.programmersbox.forestwoodass.wearable.watchface.reveal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime

private const val HOUR_HAND_WIDTH = 0.05f
private const val HOUR_HAND_EXTENT = 0.40f
private const val MINUTE_HAND_WIDTH = 0.04f
private const val MINUTE_HAND_EXTENT = 0.10f
private const val HOUR_MINUTE_HAND_STROKE = 1f
private const val HOUR_MINUTE_HAND_RADIUS = 10f
private const val HOUR_MINUTE_HANDLE_LENGTH = 0.10f

private const val ANALOG_DATE_POSITION = 0.75f

class AnalogNativeCanvasRenderer(
    context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
    ) : SecondsRevealNativeCanvasRenderer(
    context,
    surfaceHolder,
    watchState,
    complicationSlotsManager,
    currentUserStyleRepository,
    canvasType
) {
    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        super.render(canvas, bounds, zonedDateTime, sharedAssets)
        drawDial(canvas, bounds)

        // CanvasComplicationDrawable already obeys rendererParameters.
        if ((renderParameters.drawMode != DrawMode.AMBIENT || (!isBatteryLow() && watchFaceData.compAOD))) {
            drawComplications(canvas, zonedDateTime)
        }

        drawDateElement(
            canvas,
            bounds,
            zonedDateTime,
            (renderParameters.drawMode == DrawMode.AMBIENT || isBatteryLow()) && !watchFaceData.activeAsAmbient,
            ANALOG_DATE_POSITION
        )

        drawSecondsCircle(canvas, bounds, zonedDateTime, true)
        if (renderParameters.drawMode != DrawMode.AMBIENT || watchFaceData.timeAOD) {
            drawClockHands(canvas, bounds, zonedDateTime)

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
            (minuteHighlightPaint.color and 0x00ffffff) or 0x77000000

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
        canvas.drawRoundRect(
            bounds.exactCenterX() - (bounds.width() * MINUTE_HAND_WIDTH) / 2f,
            bounds.exactCenterY() - (bounds.height() * HOUR_MINUTE_HANDLE_LENGTH),
            bounds.exactCenterX() + (bounds.width() * MINUTE_HAND_WIDTH) / 2f,
            (bounds.height() / 2) * MINUTE_HAND_EXTENT,
            HOUR_MINUTE_HAND_RADIUS,
            HOUR_MINUTE_HAND_RADIUS,
            minuteTextPaint
        )

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
        canvas.drawRoundRect(
            bounds.exactCenterX() - (bounds.width() * HOUR_HAND_WIDTH) / 2f,
            bounds.exactCenterY() - (bounds.height() * HOUR_MINUTE_HANDLE_LENGTH),
            bounds.exactCenterX() + (bounds.width() * HOUR_HAND_WIDTH) / 2f,
            (bounds.height() / 2) * HOUR_HAND_EXTENT,
            HOUR_MINUTE_HAND_RADIUS,
            HOUR_MINUTE_HAND_RADIUS,
            minuteTextPaint
        )
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
}
