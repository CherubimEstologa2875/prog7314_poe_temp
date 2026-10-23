package com.example.runway.ui.sheets

import com.example.runway.R

/** Wardrobe filter sheet. Still a placeholder - see [StubBottomSheet]. */
class FilterSheet : StubBottomSheet() {
    override val titleRes = R.string.rw_sheet_filter_title
    override val bodyRes = R.string.rw_sheet_filter_body

    companion object {
        const val TAG = "FilterSheet"
    }
}
