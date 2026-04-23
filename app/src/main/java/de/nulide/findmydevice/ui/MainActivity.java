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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.nulide.findmydevice.BuildConfig;
import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.net.MinRequiredVersionResult;
import de.nulide.findmydevice.receiver.PushReceiver;
import de.nulide.findmydevice.services.ServerCommandDownloadService;
import de.nulide.findmydevice.services.TempContactExpiredService;
import de.nulide.findmydevice.ui.home.TransportListFragment;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.ui.settings.SettingsActivity;
import de.nulide.findmydevice.warnings.PushWarningsKt;
import kotlin.Unit;

public class MainActivity extends FmdActivity {

    SettingsRepository settings;

    private TaggedFragment homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));
        setupEdgeToEdgeAppBar(findViewById(R.id.fragment_container));

        settings = SettingsRepository.Companion.getInstance(this);
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

        homeFragment = new TransportListFragment();

        if (settings.serverAccountExists()) {
            checkServerVersion();
            ServerCommandDownloadService.scheduleJobNow(this);
            PushReceiver.registerWithUnifiedPush(this);
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
                .replace(R.id.fragment_container, homeFragment, homeFragment.getStaticTag())
                .commit();

        TempContactExpiredService.scheduleJob(this, 0);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.main_app_bar);
        if (shouldShowSetupWarnings(this)) {
            toolbar.getMenu().add(Menu.NONE, R.id.menuItemSetupWarnings, 0, R.string.setup_warnings_title)
                    .setIcon(R.drawable.ic_warning)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemSetupWarnings) {
            startActivity(new Intent(this, SetupWarningsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menuItemSettings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menuItemAbout) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
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
