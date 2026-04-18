package de.nulide.findmydevice.ui.helper;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.List;

import de.nulide.findmydevice.R;


public class SettingsEntry {
    String string;
    Drawable icon;

    SettingsEntry(Context context, @StringRes int stringId, @DrawableRes int iconId) {
        this.string = context.getString(stringId);
        this.icon = AppCompatResources.getDrawable(context, iconId);
    }

    public static List<SettingsEntry> getSettingsEntries(Context context) {
        List<SettingsEntry> entries = new ArrayList<>();
        entries.add(new SettingsEntry(context, R.string.Settings_FMDConfig, R.drawable.ic_settings));
        entries.add(new SettingsEntry(context, R.string.Settings_FMDServer, R.drawable.ic_cloud));
        entries.add(new SettingsEntry(context, R.string.Settings_WhiteList, R.drawable.ic_people));
        entries.add(new SettingsEntry(context, R.string.Settings_OpenCellId, R.drawable.ic_cell_tower));
        entries.add(new SettingsEntry(context, R.string.Settings_Appearance, R.drawable.ic_palette));
        entries.add(new SettingsEntry(context, R.string.Settings_Export, R.drawable.ic_import_export));
        entries.add(new SettingsEntry(context, R.string.Settings_Import, R.drawable.ic_import_export));
        entries.add(new SettingsEntry(context, R.string.Settings_Logs, R.drawable.ic_logs));
        entries.add(new SettingsEntry(context, R.string.Settings_About, R.drawable.ic_info));
        return entries;
    }
}
