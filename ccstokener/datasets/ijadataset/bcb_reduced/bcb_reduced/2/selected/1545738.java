package com.inorout.server.lastfm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import com.google.gson.Gson;
import com.inorout.server.lastfm.artist.TopArtistsLastFMContainer;
import com.inorout.server.lastfm.tracks.TopTracksLastFMContainer;

public class LastFmHelper {

    private static String URL_LAST_FM = "http://ws.audioscrobbler.com/2.0/";

    private static String API_KEY_LAST_FM = "456c6b299b630d3bc8749f60a6c980a5";

    private static String METHODE_CHART_GETTOPARTISTS = "chart.gettopartists";

    private static String METHODE_CHART_GETTOPTRACKS = "chart.gettoptracks";

    private static String FORMAT_JSON = "json";

    public static TopArtistsLastFMContainer getTopArtistsFromLastFM() {
        TopArtistsLastFMContainer artistsContainerLastFM = null;
        try {
            URL url = new URL(URL_LAST_FM + "?method=" + METHODE_CHART_GETTOPARTISTS + "&format=" + FORMAT_JSON + "&api_key=" + API_KEY_LAST_FM);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = reader.readLine();
            Gson gson = new Gson();
            artistsContainerLastFM = gson.fromJson(line, TopArtistsLastFMContainer.class);
            reader.close();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return artistsContainerLastFM;
    }

    public static TopTracksLastFMContainer getTopTracksFromLastFM() {
        TopTracksLastFMContainer tracksContainerLastFM = null;
        try {
            URL url = new URL(URL_LAST_FM + "?method=" + METHODE_CHART_GETTOPTRACKS + "&format=" + FORMAT_JSON + "&api_key=" + API_KEY_LAST_FM);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = reader.readLine();
            Gson gson = new Gson();
            tracksContainerLastFM = gson.fromJson(line, TopTracksLastFMContainer.class);
            reader.close();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return tracksContainerLastFM;
    }
}
