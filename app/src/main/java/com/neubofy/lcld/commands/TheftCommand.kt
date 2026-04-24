package com.neubofy.lcld.commands

import android.content.Context
import android.content.Intent
import com.neubofy.lcld.R
import com.neubofy.lcld.data.Settings
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.permissions.LocationPermission
import com.neubofy.lcld.services.TheftService
import com.neubofy.lcld.transports.Transport

class TheftCommand(context: Context) : Command(context) {

    override val keyword = "theft"
    override val usage = "theft [recovery_pin]"
    override val icon = R.drawable.ic_security
    override val shortDescription = R.string.command_theft_description
    override val requiredPermissions = listOf(LocationPermission())

    override internal suspend fun <T> executeInternal(args: List<String>, transport: Transport<T>) {
        val context = context
        val settings = SettingsRepository.getInstance(context)
        
        // Use PIN from command if provided, else use default app PIN
        val pin = if (args.isNotEmpty()) args[0] else (settings.get(Settings.SET_PIN) as String)
        
        settings.set(Settings.SET_THEFT_MODE_PIN, pin)
        settings.set(Settings.SET_THEFT_MODE_ACTIVE, true)

        // Trigger Location Update first
        val locateCommand = LocateCommand(context)
        locateCommand.execute(listOf("gps"), transport)

        // Enable Bluetooth (requested)
        val bluetoothCommand = BluetoothCommand(context)
        bluetoothCommand.execute(listOf("on"), transport)

        // Disable DND (requested)
        val dndCommand = NoDisturbCommand(context)
        dndCommand.execute(listOf("off"), transport)

        // Disable Flight Mode (requested)
        val flightModeCommand = FlightModeCommand(context)
        flightModeCommand.execute(listOf("off"), transport)

        // Start Background Service for looping Ring/Flash/Vibrate
        val theftIntent = Intent(context, TheftService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(theftIntent)
        } else {
            context.startService(theftIntent)
        }
    }
}
