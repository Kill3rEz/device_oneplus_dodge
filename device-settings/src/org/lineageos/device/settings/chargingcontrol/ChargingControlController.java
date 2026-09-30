/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.device.settings.chargingcontrol;

import static org.lineageos.device.settings.chargingcontrol.ChargingControlSettings.MODE_AUTO;
import static org.lineageos.device.settings.chargingcontrol.ChargingControlSettings.MODE_LIMIT;
import static org.lineageos.device.settings.chargingcontrol.ChargingControlSettings.MODE_MANUAL;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;

import org.lineageos.device.settings.R;

import vendor.lineage.health.ChargingControlSupportedMode;
import vendor.lineage.health.IChargingControl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Port of the LineageOS ChargingControlController (lineage-sdk) with its Toggle provider, which is
 * what the vendor.lineage.health HAL of this device supports. PenguinOS does not run the Lineage
 * platform services, so this runs in system_server from the device KeyHandler, on its own thread.
 * Settings come from {@link ChargingControlSettings} (Settings.Secure) instead of LineageSettings.
 */
public final class ChargingControlController {
    private static final String TAG = "ChargingControl";

    private static final String SETTINGS_PACKAGE = "org.lineageos.device.settings";
    private static final String ACTION_CANCEL_ONCE =
            "org.lineageos.device.settings.chargingcontrol.CANCEL_ONCE";
    private static final String CHANNEL_ID = "charging_control";
    private static final int NOTIFICATION_ID = 1000;

    // LineageOS defaults: config_chargingControlBatteryRechargeMargin / TimeMargin
    private static final int RECHARGE_MARGIN = 10;
    private static final long TIME_MARGIN_MS = 30 * DateUtils.MINUTE_IN_MILLIS;
    // Only above this level does the time based control start waiting.
    private static final int CHARGE_CTRL_MIN_LEVEL = 80;

    private enum Stage { NONE, INITIAL, WAITING, CONTINUE }

    private static ChargingControlController sInstance;

    private final Context mContext;
    private final ContentResolver mResolver;
    private final Handler mHandler;
    private IChargingControl mChargingControl;
    private Context mResContext;

    // Controller state
    private boolean mIsEnabled;
    private boolean mIsPowerConnected;
    private boolean mIsControlCancelledOnce;
    private float mBatteryPct;
    private BroadcastReceiver mBattReceiver;
    private BroadcastReceiver mAlarmReceiver;
    private BroadcastReceiver mDisconnectOnceReceiver;

    // Toggle provider state
    private boolean mBypassSupported;
    private int mLimitMargin;
    private boolean mProviderEnabled;
    private boolean mIsLimitSet;
    private long mSavedTargetTime;
    private long mEstimatedFullTime;
    private Stage mStage = Stage.NONE;

    // Notification state
    private boolean mNotificationPosted;

