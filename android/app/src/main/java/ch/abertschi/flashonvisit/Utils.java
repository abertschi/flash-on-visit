package ch.abertschi.flashonvisit;

import android.content.Context;
import android.util.TypedValue;

/**
 * Created by abertschi on 11.02.17.
 */
public class Utils {

    public static String colorTextInHtml(String text, int color) {
        String hexColor = String.format("#%06X", (0xFFFFFF & color));
        return String.format("<font color=\"%s\">%s</font>", hexColor, text);
    }

    public static int getThemeColorById (final Context context, int id) {
        final TypedValue value = new TypedValue ();
        context.getTheme ().resolveAttribute (id, value, true);
        return value.data;
    }
}
