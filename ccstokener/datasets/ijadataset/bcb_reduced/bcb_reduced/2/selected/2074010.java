package hambo.pim;

import java.net.URL;
import java.net.URLConnection;
import hambo.user.User;

/**
 * SyncTrigger is a convenient class that makes it possible from the
 * java code to perform a sync for a given user.
 * <p>
 * <pre>
 *   SyncTrigger trigger = new SyncTrigger(user);
 *   trigger.sync();
 *   if ( trigger.getStatus() != SyncTrigger.SYNC_COMPLETED) {
 *     // handle failure
 *   }
 */
public class SyncTrigger extends F1RedirectionRequest {

    public static final int SYNC_IN_PROGRESS = 0;

    public static final int SYNC_COMPLETED = 1;

    public static final int SYNC_FAILED = 2;

    public static final int SYNC_INITIATING = 4;

    private int itsStatus = SYNC_INITIATING;

    /**
   * Creates a new sync trigger for user.
   */
    public SyncTrigger(User user) {
        super(user, SYNC);
    }

    /**
   * Returns the triggers current status.
   */
    public int getStatus() {
        return itsStatus;
    }

    /**
   * Triggers the sync.
   */
    public void sync() {
        progress("Sync initiating");
        run();
        if (getErrorCode() != 0) {
            progress("Sync failed!");
            itsStatus = SYNC_FAILED;
            return;
        }
        try {
            String response = null;
            URL triggerURL = new URL(getRedirectURL());
            response = readURLContent(triggerURL);
            URL feedbackURL = new URL(triggerURL, retrieveURL(response));
            for (int i = 0; checkResponse(response) && i < 10; i++) {
                try {
                    Thread.sleep(2000);
                } catch (Exception ex) {
                }
                response = readURLContent(feedbackURL);
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    /** JUNK */
    protected void progress(String progress) {
        System.out.println(progress);
    }

    /**
   *�Retrieves the refresh url from the html code.
   *
   *�@param html the html text
   * @return an url string
   */
    protected String retrieveURL(String html) {
        int start = html.indexOf("URL=") + 4;
        int end = html.indexOf("\"", start);
        return html.substring(start, end);
    }

    /**
   * Checks the status shown by the html page.
   *
   * @param  html html text
   * @return true if we still are sync'ing
   */
    protected boolean checkResponse(String response) {
        boolean inProgress = false;
        if (response.indexOf("Sync in progress") > 0) {
            progress("Sync in progress");
            inProgress = true;
        } else if (response.indexOf("Sync complete") > 0) {
            progress("Sync complete");
            itsStatus = SYNC_COMPLETED;
        } else {
            progress("Sync failed!");
            itsStatus = SYNC_FAILED;
        }
        return inProgress;
    }

    /**
   * Reads the content from an url.
   *
   * @param url the url
   * @return the content
   */
    protected String readURLContent(URL url) throws Exception {
        URLConnection urlConn = url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setUseCaches(false);
        return readResponse(urlConn);
    }
}
