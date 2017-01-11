package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Created by 501820531 on 1/10/2017.
 */
public class confRegion extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_GASOLINA = "pref_gasWidget";
    SharedPreferences mPrefs;
    SharedPreferences.Editor editor;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = mPrefs.edit();
        editor.remove("getReg");
        editor.remove("getLocation");
        editor.remove("getEst");
        editor.commit();
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage( getBaseContext().getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);

    }




    @Override
    public void onResume() {
        super.onResume();
        //getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);


    }
    @Override
    public void onPause() {
        super.onPause();
        //getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        sharedPreferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    }

}

