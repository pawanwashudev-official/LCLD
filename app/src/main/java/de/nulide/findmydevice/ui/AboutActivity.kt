package de.nulide.findmydevice.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import de.nulide.findmydevice.R
import de.nulide.findmydevice.databinding.ActivityAboutBinding
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeAppBar

class AboutActivity : FmdActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdgeAppBar(binding.appBar)

        binding.aboutSourceLink.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_source_url))))
        }
        binding.aboutEmailLink.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.about_dev_email))))
        }
    }
}
