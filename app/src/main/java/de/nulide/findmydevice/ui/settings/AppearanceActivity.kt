package de.nulide.findmydevice.ui.settings

import android.app.LocaleConfig
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.processphoenix.ProcessPhoenix
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.databinding.ActivityAppearanceBinding
import de.nulide.findmydevice.ui.FmdActivity
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import de.nulide.findmydevice.utils.APP_LANGUAGES
import java.util.Locale

class AppearanceActivity : FmdActivity() {

    private lateinit var viewBinding: ActivityAppearanceBinding
    private lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = SettingsRepository.getInstance(this)

        viewBinding = ActivityAppearanceBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView))
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setupLanguageAndroid13AndAfter()
        } else {
            setupLanguageAndroid12AndBefore()
        }

        setupTheme()
        setupDynamicColors()
    }

    fun setupLanguageAndroid12AndBefore() {
        // XXX: This returns all locales, even if there is no translations for them
        // val availableLocales = Locale.getAvailableLocales()
        // Thus use a manually maintained list.
        val availableLocales = APP_LANGUAGES.map { Locale.forLanguageTag(it) }
        setupLanguagePicker(availableLocales.toMutableList())
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setupLanguageAndroid13AndAfter() {
        val supportedLocales = LocaleConfig(this).supportedLocales!!
        val locales = mutableListOf<Locale>()
        for (i in 0..supportedLocales.size() - 1) {
            locales.add(supportedLocales.get(i))
        }
        setupLanguagePicker(locales)
    }

    // Note the offsets for "System default"
    fun setupLanguagePicker(locales: MutableList<Locale>) {
        locales.sortBy { it.displayName }

        val currentLocale = AppCompatDelegate.getApplicationLocales()

        viewBinding.textViewLanguage.text =
            currentLocale.get(0)?.displayLanguage?.capitalize(Locale.getDefault())
                ?: getString(R.string.appearance_language_system_default)

        var checkedIdx = locales.indexOfFirst {
            it.toLanguageTag() == currentLocale.toLanguageTags()
        }
        checkedIdx += 1

        val names = locales.map { it.displayName.capitalize(Locale.getDefault()) }.toMutableList()
        names.add(0, getString(R.string.appearance_language_system_default))

        viewBinding.buttonEditLanguage.setOnClickListener { _ ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.appearance_language_choose)
                .setSingleChoiceItems(names.toTypedArray(), checkedIdx) { _, idx ->
                    val newLocale = if (idx == 0) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        val tag = locales[idx - 1].toLanguageTag()
                        LocaleListCompat.forLanguageTags(tag)
                    }
                    AppCompatDelegate.setApplicationLocales(newLocale)
                }
                .show()
        }
    }

    fun setupTheme() {
        val current = settings.get(Settings.SET_THEME) as String
        val resId = if (current == Settings.VAL_THEME_LIGHT) {
            R.string.appearance_theme_light
        } else if (current == Settings.VAL_THEME_DARK) {
            R.string.appearance_theme_dark
        } else {
            R.string.appearance_theme_follow_system
        }
        viewBinding.textViewTheme.text = getString(resId)

        setupThemePicker(current)
    }

    fun setupThemePicker(current: String) {
        val options = arrayOf(
            Settings.VAL_THEME_FOLLOW_SYSTEM,
            Settings.VAL_THEME_LIGHT,
            Settings.VAL_THEME_DARK
        )
        val optionsStrings = arrayOf(
            getString(R.string.appearance_theme_follow_system),
            getString(R.string.appearance_theme_light),
            getString(R.string.appearance_theme_dark),
        )
        val checkedIdx = options.indexOfFirst { it == current }

        viewBinding.buttonEditTheme.setOnClickListener { _ ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.appearance_theme_choose)
                .setSingleChoiceItems(optionsStrings, checkedIdx) { _, idx ->
                    if (idx != checkedIdx) {
                        val new = options[idx]
                        settings.set(Settings.SET_THEME, new)
                        recreate()
                    }
                }
                .show()
        }
    }

    fun setupDynamicColors() {
        // Dynamic Colors were introduced in Android 12 (SDK 31)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            viewBinding.switchDynamicColors.visibility = View.GONE
        }

        val isChecked = settings.get(Settings.SET_DYNAMIC_COLORS) as Boolean

        viewBinding.switchDynamicColors.isChecked = isChecked
        viewBinding.switchDynamicColors.setOnCheckedChangeListener { _, newChecked ->
            if (newChecked != isChecked) {
                settings.set(Settings.SET_DYNAMIC_COLORS, newChecked)
                // Recreate the application in order for parent activities to pick up the change.
                // XXX: This could also be solved by manually recreating the MainActivity, but this is easier for now.
                ProcessPhoenix.triggerRebirth(this)
            }
        }
    }
}