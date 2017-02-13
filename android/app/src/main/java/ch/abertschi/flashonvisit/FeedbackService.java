package ch.abertschi.flashonvisit;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ch.abertschi.flashonvisit.feedback.Feedback;
import ch.abertschi.flashonvisit.feedback.FlashFeedback;
import ch.abertschi.flashonvisit.feedback.LedFeedback;
import ch.abertschi.flashonvisit.feedback.VibraFeedback;

/**
 * Created by abertschi on 13.02.17.
 */
public class FeedbackService extends Service {

    private static final int FEEDBACK_DURATION = 100;

    private Handler mStatusHandler;

    public static int STATUS_HANDLER_MSG_FEEDBACK = 1;

    public class LocalBinder extends Binder {
        public FeedbackService getService() {
            return FeedbackService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    private LedFeedback ledService;
    private VibraFeedback vibraService;
    private FlashFeedback flashService;
    private Set<Feedback> feedbackServices = new LinkedHashSet<>();

    public enum TYPE {
        FLASH,
        LED,
        VIBRA
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(App.TAG_NAME, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TAG", "Service started.");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initFeedbackServices();
    }

    private void initFeedbackServices() {
        ledService = new LedFeedback(this).setDuration(FEEDBACK_DURATION);
        vibraService = new VibraFeedback(this);
        flashService = new FlashFeedback(this);
        addFeedbackService(TYPE.FLASH);
        addFeedbackService(TYPE.LED);
        addFeedbackService(TYPE.VIBRA);
    }

    public void addFeedbackService(TYPE type) {
        feedbackServices.add(getFeedbackServiceImpl(type));
    }

    public void removeFeedbackService(TYPE type) {
        feedbackServices.remove(getFeedbackServiceImpl(type));
    }

    public boolean isServiceActive(TYPE type) {
        return feedbackServices.contains(getFeedbackServiceImpl(type));
    }

    public Set<TYPE> getActiveFeedbackServices() {
        Set<TYPE> active = new LinkedHashSet<>();
        if (isServiceActive(TYPE.FLASH)) active.add(TYPE.FLASH);
        if (isServiceActive(TYPE.LED)) active.add(TYPE.LED);
        if (isServiceActive(TYPE.VIBRA)) active.add(TYPE.VIBRA);
        return active;
    }

    private <T extends Feedback> T getFeedbackServiceImpl(TYPE type) {
        switch (type) {
            case FLASH:
                return (T) flashService;
            case LED:
                return (T) ledService;
            case VIBRA:
                return (T) vibraService;
            default:
                throw new UnsupportedOperationException("Unknown feedback service");
        }
    }

    public void doFeedback() {
        for (Feedback s : feedbackServices) {
            s.feedback();

        }
    }

    private void publishStatusMessage(String message) {
        if (mStatusHandler != null) {
            Message m = mStatusHandler.obtainMessage();
            m.what = STATUS_HANDLER_MSG_FEEDBACK;
            m.obj = message;
            mStatusHandler.dispatchMessage(m);
        }
    }

    public void doFeedback(TYPE... types) {
        for (TYPE s : types) {
            getFeedbackServiceImpl(s).feedback();
        }
    }

    public void doExampleFeedback(TYPE... types) {
        for (TYPE s : types) {
            getFeedbackServiceImpl(s).exampleFeedback();
        }
    }

    public void doExampleFeedback() {
        for (Feedback s : feedbackServices) {
            s.exampleFeedback();
        }
    }

    public FeedbackService setLedKernelHack(boolean enabled) {
        this.ledService.setKernelTrigger(enabled);
        return this;
    }

    public FeedbackService setLedColor(int ledColor) {
        this.ledService.setLedColor(ledColor);
        return this;
    }

    public Handler getmStatusHandler() {
        return mStatusHandler;
    }

    public FeedbackService setmStatusHandler(Handler mStatusHandler) {
        this.mStatusHandler = mStatusHandler;
        return this;
    }
}
