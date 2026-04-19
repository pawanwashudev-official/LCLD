package de.nulide.findmydevice

import android.app.Application
import android.content.Context
import android.service.notification.StatusBarNotification
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.data.UncaughtExceptionHandler.Companion.initUncaughtExceptionHandler
import de.nulide.findmydevice.receiver.PushReceiver
import de.nulide.findmydevice.services.FmdBatteryLowService
import de.nulide.findmydevice.services.ServerConnectivityCheckService
import de.nulide.findmydevice.services.ServerLocationUploadService
import de.nulide.findmydevice.ui.onboarding.PinUpdate
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity
import de.nulide.findmydevice.utils.Notifications
import de.nulide.findmydevice.utils.log
import de.nulide.findmydevice.warnings.notifyWarnUnifiedPushRequired
import de.nulide.findmydevice.warnings.shouldWarnUnifiedPushRequired


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

            if (settings.get(Settings.SET_FMDSERVER_ENABLE_PUSH) as Boolean) { PushReceiver.unregisterWithUnifiedPush(this) }
        }
    }
}
