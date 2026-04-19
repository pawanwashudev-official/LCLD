package de.nulide.findmydevice.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TheftModeService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null

    override fun onCreate() {
        super.onCreate()

        val settings = SettingsRepository.getInstance(this)
        val ringtoneUriString = settings.get(Settings.SET_RINGER_TONE) as String
        val ringtoneUri = Uri.parse(ringtoneUriString)
        ringtone = RingtoneManager.getRingtone(this, ringtoneUri)

        ringtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull {
                cameraManager?.getCameraCharacteristics(it)?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1337, createNotification())

        // Force max volume for alarm
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )

        startTheftLoop()

        return START_STICKY
    }

    private fun startTheftLoop() {
        scope.launch {
            while (isActive) {
                // Active phase: 3 minutes
                startDistressSignals()
                delay(3 * 60 * 1000L) // 3 mins

                // Pause phase: 3 minutes
                stopDistressSignals()
                delay(3 * 60 * 1000L) // 3 mins
            }
        }
    }

    private var flashJob: Job? = null

    private fun startDistressSignals() {
        ringtone?.play()

        val pattern = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }

        flashJob = scope.launch {
            var isOn = false
            while (isActive) {
                try {
                    cameraId?.let {
                        cameraManager?.setTorchMode(it, isOn)
                        isOn = !isOn
                    }
                } catch (e: Exception) {}
                delay(500)
            }
        }
    }

    private fun stopDistressSignals() {
        ringtone?.stop()
        vibrator?.cancel()
        flashJob?.cancel()
        try {
            cameraId?.let { cameraManager?.setTorchMode(it, false) }
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        stopDistressSignals()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "theft_mode_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Theft Mode Active",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("LCLD: Theft Mode Active")
            .setContentText("Emergency tracking and distress signals running.")
            .setSmallIcon(R.drawable.ic_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }
}
