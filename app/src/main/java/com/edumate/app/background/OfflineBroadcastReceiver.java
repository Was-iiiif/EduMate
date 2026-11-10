package com.edumate.app.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.Toast;

/**
 * Displays a one-time offline message when connectivity is lost.
 * Kept separate from EduMateNetworkCallback, which continues handling cloud sync.
 */
public class OfflineBroadcastReceiver extends BroadcastReceiver {
    private static final String PREFS = "edumate_network_state";
    private static final String KEY_WAS_OFFLINE = "was_offline";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean online = hasValidatedInternet(context);
        SharedPreferences state = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean wasOffline = state.getBoolean(KEY_WAS_OFFLINE, false);

        if (!online && !wasOffline) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_LONG).show();
        }
        state.edit().putBoolean(KEY_WAS_OFFLINE, !online).apply();
    }

    private boolean hasValidatedInternet(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;

        Network activeNetwork = manager.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = manager.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
