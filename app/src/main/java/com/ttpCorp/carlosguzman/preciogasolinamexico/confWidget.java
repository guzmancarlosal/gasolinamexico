package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;


public class confWidget extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_GASOLINA = "pref_gasWidget";
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_widget);
    }




    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);


    }
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        sharedPreferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        switch (key){
            case KEY_PREF_GASOLINA:
                String valor_Gasolina = sharedPreferences.getString(KEY_PREF_GASOLINA, null);
                //UPDATE WIDGET ON CHANGE PEREFERENCES :)
                Intent intent = new Intent(this, GasAppWidgetProvider.class);
                intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
                int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), GasAppWidgetProvider.class));
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
                sendBroadcast(intent);
                Toast.makeText(getApplicationContext(), "Widget Actualizado a: "+valor_Gasolina, Toast.LENGTH_LONG).show();
                break;

        }
    }

}
