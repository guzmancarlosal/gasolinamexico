package com.ttpCorp.carlosguzman.preciogasolinamexico;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

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
import java.util.ArrayList;
import java.util.Calendar;

public class DetailActivity extends ActionBarActivity {
    private static final String TAG = "MyActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public static class DetailFragment extends Fragment {

        AsyncTask<String, Void, String[]> mTask;
        String jsonString;
        private static final String TAG = "MyActivity";
        String forecastStr;
        private AdView mAdView;
        public DetailFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            final View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            Intent intent = getActivity().getIntent();
            if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
                forecastStr = intent.getStringExtra(Intent.EXTRA_TEXT);

                getActivity().setTitle(forecastStr);

            }

            mTask = new AsyncTask <String, Void, String[]>() {
                public static final String LOG_TAG = "MyActivity";
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
                    String thisurl = "http://areliablewindowcleaning.com/gasolina/gasPrice.php?mode=chart&y=" +thisYear +"&m="+thisMonth+"&gasolina="+forecastStr;
                    //Log.d(LOG_TAG, "carlos test Error on url stream"+thisurl);
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
                    final String OWM_ANO = "ano";

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
                        ano = dayGas.getString(OWM_ANO);

                        resultStrs[i] =valor+","+mes+","+ano;


                    }

                    for (String s : resultStrs) {

                    }
                    return resultStrs;

                }
                protected void onPostExecute(String[] result) {
                    if (result != null) {
                        ArrayList<Entry> entries = new ArrayList<>();
                        ArrayList<String> labels = new ArrayList<String>();
                        LineDataSet dataset = new LineDataSet(entries, " 12 meses");
                        int counter = 0;
                        Float minYval = 0f;
                        Float maxYval = 0f;
                        Float margin = 0.5f;
                        for (String dayForecastStr :result){
                            if (dayForecastStr != null) {
                                String[] array = dayForecastStr.split(",");
                                //valor|mes|ano;

                                Float test = Float.parseFloat(array[0]);
                                entries.add(new Entry(test, counter));
                                int thisMonthVal = Integer.parseInt(array[1]);
                                String thisMonth = getMonth(thisMonthVal);
                                labels.add(thisMonth);
                                if (minYval == 0) {
                                    minYval = Float.parseFloat(array[0]);;
                                }
                                if (maxYval == 0) {
                                    maxYval = Float.parseFloat(array[0]);;
                                }
                                if (test < minYval) {
                                    minYval = test;
                                }
                                if (test > maxYval) {
                                    maxYval = test;
                                }
                                counter ++;

                            }
                        }
                        //Log.d(LOG_TAG, "Carlos Test min and max values"+maxYval+"-"+minYval);
                        LineChart lineChart = (LineChart) rootView.findViewById(R.id.chart);

                        LineData data = new LineData(labels, dataset);
                        lineChart.getAxisLeft().setLabelCount(2, true);
                        lineChart.getAxisRight().setEnabled(false);
                        lineChart.getAxisLeft().setStartAtZero(false);
                        lineChart.setAutoScaleMinMaxEnabled(false);
                        lineChart.getAxisLeft().setAxisMaxValue(maxYval + margin);
                        lineChart.getAxisLeft().setAxisMinValue(minYval - margin);

                        dataset.setValueTextSize(10);
                        dataset.setDrawCubic(true);
                        dataset.setDrawFilled(true);

                        lineChart.setData(data);

                        lineChart.animateY(10);


                    }

                }
            };
            mTask.execute();
            //create the money thing :)
            mAdView = (AdView) rootView.findViewById(R.id.adView2);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            return rootView;
        }
        public String getMonth(int month) {
            return new DateFormatSymbols().getMonths()[month-1];
        }


    }


}