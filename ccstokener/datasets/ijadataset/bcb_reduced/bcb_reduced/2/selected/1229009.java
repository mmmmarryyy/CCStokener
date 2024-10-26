package com.softwaresmithy.library.impl;

import android.net.Uri;
import android.util.Log;
import com.softwaresmithy.library.AndroidLibStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.MatchResult;

public class WebPac extends AndroidLibStatus {

    private String isbnSearchUrl;

    private static final String holdRegex = "((\\d*) hold[s]? on first copy returned of (\\d*) )?[cC]opies";

    private static final int totalGroups = 3;

    private static final int numHoldsGroup = 2;

    private static final int numCopiesGroup = 3;

    @Override
    public void init(Map<String, String> args) {
        if (args.containsKey("url")) {
            this.isbnSearchUrl = args.get("url");
        }
    }

    @Override
    public STATUS checkAvailability(String isbn) {
        HttpGet get = null;
        try {
            HttpClient client = new DefaultHttpClient();
            get = new HttpGet(String.format(this.isbnSearchUrl, isbn));
            HttpResponse resp = client.execute(get);
            Scanner s = new Scanner(resp.getEntity().getContent());
            String pattern = s.findWithinHorizon(holdRegex, 0);
            if (pattern != null) {
                MatchResult match = s.match();
                if (match.groupCount() == totalGroups) {
                    if (match.group(numHoldsGroup) == null) {
                        return STATUS.AVAILABLE;
                    }
                    int numHolds = Integer.parseInt(match.group(numHoldsGroup));
                    int numCopies = Integer.parseInt(match.group(numCopiesGroup));
                    if (numHolds < numCopies) {
                        return STATUS.SHORT_WAIT;
                    } else if (numHolds >= numCopies && numHolds <= (2 * numCopies)) {
                        return STATUS.WAIT;
                    } else {
                        return STATUS.LONG_WAIT;
                    }
                }
            }
            return STATUS.NO_MATCH;
        } catch (Exception e) {
            Log.e(this.getClass().getName(), e.getMessage(), e);
            return null;
        } finally {
            if (get != null) {
                get.abort();
            }
        }
    }

    @Override
    public boolean isCompatible(String url) throws URISyntaxException {
        URI checkurl = new URI(url);
        HttpGet get = null;
        try {
            HttpClient client = new DefaultHttpClient();
            get = new HttpGet(checkurl);
            HttpResponse resp = client.execute(get);
            Scanner s = new Scanner(resp.getEntity().getContent());
            String pattern = s.findWithinHorizon("<link.*\"/scripts/ProStyles.css\"", 0);
            return (pattern != null && !pattern.equals(""));
        } catch (ClientProtocolException e) {
            Log.e(this.getClass().getName(), "failed checking compatibility", e);
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "failed checking compatibility", e);
        } finally {
            if (get != null) {
                get.abort();
            }
        }
        return false;
    }

    @Override
    public Uri getStatusPage(String isbn) {
        return Uri.parse(String.format(isbnSearchUrl, isbn));
    }
}
