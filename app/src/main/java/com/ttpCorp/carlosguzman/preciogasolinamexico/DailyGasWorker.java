package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

public class DailyGasWorker extends Worker {

    private static final String TAG = "DailyGasWorker";
    public static final String CHANNEL_ID = "gas_smart_notifications";

    public DailyGasWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "DailyGasWorker starting 8:00 AM price check...");
        Context context = getApplicationContext();
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        String estadoId = mPrefs.getString("shared_edoID", "");
        if (estadoId.isEmpty()) {
            estadoId = mPrefs.getString("gasApp_estadoId", "");
        }

        String municipioId = mPrefs.getString("shared_munID", "");
        if (municipioId.isEmpty()) {
            municipioId = mPrefs.getString("gasApp_municipioId", "");
        }

        String municipioNombre = mPrefs.getString("gasApp_municipioNombre", "tu ciudad");

        if (estadoId.isEmpty() || municipioId.isEmpty()) {
            Log.d(TAG, "No municipality configured yet for daily notification.");
            return Result.success();
        }

        String apiUrl = "http://45.132.241.215/gasolinamexico/prod/?mode=getPrecio&estadoid=" + estadoId + "&municipioid=" + municipioId;

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String cheapestStationName = "";
        double cheapestMagna = Double.MAX_VALUE;
        double cheapestPremium = 0.0;

        try {
            URL url = new URL(apiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                return Result.success();
            }

            StringBuilder buffer = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            String jsonStr = buffer.toString();
            if (jsonStr.isEmpty()) {
                return Result.success();
            }

            JSONObject rootObj = new JSONObject(jsonStr);
            Iterator<String> keys = rootObj.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject stationObj = rootObj.getJSONObject(key);
                String stationName = stationObj.optString("NOMBRE", "Gasolinera");

                if (stationObj.has("PRECIO")) {
                    JSONObject pricesObj = stationObj.getJSONObject("PRECIO");
                    Iterator<String> pKeys = pricesObj.keys();
                    double magnaPrice = 0.0;
                    double premiumPrice = 0.0;

                    while (pKeys.hasNext()) {
                        String pKey = pKeys.next();
                        String normKey = pKey.toLowerCase();
                        double pVal = Double.parseDouble(pricesObj.getString(pKey));

                        if (normKey.contains("regular") || normKey.contains("magna")) {
                            magnaPrice = pVal;
                        } else if (normKey.contains("premium")) {
                            premiumPrice = pVal;
                        }
                    }

                    if (magnaPrice > 0 && magnaPrice < cheapestMagna) {
                        cheapestMagna = magnaPrice;
                        cheapestPremium = premiumPrice;
                        cheapestStationName = stationName.replace("S.A. DE C.V.", "").trim();
                    }
                }
            }

            if (cheapestMagna < Double.MAX_VALUE && !cheapestStationName.isEmpty()) {
                sendSmartNotification(context, municipioNombre, cheapestStationName, cheapestMagna, cheapestPremium);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking daily gas prices", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {}
            }
        }

        return Result.success();
    }

    private void sendSmartNotification(Context context, String municipio, String stationName, double magna, double premium) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Precios Diario 8 AM",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notificaciones diarias del mejor precio de gasolina");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("fromNotification", true);
        intent.putExtra("messageAlert", "Mejor precio en " + municipio + ": " + stationName + " - Magna: $" + String.format("%.2f", magna));

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "⛽ ¡Mejor precio hoy en " + municipio + "!";
        String contentText = stationName + ": Magna $" + String.format("%.2f", magna);
        if (premium > 0) {
            contentText += " | Premium $" + String.format("%.2f", premium);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify(800, builder.build());
        }
    }
}
