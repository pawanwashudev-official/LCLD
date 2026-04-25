package com.neubofy.lcld.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.services.ServerConnectivityCheckService
import com.neubofy.lcld.services.ServerVersionCheckService
import com.neubofy.lcld.services.TempContactExpiredService
import com.neubofy.lcld.utils.log


class AppUpdatedReceiver : BroadcastReceiver() {

    companion object {
        private val TAG: String = AppUpdatedReceiver::class.java.simpleName

        const val APP_UPDATED: String = "android.intent.action.MY_PACKAGE_REPLACED"
    }

    // Keep the AppUpdatedReceiver so that the app launches once after app updates.
    // However, the FmdApplication should start before this receiver runs, and it will start the main services.
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == APP_UPDATED) {
            context.log().i(TAG, "Running MY_PACKAGE_REPLACED (APP_UPDATED) handler")

            // One-shot services that don't need to run on every FmdApplication start
            TempContactExpiredService.scheduleJob(context, 0)

            val settings = SettingsRepository.getInstance(context)
            if (settings.serverAccountExists()) {
                ServerVersionCheckService.scheduleJobNow(context)
                ServerConnectivityCheckService.notifyAboutConnectivityCheck(context)
            }
        }
    }
}
