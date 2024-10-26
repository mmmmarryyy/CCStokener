package de.peacei.android.foodwatcher.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import de.peacei.android.foodwatcher.gui.R;

/**
 * @author peacei
 *
 */
public abstract class AbstractFetchTask<T> extends AsyncTask<Object, Object, List<T>> {

    private Fetcher<T> fetcher = null;

    protected Context cxt;

    AbstractFetchTask(Fetcher<T> fetcher, Context context) {
        this.fetcher = fetcher;
        this.cxt = context;
    }

    @Override
    protected void onPreExecute() {
        fetcher.onPreFetch();
    }

    @Override
    protected void onCancelled() {
        fetcher.onCancelled();
    }

    @Override
    protected void onPostExecute(List<T> results) {
        fetcher.onFetched(results);
    }

    @Override
    protected abstract List<T> doInBackground(Object... params);

    protected HttpURLConnection getConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        StringBuilder agentBuilder = new StringBuilder();
        agentBuilder.append(cxt.getString(R.string.app_name)).append(' ').append(cxt.getString(R.string.app_version)).append('|').append(Build.DISPLAY).append('|').append(VERSION.RELEASE).append('|').append(Build.ID).append('|').append(Build.MODEL).append('|').append(Locale.getDefault().getLanguage()).append('-').append(Locale.getDefault().getCountry());
        connection.setRequestProperty("User-Agent", agentBuilder.toString());
        return connection;
    }

    protected String getStringFromInputStream(final InputStream is) throws IOException {
        String line = "";
        StringBuilder builder = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        while ((line = rd.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }
}
