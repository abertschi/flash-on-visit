package ch.abertschi.flashonvisit.feedback;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.NotificationCompat;

import com.kdb.ledcontrol.LEDManager;

import ch.abertschi.flashonvisit.R;

/**
 * LED feedback service
 * <p/>
 * Created by abertschi on 11.02.17
 */
public class LedFeedback implements Feedback {

    public static final int LED_COLOR_DEFAULT = 0xffcc0000;
    public static final int DEFAULT_DURATION = 100;

    private static final int LED_NOTIFICATION_ID = 1;

    private int ledColor = LED_COLOR_DEFAULT;
    private Integer duration = new Integer(DEFAULT_DURATION);
    private Integer tempDuration;
    private LEDManager ledManager;
    private Context context;
    private boolean kernelTrigger = false;

    public LedFeedback(Context context) {
        ledManager = new LEDManager(context);
        this.context = context;
    }

    @Override
    public void exampleFeedback() {
        doFeedback(ledColor, 5000);
    }

    @Override
    public void feedback() {
        doFeedback(ledColor, 500);
    }

    protected void doFeedback(int ledColor, int delayUntilHide) {
        int feedbackDuration = duration;
        if (tempDuration != null) {
            feedbackDuration = tempDuration;
            tempDuration = null;
        }

        if (kernelTrigger && ledManager.isDeviceSupported()) {
            ledManager.setChoiseToOn();
            ledManager.ApplyBrightness(10);
            ledManager.Apply();
            new Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        public void run() {
                            ledManager.setChoiseToOff();
                            ledManager.ApplyBrightness(10);
                            ledManager.Apply();
                        }
                    },
                    feedbackDuration);
        } else {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setSmallIcon(R.mipmap.ic_launcher);
            mBuilder.setContentTitle("Flash On Visit");
            mBuilder.setPriority(Notification.PRIORITY_HIGH);

            Notification notif = mBuilder.build();

            notif.ledARGB = ledColor;
            notif.flags = Notification.FLAG_SHOW_LIGHTS;
            notif.ledOnMS = feedbackDuration;
            notif.ledOffMS = 0;
            nm.notify(LED_NOTIFICATION_ID, notif);

            new Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        public void run() {
                            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.cancel(LED_NOTIFICATION_ID);
                        }
                    },
                    delayUntilHide);
        }
    }

    public boolean isKernelTrigger() {
        return kernelTrigger;
    }

    public LedFeedback setKernelTrigger(boolean kernelTrigger) {
        this.kernelTrigger = kernelTrigger;
        return this;
    }

    public int getLedColor() {
        return ledColor;
    }

    public LedFeedback setLedColor(int ledColor) {
        this.ledColor = ledColor;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public LedFeedback setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public void setDurationForNextFeedback(int duration) {
        this.tempDuration = duration;
    }
}
