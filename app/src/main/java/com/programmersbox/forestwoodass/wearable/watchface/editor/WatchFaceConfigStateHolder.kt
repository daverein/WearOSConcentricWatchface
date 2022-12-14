/*
 * Copyright 2021 The Android Open Source Project
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
package com.programmersbox.forestwoodass.wearable.watchface.editor

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.editor.EditorSession
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.SHIFT_PIXEL_AOD_FRACTION_DEFAULT
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.SHIFT_PIXEL_AOD_FRACTION_MAXIMUM
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.SHIFT_PIXEL_AOD_FRACTION_MINIMUM
import com.programmersbox.forestwoodass.wearable.watchface.utils.*
import com.programmersbox.forestwoodass.wearable.watchface.utils.LEFT_COMPLICATION_ID
import com.programmersbox.forestwoodass.wearable.watchface.utils.RIGHT_COMPLICATION_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.yield

/**
 * Maintains the [WatchFaceConfigActivity] state, i.e., handles reads and writes to the
 * [EditorSession] which is basically the watch face data layer. This allows the user to edit their
 * watch face through [WatchFaceConfigActivity].
 *
 * Note: This doesn't use an Android ViewModel because the [EditorSession]'s constructor requires a
 * ComponentActivity and Intent (needed for the library's complication editing UI which is triggered
 * through the [EditorSession]). Generally, Activities and Views shouldn't be passed to Android
 * ViewModels, so this is named StateHolder to avoid confusion.
 *
 * Also, the scope is passed in and we recommend you use the of the lifecycleScope of the Activity.
 *
 * For the [EditorSession] itself, this class uses the keys, [UserStyleSetting], for each of our
 * user styles and sets their values [UserStyleSetting.Option]. After a new value is set, creates a
 * new image preview via screenshot class and triggers a listener (which creates new data for the
 * [StateFlow] that feeds back to the Activity).
 */
