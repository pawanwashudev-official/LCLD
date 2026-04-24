package com.neubofy.lcld.services

import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import com.neubofy.lcld.R
import com.neubofy.lcld.data.Settings
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.receiver.DeviceAdminReceiver
import com.neubofy.lcld.ui.LockScreenMessage
import com.neubofy.lcld.utils.RingerUtils
import java.util.*

class TheftService : Service() {

    private lateinit var settings: SettingsRepository
    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private var isRingingPeriod = true
    private var timerTask: Timer? = null

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_THEFT) {
            stopTheftMode()
            return START_NOT_STICKY
        }

        startTheftLogic()
        return START_STICKY
    }

    private fun startTheftLogic() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        }

        // Show Lock Screen Message
        val lockIntent = Intent(this, LockScreenMessage::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(lockIntent)

        // 3 min ON, 3 min OFF cycle
        timerTask = Timer()
        timerTask?.scheduleAtFixedRate(object : TimerTask() {
            var elapsedSeconds = 0
            override fun run() {
                if (elapsedSeconds % 360 == 0) {
                    isRingingPeriod = true // Start 3 min ring
                } else if (elapsedSeconds % 360 == 180) {
                    isRingingPeriod = false // Relax 3 min
                }
                
                if (isRingingPeriod) {
                    performActiveTheftActions()
                }
                elapsedSeconds++
            }
        }, 0, 1000)

        // Volume Reset Every 3 Secs
        handler.post(object : Runnable {
            override fun run() {
                if (isRingingPeriod) {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_RING,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                        0
                    )
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_ALARM,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                        0
                    )
                }
                handler.postDelayed(this, 3000)
            }
        })
    }

    private fun performActiveTheftActions() {
        // Ring
        if (ringtone == null || !ringtone!!.isPlaying) {
            ringtone = RingerUtils.getRingtone(applicationContext, settings.get(Settings.SET_RINGER_TONE) as String)
            ringtone?.play()
        }

        // Flashlight (Toggle every sec)
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, true)
            handler.postDelayed({ cameraManager.setTorchMode(cameraId, false) }, 500)
        } catch (e: Exception) {}

        // Vibrate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(500)
        }
    }

    private fun stopTheftMode() {
        timerTask?.cancel()
        handler.removeCallbacksAndMessages(null)
        ringtone?.stop()
        settings.set(Settings.SET_THEFT_MODE_ACTIVE, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val channelId = "theft_mode"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Theft Mode Active", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("LCLD Theft Mode")
            .setContentText("Theft mode is active. Enter PIN to stop.")
            .setSmallIcon(R.drawable.ic_security)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 701
        const val ACTION_STOP_THEFT = "com.neubofy.lcld.STOP_THEFT"
    }
}
