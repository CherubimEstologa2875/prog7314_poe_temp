package com.example.runway.ui.profile

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.data.notifications.NotificationChannels
import com.example.runway.data.notifications.NotificationPermission
import com.example.runway.data.remote.api.SendReportDto
import com.example.runway.data.remote.api.toUserMessage
import com.example.runway.databinding.FragmentNotificationsBinding
import com.example.runway.databinding.ItemNotificationBinding
import com.example.runway.domain.model.InboxNotification
import com.example.runway.domain.model.NotificationRules
import com.example.runway.domain.model.NotificationType
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.ui.components.ListRowView
import com.example.runway.ui.components.RunwayDialogs
import com.example.runway.ui.components.RunwayToast
import kotlinx.coroutines.launch

/**
 * Wash reminders and swap-request alerts (SCRUM-162). Shows whether Android will display
 * Runway's notifications and how to fix it if not, which kinds are switched on, a button
 * that sends a real push through the API, and the alerts received on this phone.
 */
class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val container get() = (requireActivity().application as RunwayApplication).container

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "permission granted=$granted")
            if (_binding == null) return@registerForActivityResult
            renderStatus()
            if (!granted) RunwayToast.show(binding.root, R.string.rw_notifications_denied, R.drawable.ic_rw_alert)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.notificationsHeader.onBackClick { findNavController().navigateUp() }
        binding.notificationsTypesHeader.onActionClick { findNavController().navigate(R.id.settingsFragment) }
        binding.notificationsRecentHeader.onActionClick { confirmClear() }
        binding.notificationsTestButton.setOnClickListener { sendTest() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { container.settingsRepository.observeSettings().collect(::renderTypes) }
                launch { container.notificationInbox.notifications.collect(::renderInbox) }
            }
        }
    }

    // The user may have changed things in Android's settings while away, so check on every return.
    override fun onResume() {
        super.onResume()
        renderStatus()
        renderTypes(container.settingsRepository.currentSettings())
    }

    private fun renderStatus() {
        val allowed = NotificationPermission.isAllowed(requireContext())
        binding.notificationsStatusTitle.setText(
            if (allowed) R.string.rw_notifications_on_title else R.string.rw_notifications_off_title
        )
        binding.notificationsStatusTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (allowed) R.drawable.ic_rw_check else R.drawable.ic_rw_alert, 0, 0, 0,
        )
        binding.notificationsStatusBody.setText(
            if (allowed) R.string.rw_notifications_on_body else R.string.rw_notifications_off_body
        )

        val action = binding.notificationsStatusAction
        action.isVisible = !allowed
        if (allowed) return

        if (canAskInApp()) {
            action.setText(R.string.rw_notifications_allow)
            action.setOnClickListener {
                NotificationPermission.markAsked(requireContext())
                permissionRequest.launch(NotificationPermission.PERMISSION)
            }
        } else {
            // Android stops showing the prompt after two refusals, and older versions never had one.
            action.setText(R.string.rw_notifications_open_settings)
            action.setOnClickListener { openSystemSettings() }
        }
    }

    // True while Android would still show its permission dialog.
    private fun canAskInApp(): Boolean {
        if (!NotificationPermission.needsRuntimeRequest) return false
        val context = requireContext()
        return !NotificationPermission.hasAskedBefore(context) ||
            shouldShowRequestPermissionRationale(NotificationPermission.PERMISSION)
    }

    private fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        startActivity(intent)
    }

    private fun renderTypes(settings: RunwaySettings) {
        if (_binding == null) return
        renderType(binding.notificationsWashRow, NotificationType.WASH, settings)
        renderType(binding.notificationsSwapRow, NotificationType.SWAP, settings)
        renderType(binding.notificationsWeatherRow, NotificationType.WEATHER, settings)
    }

    private fun renderType(row: ListRowView, type: NotificationType, settings: RunwaySettings) {
        row.value = getString(
            when {
                !NotificationRules.isEnabled(type, settings) -> R.string.rw_notifications_type_off
                !NotificationChannels.isChannelOn(requireContext(), type) -> R.string.rw_notifications_type_muted
                else -> R.string.rw_notifications_type_on
            }
        )
    }

    private fun renderInbox(entries: List<InboxNotification>) {
        val list = binding.notificationsList
        list.removeAllViews()
        binding.notificationsListCard.isVisible = entries.isNotEmpty()
        binding.notificationsEmpty.isVisible = entries.isEmpty()
        binding.notificationsRecentHeader.actionText =
            if (entries.isEmpty()) null else getString(R.string.rw_notifications_clear)

        val now = System.currentTimeMillis()
        entries.forEachIndexed { index, entry ->
            val row = ItemNotificationBinding.inflate(layoutInflater, list, false)
            row.notificationIcon.setImageResource(iconFor(entry.type))
            row.notificationTitle.text = entry.title
            row.notificationBody.text = entry.body
            row.notificationBody.isVisible = entry.body.isNotBlank()
            row.notificationTime.text = DateUtils.getRelativeTimeSpanString(
                entry.receivedAtMillis, now, DateUtils.MINUTE_IN_MILLIS,
            )
            row.notificationDivider.isVisible = index < entries.lastIndex
            list.addView(row.root)
        }
    }

    private fun iconFor(type: NotificationType) = when (type) {
        NotificationType.WASH -> R.drawable.ic_rw_droplet
        NotificationType.SWAP -> R.drawable.ic_rw_swap
        NotificationType.WEATHER -> R.drawable.ic_rw_cloud
        NotificationType.GENERAL -> R.drawable.ic_rw_bell
    }

    private fun confirmClear() {
        RunwayDialogs.confirm(
            context = requireContext(),
            titleRes = R.string.rw_notifications_clear_title,
            bodyRes = R.string.rw_notifications_clear_body,
            confirmLabelRes = R.string.rw_notifications_clear,
            destructive = true,
        ) {
            container.notificationInbox.clear()
        }
    }

    // Sends a real push through the API and FCM, so this checks the whole path, not just the screen.
    private fun sendTest() {
        val button = binding.notificationsTestButton
        button.isEnabled = false
        button.setText(R.string.rw_notifications_test_sending)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = container.pushTokenRepository.sendTest()
            val current = _binding ?: return@launch
            current.notificationsTestButton.isEnabled = true
            current.notificationsTestButton.setText(R.string.rw_notifications_test)

            result
                .onSuccess { report ->
                    Log.d(TAG, "test push: $report")
                    val (message, icon) = describe(report)
                    RunwayToast.show(current.root, message, icon)
                }
                .onFailure { error ->
                    Log.w(TAG, "test push failed: ${error.message}")
                    RunwayToast.show(current.root, error.toUserMessage(), R.drawable.ic_rw_alert)
                }
        }
    }

    private fun describe(report: SendReportDto): Pair<Int, Int> = when {
        report.sent > 0 -> R.string.rw_notifications_test_sent to R.drawable.ic_rw_check
        report.failed > 0 -> R.string.rw_notifications_test_failed to R.drawable.ic_rw_alert
        else -> R.string.rw_notifications_test_no_device to R.drawable.ic_rw_alert
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "NotificationsFragment"
    }
}

/* Reference List
Android Developers, 2026. Notification runtime permission. [online] Available at: <https://developer.android.com/develop/ui/views/notifications/notification-permission> [Accessed 29 October 2026].
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
