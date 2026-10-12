package dev.boop.android.data.local

import android.content.Context
import androidx.core.content.edit
import dev.boop.android.data.model.Device

/**
 * Stores the paired server URL and device credential in the app's private
 * SharedPreferences, plus small UI preferences.
 *
 * Note: plain storage, no at-rest encryption yet — hardening with the Android
 * Keystore is a follow-up.
 */
class SessionStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val serverUrl: String? get() = prefs.getString(KEY_SERVER, null)
    val credential: String? get() = prefs.getString(KEY_CREDENTIAL, null)
    val deviceName: String? get() = prefs.getString(KEY_DEVICE_NAME, null)

    /** Last FCM token sent to the server, to skip redundant registrations. */
    var pushToken: String?
        get() = prefs.getString(KEY_PUSH_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_PUSH_TOKEN, value) }

    val isPaired: Boolean
        get() = !serverUrl.isNullOrBlank() && !credential.isNullOrBlank()

    /** Inbox "Group repeats" toggle, remembered across launches (default on). */
    var inboxGrouped: Boolean
        get() = prefs.getBoolean(KEY_GROUPED, true)
        set(value) = prefs.edit { putBoolean(KEY_GROUPED, value) }

    fun save(server: String, credential: String, device: Device) {
        prefs.edit {
            putString(KEY_SERVER, server)
            putString(KEY_CREDENTIAL, credential)
            putString(KEY_DEVICE_ID, device.id)
            putString(KEY_DEVICE_NAME, device.name)
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private companion object {
        const val PREFS_NAME = "boop_session"
        const val KEY_SERVER = "server_url"
        const val KEY_CREDENTIAL = "credential"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_DEVICE_NAME = "device_name"
        const val KEY_GROUPED = "inbox_grouped"
        const val KEY_PUSH_TOKEN = "push_token"
    }
}
