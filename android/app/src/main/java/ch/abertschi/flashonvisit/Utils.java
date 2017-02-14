package ch.abertschi.flashonvisit;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.kdb.ledcontrol.LEDManager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by abertschi on 11.02.17.
 */
public class Utils {

    private static final int DEFAULT_ANIMATION_DURATION = 100;

    public static String colorTextInHtml(String text, int color) {
        String hexColor = String.format("#%06X", (0xFFFFFF & color));
        return String.format("<font color=\"%s\">%s</font>", hexColor, text);
    }

    public static int getThemeColorById(final Context context, int id) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(id, value, true);
        return value.data;
    }

    public static void checkIfRooted(final Argument<Boolean> callable, final Context c) {
        new AsyncTask<Void, Void, Boolean>() {
            protected Boolean doInBackground(Void... params) {
                LEDManager m = new LEDManager(c);
                return m.rooted;
            }

            protected void onPostExecute(Boolean result) {
                callable.apply(result);
            }
        }.execute();
    }

    public static void showView(final View view, int delay) {
        showView(view, delay, DEFAULT_ANIMATION_DURATION);
    }

    public static void showView(final View view, int delay, int duration) {
        System.out.println("show view: " + view.toString() + " with delay " + delay);
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }

        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeIn.setInterpolator(new DecelerateInterpolator(0.5f));
        fadeIn.setStartDelay(delay);
        fadeIn.setDuration(duration);

        fadeIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        fadeIn.start();
    }

    public static void hideView(final View view, int delay) {
        hideView(view, delay, DEFAULT_ANIMATION_DURATION);
    }

    public static void hideView(final View view, int delay, int duration) {
        if (view.getVisibility() == View.GONE) {
            return;
        }

        System.out.println("hide view: " + view.toString() + " with delay " + delay);
        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        fadeIn.setInterpolator(new DecelerateInterpolator(0.5f));
        fadeIn.setStartDelay(delay);
        fadeIn.setDuration(duration);
        fadeIn.start();
        fadeIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    public interface Argument<T> {
        void apply(T arg);
    }
}
