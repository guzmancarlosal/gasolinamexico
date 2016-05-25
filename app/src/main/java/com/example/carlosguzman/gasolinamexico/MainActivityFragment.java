package com.example.carlosguzman.gasolinamexico;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private gasAdapter mGasolinaAdapter;
    ListView listView;
    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mGasolinaAdapter = new gasAdapter(getActivity(),R.layout.list_view_gas);

        String[] name = {"Cargando..","Cargando..","Cargando.."};
        String[] Qty = {"Cargando..","Cargando..","Cargando.."};
        String[] image = {"diesel","magna","premium"};
        String[] prevValue = {"Cargando..","Cargando..","Cargando.."};

        listView = (ListView) rootView.findViewById(R.id.listview_gasolina);
        listView.setAdapter(mGasolinaAdapter);

        int i = 0;
        for (String Name : name){
            gasolinaClass obj = new gasolinaClass(image[i],Name, Qty[i],prevValue[i]);
            mGasolinaAdapter.addGas(obj);
            i++;
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String textItem = "this is my custom text";//mGasolinaAdapter.getItem(position);//getItem(position);
                //Toast.makeText(getActivity(), textItem, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                    .putExtra(Intent.EXTRA_TEXT, textItem);
                startActivity(intent);
            }
        });
        return rootView;
    }
    public void updateGasolinaPrice() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int month = cal.get(Calendar.MONTH)+1;
        int year = cal.get(Calendar.YEAR);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String thisMonth = Integer.toString(month);
        String thisYear = Integer.toString(year);
        //TODO unhardcode this
        final String TAG = "MyActivity";
        Log.d(TAG, "Carlos url params day "+ day + " today"+ thisMonth+ " month and year "+ thisYear);
        weatherTask.execute(thisMonth, thisYear);
    }
    @Override
    public void onStart() {
        super.onStart();
        updateGasolinaPrice();
    }
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_GASOLINA = "gasolina";
            final String OWM_PREVVALUE = "prevValor";
            final String OWM_VALOR = "valor";
            final String OWM_MES = "mes";
            final String OWM_ANO = "ano";



            JSONArray weatherArray = new JSONArray(forecastJsonStr);
           // JSONObject forecastJson = new JSONObject(forecastJsonStr);
            //JSONArray weatherArray = forecastJson.getJSONArray(forecastJsonStr);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();
            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                String gasolina;
                String prevValue;
                String valor;
                String mes;
                String ano;



                // Get the JSON object representing the day
                JSONObject dayGas = weatherArray.getJSONObject(i);
                //get data from JSON
                gasolina = dayGas.getString(OWM_GASOLINA);
                prevValue = dayGas.getString(OWM_PREVVALUE);
                valor = dayGas.getString(OWM_VALOR);
                mes = dayGas.getString(OWM_MES);
                ano = dayGas.getString(OWM_ANO);

                resultStrs[i] = gasolina+",Anterior: "+prevValue+","+valor+","+mes+","+ano;


            }

            for (String s : resultStrs) {

            }
            return resultStrs;

        }
        @Override
        protected String[] doInBackground(String... params) {
            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            int numDays =  3;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL =
                        "http://areliablewindowcleaning.com/gasolina/gasPrice.php?";
                final String QUERY_PARAM_MONTH = "m";
                final String QUERY_PARAM_YEAR = "y";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM_MONTH, params[0])
                        .appendQueryParameter(QUERY_PARAM_YEAR, params[1])
                        .build();

                URL url = new URL(builtUri.toString());
                final String TAG = "MyActivity";
                Log.d(TAG, "Carlos url final "+ builtUri);
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                //urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
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
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error getting data from JSON", e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }


        @Override
        protected void onPostExecute(String[] result) {
            if (result != null){

                mGasolinaAdapter.clear();
                for (String dayForecastStr :result){
                    if (dayForecastStr != null) {
                        String[] array = dayForecastStr.split(",");
                        //gasolina|prevValue|valor|mes|ano;
                        gasolinaClass obj = new gasolinaClass(array[0], array[0], array[2],array[1]);
                        mGasolinaAdapter.addGas(obj);
                    }
                }
            }
        }
    }
}
