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

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.ColorStyleIdAndResourceIds
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.LayoutStyleIdAndResourceIds
import com.programmersbox.forestwoodass.wearable.watchface.databinding.ActivityWatchFaceConfigBinding
import com.programmersbox.forestwoodass.wearable.watchface.editor.WatchFaceConfigStateHolder.Companion.SHIFT_PIXEL_AOD_DEFAULT_FOR_SLIDER
import com.programmersbox.forestwoodass.wearable.watchface.editor.WatchFaceConfigStateHolder.Companion.SHIFT_PIXEL_AOD_MAXIMUM_FOR_SLIDER
import com.programmersbox.forestwoodass.wearable.watchface.editor.WatchFaceConfigStateHolder.Companion.SHIFT_PIXEL_AOD_MINIMUM_FOR_SLIDER
import com.programmersbox.forestwoodass.wearable.watchface.utils.LEFT_COMPLICATION_ID
import com.programmersbox.forestwoodass.wearable.watchface.utils.MIDDLE_COMPLICATION_ID
import com.programmersbox.forestwoodass.wearable.watchface.utils.RIGHT_COMPLICATION_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Allows user to edit certain parts of the watch face (color style, ticks displayed, minute arm
 * length) by using the [WatchFaceConfigStateHolder]. (All widgets are disabled until data is
 * loaded.)
 */
class WatchFaceConfigActivity : ComponentActivity() {
    private val stateHolder: WatchFaceConfigStateHolder by lazy {
        WatchFaceConfigStateHolder(
            lifecycleScope,
            this@WatchFaceConfigActivity
        )
    }

    private lateinit var binding: ActivityWatchFaceConfigBinding
    private  var currentLayoutId : String = ""
    private  var currentColorId : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        binding = ActivityWatchFaceConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable widgets until data loads and values are set.
        binding.colorStylePickerButton.isEnabled = false
        binding.timeaodEnabledSwitch.isEnabled = false
        binding.shiftPixelAmountSlider.isEnabled = false
        binding.compaodEnabledSwitch.isEnabled = false
        binding.minutedialaodEnabledSwitch.isEnabled = false
        currentLayoutId = ""
        currentColorId = ""

        // Set max and min.
        binding.shiftPixelAmountSlider.valueTo = SHIFT_PIXEL_AOD_MAXIMUM_FOR_SLIDER
        binding.shiftPixelAmountSlider.valueFrom = SHIFT_PIXEL_AOD_MINIMUM_FOR_SLIDER
        binding.shiftPixelAmountSlider.value = SHIFT_PIXEL_AOD_DEFAULT_FOR_SLIDER

        binding.shiftPixelAmountSlider.addOnChangeListener { slider, value, fromUser ->
            Log.d(TAG, "addOnChangeListener(): $slider, $value, $fromUser")
            stateHolder.setShiftPixelAmount(value)
        }

