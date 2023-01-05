package com.programmersbox.forestwoodass.wearable.watchface.reveal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.WatchFaceLayer
import java.time.ZonedDateTime

private const val DIGITAL_TIME_POSITION = 0.47f
private const val DIGITAL_DATE_POSITION = 0.22f
private const val TIME_FONT_SIZE = 0.27f

class DigitalNativeCanvasRenderer(
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
            DIGITAL_DATE_POSITION
        )

        drawDigitalTime(canvas, bounds, zonedDateTime)

        drawSecondsCircle(canvas, bounds, zonedDateTime, false)
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
}
