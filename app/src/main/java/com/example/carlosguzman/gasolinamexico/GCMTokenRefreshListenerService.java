package com.example.carlosguzman.gasolinamexico;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by 501820531 on 6/8/2016.
 */
public class GCMTokenRefreshListenerService extends InstanceIDListenerService {
    /**
     * when token refresh, emepzar el servicio to get myne token.
     */
    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this,GCMRegistrationIntentService.class);
        startService(intent);
    }
}

