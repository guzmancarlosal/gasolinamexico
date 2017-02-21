package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    SharedPreferences mPrefs;
    public int munIDs [] = new int[50];
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private static final String application_offline_message = "gasolina_offline_message";
    private static final String gasolina_offline = "gasolina_offline";
    private static final String gasolina_custom_message = "gasolina_custom_message";
    public String welcomeMessage;
    public String customMjs;
    public Boolean isAppOffline = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (isInternetAvailable(getApplicationContext())) //returns true if internet available
        {
        } else {
            Toast.makeText(getApplicationContext(), "Necesitas coneccion a Internet", Toast.LENGTH_LONG).show();
        }
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("MMMM-yyyy");
        String formattedDate = df.format(c.getTime());
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setSubtitle(formattedDate);
        toolbar.setLogo(R.mipmap.ic_launcher);
        //get my Firebaseconnection
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        long cacheExpiration = 50;

        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mFirebaseRemoteConfig.activateFetched();
                        } else {

                        }
                        welcomeMessage = mFirebaseRemoteConfig.getString(application_offline_message);
                        isAppOffline = mFirebaseRemoteConfig.getBoolean(gasolina_offline);
                        if (isAppOffline) {
                            new AlertDialog.Builder(MainActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Atención").setMessage(welcomeMessage).setPositiveButton(
                                    "Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {

                                            dialog.dismiss();
                                            finish();
                                        }
                                    }).show();

                        }
                        customMjs = mFirebaseRemoteConfig.getString(gasolina_custom_message);
                        if(!customMjs.isEmpty()) {
                            new AlertDialog.Builder(MainActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Atención").setMessage(customMjs).setPositiveButton(
                                    "Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {

                                            dialog.dismiss();
                                        }
                                    }).show();
                        }

                    }
                });
        //pop up de inicio
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Activity activity = this;
        //Entendido
        final Boolean welcomeScreen1 = mPrefs.getBoolean("entendido", false);
        if (!welcomeScreen1) {
            String whatsNewTitle = getResources().getString(R.string.aviso_title);
            String whatsNewText = getResources().getString(R.string.aviso);
            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(whatsNewTitle).setMessage(whatsNewText).setPositiveButton(
                    R.string.entendido, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean("entendido", true);
            editor.commit(); // Very important to save the preference
        }//fin entendido
        //evaluanos inicio
        final Boolean evaluanos = mPrefs.getBoolean("evaluanos", false);
        int counterEval  = mPrefs.getInt("counterEval", 0);
        if (!evaluanos){
            counterEval++;
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putInt("counterEval", counterEval);
            editor.commit();
        }

        if (!evaluanos && (counterEval % 5) ==0) {
            String whatsNewTitle = getResources().getString(R.string.gracias);
            String whatsNewText = getResources().getString(R.string.gracias_text);
            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(whatsNewTitle).setMessage(whatsNewText).setPositiveButton(
                    R.string.si, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putBoolean("evaluanos", true);
                            editor.commit();
                            MainActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.ttpCorp.carlosguzman.gasolinamexico")));

                            dialog.dismiss();
                        }
                    }).setNegativeButton(
                    R.string.Luego, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();
                        }
                    }
            ).show();
        }//fin entendido
        //evaluanos Fin

        //Preparing views
        final String getRegion = "getReg";
        final String getEstado = "getEst";
        final String getMunicipio = "getMun";
        final String getLoc= "getLocation";
        final Boolean getLocation = mPrefs.getBoolean(getLoc, false);
        final String myRegion = mPrefs.getString("myReg", "");
        if (!getLocation) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.dialog_layout, null);
            final Spinner estadoBox = (Spinner) layout.findViewById(R.id.dd_estado);

            //Building dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(layout);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Save Estado preference
                    SharedPreferences.Editor editor = mPrefs.edit();
                    String savedEstado = estadoBox.getSelectedItem().toString();
                    editor.putString(getEstado, savedEstado);
                    editor.commit();
                    String sstadoID = getEntityID(savedEstado);
                    //end save Estado
                    //prepare 2nd popup
                    LayoutInflater inflater1 = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    View layout1 = inflater1.inflate(R.layout.dialog_mun, null);
                    final Spinner municipioBox = (Spinner) layout1.findViewById(R.id.dd_municipio);
                    new DownloadJSON(activity, layout1, "getMunicipio").execute(sstadoID);
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
                    builder2.setView(layout1);
                    builder2.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog2, int which) {

                            SharedPreferences.Editor editor = mPrefs.edit();
                            String savedRegion = municipioBox.getSelectedItem().toString();
                            String regionID = getEntityID(savedRegion);
                            editor.putString(getRegion, regionID);
                            editor.putBoolean(getLoc, true);
                            editor.putString("myReg", savedRegion);
                            TextView tv = (TextView)findViewById(R.id.title_Tag);
                            tv.setText("Region: "+savedRegion);
                            editor.commit();
                            //Log.d("savedPref", "savedPref Municipio." +savedRegion );
                            viewPager = (ViewPager) findViewById(R.id.viewpager);
                            setupViewPager(viewPager);
                            tabLayout = (TabLayout) findViewById(R.id.tabs);
                            tabLayout.setupWithViewPager(viewPager);
                            setupTabIcons();
                        }
                    });
                    AlertDialog dialog2 = builder2.create();
                    dialog2.show();

                }
            });
            new DownloadJSON(this,layout,"getEstado").execute();
            AlertDialog dialog = builder.create();
            dialog.show();
        } else{
            viewPager = (ViewPager) findViewById(R.id.viewpager);
            setupViewPager(viewPager);
            tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);
            setupTabIcons();
            if(myRegion != "") {
                TextView tv = (TextView)findViewById(R.id.title_Tag);
                tv.setText("Region: "+myRegion);
            }

        }
        //fin del popup

    }
    public String getEntityID(String edo) {

        String id="";

        for (int i=0; i<200;i++) {
            String s = ((MyApplication) this.getApplication()).getRegionesList(i);
            Log.d("savedPref", "looping: " + s + "pos:"+i);
            if(edo == s ){
                id =  ((MyApplication) this.getApplication()).getRegionesID(i);
                Log.d("savedPref", "savedPref Estado ID" + s);
            }

        }
        return id;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("resumming","step1");
        if (getIntent().getExtras() != null) {
            Bundle b = getIntent().getExtras();
            boolean cameFromNotification = b.getBoolean("fromNotification",false);
            String alertMsj = b.getString("messageAlert");
            Log.d("resumming","step2"+cameFromNotification);
            Log.d("resumming","step3"+alertMsj);
            if (cameFromNotification==true) {
                new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Precio Gasolina").setMessage(alertMsj).setPositiveButton(
                        R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w("MainActivity", "onPause");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //Toast.makeText(MainActivity.this, "Selected", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, confWidget.class));
            return true;
        }
        if (id == R.id.action_region) {
            //Toast.makeText(MainActivity.this, "Selected", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, confRegion.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isInternetAvailable(Context context) {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info == null) {
            Log.d("My Activity", "no internet connection");
            return false;
        } else {
            if (info.isConnected()) {
                Log.d("My Activity", " internet connection available...");
                return true;
            } else {
                Log.d("My Activity", " internet connection");
                return true;
            }

        }

    }
    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        MainActivityFragment firstTab = new MainActivityFragment();
        Bundle bundle = new Bundle();
        bundle.putString("zone", "mexico");
        firstTab.setArguments(bundle);

       /* MainActivityFragment secondTab = new MainActivityFragment();
        Bundle bundle2 = new Bundle();
        bundle2.putString("zone", "frontera");
        secondTab.setArguments(bundle2);*/


        adapter.addFragment(firstTab, getResources().getString(R.string.lb_precio));
        //adapter.addFragment(secondTab, "Frontera");
        adapter.addFragment(new CalculadoraActivity(), getResources().getString(R.string.lb_calculadora));
        viewPager.setAdapter(adapter);
    }
    private void setupTabIcons() {
        /*TextView tabThree = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabThree.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.caculator, 0, 0);
        tabLayout.getTabAt(2).setCustomView(tabThree);*/
    }
}
