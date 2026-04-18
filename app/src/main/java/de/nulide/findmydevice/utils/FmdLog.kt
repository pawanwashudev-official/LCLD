package de.nulide.findmydevice.utils

import android.content.Context
import android.util.Log
import de.nulide.findmydevice.data.LogEntry
import de.nulide.findmydevice.data.LogRepository


// Helper extension function for shorter code
fun Context.log() = FmdLog.getInstance(this)


/**
 * Custom logger that logs both the ADB Logcat and to a file.
 * This allows showing the logs in the UI and exporting them.
 */
class FmdLog private constructor(context: Context) {

    companion object : SingletonHolder<FmdLog, Context>(::FmdLog) {}

    private val repo = LogRepository.getInstance(context)

    fun d(tag: String?, msg: String) {
        Log.d(tag, msg)
        val now = System.currentTimeMillis()
        repo.add(LogEntry("DEBUG", now, tag ?: "", msg))
    }

    fun i(tag: String?, msg: String) {
        Log.i(tag, msg)
        val now = System.currentTimeMillis()
        repo.add(LogEntry("INFO", now, tag ?: "", msg))
    }

    fun w(tag: String?, msg: String) {
        Log.w(tag, msg)
        val now = System.currentTimeMillis()
        repo.add(LogEntry("WARN", now, tag ?: "", msg))
    }

    fun e(tag: String?, msg: String) {
        Log.e(tag, msg)
        val now = System.currentTimeMillis()
        repo.add(LogEntry("ERROR", now, tag ?: "", msg))
    }
}