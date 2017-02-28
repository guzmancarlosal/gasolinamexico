package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by 501820531 on 6/28/2016.
 */
public class CalculadoraActivity  extends Fragment {
    private AdView mAdView;
    private AdView mAdView2;
    Spinner spinner;
    Spinner spinner1;
    private EditText number_cant ;
    TextView text_result;
    String precioGasolina = "0";
    String precioMagna;
    String precioPremium;
    String precioDiesel;
    String num2 = "";
    SharedPreferences mPrefs;
    public CalculadoraActivity() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_calc, container, false);
        number_cant = (EditText) rootView.findViewById(R.id.number_cant);
        text_result = (TextView) rootView.findViewById(R.id.text_result);
        //populate the compras dropdown
        spinner = (Spinner) rootView.findViewById(R.id.tipo_compra);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.compra_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            String itemSelected = "";

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                itemSelected = getResources().getStringArray(R.array.compra_array)[spinner.getSelectedItemPosition()];

                if (itemSelected.equals("Por Pesos")) {
                    number_cant.getText().clear();
                    number_cant.setHint(R.string.number_hint2);
                    number_cant.getText().clear();
                } else {
                    number_cant.getText().clear();
                    number_cant.setHint(R.string.number_hint1);
                    number_cant.getText().clear();
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });
        //populate Gasolina Dropdown
        spinner1 = (Spinner) rootView.findViewById(R.id.tipo_gasolina);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(getActivity(),
                R.array.gasolina_array, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapter1);

        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            String itemSelected2 = "";


            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                itemSelected2 = getResources().getStringArray(R.array.gasolina_array)[spinner1.getSelectedItemPosition()];
                if (itemSelected2.equals("Magna")) {
                    precioGasolina = precioMagna;
                } else if (itemSelected2.equals("Premium")) {
                    precioGasolina = precioPremium;
                } else if (itemSelected2.equals("Diesel")) {
                    precioGasolina = precioDiesel;
                }
                text_result.setText("0");
                number_cant.setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });
        //start calculationg

        number_cant.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String num1 = precioGasolina;
                num2 = number_cant.getText().toString();
                String tipo_compra= spinner.getSelectedItem().toString();
                if(!num2.equals(".")) {
                    if (tipo_compra.equals("Por Litros") || tipo_compra.equals("Per Liters")) {
                        if (num2.equals(null) || num2.equals("0") || num2.equals("")) {
                            text_result.setText("$0");
                        } else if (num1.equals(null) || num1.equals("0") || num1.equals("")) {
                            text_result.setText("$0");
                        } else {
                            DecimalFormat format = new DecimalFormat("0.00");
                            double result = Double.valueOf(num1) * Double.valueOf(num2);
                            text_result.setText("$" + format.format(result) + "");
                        }
                    } else {
                        if (num2.equals(null) || num2.equals("0") || num2.equals("")) {
                            text_result.setText("0 " + getResources().getString(R.string.lb_litros));
                        } else if (num1.equals(null) || num1.equals("0") || num1.equals("")) {
                            text_result.setText("0 " + getResources().getString(R.string.lb_litros));
                        } else {
                            DecimalFormat format = new DecimalFormat("0.00");
                            double result = Double.valueOf(num2) / Double.valueOf(num1);
                            text_result.setText(format.format(result) + " " + getResources().getString(R.string.lb_litros));
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        //Create AD :D

        mAdView = (AdView) rootView.findViewById(R.id.adViewCalc2);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        return rootView;
    }


    @Override
    public void onStart() {
        super.onStart();
        updateGasolinaPrice();
    }
    public void updateGasolinaPrice() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int month = cal.get(Calendar.MONTH)+1;
        int year = cal.get(Calendar.YEAR);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String thisMonth = Integer.toString(month);
        String thisYear = Integer.toString(year);



        weatherTask.execute(thisMonth, thisYear,"mexico");
    }
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_GASOLINA = "gasolina";
            final String OWM_PREVVALUE = "prevValor";
            final String OWM_NEXTVALUE = "sigValor";
            final String OWM_VALOR = "valor";
            final String OWM_MES = "mes";
            final String OWM_ANO = "ano";




            JSONArray weatherArray = new JSONArray(forecastJsonStr);

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                String gasolina;
                String prevValue;
                String valor;
                String mes;
                String ano;
                String sigValor;

                // Get the JSON object representing the day
                JSONObject dayGas = weatherArray.getJSONObject(i);
                gasolina = dayGas.getString(OWM_GASOLINA);
                valor = dayGas.getString(OWM_VALOR);
                mes = dayGas.getString(OWM_MES);
                ano = dayGas.getString(OWM_ANO);

                //resultStrs[i] = gasolina+","+prevValue+","+valor+","+mes+","+ano+","+sigValor;
                resultStrs[i] = gasolina+", ,"+valor+","+mes+","+ano+", ";

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
                            "http://areliablewindowcleaning.com/gasolina/regions.php?";

                }

                final String QUERY_PARAM_MONTH = "m";
                final String QUERY_PARAM_YEAR = "y";
                final String QUERY_PARAM_LUGAR="lugar";
                final String QUERY_PARAM_REGIONID = "regionID";
                final String QUERY_PARAM_MODE = "mode";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM_YEAR, params[1])
                        .appendQueryParameter(QUERY_PARAM_MODE, "getRegionPrice")
                        .appendQueryParameter(QUERY_PARAM_REGIONID,getRegion)
                        .build();

                URL url = new URL(builtUri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                //Log.d("urlDebug", "url: "+ url);
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


                for (String dayForecastStr :result){
                    if (dayForecastStr != null) {
                        String[] array = dayForecastStr.split(",");
                        //gasolina|prevValue|valor|mes|ano|nextValue;
                        //gasolinaClass obj = new gasolinaClass(array[0], array[0], array[2],array[1],array[5]);
                        //mGasolinaAdapter.addGas(obj);
                        if (array[0].equals("magna")){
                            precioMagna =  array[2];
                        }
                        if (array[0].equals("premium")){
                            precioPremium = array[2];
                        }
                        if (array[0].equals("diesel")){
                            precioDiesel = array[2];
                        }
                        //Log.d("urlDebug", "precioDiesel: "+precioDiesel+",precioPremium ,"+precioPremium+",precioMagna"+precioMagna);
                    }
                }
            }
        }
    }

}