package com.example.runway.ui.components

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.example.runway.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder


object RunwayDialogs {

    private const val TAG = "RunwayDialogs"

    fun confirm(
        context: Context,
        @StringRes titleRes: Int,
        @StringRes bodyRes: Int? = null,
        @StringRes confirmLabelRes: Int,
        @StringRes cancelLabelRes: Int = R.string.rw_action_cancel,
        destructive: Boolean = false,
        onCancel: () -> Unit = {},
        onConfirm: () -> Unit,
    ) {
        Log.d(TAG, "confirm: title=${context.getString(titleRes)} destructive=$destructive")

        val builder = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Runway_Dialog)
            .setTitle(titleRes)
            .setNegativeButton(cancelLabelRes) { dialog, _ ->
                Log.d(TAG, "confirm: cancelled")
                dialog.dismiss()
                onCancel()
            }
            // Also covers the back button and tapping outside the dialog.
            .setOnCancelListener { onCancel() }
            .setPositiveButton(confirmLabelRes) { dialog, _ ->
                Log.d(TAG, "confirm: confirmed")
                dialog.dismiss()
                onConfirm()
            }

        if (bodyRes != null) builder.setMessage(bodyRes)

        val dialog = builder.create()
        if (destructive) {
            // The buttons only exist once the dialog is shown, so the tint waits for onShow.
            dialog.setOnShowListener {
                dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)?.let { button ->
                    button.setTextColor(
                        MaterialColors.getColor(
                            button, androidx.appcompat.R.attr.colorError
                        )
                    )
                }
            }
        }
        dialog.show()
    }
}
