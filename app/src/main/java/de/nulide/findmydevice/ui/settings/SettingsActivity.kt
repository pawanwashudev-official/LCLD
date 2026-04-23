package de.nulide.findmydevice.ui.settings

import android.os.Bundle
import de.nulide.findmydevice.R
import de.nulide.findmydevice.databinding.ActivitySettingsBinding
import de.nulide.findmydevice.ui.FmdActivity
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeAppBar

class SettingsActivity : FmdActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdgeAppBar(binding.appBar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
}
