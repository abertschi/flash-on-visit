package ch.abertschi.flashonvisit.feedback;

import android.content.Context;
import android.os.Vibrator;

/**
 * Created by abertschi on 12.02.17.
 */
public class VibraFeedback implements FeedbackService {

    public static final int DEFAULT_DURATION = 100;

    private Context context;
    private int duration;

    public VibraFeedback(Context context) {
        this.context = context;
    }

    @Override
    public void feedback() {
        doFeedback(duration);
    }

    private void doFeedback(int duration) {
        Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(duration);
    }

    @Override
    public void exampleFeedback() {
        doFeedback(duration);
    }

    public VibraFeedback setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public int getDuration() {
        return duration;
    }
}
