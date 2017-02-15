package ch.abertschi.flashonvisit.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
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
import com.google.firebase.messaging.FirebaseMessaging;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.List;

import ch.abertschi.flashonvisit.App;
import ch.abertschi.flashonvisit.CheckServerAvailabilityTask;
import ch.abertschi.flashonvisit.db.HistoryDbHelper;
import ch.abertschi.flashonvisit.feedback.FeedbackService;
import ch.abertschi.flashonvisit.HistoryEntry;
import ch.abertschi.flashonvisit.R;
import ch.abertschi.flashonvisit.Utils;

/**
 * MainActivity
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "flashonvisit";

    private static final int DEFAULT_ANIMATION_DURATION = 100;
    private static final int HISTORY_COLLAPSED_HEIGHT_DP = 150;

    private static final int LED_COLOR_BLUE = 0xff0099cc;
    private static final int LED_COLOR_GREEN = 0xff669900;
    private static final int LED_COLOR_RED = App.LED_COLOR_RED;

    private SharedPreferences mPrefs;

    private boolean mIsAdvancedLedOptionsCollapsed = true;
    private boolean mIsHistoryCollapsed = true;
    private RecyclerView mHistoryRecycleView;
    private ViewGroup mRootViewContainer;
    private EditText mServerTextEdit;
    private EditText mChannelTextEdit;
    private Switch mLedKernelHackSwitch;

    private TextView mLedTextColor;
    private ImageView mIndicatorDisconnected;
    private ImageView mIndicatorConnected;

    private AVLoadingIndicatorView mIndicatorConnecting;
    private HistoryAdapter mHistoryAdapter;

    private FeedbackService mFeedbackService;
    private boolean mServiceIsBound;

    boolean mIsFlashEnabled;
    boolean mIsLedEnabled;
    boolean mIsVibraEnabled;
    private boolean mIsEnabledOnBoot;
    private boolean mIsEnabled;

    private static MainActivity instance;

    private MessageHandler mUiHandler;

    public static MainActivity get() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;

        super.onCreate(savedInstanceState);
        mUiHandler = new MessageHandler(Looper.getMainLooper());
        setContentView(R.layout.activity_main);

        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.mIsEnabled = this.mPrefs.getBoolean(App.PREFS_ENABLED, Boolean.FALSE);
        this.mIsEnabledOnBoot = this.mPrefs.getBoolean(App.PREFS_START_ON_BOOT, false);

        this.mIsVibraEnabled = this.mPrefs.getBoolean(App.PREFS_FEEDBACK_VIBRA_ENABLED, false);
        this.mIsLedEnabled = this.mPrefs.getBoolean(App.PREFS_FEEDBACK_LED_ENABLED, false);
        this.mIsFlashEnabled = this.mPrefs.getBoolean(App.PREFS_FEEDBACK_FLASH_ENABLED, false);

        List<HistoryEntry> historyModel = new ArrayList<>();
        initializeViews(historyModel);
        showSplashAnimation();
        doBindService();
        checkIfRootedAndApplySettings();

        if (mIsEnabled) {
            subscribeToService();
        } else {
            showAnimationDisconnectedIfNotVisible();
        }
        checkIfServerAddressIsValid(getServerName());
        fetchDatabase();
    }

    private void fetchDatabase() {
        new AsyncTask<Void, Void, List<HistoryEntry>>() {
            @Override
            protected List<HistoryEntry> doInBackground(Void... params) {
                HistoryDbHelper dbHelper = new HistoryDbHelper(MainActivity.this);
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                List<HistoryEntry> entries = dbHelper.getAll(db);
                db.close();
                return entries;
            }

            @Override
            protected void onPostExecute(List<HistoryEntry> historyEntries) {
                super.onPostExecute(historyEntries);
                for (HistoryEntry e : historyEntries) {
                    mHistoryAdapter.addAtEnd(e);
                }
                if (mHistoryAdapter.getModel().size() == 0) {
                    addWelcomeMsgToHistoryEntries();
                }
            }
        }.execute();
    }

    private void emptyDatabase() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                HistoryDbHelper dbHelper = new HistoryDbHelper(MainActivity.this);
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                dbHelper.deleteAll(db);
                db.close();
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if (mHistoryAdapter.getModel().size() == 0) {
                    addWelcomeMsgToHistoryEntries();
                }
            }
        }.execute();
    }

    private void addWelcomeMsgToHistoryEntries() {
        addHistoryEntry("Welcome to flash-on-visit <b>:D</b>");
    }

    private void subscribeToService() {
        new RequestUserPermission(this).verifyAllPermissions();

        String channel = getChannelName(); // TODO validate better
        if (!channel.isEmpty()) {
            mIsEnabled = true;
            FirebaseMessaging.getInstance().subscribeToTopic(getChannelName());
        }
    }

    private void unsubscribeFromService() {
        mIsEnabled = false;
        String channel = getChannelName(); // TODO validate better
        if (!channel.isEmpty()) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(getChannelName());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
        instance = null;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mFeedbackService = ((FeedbackService.LocalBinder) service).getService();
            initFeedbackServices(MainActivity.this, mFeedbackService);
            //showAnimationConnectedIfNotVisible();
        }

        public void onServiceDisconnected(ComponentName className) {
            mFeedbackService = null;
            //showAnimationDisconnectedIfNotVisible();
        }
    };

    public static void initFeedbackServices(Context c, FeedbackService service) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);

        service.setLedKernelHack(p.getBoolean(App.PREFS_LED_KERNEL_HACK_ENABLED, false));
        service.setLedColor(p.getInt(App.PREFS_LED_COLOR, App.LED_COLOR_DEFAULT));

        if (p.getBoolean(App.PREFS_FEEDBACK_LED_ENABLED, false)) {
            service.addFeedbackService(FeedbackService.TYPE.LED);
        }
        if (p.getBoolean(App.PREFS_FEEDBACK_VIBRA_ENABLED, false)) {
            service.addFeedbackService(FeedbackService.TYPE.VIBRA);
        }
        if (p.getBoolean(App.PREFS_FEEDBACK_FLASH_ENABLED, false)) {
            service.addFeedbackService(FeedbackService.TYPE.FLASH);
        }
    }

    public MessageHandler getUiHandler() {
        return mUiHandler;
    }

    private void doBindService() {
        //showAnimationConnectingIfNotVisible();
        Intent intent = new Intent(this, FeedbackService.class);
        startService(intent);
        this.bindService(intent, mConnection, Context.BIND_ABOVE_CLIENT);
        mServiceIsBound = true;
    }

    private void doUnbindService() {
        if (mServiceIsBound) {
            this.unbindService(mConnection);
            mServiceIsBound = false;
        }
    }

    private void initializeViews(List<HistoryEntry> historyModel) {
        mRootViewContainer = (ViewGroup) this.findViewById(R.id.container);
        initHistoryRecycleView(historyModel);

        mIndicatorConnected = (ImageView) this.findViewById(R.id.connection_logo_connected);
        mIndicatorDisconnected = (ImageView) this.findViewById(R.id.connection_logo_disconnected);
        mIndicatorConnecting = (AVLoadingIndicatorView) this.findViewById(R.id.connection_logo_loading);

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
        new RequestUserPermission(this).verifyAllPermissions();

    }

    private void checkIfRootedAndApplySettings() {
        final View kernelHackView = findViewById(R.id.led_root_options);
        if (mPrefs.contains(App.PREFS_IS_ROOTED)) {
            kernelHackView.setVisibility(mPrefs.getBoolean(App.PREFS_IS_ROOTED, true) ? View.VISIBLE : View.GONE);
        }
        Utils.checkIfRooted(isRooted -> {
            kernelHackView.setVisibility(isRooted ? View.VISIBLE : View.GONE);
            mPrefs.edit().putBoolean(App.PREFS_IS_ROOTED, isRooted).commit();
        }, this);
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

        selectionIndicatorLed.setVisibility(mIsLedEnabled ? View.VISIBLE : View.GONE);
        selectionIndicatorVibra.setVisibility(mIsVibraEnabled ? View.VISIBLE : View.GONE);
        selectionIndicatorFlash.setVisibility(mIsFlashEnabled ? View.VISIBLE : View.GONE);

        flashButton.getBackground()
                .setColorFilter(mIsFlashEnabled
                        ? colorSelected : colorUnselected, PorterDuff.Mode.SRC_OVER);
        ledButton.getBackground()
                .setColorFilter(mIsLedEnabled
                        ? colorSelected : colorUnselected, PorterDuff.Mode.SRC_OVER);
        vibraButton.getBackground()
                .setColorFilter(mIsVibraEnabled
                        ? colorSelected : colorUnselected, PorterDuff.Mode.SRC_OVER);

        showFeedbackValidationMessageIfRequired();

        flashButton.setOnClickListener(v -> {
            int color;
            if (mIsFlashEnabled) {
                mFeedbackService.removeFeedbackService(FeedbackService.TYPE.FLASH);

                Utils.hideView(selectionIndicatorFlash, 0, SELECTION_INDICATOR_DURATION);
                color = colorUnselected;
                addHistoryEntry("Disable <b>FLASH</b> feedback");
            } else {
                RequestUserPermission p = new RequestUserPermission(MainActivity.this);
                if (p.isAllowedToUseCamera()) {
                    mFeedbackService.addFeedbackService(FeedbackService.TYPE.FLASH);
                    mFeedbackService.doExampleFeedback(FeedbackService.TYPE.FLASH);

                    Utils.showView(selectionIndicatorFlash, 0, SELECTION_INDICATOR_DURATION);
                    color = colorSelected;
                    addHistoryEntry("Enable <b>FLASH</b> feedback");
                } else {
                    p.verifyAllPermissions();
                    return;
                }
            }
            flashButton.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_OVER);
            mIsFlashEnabled = !mIsFlashEnabled;
            mPrefs.edit().putBoolean(App.PREFS_FEEDBACK_FLASH_ENABLED, mIsFlashEnabled).commit();
            showFeedbackValidationMessageIfRequired();
        });
        ledButton.setOnClickListener(v -> {
            int color;
            if (mIsLedEnabled) {
                mFeedbackService.removeFeedbackService(FeedbackService.TYPE.LED);

                Utils.hideView(selectionIndicatorLed, 0, SELECTION_INDICATOR_DURATION);
                color = colorUnselected;
                addHistoryEntry("Disable <b>LED</b> feedback");
            } else {
                mFeedbackService.addFeedbackService(FeedbackService.TYPE.LED);
                mFeedbackService.doExampleFeedback(FeedbackService.TYPE.LED);

                Utils.showView(selectionIndicatorLed, 0, SELECTION_INDICATOR_DURATION);
                color = colorSelected;
                addHistoryEntry("Enable <b>LED</b> feedback");
            }
            ledButton.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_OVER);
            mIsLedEnabled = !mIsLedEnabled;
            mPrefs.edit().putBoolean(App.PREFS_FEEDBACK_LED_ENABLED, mIsLedEnabled).commit();
            showFeedbackValidationMessageIfRequired();
        });
        vibraButton.setOnClickListener(v -> {
            int color;
            if (mIsVibraEnabled) {
                mFeedbackService.removeFeedbackService(FeedbackService.TYPE.VIBRA);

                Utils.hideView(selectionIndicatorVibra, 0, SELECTION_INDICATOR_DURATION);
                color = colorUnselected;
                addHistoryEntry("Disable <b>VIBRA</b> feedback");
            } else {
                mFeedbackService.addFeedbackService(FeedbackService.TYPE.VIBRA);
                mFeedbackService.doExampleFeedback(FeedbackService.TYPE.VIBRA);

                Utils.showView(selectionIndicatorVibra, 0, SELECTION_INDICATOR_DURATION);
                color = colorSelected;
                addHistoryEntry("Enable <b>VIBRA</b> feedback");
            }
            vibraButton.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_OVER);
            mIsVibraEnabled = !mIsVibraEnabled;
            mPrefs.edit().putBoolean(App.PREFS_FEEDBACK_VIBRA_ENABLED, mIsVibraEnabled).commit();
            showFeedbackValidationMessageIfRequired();
        });
    }

    private void showFeedbackValidationMessageIfRequired() {
        final View view = findViewById(R.id.feedback_channel_validation);
        final View feedbackSettingsContainer = findViewById(R.id.settings_container);

        if (!mIsFlashEnabled && !mIsLedEnabled && !mIsVibraEnabled) {
            Utils.hideView(feedbackSettingsContainer, 0, 200);
            Utils.showView(view, 800, 200);
        } else {
            Utils.showView(feedbackSettingsContainer, 0, 200);
            Utils.hideView(view, 800, 200);
        }
    }

    private void addHistoryEntry(String content) {
        mHistoryAdapter.addAtFront(new HistoryEntry(content));
    }

    private void initHistoryRecycleView(List<HistoryEntry> model) {
        this.mHistoryRecycleView = (RecyclerView) this.findViewById(R.id.history_recycleview);
        mHistoryRecycleView.setNestedScrollingEnabled(false);

        mHistoryAdapter = new HistoryAdapter(model, this, mHistoryRecycleView);
        mHistoryRecycleView.setAdapter(mHistoryAdapter);
        mHistoryRecycleView.addItemDecoration(new HistoryAdapter.HistoryItemDecorator(this, mHistoryAdapter));

        LinearLayoutManager historyLayout = new LinearLayoutManager(this);
        mHistoryRecycleView.setLayoutManager(historyLayout);

        this.findViewById(R.id.clearHistoryButton)
                .setOnClickListener(v -> {
                    mHistoryAdapter.clearModel();
                    emptyDatabase();
                });
    }

    private void initAboutView() {
        final TextView dots = (TextView) this.findViewById(R.id.dots);
        dots.setOnClickListener(v -> {
            new MaterialDialog.Builder(MainActivity.this)
                    .title("About")
                    .content("This app is made with <3 by abertschi\n"
                            + "and is completely free as in free speech.\n\nFeedback and Code at \n"
                            + "https://github.com/abertschi/flash-on-visit")
                    .positiveText("ROCK ON")
                    .show();
        });
    }

    private void checkIfServerAddressIsValid(String server) {
        new CheckServerAvailabilityTask(isValid -> {
            TextView txt = (TextView) findViewById(R.id.address_validation);
            if (isValid) {
                Utils.hideView(txt, 0);
            } else {
                Utils.showView(txt, 0);
            }
        }).execute(server);
    }

    private void initStartOnBootView() {
        final Switch view = (Switch) findViewById(R.id.start_on_boot_swtich);
        this.mIsEnabledOnBoot = this.mPrefs.getBoolean(App.PREFS_START_ON_BOOT, false);

        view.setChecked(mIsEnabledOnBoot);
        view.setOnClickListener(v -> {
            mIsEnabledOnBoot = !mIsEnabledOnBoot;
            mPrefs.edit().putBoolean(App.PREFS_START_ON_BOOT, mIsEnabledOnBoot).commit();
        });
    }

    private void initPowerView() {
        final Button powerButton = (Button) findViewById(R.id.button);
        powerButton.setText(this.mIsEnabled ? "STOP" : "START");
        if (mIsEnabled) {
            showAnimationConnectedIfNotVisible();
        } else {
            showAnimationDisconnectedIfNotVisible();
        }

        powerButton.setOnClickListener(v -> {
            if (mIsEnabled) {
                mIsEnabled = false;
                powerButton.setText("START");
                unsubscribeFromService();
                showAnimationDisconnectedIfNotVisible();
            } else {
                checkIfServerAddressIsValid(getServerName());
                boolean validInput = true;
                if (getChannelName().isEmpty()) {
                    validInput = false;
                    mChannelTextEdit.requestFocus();
                    mChannelTextEdit.setSelection(0);
                    YoYo.with(Techniques.Shake)
                            .duration(700)
                            .playOn(findViewById(R.id.channel_validation));
                }
                if (getServerName().isEmpty()) {
                    validInput = false;
                    mServerTextEdit.requestFocus();
                    mServerTextEdit.setSelection(0);
                    YoYo.with(Techniques.Shake)
                            .duration(700)
                            .playOn(findViewById(R.id.address_validation));
                }
                if (validInput) {
                    mIsEnabled = true;
                    powerButton.setText("STOP");
                    subscribeToService();
                    showAnimationConnectedIfNotVisible();
                }
            }
            mPrefs.edit().putBoolean(App.PREFS_ENABLED, mIsEnabled).commit();
        });
    }

    private void initLedKernelHackView() {
        this.mLedKernelHackSwitch = (Switch) this.findViewById(R.id.led_kernel_hack);
        mLedKernelHackSwitch.setChecked(mPrefs.getBoolean(App.PREFS_LED_KERNEL_HACK_ENABLED, false));
        mLedKernelHackSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(App.PREFS_LED_KERNEL_HACK_ENABLED, isChecked).commit();
            mFeedbackService.setLedKernelHack(isChecked);

            if (isChecked) {
                addHistoryEntry("Change LED mode to <b>hack</b>");
            } else {
                addHistoryEntry("Change LED mode to <b>normal</b>");
            }
        });
    }

    private void initHistoryCollapseButton() {
        final TextView historyCollapseButton = (TextView) this.findViewById(R.id.moreHistoryButton);
        historyCollapseButton.setOnClickListener(v -> {
            RelativeLayout container = (RelativeLayout) findViewById(R.id.history_recycleview_container);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) container.getLayoutParams();

            if (mIsHistoryCollapsed) {
                historyCollapseButton.setText("LESS");
                mIsHistoryCollapsed = false;
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                mIsHistoryCollapsed = true;
                historyCollapseButton.setText("MORE");
                int height = (int) TypedValue
                        .applyDimension(TypedValue.COMPLEX_UNIT_DIP
                                , HISTORY_COLLAPSED_HEIGHT_DP
                                , getResources().getDisplayMetrics());
                layoutParams.height = height;
            }
            container.setLayoutParams(layoutParams);
        });
    }

    private void initLedOptionsCollapseButton() {
        final TextView button = (TextView) this.findViewById(R.id.button_show_advanced_led);
        button.setOnClickListener(v -> {
            final View container = findViewById(R.id.advanced_options_led);
            TextView textView = (TextView) findViewById(R.id.button_show_advanced_led);
            if (mIsAdvancedLedOptionsCollapsed) {
                Utils.showView(container, 0, 50);
                textView.setText("BASIC");
                mIsAdvancedLedOptionsCollapsed = false;
            } else {
                Utils.hideView(container, 0, 50);
                textView.setText("ADVANCED");
                mIsAdvancedLedOptionsCollapsed = true;
            }
        });
    }

    private void initLedColorChangeViews() {
        final View ledRedButton = this.findViewById(R.id.led_red);
        final View ledBlueButton = this.findViewById(R.id.led_blue);
        final View ledGreenButton = this.findViewById(R.id.led_green);

        int storedColor = mPrefs.getInt(App.PREFS_LED_COLOR, App.LED_COLOR_DEFAULT);
        mLedTextColor = (TextView) findViewById(R.id.led_color_text);
        mLedTextColor.setTextColor(storedColor);

        ledRedButton.setOnClickListener(v -> {
            int color = LED_COLOR_RED;
            mFeedbackService.setLedColor(color);
            mLedTextColor.setTextColor(color);
            mPrefs.edit().putInt(App.PREFS_LED_COLOR, color).commit();
            addHistoryEntry(String.format("Change LED <b>%s</b>", Utils.colorTextInHtml("color", color)));
        });

        ledBlueButton.setOnClickListener(v -> {
            int color = LED_COLOR_BLUE;
            mFeedbackService.setLedColor(color);
            mLedTextColor.setTextColor(color);
            mPrefs.edit().putInt(App.PREFS_LED_COLOR, color).commit();
            addHistoryEntry(String.format("Change LED <b>%s</b>", Utils.colorTextInHtml("color", color)));
        });

        ledGreenButton.setOnClickListener(v -> {
            int color = LED_COLOR_GREEN;
            mFeedbackService.setLedColor(color);
            mLedTextColor.setTextColor(color);
            mPrefs.edit().putInt(App.PREFS_LED_COLOR, LED_COLOR_GREEN).commit();
            addHistoryEntry(String.format("Change LED <b>%s</b>", Utils.colorTextInHtml("color", color)));
        });
    }

    private void initLedTryOutView() {
        final View tryOutLedButton = this.findViewById(R.id.tryoutButton);

        tryOutLedButton.setOnClickListener(v -> {
            mFeedbackService.doExampleFeedback();
            mHistoryAdapter.addAtFront(new HistoryEntry("Example feedback"));

            final ImageView view = (ImageView) findViewById(R.id.lightbulp);
            view.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.mipmap.lightbulb_y2));
            Handler handler = new Handler(Looper.getMainLooper());

            final Runnable r = () -> view.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.mipmap.lightbulb));
            handler.postDelayed(r, 150);
        });
    }

    private void initServerEditText() {
        mServerTextEdit = (EditText) this.findViewById(R.id.serveradress);
        setServerName(mPrefs.getString(App.PREFS_SERVER, App.DEFAULT_SERVER_NAME));

        mServerTextEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkIfServerAddressIsValid(s.toString());
                mPrefs.edit().putString(App.PREFS_SERVER, s.toString()).commit();
            }
        });
    }

    private String getServerName() {
        return mServerTextEdit.getText().toString();
    }

    private void setServerName(String name) {
        mServerTextEdit.setText(name);
    }

    private void initChannelView() {
        this.mChannelTextEdit = (EditText) this.findViewById(R.id.channel);
        setChannelName(mPrefs.getString(App.PREFS_CHANNEL, App.DEFAULT_CHANNEL));

        if (getChannelName().isEmpty()) {
            TextView view = (TextView) findViewById(R.id.channel_validation);
            Utils.showView(view, 0);
        }

        mChannelTextEdit.addTextChangedListener(new TextWatcher() {
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
                    Utils.showView(view, 0);
                } else {
                    Utils.hideView(view, 0);
                }
                mPrefs.edit().putString(App.PREFS_CHANNEL, s.toString()).commit();
            }
        });
    }

    private void setChannelName(String name) {
        mChannelTextEdit.setText(name);
    }

    private String getChannelName() {
        return mChannelTextEdit.getText().toString();
    }

    private void showSplashAnimation() {
        for (int i = 0; i < mRootViewContainer.getChildCount(); i++) {
            View view = mRootViewContainer.getChildAt(i);

            TranslateAnimation translate = new TranslateAnimation(0, 0, 400, 0);
            translate.setDuration(300);
            translate.setStartOffset((100 * i));
            translate.setInterpolator(new DecelerateInterpolator(0.5f));
            translate.setFillAfter(true);

            Animation alpha = new AlphaAnimation(0, 1.0f);
            alpha.setDuration(800);
            alpha.setInterpolator(new DecelerateInterpolator(0.5f));
            alpha.setStartOffset((100 * i));
            alpha.setFillAfter(true);

            AnimationSet animationSet = new AnimationSet(true);
            view.setAnimation(animationSet);
            animationSet.addAnimation(translate);
            animationSet.addAnimation(alpha);
            animationSet.setFillAfter(true);
            animationSet.start();
        }
    }

    private void showAnimationConnectingIfNotVisible() {
        if (mIndicatorConnecting.getVisibility() == View.VISIBLE)
            return;

        boolean hidePrevious = false;
        if (mIndicatorConnected.getVisibility() != View.GONE) {
            Utils.hideView(mIndicatorConnected, 0);
            hidePrevious = true;
        }
        if (mIndicatorDisconnected.getVisibility() != View.GONE) {
            Utils.hideView(mIndicatorDisconnected, 0);
            hidePrevious = true;
        }
        Utils.showView(mIndicatorConnecting, hidePrevious ? DEFAULT_ANIMATION_DURATION * 2 : 0);
    }

    private void showAnimationConnectedIfNotVisible() {
        if (mIndicatorConnected.getVisibility() == View.VISIBLE)
            return;

        Utils.hideView(mIndicatorConnecting, 0);
        if (mIndicatorConnecting.getVisibility() != View.GONE) {
            Utils.hideView(mIndicatorConnecting, 0);
        }
        if (mIndicatorDisconnected.getVisibility() != View.GONE) {
            Utils.hideView(mIndicatorDisconnected, 0);
        }
        Utils.showView(mIndicatorConnected, DEFAULT_ANIMATION_DURATION * 5);
    }

    private void showAnimationDisconnectedIfNotVisible() {
        if (mIndicatorDisconnected.getVisibility() == View.VISIBLE)
            return;

        boolean hidePrevious = false;
        if (mIndicatorConnecting.getVisibility() != View.GONE) {
            Utils.hideView(mIndicatorConnecting, 0);
            hidePrevious = true;
        }
        if (mIndicatorConnected.getVisibility() != View.GONE) {
            Utils.hideView(mIndicatorConnected, 0);
            hidePrevious = true;
        }
        Utils.showView(mIndicatorDisconnected, hidePrevious ? DEFAULT_ANIMATION_DURATION * 2 : 0);
    }

    public class MessageHandler extends Handler {

        public static final int NEW_REQUEST = 1;

        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message message) {
            runOnUiThread(() -> {
                int state = message.what;
                switch (state) {
                    case NEW_REQUEST:
                        addHistoryEntry(message.obj.toString());
                        break;
                }
            });
        }
    }
}