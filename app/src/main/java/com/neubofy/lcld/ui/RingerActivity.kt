package com.neubofy.lcld.ui

import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.neubofy.lcld.R
import com.neubofy.lcld.commands.RING_DURATION_DEFAULT_SECS
import com.neubofy.lcld.commands.RING_DURATION_MAX_SECS
import com.neubofy.lcld.data.Settings
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.receiver.DeviceAdminReceiver
import com.neubofy.lcld.utils.RingerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


const val EXTRA_RING_DURATION: String = "EXTRA_RING_DURATION"

class RingerActivity : FmdActivity() {

    companion object {
        fun newInstance(context: Context, duration: Int) {
            val intent = Intent(context, RingerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_RING_DURATION, duration)
            }
            context.startActivity(intent)
        }
    }

    private var ringtone: Ringtone? = null

    private var oldRingerMode: Int? = null
    private var oldAlarmVolume: Int? = null

    private var oldInterruptionFiler: Int? = null
    private var oldNotificationPolicy: NotificationManager.Policy? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ring)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val buttonStopRinging = findViewById<Button>(R.id.buttonStopRinging)
        buttonStopRinging.setOnClickListener {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
            if (km == null) {
                finish()
                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        finish()
                    }
                })
            } else {
                if (km.isKeyguardSecure) {
                    val authIntent = km.createConfirmDeviceCredentialIntent(null, null)
                    if (authIntent != null) {
                        startActivityForResult(authIntent, 100)
                    } else {
                        finish()
                    }
                } else {
                    finish()
                }
            }
        }

        val settings = SettingsRepository.Companion.getInstance(this)
        ringtone = RingerUtils.getRingtone(this, settings.get(Settings.SET_RINGER_TONE) as String)

        val bundle = intent.extras
        var durationSec: Int = bundle?.getInt(EXTRA_RING_DURATION) ?: RING_DURATION_DEFAULT_SECS
        if (durationSec > RING_DURATION_MAX_SECS) {
            durationSec = RING_DURATION_MAX_SECS
        }

        lifecycleScope.launch(Dispatchers.Default) {
            startRinging(durationSec)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        ringtone?.stop()
        resetVolume()
    }

    private suspend fun startRinging(durationSec: Int) {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        }

        raiseVolume()
        ringtone?.play()

        delay(durationSec * 1000L)

        finish()
    }

    private fun raiseVolume() {
        val audioManager = getSystemService(AudioManager::class.java)
        val notificationManager = getSystemService(NotificationManager::class.java)

        oldRingerMode = audioManager.ringerMode
        oldAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        oldInterruptionFiler = notificationManager.currentInterruptionFilter
        oldNotificationPolicy = notificationManager.notificationPolicy

        notificationManager.setInterruptionFilter(INTERRUPTION_FILTER_ALL)
    }

    private fun resetVolume() {
        val audioManager = getSystemService(AudioManager::class.java)
        val notificationManager = getSystemService(NotificationManager::class.java)

        oldRingerMode?.let {
            audioManager.ringerMode = it
        }
        oldAlarmVolume?.let {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0)
        }

        oldInterruptionFiler?.let {
            notificationManager.setInterruptionFilter(it)
        }
        oldNotificationPolicy?.let {
            notificationManager.notificationPolicy = it
        }
    }
}
