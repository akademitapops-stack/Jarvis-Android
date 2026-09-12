# JARVIS App Access

JARVIS v2.5+ uses Android Accessibility Service for cross-app UI automation. Android does not expose a general API that lets one app arbitrarily grant itself other apps' runtime permissions, so this feature is an explicit **per-app automation allowlist**, not a bypass of Android permissions.

Path: **Settings → App Access — daftar & izin per aplikasi**

- Every launchable app is listed.
- Each app is OFF by default.
- Turn on **Akses** only for apps JARVIS should operate.
- Accessibility Service must also be enabled in Android Settings.
- `tapText`, `typeText`, and `scroll` are blocked when the current foreground app is not allowlisted.
- Android system-level permissions (camera, contacts, calendar, notifications, etc.) remain controlled by Android's own permission system.
