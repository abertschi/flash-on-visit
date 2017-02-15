package ch.abertschi.flashonvisit.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.paolorotolo.appintro.AppIntro;

import ch.abertschi.flashonvisit.App;
import ch.abertschi.flashonvisit.Interfaces;
import ch.abertschi.flashonvisit.R;
import ch.abertschi.flashonvisit.Utils;

/**
 * Created by abertschi on 14.02.17.
 */
public class IntroView extends AppIntro {

    private SharedPreferences mPrefs;
    private static final String TAG = "IntroView";

    private static final int WELCOME = 1;
    private static final int CHANNEL = 2;
    private static final int WEBSITE = 3;
    private static final int GITHUB = 4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (mPrefs.getBoolean(App.PREFS_FIRST_RUN, false)) {
            launchActivity();
            finish();
            return;
        }

        final String welcomeTxt = "Get VISUAL feedback on your website traffic.";
        addSlide(IntroSlide.create(WELCOME, R.layout.intro_welcome, R.id.logo, welcomeTxt, (writer) -> {
            writer.pause(1000)
                    .type(welcomeTxt)
                    .pause(1000).delete(".")
                    .pause(300).type("\n\n:D");
        }));

        final String channelTxt = "A channel is an identifier for your website.";
        addSlide(IntroSlide.create(CHANNEL, R.layout.intro_channel, R.id.choose_channel_logo, channelTxt, (writer) -> {
            writer.pause(300).type(channelTxt);
        }));

        final String websiteTxt = "Notify the the app whenever you get a new visit.";
        addSlide(IntroSlide.create(WEBSITE, R.layout.intro_website, R.id.img, websiteTxt, (writer) -> {
            writer.pause(300).type(websiteTxt);
        }));

        final String githubTxt = "Dig into the code on Github.";
        addSlide(IntroSlide.create(GITHUB, R.layout.intro_github, R.id.img, githubTxt, (writer) -> {
            writer.pause(300).type(githubTxt);
        }));

        showSkipButton(true);
        setProgressButtonEnabled(true);
        setVibrate(true);
        setVibrateIntensity(30);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        launchActivity();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        launchActivity();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        if (newFragment instanceof IntroSlide) {
            IntroSlide slide = (IntroSlide) newFragment;
            slide.start();
            Log.i(TAG, "new slide: started" + newFragment.toString());
        }
    }

    private void launchActivity() {
        mPrefs.edit().putBoolean(App.PREFS_FIRST_RUN, true).commit();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public static class IntroSlide extends Fragment {

        private int mId;
        private String mFinalText;
        private int mLayoutId;
        private int mImageId;
        private Interfaces.Consumer<TypewriterView> mTextFunction;

        private View mImage;
        private TypewriterView mTypewriterView;
        private boolean mInit = false;

        public static IntroSlide create(int id, int layoutId, int imageId, String finalText,
                                        Interfaces.Consumer<TypewriterView> textFunction) {
            IntroSlide slide = new IntroSlide();
            slide.mId = id;
            slide.mFinalText = finalText;
            slide.mLayoutId = layoutId;
            slide.mImageId = imageId;
            slide.mTextFunction = textFunction;
            return slide;
        }

        public int getIdentification() {
            return mId;
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(mLayoutId, container, false);
        }

        public void start() {
            if (!mInit) {
                mTextFunction.accept(mTypewriterView);
                mInit = true;
            } else {
                mTypewriterView.setText(mFinalText);
            }
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mImage = view.findViewById(mImageId);
            mTypewriterView = (TypewriterView) view.findViewById(R.id.tagline_typewriter);

            mImage.setVisibility(View.GONE);
            Utils.showView(mImage, 100, 1000);

        }
    }
}

