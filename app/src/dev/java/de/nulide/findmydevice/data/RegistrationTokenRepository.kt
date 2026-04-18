package de.nulide.findmydevice.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import de.nulide.findmydevice.utils.SingletonHolder
import java.io.File
import java.io.FileReader


private const val REGISTRATION_TOKENS_FILENAME = "registration_tokens.json"

class RegistrationTokensModel : HashMap<String, String>()


class RegistrationTokenRepository private constructor(private val context: Context) {

    companion object :
        SingletonHolder<RegistrationTokenRepository, Context>(::RegistrationTokenRepository) {}

    private val gson = Gson()

    val tokens: RegistrationTokensModel

    init {
        val file = File(context.filesDir, REGISTRATION_TOKENS_FILENAME)
        if (!file.exists()) {
            file.createNewFile()
        }
        val reader = JsonReader(FileReader(file))
        tokens = gson.fromJson(reader, RegistrationTokensModel::class.java)
            ?: RegistrationTokensModel()
    }

    private fun save() {
        val raw = gson.toJson(tokens)
        val file = File(context.filesDir, REGISTRATION_TOKENS_FILENAME)
        file.writeText(raw)
    }

    fun get(serverUrl: String): String {
        return tokens.getOrDefault(serverUrl, "")
    }

    fun set(serverUrl: String, token: String) {
        tokens[serverUrl] = token
        save()
    }
}