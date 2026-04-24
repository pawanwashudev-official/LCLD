package com.neubofy.lcld.receiver;

import android.content.Context;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.unifiedpush.android.connector.ConstantsKt;
import org.unifiedpush.android.connector.MessagingReceiver;
import org.unifiedpush.android.connector.RegistrationDialogContent;
import org.unifiedpush.android.connector.UnifiedPush;

import java.util.ArrayList;

import com.neubofy.lcld.data.Settings;
import com.neubofy.lcld.data.SettingsRepository;
import com.neubofy.lcld.net.FMDServerApiRepoSpec;
import com.neubofy.lcld.net.FMDServerApiRepository;
import com.neubofy.lcld.services.ServerCommandDownloadService;
import com.neubofy.lcld.utils.FmdLogKt;


public class PushReceiver extends MessagingReceiver {

    private final String TAG = PushReceiver.class.getSimpleName();

    public PushReceiver() {
        super();
    }

    @Override
    public void onMessage(@NonNull Context context, @NonNull byte[] message, @NonNull String instance) {
        FmdLogKt.log(context).i(TAG, "Received push message");
        ServerCommandDownloadService.scheduleJobNow(context);
    }

    @Override
    public void onNewEndpoint(@NonNull Context context, @NotNull String endpoint, @NotNull String instance) {
        SettingsRepository settings = SettingsRepository.Companion.getInstance(context);
        settings.set(Settings.SET_FMDSERVER_PUSH_URL, endpoint);

        FMDServerApiRepository repo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(context));
        repo.registerPushEndpoint(endpoint, (error) -> {
            error.printStackTrace();
        });
    }

    @Override
    public void onRegistrationFailed(@NonNull Context context, @NotNull String s) {
        // do nothing
    }

    @Override
    public void onUnregistered(@NonNull Context context, @NotNull String s) {
        SettingsRepository settings = SettingsRepository.Companion.getInstance(context);
        settings.set(Settings.SET_FMDSERVER_PUSH_URL, "");

        // Either we have triggered the push deregistration (after server account deletion),
        // or someone else (e.g., the distributor itself) has triggered the deregistration.
        // In the latter case, inform the server.
        if (settings.serverAccountExists()) {
            FMDServerApiRepository repo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(context));
            repo.registerPushEndpoint("", (error) -> {
                error.printStackTrace();
            });
        }
    }

    public static void registerWithUnifiedPush(Context context) {
        if (isUnifiedPushAvailable(context)) {
            UnifiedPush.registerAppWithDialog(context, ConstantsKt.INSTANCE_DEFAULT, new RegistrationDialogContent(), new ArrayList<>(), "");
        }
    }

    public static void unregisterWithUnifiedPush(Context context) {
        if (isRegisteredWithUnifiedPush(context)) {
            UnifiedPush.unregisterApp(context, ConstantsKt.INSTANCE_DEFAULT);
        }
        // ensure that the state is cleared
        new PushReceiver().onUnregistered(context, "");
    }

    public static boolean isRegisteredWithUnifiedPush(Context context) {
        return !UnifiedPush.getDistributor(context).isEmpty();
    }

    public static boolean isUnifiedPushAvailable(Context context) {
        return UnifiedPush.getDistributors(context, new ArrayList<>()).size() > 0;
    }
}
