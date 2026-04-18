package de.nulide.findmydevice.data

import android.content.Context
import de.nulide.findmydevice.utils.SingletonHolder


// Only store the registration tokens in the dev flavor
class RegistrationTokenRepository private constructor(private val context: Context) {

    companion object :
        SingletonHolder<RegistrationTokenRepository, Context>(::RegistrationTokenRepository) {}

    fun get(serverUrl: String): String {
        // noop
        return ""
    }

    fun set(serverUrl: String, token: String) {
        // noop
    }
}