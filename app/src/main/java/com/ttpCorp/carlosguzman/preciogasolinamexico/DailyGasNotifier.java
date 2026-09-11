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

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class DailyGasNotifier {

    private static final String TAG = "DailyGasNotifier";
    public static final String CHANNEL_ID = "gas_smart_notifications";
    public static final String PREF_LAST_NOTIFICATION_DATE = "last_notification_date";

    public static boolean checkPricesAndNotify(Context context) {
        return checkPricesAndNotify(context, false);
    }

    public static boolean checkPricesAndNotify(Context context, boolean forceCheck) {
        if (context == null) return false;

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (!forceCheck) {
            String lastSentDate = mPrefs.getString(PREF_LAST_NOTIFICATION_DATE, "");
            if (todayStr.equals(lastSentDate)) {
                Log.d(TAG, "Notification for today (" + todayStr + ") already sent. Skipping.");
                return true;
            }
        }

        String estadoId = mPrefs.getString("shared_edoID", "");
        if (estadoId.isEmpty()) {
            estadoId = mPrefs.getString("gasApp_estadoId", "14");
        }

        String municipioId = mPrefs.getString("shared_munID", "");
        if (municipioId.isEmpty()) {
            municipioId = mPrefs.getString("gasApp_municipioId", "14039");
        }

        String municipioNombre = mPrefs.getString("gasApp_municipioNombre", "tu ciudad");

        String primaryApiUrl = "http://45.132.241.215:8888/gasolinamexico/dev/api.cfm?mode=getPrecio&estadoid=" + estadoId + "&municipioid=" + municipioId;
        String fallbackApiUrl = "http://45.132.241.215/gasolinamexico/prod/?mode=getPrecio&estadoid=" + estadoId + "&municipioid=" + municipioId;

        String jsonStr = fetchUrlData(primaryApiUrl);
        if (jsonStr == null || jsonStr.trim().isEmpty() || jsonStr.trim().equals("[]")) {
            jsonStr = fetchUrlData(fallbackApiUrl);
        }

        if (jsonStr == null || jsonStr.trim().isEmpty() || jsonStr.trim().equals("[]")) {
            Log.w(TAG, "Unable to fetch gas prices from both endpoints.");
            return false;
        }

        try {
            String cheapestStationName = "";
            double cheapestMagna = Double.MAX_VALUE;
            double cheapestPremium = 0.0;

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
                        String normKey = pKey.toLowerCase(Locale.ROOT);
                        double pVal = 0.0;
                        try {
                            String priceStr = pricesObj.getString(pKey).replaceAll("[^0-9.]", "");
                            if (!priceStr.isEmpty()) {
                                pVal = Double.parseDouble(priceStr);
                            }
                        } catch (Exception ignored) {}

                        if (pVal > 0) {
                            if (normKey.contains("regular") || normKey.contains("magna") || normKey.contains("87") || normKey.contains("verde")) {
                                magnaPrice = pVal;
                            } else if (normKey.contains("premium") || normKey.contains("roja") || normKey.contains("91") || normKey.contains("92")) {
                                premiumPrice = pVal;
                            }
                        }
                    }

                    if (magnaPrice > 0 && magnaPrice < cheapestMagna) {
                        cheapestMagna = magnaPrice;
                        cheapestPremium = premiumPrice;
                        cheapestStationName = stationName.replaceAll("(?i)\\s*S\\.?A\\.?\\s*DE\\s*C\\.?V\\.?", "").trim();
                    }
                }
            }

            if (cheapestMagna < Double.MAX_VALUE && !cheapestStationName.isEmpty()) {
                sendNotification(context, municipioNombre, cheapestStationName, cheapestMagna, cheapestPremium);
                mPrefs.edit().putString(PREF_LAST_NOTIFICATION_DATE, todayStr).apply();
                Log.d(TAG, "Notification dispatched successfully for " + todayStr);
                return true;
            } else {
                Log.d(TAG, "No valid station or price found in JSON.");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing gas prices JSON", e);
            return false;
        }
    }

    private static String fetchUrlData(String urlString) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(9000);
            urlConnection.setReadTimeout(9000);
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                return null;
            }

            StringBuilder buffer = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
            return buffer.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching URL: " + urlString, e);
            return null;
        } finally {
            if (urlConnection != null) {
                try {
                    urlConnection.disconnect();
                } catch (Exception ignored) {}
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private static void sendNotification(Context context, String municipio, String stationName, double magna, double premium) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Precios Diario Gasolina",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones diarias del mejor precio de gasolina");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("fromNotification", true);
        intent.putExtra("messageAlert", "Mejor precio en " + municipio + ": " + stationName + " - Magna: $" + String.format(Locale.US, "%.2f", magna));

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "⛽ ¡Mejor precio hoy en " + municipio + "!";
        String contentText = stationName + ": Magna $" + String.format(Locale.US, "%.2f", magna);
        if (premium > 0) {
            contentText += " | Premium $" + String.format(Locale.US, "%.2f", premium);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify(800, builder.build());
        }
    }
}
