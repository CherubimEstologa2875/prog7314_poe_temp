package com.example.runway.ui.sheets

import com.example.runway.R

/** Wardrobe sort-order sheet. Still a placeholder - see [StubBottomSheet]. */
class SortSheet : StubBottomSheet() {
    override val titleRes = R.string.rw_sheet_sort_title
    override val bodyRes = R.string.rw_sheet_sort_body

    companion object {
        const val TAG = "SortSheet"
    }
}
