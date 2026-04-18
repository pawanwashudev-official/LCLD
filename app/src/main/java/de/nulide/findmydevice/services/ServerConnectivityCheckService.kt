package de.nulide.findmydevice.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.net.FMDServerApiRepoSpec
import de.nulide.findmydevice.net.FMDServerApiRepository
import de.nulide.findmydevice.ui.settings.FMDServerActivity
import de.nulide.findmydevice.utils.Notifications
import de.nulide.findmydevice.utils.log
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


/**
 * Checks the connectivity to FMD Server at regular intervals in the background.
 */
class ServerConnectivityCheckService : FmdJobService() {

    override val TAG = ServerConnectivityCheckService::class.simpleName

    companion object {
        const val JOB_ID: Int = 112

        val TAG = ServerConnectivityCheckService::class.simpleName

        @JvmStatic
        fun scheduleJob(context: Context) {
            val settings = SettingsRepository.getInstance(context)
            val interval =
                (settings.get(Settings.SET_FMD_SERVER_CONNECTIVITY_CHECK_INTERVAL_HOURS) as Number).toLong()
            scheduleJob(context, interval)
        }

        @JvmStatic
        fun scheduleJob(context: Context, intervalHours: Long) {
            if (intervalHours == 0L) {
                context.log().i(TAG, "Not scheduling connectivity check service")
                return
            }

            val intervalMillis = intervalHours * 60 * 60 * 1000

            val serviceComponent =
                ComponentName(context, ServerConnectivityCheckService::class.java)
            val builder = JobInfo.Builder(JOB_ID, serviceComponent)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                // The period must be >= 15 minutes, due to Android requirements
                .setPeriodic(intervalMillis)
                .setPersisted(true)

            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler.schedule(builder.build())
        }

        @JvmStatic
        fun cancelJob(context: Context) {
            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler.cancel(JOB_ID)
        }

        fun shouldNudgeAboutConnectivityCheck(context: Context): Boolean {
            val settings = SettingsRepository.getInstance(context)
            val interval =
                (settings.get(Settings.SET_FMD_SERVER_CONNECTIVITY_CHECK_INTERVAL_HOURS) as Number).toLong()
            return settings.serverAccountExists() && interval <= 0L
        }

        /**
         * Nudge the user to enable the server connectivity check,
         * by showing a notification telling the user that this feature exists.
         * This should be used sparingly, to not annoy the user.
         */
        fun notifyAboutConnectivityCheck(context: Context) {
            if (shouldNudgeAboutConnectivityCheck(context)) {
                val title = context.getString(R.string.server_connectivity_check_not_enabled_title)
                val text = context.getString(R.string.server_connectivity_check_not_enabled_text)

                Notifications.notify(
                    context,
                    title,
                    text,
                    Notifications.CHANNEL_SERVER,
                    cls = FMDServerActivity::class.java,
                )
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        super.onStartJob(params)
        doWork()
        return true
    }

    private fun doWork() {
        val context = this

        val fmdServerRepo = FMDServerApiRepository.getInstance(FMDServerApiRepoSpec(context))
        val settings = SettingsRepository.getInstance(context)
        val now: Long = System.currentTimeMillis()

        fmdServerRepo.checkConnection(
            { response ->
                context.log().i(TAG, "Successfully connected to FMD Server")
                settings.set(Settings.SET_FMD_SERVER_LAST_CONNECTIVITY_UNIX_TIME, now)
                jobFinished()
            },
            { error ->
                context.log().e(TAG, "Failed to connect to FMD Server: ${error.message}")

                val lastSuccessMillis =
                    (settings.get(Settings.SET_FMD_SERVER_LAST_CONNECTIVITY_UNIX_TIME) as Number).toLong()

                val notifyAfterHours =
                    (settings.get(Settings.SET_FMD_SERVER_CONNECTIVITY_CHECK_NOTIFY_AFTER_HOURS) as Number).toLong()
                val notifyAfterMillis = notifyAfterHours * 60 * 60 * 1000

                // Notify the user if the last successful connection is too long ago
                if (lastSuccessMillis + notifyAfterMillis < now) {
                    val lastSuccessString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Instant.ofEpochMilli(lastSuccessMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    } else {
                        lastSuccessMillis.toString()
                    }

                    val baseUrl = settings.get(Settings.SET_FMDSERVER_URL) as String
                    Notifications.notify(
                        context,
                        context.getString(R.string.server_connectivity_lost_title),
                        context.getString(
                            R.string.server_connectivity_lost_text,
                            baseUrl,
                            lastSuccessString
                        ),
                        Notifications.CHANNEL_FAILED
                    )
                }

                jobFinished()
            }
        )
    }
}
