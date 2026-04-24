package com.neubofy.lcld.ui.settings;

import static com.neubofy.lcld.ui.UiUtil.setupEdgeToEdgeAppBar;
import static com.neubofy.lcld.ui.UiUtil.setupEdgeToEdgeScrollView;
import static com.neubofy.lcld.utils.CypherUtils.MIN_PASSWORD_LENGTH;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.security.KeyPair;

import com.neubofy.lcld.R;
import com.neubofy.lcld.data.BackgroundLocationType;
import com.neubofy.lcld.data.EncryptedSettingsRepository;
import com.neubofy.lcld.data.Settings;
import com.neubofy.lcld.data.SettingsRepository;
import com.neubofy.lcld.net.FMDServerApiRepoSpec;
import com.neubofy.lcld.net.FMDServerApiRepository;
import com.neubofy.lcld.services.FmdBatteryLowService;
import com.neubofy.lcld.services.ServerCommandDownloadService;
import com.neubofy.lcld.services.ServerConnectivityCheckService;
import com.neubofy.lcld.services.ServerLocationUploadService;
import com.neubofy.lcld.ui.FmdActivity;
import com.neubofy.lcld.utils.CypherUtils;
import com.neubofy.lcld.utils.UnregisterUtil;
import com.neubofy.lcld.utils.Utils;

public class FMDServerActivity extends FmdActivity implements CompoundButton.OnCheckedChangeListener, TextWatcher {

    private SettingsRepository settings;
    private FMDServerApiRepository fmdServerRepo;

    private EditText editTextCheckInterval;
    private EditText editTextNotifyAfterTime;
    private EditText editTextFMDServerUpdateTime;

    private CheckBox checkBoxFMDServerGPS;

