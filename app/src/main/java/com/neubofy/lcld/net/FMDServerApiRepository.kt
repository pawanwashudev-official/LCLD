package com.neubofy.lcld.net

import android.content.Context
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.neubofy.lcld.data.EncryptedSettingsRepository
import com.neubofy.lcld.data.FmdLocation
import com.neubofy.lcld.data.FmdKeyPair
import com.neubofy.lcld.data.Settings
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.utils.CypherUtils
import com.neubofy.lcld.utils.PatchedVolley
import com.neubofy.lcld.utils.SingletonHolder
import com.neubofy.lcld.utils.log
import org.json.JSONException
import org.json.JSONObject
import java.security.KeyPair
import java.util.Date


data class FMDServerApiRepoSpec(
    val context: Context,
)

/**
 * All network requests run on a background thread. This is handled by Volley.
 */
class FMDServerApiRepository private constructor(spec: FMDServerApiRepoSpec) {

    companion object :
        SingletonHolder<FMDServerApiRepository, FMDServerApiRepoSpec>(::FMDServerApiRepository) {

        val TAG = FMDServerApiRepository::class.simpleName

        const val MIN_REQUIRED_SERVER_VERSION = "0.9.0"

        private const val URL_ACCESS_TOKEN = "/requestAccess"
        private const val URL_COMMAND = "/command"
        private const val URL_LOCATION = "/location"
        private const val URL_PICTURE = "/picture"
        private const val URL_DEVICE = "/device"
        private const val URL_PUSH = "/push"
        private const val URL_SALT = "/salt"
        private const val URL_PRIVKEY = "/key"
        private const val URL_PUBKEY = "/pubKey"
        private const val URL_PASSWORD = "/password"
        private const val URL_VERSION = "/version"

        private const val ACCESS_TOKEN_VALIDITY_SECS = 7 * 24 * 60 * 60 // 1 week
    }

    private val context = spec.context
    private var baseUrl = ""
    private val queue: RequestQueue = PatchedVolley.newRequestQueue(spec.context)
    private val settingsRepo = SettingsRepository.getInstance(context)
    private val encryptedSettingsRepo = EncryptedSettingsRepository.getInstance(context)

    init {
        loadBaseUrl()
    }

    /**
     * Reload the base URL from settings and cache it in a local field.
     * This should be called every time where the settings could have changed.
     */
    private fun loadBaseUrl() {
        val tempBaseUrl = settingsRepo.get(Settings.SET_FMDSERVER_URL) as String
        // ensure the base URL doesn't end in /
        if (tempBaseUrl.endsWith("/")) {
            settingsRepo.set(
                Settings.SET_FMDSERVER_URL,
                tempBaseUrl.trim('/')
            )
        }
        baseUrl = settingsRepo.get(Settings.SET_FMDSERVER_URL) as String + "/api/v1"
    }

    fun getServerVersion(
        customBaseUrl: String, // to allow querying other servers
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val request = StringRequest(
            Method.GET,
            "${customBaseUrl.trim('/')}/api/v1$URL_VERSION",
            onResponse,
            onError
        )
        queue.add(request)
    }

