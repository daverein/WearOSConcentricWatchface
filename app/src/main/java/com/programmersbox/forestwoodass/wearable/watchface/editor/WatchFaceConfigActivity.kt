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
import com.programmersbox.forestwoodass.wearable.watchface.editor.WatchFaceConfigStateHolder.Companion.MINUTE_HAND_LENGTH_DEFAULT_FOR_SLIDER
import com.programmersbox.forestwoodass.wearable.watchface.editor.WatchFaceConfigStateHolder.Companion.MINUTE_HAND_LENGTH_MAXIMUM_FOR_SLIDER
import com.programmersbox.forestwoodass.wearable.watchface.editor.WatchFaceConfigStateHolder.Companion.MINUTE_HAND_LENGTH_MINIMUM_FOR_SLIDER
import com.programmersbox.forestwoodass.wearable.watchface.utils.LEFT_COMPLICATION_ID
import com.programmersbox.forestwoodass.wearable.watchface.utils.MIDDLE_COMPLICATION_ID
import com.programmersbox.forestwoodass.wearable.watchface.utils.RIGHT_COMPLICATION_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        binding = ActivityWatchFaceConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable widgets until data loads and values are set.
        binding.colorStylePickerButton.isEnabled = false
        binding.ticksEnabledSwitch.isEnabled = false
        binding.minuteHandLengthSlider.isEnabled = false
        binding.compaodEnabledSwitch.isEnabled = false

        // Set max and min.
        binding.minuteHandLengthSlider.valueTo = MINUTE_HAND_LENGTH_MAXIMUM_FOR_SLIDER
        binding.minuteHandLengthSlider.valueFrom = MINUTE_HAND_LENGTH_MINIMUM_FOR_SLIDER
        binding.minuteHandLengthSlider.value = MINUTE_HAND_LENGTH_DEFAULT_FOR_SLIDER

        binding.minuteHandLengthSlider.addOnChangeListener { slider, value, fromUser ->
            Log.d(TAG, "addOnChangeListener(): $slider, $value, $fromUser")
            stateHolder.setMinuteHandArmLength(value)
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

        binding.ticksEnabledSwitch.isChecked = userStylesAndPreview.ticksEnabled
        binding.compaodEnabledSwitch.isChecked = userStylesAndPreview.compaodEnabled
        binding.minuteHandLengthSlider.value = userStylesAndPreview.minuteHandLength
        binding.preview.watchFaceBackground.setImageBitmap(userStylesAndPreview.previewImage)

        enabledWidgets()
    }

    private fun enabledWidgets() {
        binding.colorStylePickerButton.isEnabled = true
        binding.ticksEnabledSwitch.isEnabled = true
        binding.minuteHandLengthSlider.isEnabled = true
        binding.compaodEnabledSwitch.isEnabled = true
    }

    fun onClickColorStylePickerButton(view: View) {
        Log.d(TAG, "onClickColorStylePickerButton() $view")

        // TODO (codingjeremy): Replace with a RecyclerView to choose color style (next CL)
        // Selects a random color style from list.
        val colorStyleIdAndResourceIdsList = enumValues<ColorStyleIdAndResourceIds>()
        val newColorStyle: ColorStyleIdAndResourceIds = colorStyleIdAndResourceIdsList.random()

        stateHolder.setColorStyle(newColorStyle.id)
    }
    fun onClickLayoutStylePickerButton(view: View) {
        Log.d(TAG, "onClickLayoutStylePickerButton() $view")

        // TODO (codingjeremy): Replace with a RecyclerView to choose color style (next CL)
        // Selects a random color style from list.
        val layoutStyleIdAndResourceIdsList = enumValues<LayoutStyleIdAndResourceIds>()
        val newLayoutStyle: LayoutStyleIdAndResourceIds = layoutStyleIdAndResourceIdsList.random()

        stateHolder.setLayoutStyle(newLayoutStyle.id)
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

    fun onClickTicksEnabledSwitch(view: View) {
        Log.d(TAG, "onClickTicksEnabledSwitch() $view")
        stateHolder.setDrawPips(binding.ticksEnabledSwitch.isChecked)
    }
    fun onClickCompAODEnabledSwitch(view: View) {
        Log.d(TAG, "onClickTicksEnabledSwitch() $view")
        stateHolder.setCompAOD(binding.compaodEnabledSwitch.isChecked)
    }
    companion object {
        const val TAG = "WatchFaceConfigActivity"
    }
}
