package com.example.runway.ui.components

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.example.runway.databinding.ViewBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// Base class for the app's bottom sheets. Subclasses supply the content layout and the title.
abstract class RunwayBottomSheet(
    @LayoutRes private val contentLayoutRes: Int,
) : BottomSheetDialogFragment() {

    private var _binding: ViewBottomSheetBinding? = null
    private val binding get() = requireNotNull(_binding) { "Accessed binding outside the view lifecycle" }

    /** Heading shown at the top of the sheet. */
    abstract val sheetTitle: CharSequence

    // Optional layout pinned under the content, like an Apply button. Null hides the footer.
    @get:LayoutRes
    protected open val footerLayoutRes: Int? = null

    /** Called once the content layout has been inflated into the sheet. */
    protected open fun onContentCreated(content: View) = Unit

    /** Called once the footer layout has been inflated, when there is one. */
    protected open fun onFooterCreated(footer: View) = Unit

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(TAG, "${javaClass.simpleName}: onCreateView")
        _binding = ViewBottomSheetBinding.inflate(inflater, container, false)

        binding.sheetTitle.text = sheetTitle
        binding.sheetClose.setOnClickListener { dismiss() }

        val content = inflater.inflate(contentLayoutRes, binding.sheetContent, false)
        binding.sheetContent.addView(content)
        onContentCreated(content)

        footerLayoutRes?.let { layoutRes ->
            val footer = inflater.inflate(layoutRes, binding.sheetFooter, false)
            binding.sheetFooter.addView(footer)
            binding.sheetFooter.isVisible = true
            binding.sheetFooterDivider.isVisible = true
            onFooterCreated(footer)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "${javaClass.simpleName}: onDestroyView")
        // A fragment's view outlives neither the binding nor the other way round;
        _binding = null
    }

    private companion object {
        const val TAG = "RunwayBottomSheet"
    }
}
