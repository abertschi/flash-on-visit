package ch.abertschi.flashonvisit;

import android.os.AsyncTask;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Checks if a hostaddress is reachable
 * <p>
 * Created by abertschi on 11.02.17.
 */
public class CheckServerAvailabilityTask extends AsyncTask<String, Void, Boolean> {

    private Utils.Argument<Boolean> callable;

    public CheckServerAvailabilityTask(Utils.Argument<Boolean> callable) {
        this.callable = callable;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        boolean reachable = false;
        String host = params[0];
        int port;
        try {
            if (!host.startsWith("http://") && !host.startsWith("https://")) {
                host = "http://" + host;
            }
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

            URL url = new URL(host + ":" + port);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            reachable = true;

        } catch (Exception e) {
        }
        return reachable;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        callable.apply(result);
    }
}

