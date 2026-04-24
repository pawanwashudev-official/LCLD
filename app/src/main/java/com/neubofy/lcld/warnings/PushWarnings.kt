package com.neubofy.lcld.warnings

import android.content.Context
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.lcld.R
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.receiver.PushReceiver
import com.neubofy.lcld.ui.settings.FMDServerActivity
import com.neubofy.lcld.utils.Notifications
import com.neubofy.lcld.utils.Utils.Companion.openUrl


fun shouldWarnUnifiedPushRequired(context: Context): Boolean {
    val settings = SettingsRepository.getInstance(context)
    if (!settings.serverAccountExists()) {
        return false
    }

    return !PushReceiver.isRegisteredWithUnifiedPush(context)
}

fun notifyWarnUnifiedPushRequired(context: Context) {
    val title = context.getString(R.string.missing_push_title)
    val text = context.getString(R.string.missing_push_description)

    Notifications.notify(
        context,
        title,
        text,
        Notifications.CHANNEL_SERVER,
        cls = FMDServerActivity::class.java,
    )
}

fun dialogWarnUnifiedPushRequired(context: Context) {
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.missing_push_title)
        .setMessage(R.string.missing_push_description)
        .setPositiveButton(R.string.Settings_LCLDServer_Push_Register, { dialog, _ ->
            val intent = Intent(context, FMDServerActivity::class.java)
            context.startActivity(intent)
        })
        .setNeutralButton(R.string.Settings_LCLDServer_Push_Open_Help, { _, _ ->
            openUrl(context, "https://fmd-foss.org/docs/fmd-android/push")
        })
        .setNegativeButton(android.R.string.cancel, { dialog, _ -> dialog.dismiss() })
        .setCancelable(false)
        .show()
}
