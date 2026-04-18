package de.nulide.findmydevice.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.lifecycle.lifecycleScope
import com.mikepenz.aboutlibraries.LibsBuilder
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.ui.TaggedFragment
import de.nulide.findmydevice.ui.helper.SettingsEntry
import de.nulide.findmydevice.ui.helper.SettingsViewAdapter
import de.nulide.findmydevice.utils.SettingsImportExporter
import de.nulide.findmydevice.utils.SettingsImportExporter.Companion.filenameForExport
import kotlinx.coroutines.launch

class SettingsFragment : TaggedFragment() {

    companion object {
        private const val EXPORT_REQ_CODE = 30
        private const val IMPORT_REQ_CODE = 40
    }

    private lateinit var settings: SettingsRepository

    override fun getStaticTag(): String = "SettingsFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository.Companion.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settingsEntries = SettingsEntry.getSettingsEntries(view.context)

        val listSettings = view.findViewById<ListView>(R.id.listSettings)
        listSettings.setAdapter(SettingsViewAdapter(view.context, settingsEntries))
        listSettings.onItemClickListener =
            OnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
                this.onItemClick(parent, view, position, id)
            }
    }

    fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val context = view.context

        var settingIntent: Intent? = null
        when (position) {
            0 -> settingIntent = Intent(context, FMDConfigActivity::class.java)
            1 -> settingIntent = if (!settings.serverAccountExists()) {
                Intent(context, AddAccountActivity::class.java)
            } else {
                Intent(context, FMDServerActivity::class.java)
            }

            2 -> settingIntent = Intent(context, AllowlistActivity::class.java)
            3 -> settingIntent = Intent(context, OpenCellIdActivity::class.java)
            4 -> settingIntent = Intent(context, AppearanceActivity::class.java)
            5 -> {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.putExtra(Intent.EXTRA_TITLE, filenameForExport())
                intent.setType("*/*")
                startActivityForResult(intent, EXPORT_REQ_CODE)
            }

            6 -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.setType("*/*")
                startActivityForResult(intent, IMPORT_REQ_CODE)
            }

            7 -> settingIntent = Intent(context, LogViewActivity::class.java)
            8 -> {
                val activityTitle = getString(R.string.Settings_About)
                settingIntent = LibsBuilder().withActivityTitle(activityTitle).withAboutDescription(getString(R.string.about_gratitude))
                    .withListener(AboutLibsListener.listener).intent(context)
            }
        }

        if (settingIntent != null) {
            startActivity(settingIntent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val context: Context? = activity
        if (context == null) {
            return
        }
        if (requestCode == IMPORT_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                if (uri != null) {
                    lifecycleScope.launch {
                        SettingsImportExporter(context).importData(uri)
                    }
                }
            }
        } else if (requestCode == EXPORT_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                if (uri != null) {
                    lifecycleScope.launch {
                        SettingsImportExporter(context).exportData(uri)
                    }
                }
            }
        }
    }
}
