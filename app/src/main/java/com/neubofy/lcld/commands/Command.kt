package com.neubofy.lcld.commands

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.lcld.R
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.permissions.Permission
import com.neubofy.lcld.transports.Transport
import com.neubofy.lcld.utils.log


abstract class Command(val context: Context) {
    companion object {
        private val TAG = Command::class.simpleName
    }

    val settings = SettingsRepository.getInstance(context)

    abstract val keyword: String
    abstract val usage: String

    @get:DrawableRes
    abstract val icon: Int

    @get:StringRes
    abstract val shortDescription: Int

    @get:StringRes
    open val longDescription: Int? = null

    abstract val requiredPermissions: List<Permission>
    open val optionalPermissions: List<Permission> = emptyList()

    fun missingRequiredPermissions(): List<Permission> {
        return requiredPermissions.filter { p -> !p.isGranted(context) }
    }

    suspend fun <T> execute(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val missing = missingRequiredPermissions()
        if (missing.isNotEmpty()) {
            val msg = context.getString(
                R.string.cmd_missing_permissions,
                args.joinToString(" "),
                missing.joinToString(", ") { it.toString(context) }
            )
            context.log().w(TAG, msg)
            transport.send(context, msg)
            return
        }

        // Continue executing command.
        // The concrete classes should implement executeInternal.
        //
        // This MUST only return if the command has finished executing!
        // If you need to wait internally (e.g., for a callback from the OS),
        // the command should internally use a CompletableDeferred.
        executeInternal(args, transport)
    }

    internal abstract suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    )

    /**
     * This method should be called by the CommandHandler both during normal completion
     * and when an execution is externally interrupted.
     * This allows commands to put their cleanup logic here,
     * without the commands needing to explicitly invoke this function themselves.
     */
    open fun onExecuteStopped() {
        // "type" because it does not log sub-options which may be present
        context.log().w(TAG, "Stopping command type $keyword")
    }
}
