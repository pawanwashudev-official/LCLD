package com.neubofy.lcld.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.android.volley.VolleyError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.lcld.BuildConfig
import com.neubofy.lcld.R
import com.neubofy.lcld.data.FmdKeyPair
import com.neubofy.lcld.data.RegistrationTokenRepository
import com.neubofy.lcld.data.Settings
import com.neubofy.lcld.data.SettingsRepository
import com.neubofy.lcld.net.FMDServerApiRepoSpec
import com.neubofy.lcld.net.FMDServerApiRepository
import com.neubofy.lcld.net.MinRequiredVersionResult
import com.neubofy.lcld.net.isMinRequiredVersion
import com.neubofy.lcld.services.ServerConnectivityCheckService
import com.neubofy.lcld.services.ServerLocationUploadService
import com.neubofy.lcld.ui.FmdActivity
import com.neubofy.lcld.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.neubofy.lcld.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import com.neubofy.lcld.utils.CypherUtils
import com.neubofy.lcld.utils.CypherUtils.MIN_PASSWORD_LENGTH
import com.neubofy.lcld.utils.Utils.Companion.copyToClipboard
import com.neubofy.lcld.utils.Utils.Companion.openUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

class AddAccountActivity : FmdActivity(), TextWatcher {
    private lateinit var editTextServerUrl: EditText
    private lateinit var textViewServerVersion: TextView
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var fmdServerRepo: FMDServerApiRepository
    private lateinit var registrationTokensRepo: RegistrationTokenRepository

    private var loadingDialog: AlertDialog? = null

