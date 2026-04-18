package de.nulide.findmydevice.ui;

import static de.nulide.findmydevice.ui.UiUtil.setupEdgeToEdgeAppBar;
import static de.nulide.findmydevice.ui.UiUtil.setupEdgeToEdgeScrollView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.LogEntry;
import de.nulide.findmydevice.data.LogRepository;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.utils.Utils;

public class CrashedActivity extends FmdActivity {

    private String crashLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crashed);

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView));

        SettingsRepository settings = SettingsRepository.Companion.getInstance(this);
        settings.set(Settings.SET_APP_CRASHED_LOG_ENTRY, 0);

        LogRepository repo = LogRepository.Companion.getInstance(this);
        LogEntry entry = repo.getLastCrashLog();
        if (entry == null) {
            continueToMain();
            return;
        }
        crashLog = entry.getMsg();

        TextView textViewCrashLog = findViewById(R.id.textViewCrash);
        textViewCrashLog.setText(crashLog);

        Button buttonSendLog = findViewById(R.id.buttonSendLog);
        buttonSendLog.setOnClickListener(this::onSendLogClicked);

        Button buttonCopy = findViewById(R.id.buttonCopyLog);
        buttonCopy.setOnClickListener(this::onCopyClicked);

        Button buttonContinue = findViewById(R.id.buttonContinue);
        buttonContinue.setOnClickListener(this::onContinueClicked);
    }

    private void onSendLogClicked(View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://gitlab.com/fmd-foss/fmd-android/-/issues"));
        startActivity(intent);
        finish();
    }

    private void onCopyClicked(View v) {
        Utils.copyToClipboard(v.getContext(), "CrashLog", crashLog);
    }

    private void onContinueClicked(View v) {
        continueToMain();
    }

    private void continueToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
