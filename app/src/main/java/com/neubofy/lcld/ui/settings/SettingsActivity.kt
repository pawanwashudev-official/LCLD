package com.neubofy.lcld.ui.settings

import android.os.Bundle
import com.neubofy.lcld.R
import com.neubofy.lcld.ui.FmdActivity

class SettingsActivity : FmdActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
}
