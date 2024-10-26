package org.openstreetmap.xapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import org.openstreetmap.xapi.interfaces.DataCache;
import org.openstreetmap.xapi.interfaces.DataSource;

public class OsmDataLoader {

    /**
	 * Holds the used user agent used for HTTP requests. If this field is
	 * <code>null</code>, the default Java user agent is used.
	 */
    public static String USER_AGENT = null;

    public static String ACCEPT = "text/plain";

    DataLoaderListener listener;

    public OsmDataLoader(DataLoaderListener listener) {
        this.listener = listener;
    }

    public Runnable getLoader(final DataSource dataSource, final OsmData osmData) {
        return new Runnable() {

            public void run() {
                loadData(dataSource, osmData);
            }
        };
    }

    private void loadData(DataSource dataSource, OsmData osmData) {
        DataCache cache = listener.getDataCache();
        OsmData data = cache.getData(osmData.getBbox(), osmData.getQuery());
        if (data == null) {
            Writer dataWriter = null;
            BufferedReader sourceReader = null;
            try {
                cache.addData(osmData);
                dataWriter = osmData.getRawDataProvider().getDataWriter();
                InputStream inStream = loadDataFromOsm(dataSource, osmData).getInputStream();
                sourceReader = new BufferedReader(new InputStreamReader(inStream));
                String inputLine;
                int bytes = 0;
                while ((inputLine = sourceReader.readLine()) != null) {
                    bytes += inputLine.length();
                    listener.bytesLoaded(bytes);
                    dataWriter.write(inputLine);
                }
                sourceReader.close();
                dataWriter.close();
            } catch (Exception e) {
                listener.dataLoadingFinished(osmData, false);
            }
            listener.dataLoadingFinished(osmData, true);
        } else {
            listener.dataLoadingFinished(data, true);
        }
    }

    protected HttpURLConnection loadDataFromOsm(DataSource dataSource, OsmData data) throws IOException {
        URL url;
        url = new URL(dataSource.getUrl() + data.getUrl());
        System.out.println("url: " + url.toString());
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setReadTimeout(30000);
        return urlConn;
    }

    protected void prepareHttpUrlConnection(HttpURLConnection urlConn) {
        if (USER_AGENT != null) urlConn.setRequestProperty("User-agent", USER_AGENT);
        urlConn.setRequestProperty("Accept", ACCEPT);
    }
}
