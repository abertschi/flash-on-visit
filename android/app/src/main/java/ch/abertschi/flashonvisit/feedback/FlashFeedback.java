package ch.abertschi.flashonvisit.feedback;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by abertschi on 12.02.17.
 */
public class FlashFeedback implements Feedback {

    private static final String TAG = "FlashFeedback";

    public static final int DEFAULT_DURATION = 10;

    private Context context;

    private int duration = DEFAULT_DURATION;

    public FlashFeedback(Context context) {
        this.context = context;
    }

    public boolean isSupported() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    public void feedback() {
        doFeedback(duration);
    }

    private void doFeedback(final int duration) {

        new AsyncTask<Void, String, Camera>() {
            @Override
            protected Camera doInBackground(Void... params) {
                final Camera camera = Camera.open();
                Camera.Parameters p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(p);
                return camera;
            }

            @Override
            protected void onPostExecute(final Camera camera) {
                try {
                    camera.startPreview();
                } catch (RuntimeException e) {
                    Log.e(TAG, "Error with flash feedback: " + e.getMessage());
                }

                new Handler().postDelayed(
                        new Runnable() {
                            public void run() {
                                try {
                                    camera.stopPreview();
                                    camera.release();
                                } catch (RuntimeException e) {
                                    Log.e(TAG, "Error with flash feedback: " + e.getMessage());
                                }

                            }
                        },
                        duration);
            }
        }.execute();
    }

    @Override
    public void exampleFeedback() {
        feedback();
    }

    public int getDuration() {
        return duration;
    }

    public FlashFeedback setDuration(int duration) {
        this.duration = duration;
        return this;
    }
}
