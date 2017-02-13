package ch.abertschi.flashonvisit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by abertschi on 13.02.17.
 */
public class StartOnBootService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(App.TAG_NAME, "StartOnBootService loading ...");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(App.PREFS_START_ON_BOOT, false)) {
            Intent serviceIntent = new Intent(context, FeedbackService.class);
            context.startService(serviceIntent);
        }
    }
}
