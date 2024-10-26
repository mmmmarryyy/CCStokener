package com.trackerdogs.websources;

import java.util.*;
import java.net.*;
import java.io.*;
import javax.naming.*;
import javax.naming.directory.*;
import com.trackerdogs.websources.results.*;
import com.trackerdogs.search.*;

/**********************************************************************
 * This class represents page of a search engine. It should always be a
 * part of  a certain search engine
 * 
 * Contains how to fetch results
 *
 * @author Koen Witters
 * @version $Header: /cvsroot/trackerdogs/trackerdogs/src/com/trackerdogs/websources/WebSourcePage.java,v 1.2 2002/08/10 15:38:12 kwitters Exp $
 *
 * FIXME: make sure only SearchEngine uses this class
 */
public class WebSourcePage extends Thread {

    private static final int MAX_BUFFER_SIZE = 500;

    private static final int TIMEOUT = 10000;

    private String log_;

    private URL url_;

    private Results results_;

    private WebSource webSource_;

    private boolean fAllResultsExtracted;

    private boolean fInError;

    private Keywords fKeys;

    private int score_;

    private int resultNo_;

    private int newPages;

    private long startTime_;

    private long totalTime_;

    private boolean runThread_;

    private Reader is_;

    private int pageNr_;

    private int lastPlaceResult_;

    private URL nextPage_;

    /**
     * init the WebSourcePage with the specified WebSource, Keyword.
     */
    public WebSourcePage(WebSource se, Keywords keys) {
        url_ = se.getUrl(keys);
        webSource_ = se;
        pageNr_ = 0;
        lastPlaceResult_ = 0;
        runThread_ = true;
        fAllResultsExtracted = false;
        fInError = false;
        totalTime_ = -1;
        log_ = new String();
        this.score_ = 0;
        this.resultNo_ = 0;
        this.newPages = 0;
    }

    /**
     * Construct the next WebSourcePage
     */
    private WebSourcePage(WebSourcePage wsp, URL url) {
        url_ = url;
        webSource_ = wsp.getWebSource();
        pageNr_ = wsp.pageNr_ + 1;
        lastPlaceResult_ = wsp.lastPlaceResult_;
        runThread_ = true;
        fAllResultsExtracted = false;
        fInError = false;
        totalTime_ = -1;
        log_ = new String();
        this.score_ = 0;
        this.resultNo_ = 0;
        this.newPages = 0;
    }

    /**
     * Fetch the results and put them in Results
     *
     * @param res to put the results in
     */
    public void fetchResultsIn(Results res) {
        results_ = res;
        startTime_ = (new Date()).getTime();
        start();
    }

    private void logIt(String message) {
        this.log_ += message + "\n";
    }

    /**********************************************************************
     * Returns the log (written in case of an error)
     *
     * @return the log
     */
    public String getLog() {
        return log_;
    }

    public void run() {
        try {
            try {
                BufferedReader is = new BufferedReader(new InputStreamReader(url_.openStream()));
                webSource_.getWrapper().wrapResults(is, this);
                is.close();
            } catch (NullPointerException ex) {
                logIt("WebSourcePage.run() nullpointer: " + ex);
                fInError = true;
            }
            totalTime_ = (new Date()).getTime() - startTime_;
            fAllResultsExtracted = true;
        } catch (NoRouteToHostException ex) {
            logIt("WebSourcePage.run() NoRouteToHost: " + ex);
            fInError = true;
        } catch (ConnectException ex) {
            logIt("WebSourcePage.run(): " + ex);
            fInError = true;
        } catch (MalformedURLException ex) {
            logIt("WebSourcePage.run(): " + ex);
            fInError = true;
        } catch (IOException ex) {
            logIt("WebSourcePage.run(): " + ex);
            fInError = true;
        }
    }

    public int getAverageScore() {
        if (resultNo_ > 0) return this.score_ / resultNo_;
        return 0;
    }

    public int getNewPages() {
        return this.newPages;
    }

    public void addNewPage() {
        this.newPages++;
    }

    /**
     * returns this pages search engine
     */
    public WebSource getWebSource() {
        return webSource_;
    }

    protected void skipTextUntil(Reader in, String until) throws IOException {
        int pos = 0;
        int ch;
        boolean other = false;
        while (runThread_) {
            ch = in.read();
            if (until.charAt(pos) != ch) {
                if (other) {
                    other = false;
                    in.reset();
                }
                if (ch == -1) {
                    throw new IOException();
                }
                pos = 0;
            } else {
                pos++;
                if (pos == until.length()) {
                    return;
                }
                if (until.charAt(0) == ch && (pos > 1) && !other) {
                    in.mark(until.length());
                    other = true;
                }
            }
        }
    }

