package de.nulide.findmydevice.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.EncryptedSettingsRepository
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.services.TheftModeService

class TheftModeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_theft_mode)

        val settings = SettingsRepository.getInstance(this)
        val ownerPhone = settings.get(Settings.SET_OWNER_PHONE) as String
        val ownerEmail = settings.get(Settings.SET_OWNER_EMAIL) as String

        val ownerInfoText = findViewById<TextView>(R.id.textViewOwnerInfo)
        var infoText = "Owner Info:\n"
        if (ownerPhone.isNotEmpty()) infoText += "Phone: $ownerPhone\n"
        if (ownerEmail.isNotEmpty()) infoText += "Email: $ownerEmail"
        ownerInfoText.text = infoText

        val pinInput = findViewById<EditText>(R.id.editTextPin)
        val stopButton = findViewById<Button>(R.id.buttonStop)

        stopButton.setOnClickListener {
            val enteredPin = pinInput.text.toString()
            val encSettings = EncryptedSettingsRepository.getInstance(this)
            val correctPin = encSettings.getFmdPin()

            if (correctPin != null && enteredPin == correctPin) {
                // PIN matches, stop theft mode
                settings.set(Settings.SET_THEFT_MODE_ACTIVE, false)
                val serviceIntent = Intent(this, TheftModeService::class.java)
                stopService(serviceIntent)
                finish()
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Prevent back button from closing it
    override fun onBackPressed() {
        // Do nothing
    }
}
