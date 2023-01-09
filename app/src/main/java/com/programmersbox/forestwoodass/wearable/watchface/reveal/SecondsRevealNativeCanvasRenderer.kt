package com.programmersbox.forestwoodass.wearable.watchface.reveal

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.WatchFaceLayer
import com.programmersbox.forestwoodass.wearable.watchface.R
import com.programmersbox.forestwoodass.wearable.watchface.common.NativeCanvasRenderer
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.*
import com.programmersbox.forestwoodass.wearable.watchface.utils.*
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

private const val DIAL_TICKS_MAJOR_STROKE = 4f
private const val DIAL_TICKS_MINOR_STROKE = 2f
private const val DIAL_TICKS_MAJOR_LENGTH = 0.94f
private const val DIAL_TICKS_MINOR_LENGTH = 0.98f
private const val SECONDS_CIRCLE_RADIUS = 0.08f
private const val SECONDS_CIRCLE_STROKE = 3f
private const val SECONDS_HAND_STROKE = 2f
private const val SECONDS_MAJOR_FONT_SIZE = (SECOND_DIAL_FONT_SIZE * 1.6f)
private const val SECONDS_MINOR_FONT_SIZE = (SECOND_DIAL_FONT_SIZE * 0.62f)
private const val SECONDS_EDGE_MINOR_PADDING = 0.99f
private const val SECONDS_EDGE_MAJOR_PADDING = 0.96f
private const val SECOND_MARK_LENGTH = 0.03f
private const val SECONDS_CIRCLE_OFFSET = 0.0025f

private const val DATE_FONT_SIZE = 0.05f

