package com.neubofy.lcld.commands

import android.content.Context
import android.location.LocationManager.GPS_PROVIDER
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.lcld.R
import com.neubofy.lcld.locationproviders.AddJobResult
import com.neubofy.lcld.locationproviders.CellLocationProvider
import com.neubofy.lcld.locationproviders.FUSED_PROVIDER
import com.neubofy.lcld.locationproviders.GpsLocationProvider
import com.neubofy.lcld.locationproviders.LocationAutoOnOffHandler
import com.neubofy.lcld.locationproviders.LocationProvider
import com.neubofy.lcld.permissions.LocationPermission
import com.neubofy.lcld.permissions.WriteSecureSettingsPermission
import com.neubofy.lcld.transports.Transport
import com.neubofy.lcld.utils.log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class LocateCommand(context: Context) : Command(context) {

    companion object {
        private val TAG = LocateCommand::class.simpleName
    }

    override val keyword = "locate"

    // Do not document the accuracy parameter. *It is for debugging.*
    // Users should rely on the normal auto-convergence of the "gps" option.
    // In any real scenario where you don't know where your device is
    // (At walking speed under clear sky? Moving at 120 km/h in a train?)
    // you don't know a-priori what accuracy is possible.
    override val usage = "locate [last | all | cell | fused | gps]"

    @get:DrawableRes
    override val icon = R.drawable.ic_location

    @get:StringRes
    override val shortDescription = R.string.cmd_locate_description_short

    @get:StringRes
    override val longDescription = R.string.cmd_locate_description_long

    override val requiredPermissions = listOf(LocationPermission())

    override val optionalPermissions = listOf(WriteSecureSettingsPermission())

    // Fields for execution
    private var providers = mutableListOf<LocationProvider>()
    private val locOnOffHandler = LocationAutoOnOffHandler.getInstance(context)
    private var addJobResult: AddJobResult? = null
    private var deferred: CompletableDeferred<Unit>? = null

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        // fmd locate last
        if (args.contains("last")) {
            withContext(Dispatchers.IO) {
                val provider = GpsLocationProvider(context, transport, FUSED_PROVIDER, null)
                provider.getLastKnownLocation()
            }
            // Even if last location is not available, return here.
            // Because requesting "last" explicitly asks not to refresh the location.
            return
        }

        var accuracy: Int? = null
        try {
            accuracy = args.firstOrNull { it.startsWith("acc=") }?.substring(4)?.toInt()
        } catch (e: NumberFormatException) {
            context.log().w(TAG, "Invalid accuracy, using null")
        }

        addJobResult = locOnOffHandler.addJob()

        if (!addJobResult!!.isLocationOn) {
            context.log().w(
                TAG,
                "Cannot locate: Location is off and missing permission WRITE_SECURE_SETTINGS"
            )
            transport.send(context, context.getString(R.string.cmd_locate_response_location_off))
            return
        }

        deferred = CompletableDeferred<Unit>()

        // build the location providers
        providers.clear()
        var chosen = false

        if (args.contains("cell")) {
            providers.add(CellLocationProvider(context, transport))
            chosen = true
        }
        if (args.contains("fused")) {
            providers.add(GpsLocationProvider(context, transport, FUSED_PROVIDER, accuracy))
            chosen = true
        }
        if (args.contains("gps")) {
            providers.add(GpsLocationProvider(context, transport, GPS_PROVIDER, accuracy))
            chosen = true
        }

        if (args.contains("all") || !chosen) {
            providers.add(GpsLocationProvider(context, transport, FUSED_PROVIDER, accuracy))
            providers.add(CellLocationProvider(context, transport))
        }

        // run the providers and get the locations
        withContext(Dispatchers.IO) {
            providers
                // launch all providers in parallel
                .map { prov -> prov.getAndSendLocation() }
                // await all providers
                .forEach { deferredProvider -> deferredProvider.await() }
        }
        deferred?.complete(Unit)
    }

    override fun onExecuteStopped() {
        super.onExecuteStopped()

        providers.forEach { it.onStopped() }
        addJobResult?.let {
            locOnOffHandler.removeJob(it.jobId)
        }
        deferred?.complete(Unit)
    }
}
