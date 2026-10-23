package com.example.runway.ui.sheets

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import com.example.runway.R
import com.example.runway.ui.components.RunwayBottomSheet

/**
 * Placeholder for a bottom sheet that has not been built yet. Each sheet subclasses
 * this and supplies its own title and body text.
 */
abstract class StubBottomSheet : RunwayBottomSheet(R.layout.sheet_stub) {

    @get:StringRes
    protected abstract val titleRes: Int

    @get:StringRes
    protected abstract val bodyRes: Int

    override val sheetTitle: CharSequence get() = getString(titleRes)

    override fun onContentCreated(content: View) {
        content.findViewById<TextView>(R.id.sheetStubBody).setText(bodyRes)
    }
}
