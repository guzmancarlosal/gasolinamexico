package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.webkit.ConsoleMessage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    private ViewPager viewPager;
    SharedPreferences mPrefs;
    SharedPreferences prefs;
    public int munIDs [] = new int[50];
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private static final String application_offline_message = "gasolina_offline_message";
    private static final String gasolina_offline = "gasolina_offline";
    private static final String gasolina_custom_message = "gasolina_custom_message";
    public String welcomeMessage;
    public String customMjs;
    public Boolean isAppOffline = true;
    public WebView webView;
    public String thisurl;
    public ProgressDialog progressBar;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                // You can optionally check the initialization status here
                // and take actions if needed.
                // For example, you can start loading ads once initialization is complete.
            }
        });


        webView = (WebView)findViewById(R.id.webview);
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("MMMM-yyyy");
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        //toolbar.setBackgroundColor((Color.parseColor("#80000000")));

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebViewJavaScriptInterface(this), "app");
        //final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //get my Firebaseconnection
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(BuildConfig.DEBUG ? 0 : 3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        final Activity activity = this;
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d("FirebaseConfig", "Config params updated: " + updated);
                        } else {
                            Log.d("FirebaseConfig", "Fetch failed");
                        }
                        welcomeMessage = mFirebaseRemoteConfig.getString(application_offline_message);
                        isAppOffline = mFirebaseRemoteConfig.getBoolean(gasolina_offline);
                        if (isAppOffline) {
                            new AlertDialog.Builder(MainActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Atención").setMessage(welcomeMessage).setPositiveButton(
                                    "Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {

                                            dialog.dismiss();
                                            activity.finish();
                                            System.exit(0);
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

        //Entendido
        final Boolean welcomeScreen1 = mPrefs.getBoolean("entendido", false);
        if (!welcomeScreen1) {
            String whatsNewTitle = getResources().getString(R.string.aviso_title);
            String whatsNewText = getResources().getString(R.string.aviso);
            AlertDialog dialog  = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(whatsNewTitle).setMessage(whatsNewText).setPositiveButton(
                    "OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create();
            dialog.setOnShowListener(dlg -> {
                // elige un color que contraste (ej. negro o tu primario)
                int color = ContextCompat.getColor(this, android.R.color.black);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                // opcional: negativo / neutral si los usas
                // dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
            });
            dialog.show();
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

        if (!evaluanos && (counterEval % 3) ==0) {
            String whatsNewTitle = getResources().getString(R.string.gracias);
            String whatsNewText = getResources().getString(R.string.gracias_text);
            AlertDialog dialog2 = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(whatsNewTitle).setMessage(whatsNewText).setPositiveButton(
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
            ).create();
            dialog2.setOnShowListener(dlg -> {
                // elige un color que contraste (ej. negro o tu primario)
                int color = ContextCompat.getColor(this, android.R.color.black);
                dialog2.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                // opcional: negativo / neutral si los usas
                // dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
            });
            dialog2.show();
        }//fin entendido
        //evaluanos Fin


        //get all shared preferences and check them.
        Map<String,?> keys = mPrefs.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            Log.d("DebugGasolina values",entry.getKey() + ": " + entry.getValue().toString());
        }
        //adding webview
        if (isInternetAvailable(getApplicationContext())) //returns true if internet available
        {
            loadApp();

        } else {
            AlertDialog dialog = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Alerta").setMessage("Necesitas Coneccion a Internet").setPositiveButton(
                    R.string.entendido, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finish();
                            System.exit(0);
                        }
                    }).create();

            dialog.setOnShowListener(dlg -> {
                int color = ContextCompat.getColor(this, android.R.color.black);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);

            });

            dialog.show();
        }

        //this line removes all sharedPreferences.
        //PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().clear().apply();


        //Preparing views
        /*final String getRegion = "getReg";
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
                            final String savedRegion = municipioBox.getSelectedItem().toString();
                            String regionID = getEntityID(savedRegion);
                            editor.putString(getRegion, regionID);
                            editor.putBoolean(getLoc, true);
                            editor.putString("myReg", savedRegion);
                            TextView tv = (TextView)findViewById(R.id.title_Tag);
                            tv.setText("Región: "+savedRegion);
                            editor.commit();
                            //Log.d("savedPref", "savedPref Municipio." +savedRegion );
                            viewPager = (ViewPager) findViewById(R.id.viewpager);
                            setupViewPager(viewPager);
                            tabLayout = (TabLayout) findViewById(R.id.tabs);
                            tabLayout.setupWithViewPager(viewPager);
                            setupTabIcons();
                            Log.d("favoritos2"," hola a Nah"+savedRegion);
                            Log.d("favoritos3"," hola a Nah"+list);
                            if (list.contains(savedRegion)) {
                                fab.setVisibility(View.INVISIBLE);
                            } else{
                                fab.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (addLike(savedRegion)) {
                                            Snackbar.make(view, "Region Agregada a Favoritos", Snackbar.LENGTH_LONG)
                                                    .setAction("Action", null).show();
                                            //fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_heartfull));
                                            fab.setVisibility(View.INVISIBLE);
                                            saveArray(savedRegion);
                                        }

                                    }
                                });
                            }
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
                SharedPreferences.Editor editor = mPrefs.edit();
                TextView tv = (TextView)findViewById(R.id.title_Tag);
                tv.setText("Region: "+myRegion);
                editor.putString("myReg", myRegion);
                editor.commit();
            }

        }
        //fin del popup

        if (list.contains(myRegion)) {
            fab.setVisibility(View.INVISIBLE);
        } else{
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (addLike(myRegion)) {
                        Snackbar.make(view, "Region Agregada a Favoritos", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        //fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_heartfull));
                        fab.setVisibility(View.INVISIBLE);
                        saveArray(myRegion);
                    }

                }
            });
        }*/

    }
    public class WebViewJavaScriptInterface {
        private final Activity activity;

        public WebViewJavaScriptInterface(Activity activity){
            this.activity = activity;
        }

        @JavascriptInterface
        public void loadApp() {
            activity.runOnUiThread(() -> {
                try {
                    // Llama al metodo de tu Activity que refresca el WebView / UI
                    ((MainActivity) activity).loadApp();
                } catch (Exception e) {
                    Log.e("JSI", "loadApp() error", e);
                }
            });
        }

        @JavascriptInterface
        public void addMyMun(String mun, String edo){
            Log.d("addMyMun", "addMyMun here " +mun+ " " +edo );
            try {
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString("shared_edoID", edo);
                editor.putString("shared_munID", mun);
                editor.commit();
                Log.d("addMyMun", "addMyMun commited " +mun+ " " +edo );
                // (opcional) ya puedes forzar el refresh desde aquí también:
                loadApp();

            } catch (Exception e) {
                Log.e("addMyMun", "addMyMun error", e);
            }
        }
    }
    public void loadApp() {
        thisurl = BuildConfig.BASE_URL + "/precio.cfm";
        // final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this).create(); // Removed this line
        //progressBar = ProgressDialog.show(this,"Precio Gasolina Mexico", "Cargando...");

        final String nameMun = mPrefs.getString("shared_munID", "");
        final String nameEdo = mPrefs.getString("shared_edoID", "");
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //Log.i(TAG, "Processing webview url click...");
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //Log.i(TAG, "Finished loading URL: " +url);
                if (progressBar.isShowing()) {
                    progressBar.dismiss();
                }

            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                //Toast.makeText(, "Oh no! " + description, Toast.LENGTH_SHORT).show();
                // Create and show a new AlertDialog on error
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage(description)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Simply dismiss the dialog, or add other error handling logic if needed.
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });
        webView.addJavascriptInterface(new WebViewJavaScriptInterface(this), "app");
        webView.setWebContentsDebuggingEnabled(true);
        try {
            webView.getSettings().setJavaScriptEnabled(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {

                return true;
            }
        });

        if (nameMun != "" && nameEdo != ""){
            thisurl = thisurl + "?estadoID="+nameEdo+"&municipioID="+nameMun;

        }else{
            PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().remove("shared_munID").commit();
            PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().remove("shared_edoID").commit();
            Log.d("DebugGasolina method","Reloading...3.2");
        }

        webView.post(new Runnable() {
            @Override
            public void run() {
                webviewLoadURL(thisurl);
            }
        });



    }
    public void webviewLoadURL(String url) {

        webView.clearHistory();
        webView.clearFormData();
        webView.clearCache(true);
        webView.loadUrl(url);
    }
    public String getEntityID(String edo) {

        String id="";

        for (int i=0; i<200;i++) {
            String s = ((MyApplication) this.getApplication()).getRegionesList(i);
            //Log.d("savedPref", "looping: " + s + "pos:"+i);
            if(edo == s ){
                id =  ((MyApplication) this.getApplication()).getRegionesID(i);
                //Log.d("savedPref", "savedPref Estado ID" + s);
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
        Intent fcmIntent = getIntent();
        if (fcmIntent.getExtras() != null) {
           Bundle b = getIntent().getExtras();
           boolean cameFromNotification = b.getBoolean("fromNotification",false);
           String alertMsj = b.getString("messageAlert");
           if (cameFromNotification) {
                   AlertDialog dialog = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Precio Gasolina").setMessage(alertMsj).setPositiveButton(
                                   R.string.ok, new DialogInterface.OnClickListener() {
                                           public void onClick(DialogInterface dialog, int which) {
                                                   dialog.dismiss();
                                               }
                                       }).create();
               dialog.setOnShowListener(dlg -> {
                   int color = ContextCompat.getColor(this, android.R.color.black);
                   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);

               });

               dialog.show();
               }
           }
        getIntent().removeExtra("messageAlert");
        getIntent().removeExtra("fromNotification");

    }
    @Override
    protected void onNewIntent(Intent intent)   {
        super.onNewIntent(intent);
        Intent fcmIntent = getIntent();
        //Log.d("resumming","step1");
        if (fcmIntent.getExtras() != null) {
            Bundle b = getIntent().getExtras();
            boolean cameFromNotification = b.getBoolean("fromNotification",false);
            String alertMsj = b.getString("messageAlert");
            if (cameFromNotification) {
                AlertDialog dialog = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Precio Gasolina").setMessage(alertMsj).setPositiveButton(
                        R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                dialog.setOnShowListener(dlg -> {
                    int color = ContextCompat.getColor(this, android.R.color.black);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                });

                dialog.show();

            }
        }
        getIntent().removeExtra("messageAlert");
        getIntent().removeExtra("fromNotification");

    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.w("MainActivity", "onPause");

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    public boolean isInternetAvailable(Context context) {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info == null) {
            return false;
        } else {
            if (info.isConnected()) {
                return true;
            } else {
                return true; // This should likely be false if not connected
            }

        }

    }
    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        MainActivityFragment firstTab = new MainActivityFragment();
        Bundle bundle = new Bundle();
        bundle.putString("zone", "mexico");
        firstTab.setArguments(bundle);

        fav.DetailFragment secondTab = new fav.DetailFragment();
        Bundle bundle2 = new Bundle();
        bundle2.putString("EXTRA_TITLE", "favoritos");
        secondTab.setArguments(bundle2);



        adapter.addFragment(firstTab, getResources().getString(R.string.lb_precio));
        adapter.addFragment(secondTab, "Favoritos");
        adapter.addFragment(new CalculadoraActivity(), getResources().getString(R.string.lb_calculadora));
        viewPager.setAdapter(adapter);
    }



}
