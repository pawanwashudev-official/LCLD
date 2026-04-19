package de.nulide.findmydevice.ui;

import static de.nulide.findmydevice.net.ServerRequiredVersionCheckKt.isMinRequiredVersion;
import static de.nulide.findmydevice.ui.SetupWarningsActivityKt.shouldShowSetupWarnings;
import static de.nulide.findmydevice.ui.UiUtil.setupEdgeToEdgeAppBar;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;

import de.nulide.findmydevice.BuildConfig;
import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.net.MinRequiredVersionResult;
import de.nulide.findmydevice.receiver.PushReceiver;
import de.nulide.findmydevice.services.ServerCommandDownloadService;
import de.nulide.findmydevice.services.TempContactExpiredService;
import de.nulide.findmydevice.ui.home.CommandListFragment;
import de.nulide.findmydevice.ui.home.TransportListFragment;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.ui.settings.SettingsFragment;
import de.nulide.findmydevice.warnings.PushWarningsKt;
import kotlin.Unit;


public class MainActivity extends FmdActivity {

    private static final String KEY_ACTIVE_FRAGMENT_TAG = "activeFragmentTag";

    SettingsRepository settings;

    private TaggedFragment commandsFragment, transportFragment, settingsFragment;
    private TaggedFragment activeFragment;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // for some reason, getTag() returns null, so we need to use getStaticTag()
        outState.putString(KEY_ACTIVE_FRAGMENT_TAG, activeFragment.getStaticTag());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);

        // Make 3-button navigation bar transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));
        setupEdgeToEdgeAppBar(findViewById(R.id.fragment_container)); // shift the container down, too

        settings = SettingsRepository.Companion.getInstance(this);

        // Around the CrashedActivity it can happen that the two activities run in different processes.
        // In different processes, the SettingsRepository instance is different.
        // This can result in an endless "Continue to MainActivity" loop, because one repo sets the
        // flag to 0, but the other repo does not load the updated file.
        // To make sure we load the status correctly, reload from disk.
        settings.load();

        if (((Integer) settings.get(Settings.SET_APP_CRASHED_LOG_ENTRY)) == 1) {
            Intent intent = new Intent(this, CrashedActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        if (!(Boolean) settings.get(Settings.SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED)) {
            Intent intent = new Intent(this, UpdateboardingModernCryptoActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(navListener);

        commandsFragment = new CommandListFragment();
        transportFragment = new TransportListFragment();
        settingsFragment = new SettingsFragment();

        if (savedInstanceState == null) {
            activeFragment = commandsFragment;
        } else {
            String tag = savedInstanceState.getString(KEY_ACTIVE_FRAGMENT_TAG);
            if (tag == null || tag.equals(commandsFragment.getStaticTag())) {
                activeFragment = commandsFragment;
            } else if (tag.equals(transportFragment.getStaticTag())) {
                activeFragment = transportFragment;
            } else if (tag.equals(settingsFragment.getStaticTag())) {
                activeFragment = settingsFragment;
            }
        }

        if (settings.serverAccountExists()) {
            checkServerVersion();
            ServerCommandDownloadService.scheduleJobNow(this);

            // This must be cannot be in the FmdApplication because it needs an Activity context,
            // because it might show a dialog to choose between different distributors.
            if ((Boolean) settings.get(Settings.SET_FMDSERVER_ENABLE_PUSH)) { PushReceiver.registerWithUnifiedPush(this); }
        }
        if (PushWarningsKt.shouldWarnUnifiedPushRequired(this)) {
            PushWarningsKt.dialogWarnUnifiedPushRequired(this);
        }

        if (BuildConfig.FLAVOR == "edge" &&
                !(Boolean) settings.get(Settings.SET_FMD_EDGE_INFO_SHOWN)) {
            showFmdEdgeInfoDialog(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, activeFragment, activeFragment.getStaticTag())
                .commit();

        TempContactExpiredService.scheduleJob(this, 0);
        invalidateOptionsMenu();
    }

    private final NavigationBarView.OnItemSelectedListener navListener = (item) -> {
        switch (item.getItemId()) {
            case R.id.nav_commands: {
                activeFragment = commandsFragment;
                break;
            }
            case R.id.nav_transports: {
                activeFragment = transportFragment;
                break;
            }
            case R.id.nav_settings: {
                activeFragment = settingsFragment;
                break;
            }
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, activeFragment)
                .commit();
        return true;
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (shouldShowSetupWarnings(this)) {
            toolbar.inflateMenu(R.menu.main_app_bar_warnings);
        } else {
            toolbar.inflateMenu(R.menu.main_app_bar);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemSetupWarnings) {
            Intent intent = new Intent(this, SetupWarningsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkServerVersion() {
        isMinRequiredVersion(this, result -> {
            if (result instanceof MinRequiredVersionResult.ServerOutdated outdated) {
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
