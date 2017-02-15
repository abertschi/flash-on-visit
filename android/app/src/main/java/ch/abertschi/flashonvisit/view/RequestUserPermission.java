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
    private static final int REQUEST_CODE = 1;
    private static String[] PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    public RequestUserPermission(Activity activity) {
        this.activity = activity;
    }

    public boolean isAllowedToUseCamera() {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public void verifyAllPermissions() {
        if (!isAllowedToUseCamera()) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS,
                    REQUEST_CODE
            );
        }
    }
}


