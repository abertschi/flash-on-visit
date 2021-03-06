package ch.abertschi.flashonvisit.firebase;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ch.abertschi.flashonvisit.App;
import ch.abertschi.flashonvisit.HistoryEntry;
import ch.abertschi.flashonvisit.db.HistoryDbHelper;
import ch.abertschi.flashonvisit.feedback.FeedbackService;
import ch.abertschi.flashonvisit.view.MainActivity;

/**
 * Created by abertschi on 13.02.17.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    private FeedbackService mFeedbackService;
    private boolean mServiceIsBound;
    private SharedPreferences mPrefs;
    private List<RemoteMessage> mQueuedRequests = new LinkedList<>();

    private HistoryDbHelper mHistoryDbHelper;
    private SQLiteDatabase mDb;

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(MyFirebaseMessagingService.this);
        if (isAllowedToRun()) {
            mHistoryDbHelper = new HistoryDbHelper(this);
            mDb = mHistoryDbHelper.getWritableDatabase();
            FirebaseMessaging.getInstance().subscribeToTopic(mPrefs.getString(App.PREFS_CHANNEL, App.DEFAULT_CHANNEL));
            doBindService();
        }
    }

    private boolean isAllowedToRun() {
        return mPrefs.getBoolean(App.PREFS_START_ON_BOOT, false) ||
                (mPrefs.getBoolean(App.PREFS_ENABLED, false && MainActivity.get() != null));

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDb != null) {
            mDb.close();
        }
        doUnbindService();
    }

    private void processMessage(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            String ip = remoteMessage.getData().get("ip");
            String channel = remoteMessage.getData().get("channel");
            String historyMsg = String.format("New request in <b>%s</b> by %s", channel, ip);
            if (MainActivity.get() != null) {
                MainActivity.MessageHandler handler = MainActivity.get().getUiHandler();
                Message message = handler.obtainMessage();
                message.obj = historyMsg;
                message.what = MainActivity.MessageHandler.NEW_REQUEST;
                handler.dispatchMessage(message);
            }
            mFeedbackService.doFeedback();
            HistoryEntry entry = new HistoryEntry(historyMsg);
            mHistoryDbHelper.addHistoryEntry(mDb, entry);
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (!isAllowedToRun()) {
            return;
        }
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        if (mFeedbackService == null) {
            Log.e(TAG, "Can not do feedback because mFeedbackService is null!. Queuing requests ...");
            mQueuedRequests.add(remoteMessage);
            return;
        } else {
            processMessage(remoteMessage);
        }
    }

    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mFeedbackService = ((FeedbackService.LocalBinder) service).getService();
            MainActivity.initFeedbackServices(MyFirebaseMessagingService.this, mFeedbackService);

            Iterator<RemoteMessage> iterator = mQueuedRequests.iterator();
            while (iterator.hasNext()) {
                RemoteMessage m = iterator.next();
                processMessage(m);
                iterator.remove();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mFeedbackService = null;
        }
    };

    protected void doBindService() { // TODO: Battery optimization, because this service runs all the time
        Intent i = new Intent(this, FeedbackService.class);
        this.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        mServiceIsBound = true;
    }

    protected void doUnbindService() {
        if (mServiceIsBound) {
            this.unbindService(mConnection);
            mServiceIsBound = false;
        }
    }
}

