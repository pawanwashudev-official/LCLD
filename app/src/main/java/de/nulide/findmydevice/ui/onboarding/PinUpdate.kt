package de.nulide.findmydevice.ui.onboarding

import android.content.Context
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.ui.settings.FMDConfigActivity
import de.nulide.findmydevice.utils.Notifications

class PinUpdate {

    companion object {
        fun migratePin(context: Context) {
            val settings = SettingsRepository.getInstance(context)
            val oldPinHash = settings.get(Settings.SET_PIN) as String

            if (oldPinHash.isNotBlank()) {
                val title = context.getString(R.string.notify_crypto_update_title)
                val text = context.getString(R.string.update_pin_hash_change_text)
                Notifications.notify(
                    context,
                    title,
                    text,
                    Notifications.CHANNEL_SECURITY,
                    cls = FMDConfigActivity::class.java,
                )
            }
            settings.remove(Settings.SET_PIN)
        }
    }
}