    protected String getTextUntil(Reader in, String until) throws IOException {
        String ret = new String();
        int pos = 0;
        int ch;
        boolean other = false;
        while (runThread_) {
            ch = in.read();
            ret += (char) ch;
            if (until.charAt(pos) != ch) {
                if (other) {
                    other = false;
                    in.reset();
                }
                if (ch == -1) {
                    throw new IOException();
                }
                pos = 0;
            } else {
                pos++;
                if (pos == until.length()) {
                    return ret.substring(0, ret.length() - until.length());
                }
                if (until.charAt(0) == ch && (pos > 1) && !other) {
                    in.mark(until.length());
                    other = true;
                }
            }
        }
        return null;
    }

    protected void skipTextUntilNoCase(BufferedReader in, String until) throws IOException {
        int pos = 0;
        int ch;
        char chr;
        boolean other = false;
        until = until.toLowerCase();
        while (runThread_) {
            ch = in.read();
            chr = Character.toLowerCase((char) ch);
            if (until.charAt(pos) != chr) {
                if (other) {
                    other = false;
                    in.reset();
                }
                if (ch == -1) {
                    throw new IOException();
                }
                pos = 0;
            } else {
                pos++;
                if (pos == until.length()) {
                    return;
                }
                if (until.charAt(0) == chr && (pos > 1) && !other) {
                    in.mark(until.length());
                    other = true;
                }
            }
        }
    }

    protected String getTextUntilNoCase(Reader in, String until) throws IOException {
        int pos = 0;
        char chr;
        boolean other = false;
        String ret = new String();
        until = until.toLowerCase();
        while (runThread_) {
            chr = Character.toLowerCase((char) in.read());
            ret += chr;
            if (until.charAt(pos) != chr) {
                if (other) {
                    other = false;
                    in.reset();
                }
                if (chr == ((char) -1)) {
                    throw new IOException();
                }
                pos = 0;
            } else {
                pos++;
                if (pos == until.length()) {
                    return ret.substring(0, ret.length() - until.length());
                }
                if (until.charAt(0) == chr && (pos > 1) && !other) {
                    in.mark(until.length());
                    other = true;
                }
            }
        }
        return null;
    }

    protected static Vector removePrefix(Vector keywords) {
        String key;
        Vector noPrefix = new Vector();
        for (int i = 0; i < keywords.size(); i++) {
            key = (String) keywords.elementAt(i);
            if (key.substring(0, 1).equals("+") || key.substring(0, 1).equals("-")) {
                key = key.substring(1);
            }
            noPrefix.addElement(key);
        }
        return noPrefix;
    }

    protected static String removeTags(String str) {
        String notags = new String();
        while (str.indexOf("<") > -1) {
            notags = notags + str.substring(0, str.indexOf("<"));
            if (str.indexOf(">") != -1) {
                str = str.substring(str.indexOf(">") + 1);
            } else {
                str = "";
            }
        }
        notags = notags + str;
        return notags;
    }

    /**********************************************************************
     * Returns true if all results are extracted. If this is used in a loop
     * , please AND it with errorOccured().
     * This can still hang when the URL.openStream() hangs.... fuck!
     * This can still hang when the is_.close() hangs.... FUCK!
     *
     * @return true if all results are extracted without errors.
     */
    public boolean allResultsExtracted() {
        if ((startTime_ + TIMEOUT) < (new Date()).getTime()) {
            fInError = true;
            if (this.isAlive()) {
                this.runThread_ = false;
            } else {
                fAllResultsExtracted = true;
            }
            totalTime_ = (new Date()).getTime() - startTime_;
        }
        return fAllResultsExtracted;
    }

    /**********************************************************************
     * Returns the total time this page has been running.
     */
    public long getTotalTime() {
        if (totalTime_ == -1) {
            return (new Date()).getTime() - startTime_;
        } else {
            return totalTime_;
        }
    }

    /************************************************************
     * Returns true if an error occured during retreival of the 
     * page.
     *
     * @return true if an error occured
     */
    public boolean errorOccured() {
        return fInError;
    }

    public Results getResults() {
        return results_;
    }

    public void addResult(Result result) {
        result.addSource(webSource_, lastPlaceResult_);
        lastPlaceResult_++;
        try {
            results_.addElement(result, this);
        } catch (Exception ex) {
            assert false : ex.getMessage();
        }
    }

    /************************************************************
     * returns the Page with results after this one
     *
     * @return null if this is the last page
     */
    public WebSourcePage getNextPage() {
        return null;
    }

    /************************************************************
     * Returns the page number of this page. Counting starts at 0.
     */
    public int getPageNr() {
        return pageNr_;
    }
}
