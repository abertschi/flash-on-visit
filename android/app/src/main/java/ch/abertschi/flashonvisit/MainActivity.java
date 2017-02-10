package ch.abertschi.flashonvisit;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LED_NOTIFICATION_ID = 1;
    public static final long RECONNECT_FREQUENCY = 5000;
    private static final int CONNECTION_ANIMATION_DURATION = 100;
    private Socket mSocket;

    private SharedPreferences prefs;
    private EditText serverTextEdit;
    private EditText channelTextEdit;
    private Button enableButton;
    private boolean isEnabled = false;

    private static final String SERVER = "server_name";
    private static final String CHANNEL = "channel_name";
    private static final String ENABLED = "is_enabled";

    public static final int STARTUP_DELAY = 300;
    public static final int ANIM_ITEM_DURATION = 1000;
    public static final int ITEM_DELAY = 300;

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

    private void animate() {

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

//    private void showStatusBar(String text) {
//        statusBarText.setText(text);
//        statusBar.setVisibility(View.VISIBLE);
//
//        //ObjectAnimator fadeOut = ObjectAnimator.ofFloat(statusBar, "alpha",  1f, .3f);
//        //fadeOut.setDuration(2000);
//        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(statusBar, "alpha", 0f, 1f);
//        fadeIn.setDuration(500);
//
//        final Handler connectHandler = new Handler(Looper.getMainLooper());
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                // Do the task...
//                //connectHandler.postDelayed(this);
//                ObjectAnimator fadeOut = ObjectAnimator.ofFloat(statusBar, "alpha", 1f, 0f);
//                fadeOut.setDuration(500);
//                fadeOut.start();
//                fadeOut.addListener(new Animator.AnimatorListener() {
//                    @Override
//                    public void onAnimationStart(Animator animation) {
//
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        statusBar.setVisibility(View.GONE);
//                    }
//
//                    @Override
//                    public void onAnimationCancel(Animator animation) {
//
//                    }
//
//                    @Override
//                    public void onAnimationRepeat(Animator animation) {
//
//                    }
//                });
//            }
//        };
//        connectHandler.postDelayed(runnable, 5000);
//
//// Stop a repeating task like this.
//        //hander.removeCallbacks(runnable);
//
////        TranslateAnimation animation = new TranslateAnimation(0, 0, -300, 0);
////        animation.setDuration(500);
////        animation.setFillAfter(false);
////        statusBar.setAnimation(animation);
////        animation.start();
////        fadeIn.start();
//    }
//
//    private void hideStatusBar() {
//        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, -300);
//        animation.setDuration(500);
//        animation.setFillAfter(false);
//        statusBar.setAnimation(animation);
//        animation.start();
//        animation.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//                statusBar.setVisibility(View.GONE);
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//            }
//        });
//    }
//
//    private void updateStatusBar(String text) {
//        statusBarText.setText(text);
//    }

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

        boolean hidePrevious = false;
        if (indicatorConnecting.getVisibility() != View.GONE) {
            hideView(indicatorConnecting, 0);
            hidePrevious = true;
        }
        if (indicatorDisconnected.getVisibility() != View.GONE) {
            hideView(indicatorDisconnected, 0);
            hidePrevious = true;
        }
        showView(indicatorConnected, hidePrevious ? CONNECTION_ANIMATION_DURATION * 2 : 0);
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

        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, r.getDisplayMetrics());
        System.out.println(px + " DP TO PX");

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
        final Button tryOutLedButton = (Button) this.findViewById(R.id.tryoutButton);
        final Button clearHistoryButton = (Button) this.findViewById(R.id.clearHistoryButton);
        this.indicatorConnected = (ImageView) this.findViewById(R.id.connection_logo_connected);
        this.indicatorDisconnected = (ImageView) this.findViewById(R.id.connection_logo_disconnected);
        this.indicatorConnecting = (AVLoadingIndicatorView) this.findViewById(R.id.connection_logo_loading);

        this.historyRecycleView = (RecyclerView) this.findViewById(R.id.history_recycleview);
        historyLayoutManager = new LinearLayoutManager(this);
        historyRecycleView.setLayoutManager(historyLayoutManager);

        this.logoHeader = this.findViewById(R.id.logo_card);
        container = (ViewGroup) this.findViewById(R.id.container);

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
                final String toastMsg;
                if (isEnabled) {
                    isEnabled = false;
                    enableButton.setText("START");
                    disconnectSocket();
                } else {
                    isEnabled = true;
                    enableButton.setText("STOP");
                    connectToSocketAndRetryIfFailed();
                }
                prefs.edit().putBoolean(ENABLED, isEnabled).commit();
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
                prefs.edit().putString(CHANNEL, s.toString()).commit();
            }
        });

        tryOutLedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashLedLight();
                historyAdapter.addAtFront(new HistoryEntry("Try out LED"));

            }
        });

        clearHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyAdapter.clearModel();
            }
        });

        animate();
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

                        historyAdapter.addAtFront(new HistoryEntry(String.format("Flash in channel %s by %s ", channel, who)));

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
//        if (ledManager.isDeviceSupported()) {
//            ledManager.setChoiseToOn();
//            ledManager.ApplyBrightness(10);
//            ledManager.Apply();
//
//            new Handler().postDelayed(
//                    new Runnable() {
//                        public void run() {
//                            Log.i("tag", "This'll run 300 milliseconds later");
//                            ledManager.setChoiseToOff();
//                            ledManager.ApplyBrightness(10);
//                            ledManager.Apply();
//                        }
//                    },
//                    100);
//        } else {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        //mBuilder.setLights(Color.RED, 1000, 1000); // will blink
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setPriority(Notification.PRIORITY_HIGH);
        mBuilder.setOngoing(true);
        mBuilder.setLights(0xff00ff00, 300, 100);
        Notification notif = mBuilder.build();

        // notif.ledARGB = 0xFFff0000;
        //notif.flags = Notification.FLAG_SHOW_LIGHTS;
        //notif.ledOnMS = 100;
        //notif.ledOffMS = 100;

        nm.notify(LED_NOTIFICATION_ID, notif);
//            new Handler().postDelayed(
//                    new Runnable() {
//                        public void run() {
//                            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//                            nm.cancel(LED_NOTIFICATION_ID);
//                        }
//                    },
//                    500);
////        }
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
                        historyAdapter.addAtFront(new HistoryEntry(String.format("Connected to channel %s", channel, server)));

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
                    System.out.println("Disconnected");
                    showAnimationDisconnectedIfNotVisible();
                    String server = serverTextEdit.getText().toString();
                    historyAdapter.addAtFront(new HistoryEntry("Disconnected from " + server));
                    if (isEnabled) {
                        connectToSocketAndRetryIfFailed();
                    }
                }
            });
        }
    };

    private void connectToSocketAndRetryIfFailed() {
        String server = serverTextEdit.getText().toString();
        historyAdapter.addAtFront(new HistoryEntry(String.format("Connecting to %s", server)));
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
            if (mSocket != null) {
                disconnectSocket();
            }
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
}