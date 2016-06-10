package com.example.carlosguzman.gasolinamexico;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by 501820531 on 5/27/2016.
 */
public class GasAppWidgetProvider extends AppWidgetProvider {
    AsyncTask<String, Void, String[]> mTask;
    String forecastStr = "Premium"; // todo assing this variable to what the user saved on preferences.
    String newValue;
    RemoteViews views;
    public static  String LOG_TAG = "MyActivity";
    public static final String KEY_PREF_SYNC_CONN = "pref_gasWidget";

    public void onUpdate(Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        forecastStr = sharedPref.getString("pref_gasWidget","1");
        Log.d(LOG_TAG,"Carlos Test this widget was updated!"+sharedPref.getString("pref_gasWidget","1"));

        for (int i=0; i<N; i++) {
            //String syncConnPref = sharedPref.getString(pref_gasWidget, "");
            final int appWidgetId = appWidgetIds[i];

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy");
            String formattedDate = df.format(c.getTime());

            views = new RemoteViews(context.getPackageName(), R.layout.activity_widget);

            views.setOnClickPendingIntent(R.id.wid_gasolina, pendingIntent);
            views.setOnClickPendingIntent(R.id.wid_precio, pendingIntent);
            views.setOnClickPendingIntent(R.id.wid_fecha, pendingIntent);

            views.setTextViewText(R.id.wid_fecha, formattedDate);
            mTask = new AsyncTask<String, Void, String[]>() {

                @Override
                public String[] doInBackground(String... params) {

                    BufferedReader reader = null;
                    HttpURLConnection urlConnection = null;
                    Calendar cal = Calendar.getInstance();
                    int year = cal.get(Calendar.YEAR);
                    int month = cal.get(Calendar.MONTH)+1;
                    String thisMonth = Integer.toString(month);
                    String thisYear = Integer.toString(year);
                    String forecastJsonStr = null;
                    String thisurl = "http://areliablewindowcleaning.com/gasolina/gasPrice.php?mode=gasolina&y=" +thisYear +"&m="+thisMonth+"&gasolina="+forecastStr;

                    try {
                        URL url = new URL(thisurl.toString());
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.connect();
                        InputStream inputStream = urlConnection.getInputStream();
                        StringBuffer buffer = new StringBuffer();
                        if (inputStream == null) {
                            return null;
                        }
                        reader = new BufferedReader(new InputStreamReader(inputStream));

                        String line;
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line + "\n");
                        }

                        if (buffer.length() == 0) {
                            return null;
                        }
                        forecastJsonStr = buffer.toString();

                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error ", e);
                        return null;
                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (final IOException e) {
                                Log.e(LOG_TAG, "Error closing stream", e);
                            }
                        }
                    }
                    try {

                        return getWeatherDataFromJson(forecastJsonStr, 12);


                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error getting data from JSON", e);
                        e.printStackTrace();
                    }
                    return null;
                }

                private  String[] getWeatherDataFromJson(String forecastJsonStr,int numDays)
                        throws JSONException {

                    final String OWM_VALOR = "valor";
                    final String OWM_MES = "mes";

                    JSONArray weatherArray = new JSONArray(forecastJsonStr);

                    Time dayTime = new Time();
                    dayTime.setToNow();
                    String[] resultStrs = new String[numDays];
                    for(int i = 0; i < weatherArray.length(); i++) {
                        String valor;
                        String mes;
                        String ano;

                        JSONObject dayGas = weatherArray.getJSONObject(i);

                        valor = dayGas.getString(OWM_VALOR);
                        mes = dayGas.getString(OWM_MES);

                        resultStrs[i] =valor+","+mes;

                    }

                    for (String s : resultStrs) {

                    }
                    return resultStrs;

                }
                protected void onPostExecute(String[] result) {
                    if (result != null) {
                        for (String dayForecastStr :result){
                            if (dayForecastStr != null) {
                                String[] array = dayForecastStr.split(",");
                                //valor|mes;
                                newValue = array[0];

                                views.setTextViewText(R.id.wid_precio, newValue);


                            }
                        }
                    }
                    views.setTextViewText(R.id.wid_gasolina, forecastStr);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            };
            mTask.execute();



            // Tell the AppWidgetManager to perform an update on the current app widget

        }

    }
    public String getMonth(int month) {
        return new DateFormatSymbols().getMonths()[month-1];
    }
}
