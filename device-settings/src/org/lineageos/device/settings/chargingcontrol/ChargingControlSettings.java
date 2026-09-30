/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.device.settings.chargingcontrol;

import android.content.ContentResolver;
import android.provider.Settings;

/**
 * Charging control settings, shared by the settings UI and the controller running in
 * system_server. They live in Settings.Secure because LineageSettings is not available here.
 * Keys, modes and defaults follow LineageOS (lineage-sdk HealthInterface / config.xml).
 */
public final class ChargingControlSettings {
    public static final String KEY_ENABLED = "charging_control_enabled";
    public static final String KEY_MODE = "charging_control_mode";
    public static final String KEY_LIMIT = "charging_control_limit";
    public static final String KEY_START_TIME = "charging_control_start_time";
    public static final String KEY_TARGET_TIME = "charging_control_target_time";

    /** Charge to full right before the next alarm. */
    public static final int MODE_AUTO = 1;
    /** Charge to full by a user configured time. */
    public static final int MODE_MANUAL = 2;
    /** Stop charging at a user configured battery level. */
    public static final int MODE_LIMIT = 3;

    public static final boolean DEFAULT_ENABLED = false;
    public static final int DEFAULT_MODE = MODE_AUTO;
    public static final int DEFAULT_LIMIT = 80;
    /** Seconds of the day: 22:00. */
    public static final int DEFAULT_START_TIME = 79200;
    /** Seconds of the day: 06:00. */
    public static final int DEFAULT_TARGET_TIME = 21600;

    public static final int LIMIT_MIN = 70;
    public static final int LIMIT_MAX = 100;

    private ChargingControlSettings() {}

    public static boolean isEnabled(ContentResolver cr) {
        return Settings.Secure.getInt(cr, KEY_ENABLED, DEFAULT_ENABLED ? 1 : 0) != 0;
    }

    public static int getMode(ContentResolver cr) {
        final int mode = Settings.Secure.getInt(cr, KEY_MODE, DEFAULT_MODE);
        return mode >= MODE_AUTO && mode <= MODE_LIMIT ? mode : DEFAULT_MODE;
    }

    public static int getLimit(ContentResolver cr) {
        final int limit = Settings.Secure.getInt(cr, KEY_LIMIT, DEFAULT_LIMIT);
        return Math.max(LIMIT_MIN, Math.min(LIMIT_MAX, limit));
    }

    public static int getStartTime(ContentResolver cr) {
        return clampTime(Settings.Secure.getInt(cr, KEY_START_TIME, DEFAULT_START_TIME),
                DEFAULT_START_TIME);
    }

    public static int getTargetTime(ContentResolver cr) {
        return clampTime(Settings.Secure.getInt(cr, KEY_TARGET_TIME, DEFAULT_TARGET_TIME),
                DEFAULT_TARGET_TIME);
    }

    private static int clampTime(int time, int def) {
        return time >= 0 && time < 24 * 60 * 60 ? time : def;
    }
}
