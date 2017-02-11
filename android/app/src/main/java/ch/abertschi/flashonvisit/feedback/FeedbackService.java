package ch.abertschi.flashonvisit.feedback;

import android.content.Context;

import java.util.Map;

/**
 * Created by abertschi on 11.02.17.
 */
public interface FeedbackService {

    void exampleFeedback(Map<String, Object> params);

    void feedback(Map<String, Object> params);
}
