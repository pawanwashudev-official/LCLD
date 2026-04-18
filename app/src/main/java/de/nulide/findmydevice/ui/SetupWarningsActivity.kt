package de.nulide.findmydevice.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import de.nulide.findmydevice.databinding.ActivitySetupWarningsBinding
import de.nulide.findmydevice.permissions.globalAppPermissions
import de.nulide.findmydevice.permissions.isMissingGlobalAppPermission
import de.nulide.findmydevice.services.ServerConnectivityCheckService
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import de.nulide.findmydevice.ui.settings.FMDServerActivity
import de.nulide.findmydevice.warnings.shouldWarnUnifiedPushRequired


class SetupWarningsActivity : FmdActivity() {

    private lateinit var viewBinding: ActivitySetupWarningsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivitySetupWarningsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupEdgeToEdgeAppBar(viewBinding.appBar)
        setupEdgeToEdgeScrollView(viewBinding.scrollView)
    }

    override fun onResume() {
        super.onResume()

        // For simplicity, we always show all warnings/recommendations.
        // Easier for developers (no big if-else tree), and
        // more transparent for users (why did this suddenly disappear?).
        setupRecommPush(this)
        setupRecommConnectivity(this)
        setupPermissionsList(
            this,
            viewBinding.permissionsRequiredTitle,
            viewBinding.permissionsRequiredList,
            globalAppPermissions()
        )
    }

    private fun setupRecommPush(context: Context) {
        val shouldNudge = shouldWarnUnifiedPushRequired(context)

        viewBinding.push.icCheck.isVisible = !shouldNudge
        viewBinding.push.button.isVisible = shouldNudge

        viewBinding.push.button.setOnClickListener {
            val intent = Intent(this, FMDServerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecommConnectivity(context: Context) {
        val shouldNudge =
            ServerConnectivityCheckService.shouldNudgeAboutConnectivityCheck(context)

        viewBinding.connectivity.icCheck.isVisible = !shouldNudge
        viewBinding.connectivity.recommendationConnCheckEnableButton.isVisible = shouldNudge

        viewBinding.connectivity.recommendationConnCheckEnableButton.setOnClickListener {
            val intent = Intent(this, FMDServerActivity::class.java)
            startActivity(intent)
        }
    }
}

fun shouldShowSetupWarnings(context: Context): Boolean {
    return ServerConnectivityCheckService.shouldNudgeAboutConnectivityCheck(context)
            || isMissingGlobalAppPermission(context)
}
