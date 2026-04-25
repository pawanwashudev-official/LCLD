package com.neubofy.lcld.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.neubofy.lcld.databinding.ActivitySetupWarningsBinding
import com.neubofy.lcld.permissions.globalAppPermissions
import com.neubofy.lcld.permissions.isMissingGlobalAppPermission
import com.neubofy.lcld.services.ServerConnectivityCheckService
import com.neubofy.lcld.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.neubofy.lcld.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import com.neubofy.lcld.ui.settings.FMDServerActivity


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
        setupRecommConnectivity(this)
        setupPermissionsList(
            this,
            viewBinding.permissionsRequiredTitle,
            viewBinding.permissionsRequiredList,
            globalAppPermissions()
        )
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
