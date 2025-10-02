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


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView)findViewById(R.id.webview);
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("MMMM-yyyy");
        //toolbar.setBackgroundColor((Color.parseColor("#80000000")));

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        webView.getSettings().setJavaScriptEnabled(true);
        Log.d("WebViewSetup", "WebView JavaScript enabled: " + webView.getSettings().getJavaScriptEnabled());
        webView.getSettings().setDomStorageEnabled(true); // Added this line
        Log.d("WebViewSetup", "WebView DOM Storage enabled: " + webView.getSettings().getDomStorageEnabled()); // And a log for it
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
                loadApp();

            } catch (Exception e) {
                Log.e("addMyMun", "addMyMun error", e);
            }
        }
    }
    public void loadApp() {
        thisurl = BuildConfig.BASE_URL;
        Log.d("WebViewLoad", "Initial thisurl from BuildConfig.BASE_URL: " + thisurl);

        final String nameMun = mPrefs.getString("shared_munID", "");
        final String nameEdo = mPrefs.getString("shared_edoID", "");
        Log.d("WebViewLoad", "nameMun: '" + nameMun + "', nameEdo: '" + nameEdo + "'");

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("intent://")) {
                    try {
                        Context context = view.getContext();
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                        if (intent.resolveActivity(context.getPackageManager()) != null) {
                            context.startActivity(intent);
                            return true;
                        }

                        String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                        if (fallbackUrl != null) {
                            view.loadUrl(fallbackUrl);
                            return true;
                        }

                        if (intent.getPackage() != null) {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + intent.getPackage()));
                            if (marketIntent.resolveActivity(context.getPackageManager()) != null) {
                                context.startActivity(marketIntent);
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("WebView", "Error parsing intent URL", e);
                    }
                    return true;
                }

                Log.i("W", "WebViewJS Processing webview url click: " + url);
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i("W", "WebViewJS Finished loading URL: " +url);
                if (progressBar != null && progressBar.isShowing()) {
                    progressBar.dismiss();
                }

            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("WebViewError", "Error Code: " + errorCode + " Description: " + description + " Failing URL: " + failingUrl);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage(description)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
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
                Log.d("WebViewConsole", consoleMessage.message() + " -- From line " +
                        consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
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
        Log.d("WebViewLoad", "Attempting to load URL: " + thisurl);
        webView.post(new Runnable() {
            @Override
            public void run() {
                Log.d("WebViewLoad", "Final URL to load in webviewLoadURL: " + thisurl);
                webviewLoadURL(thisurl);
            }
        });



    }
    public void webviewLoadURL(String url) {
        Log.d("WebViewLoad", "webviewLoadURL called with: " + url);
        webView.clearHistory();
        webView.clearFormData();
        webView.clearCache(true);
        webView.loadUrl(url);
    }
    public String getEntityID(String edo) {

        String id="";

        for (int i=0; i<200;i++) {
            String s = ((MyApplication) this.getApplication()).getRegionesList(i);
            if(edo == s ){
                id =  ((MyApplication) this.getApplication()).getRegionesID(i);
            }

        }
        return id;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
    protected void onPause() {
        super.onPause();
        Log.w("MainActivity", "onPause");

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    public boolean isInternetAvailable(Context context) {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info == null) {
            Log.d("NetworkCheck", "NetworkInfo is null. No internet.");
            return false;
        } else {
            if (info.isConnected()) {
                Log.d("NetworkCheck", "Network is connected.");
                return true;
            } else {
                Log.d("NetworkCheck", "Network is not connected.");
                return false; // This was 'true' before, corrected to 'false'
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
        viewPager.setAdapter(adapter);
    }

}
