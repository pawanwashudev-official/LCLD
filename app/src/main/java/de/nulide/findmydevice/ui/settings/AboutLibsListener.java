package de.nulide.findmydevice.ui.settings;

import android.view.View;

import androidx.annotation.NonNull;

import com.mikepenz.aboutlibraries.LibsConfiguration;
import com.mikepenz.aboutlibraries.entity.Library;
import com.mikepenz.aboutlibraries.util.SpecialButton;

import de.nulide.findmydevice.utils.Utils;

public class AboutLibsListener {
    public static final LibsConfiguration.LibsListener listener = new LibsConfiguration.LibsListener() {

        @Override
        public boolean onLibraryContentLongClicked(@NonNull View view, @NonNull Library library) {
            return false;
        }

        @Override
        public boolean onLibraryContentClicked(@NonNull View view, @NonNull Library library) {
            return false;
        }

        @Override
        public boolean onLibraryBottomLongClicked(@NonNull View view, @NonNull Library library) {
            return false;
        }

        @Override
        public boolean onLibraryBottomClicked(@NonNull View view, @NonNull Library library) {
            return false;
        }

        @Override
        public boolean onLibraryAuthorLongClicked(@NonNull View view, @NonNull Library library) {
            return false;
        }

        @Override
        public boolean onLibraryAuthorClicked(@NonNull View view, @NonNull Library library) {
            return false;
        }

        @Override
        public boolean onIconLongClicked(@NonNull View view) {
            return false;
        }

        @Override
        public void onIconClicked(@NonNull View view) {
            // unused
        }

        @Override
        public boolean onExtraClicked(@NonNull View view, @NonNull SpecialButton specialButton) {
            switch (specialButton) {
                case SPECIAL1:
                    Utils.openUrl(view.getContext(), "https://gitlab.com/fmd-foss/fmd-android");
                    return true;
                case SPECIAL2:
                    Utils.openUrl(view.getContext(), "https://fmd-foss.org/docs/overview");
                    return true;
                case SPECIAL3:
                    return false;
            }
            return false;
        }
    };
}
