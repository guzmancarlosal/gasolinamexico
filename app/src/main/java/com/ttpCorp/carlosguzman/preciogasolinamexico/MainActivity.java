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
import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
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
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import androidx.webkit.WebViewAssetLoader;
import android.widget.Toast;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
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
    public Boolean isAppOffline = false;
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
        int targetHour = mPrefs.getInt("notification_hour", 8);
        if (targetHour != -1) {
            scheduleDailyGasWorker(targetHour);
        }
        requestNotificationPermissionIfNeeded();

        try {
            MobileAds.initialize(this, initializationStatus -> {});
            AdView mAdView = findViewById(R.id.adView);
            if (mAdView != null) {
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "AdMob initialization error", e);
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
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

        @JavascriptInterface
        public void updateWidgetData(String stateId, String munId, String munName, String lowestMagna, String lowestPremium) {
            try {
                SharedPreferences.Editor editor = mPrefs.edit();
                if (stateId != null && !stateId.isEmpty()) editor.putString("shared_edoID", stateId);
                if (munId != null && !munId.isEmpty()) editor.putString("shared_munID", munId);
                if (munName != null) editor.putString("gasApp_municipioNombre", munName);
                if (lowestMagna != null) editor.putString("widget_lowestMagna", lowestMagna);
                if (lowestPremium != null) editor.putString("widget_lowestPremium", lowestPremium);
                editor.apply();

                Intent intent = new Intent(activity, GasAppWidgetProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = AppWidgetManager.getInstance(activity.getApplication())
                        .getAppWidgetIds(new ComponentName(activity.getApplication(), GasAppWidgetProvider.class));
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                activity.sendBroadcast(intent);
            } catch (Exception e) {
                Log.e("JSI", "updateWidgetData error", e);
            }
        }

        @JavascriptInterface
        public void requestNotificationPermission() {
            requestNotificationPermissionIfNeeded();
        }

        @JavascriptInterface
        public void setNotificationTime(int hourOfDay) {
            try {
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putInt("notification_hour", hourOfDay);
                editor.apply();

                if (hourOfDay == -1) {
                    WorkManager.getInstance(activity).cancelUniqueWork("Daily8AMGasWork");
                    Log.d("MainActivity", "Daily gas notification cancelled.");
                } else {
                    ((MainActivity) activity).scheduleDailyGasWorker(hourOfDay);
                }
            } catch (Exception e) {
                Log.e("JSI", "setNotificationTime error", e);
            }
        }

        @JavascriptInterface
        public void openMap(String address) {
            try {
                String encoded = Uri.encode(address);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + encoded));
                activity.startActivity(intent);
            } catch (Exception e) {
                Log.e("JSI", "openMap error", e);
            }
        }
    }
    public void loadApp() {
        thisurl = BuildConfig.BASE_URL;
        Log.d("WebViewLoad", "Initial thisurl from BuildConfig.BASE_URL: " + thisurl);

        final String nameMun = mPrefs.getString("shared_munID", "");
        final String nameEdo = mPrefs.getString("shared_edoID", "");
        Log.d("WebViewLoad", "nameMun: '" + nameMun + "', nameEdo: '" + nameEdo + "'");

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                WebResourceResponse response = assetLoader.shouldInterceptRequest(request.getUrl());
                if (response != null) {
                    return response;
                }
                return super.shouldInterceptRequest(view, request);
            }

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
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setAllowFileAccess(true);
            webView.getSettings().setAllowFileAccessFromFileURLs(true);
            webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
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

        if (nameMun != null && !nameMun.isEmpty() && nameEdo != null && !nameEdo.isEmpty()){
            if (thisurl.startsWith("file://") || thisurl.startsWith("https://appassets")) {
                thisurl = thisurl + "#/?estadoID="+nameEdo+"&municipioID="+nameMun;
            } else {
                thisurl = thisurl + "?estadoID="+nameEdo+"&municipioID="+nameMun;
            }
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
        adapter.addFragment(firstTab, getResources().getString(R.string.lb_precio));
        adapter.addFragment(secondTab, "Favoritos");
        viewPager.setAdapter(adapter);
    }

    public void scheduleDailyGasWorker(int targetHour) {
        try {
            Calendar currentDate = Calendar.getInstance();
            Calendar dueDate = Calendar.getInstance();
            dueDate.set(Calendar.HOUR_OF_DAY, targetHour);
            dueDate.set(Calendar.MINUTE, 0);
            dueDate.set(Calendar.SECOND, 0);

            if (dueDate.before(currentDate)) {
                dueDate.add(Calendar.HOUR_OF_DAY, 24);
            }

            long timeDiff = dueDate.getTimeInMillis() - currentDate.getTimeInMillis();

            PeriodicWorkRequest dailyWorkRequest = new PeriodicWorkRequest.Builder(
                    DailyGasWorker.class,
                    24, TimeUnit.HOURS
            )
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "Daily8AMGasWork",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    dailyWorkRequest
            );
            Log.d("MainActivity", "Daily Gas Work scheduled for hour: " + targetHour);
        } catch (Exception e) {
            Log.e("MainActivity", "Error scheduling daily gas work", e);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
