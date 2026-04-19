package de.nulide.findmydevice.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.services.TheftModeService
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.ui.TheftModeActivity
import de.nulide.findmydevice.utils.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProtectCommand(context: Context) : Command(context) {

    companion object {
        private val TAG = ProtectCommand::class.simpleName
    }

    override val keyword = "protect"
    override val usage = "protect"

    @get:DrawableRes
    override val icon = R.drawable.ic_warning // Using warning icon as default

    @get:StringRes
    override val shortDescription = de.nulide.findmydevice.R.string.cmd_flash_description_short // We will add these strings later

    override val longDescription = de.nulide.findmydevice.R.string.cmd_locate_description_long

    override val requiredPermissions = emptyList<de.nulide.findmydevice.permissions.Permission>()

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val settings = SettingsRepository.getInstance(context)

        withContext(Dispatchers.Main) {
            settings.set(Settings.SET_THEFT_MODE_ACTIVE, true)

            // Try to lock the device
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                if (dpm.isAdminActive(android.content.ComponentName(context, de.nulide.findmydevice.receiver.DeviceAdminReceiver::class.java))) {
                    dpm.lockNow()
                }
            } catch (e: Exception) {
                context.log().e(TAG, "Failed to lock device: ${e.message}")
            }

            // Start the TheftModeService (foreground)
            val serviceIntent = Intent(context, TheftModeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            // Launch the lockscreen activity trap
            val activityIntent = Intent(context, TheftModeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(activityIntent)

            transport.send(context, "Theft Mode has been activated on the device.")
        }
    }
}
