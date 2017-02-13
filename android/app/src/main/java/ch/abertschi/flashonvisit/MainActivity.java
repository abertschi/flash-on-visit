package ch.abertschi.flashonvisit;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.kdb.ledcontrol.LEDManager;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ch.abertschi.flashonvisit.feedback.FeedbackService;
import ch.abertschi.flashonvisit.feedback.FlashFeedback;
import ch.abertschi.flashonvisit.feedback.LedFeedback;
import ch.abertschi.flashonvisit.feedback.VibraFeedback;

/**
 * MainActivity
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "flashonvisit";

    private static final String DEFAULT_SERVER_NAME = "http://213.136.81.179:3004";
    private static final String DEFAULT_CHANNEL = "abertschi";

    private static final long RECONNECT_FREQUENCY = 5000;
    private static final int DEFAULT_ANIMATION_DURATION = 100;
    private static final int HISTORY_COLLAPSED_HEIGHT_DP = 150;

    private static final int LED_COLOR_BLUE = 0xff0099cc;
    private static final int LED_COLOR_GREEN = 0xff669900;
    private static final int LED_COLOR_RED = 0xffcc0000;
    private static final int LED_COLOR_DEFAULT = LED_COLOR_RED;

    private static final int FEEDBACK_DURATION = 100;

    private static final String SERVER = "server_name";
    private static final String CHANNEL = "channel_name";
    private static final String ENABLED = "is_enabled";
    private static final String LED_COLOR = "led_color";
    private static final String LED_KERNEL_HACK = "led_kernel_hack";
    private SharedPreferences prefs;

    private LEDManager ledManager;
    private Socket mSocket;
    private boolean isRunning = false;
    private boolean isHistoryCollapsed = true;
    private final Handler connectHandler = new Handler(Looper.getMainLooper());

    private boolean isAdvancedLedOptionsCollapsed = true;
    private RecyclerView historyRecycleView;
    private ViewGroup rootViewContainer;
    private EditText serverTextEdit;
    private EditText channelTextEdit;
    private Switch ledKernelHackSwitch;

    private TextView ledTextColor;
    private ImageView indicatorDisconnected;
    private ImageView indicatorConnected;

    private AVLoadingIndicatorView indicatorConnecting;
    private HistoryAdapter historyAdapter;

    private LedFeedback ledService;
    private VibraFeedback vibraService;
    private FlashFeedback flashService;
    private Set<FeedbackService> feedbackServices = new LinkedHashSet<>();

    private enum FeedbackServiceType {
        FLASH,
        LED,
        VIBRA
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new RequestUserPermission(this).verifyStoragePermissions();

        this.ledManager = new LEDManager(this);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.isRunning = this.prefs.getBoolean(ENABLED, false);
        initFeedbackServices();

        List<HistoryEntry> historyModel = new ArrayList<>();
        initializeViews(historyModel);
        showSplashAnimation();

        if (isRunning) {
            connectToSocketAndRetryIfFailed();
        } else {
            showAnimationDisconnectedIfNotVisible();
        }
        checkIfServerAddressIsValid(getServerName());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectSocket();
    }

    private void initFeedbackServices() {
        ledService = new LedFeedback(this).setDuration(FEEDBACK_DURATION);
        vibraService = new VibraFeedback(this);
        flashService = new FlashFeedback(this);
        addFeedbackService(FeedbackServiceType.FLASH);
        addFeedbackService(FeedbackServiceType.LED);
        addFeedbackService(FeedbackServiceType.VIBRA);
    }

    private void initializeViews(List<HistoryEntry> historyModel) {
        rootViewContainer = (ViewGroup) this.findViewById(R.id.container);
        initHistoryRecycleView(historyModel);

        indicatorConnected = (ImageView) this.findViewById(R.id.connection_logo_connected);
        indicatorDisconnected = (ImageView) this.findViewById(R.id.connection_logo_disconnected);
        indicatorConnecting = (AVLoadingIndicatorView) this.findViewById(R.id.connection_logo_loading);

        initPowerView();
        initAboutView();
        initLedKernelHackView();
        initLedColorChangeViews();
        initHistoryCollapseButton();
        initLedOptionsCollapseButton();
        initLedTryOutView();
        initServerEditText();
        initChannelView();
        initServiceSelectionView();

        final View kernelHackView = this.findViewById(R.id.led_root_options);
        kernelHackView.setVisibility(ledManager.rooted ? View.VISIBLE : View.GONE);
        addHistoryEntry("Welcome to flash-on-visit <b>:D</b>");
    }

    private void initServiceSelectionView() {
        final int colorSelected = Color.parseColor("#5F0F40");
        final int colorUnselected = Color.parseColor("#321325");
        final Button flashButton = (Button) findViewById(R.id.flash_feedback);
        final Button ledButton = (Button) findViewById(R.id.led_feedback);
        final Button vibraButton = (Button) findViewById(R.id.vibra_feedback);

        final View selectionIndicatorLed = findViewById(R.id.feedback_channel_selection_led);
        final View selectionIndicatorVibra = findViewById(R.id.feedback_channel_selection_vibra);
        final View selectionIndicatorFlash = findViewById(R.id.feedback_channel_selection_flash);
        final int SELECTION_INDICATOR_DURATION = 300;

        selectionIndicatorLed.setVisibility(isServiceActive(FeedbackServiceType.LED) ? View.VISIBLE : View.GONE);
        selectionIndicatorVibra.setVisibility(isServiceActive(FeedbackServiceType.VIBRA) ? View.VISIBLE : View.GONE);
        selectionIndicatorFlash.setVisibility(isServiceActive(FeedbackServiceType.FLASH) ? View.VISIBLE : View.GONE);

        flashButton.getBackground()
                .setColorFilter(isServiceActive(FeedbackServiceType.FLASH)
                        ? colorSelected : colorUnselected, PorterDuff.Mode.SRC_OVER);
        ledButton.getBackground()
                .setColorFilter(isServiceActive(FeedbackServiceType.LED)
                        ? colorSelected : colorUnselected, PorterDuff.Mode.SRC_OVER);
        vibraButton.getBackground()
                .setColorFilter(isServiceActive(FeedbackServiceType.VIBRA)
                        ? colorSelected : colorUnselected, PorterDuff.Mode.SRC_OVER);

        showFeedbackValidationMessageIfRequired();

        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color;
                if (isServiceActive(FeedbackServiceType.FLASH)) {
                    hideView(selectionIndicatorFlash, 0, SELECTION_INDICATOR_DURATION);
                    removeFeedbackService(FeedbackServiceType.FLASH);
                    color = colorUnselected;
                    addHistoryEntry("Disable <b>FLASH</b> feedback");
                } else {
                    showView(selectionIndicatorFlash, 0, SELECTION_INDICATOR_DURATION);
                    addFeedbackService(FeedbackServiceType.FLASH);
                    color = colorSelected;
                    doExampleFeedback(FeedbackServiceType.FLASH);
                    addHistoryEntry("Enable <b>FLASH</b> feedback");
                }
                flashButton.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_OVER);
                showFeedbackValidationMessageIfRequired();
            }
        });
        ledButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color;
                if (isServiceActive(FeedbackServiceType.LED)) {
                    hideView(selectionIndicatorLed, 0, SELECTION_INDICATOR_DURATION);
                    removeFeedbackService(FeedbackServiceType.LED);
                    color = colorUnselected;
                    addHistoryEntry("Disable <b>LED</b> feedback");
                } else {
                    showView(selectionIndicatorLed, 0, SELECTION_INDICATOR_DURATION);
                    addFeedbackService(FeedbackServiceType.LED);
                    color = colorSelected;
                    doExampleFeedback(FeedbackServiceType.LED);
                    addHistoryEntry("Enable <b>LED</b> feedback");
                }
                ledButton.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_OVER);
                showFeedbackValidationMessageIfRequired();
            }
        });
        vibraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color;
                if (isServiceActive(FeedbackServiceType.VIBRA)) {
                    hideView(selectionIndicatorVibra, 0, SELECTION_INDICATOR_DURATION);
                    removeFeedbackService(FeedbackServiceType.VIBRA);
                    color = colorUnselected;
                    addHistoryEntry("Disable <b>VIBRA</b> feedback");
                } else {
                    showView(selectionIndicatorVibra, 0, SELECTION_INDICATOR_DURATION);
                    color = colorSelected;
                    addFeedbackService(FeedbackServiceType.VIBRA);
                    doExampleFeedback(FeedbackServiceType.VIBRA);
                    addHistoryEntry("Enable <b>LED</b> feedback");
                }
                vibraButton.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_OVER);
                showFeedbackValidationMessageIfRequired();
            }
        });
    }

    private void showFeedbackValidationMessageIfRequired() {
        final View view = findViewById(R.id.feedback_channel_validation);
        if (feedbackServices.size() == 0) {
            showView(view, 300, 50);
        } else {
            hideView(view, 0, 50);
        }
    }

    private void addHistoryEntry(String content) {
        historyAdapter.addAtFront(new HistoryEntry(content));
    }

    private void connectToSocketAndRetryIfFailed() {
        historyAdapter.addAtFront(new HistoryEntry(String.format("Connecting with <b>%s</b>", getServerName())));
        connectHandler.removeCallbacks(reconnectRunnable);

        if (isRunning) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isRunning) {
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
            String server = getServerName();
            mSocket = IO.socket(server);
            mSocket.connect();
            mSocket.on("flash", onFlashEvent);
            mSocket.on("connect", onConnectEvent);
            mSocket.on("disconnect", onDisconnectEvent);

        } catch (URISyntaxException e) {
            System.out.println(e);
        }
    }

    private void disconnectSocket() {
        if (mSocket != null) {
            showAnimationDisconnectedIfNotVisible();
            historyAdapter.addAtFront(new HistoryEntry(String.format("Leaving <b>%s</b>", getChannelName())));
            mSocket.disconnect();
            mSocket.off("connect", onConnectEvent);
            mSocket.off("disconnect", onDisconnectEvent);
            mSocket.off("flash", onFlashEvent);
            mSocket = null;
        }
    }

    private Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                if (mSocket == null || !mSocket.connected()) {
                    connectSocket();
                    connectHandler.postDelayed(this, RECONNECT_FREQUENCY);
                }
            }
        }
    };

    private Emitter.Listener onConnectEvent = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        showAnimationConnectedIfNotVisible();
                        String channel = getChannelName();
                        String server = getServerName();
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

    private Emitter.Listener onFlashEvent = new Emitter.Listener() {
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
                        doFeedback();
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

    private Emitter.Listener onDisconnectEvent = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showAnimationDisconnectedIfNotVisible();
                    String channel = getChannelName();
                    historyAdapter.addAtFront(new HistoryEntry(String.format("Leaving <b>%s</b>", channel)));
                    if (isRunning) {
                        connectToSocketAndRetryIfFailed();
                    }
                }
            });
        }
    };

    private void addFeedbackService(FeedbackServiceType type) {
        feedbackServices.add(getFeedbackServiceImpl(type));
    }

    private void removeFeedbackService(FeedbackServiceType type) {
        feedbackServices.remove(getFeedbackServiceImpl(type));
    }

    private boolean isServiceActive(FeedbackServiceType type) {
        return feedbackServices.contains(getFeedbackServiceImpl(type));
    }

    private <T extends FeedbackService> T getFeedbackServiceImpl(FeedbackServiceType type) {
        switch (type) {
            case FLASH:
                return (T) flashService;
            case LED:
                return (T) ledService;
            case VIBRA:
                return (T) vibraService;
            default:
                throw new UnsupportedOperationException("Unknown feedback service");
        }
    }

    private void doFeedback() {
        for (FeedbackService s : feedbackServices) {
            s.feedback();
        }
    }

    private void doFeedback(FeedbackServiceType... types) {
        for (FeedbackServiceType s : types) {
            getFeedbackServiceImpl(s).feedback();
        }
    }

    private void doExampleFeedback(FeedbackServiceType... types) {
        for (FeedbackServiceType s : types) {
            getFeedbackServiceImpl(s).exampleFeedback();
        }
    }

    private void doExampleFeedback() {
        for (FeedbackService s : feedbackServices) {
            s.exampleFeedback();
        }
    }

    private void initHistoryRecycleView(List<HistoryEntry> model) {
        this.historyRecycleView = (RecyclerView) this.findViewById(R.id.history_recycleview);
        historyRecycleView.setNestedScrollingEnabled(false);

        historyAdapter = new HistoryAdapter(model, this, historyRecycleView);
        historyRecycleView.setAdapter(historyAdapter);
        historyRecycleView.addItemDecoration(new HistoryAdapter.HistoryItemDecorator(this, historyAdapter));

        LinearLayoutManager historyLayout = new LinearLayoutManager(this);
        historyRecycleView.setLayoutManager(historyLayout);

        this.findViewById(R.id.clearHistoryButton)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        historyAdapter.clearModel();
                        addHistoryEntry("Welcome to flash-on-visit <b>:D</b>");
                    }
                });
    }

    private void initAboutView() {
        final TextView dots = (TextView) this.findViewById(R.id.dots);
        dots.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDialog dialog = new MaterialDialog.Builder(MainActivity.this)
                        .title("About")
                        .content("This app is made with <3 by abertschi\n"
                                + "and is completely free as in free speech.\n\nFeedback and Code at \n"
                                + "https://github.com/abertschi/flash-on-visit")

                        .positiveText("ROCK ON")
                        .show();
            }
        });
    }

    private void checkIfServerAddressIsValid(String server) {
        new CheckServerAvailabilityTask(new CheckServerAvailabilityTask.Argument() {
            @Override
            public void apply(boolean isValid) {
                TextView txt = (TextView) findViewById(R.id.address_validation);
                if (isValid) {
                    hideView(txt, 0);
                } else {
                    showView(txt, 0);
                }
            }
        }).execute(server);
    }

    private void initPowerView() {
        final Button powerButton = (Button) findViewById(R.id.button);
        powerButton.setText(this.isRunning ? "STOP" : "START");

        powerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    isRunning = false;
                    powerButton.setText("START");
                    disconnectSocket();
                } else {
                    checkIfServerAddressIsValid(getServerName());
                    boolean validInput = true;
                    if (getChannelName().isEmpty()) {
                        validInput = false;
                        channelTextEdit.requestFocus();
                        channelTextEdit.setSelection(0);
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .playOn(findViewById(R.id.channel_validation));
                    }
                    if (getServerName().isEmpty()) {
                        validInput = false;
                        serverTextEdit.requestFocus();
                        serverTextEdit.setSelection(0);
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .playOn(findViewById(R.id.address_validation));
                    }
                    if (validInput) {
                        isRunning = true;
                        powerButton.setText("STOP");
                        connectToSocketAndRetryIfFailed();
                    }
                }
                prefs.edit().putBoolean(ENABLED, isRunning).commit();
            }
        });
    }

    private void initLedKernelHackView() {
        this.ledKernelHackSwitch = (Switch) this.findViewById(R.id.led_kernel_hack);
        ledKernelHackSwitch.setChecked(prefs.getBoolean(LED_KERNEL_HACK, false));
        ledService.setKernelTrigger(ledKernelHackSwitch.isChecked());

        ledKernelHackSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(LED_KERNEL_HACK, isChecked).commit();
                ledService.setKernelTrigger(isChecked);
                if (isChecked) {
                    addHistoryEntry("Change LED mode to <b>hack</b>");
                } else {
                    addHistoryEntry("Change LED mode to <b>normal</b>");
                }
            }
        });
    }

    private void initHistoryCollapseButton() {
        final TextView historyCollapseButton = (TextView) this.findViewById(R.id.moreHistoryButton);
        historyCollapseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RelativeLayout container = (RelativeLayout) findViewById(R.id.history_recycleview_container);
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) container.getLayoutParams();
                if (isHistoryCollapsed) {
                    historyCollapseButton.setText("LESS");
                    isHistoryCollapsed = false;
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    isHistoryCollapsed = true;
                    historyCollapseButton.setText("MORE");
                    int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            HISTORY_COLLAPSED_HEIGHT_DP, getResources().getDisplayMetrics());
                    layoutParams.height = height;
                }
                container.setLayoutParams(layoutParams);
            }
        });
    }

    private void initLedOptionsCollapseButton() {
        final TextView button = (TextView) this.findViewById(R.id.button_show_advanced_led);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View container = findViewById(R.id.advanced_options_led);
                TextView textView = (TextView) findViewById(R.id.button_show_advanced_led);
                if (isAdvancedLedOptionsCollapsed) {
//                    ScaleAnimation anim = new ScaleAnimation(1, 1, 0, 1);
//                    container.setAnimation(anim);
//                    anim.setDuration(500);
//                    anim.setAnimationListener(new Animation.AnimationListener() {
//                        @Override
//                        public void onAnimationStart(Animation animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animation animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animation animation) {
//
//                        }
//                    });
//                    anim.start();
//                    container.setVisibility(View.VISIBLE);

                    showView(container, 0, 50);
                    textView.setText("BASIC");
                    isAdvancedLedOptionsCollapsed = false;
                } else {
                    hideView(container, 0, 50);
//                    ScaleAnimation anim = new ScaleAnimation(1, 1, 1, 0);
//                    container.setAnimation(anim);
//                    anim.setDuration(500);
//                    anim.setAnimationListener(new Animation.AnimationListener() {
//                        @Override
//                        public void onAnimationStart(Animation animation) {
//                            System.out.println("ON START");
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animation animation) {
//                            System.out.println("ENDED");
//                            container.setVisibility(View.GONE);
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animation animation) {
//
//                        }
//                    });
//                    anim.startNow();
//                    System.out.println("started");

                    textView.setText("ADVANCED");
                    isAdvancedLedOptionsCollapsed = true;
                }
            }
        });
    }

    private void initLedColorChangeViews() {
        final View ledRedButton = this.findViewById(R.id.led_red);
        final View ledBlueButton = this.findViewById(R.id.led_blue);
        final View ledGreenButton = this.findViewById(R.id.led_green);

        int storedColor = prefs.getInt(LED_COLOR, LED_COLOR_DEFAULT);
        ledTextColor = (TextView) findViewById(R.id.led_color_text);
        ledTextColor.setTextColor(storedColor);
        ledService.setLedColor(storedColor);

        ledRedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = LED_COLOR_RED;
                ledTextColor.setTextColor(color);
                prefs.edit().putInt(LED_COLOR, color).commit();
                addHistoryEntry(String.format("Change LED <b>%s</b>", Utils.colorTextInHtml("color", color)));
                ledService.setLedColor(color);
                doFeedback();
            }
        });
        ledBlueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = LED_COLOR_BLUE;
                ledTextColor.setTextColor(color);
                prefs.edit().putInt(LED_COLOR, color).commit();
                addHistoryEntry(String.format("Change LED <b>%s</b>", Utils.colorTextInHtml("color", color)));
                ledService.setLedColor(color);
                doFeedback();
            }
        });
        ledGreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = LED_COLOR_GREEN;
                ledTextColor.setTextColor(color);
                prefs.edit().putInt(LED_COLOR, LED_COLOR_GREEN).commit();
                addHistoryEntry(String.format("Change LED <b>%s</b>", Utils.colorTextInHtml("color", color)));
                ledService.setLedColor(color);
                doFeedback();
            }
        });
    }

    private void initLedTryOutView() {
        final View tryOutLedButton = this.findViewById(R.id.tryoutButton);

        tryOutLedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doExampleFeedback();
                historyAdapter.addAtFront(new HistoryEntry("Try out LED"));
                final ImageView view = (ImageView) findViewById(R.id.lightbulp);
                view.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.mipmap.lightbulb_y2));
                Handler handler = new Handler(Looper.getMainLooper());
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        view.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.mipmap.lightbulb));
                    }
                };
                handler.postDelayed(r, 150);
            }
        });
    }

    private void initServerEditText() {
        serverTextEdit = (EditText) this.findViewById(R.id.serveradress);
        setServerName(prefs.getString(SERVER, DEFAULT_SERVER_NAME));

        serverTextEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkIfServerAddressIsValid(s.toString());
                prefs.edit().putString(SERVER, s.toString()).commit();
            }
        });
    }

    private String getServerName() {
        return serverTextEdit.getText().toString();
    }

    private void setServerName(String name) {
        serverTextEdit.setText(name);
    }

    private void initChannelView() {
        this.channelTextEdit = (EditText) this.findViewById(R.id.channel);
        setChannelName(prefs.getString(CHANNEL, DEFAULT_CHANNEL));
        if (getChannelName().isEmpty()) {
            TextView view = (TextView) findViewById(R.id.channel_validation);
            showView(view, 0);
        }
        channelTextEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
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
    }

    private void setChannelName(String name) {
        channelTextEdit.setText(name);
    }

    private String getChannelName() {
        return channelTextEdit.getText().toString();
    }

    private void showSplashAnimation() {
        for (int i = 0; i < rootViewContainer.getChildCount(); i++) {
            View v = rootViewContainer.getChildAt(i);

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
        showView(indicatorConnecting, hidePrevious ? DEFAULT_ANIMATION_DURATION * 2 : 0);
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
        showView(indicatorConnected, DEFAULT_ANIMATION_DURATION * 5);
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
        showView(indicatorDisconnected, hidePrevious ? DEFAULT_ANIMATION_DURATION * 2 : 0);
    }

    private void showView(final View view, int delay) {
        showView(view, delay, DEFAULT_ANIMATION_DURATION);
    }

    private void showView(final View view, int delay, int duration) {
        System.out.println("show view: " + view.toString() + " with delay " + delay);

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

    private void hideView(final View view, int delay) {
        hideView(view, delay, DEFAULT_ANIMATION_DURATION);
    }

    private void hideView(final View view, int delay, int duration) {
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

    private class ChangeViewVisibilityListener implements View.OnClickListener {

        private View underlineView;

        private ChangeViewVisibilityListener(View underlineView) {
            this.underlineView = underlineView;
        }

        @Override
        public void onClick(View v) {
            if (underlineView.getVisibility() == View.GONE) {
                showView(underlineView, 0, 100);
            } else {
                hideView(underlineView, 0, 100);
            }
        }
    }
}