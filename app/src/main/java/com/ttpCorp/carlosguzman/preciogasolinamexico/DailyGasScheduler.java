package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class DailyGasScheduler {

    private static final String TAG = "DailyGasScheduler";
    public static final String PREF_NOTIFICATION_HOUR = "notification_hour";
    public static final int ALARM_REQUEST_CODE = 1001;
    public static final String WORK_NAME_PERIODIC = "Daily8AMGasWork";
    public static final String WORK_NAME_IMMEDIATE = "DailyGasWorkImmediate";

    public static void scheduleDaily(Context context, int targetHour) {
        scheduleDaily(context, targetHour, false);
    }

    public static void scheduleDaily(Context context, int targetHour, boolean forceReplace) {
        if (context == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int oldHour = prefs.getInt(PREF_NOTIFICATION_HOUR, -999);
        prefs.edit().putInt(PREF_NOTIFICATION_HOUR, targetHour).apply();

        if (targetHour == -1) {
            cancelDaily(context);
            Log.d(TAG, "Daily notifications disabled by user.");
            return;
        }

        // 1. Programar la alarma en AlarmManager
        scheduleNextAlarmOnly(context, targetHour);

        // 2. Programar respaldo periódico de WorkManager con política KEEP
        // Solo reemplazar el trabajo periódico si la hora configurada cambió explícitamente o si forceReplace es true
        boolean shouldReplaceWork = forceReplace || (oldHour != targetHour && oldHour != -999);
        schedulePeriodicWorkBackup(context, targetHour, shouldReplaceWork);
    }

    public static void scheduleNextAlarmOnly(Context context) {
        if (context == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int targetHour = prefs.getInt(PREF_NOTIFICATION_HOUR, 8);
        if (targetHour != -1) {
            scheduleNextAlarmOnly(context, targetHour);
        }
    }

    public static void scheduleNextAlarmOnly(Context context, int targetHour) {
        if (context == null || targetHour == -1) return;

        try {
            Calendar now = Calendar.getInstance();
            Calendar nextRun = Calendar.getInstance();
            nextRun.set(Calendar.HOUR_OF_DAY, targetHour);
            nextRun.set(Calendar.MINUTE, 0);
            nextRun.set(Calendar.SECOND, 0);
            nextRun.set(Calendar.MILLISECOND, 0);

            if (!nextRun.after(now)) {
                nextRun.add(Calendar.DAY_OF_YEAR, 1);
            }

            long timeDiff = nextRun.getTimeInMillis() - now.getTimeInMillis();

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(context, DailyGasAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_REQUEST_CODE,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextRun.getTimeInMillis(), pendingIntent);
                        Log.d(TAG, "Exact alarm scheduled for " + nextRun.getTime() + " (in " + (timeDiff / 60000) + " minutes)");
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextRun.getTimeInMillis(), pendingIntent);
                        Log.d(TAG, "Inexact alarm scheduled (exact permission not granted) for " + nextRun.getTime());
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextRun.getTimeInMillis(), pendingIntent);
                    Log.d(TAG, "Exact alarm scheduled (API 23+) for " + nextRun.getTime());
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, nextRun.getTimeInMillis(), pendingIntent);
                    Log.d(TAG, "Standard alarm scheduled for " + nextRun.getTime());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm in DailyGasScheduler", e);
        }
    }

    private static void schedulePeriodicWorkBackup(Context context, int targetHour, boolean replace) {
        try {
            Calendar now = Calendar.getInstance();
            Calendar nextRun = Calendar.getInstance();
            nextRun.set(Calendar.HOUR_OF_DAY, targetHour);
            nextRun.set(Calendar.MINUTE, 0);
            nextRun.set(Calendar.SECOND, 0);
            nextRun.set(Calendar.MILLISECOND, 0);

            if (!nextRun.after(now)) {
                nextRun.add(Calendar.DAY_OF_YEAR, 1);
            }

            long timeDiff = nextRun.getTimeInMillis() - now.getTimeInMillis();

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest dailyWorkRequest = new PeriodicWorkRequest.Builder(
                    DailyGasWorker.class,
                    24, TimeUnit.HOURS
            )
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build();

            ExistingPeriodicWorkPolicy policy = replace ? ExistingPeriodicWorkPolicy.REPLACE : ExistingPeriodicWorkPolicy.KEEP;
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    policy,
                    dailyWorkRequest
            );
            Log.d(TAG, "PeriodicWorkRequest configured with policy: " + policy);

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling periodic work backup", e);
        }
    }

    public static void rescheduleIfNeeded(Context context) {
        if (context == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int hour = prefs.getInt(PREF_NOTIFICATION_HOUR, 8);
        if (hour != -1) {
            Log.d(TAG, "Rescheduling daily alarm for hour: " + hour);
            // Programar la alarma sin reiniciar destructivamente el periodic work
            scheduleNextAlarmOnly(context, hour);
            schedulePeriodicWorkBackup(context, hour, false);
        }
    }

    public static void cancelDaily(Context context) {
        if (context == null) return;
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(context, DailyGasAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_REQUEST_CODE,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC);
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_IMMEDIATE);
            Log.d(TAG, "All daily gas alarms and work cancelled.");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling daily gas alarms", e);
        }
    }

    public static boolean isBatteryOptimizationIgnored(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
        } catch (Exception e) {
            Log.e(TAG, "Error checking battery optimization status", e);
            return false;
        }
    }

    @SuppressLint("BatteryLife")
    public static void requestIgnoreBatteryOptimization(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
            } catch (Exception ex) {
                Log.e(TAG, "Could not open battery optimization settings", ex);
            }
        }
    }
}