@Suppress("SpellCheckingInspection")
class WatchFaceConfigStateHolder(
    private val scope: CoroutineScope,
    private val activity: ComponentActivity
) {
    private lateinit var editorSession: EditorSession

    // Keys from Watch Face Data Structure
    private lateinit var colorStyleKey: UserStyleSetting.ListUserStyleSetting
    private lateinit var layoutStyleKey: UserStyleSetting.ListUserStyleSetting
    private lateinit var styleIconKey: UserStyleSetting.BooleanUserStyleSetting
    private lateinit var timeaodKey: UserStyleSetting.BooleanUserStyleSetting
    private lateinit var drawDateKey: UserStyleSetting.BooleanUserStyleSetting
    private lateinit var lowPowerKey: UserStyleSetting.BooleanUserStyleSetting
    private lateinit var compCirclesKey: UserStyleSetting.BooleanUserStyleSetting
    private lateinit var compaodKey: UserStyleSetting.BooleanUserStyleSetting
    private lateinit var activeAsAmbientKey: UserStyleSetting.BooleanUserStyleSetting
    private lateinit var minutedialaodKey: UserStyleSetting.BooleanUserStyleSetting
    private lateinit var shiftpixelamountKey: UserStyleSetting.DoubleRangeUserStyleSetting
    lateinit var serviceName: String

    val uiState: StateFlow<EditWatchFaceUiState> =
        flow<EditWatchFaceUiState> {
            editorSession = EditorSession.createOnWatchEditorSession(
                activity = activity
            )
            serviceName = editorSession.watchFaceComponentName.className
            extractsUserStyles(editorSession.userStyleSchema)

            emitAll(
                combine(
                    editorSession.userStyle,
                    editorSession.complicationsPreviewData
                ) { userStyle, complicationsPreviewData ->
                    yield()
                    EditWatchFaceUiState.Success(
                        createWatchFacePreview(userStyle, complicationsPreviewData)
                    )
                }
            )
        }
            .stateIn(
                scope + Dispatchers.Main.immediate,
                SharingStarted.Eagerly,
                EditWatchFaceUiState.Loading("Initializing")
            )

    private fun extractsUserStyles(userStyleSchema: UserStyleSchema) {
        // Loops through user styles and retrieves user editable styles.
        for (setting in userStyleSchema.userStyleSettings) {
            when (setting.id.toString()) {
                COLOR_STYLE_SETTING -> {
                    colorStyleKey = setting as UserStyleSetting.ListUserStyleSetting
                }

                LAYOUT_STYLE_SETTING -> {
                    layoutStyleKey = setting as UserStyleSetting.ListUserStyleSetting
                }
                DRAW_DATE_STYLE_SETTING -> {
                    drawDateKey = setting as UserStyleSetting.BooleanUserStyleSetting
                }
                LOW_POWER_STYLE_SETTING -> {
                    lowPowerKey = setting as UserStyleSetting.BooleanUserStyleSetting
                }
                DRAW_TIME_AOD_STYLE_SETTING -> {
                    timeaodKey = setting as UserStyleSetting.BooleanUserStyleSetting
                }
                STYLE_ICON_STYLE_SETTING -> {
                    styleIconKey = setting as UserStyleSetting.BooleanUserStyleSetting
                }
                DRAW_COMP_CIRCLES_STYLE_SETTING -> {
                    compCirclesKey = setting as UserStyleSetting.BooleanUserStyleSetting
                }
                COMPAOD_STYLE_SETTING -> {
                    compaodKey = setting as UserStyleSetting.BooleanUserStyleSetting
                }
                ACTIVE_AS_AMBIENT_STYLE_SETTING -> {
                    activeAsAmbientKey = setting as UserStyleSetting.BooleanUserStyleSetting
                }
                MINUTEDIALAOD_STYLE_SETTING -> {
                    minutedialaodKey = setting as UserStyleSetting.BooleanUserStyleSetting
                }
                SHIFT_PIXEL_STYLE_SETTING -> {
                    shiftpixelamountKey = setting as UserStyleSetting.DoubleRangeUserStyleSetting
                }
            }
        }
    }

    /* Creates a new bitmap render of the updated watch face and passes it along (with all the other
     * updated values) to the Activity to render.
     */
    private fun createWatchFacePreview(
        userStyle: UserStyle,
        complicationsPreviewData: Map<Int, ComplicationData>
    ): UserStylesAndPreview {
        Log.d(TAG, "updatesWatchFacePreview()")

        val bitmap = editorSession.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                RenderParameters.HighlightLayer(
                    RenderParameters.HighlightedElement.AllComplicationSlots,
                    Color.CYAN, // Red complication highlight.
                    Color.argb(0, 0, 0, 0) // Darken everything else.
                )
            ),
            editorSession.previewReferenceInstant,
            complicationsPreviewData
        )

        val colorStyle =
            userStyle[colorStyleKey] as UserStyleSetting.ListUserStyleSetting.ListOption
        val layoutStyle = if (::layoutStyleKey.isInitialized) {
            userStyle[layoutStyleKey] as UserStyleSetting.ListUserStyleSetting.ListOption
        } else {
            null
        }
        val timeaodEnabledStyle =
            userStyle[timeaodKey] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption
        val styleIconEnabledStyle =
            userStyle[styleIconKey] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption
        val drawDateEnabledStyle =
            userStyle[drawDateKey] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption
        val lowPowerEnabledStyle = if (::lowPowerKey.isInitialized) {
            userStyle[lowPowerKey] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption
        } else {
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.FALSE
        }
        val drawCompCirclesEnabledStyle =
            userStyle[compCirclesKey] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption
        val compaodEnabledStyle =
            userStyle[compaodKey] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption
        val activeAsAmbientEnabledStyle =
            userStyle[activeAsAmbientKey] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption
        val minuteDialAODEnabledStyle = if (::layoutStyleKey.isInitialized) {
            userStyle[minutedialaodKey] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption
        } else {
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.TRUE
        }
        val shiftpixelamountStyle =
            userStyle[shiftpixelamountKey]
                as UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption

        Log.d(TAG, "/new values: $colorStyle, $timeaodEnabledStyle, $shiftpixelamountStyle")

        return UserStylesAndPreview(
            colorStyleId = colorStyle.id.toString(),
            layoutStyleId = layoutStyle?.id.toString(),
            timeaodEnabled = timeaodEnabledStyle.value,
            styleIconEnabled = styleIconEnabledStyle.value,
            drawDateEnabled = drawDateEnabledStyle.value,
            lowPowerEnabled = lowPowerEnabledStyle.value,
            drawCompCirclesEnabled = drawCompCirclesEnabledStyle.value,
            compaodEnabled = compaodEnabledStyle.value,
            activeAsAmbientEnabled = activeAsAmbientEnabledStyle.value,
            minutedialaodEnabled = minuteDialAODEnabledStyle.value,
            shiftpixelamount = multiplyByMultipleForSlider(shiftpixelamountStyle.value).toFloat(),
            previewImage = bitmap
        )
    }

    fun setComplication(complicationLocation: Int) {
        val complicationSlotId = when (complicationLocation) {
            LEFT_COMPLICATION_ID -> LEFT_COMPLICATION_ID
            RIGHT_COMPLICATION_ID -> RIGHT_COMPLICATION_ID
            MIDDLE_COMPLICATION_ID -> MIDDLE_COMPLICATION_ID
            else -> return
        }
        try {
            scope.launch { editorSession.openComplicationDataSourceChooser(complicationSlotId) }
        }
        catch ( ex: java.lang.IllegalStateException)
        {
            // Not sure why this is, but let's catch it and hope its just a transient issue?
            Log.e(TAG, "Error in launching complication data source chooser")
        }
    }

    fun setColorStyle(newColorStyleId: String) {
        val userStyleSettingList = editorSession.userStyleSchema.userStyleSettings

        // Loops over all UserStyleSettings (basically the keys in the map) to find the setting for
        // the color style (which contains all the possible options for that style setting).
        for (userStyleSetting in userStyleSettingList) {
            if (userStyleSetting.id == UserStyleSetting.Id(COLOR_STYLE_SETTING)) {
                val colorUserStyleSetting =
                    userStyleSetting as UserStyleSetting.ListUserStyleSetting

                // Loops over the UserStyleSetting.Option colors (all possible values for the key)
                // to find the matching option, and if it exists, sets it as the color style.
                for (colorOptions in colorUserStyleSetting.options) {
                    if (colorOptions.id.toString() == newColorStyleId) {
                        setUserStyleOption(colorStyleKey, colorOptions)
                        return
                    }
                }
            }
        }
    }

    fun setLayoutStyle(newLayoutStyleId: String) {
        val userStyleSettingList = editorSession.userStyleSchema.userStyleSettings

        // Loops over all UserStyleSettings (basically the keys in the map) to find the setting for
        // the color style (which contains all the possible options for that style setting).
        for (userStyleSetting in userStyleSettingList) {
            if (userStyleSetting.id == UserStyleSetting.Id(LAYOUT_STYLE_SETTING)) {
                val layoutUserStyleSetting =
                    userStyleSetting as UserStyleSetting.ListUserStyleSetting

                // Loops over the UserStyleSetting.Option colors (all possible values for the key)
                // to find the matching option, and if it exists, sets it as the color style.
                for (layoutOptions in layoutUserStyleSetting.options) {
                    if (layoutOptions.id.toString() == newLayoutStyleId) {
                        setUserStyleOption(layoutStyleKey, layoutOptions)
                        return
                    }
                }
            }
        }
    }

    fun setTimeAOD(enabled: Boolean) {
        setUserStyleOption(
            timeaodKey,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }

    fun setStyleIcon(enabled: Boolean) {
        setUserStyleOption(
            styleIconKey,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }


    fun setDrawDate(enabled: Boolean) {
        setUserStyleOption(
            drawDateKey,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }

    fun setLowPower(enabled: Boolean) {
        setUserStyleOption(
            lowPowerKey,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }

    fun setDrawCompCircles(enabled: Boolean) {
        setUserStyleOption(
            compCirclesKey,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }

    fun setCompAOD(enabled: Boolean) {
        setUserStyleOption(
            compaodKey,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }

    fun setMinuteDialAOD(enabled: Boolean) {
        setUserStyleOption(
            minutedialaodKey,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }

    fun setActiveAsAmbient(enabled: Boolean) {
        setUserStyleOption(
            activeAsAmbientKey,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }

    fun setShiftPixelAmount(newShiftPixel: Float) {
        val newShiftPixelRatio = newShiftPixel.toDouble() / MULTIPLE_FOR_SLIDER

        setUserStyleOption(
            shiftpixelamountKey,
            UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption(newShiftPixelRatio)
        )
    }

    // Saves User Style Option change back to the back to the EditorSession.
    // Note: The UI widgets in the Activity that can trigger this method (through the 'set' methods)
    // will only be enabled after the EditorSession has been initialized.
    private fun setUserStyleOption(
        userStyleSetting: UserStyleSetting,
        userStyleOption: UserStyleSetting.Option
    ) {
        val mutableUserStyle = editorSession.userStyle.value.toMutableUserStyle()
        mutableUserStyle[userStyleSetting] = userStyleOption
        editorSession.userStyle.value = mutableUserStyle.toUserStyle()
    }

    sealed class EditWatchFaceUiState {
        data class Success(val userStylesAndPreview: UserStylesAndPreview) : EditWatchFaceUiState()
        data class Loading(val message: String) : EditWatchFaceUiState()
        data class Error(val exception: Throwable) : EditWatchFaceUiState()
    }

    data class UserStylesAndPreview(
        val colorStyleId: String,
        val layoutStyleId: String,
        val drawDateEnabled: Boolean,
        val lowPowerEnabled: Boolean,
        val styleIconEnabled: Boolean,
        val timeaodEnabled: Boolean,
        val compaodEnabled: Boolean,
        val drawCompCirclesEnabled: Boolean,
        val minutedialaodEnabled: Boolean,
        val activeAsAmbientEnabled: Boolean,
        val shiftpixelamount: Float,
        val previewImage: Bitmap
    )

    companion object {
        private const val TAG = "WatchFaceConfigStateHolder"

        // To convert the double representing the arm length to valid float value in the range the
        // slider can support, we need to multiply the original value times 1,000.
        private const val MULTIPLE_FOR_SLIDER: Float = 1f

        const val SHIFT_PIXEL_AOD_MINIMUM_FOR_SLIDER =
            SHIFT_PIXEL_AOD_FRACTION_MINIMUM * MULTIPLE_FOR_SLIDER

        const val SHIFT_PIXEL_AOD_MAXIMUM_FOR_SLIDER =
            SHIFT_PIXEL_AOD_FRACTION_MAXIMUM * MULTIPLE_FOR_SLIDER

        const val SHIFT_PIXEL_AOD_DEFAULT_FOR_SLIDER =
            SHIFT_PIXEL_AOD_FRACTION_DEFAULT * MULTIPLE_FOR_SLIDER

        private fun multiplyByMultipleForSlider(amountFraction: Double) =
            amountFraction * MULTIPLE_FOR_SLIDER
    }
}
