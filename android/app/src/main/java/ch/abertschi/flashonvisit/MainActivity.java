package ch.abertschi.flashonvisit;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.kdb.ledcontrol.LEDManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private static final int LED_NOTIFICATION_ID = 1;
    private com.github.nkzawa.socketio.client.Socket mSocket;

    private SharedPreferences prefs;
    private EditText serverTextEdit;
    private EditText channelTextEdit;
    private Button enableButton;
    private boolean isEnabled = false;

    private static final String SERVER = "server_name";
    private static final String CHANNEL = "channel_name";
    private static final String ENABLED = "is_enabled";

    private static final String DEFAULT_SERVER_NAME = "http://213.136.81.179:3004";

    private LEDManager ledManager;

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
        final Button tryOutLedButton = (Button) this.findViewById(R.id.tryoutButton);

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
        this.enableButton.setText(this.isEnabled ? "TURN OFF" : "TURN ON");

        if (isEnabled) {
            connectSocket();
            Toast.makeText(MainActivity.this, "Connecting ...", Toast.LENGTH_SHORT).show();
        }

        enableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEnabled) {
                    isEnabled = false;
                    enableButton.setText("TURN ON");
                    disconnectSocket();
                    Toast.makeText(MainActivity.this, "Disconnecting ...", Toast.LENGTH_SHORT).show();
                } else {
                    isEnabled = true;
                    enableButton.setText("TURN OFF");
                    connectSocket();
                    Toast.makeText(MainActivity.this, "Connecting ...", Toast.LENGTH_SHORT).show();
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
            }
        });

    }

    private Emitter.Listener onFlash = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("On flash");
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    String message;
                    try {
                        username = data.getString("ip");
                        message = data.getString("channel");
                        flashLedLight();
                    } catch (JSONException e) {
                        return;
                    }
                    // add the message to view
                    System.out.println(username + " " + message);
                }
            });
        }
    };

    private void flashLedLight() {
        if (ledManager.isDeviceSupported()) {
            ledManager.setChoiseToOn();
            ledManager.ApplyBrightness(10);
            ledManager.Apply();

            new android.os.Handler().postDelayed(
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
            mBuilder.setLights(Color.RED, 1, 1); // will blink
            mBuilder.setSmallIcon(R.mipmap.ic_launcher);

            Notification notif = mBuilder.build();

            notif.ledARGB = 0xFFff0000;
            notif.flags = Notification.FLAG_SHOW_LIGHTS;
            notif.ledOnMS = 100;
            notif.ledOffMS = 100;

            nm.notify(LED_NOTIFICATION_ID, notif);
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            nm.cancel(LED_NOTIFICATION_ID);
                        }
                    },
                    500);
        }
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String channel = MainActivity.this.channelTextEdit.getText().toString();
                        Toast.makeText(MainActivity.this, String.format("Connected to channel %s", channel), Toast.LENGTH_SHORT).show();
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

                }
            });
        }
    };

    private void connectSocket() {
        try {
            if (mSocket != null) {
                disconnectSocket();
            }
            mSocket = IO.socket(serverTextEdit.getText().toString());
            mSocket.connect();
            mSocket.on("flash", onFlash);
            mSocket.on("connect", onConnect);
            mSocket.on("disconnect", onDisconnect);

        } catch (URISyntaxException e) {
            System.out.println(e);
        }
    }

    private void disconnectSocket() {
        mSocket.disconnect();
        mSocket.off("connect", onConnect);
        mSocket.off("disconnect", onDisconnect);
        mSocket.off("flash", onFlash);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectSocket();
    }
}