    private CheckBox checkBoxLowBat;

    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_f_m_d_server);

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView));

        settings = SettingsRepository.Companion.getInstance(this);
        fmdServerRepo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(this));

        TextView textViewServerUrl = findViewById(R.id.textViewServerUrl);
        TextView textViewUserId = findViewById(R.id.textViewUserId);
        textViewServerUrl.setText((String) settings.get(Settings.SET_FMDSERVER_URL));
        textViewUserId.setText((String) settings.get(Settings.SET_FMDSERVER_ID));

        findViewById(R.id.buttonOpenWebClient).setOnClickListener(this::onOpenWebClientClicked);
        findViewById(R.id.buttonCopyServerUrl).setOnClickListener(this::onCopyServerUrlClicked);
        findViewById(R.id.buttonCopyUserId).setOnClickListener(this::onCopyUserIdClicked);

        findViewById(R.id.buttonChangePassword).setOnClickListener(this::onChangePasswordClicked);
        findViewById(R.id.buttonLogout).setOnClickListener(this::onLogoutClicked);
        findViewById(R.id.buttonDeleteData).setOnClickListener(this::onDeleteClicked);

        editTextCheckInterval = findViewById(R.id.editTextCheckInterval);
        editTextCheckInterval.setText(settings.get(Settings.SET_FMD_SERVER_CONNECTIVITY_CHECK_INTERVAL_HOURS).toString());
        editTextCheckInterval.addTextChangedListener(this);

        editTextNotifyAfterTime = findViewById(R.id.editTextNotifyAfterTime);
        editTextNotifyAfterTime.setText(settings.get(Settings.SET_FMD_SERVER_CONNECTIVITY_CHECK_NOTIFY_AFTER_HOURS).toString());
        editTextNotifyAfterTime.addTextChangedListener(this);

        editTextFMDServerUpdateTime = findViewById(R.id.editTextFMDServerUpdateTime);
        editTextFMDServerUpdateTime.setText(settings.get(Settings.SET_FMDSERVER_UPDATE_TIME).toString());
        editTextFMDServerUpdateTime.addTextChangedListener(this);

        checkBoxFMDServerGPS = findViewById(R.id.checkBoxFMDServerGPS);

        int locTypeInt = (int) settings.get(Settings.SET_FMDSERVER_LOCATION_TYPE);
        BackgroundLocationType locType = new BackgroundLocationType(locTypeInt);

        checkBoxFMDServerGPS.setChecked(locType.getGps());
        checkBoxFMDServerGPS.setOnCheckedChangeListener(this);

        checkBoxLowBat = findViewById(R.id.checkBoxFMDServerLowBatUpload);
        checkBoxLowBat.setChecked((Boolean) settings.get(Settings.SET_FMD_LOW_BAT_SEND));
        checkBoxLowBat.setOnCheckedChangeListener(this);
        if ((Boolean) settings.get(Settings.SET_FMD_LOW_BAT_SEND)) {
            FmdBatteryLowService.scheduleJobNow(this);
        }

        getServerVersion();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkConnection();
        ServerCommandDownloadService.scheduleJobNow(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == checkBoxFMDServerGPS) {

            BackgroundLocationType locType = BackgroundLocationType.Companion.fromEmpty();
            locType.setGps(checkBoxFMDServerGPS.isChecked());
            settings.set(Settings.SET_FMDSERVER_LOCATION_TYPE, locType.encode());

            if (!locType.isEmpty()) {
                ServerLocationUploadService.scheduleRecurring(this);
            } else {
                ServerLocationUploadService.cancelJob(this);
            }
        } else if (buttonView == checkBoxLowBat) {
            settings.set(Settings.SET_FMD_LOW_BAT_SEND, isChecked);
            if (isChecked) {
                FmdBatteryLowService.scheduleJobNow(this);
            } else {
                FmdBatteryLowService.cancelJob(this);
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // unused
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // unused
    }

    @Override
    public void afterTextChanged(Editable edited) {
        String string = edited.toString();
        if (string.isEmpty()) {
            return;
        }

        long value;
        try {
            value = Long.parseLong(string);
        } catch (NumberFormatException e) {
            return;
        }

        if (edited == editTextCheckInterval.getText()) {
            settings.set(Settings.SET_FMD_SERVER_CONNECTIVITY_CHECK_INTERVAL_HOURS, value);

            if (value > 0) {
                ServerConnectivityCheckService.scheduleJob(this);
            } else {
                ServerConnectivityCheckService.cancelJob(this);
            }
        } else if (edited == editTextNotifyAfterTime.getText()) {
            settings.set(Settings.SET_FMD_SERVER_CONNECTIVITY_CHECK_NOTIFY_AFTER_HOURS, value);
        } else if (edited == editTextFMDServerUpdateTime.getText()) {
            int interval = (int) value;
            settings.set(Settings.SET_FMDSERVER_UPDATE_TIME, interval);

            // Reschedule with new interval
            if (checkBoxFMDServerGPS.isChecked()) {
                ServerLocationUploadService.scheduleJob(this, interval);
            }
        }
    }

    private void onOpenWebClientClicked(View view) {
        String url = (String) settings.get(Settings.SET_FMDSERVER_URL);
        Utils.openUrl(this, url);
    }

    private void onCopyServerUrlClicked(View view) {
        String label = getString(R.string.Settings_LCLD_Server_Server_URL).replace(":", "");
        String text = (String) settings.get(Settings.SET_FMDSERVER_URL);
        Utils.copyToClipboard(this, label, text);
    }

    private void onCopyUserIdClicked(View view) {
        String label = getString(R.string.Settings_LCLD_Server_User_ID).replace(":", "");
        String text = (String) settings.get(Settings.SET_FMDSERVER_ID);
        Utils.copyToClipboard(this, label, text);
    }

    private void onDeleteClicked(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.Settings_LCLDServer_Delete_Account))
                .setMessage(R.string.Settings_LCLDServer_Alert_DeleteData_Desc)
                .setPositiveButton(getString(R.string.Ok), (dialog, whichButton) -> runDelete())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void onLogoutClicked(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.Settings_LCLDServer_Logout_Button))
                .setMessage(R.string.Settings_LCLDServer_Logout_Text)
                .setPositiveButton(getString(R.string.Ok), (dialog, whichButton) -> {
                    settings.removeServerAccount();
                    EncryptedSettingsRepository encryptedSettingsRepo = EncryptedSettingsRepository.Companion.getInstance(this);
                    encryptedSettingsRepo.setCachedAccessToken("");
                    ServerLocationUploadService.cancelJob(this);
                    ServerConnectivityCheckService.cancelJob(this);
                    finish();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void onChangePasswordClicked(View view) {
        LayoutInflater inflater = getLayoutInflater();
        final AlertDialog.Builder alert = new MaterialAlertDialogBuilder(this);
        alert.setTitle(getString(R.string.Settings_LCLDServer_Change_Password_Button));
        View registerLayout = inflater.inflate(R.layout.dialog_password_change, null);
        alert.setView(registerLayout);
        EditText oldPasswordInput = registerLayout.findViewById(R.id.editTextFMDOldPassword);
        EditText passwordInput = registerLayout.findViewById(R.id.editTextPassword);
        EditText passwordInputCheck = registerLayout.findViewById(R.id.editTextFMDPasswordCheck);
        alert.setView(registerLayout);

        alert.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String oldPassword = oldPasswordInput.getText().toString();
                String password = passwordInput.getText().toString();
                String passwordCheck = passwordInputCheck.getText().toString();

                if (password.isEmpty() || oldPassword.isEmpty()) {
                    Toast.makeText(view.getContext(), R.string.pw_change_empty, Toast.LENGTH_LONG).show();
                } else if (!password.equals(passwordCheck)) {
                    Toast.makeText(view.getContext(), R.string.pw_change_mismatch, Toast.LENGTH_LONG).show();
                } else if (password.length() < MIN_PASSWORD_LENGTH) {
                    Toast.makeText(view.getContext(), R.string.password_min_length, Toast.LENGTH_LONG).show();
                } else {
                    runChangePassword(oldPassword, password);
                }
            }
        });
        alert.show();
    }

    private void showLoadingIndicator(Context context) {
        View loadingLayout = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        loadingDialog = new MaterialAlertDialogBuilder(context).setView(loadingLayout).setCancelable(false).create();
        loadingDialog.show();
    }

    private void runChangePassword(String oldPassword, String password) {
        showLoadingIndicator(this);
        new Thread(() -> {
            try {
                KeyPair keyPair = CypherUtils.decryptPrivateKeyWithPassword((String) settings.get(Settings.SET_FMD_CRYPT_PRIVKEY), oldPassword);
                if (keyPair == null) {
                    Toast.makeText(this, R.string.pw_change_wrong_password, Toast.LENGTH_LONG).show();
                    loadingDialog.cancel();
                    return;
                }
                String newPrivKey = CypherUtils.encryptPrivateKeyWithPassword(keyPair.getPrivate(), password);
                String hashedPW = CypherUtils.hashPasswordForLogin(password);

                runOnUiThread(() -> {
                    fmdServerRepo.changePassword(hashedPW, newPrivKey,
                            (response -> {
                                loadingDialog.cancel();
                                Toast.makeText(this, R.string.pw_change_success, Toast.LENGTH_LONG).show();
                            }),
                            (error) -> {
                                Toast.makeText(this, R.string.pw_change_network_failed, Toast.LENGTH_LONG).show();
                                loadingDialog.cancel();
                            });
                });
            } catch (Exception bdp) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.pw_change_wrong_password, Toast.LENGTH_LONG).show();
                    loadingDialog.cancel();
                });
            }
        }).start();
    }

    private void runDelete() {
        Context context = this;
        showLoadingIndicator(context);
        ServerLocationUploadService.cancelJob(context);
        ServerConnectivityCheckService.cancelJob(context);
        fmdServerRepo.unregister(
                response -> {
                    loadingDialog.cancel();
                    Toast.makeText(context, R.string.Settings_LCLDServer_Unregister_Success, Toast.LENGTH_LONG).show();
                    finish();
                }, error -> {
                    loadingDialog.cancel();
                    UnregisterUtil.showUnregisterFailedDialog(context, error, this::finish);
                }
        );
    }

    private void checkConnection() {
        TextView textViewConnectionStatus = findViewById(R.id.textViewConnectionStatus);

        fmdServerRepo.checkConnection(
                response -> {
                    settings.set(
                            Settings.SET_FMD_SERVER_LAST_CONNECTIVITY_UNIX_TIME,
                            System.currentTimeMillis()
                    );

                    textViewConnectionStatus.setText(R.string.Settings_LCLD_Server_Connection_Status_Success);
                    textViewConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.md_theme_primary));
                    textViewConnectionStatus.setOnClickListener(v -> {
                    });
                },
                error -> {
                    textViewConnectionStatus.setText(error.toString());
                    textViewConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.md_theme_error));
                    textViewConnectionStatus.setOnClickListener(v -> {
                        Utils.copyToClipboard(this, "", error.toString());
                    });
                }
        );
    }

    @SuppressLint("SetTextI18n")
    private void getServerVersion() {
        TextView serverVersion = findViewById(R.id.serverVersion);

        String baseUrl = (String) settings.get(Settings.SET_FMDSERVER_URL);
        fmdServerRepo.getServerVersion(baseUrl, response -> {
            serverVersion.setText(getString(R.string.server_version) + ": " + response);
            serverVersion.setVisibility(View.VISIBLE);
        }, error -> {
            // Silently ignore
            serverVersion.setVisibility(View.GONE);
        });
    }
}
