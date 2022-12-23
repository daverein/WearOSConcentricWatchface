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
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.programmersbox.forestwoodass.wearable.watchface.data.watchface.ColorStylesDynamic
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
        binding.drawdateEnabledSwitch.isEnabled = false
        binding.timeaodEnabledSwitch.isEnabled = false
        binding.styleIconEnabledSwitch.isEnabled = false
        binding.shiftPixelAmountSlider.isEnabled = false
        binding.drawcompcirclesEnabledSwitch.isEnabled = false
        binding.compaodEnabledSwitch.isEnabled = false
        binding.minutedialaodEnabledSwitch.isEnabled = false
        currentLayoutId = ""
        currentColorId = ""

        // Set max and min.
        binding.shiftPixelAmountSlider.max = SHIFT_PIXEL_AOD_MAXIMUM_FOR_SLIDER.toInt()
        binding.shiftPixelAmountSlider.min = SHIFT_PIXEL_AOD_MINIMUM_FOR_SLIDER.toInt()
        binding.shiftPixelAmountSlider.progress = SHIFT_PIXEL_AOD_DEFAULT_FOR_SLIDER.toInt()
        binding.shiftPixelAmountSlider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                stateHolder.setShiftPixelAmount(binding.shiftPixelAmountSlider.progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Not needed
            }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Not needed just need the final result
            }
        })

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

        binding.drawdateEnabledSwitch.isChecked = userStylesAndPreview.drawDateEnabled
        binding.timeaodEnabledSwitch.isChecked = userStylesAndPreview.timeaodEnabled
        binding.styleIconEnabledSwitch.isChecked = userStylesAndPreview.styleIconEnabled
        binding.compaodEnabledSwitch.isChecked = userStylesAndPreview.compaodEnabled
        binding.drawcompcirclesEnabledSwitch.isChecked = userStylesAndPreview.drawCompCirclesEnabled
        binding.minutedialaodEnabledSwitch.isChecked = userStylesAndPreview.minutedialaodEnabled
        binding.activeAsAmbientEnabledSwitch.isChecked = userStylesAndPreview.activeAsAmbientEnabled
        binding.shiftPixelAmountSlider.progress = userStylesAndPreview.shiftpixelamount.toInt()
        currentLayoutId = userStylesAndPreview.layoutStyleId
        currentColorId = userStylesAndPreview.colorStyleId
        binding.preview.watchFaceBackground.setImageBitmap(userStylesAndPreview.previewImage)
        setColorObject(userStylesAndPreview.colorStyleId)
        setLayoutObject(userStylesAndPreview.layoutStyleId)
        enabledWidgets()
    }

    private fun enabledWidgets() {
        binding.colorStylePickerButton.isEnabled = true
        binding.drawdateEnabledSwitch.isEnabled = true
        binding.timeaodEnabledSwitch.isEnabled = true
        binding.styleIconEnabledSwitch.isEnabled = true
        binding.activeAsAmbientEnabledSwitch.isEnabled = true
        binding.drawcompcirclesEnabledSwitch.isEnabled = true
        binding.shiftPixelAmountSlider.isEnabled = true
        binding.compaodEnabledSwitch.isEnabled = true
        binding.minutedialaodEnabledSwitch.isEnabled = true
    }

    private fun setColorObject(id: String)
    {
        val colorStyleIdAndResourceIdsList = ColorStylesDynamic.instance
        for ( i in 0 until colorStyleIdAndResourceIdsList.size) {
            val r = colorStyleIdAndResourceIdsList[i]
            if ( id == r.id ) {
                binding.currentColorStyleIcon.setImageBitmap(
                    ColorStylesDynamic.getBitmap(baseContext, colorStyleIdAndResourceIdsList[(i)%colorStyleIdAndResourceIdsList.size].getName(baseContext),
                        colorStyleIdAndResourceIdsList[(i)%colorStyleIdAndResourceIdsList.size].getPrimaryColor(baseContext),
                        colorStyleIdAndResourceIdsList[(i)%colorStyleIdAndResourceIdsList.size].getSecondaryColor(baseContext))
                )
            }
        }
    }

    fun onClickColorStylePickerButton(view: View) {
        Log.d(TAG, "onClickColorStylePickerButton() $view")

        val colorStyleIdAndResourceIdsList = ColorStylesDynamic.instance
        var newColorStyle = ""
        for ( i in colorStyleIdAndResourceIdsList.indices) {
            val r = colorStyleIdAndResourceIdsList[i]
            if ( currentColorId == r.id ) {
                newColorStyle = colorStyleIdAndResourceIdsList[(i+1)%colorStyleIdAndResourceIdsList.size].id
                 binding.currentColorStyleIcon.setImageBitmap(
                     ColorStylesDynamic.getBitmap(view.context,colorStyleIdAndResourceIdsList[(i+1)%colorStyleIdAndResourceIdsList.size].getName(baseContext),
                        colorStyleIdAndResourceIdsList[(i+1)%colorStyleIdAndResourceIdsList.size].getPrimaryColor(baseContext),
                        colorStyleIdAndResourceIdsList[(i+1)%colorStyleIdAndResourceIdsList.size].getSecondaryColor(baseContext))
                 )
            }
        }
        stateHolder.setColorStyle(newColorStyle)
    }

    private fun setLayoutObject(id: String)
    {
        val layoutStyleIdAndResourceIdsList = enumValues<LayoutStyleIdAndResourceIds>()
        for ( i in 0 until layoutStyleIdAndResourceIdsList.size) {
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
        for ( i in 0 until layoutStyleIdAndResourceIdsList.size) {
            val r = layoutStyleIdAndResourceIdsList[i]
            if ( currentLayoutId == r.id ) {
                newLayoutStyle = layoutStyleIdAndResourceIdsList[(i+1)%layoutStyleIdAndResourceIdsList.size].id
                binding.currentLayoutStyleIcon.setImageResource(layoutStyleIdAndResourceIdsList[(i+1)%layoutStyleIdAndResourceIdsList.size].iconResourceId)
            }
        }

        stateHolder.setLayoutStyle(newLayoutStyle)

    }

    fun onClickStyleIconEnabledSwitch(view: View) {
        Log.d(TAG, "onClickStyleIconEnabledSwitch() $view")
        stateHolder.setStyleIcon(binding.styleIconEnabledSwitch.isChecked)
    }

    fun onClickActiveAsAmbientEnabledSwitch(view: View) {
        Log.d(TAG, "onClickActiveAsAmbientEnabledSwitch() $view")
        stateHolder.setActiveAsAmbient(binding.activeAsAmbientEnabledSwitch.isChecked)
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

    fun onClickDrawDateEnabledSwitch(view: View) {
        Log.d(TAG, "onClickDrawDateEnabledSwitch() $view")
        stateHolder.setDrawDate(binding.drawdateEnabledSwitch.isChecked)
    }

    fun onClickTimeAODEnabledSwitch(view: View) {
        Log.d(TAG, "onClickTimeAODEnabledSwitch() $view")
        stateHolder.setTimeAOD(binding.timeaodEnabledSwitch.isChecked)
    }

    fun onClickDrawCompCirclesEnabledSwitch(view: View) {
        Log.d(TAG, "onClickDrawCompCirclesEnabledSwitch() $view")
        stateHolder.setDrawCompCircles(binding.drawcompcirclesEnabledSwitch.isChecked)
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
