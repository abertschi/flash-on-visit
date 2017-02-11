package ch.abertschi.flashonvisit;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.kdb.ledcontrol.LEDManager;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LED_NOTIFICATION_ID = 1;
    public static final long RECONNECT_FREQUENCY = 5000;
    private static final int CONNECTION_ANIMATION_DURATION = 100;
    public static final int LED_COLOR_BLUE = 0xff0099cc;
    public static final int LED_COLOR_GREEN = 0xff669900;
    public static final int LED_COLOR_RED = 0xffcc0000;
    public static final int LED_COLOR_DEFAULT = LED_COLOR_RED;
    private Socket mSocket;
    private static final String TAG = "flashonvisit";

    private SharedPreferences prefs;
    private EditText serverTextEdit;
    private EditText channelTextEdit;
    private Button enableButton;
    private boolean isEnabled = false;

    private boolean isHistoryCollapsed = true;

    private static final String SERVER = "server_name";
    private static final String CHANNEL = "channel_name";
    private static final String ENABLED = "is_enabled";
    private static final String LED_COLOR = "led_color";
    private static final String LED_KERNEL_HACK = "led_kernel_hack";

    public static final int STARTUP_DELAY = 300;
    public static final int ANIM_ITEM_DURATION = 1000;
    public static final int ITEM_DELAY = 300;
    public static final int HISTORY_COLLAPSED_HEIGHT_DP = 150;

    private static final String DEFAULT_SERVER_NAME = "http://213.136.81.179:3004";

    private LEDManager ledManager;

    private RecyclerView historyRecycleView;
    private HistoryAdapter historyAdapter;
    private RecyclerView.LayoutManager historyLayoutManager;

    private View statusBar;
    private Button statusBarText;
    private View logoHeader;
    private ViewGroup container;

    private final Handler connectHandler = new Handler(Looper.getMainLooper());
    private Runnable reconnectReunnable = new Runnable() {
        @Override
        public void run() {
            if (isEnabled) {
                if (mSocket == null || !mSocket.connected()) {
                    connectSocket();
                    connectHandler.postDelayed(this, RECONNECT_FREQUENCY);
                }
            }
        }
    };
    private ImageView indicatorDisconnected;
    private ImageView indicatorConnected;
    private AVLoadingIndicatorView indicatorConnecting;
    private boolean isAdvancedLedOptionsCollapsed = true;
    private Switch ledKernelHackSwitch;
    private TextView ledTextColor;

    private void spalshAnimation() {
        for (int i = 0; i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);

            TranslateAnimation animation = new TranslateAnimation(0, 0, 400, 0);
            animation.setDuration(300);
            animation.setStartOffset((100 * i));
            animation.setInterpolator(new DecelerateInterpolator(0.5f));
            animation.setFillAfter(true);

            Animation a = new AlphaAnimation(0, 1.0f);
            a.setDuration(800);
            a.setInterpolator(new DecelerateInterpolator(0.5f));
            a.setStartOffset((100 * i));
            a.setFillAfter(true);

            AnimationSet set = new AnimationSet(true);
            set.addAnimation(animation);
            set.addAnimation(a);
            set.setFillAfter(true);
            v.setAnimation(set);
            set.start();

        }
    }

    private void showAnimationConnectingIfNotVisible() {
        if (indicatorConnecting.getVisibility() == View.VISIBLE)
            return;

        boolean hidePrevious = false;
        if (indicatorConnected.getVisibility() != View.GONE) {
            hideView(indicatorConnected, 0);
            hidePrevious = true;
        }
        if (indicatorDisconnected.getVisibility() != View.GONE) {
            hideView(indicatorDisconnected, 0);
            hidePrevious = true;
        }
        showView(indicatorConnecting, hidePrevious ? CONNECTION_ANIMATION_DURATION * 2 : 0);
    }

    private void showAnimationConnectedIfNotVisible() {
        if (indicatorConnected.getVisibility() == View.VISIBLE)
            return;

        hideView(indicatorConnecting, 0);
        if (indicatorConnecting.getVisibility() != View.GONE) {
            hideView(indicatorConnecting, 0);
        }
        if (indicatorDisconnected.getVisibility() != View.GONE) {
            hideView(indicatorDisconnected, 0);
        }
        showView(indicatorConnected, CONNECTION_ANIMATION_DURATION * 5);
    }

    private void showAnimationDisconnectedIfNotVisible() {
        if (indicatorDisconnected.getVisibility() == View.VISIBLE)
            return;

        boolean hidePrevious = false;
        if (indicatorConnecting.getVisibility() != View.GONE) {
            hideView(indicatorConnecting, 0);
            hidePrevious = true;
        }
        if (indicatorConnected.getVisibility() != View.GONE) {
            hideView(indicatorConnected, 0);
            hidePrevious = true;
        }
        showView(indicatorDisconnected, hidePrevious ? CONNECTION_ANIMATION_DURATION * 2 : 0);
    }

    private void showView(final View view, int delay) {
        System.out.println("show view: " + view.toString() + " with delay " + delay);

        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeIn.setInterpolator(new DecelerateInterpolator(0.5f));
        fadeIn.setStartDelay(delay);
        fadeIn.setDuration(CONNECTION_ANIMATION_DURATION);

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

    private void hideView(final View view, int delay) {
        System.out.println("hide view: " + view.toString() + " with delay " + delay);
        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        fadeIn.setInterpolator(new DecelerateInterpolator(0.5f));
        fadeIn.setStartDelay(delay);
        fadeIn.setDuration(CONNECTION_ANIMATION_DURATION);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.ledManager = new LEDManager(this);
        this.prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        this.enableButton = (Button) findViewById(R.id.button);
        this.isEnabled = this.prefs.getBoolean(ENABLED, false);
        this.serverTextEdit = (EditText) this.findViewById(R.id.serveradress);
        this.channelTextEdit = (EditText) this.findViewById(R.id.channel);
        final Button enableButton = (Button) this.findViewById(R.id.button);
        final View tryOutLedButton = this.findViewById(R.id.tryoutButton);
        final View clearHistoryButton = this.findViewById(R.id.clearHistoryButton);
        final TextView expandHistoryButton = (TextView) this.findViewById(R.id.moreHistoryButton);
        final TextView expandLedOptions = (TextView) this.findViewById(R.id.button_show_advanced_led);
        this.ledKernelHackSwitch = (Switch) this.findViewById(R.id.led_kernel_hack);
        ledKernelHackSwitch.setChecked(prefs.getBoolean(LED_KERNEL_HACK, false));
        ledTextColor = (TextView) findViewById(R.id.led_color_text);
        ledTextColor.setTextColor(prefs.getInt(LED_COLOR, LED_COLOR_DEFAULT));

        ledKernelHackSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(LED_KERNEL_HACK, isChecked).commit();
                if (isChecked) {
                    addHistoryEntry("Change LED mode to <b>hack</b>");
                } else {
                    addHistoryEntry("Change LED mode to <b>normal</b>");
                }

            }
        });

        this.indicatorConnected = (ImageView) this.findViewById(R.id.connection_logo_connected);
        this.indicatorDisconnected = (ImageView) this.findViewById(R.id.connection_logo_disconnected);
        this.indicatorConnecting = (AVLoadingIndicatorView) this.findViewById(R.id.connection_logo_loading);

        this.historyRecycleView = (RecyclerView) this.findViewById(R.id.history_recycleview);
        historyLayoutManager = new LinearLayoutManager(this);
        historyRecycleView.setLayoutManager(historyLayoutManager);

        this.logoHeader = this.findViewById(R.id.logo_card);
        container = (ViewGroup) this.findViewById(R.id.container);

        final View ledRedButton = this.findViewById(R.id.led_red);
        final View ledBlueButton = this.findViewById(R.id.led_blue);
        final View ledGreenButton = this.findViewById(R.id.led_green);

        final View kernelHackView = this.findViewById(R.id.kernel_hack);
        kernelHackView.setVisibility(ledManager.isDeviceSupported() ? View.VISIBLE : View.GONE);

        ledRedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = LED_COLOR_RED;
                ledTextColor.setTextColor(color);
                prefs.edit().putInt(LED_COLOR, color).commit();
                addHistoryEntry(String.format("Change LED <b>%s</b>", colorTextInHtml("color", color)));
                flashLedLight();
            }
        });

        ledBlueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = LED_COLOR_BLUE;
                ledTextColor.setTextColor(color);
                prefs.edit().putInt(LED_COLOR, color).commit();
                addHistoryEntry(String.format("Change LED <b>%s</b>", colorTextInHtml("color", color)));
                flashLedLight();
            }
        });
        ledGreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = LED_COLOR_GREEN;
                ledTextColor.setTextColor(color);
                prefs.edit().putInt(LED_COLOR, LED_COLOR_GREEN).commit();
                addHistoryEntry(String.format("Change LED <b>%s</b>", colorTextInHtml("color", color)));
                flashLedLight();
            }
        });
        List<HistoryEntry> historyEntries = new ArrayList<>();

        historyAdapter = new HistoryAdapter(historyEntries, this, historyRecycleView);
        historyRecycleView.setAdapter(historyAdapter);
        historyRecycleView.setNestedScrollingEnabled(false);
        historyRecycleView.addItemDecoration(new HistoryAdapter.HistoryItemDecorator(this, historyAdapter));

        TextView dotsTextview = (TextView) this.findViewById(R.id.dots);
        dotsTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDialog dialog = new MaterialDialog.Builder(MainActivity.this)
                        .title("About")
                        .content("This app is made with <3 by abertschi.\nhttp://www.abertschi.ch")
                        .positiveText("OK")
                        .show();
            }
        });

        serverTextEdit.setText(prefs.getString(SERVER, DEFAULT_SERVER_NAME));
        channelTextEdit.setText(prefs.getString(CHANNEL, ""));
        this.enableButton.setText(this.isEnabled ? "STOP" : "START");

        if (isEnabled) {
            connectToSocketAndRetryIfFailed();
        } else {
            showAnimationDisconnectedIfNotVisible();
        }

        enableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEnabled) {
                    isEnabled = false;
                    enableButton.setText("START");
                    disconnectSocket();
                } else {
                    new CheckServerAvailabilityTask().execute(serverTextEdit.getText().toString());
                    isEnabled = true;
                    enableButton.setText("STOP");
                    connectToSocketAndRetryIfFailed();
                }
                prefs.edit().putBoolean(ENABLED, isEnabled).commit();
            }
        });

        expandHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RelativeLayout container = (RelativeLayout) findViewById(R.id.history_recycleview_container);
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) container.getLayoutParams();
                if (isHistoryCollapsed) {
                    expandHistoryButton.setText("LESS");
                    isHistoryCollapsed = false;
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    isHistoryCollapsed = true;
                    expandHistoryButton.setText("MORE");
                    int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            HISTORY_COLLAPSED_HEIGHT_DP, getResources().getDisplayMetrics());
                    layoutParams.height = height;
                }
                container.setLayoutParams(layoutParams);
            }
        });

        expandLedOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View container = findViewById(R.id.advanced_options_led);
                TextView textView = (TextView) findViewById(R.id.button_show_advanced_led);
                if (isAdvancedLedOptionsCollapsed) {
                    showView(container, 0);
                    textView.setText("BASIC");
                    isAdvancedLedOptionsCollapsed = false;
                } else {
                    hideView(container, 0);
                    textView.setText("ADVANCED");
                    isAdvancedLedOptionsCollapsed = true;
                }
            }
        });

        serverTextEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                new CheckServerAvailabilityTask().execute(s.toString());
                prefs.edit().putString(SERVER, s.toString()).commit();
            }
        });

        channelTextEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                TextView view = (TextView) findViewById(R.id.channel_validation);
                if (s.toString().isEmpty()) {
                    showView(view, 0);
                } else {
                    hideView(view, 0);
                }
                prefs.edit().putString(CHANNEL, s.toString()).commit();
            }
        });

        tryOutLedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashLedLight(4000);
                historyAdapter.addAtFront(new HistoryEntry("Try out LED"));
                final ImageView view = (ImageView) findViewById(R.id.lightbulp);
                view.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.mipmap.lightbulb_y2));
                Handler handler = new Handler(Looper.getMainLooper());
                final Runnable r = new Runnable() {
                    public void run() {
                        view.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.mipmap.lightbulb));
                    }
                };
                handler.postDelayed(r, 150);
            }
        });

        clearHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyAdapter.clearModel();
            }
        });

        spalshAnimation();
        new CheckServerAvailabilityTask().execute(serverTextEdit.getText().toString());
    }

    private void addHistoryEntry(String content) {
        historyAdapter.addAtFront(new HistoryEntry(content));
    }

    private String colorTextInHtml(String text, int color) {
        String hexColor = String.format("#%06X", (0xFFFFFF & color));
        return String.format("<font color=\"%s\">%s</font>", hexColor, text);
    }

    private Emitter.Listener onFlash = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("On flash");
                    JSONObject data = (JSONObject) args[0];
                    String who;
                    String channel;
                    try {
                        who = data.getString("ip");
                        channel = data.getString("channel");
                        flashLedLight();
                        addHistoryEntry(String.format("%s visits <b>%s</b>", who, channel));

                    } catch (JSONException e) {
                        return;
                    }
                    // add the message to view
                    System.out.println(who + " " + channel);
                }
            });
        }
    };

    private void flashLedLight() {
        flashLedLight(500);
    }

    private void flashLedLight(int delayUntilHideNotification) {
        if (ledManager.isDeviceSupported() && ledKernelHackSwitch.isChecked()) {
            ledManager.setChoiseToOn();
            ledManager.ApplyBrightness(10);
            ledManager.Apply();

            new Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            Log.i("tag", "This'll run 300 milliseconds later");
                            ledManager.setChoiseToOff();
                            ledManager.ApplyBrightness(10);
                            ledManager.Apply();
                        }
                    },
                    100);
        } else {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
            mBuilder.setSmallIcon(R.mipmap.ic_launcher);
            mBuilder.setContentTitle("Flash On Visit");
            mBuilder.setPriority(Notification.PRIORITY_HIGH);
            Notification notif = mBuilder.build();
            notif.ledARGB = prefs.getInt(LED_COLOR, LED_COLOR_DEFAULT);
            notif.flags = Notification.FLAG_SHOW_LIGHTS;
            notif.ledOnMS = 100;
            notif.ledOffMS = 0;
            nm.notify(LED_NOTIFICATION_ID, notif);
            new Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            nm.cancel(LED_NOTIFICATION_ID);
                        }
                    },
                    delayUntilHideNotification);
        }
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        showAnimationConnectedIfNotVisible();
                        String channel = MainActivity.this.channelTextEdit.getText().toString();
                        String server = MainActivity.this.serverTextEdit.getText().toString();
                        Toast.makeText(MainActivity.this, String.format("Connected to channel %s", channel), Toast.LENGTH_SHORT).show();
                        historyAdapter.addAtFront(new HistoryEntry(String.format("Joining <b>%s</b>", channel, server)));

                        System.out.println("On connect");
                        JSONObject payload = new JSONObject();

                        payload.put("channel", channel);
                        System.out.println("channel " + payload);
                        mSocket.emit("regist", payload);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showAnimationDisconnectedIfNotVisible();
                    String channel = channelTextEdit.getText().toString();
                    historyAdapter.addAtFront(new HistoryEntry("Leaving <b>%s</b>" + channel));
                    if (isEnabled) {
                        connectToSocketAndRetryIfFailed();
                    }
                }
            });
        }
    };

    private void connectToSocketAndRetryIfFailed() {
        String server = serverTextEdit.getText().toString();
        historyAdapter.addAtFront(new HistoryEntry(String.format("Connecting with <b>%s</b>", server)));
        connectHandler.removeCallbacks(reconnectReunnable);
        if (isEnabled) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isEnabled) {
                        if (mSocket == null || !mSocket.connected()) {
                            connectSocket();
                            connectHandler.postDelayed(this, RECONNECT_FREQUENCY);
                        }
                    }
                }
            };
            connectHandler.postDelayed(runnable, 0);
        }
    }

    private void connectSocket() {
        try {
//            if (mSocket != null) {
//                disconnectSocket();
//            }
            showAnimationConnectingIfNotVisible();
            String server = serverTextEdit.getText().toString();
            mSocket = IO.socket(server);
            mSocket.connect();
            mSocket.on("flash", onFlash);
            mSocket.on("connect", onConnect);
            mSocket.on("disconnect", onDisconnect);

        } catch (URISyntaxException e) {
            System.out.println(e);
        }
    }

    private void disconnectSocket() {
        if (mSocket != null) {
            showAnimationDisconnectedIfNotVisible();
            historyAdapter.addAtFront(new HistoryEntry(String.format("Leaving <b>%s</b>", channelTextEdit.getText())));
            mSocket.disconnect();
            mSocket.off("connect", onConnect);
            mSocket.off("disconnect", onDisconnect);
            mSocket.off("flash", onFlash);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectSocket();
    }

    private class CheckServerAvailabilityTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            boolean reachable = false;
            String host = params[0];
            int port = 0;
            try {
                if (!host.startsWith("http://") && !host.startsWith("https://")) {
                    host = "http://" + host;
                }
                System.out.println(host.lastIndexOf(":"));
                if (host.lastIndexOf(":") < 6) {

                    port = 80;
                } else {
                    String p = host.substring(host.lastIndexOf(":") + 1, host.length());
                    if (p.endsWith("/")) {
                        p = p.substring(0, p.length() - 1);
                    }
                    port = Integer.valueOf(p);
                    host = host.substring(0, host.length() - p.length() - 1);
                }

                System.out.println(host + " / " + port);
                URL url = new URL(host + ":" + port);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                reachable = true;

            } catch (Exception e) {
                System.out.println(e);
            }
            return reachable;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            TextView txt = (TextView) findViewById(R.id.address_validation);
            if (result) {
                hideView(txt, 0);
            } else {
                showView(txt, 0);
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}