package com.neubofy.lcld.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.services.ServerConnectivityCheckService
import com.neubofy.lcld.services.ServerVersionCheckService
import com.neubofy.lcld.services.TempContactExpiredService
import com.neubofy.lcld.utils.log


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
            if (settings.get(com.neubofy.lcld.data.Settings.SET_THEFT_MODE_ACTIVE) == true) {
                // To keep it simple, we just re-execute the ring command via its Activity
                // since the background service was removed.
                try {
                    com.neubofy.lcld.ui.RingerActivity.newInstance(context, com.neubofy.lcld.commands.RING_DURATION_LONG_SECS)
                } catch (e: Exception) {
                    context.log().e(TAG, "Failed to start RingerActivity on boot: \${e.message}")
                }
            }

            if (settings.serverAccountExists()) {
                ServerVersionCheckService.scheduleJobNow(context)
                com.neubofy.lcld.services.ServerCommandDownloadService.scheduleRecurring(context)

                // Don't notify about this on every boot. It is too intrusive/annoying if you don't want it.
                // ServerConnectivityCheckService.notifyAboutConnectivityCheck(context)
            }
        }
    }
}