    private var lastTextChangedMillis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account)

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView))

        settingsRepo = SettingsRepository.getInstance(this)
        fmdServerRepo = FMDServerApiRepository.getInstance(FMDServerApiRepoSpec(this))
        registrationTokensRepo = RegistrationTokenRepository.getInstance(this)

        if (settingsRepo.serverAccountExists()) {
            val fmdServerIntent = Intent(this, FMDServerActivity::class.java)
            finish()
            startActivity(fmdServerIntent)
        }

        val btnOpenWebsite = findViewById<Button>(R.id.buttonOpenFmdServerWebsite)
        btnOpenWebsite.setOnClickListener { _ ->
            openUrl(this, "https://fmd-foss.org/docs/fmd-server/overview")
        }

        // Actively opt-in to using the "official" server
        val btnUseDefaultServer = findViewById<Button>(R.id.buttonUseDefaultServer)
        btnUseDefaultServer.setOnClickListener { _ -> editTextServerUrl.setText(BuildConfig.DEFAULT_FMD_SERVER_URL) }

        editTextServerUrl = findViewById(R.id.editTextServerUrl)
        editTextServerUrl.addTextChangedListener(this)

        textViewServerVersion = findViewById(R.id.textViewServerVersion)

        btnLogin = findViewById(R.id.buttonLogin)
        btnLogin.setOnClickListener { view: View -> this.onLoginClicked(view) }

        btnRegister = findViewById(R.id.buttonRegister)
        btnRegister.setOnClickListener { view: View -> this.onRegisterClicked(view) }

        val lastKnownServerUrl = settingsRepo.get(Settings.SET_FMDSERVER_URL) as String

        // This must be after btnRegister and btnLogin are assigned,
        // because it causes a call to afterTextChanged, which then accesses btnRegister.
        editTextServerUrl.setText(lastKnownServerUrl)

        getAndShowServerVersion(this, lastKnownServerUrl)
    }

    private fun prefillRegistrationToken(editText: EditText) {
        val serverUrl = settingsRepo.get(Settings.SET_FMDSERVER_URL) as String
        val cachedToken = registrationTokensRepo.get(serverUrl)
        editText.setText(cachedToken)
    }

    private fun cacheRegistrationToken(token: String) {
        val serverUrl = settingsRepo.get(Settings.SET_FMDSERVER_URL) as String
        registrationTokensRepo.set(serverUrl, token)
    }

    private fun onRegisterClicked(view: View) {
        val context = view.context
        val registerLayout = layoutInflater.inflate(R.layout.dialog_register, null)

        val usernameInput = registerLayout.findViewById<EditText>(R.id.editTextUsername)
        val passwordInput = registerLayout.findViewById<EditText>(R.id.editTextPassword)
        val registrationTokenInput =
            registerLayout.findViewById<EditText>(R.id.editTextRegistrationToken)

        prefillRegistrationToken(registrationTokenInput)

        val registerDialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.Settings_FMDServer_Register))
            .setView(registerLayout)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.Ok)) { _, _ ->
                showLoadingIndicator(context)

                val username = usernameInput.text.toString()
                val password = passwordInput.text.toString()
                val registrationToken = registrationTokenInput.text.toString()

                cacheRegistrationToken(registrationToken)

                if (password.isEmpty()) {
                    Toast.makeText(context, R.string.pw_change_empty, Toast.LENGTH_LONG).show()
                    loadingDialog?.cancel()
                } else if (password.length < MIN_PASSWORD_LENGTH) {
                    Toast.makeText(context, R.string.password_min_length, Toast.LENGTH_LONG).show()
                    loadingDialog?.cancel()
                } else {
                    // Key generation and password hashing is expensive-ish, so we don't want
                    // to do it on the UI thread (e.g., it would block the loading indicator).
                    lifecycleScope.launch(Dispatchers.IO) {
                        val keys = FmdKeyPair.generateNewFmdKeyPair(password)
                        settingsRepo.setKeys(keys)
                        val hashedPW = CypherUtils.hashPasswordForLogin(password)
                        settingsRepo.set(Settings.SET_FMD_CRYPT_HPW, hashedPW)
                        settingsRepo.set(Settings.SET_FMDSERVER_PASSWORD_SET, true)

                        fmdServerRepo.registerAccount(
                            username,
                            keys.encryptedPrivateKey,
                            keys.base64PublicKey,
                            hashedPW,
                            registrationToken,
                            this@AddAccountActivity::onRegisterOrLoginSuccess,
                            this@AddAccountActivity::onRegisterOrLoginError,
                        )
                    }
                }
            }
        showPrivacyPolicyThenDialog(context, registerDialog)
    }

    private fun onLoginClicked(view: View) {
        val context = view.context
        val loginLayout = layoutInflater.inflate(R.layout.dialog_login, null)

        val idInput = loginLayout.findViewById<EditText>(R.id.editTextFMDID)
        val passwordInput = loginLayout.findViewById<EditText>(R.id.editTextPassword)

        val loginDialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.Settings_FMDServer_Login))
            .setView(loginLayout)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.Ok)) { _, _ ->
                showLoadingIndicator(context)

                val id = idInput.text.toString()
                val password = passwordInput.text.toString()

                if (id.isNotEmpty() && password.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        fmdServerRepo.login(
                            id,
                            password,
                            this@AddAccountActivity::onRegisterOrLoginSuccess,
                            this@AddAccountActivity::onRegisterOrLoginError,
                        )
                    }
                } else {
                    Toast.makeText(
                        context, R.string.Settings_FMDServer_Error_id_or_pw_empty, Toast.LENGTH_LONG
                    ).show()
                    loadingDialog?.cancel()
                }
            }
        showPrivacyPolicyThenDialog(context, loginDialog)
    }

    private fun showPrivacyPolicyThenDialog(
        context: Context,
        dialogToShowAfterAccepting: AlertDialog.Builder
    ) {
        val webView = WebView(context)
        webView.clearCache(true) // make sure to load the latest policy

        @SuppressLint("SetJavaScriptEnabled")
        webView.settings.apply {
            // JS is needed because the website is a React app
            javaScriptEnabled = true
            // DOM Storage is needed for the "follow system theme" of the React app to work
            domStorageEnabled = true
        }

        val url = editTextServerUrl.text.toString().removeSuffix("/") + "/privacy?embedded=true"
        webView.loadUrl(url)

        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.Settings_FMDServer_Alert_PrivacyPolicy_Title))
            .setView(webView)
            .setPositiveButton(getString(R.string.accept)) { _, _ -> dialogToShowAfterAccepting.show() }
            .setNegativeButton(getString(R.string.cancel), null)
            .setCancelable(false)
            .show()
    }

    private fun showLoadingIndicator(context: Context) {
        val loadingLayout = layoutInflater.inflate(R.layout.dialog_loading, null)
        loadingDialog =
            MaterialAlertDialogBuilder(context).setView(loadingLayout).setCancelable(false).create()
        loadingDialog?.show()
    }

    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
        // unused
    }

    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
        // unused
    }

    override fun afterTextChanged(editable: Editable) {
        if (editable === editTextServerUrl.text) {
            val url = editTextServerUrl.text.toString().removeSuffix("/")
            settingsRepo.set(Settings.SET_FMDSERVER_URL, url)
            if (url.isEmpty()) {
                btnRegister.isEnabled = false
                btnLogin.isEnabled = false
            } else {
                btnRegister.isEnabled = true
                btnLogin.isEnabled = true
            }
            getAndShowServerVersionWithDelay(this, url)
        }
    }

    private fun onRegisterOrLoginSuccess(unit: Unit) {
        runOnUiThread {
            val context = applicationContext
            loadingDialog?.cancel()

            if (!settingsRepo.serverAccountExists()) {
                Toast.makeText(context, "Failed: no user id", Toast.LENGTH_LONG).show()
                return@runOnUiThread
            }

            settingsRepo.set(
                Settings.SET_FMD_SERVER_LAST_CONNECTIVITY_UNIX_TIME,
                System.currentTimeMillis()
            )

            ServerLocationUploadService.scheduleRecurring(context)
            ServerConnectivityCheckService.scheduleJob(context)
            ServerConnectivityCheckService.notifyAboutConnectivityCheck(context)

            val fmdServerActivityIntent = Intent(context, FMDServerActivity::class.java)
            startActivity(fmdServerActivityIntent)
            finish()
        }
    }

    private fun onRegisterOrLoginError(error: VolleyError) {
        runOnUiThread {
            loadingDialog?.cancel()
            error.printStackTrace()

            var message = ""
            if (error.networkResponse != null) {
                message = """
                ${getString(R.string.request_failed_status_code)}: ${error.networkResponse.statusCode}
                ${getString(R.string.request_failed_response_body)}: ${String(error.networkResponse.data)}
                """.trimIndent()
            }
            message += getString(R.string.request_failed_exception) + ": " + error.message

            if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                message = getString(R.string.server_registration_token_error)
            }

            val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(this)
            builder.setTitle(R.string.request_failed_title)
            builder.setMessage(message)
            builder.setNeutralButton(R.string.copy) { _, _ ->
                copyToClipboard(this, getString(R.string.request_failed_title), message)
            }
            builder.setPositiveButton(R.string.Ok) { dialog: DialogInterface, _ -> dialog.dismiss() }
            builder.show()
        }
    }

    private fun getAndShowServerVersionWithDelay(context: Context, serverBaseUrl: String) {
        val DELAY_MILLIS: Long = 1500
        this.lastTextChangedMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis

        // Only send the request to the URL if there has been no change within the last DELAY ms.
        // This prevents spamming the server with every keystroke.
        lifecycleScope.launch {
            delay(DELAY_MILLIS)
            val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
            if (now - lastTextChangedMillis > DELAY_MILLIS) {
                getAndShowServerVersion(context, serverBaseUrl)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getAndShowServerVersion(context: Context, serverBaseUrl: String) {
        if (serverBaseUrl.isEmpty()) {
            textViewServerVersion.text = ""
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            isMinRequiredVersion(context, serverBaseUrl) { result ->
                runOnUiThread {
                    when (result) {
                        is MinRequiredVersionResult.Success -> {
                            textViewServerVersion.text =
                                "${context.getString(R.string.server_version)}: ${result.actualVersion}"
                        }

                        is MinRequiredVersionResult.ServerOutdated -> {
                            var warningText =
                                context.getString(R.string.server_version_error_low_version)
                            warningText = warningText.replace(
                                "{MIN}", result.minRequiredVersion
                            )
                            warningText = warningText.replace("{CURRENT}", result.actualVersion)
                            textViewServerVersion.text = warningText
                        }

                        is MinRequiredVersionResult.Error -> {
                            textViewServerVersion.text =
                                "${context.getString(R.string.server_version_error)}: ${result.message}"
                        }
                    }
                }
            }
        }
    }
}
