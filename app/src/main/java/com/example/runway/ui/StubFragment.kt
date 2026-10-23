package com.example.runway.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.runway.databinding.FragmentStubBinding
import com.example.runway.ui.components.EmptyStateView

// Placeholder for a screen that has not been built yet.
abstract class StubFragment : Fragment() {

    /** Title shown in the header and the empty state. */
    @get:StringRes
    protected abstract val titleRes: Int

    /** Short description of what will live on this screen. */
    @get:StringRes
    protected abstract val bodyRes: Int

    /** Off for a screen with nothing behind it, like the first screen of the app. */
    protected open val showBack: Boolean = true

    private var _binding: FragmentStubBinding? = null
    private val binding get() = requireNotNull(_binding) { "Accessed binding outside the view lifecycle" }

    /**
     * The empty-state view showing the title/body, exposed so a subclass that needs to
     * demonstrate its one forward navigation (e.g. a "Continue" button) can add it here.
     * Only valid between onViewCreated and onDestroyView.
     */
    protected val emptyState: EmptyStateView get() = binding.stubEmpty

    // Named logTag, not tag: Fragment already has a tag property.
    private val logTag: String get() = javaClass.simpleName

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(logTag, "onCreateView")
        _binding = FragmentStubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(logTag, "onViewCreated")

        val title = getString(titleRes)
        binding.stubHeader.title = title
        binding.stubEmpty.title = title
        binding.stubEmpty.body = getString(bodyRes)

        binding.stubHeader.onBackClick {
            Log.d(logTag, "back from a placeholder screen")
            findNavController().navigateUp()
        }
        // onBackClick turns the arrow on, so hiding it has to come after.
        binding.stubHeader.setShowBack(showBack)
    }

    override fun onResume() {
        super.onResume()
        Log.d(logTag, "onResume - this tab is now visible")
    }

    override fun onPause() {
        super.onPause()
        Log.d(logTag, "onPause")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding here or the view hierarchy leaks.
        Log.d(logTag, "onDestroyView - releasing the binding")
        _binding = null
    }
}