        lifecycleScope.launch(Dispatchers.Main.immediate) {
            stateHolder.uiState
                .collect { uiState: WatchFaceConfigStateHolder.EditWatchFaceUiState ->
                    when (uiState) {
                        is WatchFaceConfigStateHolder.EditWatchFaceUiState.Loading -> {
                            Log.d(TAG, "StateFlow Loading: ${uiState.message}")
                        }
                        is WatchFaceConfigStateHolder.EditWatchFaceUiState.Success -> {
                            Log.d(TAG, "StateFlow Success.")
                            updateWatchFacePreview(uiState.userStylesAndPreview)
                        }
                        is WatchFaceConfigStateHolder.EditWatchFaceUiState.Error -> {
                            Log.e(TAG, "Flow error: ${uiState.exception}")
                        }
                    }
                }
        }
    }

    private fun updateWatchFacePreview(
        userStylesAndPreview: WatchFaceConfigStateHolder.UserStylesAndPreview
    ) {
        Log.d(TAG, "updateWatchFacePreview: $userStylesAndPreview")

        val colorStyleId: String = userStylesAndPreview.colorStyleId
        Log.d(TAG, "\tselected color style: $colorStyleId")

        binding.timeaodEnabledSwitch.isChecked = userStylesAndPreview.timeaodEnabled
        binding.compaodEnabledSwitch.isChecked = userStylesAndPreview.compaodEnabled
        binding.minutedialaodEnabledSwitch.isChecked = userStylesAndPreview.minutedialaodEnabled
        binding.shiftPixelAmountSlider.value = userStylesAndPreview.shiftpixelamount
        currentLayoutId = userStylesAndPreview.layoutStyleId
        currentColorId = userStylesAndPreview.colorStyleId
        binding.preview.watchFaceBackground.setImageBitmap(userStylesAndPreview.previewImage)
        setColorObject(userStylesAndPreview.colorStyleId)
        setLayoutObject(userStylesAndPreview.layoutStyleId)
        enabledWidgets()
    }

    private fun enabledWidgets() {
        binding.colorStylePickerButton.isEnabled = true
        binding.timeaodEnabledSwitch.isEnabled = true
        binding.shiftPixelAmountSlider.isEnabled = true
        binding.compaodEnabledSwitch.isEnabled = true
        binding.minutedialaodEnabledSwitch.isEnabled = true
    }

    private fun setColorObject(id: String)
    {
        val colorStyleIdAndResourceIdsList = enumValues<ColorStyleIdAndResourceIds>()
        for ( i in 0 until colorStyleIdAndResourceIdsList.size-1) {
            val r = colorStyleIdAndResourceIdsList[i]
            if ( id == r.id ) {
                binding.currentColorStyleIcon.setImageResource(colorStyleIdAndResourceIdsList[(i)%colorStyleIdAndResourceIdsList.size].iconResourceId)
            }
        }
    }

    fun onClickColorStylePickerButton(view: View) {
        Log.d(TAG, "onClickColorStylePickerButton() $view")

        val colorStyleIdAndResourceIdsList = enumValues<ColorStyleIdAndResourceIds>()
        var newColorStyle = ""
        for ( i in 0 until colorStyleIdAndResourceIdsList.size-1) {
            val r = colorStyleIdAndResourceIdsList[i]
            if ( currentColorId == r.id ) {
                newColorStyle = colorStyleIdAndResourceIdsList[(i+1)%colorStyleIdAndResourceIdsList.size].id
                binding.currentColorStyleIcon.setImageResource(colorStyleIdAndResourceIdsList[(i+1)%colorStyleIdAndResourceIdsList.size].iconResourceId)
            }
        }
        stateHolder.setColorStyle(newColorStyle)
    }

    private fun setLayoutObject(id: String)
    {
        val layoutStyleIdAndResourceIdsList = enumValues<LayoutStyleIdAndResourceIds>()
        for ( i in 0..layoutStyleIdAndResourceIdsList.size-1) {
            val r = layoutStyleIdAndResourceIdsList[i]
            if ( id == r.id ) {
                binding.currentLayoutStyleIcon.setImageResource(layoutStyleIdAndResourceIdsList[(i)%layoutStyleIdAndResourceIdsList.size].iconResourceId)
            }
        }
    }

    fun onClickLayoutStylePickerButton(view: View) {
        Log.d(TAG, "onClickLayoutStylePickerButton() $view")

        val layoutStyleIdAndResourceIdsList = enumValues<LayoutStyleIdAndResourceIds>()
        var newLayoutStyle = ""
        for ( i in 0 until layoutStyleIdAndResourceIdsList.size-1) {
            val r = layoutStyleIdAndResourceIdsList[i]
            if ( currentLayoutId == r.id ) {
                newLayoutStyle = layoutStyleIdAndResourceIdsList[(i+1)%layoutStyleIdAndResourceIdsList.size].id
                binding.currentLayoutStyleIcon.setImageResource(layoutStyleIdAndResourceIdsList[(i+1)%layoutStyleIdAndResourceIdsList.size].iconResourceId)
            }
        }

        stateHolder.setLayoutStyle(newLayoutStyle)

    }

    fun onClickLeftComplicationButton(view: View) {
        Log.d(TAG, "onClickLeftComplicationButton() $view")
        stateHolder.setComplication(LEFT_COMPLICATION_ID)
    }

    fun onClickMiddleComplicationButton(view: View) {
        Log.d(TAG, "onClickLeftComplicationButton() $view")
        stateHolder.setComplication(MIDDLE_COMPLICATION_ID)
    }

    fun onClickRightComplicationButton(view: View) {
        Log.d(TAG, "onClickRightComplicationButton() $view")
        stateHolder.setComplication(RIGHT_COMPLICATION_ID)
    }

    fun onClickTimeAODEnabledSwitch(view: View) {
        Log.d(TAG, "onClickTimeAODEnabledSwitch() $view")
        stateHolder.setTimeAOD(binding.timeaodEnabledSwitch.isChecked)
    }
    fun onClickCompAODEnabledSwitch(view: View) {
        Log.d(TAG, "onClickCompAODEnabledSwitch() $view")
        stateHolder.setCompAOD(binding.compaodEnabledSwitch.isChecked)
    }
    fun onClickMinuteDialAODEnabledSwitch(view: View) {
        Log.d(TAG, "onClickMinuteDialAODEnabledSwitch() $view")
        stateHolder.setMinuteDialAOD(binding.minutedialaodEnabledSwitch.isChecked)
    }
    companion object {
        const val TAG = "WatchFaceConfigActivity"
    }
}
