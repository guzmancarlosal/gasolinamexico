package com.ttpCorp.carlosguzman.preciogasolinamexico;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.Arrays;
import java.util.List;


public class fav extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }

    }
    public static class DetailFragment extends Fragment {
        String regionID;
        private AdView mAdView;
        SharedPreferences prefs;
        private gasAdapter mGasolinaAdapter;
        public DetailFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            final View rootView = inflater.inflate(R.layout.activity_fav, container, false);
            SharedPreferences prefs = getActivity().getSharedPreferences("Favoritos", 0);
            String all_vals =prefs.getString("Favoritos", "");
            if (all_vals ==""){
                all_vals=",";
            }
            new DownloadJSON(getActivity(),rootView,"getFavoritos").execute(""+all_vals.substring(1));
            mGasolinaAdapter = new gasAdapter(getActivity(),R.layout.list_view_fav);

            mAdView = (AdView) rootView.findViewById(R.id.adView3);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

            return rootView;
        }



    }

}
