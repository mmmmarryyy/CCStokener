package com.totsp.restaurant.data;

import android.util.Log;
import android.util.Xml;
import com.totsp.restaurant.Constants;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Use Google Base with specified criteria to obtain Review data.
 * 
 * @author charliecollins
 */
public class ReviewFetcher {

    private static final String CLASSTAG = ReviewFetcher.class.getSimpleName();

    private static final String QBASE = "http://www.google.com/base/feeds/snippets/-/reviews?bq=[review%20type:restaurant]";

    private static final String QD_PREFIX = "[description:";

    private static final String QD_SUFFIX = "]";

    private static final String QL_PREFIX = "[location:";

    private static final String QL_SUFFIX = "]";

    private static final String QMAX_RESULTS = "&max-results=";

    private static final String QR_PREFIX = "[rating:";

    private static final String QR_SUFFIX = "]";

    private static final String QSTART_INDEX = "&start-index=";

    private final int numResults;

    private String query;

    private final int start;

    /**
     * Construct ReviewFetcher with location, description, rating, and paging params.
     * 
     * @param location
     * @param description
     * @param rating
     * @param start
     * @param numResults
     */
    public ReviewFetcher(String loc, String description, String rat, int start, int numResults) {
        Log.v(Constants.LOG_TAG, " " + ReviewFetcher.CLASSTAG + " location = " + loc + " rating = " + rat + " start = " + start + " numResults = " + numResults);
        this.start = start;
        this.numResults = numResults;
        String location = loc;
        String rating = rat;
        try {
            if (location != null) {
                location = URLEncoder.encode(location, "UTF-8");
            }
            if (rating != null) {
                rating = URLEncoder.encode(rating, "UTF-8");
            }
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        this.query = ReviewFetcher.QBASE;
        if ((rating != null) && !rating.equals("ALL")) {
            this.query += (ReviewFetcher.QR_PREFIX + rating + ReviewFetcher.QR_SUFFIX);
        }
        if ((location != null) && !location.equals("")) {
            this.query += (ReviewFetcher.QL_PREFIX + location + ReviewFetcher.QL_SUFFIX);
        }
        if ((description != null) && !description.equals("ANY")) {
            this.query += (ReviewFetcher.QD_PREFIX + description + ReviewFetcher.QD_SUFFIX);
        }
        this.query += (ReviewFetcher.QSTART_INDEX + this.start + ReviewFetcher.QMAX_RESULTS + this.numResults);
        Log.v(Constants.LOG_TAG, " " + ReviewFetcher.CLASSTAG + " query - " + this.query);
    }

    /**
     * Call Google Base and parse via SAX.
     * 
     * @return
     */
    public ArrayList<Review> getReviews() {
        long startTime = System.currentTimeMillis();
        ArrayList<Review> results = null;
        try {
            ReviewHandler handler = new ReviewHandler();
            URL url = new URL(this.query);
            Xml.parse(url.openStream(), Xml.Encoding.UTF_8, handler);
            results = handler.getReviews();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long duration = System.currentTimeMillis() - startTime;
        Log.v(Constants.LOG_TAG, " " + ReviewFetcher.CLASSTAG + " call and parse duration - " + duration);
        return results;
    }
}
