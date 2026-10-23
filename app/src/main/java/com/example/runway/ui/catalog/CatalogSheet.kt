package com.example.runway.ui.catalog

import android.view.View
import com.example.runway.R
import com.example.runway.ui.components.RunwayBottomSheet
import com.example.runway.ui.components.RunwayToast

/** Example [RunwayBottomSheet] shown from the catalog. */
class CatalogSheet : RunwayBottomSheet(R.layout.sheet_catalog) {

    override val sheetTitle: CharSequence get() = getString(R.string.rw_catalog_sheet_title)

    override val footerLayoutRes: Int = R.layout.sheet_catalog_footer

    override fun onFooterCreated(footer: View) {
        footer.findViewById<View>(R.id.sheetApply).setOnClickListener {
            RunwayToast.show(footer, R.string.rw_action_save)
            dismiss()
        }
    }

    companion object {
        const val TAG = "CatalogSheet"
    }
}
