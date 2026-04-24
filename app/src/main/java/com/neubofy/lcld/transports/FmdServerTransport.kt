package com.neubofy.lcld.transports

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.lcld.R
import com.neubofy.lcld.commands.ParserResult
import com.neubofy.lcld.data.FmdLocation
import com.neubofy.lcld.data.Settings
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.net.FMDServerApiRepoSpec
import com.neubofy.lcld.net.FMDServerApiRepository
import com.neubofy.lcld.permissions.Permission
import com.neubofy.lcld.ui.settings.AddAccountActivity


class FmdServerTransport(
    context: Context,
    private val destination: String,
) : Transport<Unit>(Unit) {

    constructor(context: Context) : this(context, "FMD Server")

    private val repo = FMDServerApiRepository.getInstance(FMDServerApiRepoSpec(context))
    private val settings = SettingsRepository.getInstance(context)

    @get:DrawableRes
    override val icon = R.drawable.ic_cloud

    @get:StringRes
    override val title = R.string.transport_fmd_server_title

    override val description = context.getString(R.string.transport_fmd_server_description)

    override val descriptionAuth = context.getString(R.string.transport_fmd_server_description_auth)

    override val descriptionNote = context.getString(R.string.transport_fmd_server_description_note)

    override val requiredPermissions = emptyList<Permission>()

    override val actions = listOf(TransportAction(R.string.Settings_Settings) { activity ->
        activity.startActivity(Intent(context, AddAccountActivity::class.java))
    })

    override fun getDestinationString() = destination

    override fun isAllowed(parsed: ParserResult.Success): Boolean {
        return true
    }

    @SuppressLint("MissingSuperCall")
    override fun send(context: Context, msg: String) {
        //super.send(context, msg, destination)
        // not implemented for FMD Server
    }

    override fun sendNewLocation(context: Context, location: FmdLocation) {
        // no call to super(), we need to completely replace this for FMD Server
        settings.set(Settings.SET_FMDSERVER_LAST_LOCATION_UPLOAD_TIME, location.timeMillis)
        repo.sendLocation(location)
    }
}
