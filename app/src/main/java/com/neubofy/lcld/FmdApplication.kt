package com.neubofy.lcld

import android.app.Application
import android.content.Context
import android.service.notification.StatusBarNotification
import com.neubofy.lcld.data.Settings
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.data.UncaughtExceptionHandler.Companion.initUncaughtExceptionHandler
import com.neubofy.lcld.receiver.PushReceiver
import com.neubofy.lcld.services.FmdBatteryLowService
import com.neubofy.lcld.services.ServerConnectivityCheckService
import com.neubofy.lcld.services.ServerLocationUploadService
import com.neubofy.lcld.ui.onboarding.PinUpdate
import com.neubofy.lcld.ui.onboarding.UpdateboardingModernCryptoActivity
import com.neubofy.lcld.utils.Notifications
import com.neubofy.lcld.utils.log
import com.neubofy.lcld.warnings.notifyWarnUnifiedPushRequired
import com.neubofy.lcld.warnings.shouldWarnUnifiedPushRequired


class FmdApplication : Application() {

    companion object {
        private val TAG = FmdApplication::class.java.simpleName
    }

    // Workaround to "pass" this from the NotificationListenerService to the CommandExecutionWorker.
    // The problem is that we cannot pass objects between them directly.
    // But we also cannot retrieve the notification in the worker by ID,
    // because notificationManager.activeNotifications only returns the notifications posted by our own app.
    var latestStatusBarNotification: StatusBarNotification? = null

    override fun onCreate() {
        super.onCreate()

        this.log().i(TAG, "Starting FmdApplication")

        Notifications.init(this)
        initUncaughtExceptionHandler(this)

        doUpdateMigrations(this)

        restartServices()
    }

    private fun doUpdateMigrations(context: Context) {
        val settings = SettingsRepository.getInstance(context)
        settings.migrateSettings()
        UpdateboardingModernCryptoActivity.notifyAboutCryptoRefreshIfRequired(context)
        PinUpdate.migratePin(context)
    }

    fun restartServices() {
        val settings = SettingsRepository.getInstance(this)
        if (settings.serverAccountExists()) {
            // Scheduling a job that is already running should be fine (?),
            // because they have the same, fixed JOB_ID.
            if (settings.get(Settings.SET_FMD_LOW_BAT_SEND) as Boolean) {
                FmdBatteryLowService.scheduleJobNow(this)
            }
            ServerLocationUploadService.scheduleRecurring(this)
            ServerConnectivityCheckService.scheduleJob(this)

            if (shouldWarnUnifiedPushRequired(this)) {
                notifyWarnUnifiedPushRequired(this)
            }

            // Do NOT try to register with UnifiedPush.
            // This needs a UI context, and should thus happen in the MainActivity.
        } else {
            FmdBatteryLowService.cancelJob(this)
            ServerLocationUploadService.cancelJob(this)
            ServerConnectivityCheckService.cancelJob(this)

            PushReceiver.unregisterWithUnifiedPush(this)
        }
    }
}
