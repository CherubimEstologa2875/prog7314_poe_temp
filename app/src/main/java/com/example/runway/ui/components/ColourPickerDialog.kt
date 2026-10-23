package com.example.runway.ui.components

import android.content.Context
import androidx.core.graphics.ColorUtils
import com.example.runway.R
import com.example.runway.databinding.DialogColourPickerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Mixes a colour and names it, so it can be added to the palette.
object ColourPickerDialog {

    fun show(context: Context, onPicked: (label: String, argb: Int) -> Unit) {
        val binding = DialogColourPickerBinding.inflate(android.view.LayoutInflater.from(context))

        fun currentColour(): Int = ColorUtils.HSLToColor(
            floatArrayOf(
                binding.pickerHue.value,
                binding.pickerSaturation.value / 100f,
                binding.pickerLightness.value / 100f,
            )
        )

        fun refresh() {
            binding.pickerPreview.swatchColor = currentColour()
        }
        refresh()

        // Sliders step in whole numbers, so a value off the step can never be set.
        listOf(binding.pickerHue, binding.pickerSaturation, binding.pickerLightness).forEach {
            it.addOnChangeListener { _, _, _ -> refresh() }
        }

        val dialog = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Runway_Dialog)
            .setTitle(R.string.rw_colour_picker_title)
            .setView(binding.root)
            .setNegativeButton(R.string.rw_action_cancel) { d, _ -> d.dismiss() }
            .setPositiveButton(R.string.rw_colour_picker_save, null)
            .create()

        dialog.setOnShowListener {
            // Set after showing, otherwise the dialog closes even when the name is empty.
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val label = binding.pickerName.text?.toString()?.trim().orEmpty()
                if (label.isEmpty()) {
                    binding.pickerNameLayout.error = context.getString(R.string.rw_colour_picker_name_required)
                    return@setOnClickListener
                }
                onPicked(label, currentColour())
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
