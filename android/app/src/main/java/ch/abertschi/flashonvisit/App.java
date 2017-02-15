package ch.abertschi.flashonvisit;

/**
 * Created by abertschi on 13.02.17.
 */
public class App {

    public static final String TAG_NAME = "flash-on-visit";

    public static final String PREFS_START_ON_BOOT = "start_on_boot";
    public static final String PREFS_ENABLED = "is_enabeld";
    public static final String PREFS_FEEDBACK_LED_ENABLED = "led_enabled";
    public static final String PREFS_FEEDBACK_FLASH_ENABLED = "flash_enabled";
    public static final String PREFS_FEEDBACK_VIBRA_ENABLED = "vibra_enabled";
    public static final String PREFS_LED_KERNEL_HACK_ENABLED = "led_kernel_hack_enabled";
    public static final String PREFS_LED_COLOR = "led_color";
    public static final String PREFS_SERVER = "server_name";
    public static final String PREFS_CHANNEL = "channel_name";
    public static final String PREFS_IS_ROOTED = "is_rooted";
    public static final String PREFS_FIRST_RUN = "first_run";

    public static final int LED_COLOR_RED = 0xffcc0000;
    public static final int LED_COLOR_DEFAULT = LED_COLOR_RED;

    public static final String DEFAULT_SERVER_NAME = "http://213.136.81.179:3004";
    public static final String DEFAULT_CHANNEL = "abertschi";
}
