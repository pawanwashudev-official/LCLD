package de.nulide.findmydevice.permissions

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.receiver.DeviceAdminReceiver


class DeviceAdminPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_device_admin_name

    override fun isGranted(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(context, DeviceAdminReceiver::class.java))
    }

    override fun request(activity: Activity) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            ComponentName(activity, DeviceAdminReceiver::class.java)
        )
        activity.startActivity(intent)
    }
}
