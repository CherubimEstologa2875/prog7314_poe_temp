# Notifications test plan (SCRUM-163)

Run on a phone, or an emulator with Google Play, against an API that has the notification endpoints deployed. Send tests with the **Send a test notification** button on the Notifications screen, or with `POST /api/v1/notifications/test` in Swagger. Save a screenshot of each result in `docs/screenshots/`.

| # | Case | How | Expected |
|---|---|---|---|
| 1 | App in foreground | App open, send a test | Notification shows, and appears in Recent |
| 2 | App in background | Press Home, send a test | Notification shows |
| 3 | App closed | `adb shell am force-stop com.example.runway`, then send from the API | Notification shows |
| 4 | Tap opens the screen | Tap any notification | Notifications screen, after the biometric check if it is on |
| 5 | Permission denied | `adb shell pm revoke com.example.runway android.permission.POST_NOTIFICATIONS` | No notification and no crash. The alert is still in Recent, and the screen says notifications are blocked |
| 6 | Type turned off | Turn wash reminders off, push `{"type":"wash"}` | Nothing shown. Other types still arrive |
| 7 | Channel muted | Mute "Swap requests" in Android settings | Swap row reads "Muted in Android" |
| 8 | Offline delivery | Airplane mode, send, reconnect | Arrives after reconnecting |
| 9 | Wash reminder | Log wears on an item until it reaches its limit | One "Time for a wash" alert, not repeated on later wears |
| 10 | Stale token | Uninstall, send a test from Swagger | Response has `removed: 1`, and the token is gone from `deviceTokens` |
| 11 | Sign-out | Sign out, send from Swagger as that user | `sent: 0` |
| 12 | Shared phone | Sign out, sign in as someone else, push to the first user | Nothing arrives |

| # | Result | Screenshot |
|---|---|---|
| 1 | | |
| 2 | | |
| 3 | | |
| 4 | | |
| 5 | | |
| 6 | | |
| 7 | | |
| 8 | | |
| 9 | | |
| 10 | | |
| 11 | | |
| 12 | | |