    fun registerAccount(
        username: String,
        privKey: String,
        pubKey: String,
        hashedPW: String,
        registrationToken: String,
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        loadBaseUrl()
        val jsonObject = JSONObject()
        try {
            jsonObject.put("hashedPassword", hashedPW)
            jsonObject.put("pubkey", pubKey)
            jsonObject.put("privkey", privKey)
            jsonObject.put("requestedUsername", username)
            jsonObject.put("registrationToken", registrationToken)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be POST instead of PUT
            Method.PUT, baseUrl + URL_DEVICE, jsonObject,
            { response: JSONObject ->
                try {
                    settingsRepo.set(Settings.SET_FMDSERVER_ID, response["DeviceId"])
                    onResponse.onResponse(Unit)
                } catch (e: JSONException) {
                    context.log().w(TAG, "registerAccount: ${e.stackTraceToString()}")
                    onError.onErrorResponse(VolleyError("Response has no DeviceId field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    fun getSalt(
        userId: String,
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", userId)
            jsonObject.put("Data", "")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be GET (or POST-as-GET) instead of PUT
            Method.PUT, baseUrl + URL_SALT, jsonObject,
            { response ->
                try {
                    val salt = response["Data"] as String
                    onResponse.onResponse(salt)
                } catch (e: JSONException) {
                    context.log().w(TAG, "getSalt: ${e.stackTraceToString()}")
                    onError.onErrorResponse(VolleyError("Salt response has no Data field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    fun getAccessToken(
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        getAccessToken(
            settingsRepo.get(Settings.SET_FMDSERVER_ID) as String,
            settingsRepo.get(Settings.SET_FMD_CRYPT_HPW) as String,
            onResponse,
            onError,
        )
    }

    fun getAccessToken(
        userId: String,
        hashedPW: String,
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", userId)
            jsonObject.put("Data", hashedPW)
            jsonObject.put("SessionDurationSeconds", ACCESS_TOKEN_VALIDITY_SECS)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be GET (or POST-as-GET) instead of PUT
            Method.PUT, baseUrl + URL_ACCESS_TOKEN, jsonObject,
            { response ->
                try {
                    val accessToken = response["Data"] as String
                    onResponse.onResponse(accessToken)
                } catch (e: JSONException) {
                    context.log().w(TAG, "getAccessToken: ${e.stackTraceToString()}")
                    onError.onErrorResponse(VolleyError("Access Token response has no Data field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    fun <T> doRequestWithCachedToken(
        doRequest: (String, Response.Listener<T>, Response.ErrorListener) -> Unit,
        onResponse: Response.Listener<T>,
        onError: Response.ErrorListener,
    ) {
        val accessToken = encryptedSettingsRepo.getCachedAccessToken()
        doRequest(
            accessToken,
            onResponse,
            { error ->
                // Try to refresh the access token
                context.log().i(TAG, "Refreshing access token")
                getAccessToken(
                    { newAccessToken ->
                        // If refreshing succeeds, store it and retry the original request
                        encryptedSettingsRepo.setCachedAccessToken(newAccessToken)
                        doRequest(newAccessToken, onResponse, onError)
                    },
                    // If refreshing fails, use the original error handler
                    onError,
                )
            },
        )
    }

    fun checkConnection(
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        // TODO: dedicated connectivity check endpoint that doesn't return data
        // Previously, we used getAccessToken for the connectivity check.
        // However, that doesn't work when the account is locked.
        // We need an endpoint that we can access with the cached access token.
        doRequestWithCachedToken(this::getPrivateKeyRaw, onResponse, onError)
    }

    /**
     * Gets the private key stored on the server for this account.
     * Decrypts and parses the private key into a [KeyPair].
     *
     * ## Security
     *
     * This is safe because the private key is encrypted using AES-GCM.
     * AES-GCM is an AEAD, which provides integrity protection.
     * This means that the server cannot modify the ciphertext without
     * causing encryption to fail (unlike other modes like AES-CBC).
     */
    fun getPrivateKey(
        password: String,
        accessToken: String,
        onResponse: Response.Listener<KeyPair>,
        onError: Response.ErrorListener,
    ) {
        getPrivateKeyRaw(
            accessToken,
            onResponse = { rawPrivateKey ->
                val keyPair: KeyPair? =
                    CypherUtils.decryptPrivateKeyWithPassword(rawPrivateKey, password)
                if (keyPair == null) {
                    context.log().w(TAG, "getPrivateKey: Failed to decrypt private key")
                    onError.onErrorResponse(VolleyError("Failed to decrypt private key"))
                } else {
                    onResponse.onResponse(keyPair)
                }
            },
            onError = onError,
        )
    }

    /**
     * Gets the raw private key stored on the server for this account.
     * See also [getPrivateKey].
     */
    fun getPrivateKeyRaw(
        accessToken: String,
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", accessToken)
            jsonObject.put("Data", "")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be GET (or POST-as-GET) instead of PUT
            Method.PUT, baseUrl + URL_PRIVKEY, jsonObject,
            { response ->
                try {
                    val privateKey = response["Data"] as String
                    onResponse.onResponse(privateKey)
                } catch (e: JSONException) {
                    context.log().w(TAG, "getPrivateKeyRaw: ${e.stackTraceToString()}")
                    onError.onErrorResponse(VolleyError("Private Key response has no Data field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    /**
     * Gets the public key stored on the server for this account.
     *
     * WARNING: The returned public key is unauthenticated!
     * A malicious server could provide a wrong public key and MITM you.
     *
     * The safe option is to get the private key and derive the public key from that.
     * See [getPrivateKey].
     */
    fun getPublicKeyUnsafe(
        accessToken: String,
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", accessToken)
            jsonObject.put("Data", "")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be GET (or POST-as-GET) instead of PUT
            Method.PUT, baseUrl + URL_PUBKEY, jsonObject,
            { response ->
                try {
                    val publicKey = response["Data"] as String
                    onResponse.onResponse(publicKey)
                } catch (e: JSONException) {
                    context.log().w(TAG, "getPublicKey: ${e.stackTraceToString()}")
                    onError.onErrorResponse(VolleyError("Public Key response has no Data field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    /**
     * This MUST be wrapped in a Thread() because it does password hashing.
     *
     * TODO: handled this internally in the repo.
     */
    fun login(
        userId: String,
        password: String,
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        loadBaseUrl()

        getSalt(userId, onError = onError, onResponse = { salt ->
            val authPassword = CypherUtils.hashPasswordForLogin(password, salt)
            getAccessToken(userId, authPassword, onError = onError, onResponse = { accessToken ->
                encryptedSettingsRepo.setCachedAccessToken(accessToken)

                getPrivateKey(
                    password,
                    accessToken,
                    onError = onError,
                    onResponse = { keyPair ->
                        // Security: don't store the private+public key PEM strings as received from the server.
                        // Instead, decrypt and parse them to trusted, well-formed objects.
                        // Then encode them again for storage.
                        val fmdKeyPair = FmdKeyPair(keyPair, password)
                        settingsRepo.apply {
                            set(Settings.SET_FMD_CRYPT_HPW, authPassword)
                            set(Settings.SET_FMDSERVER_ID, userId)
                            setKeys(fmdKeyPair)
                        }
                        onResponse.onResponse(Unit)
                    })
            })
        })
    }

    fun unregister(
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        doRequestWithCachedToken(this::unregisterInternal, onResponse, onError)
    }

    private fun unregisterInternal(
        accessToken: String,
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", accessToken)
            jsonObject.put("Data", "")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonPostRequest(
            // XXX: This should be a dedicated /deleteDevice endpoint
            Method.POST, baseUrl + URL_DEVICE, jsonObject,
            { _ ->
                settingsRepo.removeServerAccount()
                encryptedSettingsRepo.setCachedAccessToken("")
                onResponse.onResponse(Unit)
            },
            { error ->
                context.log().w(TAG, "unregisterInternal: ${error.stackTraceToString()}")
                onError.onErrorResponse(error)
            },
        )
        queue.add(request)
    }

    fun registerPushEndpoint(
        endpoint: String,
        onError: Response.ErrorListener,
    ) {
        doRequestWithCachedToken<Unit>(
            doRequest = { accessToken, _, onError2 ->
                registerPushEndpointInternal(accessToken, endpoint, onError2)
            },
            onResponse = { _ -> },
            onError,
        )
    }

    fun registerPushEndpointInternal(
        accessToken: String,
        endpoint: String,
        onError: Response.ErrorListener,
    ) {
        context.log().i(TAG, "Registering push endpoint $endpoint")
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", accessToken)
            jsonObject.put("Data", endpoint)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonPostRequest(
            Method.PUT, baseUrl + URL_PUSH, jsonObject,
            { _ -> },
            { error ->
                context.log().w(TAG, "registerPushEndpointInternal: ${error.stackTraceToString()}")
                onError.onErrorResponse(error)
            }
        )
        queue.add(request)
    }

    fun changePassword(
        newHashedPW: String,
        newPrivKey: String,
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        doRequestWithCachedToken(
            doRequest = { accessToken, onResponse2, onError2 ->
                changePasswordInternal(accessToken, newHashedPW, newPrivKey, onResponse2, onError2)
            },
            onResponse,
            onError,
        )
    }

    fun changePasswordInternal(
        accessToken: String,
        newHashedPW: String,
        newPrivKey: String,
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", accessToken)
            jsonObject.put("hashedPassword", newHashedPW)
            jsonObject.put("privkey", newPrivKey)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            Method.POST, baseUrl + URL_PASSWORD, jsonObject,
            { response ->
                if (response.has("Data")) {
                    settingsRepo.set(Settings.SET_FMD_CRYPT_PRIVKEY, newPrivKey)
                    settingsRepo.set(Settings.SET_FMD_CRYPT_HPW, newHashedPW)
                    onResponse.onResponse(Unit)
                } else {
                    onError.onErrorResponse(VolleyError("change password response has no Data field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    fun getCommand(
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        doRequestWithCachedToken<String>(this::getCommandInternal, onResponse, onError)
    }

    fun getCommandInternal(
        accessToken: String,
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", accessToken)
            jsonObject.put("Data", "")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be GET (or POST-as-GET) instead of PUT
            Method.PUT, baseUrl + URL_COMMAND, jsonObject,
            { response ->
                try {
                    val command = response["Data"] as String
                    val time = (response["UnixTime"] as Number).toLong()
                    val sig = response["CmdSig"] as String

                    if (command.isEmpty()) {
                        return@JsonObjectRequest onResponse.onResponse("")
                    }
                    // Exception: this value does not need to be signed.
                    // The command field is abused to send this status notification.
                    if (command == "423") {
                        return@JsonObjectRequest onResponse.onResponse("423")
                    }

                    // This only needs to be strictly increasing, to prevent replay attacks.
                    // It doesn't need to be "current", i.e. we don't care how far away from "now" this timestamp is.
                    val lastCmdMillis =
                        (settingsRepo.get(Settings.SET_FMDSERVER_LAST_CMD_MILLIS) as Number).toLong()
                    if (time <= lastCmdMillis) {
                        val errorMsg = "Timestamp is not increasing: $time <= $lastCmdMillis"
                        context.log().e(TAG, errorMsg)
                        return@JsonObjectRequest onError.onErrorResponse(VolleyError(errorMsg))
                    }

                    val publicKeyPem = settingsRepo.get(Settings.SET_FMD_CRYPT_PUBKEY) as String
                    if (!CypherUtils.verifySig(publicKeyPem, "$time:$command", sig)) {
                        val errorMsg = "Failed to verify the signature of command '$command'"
                        context.log().e(TAG, errorMsg)
                        return@JsonObjectRequest onError.onErrorResponse(VolleyError(errorMsg))
                    }

                    settingsRepo.set(Settings.SET_FMDSERVER_LAST_CMD_MILLIS, time)
                    onResponse.onResponse(command)
                } catch (e: JSONException) {
                    context.log().w(TAG, "getCommandInternal: ${e.stackTraceToString()}")
                    onError.onErrorResponse(VolleyError("get command response has no Data field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    /**
     * This MUST be wrapped in a Thread() because it does async crypto.
     *
     * TODO: handled this internally in the repo.
     */
    fun sendPicture(
        picture: String,
    ) {
        val publicKey = settingsRepo.getKeys()?.publicKey
        if (publicKey == null) {
            context.log().e(TAG, "Public key was null")
            return
        }
        val dataBytes = CypherUtils.encryptWithKey(publicKey, picture)
        val dataBase64 = CypherUtils.encodeBase64(dataBytes)

        val onError = { error: VolleyError -> error.printStackTrace() }

        doRequestWithCachedToken<Unit>(
            doRequest = { accessToken, _, onError2 ->
                sendPictureInternal(accessToken, dataBase64, onError2)
            },
            { _ -> },
            onError,
        )
    }

    fun sendPictureInternal(
        accessToken: String,
        encryptedPicture: String,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", accessToken)
            jsonObject.put("Data", encryptedPicture)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonPostRequest(
            Method.POST, baseUrl + URL_PICTURE, jsonObject,
            { _ -> },
            onError,
        )
        queue.add(request)
    }

    /**
     * This MUST be wrapped in a Thread() because it does async crypto.
     *
     * TODO: handled this internally in the repo.
     */
    fun sendLocation(location: FmdLocation) {
        // Prepare payload
        val publicKey = settingsRepo.getKeys()?.publicKey
        if (publicKey == null) {
            context.log().e(TAG, "Public key was null")
            return
        }

        val locationDataObject = JSONObject()
        try {
            locationDataObject.put("provider", location.provider)

            locationDataObject.put("lat", location.lat)
            locationDataObject.put("lon", location.lon)

            locationDataObject.put("accuracy", location.accuracy)
            locationDataObject.put("altitude", location.altitude)
            locationDataObject.put("heading", location.bearing)
            locationDataObject.put("speed", location.speed)

            locationDataObject.put("bat", location.batteryLevel)
            locationDataObject.put("date", location.timeMillis)
            locationDataObject.put("time", Date(location.timeMillis).toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val jsonSerialised = locationDataObject.toString()
        val encryptedLocationBytes = CypherUtils.encryptWithKey(publicKey, jsonSerialised)
        val encryptedLocation = CypherUtils.encodeBase64(encryptedLocationBytes)

        val onError = { error: VolleyError -> error.printStackTrace() }

        // Send payload
        doRequestWithCachedToken<Unit>(
            doRequest = { accessToken, _, onError2 ->
                sendLocationInternal(accessToken, encryptedLocation, onError2)
            },
            { _ -> },
            onError,
        )
    }

    fun sendLocationInternal(
        accessToken: String,
        encryptedLocation: String,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", accessToken)
            jsonObject.put("Data", encryptedLocation)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonPostRequest(
            Method.POST, baseUrl + URL_LOCATION, jsonObject,
            { _ -> },
            onError,
        )
        queue.add(request)
    }

}
