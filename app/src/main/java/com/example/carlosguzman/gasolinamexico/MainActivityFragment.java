package com.example.carlosguzman.gasolinamexico;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import java.text.SimpleDateFormat;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private ArrayAdapter<String> mGasolinaAdapter;
    ListView listView;
    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String [] gasolinaName = {"magna", "premium","diesel"};
        String [] gasolinaPrice = {"13.90", "13.89","13.77"};


        mGasolinaAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_view_gas, // The name of the layout ID.
                        R.id.list_view_gas_text, // The ID of the textview to populate.
                        gasolinaName);
      //  gasAdapter mGasolinaAdapter = new gasAdapter(getActivity(),R.layout.list_view_gas);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        listView = (ListView) rootView.findViewById(R.id.listview_gasolina);
        listView.setAdapter(mGasolinaAdapter);
        return rootView;
    }
    public void updateGasolinaPrice() {
        final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        Log.d(LOG_TAG,"task created!");
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        //weatherTask.execute(location);
        weatherTask.execute();
        Log.d(LOG_TAG, "task excecuted!");
    }
    @Override
    public void onStart() {
        final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        Log.d(LOG_TAG,"started!");
        super.onStart();
        Log.d(LOG_TAG, "updating!");
        updateGasolinaPrice();
        Log.d(LOG_TAG, "updated!");
    }
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */

        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low, String unitType) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            //aqui van las preferencias
           /* if (unitType.equals(getString(R.string.pref_units_imperial))) {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            } else if (!unitType.equals(getString(R.string.pref_units_metric))) {
                Log.d(LOG_TAG, "Unit type not found: " + unitType);
            }*/
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_GASOLINA = "gasolina";
            final String OWM_LUGAR = "lugar";
            final String OWM_VALOR = "valor";
            final String OWM_MES = "mes";
            final String OWM_ANO = "ano";
            final String OWM_COMENTARIO = "commment";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_GASOLINA);

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
                Log.e(LOG_TAG, "items! ");
                String gasolina;
                String lugar;
                String valor;
                String mes;
                String ano;
                String comment;


                // Get the JSON object representing the day
                JSONObject dayGas = weatherArray.getJSONObject(i);
                //get data from JSON
                gasolina = dayGas.getString(OWM_VALOR);
                lugar = dayGas.getString(OWM_VALOR);
                valor = dayGas.getString(OWM_VALOR);
                mes = dayGas.getString(OWM_VALOR);
                ano = dayGas.getString(OWM_VALOR);
                comment = dayGas.getString(OWM_VALOR);


                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.

                resultStrs[i] = gasolina + " - " + lugar + " - " + valor + " - " + mes + " - " + ano + " - " + comment;
            }

            for (String s : resultStrs) {

            }
            Log.d(LOG_TAG, "final result string!"+resultStrs);
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
            int numDays =  7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL =
                        "http://areliablewindowcleaning.com/gasolina/gasPrice.php?";
                final String QUERY_PARAM = "q";


                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .build();

                URL url = new URL(builtUri.toString());
                Log.d(LOG_TAG, "this is the URL"+url);
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
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
                    mGasolinaAdapter.add(dayForecastStr);
                }
            }
        }
    }
}
