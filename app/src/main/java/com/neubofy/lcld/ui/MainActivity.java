package com.neubofy.lcld.ui;

import static com.neubofy.lcld.net.ServerRequiredVersionCheckKt.isMinRequiredVersion;
import static com.neubofy.lcld.ui.SetupWarningsActivityKt.shouldShowSetupWarnings;
import static com.neubofy.lcld.ui.UiUtil.setupEdgeToEdgeAppBar;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.neubofy.lcld.BuildConfig;
import com.neubofy.lcld.R;
import com.neubofy.lcld.data.Settings;
import com.neubofy.lcld.data.SettingsRepository;
import com.neubofy.lcld.net.MinRequiredVersionResult;
import com.neubofy.lcld.receiver.PushReceiver;
import com.neubofy.lcld.services.ServerCommandDownloadService;
import com.neubofy.lcld.services.TempContactExpiredService;
import com.neubofy.lcld.ui.home.MainPageFragment;
import com.neubofy.lcld.ui.onboarding.UpdateboardingModernCryptoActivity;
import com.neubofy.lcld.ui.settings.SettingsActivity;
import com.neubofy.lcld.warnings.PushWarningsKt;
import kotlin.Unit;

public class MainActivity extends FmdActivity {

    SettingsRepository settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
        setSupportActionBar(toolbar);

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));

        settings = SettingsRepository.Companion.getInstance(this);
        settings.load();

        if (((Integer) settings.get(Settings.SET_APP_CRASHED_LOG_ENTRY)) == 1) {
            startActivity(new Intent(this, CrashedActivity.class));
            finish();
            return;
        }

        if (!(Boolean) settings.get(Settings.SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED)) {
            startActivity(new Intent(this, UpdateboardingModernCryptoActivity.class));
            finish();
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MainPageFragment())
                    .commit();
        }

        if (settings.serverAccountExists()) {
            checkServerVersion();
            ServerCommandDownloadService.scheduleJobNow(this);
            PushReceiver.registerWithUnifiedPush(this);
        }
        
        if (PushWarningsKt.shouldWarnUnifiedPushRequired(this)) {
            PushWarningsKt.dialogWarnUnifiedPushRequired(this);
        }

        if (BuildConfig.FLAVOR.equals("edge") && !(Boolean) settings.get(Settings.SET_FMD_EDGE_INFO_SHOWN)) {
            showFmdEdgeInfoDialog(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TempContactExpiredService.scheduleJob(this, 0);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemSetupWarnings) {
            startActivity(new Intent(this, SetupWarningsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menuItemSettings) {
            // Need to wrap SettingsFragment in an activity or handle it
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkServerVersion() {
        isMinRequiredVersion(this, result -> {
            if (result instanceof MinRequiredVersionResult.ServerOutdated) {
                MinRequiredVersionResult.ServerOutdated outdated = (MinRequiredVersionResult.ServerOutdated) result;
                String text = getString(R.string.server_version_upgrade_required_text)
                        .replace("{CURRENT}", outdated.getActualVersion())
                        .replace("{MIN}", outdated.getMinRequiredVersion());

                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.server_version_upgrade_required_title))
                        .setMessage(text)
                        .setPositiveButton(getString(R.string.Ok), null)
                        .setCancelable(false)
                        .show();
            }
            return Unit.INSTANCE;
        });
    }

    private void showFmdEdgeInfoDialog(Context context) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.app_name)
                .setMessage(R.string.fmd_edge_info_message)
                .setPositiveButton(getString(android.R.string.ok), null)
                .setNeutralButton(getString(R.string.dont_show_again), (dialog, whichButton) -> {
                    settings.set(Settings.SET_FMD_EDGE_INFO_SHOWN, true);
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
}
