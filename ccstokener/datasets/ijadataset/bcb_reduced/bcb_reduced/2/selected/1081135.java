package org.rockaa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Pattern;
import org.rockaa.gui.GUIFactory;
import org.rockaa.observer.Observable;
import org.rockaa.observer.Observer;
import org.rockaa.search.SearchKiller;
import org.rockaa.translation.Translator;

/**
 * A class for downloading the content of a HTML page via HTTP in a thread.<br>
 * Uses Observer pattern for returning the result<br>
 * The number of parallel HTTP threads is limited
 * @author Johannes Degler
 *
 */
public class HTTPDownloadThread extends Observable implements Runnable, Observer<SearchKiller> {

    private static int threadCount = 0;

    private final String url;

    private String content;

    private boolean killed = false;

    private Pattern keepPattern;

    private Pattern endPattern;

    private final boolean displayError;

    /**
	 * Simple constructor for downloading the complete content from an URL
	 * @param url The url to download
	 */
    public HTTPDownloadThread(final String url, final boolean displayError) {
        this.displayError = displayError;
        this.url = url;
    }

    /**
	 * Advanced constructor which uses Regex patterns that define which parts of the content should be returned and when the download can be aborted early
	 * @param url The url to download
	 * @param keepPattern Keep only lines of the content matching this pattern
	 * @param endPattern Abort download early if a line matches this pattern
	 */
    public HTTPDownloadThread(final String url, final Pattern keepPattern, final Pattern endPattern, final boolean displayError) {
        this.displayError = displayError;
        SearchKiller.getInstance().addObserver(this);
        this.url = url;
        this.keepPattern = keepPattern;
        this.endPattern = endPattern;
    }

    /**
	 * Get the number of active HTTPDownloadThreads
	 * @return
	 */
    public static int getThreadCount() {
        return HTTPDownloadThread.threadCount;
    }

    private void download() {
        try {
            this.content = this.getContent(this.url);
        } catch (final IOException e) {
            if (this.displayError) {
                e.printStackTrace();
                GUIFactory.getGUI().displayErrorMessage(Translator.translate("error_downloading").replace("%source", this.url));
            }
        }
    }

    /**
	 * Get the content the HTTPDownloadThread downloaded
	 * @return Downloaded content
	 */
    public String getContent() {
        return this.content;
    }

    private String getContent(final String adress) throws IOException {
        final URL url = new URL(adress);
        final StringBuilder content = new StringBuilder();
        final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        for (String s; (s = in.readLine()) != null && !this.killed; ) {
            if (this.matchesKeppPattern(s)) content.append(s);
            if (this.matchesEndPattern(s)) break;
        }
        in.close();
        return content.toString();
    }

    private boolean matchesEndPattern(final String s) {
        return this.endPattern != null && this.endPattern.matcher(s).matches();
    }

    private boolean matchesKeppPattern(final String s) {
        return this.keepPattern == null || this.keepPattern.matcher(s).matches();
    }

    public void run() {
        this.waitForFreeSlot();
        if (this.killed) return;
        HTTPDownloadThread.threadCount++;
        this.download();
        HTTPDownloadThread.threadCount--;
        if (this.killed) return;
        this.setChanged();
        this.notifyObservers();
        SearchKiller.getInstance().removeObserver(this);
    }

    public void update(final SearchKiller observable) {
        this.killed = true;
    }

    private void waitForFreeSlot() {
        while (!this.killed && HTTPDownloadThread.threadCount >= Setting.MAX_HTTP_THREADS.getValue()) try {
            Thread.sleep(10);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }
}
