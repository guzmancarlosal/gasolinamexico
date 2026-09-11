package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DailyGasAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "DailyGasAlarmReceiver";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "DailyGasAlarmReceiver triggered. Executing daily check...");

        // 1. Reschedule tomorrow's alarm without destroying the WorkManager periodic backup
        DailyGasScheduler.scheduleNextAlarmOnly(context);

        // 2. Keep receiver alive for up to 30s using goAsync and fetch immediately
        final PendingResult pendingResult = goAsync();

        EXECUTOR.execute(() -> {
            try {
                Log.d(TAG, "Executing immediate price check in background thread...");
                boolean success = DailyGasNotifier.checkPricesAndNotify(context);
                if (!success) {
                    Log.w(TAG, "Immediate price check failed. Enqueuing WorkManager backup...");
                    enqueueWorkManagerBackup(context);
                } else {
                    Log.d(TAG, "Daily check completed successfully via alarm receiver.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during alarm price check", e);
                enqueueWorkManagerBackup(context);
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void enqueueWorkManagerBackup(Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DailyGasWorker.class)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build();

            WorkManager.getInstance(context).enqueueUniqueWork(
                    DailyGasScheduler.WORK_NAME_IMMEDIATE,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
            );
            Log.d(TAG, "Enqueued WorkManager backup DailyGasWorker.");
        } catch (Exception e) {
            Log.e(TAG, "Error enqueuing backup work", e);
        }
    }
}
