package de.nulide.findmydevice.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.databinding.ActivityOpenCellIdBinding
import de.nulide.findmydevice.net.OpenCelliDRepository
import de.nulide.findmydevice.net.OpenCelliDSpec
import de.nulide.findmydevice.permissions.LocationPermission
import de.nulide.findmydevice.ui.FmdActivity
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import de.nulide.findmydevice.utils.CellParameters
import de.nulide.findmydevice.utils.Utils.Companion.getGeoURI
import de.nulide.findmydevice.utils.Utils.Companion.getOpenStreetMapLink
import de.nulide.findmydevice.utils.Utils.Companion.openUrl
import de.nulide.findmydevice.utils.Utils.Companion.pasteFromClipboard
import de.nulide.findmydevice.utils.log
import de.nulide.findmydevice.utils.requestCellInfo


class OpenCellIdActivity : FmdActivity(), TextWatcher {

    private lateinit var viewBinding: ActivityOpenCellIdBinding

    private lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityOpenCellIdBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView))

        settings = SettingsRepository.getInstance(this)
        val apiToken = settings.get(Settings.SET_OPENCELLID_API_KEY) as String

        viewBinding.editTextOpenCellIDAPIKey.setText(apiToken)
        viewBinding.editTextOpenCellIDAPIKey.addTextChangedListener(this)

        viewBinding.buttonPaste.setOnClickListener(::onPasteClicked)
        viewBinding.buttonOpenOpenCellIdWebsite.setOnClickListener(::onOpenWebsiteClicked)
        viewBinding.buttonDownloadTowerCollector.setOnClickListener(::onDownloadTowerCollectorClicked)
        viewBinding.buttonTestOpenCellId.setOnClickListener(::onTestConnectionClicked)

        setupTestConnection(apiToken.isEmpty())
    }

    private fun setupTestConnection(isApiTokenEmpty: Boolean) {
        if (isApiTokenEmpty) {
            viewBinding.buttonTestOpenCellId.isEnabled = false
            viewBinding.textViewTestOpenCellIdResponse.visibility = View.GONE
        } else {
            viewBinding.buttonTestOpenCellId.isEnabled = true
            viewBinding.textViewTestOpenCellIdResponse.text = ""
            viewBinding.textViewTestOpenCellIdResponse.visibility = View.VISIBLE
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(edited: Editable) {
        if (edited === viewBinding.editTextOpenCellIDAPIKey.text) {
            val newToken = edited.toString().trim()
            settings.set(Settings.SET_OPENCELLID_API_KEY, newToken)
            setupTestConnection(newToken.isEmpty())
        }
    }

    private fun onPasteClicked(view: View) {
        viewBinding.editTextOpenCellIDAPIKey.setText(pasteFromClipboard(view.context))
    }

    private fun onOpenWebsiteClicked(view: View) {
        openUrl(view.context, "https://opencellid.org/")
    }

    private fun onDownloadTowerCollectorClicked(view: View) {
        openUrl(view.context, "https://f-droid.org/packages/info.zamojski.soft.towercollector/")
    }

    private fun onTestConnectionClicked(view: View) {
        val context = view.context

        val permission = LocationPermission()
        if (!permission.isGranted(context)) {
            permission.request(this)
            return
        }

        @SuppressLint("MissingPermission") // ACCESS_FINE_LOCATION
        requestCellInfo(context, this::onCellInfoUpdate)
    }

    private fun onCellInfoUpdate(paras: List<CellParameters>) {
        val context = this
        if (paras.isEmpty()) {
            context.log().i(TAG, "No cell location found")
            viewBinding.textViewTestOpenCellIdResponse.text =
                context.getString(R.string.OpenCellId_test_no_connection)
            return
        }

        val repo = OpenCelliDRepository.getInstance(OpenCelliDSpec(context))
        val apiAccessToken = settings.get(Settings.SET_OPENCELLID_API_KEY) as String

        viewBinding.textViewTestOpenCellIdResponse.text = ""

        paras.forEach {
            queryOpenCelliD(it, repo, apiAccessToken)
        }
    }

    private fun queryOpenCelliD(
        paras: CellParameters,
        repo: OpenCelliDRepository,
        apiAccessToken: String
    ) {
        repo.getCellLocation(
            paras, apiAccessToken,
            onSuccess = {
                val geoURI = getGeoURI(it.lat, it.lon)
                val osm = getOpenStreetMapLink(it.lat, it.lon)
                append(
                    "Paras: $paras\n\nOpenCelliD: ${it.url}\n${geoURI}\nOpenStreetMap: $osm"
                )
            },
            onError = {
                append(
                    "Paras: $paras\n\nOpenCelliD: ${it.url}\n\nError: ${it.error}"
                )
            },
        )
    }

    @SuppressLint("SetTextI18n")
    private fun append(string: String) {
        val old = viewBinding.textViewTestOpenCellIdResponse.text.toString()
        viewBinding.textViewTestOpenCellIdResponse.text = old + "\n\n" + string
    }

    companion object {
        private val TAG = OpenCellIdActivity::class.simpleName
    }
}
