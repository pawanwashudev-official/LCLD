package de.nulide.findmydevice.utils

import android.content.Context
import android.widget.Toast
import de.nulide.findmydevice.R
import java.io.OutputStream


class RootAccess {
    companion object {
        @JvmStatic
        fun isRooted(): Boolean {
            var proc: Process? = null
            return try {
                proc = Runtime.getRuntime().exec("su -c exit")
                proc.waitFor() == 0
            } catch (e: Exception) {
                false
            } finally {
                proc?.destroy()
            }
        }

        @JvmStatic
        fun execCommand(context: Context, com: String) {
            var proc: Process? = null
            try {
                val toExec = "$com && echo hi && exit\n"
                proc = Runtime.getRuntime().exec("su")

                val outputStream: OutputStream = proc.outputStream
                outputStream.write(toExec.toByteArray())
                outputStream.flush()
                outputStream.close()

                proc.waitFor()
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.perm_root_denied), Toast.LENGTH_LONG).show()
                e.printStackTrace()
            } finally {
                proc?.destroy()
            }
        }
    }
}
