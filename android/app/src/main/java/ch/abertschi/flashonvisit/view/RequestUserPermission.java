package ch.abertschi.flashonvisit.view;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v13.app.ActivityCompat;

/**
 * Created by abertschi on 12.02.17.
 */
public class RequestUserPermission {

    private Activity activity;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.CAMERA
    };

    public RequestUserPermission(Activity activity) {
        this.activity = activity;
    }

    public void verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}


