package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by 501820531 on 1/6/2017.
 */
// Download JSON file AsyncTask
public class DownloadJSON extends AsyncTask<String, Void, String[]> {
    private Activity mActivity;
    private View mContext;
    private String thisMethod;
    String LOG_TAG = "DoBackground";
    public DownloadJSON(Activity myActivity, View myContext, String method){
        this.mActivity=myActivity;
        this.mContext=myContext;
        this.thisMethod = method;

    }
    @Override
    public String[] doInBackground(String... params) {
        // Locate the WorldPopulation Class

        String thisurl = "";
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String forecastJsonStr = null;
        if (thisMethod == "getEstado") {
            thisurl = "http://areliablewindowcleaning.com/gasolina/regions.php?mode=getEstados";
        }else if(thisMethod == "getMunicipio") {
            thisurl = "http://areliablewindowcleaning.com/gasolina/regions.php?mode=getMunicipios&estadoID="+params[0];
        }else {

        }
        //Log.d(LOG_TAG, "url "+thisurl+" method "+thisMethod);
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

            return getDataFromJson(forecastJsonStr);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error getting data from JSON", e);
            e.printStackTrace();
        }
        return null;

    }
    private  String[] getDataFromJson(String forecastJsonStr)
            throws JSONException {
        JSONArray weatherArray = new JSONArray(forecastJsonStr);
        final String OWM_VALOR = "nombre";
        final String OWM_ID_REGION = "idregion";
        final String OWM_ID = "id";
        int getL = weatherArray.length();

        String[] resultStrs = new String[getL];
        if (thisMethod == "getEstado") {
            for(int i = 0; i < weatherArray.length(); i++) {
                String estado;
                String id;
                JSONObject dayGas = weatherArray.getJSONObject(i);
                estado = dayGas.getString(OWM_VALOR);
                id = dayGas.getString(OWM_ID);
                resultStrs[i] =estado+","+id;
            }

        }else if(thisMethod == "getMunicipio") {
            String estado;
            String id;
            for(int i = 0; i < weatherArray.length(); i++) {
                JSONObject dayGas = weatherArray.getJSONObject(i);
                estado = dayGas.getString(OWM_VALOR);
                id = dayGas.getString(OWM_ID_REGION);
                resultStrs[i] =estado+","+id;
                //Log.d(LOG_TAG, "idRegion."+id);

            }
        }
        //Log.d(LOG_TAG, "Metodo:"+thisMethod+" Resultado:" + resultStrs);
        for (String s : resultStrs) {

        }
        return resultStrs;

    }
    @Override
    protected void onPostExecute(String[] result) {
        //Log.d(LOG_TAG, "idRegion."+result);
        String [] arrayEntity = new String [result.length];
        String [] arrayID = new String [result.length];
        int i=0;
        for (String dayForecastStr :result) {
            String[] tmparray = dayForecastStr.split(",");
            arrayEntity[i]= tmparray[0];
            arrayID[i]= tmparray[1];
            ((MyApplication) mActivity.getApplication()).setRegionesList(tmparray[0],i);
            ((MyApplication) mActivity.getApplication()).setRegionesIDs(tmparray[1],i);
            i++;
        }
        if (result != null){
            if (thisMethod =="getEstado") {
                Spinner autoInstitute = (Spinner) mContext.findViewById(R.id.dd_estado);
                final ArrayAdapter<String> instituteApapdter =
                        new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_1, arrayEntity);
                autoInstitute.setAdapter(instituteApapdter);

            } else if (thisMethod =="getMunicipio") {

                Spinner autoInstitute1 = (Spinner) mContext.findViewById(R.id.dd_municipio);
                Log.d(LOG_TAG, "Lo hicimos en postExecute.");
                //((MainActivity) mContext.getApplication()).setSomeVariable("foo");
                final ArrayAdapter<String> instituteApapdter1 =
                        new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_1, arrayEntity);
                autoInstitute1.setAdapter(instituteApapdter1);

            }


        }
    }
}
