package ch.abertschi.flashonvisit.feedback;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;

import com.kdb.ledcontrol.LEDManager;

import java.util.Map;

import ch.abertschi.flashonvisit.R;

/**
 * Created by abertschi on 11.02.17
 */
public class LedFeedback implements FeedbackService {

    private static final int LED_COLOR_DEFAULT = 0xffcc0000;
    private static final int DELAY_UNTIL_HIDE_DEFAULT = 500;
    private static final int LED_NOTIFICATION_ID = 1;

    private LEDManager ledManager;
    private Context context;
    private boolean kernelTrigger = false;
    private int ledColor;

    public LedFeedback(Context context) {
        ledManager = new LEDManager(context);
        this.context = context;
    }

    @Override
    public void exampleFeedback(Map<String, Object> params) {
        doFeedback(ledColor, 5000);
    }

    @Override
    public void feedback(Map<String, Object> params) {
        doFeedback(ledColor, 500);
    }

    protected void doFeedback(int ledColor, int delayUntilHide) {
        if (ledManager.isDeviceSupported() && kernelTrigger) {
            ledManager.setChoiseToOn();
            ledManager.ApplyBrightness(10);
            ledManager.Apply();
            new Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            ledManager.setChoiseToOff();
                            ledManager.ApplyBrightness(10);
                            ledManager.Apply();
                        }
                    },
                    100);
        } else {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setSmallIcon(R.mipmap.ic_launcher);
            mBuilder.setContentTitle("Flash On Visit");
            mBuilder.setPriority(Notification.PRIORITY_HIGH);

            Notification notif = mBuilder.build();

            notif.ledARGB = ledColor;
            notif.flags = Notification.FLAG_SHOW_LIGHTS;
            notif.ledOnMS = 100;
            notif.ledOffMS = 0;
            nm.notify(LED_NOTIFICATION_ID, notif);

            new Handler().postDelayed(
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
}
