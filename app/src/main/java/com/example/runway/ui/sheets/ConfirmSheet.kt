package com.example.runway.ui.sheets

import com.example.runway.R

/** Generic confirm/cancel sheet, opened ad hoc from several screens. Still a placeholder -
 * see [StubBottomSheet]. */
class ConfirmSheet : StubBottomSheet() {
    override val titleRes = R.string.rw_sheet_confirm_title
    override val bodyRes = R.string.rw_sheet_confirm_body

    companion object {
        const val TAG = "ConfirmSheet"
    }
}
