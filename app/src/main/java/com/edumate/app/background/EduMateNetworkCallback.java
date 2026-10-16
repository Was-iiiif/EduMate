package com.edumate.app.background;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

import com.edumate.app.repository.SyncRepository;

public class EduMateNetworkCallback extends ConnectivityManager.NetworkCallback {
    private final Context context;

    public EduMateNetworkCallback(Context context) {
        this.context = context.getApplicationContext();
    }

    public static void register(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            try {
                cm.registerNetworkCallback(request, new EduMateNetworkCallback(context));
            } catch (Exception e) {
                // Ignore if security or system limit
            }
        }
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        super.onAvailable(network);
        // Internet reconnected: trigger pending sync automatically
        SyncRepository.getInstance(context).syncPendingSessions(null);
    }

    @Override
    public void onLost(@NonNull Network network) {
        super.onLost(network);
    }
}
