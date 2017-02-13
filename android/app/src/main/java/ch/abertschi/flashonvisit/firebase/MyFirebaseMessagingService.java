package ch.abertschi.flashonvisit.firebase;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import ch.abertschi.flashonvisit.App;
import ch.abertschi.flashonvisit.FeedbackService;

/**
 * Created by abertschi on 13.02.17.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    private FeedbackService mFeedbackService;
    private boolean mServiceIsBound;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(MyFirebaseMessagingService.this);
        if (prefs.getBoolean(App.PREFS_START_ON_BOOT, false) || prefs.getBoolean(App.PREFS_ENABLED, false)) {
            doBindService();
            FirebaseMessaging.getInstance().subscribeToTopic(prefs.getString(App.PREFS_CHANNEL, App.DEFAULT_CHANNEL));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (mFeedbackService == null) {
            Log.e(TAG, "Can not do feedback because mFeedbackService is null!");
            return;
        }

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            String ip = remoteMessage.getData().get("ip");
            String channel = remoteMessage.getData().get("channel");
            mFeedbackService.doFeedback();
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mFeedbackService = ((FeedbackService.LocalBinder) service).getService();
            mFeedbackService.setLedKernelHack(prefs.getBoolean(App.PREFS_LED_KERNEL_HACK_ENABLED, false));
            mFeedbackService.setLedColor(prefs.getInt(App.PREFS_LED_COLOR, App.LED_COLOR_DEFAULT));

            System.out.println("onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName className) {
            System.out.println("onServiceDisconnected");
            mFeedbackService = null;
        }
    };

    protected void doBindService() { // TODO: Battery optimization, because this service runs all the time
        this.bindService(new Intent(this, FeedbackService.class), mConnection, Context.BIND_IMPORTANT);
        mServiceIsBound = true;
        System.out.println("doBindService");
    }

    protected void doUnbindService() {
        if (mServiceIsBound) {
            this.unbindService(mConnection);
            mServiceIsBound = false;
        }
    }
}