    /**
     * Starts the controller once; safe to call from system_server early in boot. The KeyHandler
     * is created long before the notification and battery stats services are up, so the real
     * start waits for LOCKED_BOOT_COMPLETED, like LineageOS starts its health features at
     * PHASE_BOOT_COMPLETED.
     */
    public static synchronized void start(Context context) {
        if (sInstance != null) {
            return;
        }
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        final ChargingControlController controller =
                new ChargingControlController(context, new Handler(thread.getLooper()));
        sInstance = controller;
        if ("1".equals(SystemProperties.get("sys.boot_completed"))) {
            controller.mHandler.post(controller::init);
            return;
        }
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                ctx.unregisterReceiver(this);
                safely(controller::init);
            }
        }, new IntentFilter(Intent.ACTION_LOCKED_BOOT_COMPLETED), null, controller.mHandler,
                Context.RECEIVER_EXPORTED);
    }

    private ChargingControlController(Context context, Handler handler) {
        mContext = context;
        mResolver = context.getContentResolver();
        mHandler = handler;
    }

    private void init() {
        try {
            if (!ServiceManager.isDeclared(IChargingControl.DESCRIPTOR + "/default")) {
                Log.i(TAG, "Lineage health HAL not declared, charging control disabled");
                return;
            }
            IBinder binder = ServiceManager.waitForDeclaredService(
                    IChargingControl.DESCRIPTOR + "/default");
            mChargingControl = IChargingControl.Stub.asInterface(binder);
            if (mChargingControl == null
                    || !isHalModeSupported(ChargingControlSupportedMode.TOGGLE)) {
                Log.i(TAG, "Charging control toggle not supported by the HAL");
                mChargingControl = null;
                return;
            }
            mBypassSupported = isHalModeSupported(ChargingControlSupportedMode.BYPASS);
            mLimitMargin = mBypassSupported ? 1 : RECHARGE_MARGIN;
            mResContext = mContext.createPackageContext(SETTINGS_PACKAGE, 0);
            createNotificationChannel();

            ContentObserver observer = new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    safely(ChargingControlController.this::handleSettingChange);
                }
            };
            for (String key : new String[] {ChargingControlSettings.KEY_ENABLED,
                    ChargingControlSettings.KEY_MODE, ChargingControlSettings.KEY_LIMIT,
                    ChargingControlSettings.KEY_START_TIME,
                    ChargingControlSettings.KEY_TARGET_TIME}) {
                mResolver.registerContentObserver(Settings.Secure.getUriFor(key), false,
                        observer);
            }

            IntentFilter powerFilter = new IntentFilter();
            powerFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            powerFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    safely(() -> onPowerStatus(
                            Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())));
                }
            }, powerFilter, null, mHandler, Context.RECEIVER_EXPORTED);

            mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i(TAG, "Charge to full requested from the notification");
                    safely(ChargingControlController.this::setChargingCancelledOnce);
                }
            }, new IntentFilter(ACTION_CANCEL_ONCE), null, mHandler,
                    Context.RECEIVER_NOT_EXPORTED);

            Log.i(TAG, "Started, bypass supported: " + mBypassSupported);
            handleSettingChange();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start charging control", t);
        }
    }

    /** Runs on the controller thread inside system_server: never let an exception escape. */
    private static void safely(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            Log.e(TAG, "Charging control error", t);
        }
    }

    private boolean isHalModeSupported(int mode) {
        try {
            return (mChargingControl.getSupportedMode() & mode) == mode;
        } catch (Exception e) {
            Log.e(TAG, "Unable to get supported mode from HAL", e);
            return false;
        }
    }

    private boolean isEnabled() {
        return ChargingControlSettings.isEnabled(mResolver);
    }

    private int getMode() {
        return ChargingControlSettings.getMode(mResolver);
    }

    // ---------------------------------------------------------------- controller

    private void handleSettingChange() {
        if (mIsEnabled != isEnabled()) {
            mIsEnabled = isEnabled();
            if (mIsEnabled) {
                registerBatteryReceiver();
                Log.i(TAG, "Enabled charging control, start monitoring battery");
            } else {
                unregisterBatteryReceiver();
                Log.i(TAG, "Disabled charging control, stop monitoring battery");
            }
        }
        resetInternalState();
        updateBatteryInfo();
        updateChargeControl();
    }

    private void registerBatteryReceiver() {
        unregisterBatteryReceiver();
        mBattReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                safely(() -> {
                    updateBatteryInfo(intent);
                    updateChargeControl();
                });
            }
        };
        mContext.registerReceiver(mBattReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                null, mHandler, Context.RECEIVER_EXPORTED);
    }

    private void unregisterBatteryReceiver() {
        if (mBattReceiver != null) {
            mContext.unregisterReceiver(mBattReceiver);
            mBattReceiver = null;
        }
    }

    private void onPowerStatus(boolean connected) {
        if (!isEnabled()) {
            return;
        }
        if (connected) {
            registerBatteryReceiver();
            updateChargeControl();
        } else {
            unregisterBatteryReceiver();
            resetInternalState();
        }
    }

    private void updateBatteryInfo(Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            mIsControlCancelledOnce = false;
        }
        if (!mBypassSupported) {
            // Charging is cut at the source: the charger looks disconnected, keep monitoring.
            mIsPowerConnected = true;
        } else {
            mIsPowerConnected = plugged != 0
                    || (status != BatteryManager.BATTERY_STATUS_DISCHARGING
                    && status != BatteryManager.BATTERY_STATUS_UNKNOWN);
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level == -1 || scale == -1) {
            return;
        }
        mBatteryPct = level * 100 / (float) scale;
    }

    private void updateBatteryInfo() {
        Intent status = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_EXPORTED);
        if (status != null) {
            updateBatteryInfo(status);
        }
    }

    private void resetInternalState() {
        mIsControlCancelledOnce = false;
        cancelNotification();
        providerReset();
    }

    private void setChargingCancelledOnce() {
        mIsControlCancelledOnce = true;
        if (!mBypassSupported && mDisconnectOnceReceiver == null) {
            // Reset the one-shot "charge to full" when the charger is unplugged.
            mDisconnectOnceReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i(TAG, "Power disconnected, reset internal states");
                    mContext.unregisterReceiver(this);
                    mDisconnectOnceReceiver = null;
                    safely(ChargingControlController.this::resetInternalState);
                }
            };
            mContext.registerReceiver(mDisconnectOnceReceiver,
                    new IntentFilter(Intent.ACTION_POWER_DISCONNECTED), null, mHandler,
                    Context.RECEIVER_EXPORTED);
        }
        providerDisable();
        cancelNotification();
    }

    private long[] getChargeTime() {
        final long now = System.currentTimeMillis();
        long startTime;
        long targetTime;
        final int mode = getMode();
        if (mode == MODE_AUTO) {
            AlarmManager am = mContext.getSystemService(AlarmManager.class);
            AlarmManager.AlarmClockInfo alarm = am != null ? am.getNextAlarmClock() : null;
            if (alarm == null) {
                Log.w(TAG, "No alarm found, auto charging control has no effect");
                cancelNotification();
                return null;
            }
            targetTime = alarm.getTriggerTime();
            // Start time is 9 hours before the alarm
            startTime = targetTime - DateUtils.HOUR_IN_MILLIS * 9;
        } else if (mode == MODE_MANUAL) {
            startTime = millisFromSecondOfDay(ChargingControlSettings.getStartTime(mResolver));
            targetTime = millisFromSecondOfDay(ChargingControlSettings.getTargetTime(mResolver));
            if (startTime > targetTime) {
                if (now > targetTime) {
                    targetTime += DateUtils.DAY_IN_MILLIS;
                } else {
                    startTime -= DateUtils.DAY_IN_MILLIS;
                }
            } else if (now >= targetTime) {
                startTime += DateUtils.DAY_IN_MILLIS;
                targetTime += DateUtils.DAY_IN_MILLIS;
            }
        } else {
            return null;
        }
        return new long[] {startTime, targetTime};
    }

    private void updateChargeControl() {
        if (mChargingControl == null) {
            return;
        }
        if (!isEnabled() || mIsControlCancelledOnce || !mIsPowerConnected) {
            providerDisable();
            cancelNotification();
            updateAlarmReceiver(false);
            return;
        }

        final int mode = getMode();
        providerEnable();
        if (mode == MODE_LIMIT) {
            final int limit = ChargingControlSettings.getLimit(mResolver);
            if (mProviderEnabled && onBatteryChangedLimit(mBatteryPct, limit)) {
                postLimitNotification(limit, mBatteryPct >= limit);
            }
        } else {
            long[] chargeTime = getChargeTime();
            if (chargeTime != null) {
                if (mProviderEnabled && onBatteryChangedTime(mBatteryPct, chargeTime[0],
                        chargeTime[1])) {
                    postTimeNotification(chargeTime[1], mBatteryPct == 100);
                } else {
                    cancelNotification();
                }
            }
        }
        updateAlarmReceiver(mode == MODE_AUTO);
    }

    private void updateAlarmReceiver(boolean register) {
        if (register && mAlarmReceiver == null) {
            mAlarmReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i(TAG, "Alarm changed, update charge times");
                    safely(ChargingControlController.this::updateChargeControl);
                }
            };
            mContext.registerReceiver(mAlarmReceiver,
                    new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED), null,
                    mHandler, Context.RECEIVER_EXPORTED);
        } else if (!register && mAlarmReceiver != null) {
            mContext.unregisterReceiver(mAlarmReceiver);
            mAlarmReceiver = null;
        }
    }

    private static long millisFromSecondOfDay(int secondOfDay) {
        return ZonedDateTime.of(LocalDate.now(), LocalTime.ofSecondOfDay(secondOfDay),
                ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ---------------------------------------------------------------- toggle provider

    private void providerEnable() {
        if (!mProviderEnabled) {
            mProviderEnabled = true;
            providerReset();
        }
    }

    private void providerDisable() {
        if (mProviderEnabled) {
            mProviderEnabled = false;
            providerReset();
        }
    }

    private void providerReset() {
        mIsLimitSet = false;
        mSavedTargetTime = 0;
        mEstimatedFullTime = 0;
        mStage = Stage.NONE;
        setChargingEnabled(true);
    }

    private boolean onBatteryChangedLimit(float currentPct, int targetPct) {
        mIsLimitSet = mIsLimitSet ? currentPct >= targetPct - mLimitMargin
                : currentPct >= targetPct;
        return setChargingEnabled(!mIsLimitSet);
    }

    private boolean onBatteryChangedTime(float batteryPct, long startTime, long targetTime) {
        mStage = getNextStage(batteryPct, startTime, targetTime);
        switch (mStage) {
            case NONE:
                setChargingEnabled(true);
                return false;
            case WAITING:
                return setChargingEnabled(false);
            default:
                return setChargingEnabled(true);
        }
    }

    private Stage getNextStage(float batteryPct, long startTime, long targetTime) {
        final long now = System.currentTimeMillis();
        Stage stage = mStage;

        Intent status = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_EXPORTED);
        boolean plugged = status != null
                && status.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;

        if (startTime > now && stage != Stage.CONTINUE) {
            // Not yet in the configured time frame
            return Stage.NONE;
        }
        if (mSavedTargetTime != targetTime
                && (mSavedTargetTime == 0 || mSavedTargetTime >= now)) {
            mSavedTargetTime = targetTime;
            stage = Stage.INITIAL;
        }

        long remaining = -1;
        BatteryStatsManager bsm = mContext.getSystemService(BatteryStatsManager.class);
        if (bsm != null) {
            BatteryUsageStats stats = bsm.getBatteryUsageStats();
            remaining = stats.getChargeTimeRemainingMs();
            try {
                stats.close();
            } catch (Exception ignored) {
            }
        }
        if (remaining != -1) {
            remaining += TIME_MARGIN_MS;
        }
        final long deltaTime = targetTime - now;

        switch (stage) {
            case NONE:
            case INITIAL:
                if (!plugged || batteryPct < CHARGE_CTRL_MIN_LEVEL || remaining == -1) {
                    return Stage.INITIAL;
                } else if (deltaTime > remaining) {
                    mEstimatedFullTime = remaining;
                    return Stage.WAITING;
                } else {
                    return Stage.CONTINUE;
                }
            case WAITING:
                return deltaTime <= mEstimatedFullTime ? Stage.CONTINUE : Stage.WAITING;
            case CONTINUE:
                return plugged ? Stage.CONTINUE : Stage.INITIAL;
        }
        return Stage.NONE;
    }

    private boolean setChargingEnabled(boolean enabled) {
        if (mChargingControl == null) {
            return false;
        }
        try {
            if (mChargingControl.getChargingEnabled() != enabled) {
                mChargingControl.setChargingEnabled(enabled);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set charging enabled", e);
            return false;
        }
    }

    // ---------------------------------------------------------------- notification

    private void createNotificationChannel() {
        NotificationManager nm = mContext.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                mResContext.getString(R.string.charging_control_title),
                NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(channel);
    }

    private void postLimitNotification(int limit, boolean done) {
        String text = done
                ? mResContext.getString(R.string.charging_control_notification_limit_done, limit)
                : mResContext.getString(R.string.charging_control_notification_limit, limit);
        postNotification(text, done);
    }

    private void postTimeNotification(long targetTime, boolean done) {
        String text = done
                ? mResContext.getString(R.string.charging_control_notification_full)
                : mResContext.getString(R.string.charging_control_notification_time,
                        DateFormat.getTimeFormat(mResContext).format(targetTime));
        postNotification(text, done);
    }

    private void postNotification(String text, boolean done) {
        NotificationManager nm = mContext.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        Notification.Builder builder = new Notification.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle(mResContext.getString(R.string.charging_control_title))
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setOngoing(!done)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);
        if (!done) {
            Intent intent = new Intent(ACTION_CANCEL_ONCE).setPackage(mContext.getPackageName());
            PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(new Notification.Action.Builder(null,
                    mResContext.getString(R.string.charging_control_notification_charge_full),
                    pi).build());
        }
        nm.notify(NOTIFICATION_ID, builder.build());
        mNotificationPosted = true;
    }

    private void cancelNotification() {
        if (!mNotificationPosted) {
            return;
        }
        NotificationManager nm = mContext.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.cancel(NOTIFICATION_ID);
        }
        mNotificationPosted = false;
    }
}
