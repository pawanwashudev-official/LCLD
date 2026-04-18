package de.nulide.findmydevice.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import de.nulide.findmydevice.R
import de.nulide.findmydevice.net.MinRequiredVersionResult
import de.nulide.findmydevice.net.isMinRequiredVersion
import de.nulide.findmydevice.utils.Notifications
import de.nulide.findmydevice.utils.log


class ServerVersionCheckService : FmdJobService() {

    override val TAG = ServerVersionCheckService::class.simpleName

    companion object {
        const val JOB_ID: Int = 111

        @JvmStatic
        fun scheduleJobNow(context: Context) {
            val serviceComponent = ComponentName(context, ServerVersionCheckService::class.java)
            val builder = JobInfo.Builder(JOB_ID, serviceComponent)
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler.schedule(builder.build())
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        super.onStartJob(params)
        doWork()
        return true
    }

    private fun doWork() {
        val context = this

        isMinRequiredVersion(context) { result ->
            when (result) {
                is MinRequiredVersionResult.Success -> {
                    // do nothing
                    jobFinished()
                }

                is MinRequiredVersionResult.Error -> {
                    context.log().e(TAG, "Failed to get server version: ${result.message}")
                    jobFinished()
                }

                is MinRequiredVersionResult.ServerOutdated -> {
                    val text =
                        getString(R.string.server_version_upgrade_required_text)
                            .replace("{CURRENT}", result.actualVersion)
                            .replace("{MIN}", result.minRequiredVersion)

                    Notifications.notify(
                        context,
                        context.getString(R.string.server_version_upgrade_required_title),
                        text,
                        Notifications.CHANNEL_SERVER
                    )
                    jobFinished()
                }
            }
        }
    }
}
