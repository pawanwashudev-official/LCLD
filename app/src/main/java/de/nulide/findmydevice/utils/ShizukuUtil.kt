package de.nulide.findmydevice.utils

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

class ShizukuUtil {
    companion object {
        const val REQUEST_CODE = 4354

        fun isShizukuRunning(): Boolean {
            return Shizuku.pingBinder()
        }

        fun isShizukuPermissionGranted(): Boolean {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }

        fun requestShizukuPermission() {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }
}
