package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

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
import java.util.Calendar;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private gasAdapter mGasolinaAdapter;
    ListView listView;
    private AdView mAdView;
    SharedPreferences mPrefs;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //populate list view

       // Toast.makeText(getActivity(), "strtext"+strtext, Toast.LENGTH_SHORT).show();
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mGasolinaAdapter = new gasAdapter(getActivity(),R.layout.list_view_gas);

        String[] name = {"...","...","..."};
        String[] Qty = {"...","...","..."};
        String[] image = {"magna","premium","diesel"};
        String[] prevValue = {"...","....","..."};
        String[] nextValue = {"...","....","..."};

        listView = (ListView) rootView.findViewById(R.id.listview_gasolina);
        listView.setAdapter(mGasolinaAdapter);

        int i = 0;
        for (String Name : name){
            gasolinaClass obj = new gasolinaClass(image[i],Name, Qty[i],prevValue[i],nextValue[i]);
            mGasolinaAdapter.addGas(obj);
            i++;
        }
        //set on click listener to each view
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String backgroundImageName="";
                ImageView v = (ImageView) view.findViewById(R.id.thumbImage);
                int drawable = (Integer) v.getTag();
                if(drawable == R.drawable.magna){
                    backgroundImageName = "Magna";
                }
                if(drawable == R.drawable.premium){
                    backgroundImageName = "Premium";
                }
                if(drawable == R.drawable.diesel){
                    backgroundImageName = "Diesel";
                }
                //add intent to each list item and pass extra_text as data
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                    .putExtra(Intent.EXTRA_TEXT, backgroundImageName);
                startActivity(intent);
            }
        });
        //create the money thing :)
        mAdView = (AdView) rootView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        return rootView;
    }
    public void updateGasolinaPrice() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int month = cal.get(Calendar.MONTH)+1;
        int year = cal.get(Calendar.YEAR);
        String thisMonth = Integer.toString(month);
        String thisYear = Integer.toString(year);
        String thisLugar = getArguments().getString("zone");
        weatherTask.execute(thisMonth, thisYear,thisLugar);
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
            //final String OWM_PREVVALUE = "prevValor";
           // final String OWM_NEXTVALUE = "sigValor";
            final String OWM_VALOR = "valor";
            final String OWM_MES = "mes";
            final String OWM_ANO = "ano";


            JSONArray weatherArray = new JSONArray(forecastJsonStr);

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                String gasolina;
                //String prevValue;
                String valor;
                String mes;
                String ano;
                /*String sigValor;
                String lugar;*/



                // Get the JSON object representing the day
                JSONObject dayGas = weatherArray.getJSONObject(i);
                //get data from JSON
                gasolina = dayGas.getString(OWM_GASOLINA);
                //prevValue = dayGas.getString(OWM_PREVVALUE);
                //sigValor = dayGas.getString(OWM_NEXTVALUE);
                valor = dayGas.getString(OWM_VALOR);
                mes = dayGas.getString(OWM_MES);
                ano = dayGas.getString(OWM_ANO);

                //resultStrs[i] = gasolina+","+prevValue+","+valor+","+mes+","+ano+","+sigValor;
                resultStrs[i] = gasolina+", ,"+valor+","+mes+","+ano+", ";
                Log.d("urlDebug", "url: "+gasolina+", ,"+valor+","+mes+","+ano+", ");
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
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr = null;
            int numDays =  3;

            try {
                mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                final String getRegion = mPrefs.getString("getReg", "");
                //Log.d("urlDebug", "url: "+ getRegion);
                String FORECAST_BASE_URL =
                        "http://areliablewindowcleaning.com/gasolina/gasPrice.php?";
                if (getRegion != ""){
                    FORECAST_BASE_URL =
                            "http://areliablewindowcleaning.com/gasolina/regionsdiario.php?";

                }
                Log.d("urldebug",FORECAST_BASE_URL+""+params[1]);
                final String QUERY_PARAM_YEAR = "y";
                final String QUERY_PARAM_MONTH = "m";
                final String QUERY_PARAM_MODE = "mode";
                final String QUERY_PARAM_REGIONID = "regionID";


                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM_YEAR, params[1])
                        .appendQueryParameter(QUERY_PARAM_MODE, "getRegionPrice")
                        .appendQueryParameter(QUERY_PARAM_REGIONID,getRegion)
                        .build();

                URL url = new URL(builtUri.toString());
                //Log.d("urlDebug_fragment", "url: "+ url);
                urlConnection = (HttpURLConnection) url.openConnection();
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
                Toast.makeText(getActivity(), "Error de Coneccion!", Toast.LENGTH_SHORT).show();
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
                        //gasolina|prevValue|valor|mes|ano|nextValue;
                        gasolinaClass obj = new gasolinaClass(array[0], array[0], array[2],array[1],array[5]);
                        mGasolinaAdapter.addGas(obj);
                    }
                }
            }
        }
    }
}
