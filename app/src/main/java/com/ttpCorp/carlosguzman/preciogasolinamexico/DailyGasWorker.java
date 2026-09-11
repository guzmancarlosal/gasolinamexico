package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DailyGasWorker extends Worker {

    private static final String TAG = "DailyGasWorker";

    public DailyGasWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "DailyGasWorker executing via WorkManager...");
        Context context = getApplicationContext();

        boolean success = DailyGasNotifier.checkPricesAndNotify(context);
        if (!success) {
            Log.w(TAG, "DailyGasNotifier failed. Attempt count: " + getRunAttemptCount());
            if (getRunAttemptCount() < 3) {
                return Result.retry();
            }
        }
        return Result.success();
    }
}
