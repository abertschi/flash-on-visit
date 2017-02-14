package ch.abertschi.flashonvisit.view;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntro2Fragment;
import com.github.paolorotolo.appintro.AppIntroFragment;

import ch.abertschi.flashonvisit.App;
import ch.abertschi.flashonvisit.R;
import ch.abertschi.flashonvisit.Utils;

/**
 * Created by abertschi on 14.02.17.
 */
public class IntroView extends AppIntro {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(App.PREFS_FIRST_RUN, false)) {
            launchActivity();
            finish();
            return;
        }

        addSlide(new IntroFragment());
        addSlide(new Overview());
        addSlide(new SetupSite());
        addSlide(new GithubSite());

        // Hide Skip/Done button.
        showSkipButton(true);
        setProgressButtonEnabled(true);

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
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

    private void launchActivity() {
        prefs.edit().putBoolean(App.PREFS_FIRST_RUN, true).commit();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);

        if (newFragment instanceof Overview) {
            ((Overview) newFragment).start();
        } else if (newFragment instanceof SetupSite) {
            ((SetupSite) newFragment).start();
        } else if (newFragment instanceof GithubSite) {
            ((GithubSite) newFragment).start();
        }
    }

    public static class IntroFragment extends Fragment {

        private View logo;
        private TypewriterView typewriterView;
        private static boolean init = false;

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.intro_welcome, container, false);
        }

        public void start() {
            new Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        public void run() {
                            if (!init) {
                                typewriterView.pause(1000).type("Get VISUAL feedback on your website traffic.")
                                        .pause(1000).delete(".").pause(300).type("\n\n:D");
                                init = true;
                            }
                        }
                    },
                    0);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            logo = view.findViewById(R.id.logo);
            logo.setVisibility(View.GONE);
            Utils.showView(logo, 100, 500);
            typewriterView = (TypewriterView) view.findViewById(R.id.tagline_typewriter);
            start();

        }
    }

    public static class Overview extends Fragment {

        private View img;

        private TypewriterView typewriterView;

        private static boolean init = false;

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.intro_channel, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            img = view.findViewById(R.id.choose_channel_logo);
            img.setVisibility(View.GONE);
            Utils.showView(img, 100, 500);
            typewriterView = (TypewriterView) view.findViewById(R.id.tagline_typewriter);
        }

        public void start() {
            final String msg = "A channel is an identifier for your website.";
            new Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        public void run() {
                            if (!init) {
                                typewriterView.pause(300).type(msg);
                                init = true;
                            }
                        }
                    },
                    0);
        }
    }

    public static class SetupSite extends Fragment {

        private View img;

        private TypewriterView typewriterView;

        private static boolean init = false;

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.intro_website, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            img = view.findViewById(R.id.img);
            img.setVisibility(View.GONE);
            Utils.showView(img, 100, 500);
            typewriterView = (TypewriterView) view.findViewById(R.id.tagline_typewriter);
        }

        public void start() {
            final String msg = "Notify the the app whenever you get a new visit.";
            new Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        public void run() {
                            if (!init) {
                                typewriterView.pause(300).type(msg);
                                init = true;
                            }
                        }
                    },
                    0);
        }
    }

    public static class GithubSite extends Fragment {

        private View img;

        private TypewriterView typewriterView;

        private static boolean init = false;

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.intro_github, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            img = view.findViewById(R.id.img);
            img.setVisibility(View.GONE);
            Utils.showView(img, 100, 500);
            typewriterView = (TypewriterView) view.findViewById(R.id.tagline_typewriter);
        }

        public void start() {
            final String msg = "Dig into the code on Github.";
            new Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        public void run() {
                            if (!init) {
                                typewriterView.pause(300).type(msg);
                                init = true;
                            }
                        }
                    },
                    0);
        }
    }
}

