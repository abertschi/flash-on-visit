package ch.abertschi.flashonvisit;

/**
 * Created by abertschi on 11.02.17.
 */
public class Utils {

    public static String colorTextInHtml(String text, int color) {
        String hexColor = String.format("#%06X", (0xFFFFFF & color));
        return String.format("<font color=\"%s\">%s</font>", hexColor, text);
    }
}
