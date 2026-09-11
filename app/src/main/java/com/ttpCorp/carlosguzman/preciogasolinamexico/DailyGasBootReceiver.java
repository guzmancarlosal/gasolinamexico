package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DailyGasBootReceiver extends BroadcastReceiver {

    private static final String TAG = "DailyGasBootReceiver";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "DailyGasBootReceiver received broadcast action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            Log.d(TAG, "Device restarted or app updated. Restoring daily gas alert schedule...");
            DailyGasScheduler.rescheduleIfNeeded(context);

            // Catch-up check: si el teléfono estuvo apagado durante la hora programada hoy,
            // verificar y disparar la alerta del día si aún no se ha enviado.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int targetHour = prefs.getInt(DailyGasScheduler.PREF_NOTIFICATION_HOUR, 8);
            if (targetHour != -1) {
                int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                if (currentHour >= targetHour) {
                    final PendingResult pendingResult = goAsync();
                    EXECUTOR.execute(() -> {
                        try {
                            DailyGasNotifier.checkPricesAndNotify(context, false);
                        } catch (Exception e) {
                            Log.e(TAG, "Error in catch-up notification after boot", e);
                        } finally {
                            pendingResult.finish();
                        }
                    });
                }
            }
        }
    }
}
