package com.neubofy.lcld.commands

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.neubofy.lcld.R
import com.neubofy.lcld.permissions.WriteSecureSettingsPermission
import com.neubofy.lcld.transports.Transport

class FlightModeCommand(context: Context) : Command(context) {
    override val keyword = "flight"
    override val usage = "flight [on|off]"
    override val icon = R.drawable.ic_volume_up
    override val shortDescription = R.string.cmd_flight_description_short

    override val requiredPermissions = listOf(WriteSecureSettingsPermission())

    override suspend fun <T> executeInternal(args: List<String>, transport: Transport<T>) {
        if (args.isEmpty()) {
            transport.send(context, "Usage: flight [on|off]")
            return
        }

        val enable = args[0].equals("on", ignoreCase = true)
        val value = if (enable) 1 else 0

        try {
            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                value
            )
            val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            intent.putExtra("state", enable)
            context.sendBroadcast(intent)

            val status = if (enable) "ON" else "OFF"
            transport.send(context, "Flight mode set to $status")
        } catch (e: Exception) {
            transport.send(context, "Failed to change flight mode: ${e.message}")
        }
    }
}