@Suppress("SpellCheckingInspection")
abstract class SecondsRevealNativeCanvasRenderer(
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
) {
    private var hiddenSecondsBitmap: Bitmap? = null
    private var hiddenSecondsCutoutMaskBitmap: Bitmap? = null
    private var hiddenSecondsResultBitmap: Bitmap? = null
    private val xferMode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

    private val secondPainter = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        xfermode = xferMode
    }

    init {
        calendarMonthPaint.typeface = context.resources.getFont(R.font.rubik_regular)
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

    }




    fun drawDateElement(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean,
        positionY: Float
    ) {
        if (!watchFaceData.drawDate || (renderParameters.drawMode == DrawMode.AMBIENT && !watchFaceData.timeAOD))
            return
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
            calendarMonthPaint.textSize = bounds.height() * (DATE_FONT_SIZE)
            val textBounds = Rect()
            val tx = "${zonedDateTime.toLocalDate().dayOfWeek.toString().substring(0, 3)}, ${
                zonedDateTime.toLocalDate().month.toString().substring(0, 3)
            } ${zonedDateTime.toLocalDate().dayOfMonth}"
            calendarMonthPaint.getTextBounds(tx, 0, tx.length, textBounds)
            calendarMonthPaint.color = when {
                isAmbient -> watchFaceColors.ambientSecondaryColor
                else -> watchFaceColors.activePrimaryTextColor
            }

            canvas.drawText(
                tx,
                bounds.exactCenterX() - (textBounds.width().toFloat() / 2.0f),
                bounds.height() * positionY,
                calendarMonthPaint
            )
        }
    }





    override fun drawComplications(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime
    ): Boolean {
        if ( !super.drawComplications(canvas, zonedDateTime))
            return false
        // A bit goofy, but readjust the complications from their initial positions to ones that's best
        // for this watchface
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
        return true
    }

    private fun createCurrentSecondsMaskCircle(
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ): Bitmap {
        val sec = zonedDateTime.toLocalTime().second
        val nano = zonedDateTime.toLocalTime().nano.toFloat() / 1000000f


        // Create the second/nano-time specific bitmap
        val maskC = Canvas()
        hiddenSecondsResultBitmap!!.eraseColor(Color.TRANSPARENT)
        maskC.setBitmap(hiddenSecondsResultBitmap!!)

        secondPainter.xfermode = null

        // Only copy an area around the target to reduce bitmap draw operations
        val cx = sin((0f+ (Math.toRadians((nano / 1000f) * 1f+zonedDateTime.second.toDouble()) )*6f )) * (bounds.width()/2 - (bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt()) + bounds.exactCenterX()
        val cy = -cos((0f+ (Math.toRadians((nano / 1000f) * 1f+zonedDateTime.second.toDouble()) )*6f )) * (bounds.width()/2 - (bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt()) + bounds.exactCenterY()
        maskC.drawBitmap(hiddenSecondsBitmap!!,
            Rect(cx.toInt()-(bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                cy.toInt()-(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                cx.toInt()+(bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                cy.toInt()+(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt()),
            Rect(cx.toInt()-(bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                cy.toInt()-(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                cx.toInt()+(bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                cy.toInt()+(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt()),
            secondPainter)

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
            Rect(
                (bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET) - bounds.height() * SECONDS_CIRCLE_RADIUS * 2).toInt(),
                (bounds.exactCenterY() - bounds.height() * SECONDS_CIRCLE_RADIUS * 2).toInt(),
                (bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET) + bounds.height() * SECONDS_CIRCLE_RADIUS * 2).toInt(),
                (bounds.exactCenterY() + bounds.height() * SECONDS_CIRCLE_RADIUS * 2).toInt()
            ),
            Rect(
                (bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET) - bounds.height() * SECONDS_CIRCLE_RADIUS * 2).toInt(),
                (bounds.exactCenterY() - bounds.height() * SECONDS_CIRCLE_RADIUS * 2).toInt(),
                (bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET) + bounds.height() * SECONDS_CIRCLE_RADIUS * 2).toInt(),
                (bounds.exactCenterY() + bounds.height() * SECONDS_CIRCLE_RADIUS * 2).toInt()
            ),
            secondPainter
        )

        secondPainter.xfermode = null
        return hiddenSecondsResultBitmap!!
    }

    fun drawSecondsCircle(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        drawSecondHand: Boolean
    ) {
        val sec = zonedDateTime.toLocalTime().second
        val nano = zonedDateTime.toLocalTime().nano.toFloat() / 1000000f

        // Draw second bullet if NOT in ambient mode
        if (renderParameters.drawMode != DrawMode.AMBIENT) {
            val secondsMaskedCircle = createCurrentSecondsMaskCircle(bounds, zonedDateTime)

            if ( drawSecondHand ) {
                // Draw the second hand
                canvas.save()
                canvas.rotate(
                    90f + sec * 6f + (nano / 1000f) * 6f,
                    bounds.exactCenterX(),
                    bounds.exactCenterY()
                )
                minuteTextPaint.strokeWidth = SECONDS_HAND_STROKE
                canvas.drawLine(
                    bounds.width() * SECONDS_CIRCLE_RADIUS, bounds.exactCenterY(),
                    bounds.exactCenterX(), bounds.exactCenterY(), minuteTextPaint
                )
                canvas.restore()
            }
            // Only copy an area around the target to reduce bitmap draw operations
            val cx = sin((0f+ (Math.toRadians((nano / 1000f) * 1f+zonedDateTime.second.toDouble()) )*6f )) * (bounds.width()/2 - (bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt()) + bounds.exactCenterX()
            val cy = -cos((0f+ (Math.toRadians((nano / 1000f) * 1f+zonedDateTime.second.toDouble()) )*6f )) * (bounds.width()/2 - (bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt()) + bounds.exactCenterY()

            // Draw the resulting cut out imaged into the bigger canvas
            canvas.drawBitmap(secondsMaskedCircle,
                Rect(cx.toInt()-(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                    cy.toInt()-(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                    cx.toInt()+(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                    cy.toInt()+(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt()),
                    Rect(cx.toInt()-(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                cy.toInt()-(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                cx.toInt()+(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt(),
                cy.toInt()+(bounds.height() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET)).toInt()),
                minuteTextPaint)

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

    fun drawDial(
        canvas: Canvas,
        bounds: Rect
    ) {
        if (renderParameters.drawMode == DrawMode.AMBIENT && (!watchFaceData.minuteDialAOD || isBatteryLow()))
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
            bounds.width() * (SECONDS_CIRCLE_RADIUS + SECONDS_CIRCLE_OFFSET),
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
