package ch.abertschi.flashonvisit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

import java.util.concurrent.Future;

import ch.abertschi.flashonvisit.view.MainActivity;

/**
 * Created by abertschi on 13.02.17.
 */
public class FeedbackServiceConnector {

    private FeedbackService mBoundFeedbackService;

    private boolean serviceIsBound;
    private Context context;

    public FeedbackServiceConnector(Context context) {
        this.context = context;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundFeedbackService = ((FeedbackService.LocalBinder) service).getService();
            System.out.println("onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName className) {
            System.out.println("onServiceDisconnected");
            mBoundFeedbackService = null;
        }
    };

    public void bindService() {
        context.bindService(new Intent(context, FeedbackService.class), mConnection, Context.BIND_AUTO_CREATE);
        serviceIsBound = true;
        System.out.println("doBindService");
    }

    public FeedbackService getService() {
        return mBoundFeedbackService;
    }

    public void unbindService() {
        if (serviceIsBound) {
            context.unbindService(mConnection);
            serviceIsBound = false;
        }
    }
}
