package de.nulide.findmydevice.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.services.ServerConnectivityCheckService
import de.nulide.findmydevice.services.ServerVersionCheckService
import de.nulide.findmydevice.services.TempContactExpiredService
import de.nulide.findmydevice.utils.log


class BootReceiver : BroadcastReceiver() {

    companion object {
        private val TAG: String = BootReceiver::class.java.simpleName

        const val BOOT_COMPLETED: String = "android.intent.action.BOOT_COMPLETED"
    }

    // Keep the BootReceiver so that the app launches once after boot.
    // However, the FmdApplication should start before this receiver runs, and it will start the main services.
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BOOT_COMPLETED) {
            context.log().i(TAG, "Running BOOT_COMPLETED handler")

            // One-shot services that don't need to run on every FmdApplication start
            TempContactExpiredService.scheduleJob(context, 0)

            val settings = SettingsRepository.getInstance(context)
            if (settings.serverAccountExists()) {
                ServerVersionCheckService.scheduleJobNow(context)

                // Don't notify about this on every boot. It is too intrusive/annoying if you don't want it.
                // ServerConnectivityCheckService.notifyAboutConnectivityCheck(context)
            }
        }
    }
}
