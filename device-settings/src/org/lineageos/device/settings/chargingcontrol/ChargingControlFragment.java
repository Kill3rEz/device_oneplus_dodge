/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.device.settings.chargingcontrol;

import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.lineageos.device.settings.R;
import org.lineageos.device.settings.preferences.CustomSeekBarPreference;

import java.time.LocalTime;

/**
 * Settings > Battery > Charging control. Writes {@link ChargingControlSettings}; the controller
 * in system_server observes them and drives the vendor.lineage.health HAL.
 */
public class ChargingControlFragment extends PreferenceFragmentCompat {
    private static final String KEY_START_TIME = "charging_control_start_time";
    private static final String KEY_TARGET_TIME = "charging_control_target_time";
    private static final String KEY_FOOTER = "charging_control_footer";

    private ContentResolver mResolver;
    private SwitchPreferenceCompat mEnabled;
    private ListPreference mMode;
    private Preference mStartTime;
    private Preference mTargetTime;
    private CustomSeekBarPreference mLimit;
    private Preference mFooter;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.charging_control_settings, rootKey);
        mResolver = getContext().getContentResolver();

        mEnabled = findPreference(ChargingControlSettings.KEY_ENABLED);
        mMode = findPreference(ChargingControlSettings.KEY_MODE);
        mStartTime = findPreference(KEY_START_TIME);
        mTargetTime = findPreference(KEY_TARGET_TIME);
        mLimit = findPreference(ChargingControlSettings.KEY_LIMIT);
        mFooter = findPreference(KEY_FOOTER);

        mEnabled.setChecked(ChargingControlSettings.isEnabled(mResolver));
        mEnabled.setOnPreferenceChangeListener((pref, value) -> {
            putInt(ChargingControlSettings.KEY_ENABLED, (Boolean) value ? 1 : 0);
            updateState((Boolean) value, ChargingControlSettings.getMode(mResolver));
            return true;
        });

        mMode.setValue(String.valueOf(ChargingControlSettings.getMode(mResolver)));
        mMode.setOnPreferenceChangeListener((pref, value) -> {
            int mode = Integer.parseInt((String) value);
            putInt(ChargingControlSettings.KEY_MODE, mode);
            updateState(mEnabled.isChecked(), mode);
            return true;
        });

        mStartTime.setOnPreferenceClickListener(pref -> {
            pickTime(ChargingControlSettings.KEY_START_TIME,
                    ChargingControlSettings.getStartTime(mResolver));
            return true;
        });
        mTargetTime.setOnPreferenceClickListener(pref -> {
            pickTime(ChargingControlSettings.KEY_TARGET_TIME,
                    ChargingControlSettings.getTargetTime(mResolver));
            return true;
        });

        // min/max come from the XML: CustomSeekBarPreference.setMin/setMax write raw values to
        // a SeekBar that works in (value - min) units, which pins the thumb.
        mLimit.setValue(ChargingControlSettings.getLimit(mResolver));
        mLimit.setOnPreferenceChangeListener((pref, value) -> {
            putInt(ChargingControlSettings.KEY_LIMIT, (int) value);
            return true;
        });

        updateState(mEnabled.isChecked(), ChargingControlSettings.getMode(mResolver));
    }

    private void updateState(boolean enabled, int mode) {
        final boolean manual = mode == ChargingControlSettings.MODE_MANUAL;
        final boolean limit = mode == ChargingControlSettings.MODE_LIMIT;
        mMode.setEnabled(enabled);
        mStartTime.setVisible(manual);
        mTargetTime.setVisible(manual);
        mStartTime.setEnabled(enabled);
        mTargetTime.setEnabled(enabled);
        mLimit.setVisible(limit);
        mLimit.setEnabled(enabled);
        mStartTime.setSummary(formatTime(ChargingControlSettings.getStartTime(mResolver)));
        mTargetTime.setSummary(formatTime(ChargingControlSettings.getTargetTime(mResolver)));
        switch (mode) {
            case ChargingControlSettings.MODE_AUTO:
                mFooter.setSummary(R.string.charging_control_mode_auto_summary);
                break;
            case ChargingControlSettings.MODE_MANUAL:
                mFooter.setSummary(R.string.charging_control_mode_custom_summary);
                break;
            default:
                mFooter.setSummary(R.string.charging_control_mode_limit_summary);
                break;
        }
    }

    private void pickTime(String key, int secondOfDay) {
        LocalTime time = LocalTime.ofSecondOfDay(secondOfDay);
        new TimePickerDialog(getContext(), (view, hour, minute) -> {
            putInt(key, hour * 3600 + minute * 60);
            updateState(mEnabled.isChecked(), ChargingControlSettings.getMode(mResolver));
        }, time.getHour(), time.getMinute(), DateFormat.is24HourFormat(getContext())).show();
    }

    private String formatTime(int secondOfDay) {
        LocalTime time = LocalTime.ofSecondOfDay(secondOfDay);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, time.getHour());
        cal.set(java.util.Calendar.MINUTE, time.getMinute());
        return DateFormat.getTimeFormat(getContext()).format(cal.getTime());
    }

    private void putInt(String key, int value) {
        Settings.Secure.putInt(mResolver, key, value);
    }
}
