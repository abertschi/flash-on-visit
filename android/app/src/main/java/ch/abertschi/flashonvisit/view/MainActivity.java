package ch.abertschi.flashonvisit.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.IBinder;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.github.nkzawa.socketio.client.Socket;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kdb.ledcontrol.LEDManager;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.List;

import ch.abertschi.flashonvisit.App;
import ch.abertschi.flashonvisit.CheckServerAvailabilityTask;
import ch.abertschi.flashonvisit.FeedbackService;
import ch.abertschi.flashonvisit.HistoryEntry;
import ch.abertschi.flashonvisit.R;
import ch.abertschi.flashonvisit.Utils;

/**
 * MainActivity
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "flashonvisit";

    private static final long RECONNECT_FREQUENCY = 5000;
    private static final int DEFAULT_ANIMATION_DURATION = 100;
    private static final int HISTORY_COLLAPSED_HEIGHT_DP = 150;

    private static final int LED_COLOR_BLUE = 0xff0099cc;
    private static final int LED_COLOR_GREEN = 0xff669900;
    private static final int LED_COLOR_RED = App.LED_COLOR_RED;

    private SharedPreferences prefs;

    private LEDManager ledManager;
    private Socket mSocket;
    private boolean isEnabled = false;
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

    private FeedbackService mFeedbackService;
    private boolean mServiceIsBound;

    boolean isFlashEnabled;
    boolean isLedEnabled;
    boolean isVibraEnabled;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mFeedbackService = ((FeedbackService.LocalBinder) service).getService();
            System.out.println("onServiceConnected");

            // init service configuration
            mFeedbackService.setLedKernelHack(prefs.getBoolean(App.PREFS_LED_KERNEL_HACK_ENABLED, false));
            mFeedbackService.setLedColor(prefs.getInt(App.PREFS_LED_COLOR, App.LED_COLOR_DEFAULT));
            //showAnimationConnectedIfNotVisible();
        }

        public void onServiceDisconnected(ComponentName className) {
            System.out.println("onServiceDisconnected");
            mFeedbackService = null;
            //showAnimationDisconnectedIfNotVisible();
        }
    };
    private boolean isEnabledOnBoot;

    public void doBindService() {
        //showAnimationConnectingIfNotVisible();
        Intent intent = new Intent(this, FeedbackService.class);
        startService(intent);
        this.bindService(intent, mConnection, Context.BIND_ABOVE_CLIENT);
        mServiceIsBound = true;
        System.out.println("doBindService");
    }

    public void doUnbindService() {
        if (mServiceIsBound) {
            this.unbindService(mConnection);
            mServiceIsBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new RequestUserPermission(this).verifyStoragePermissions();
        doBindService();

        this.ledManager = new LEDManager(this);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.isEnabled = this.prefs.getBoolean(App.PREFS_ENABLED, Boolean.FALSE);
        this.isEnabledOnBoot = this.prefs.getBoolean(App.PREFS_START_ON_BOOT, false);

        this.isVibraEnabled = this.prefs.getBoolean(App.PREFS_FEEDBACK_VIBRA_ENABLED, false);
        this.isLedEnabled = this.prefs.getBoolean(App.PREFS_FEEDBACK_LED_ENABLED, false);
        this.isFlashEnabled = this.prefs.getBoolean(App.PREFS_FEEDBACK_FLASH_ENABLED, false);

        List<HistoryEntry> historyModel = new ArrayList<>();
        initializeViews(historyModel);
        showSplashAnimation();

        if (isEnabled) {
            subscribeToService();
        } else {
            showAnimationDisconnectedIfNotVisible();
        }
        checkIfServerAddressIsValid(getServerName());

    }

    private void subscribeToService() {
        String channel = getChannelName(); // TODO validate better
        if (!channel.isEmpty()) {
            isEnabled = true;
            FirebaseMessaging.getInstance().subscribeToTopic(getChannelName());
        }
    }

    private void unsubscribeFromService() {
        isEnabled = false;
        String channel = getChannelName(); // TODO validate better
        if (!channel.isEmpty()) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(getChannelName());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    private void initializeViews(List<HistoryEntry> historyModel) {
        rootViewContainer = (ViewGroup) this.findViewById(R.id.container);
        initHistoryRecycleView(historyModel);

        indicatorConnected = (ImageView) this.findViewById(R.id.connection_logo_connected);
        indicatorDisconnected = (ImageView) this.findViewById(R.id.connection_logo_disconnected);
        indicatorConnecting = (AVLoadingIndicatorView) this.findViewById(R.id.connection_logo_loading);

        initPowerView();
        initStartOnBootView();
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

        selectionIndicatorLed.setVisibility(isLedEnabled ? View.VISIBLE : View.GONE);
        selectionIndicatorVibra.setVisibility(isVibraEnabled ? View.VISIBLE : View.GONE);
        selectionIndicatorFlash.setVisibility(isFlashEnabled ? View.VISIBLE : View.GONE);

        flashButton.getBackground()
                .setColorFilter(isFlashEnabled
                        ? colorSelected : colorUnselected, PorterDuff.Mode.SRC_OVER);
        ledButton.getBackground()
                .setColorFilter(isLedEnabled
                        ? colorSelected : colorUnselected, PorterDuff.Mode.SRC_OVER);
        vibraButton.getBackground()
                .setColorFilter(isVibraEnabled
                        ? colorSelected : colorUnselected, PorterDuff.Mode.SRC_OVER);

        showFeedbackValidationMessageIfRequired();

        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color;
                if (isFlashEnabled) {
                    hideView(selectionIndicatorFlash, 0, SELECTION_INDICATOR_DURATION);
                    mFeedbackService.removeFeedbackService(FeedbackService.TYPE.FLASH);
                    color = colorUnselected;
                    addHistoryEntry("Disable <b>FLASH</b> feedback");
                } else {
                    showView(selectionIndicatorFlash, 0, SELECTION_INDICATOR_DURATION);
                    mFeedbackService.addFeedbackService(FeedbackService.TYPE.FLASH);
                    color = colorSelected;
                    mFeedbackService.doExampleFeedback(FeedbackService.TYPE.FLASH);
                    addHistoryEntry("Enable <b>FLASH</b> feedback");
                }
                flashButton.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_OVER);
                isFlashEnabled = !isFlashEnabled;
                prefs.edit().putBoolean(App.PREFS_FEEDBACK_FLASH_ENABLED, isFlashEnabled).commit();
                showFeedbackValidationMessageIfRequired();
            }
        });
        ledButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color;
                if (isLedEnabled) {
                    hideView(selectionIndicatorLed, 0, SELECTION_INDICATOR_DURATION);
                    mFeedbackService.removeFeedbackService(FeedbackService.TYPE.LED);
                    color = colorUnselected;
                    addHistoryEntry("Disable <b>LED</b> feedback");
                } else {
                    showView(selectionIndicatorLed, 0, SELECTION_INDICATOR_DURATION);
                    mFeedbackService.addFeedbackService(FeedbackService.TYPE.LED);
                    color = colorSelected;
                    mFeedbackService.doExampleFeedback(FeedbackService.TYPE.LED);
                    addHistoryEntry("Enable <b>LED</b> feedback");
                }
                ledButton.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_OVER);
                isLedEnabled = !isLedEnabled;
                prefs.edit().putBoolean(App.PREFS_FEEDBACK_LED_ENABLED, isLedEnabled).commit();
                showFeedbackValidationMessageIfRequired();
            }
        });
        vibraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color;
                if (isVibraEnabled) {
                    hideView(selectionIndicatorVibra, 0, SELECTION_INDICATOR_DURATION);
                    mFeedbackService.removeFeedbackService(FeedbackService.TYPE.VIBRA);
                    color = colorUnselected;
                    addHistoryEntry("Disable <b>VIBRA</b> feedback");
                } else {
                    showView(selectionIndicatorVibra, 0, SELECTION_INDICATOR_DURATION);
                    color = colorSelected;
                    mFeedbackService.addFeedbackService(FeedbackService.TYPE.VIBRA);
                    mFeedbackService.doExampleFeedback(FeedbackService.TYPE.VIBRA);
                    addHistoryEntry("Enable <b>VIBRA</b> feedback");
                }
                vibraButton.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_OVER);
                isVibraEnabled = !isVibraEnabled;
                prefs.edit().putBoolean(App.PREFS_FEEDBACK_VIBRA_ENABLED, isVibraEnabled).commit();
                showFeedbackValidationMessageIfRequired();
            }
        });
    }

    private void showFeedbackValidationMessageIfRequired() {
        final View view = findViewById(R.id.feedback_channel_validation);
        if (!isFlashEnabled && !isLedEnabled && !isVibraEnabled) {
            showView(view, 300, 50);
        } else {
            hideView(view, 0, 50);
        }
    }

    private void addHistoryEntry(String content) {
        historyAdapter.addAtFront(new HistoryEntry(content));
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

    private void initStartOnBootView() {
        final Switch view = (Switch) findViewById(R.id.start_on_boot_swtich);
        this.isEnabledOnBoot = this.prefs.getBoolean(App.PREFS_START_ON_BOOT, false);
        view.setChecked(isEnabledOnBoot);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isEnabledOnBoot = !isEnabledOnBoot;
                prefs.edit().putBoolean(App.PREFS_START_ON_BOOT, isEnabledOnBoot).commit();
            }
        });
    }

    private void initPowerView() {
        final Button powerButton = (Button) findViewById(R.id.button);
        powerButton.setText(this.isEnabled ? "STOP" : "START");
        if (isEnabled) {
            showAnimationConnectedIfNotVisible();
        } else {
            showAnimationDisconnectedIfNotVisible();
        }

        powerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEnabled) {
                    isEnabled = false;
                    powerButton.setText("START");
                    unsubscribeFromService();
                    showAnimationDisconnectedIfNotVisible();
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
                        isEnabled = true;
                        powerButton.setText("STOP");
                        subscribeToService();
                        showAnimationConnectedIfNotVisible();
                    }
                }
                prefs.edit().putBoolean(App.PREFS_ENABLED, isEnabled).commit();
            }
        });
    }

    private void initLedKernelHackView() {
        this.ledKernelHackSwitch = (Switch) this.findViewById(R.id.led_kernel_hack);
        ledKernelHackSwitch.setChecked(prefs.getBoolean(App.PREFS_LED_KERNEL_HACK_ENABLED, false));

        ledKernelHackSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(App.PREFS_LED_KERNEL_HACK_ENABLED, isChecked).commit();
                mFeedbackService.setLedKernelHack(isChecked);
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

        int storedColor = prefs.getInt(App.PREFS_LED_COLOR, App.LED_COLOR_DEFAULT);
        ledTextColor = (TextView) findViewById(R.id.led_color_text);
        ledTextColor.setTextColor(storedColor);

        ledRedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = LED_COLOR_RED;
                ledTextColor.setTextColor(color);
                prefs.edit().putInt(App.PREFS_LED_COLOR, color).commit();
                addHistoryEntry(String.format("Change LED <b>%s</b>", Utils.colorTextInHtml("color", color)));
                mFeedbackService.setLedColor(color);
                mFeedbackService.doFeedback();
            }
        });
        ledBlueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = LED_COLOR_BLUE;
                ledTextColor.setTextColor(color);
                prefs.edit().putInt(App.PREFS_LED_COLOR, color).commit();
                addHistoryEntry(String.format("Change LED <b>%s</b>", Utils.colorTextInHtml("color", color)));
                mFeedbackService.setLedColor(color);
                mFeedbackService.doFeedback();
            }
        });
        ledGreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = LED_COLOR_GREEN;
                ledTextColor.setTextColor(color);
                prefs.edit().putInt(App.PREFS_LED_COLOR, LED_COLOR_GREEN).commit();
                addHistoryEntry(String.format("Change LED <b>%s</b>", Utils.colorTextInHtml("color", color)));

                mFeedbackService.setLedColor(color);
                mFeedbackService.doFeedback();
            }
        });
    }

    private void initLedTryOutView() {
        final View tryOutLedButton = this.findViewById(R.id.tryoutButton);

        tryOutLedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFeedbackService.doExampleFeedback();
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
        setServerName(prefs.getString(App.PREFS_SERVER, App.DEFAULT_SERVER_NAME));

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
                prefs.edit().putString(App.PREFS_SERVER, s.toString()).commit();
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
        setChannelName(prefs.getString(App.PREFS_CHANNEL, App.DEFAULT_CHANNEL));
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
                prefs.edit().putString(App.PREFS_CHANNEL, s.toString()).commit();
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
}