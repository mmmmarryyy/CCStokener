package net.assimilator.examples.sca.web.tomcat.installer;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

/**
 * Abstract base class for install tasks that interact with the
 * <em>Tomcat Manager</em> web application for dynamically deploying,
 * installing, undeploying and removing applications.
 *
 * @author Kevin Hartig
 * @version $Id: AbstractTask.java 142 2007-04-28 20:54:47Z khartig $
 */
public abstract class AbstractTask {

    /**
     * The login password for the <code>Manager</code> application.
     */
    protected String password = null;

    protected static final String CHAR_ENCODING = "UTF-8";

    protected Logger logger = Logger.getLogger("net.assimilator.examples.sca.web.tomcat");

    public String getPassword() {
        return (this.password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * The URL of the <code>Manager</code> application to be used.
     */
    protected String url = "http://localhost:8080/manager/html";

    public String getUrl() {
        return (this.url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * The login username for the <code>Manager</code> application.
     */
    protected String username = null;

    public String getUsername() {
        return (this.username);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Execute the specified command.  This logic only performs the common
     * attribute validation required by all subclasses; it does not perform
     * any functional logic directly.
     *
     * @throws TaskExecutionException if a validation error occurs
     */
    public void execute() throws TaskExecutionException {
        if ((username == null) || (password == null) || (url == null)) {
            throw new TaskExecutionException("Must specify all of 'username', 'password', and 'url'");
        }
    }

    /**
     * Execute the specified command, based on the configured properties.
     *
     * @param command Command to be executed
     * @throws TaskExecutionException if an error occurs
     */
    public void execute(String command) throws TaskExecutionException {
        execute(command, null, null, -1);
    }

    /**
     * Execute the specified command, based on the configured properties.
     * The input stream will be closed upon completion of this task, whether
     * it was executed successfully or not.
     *
     * @param command       Command to be executed
     * @param istream       InputStream to include in an HTTP PUT, if any
     * @param contentType   Content type to specify for the input, if any
     * @param contentLength Content length to specify for the input, if any
     * @throws TaskExecutionException if an error occurs
     */
    public void execute(String command, InputStream istream, String contentType, int contentLength) throws TaskExecutionException {
        URLConnection conn;
        InputStreamReader reader = null;
        try {
            logger.info("Task execution using URL = " + url + command);
            conn = (new URL(url + command)).openConnection();
            HttpURLConnection hconn = (HttpURLConnection) conn;
            hconn.setAllowUserInteraction(false);
            hconn.setDoInput(true);
            hconn.setUseCaches(false);
            if (istream != null) {
                hconn.setDoOutput(true);
                hconn.setRequestMethod("PUT");
                if (contentType != null) {
                    hconn.setRequestProperty("Content-Type", contentType);
                }
                if (contentLength >= 0) {
                    hconn.setRequestProperty("Content-Length", "" + contentLength);
                }
            } else {
                hconn.setDoOutput(false);
                hconn.setRequestMethod("GET");
            }
            hconn.setRequestProperty("User-Agent", "Project-Assimilator-Reload-Task/1.0");
            String input = username + ":" + password;
            String output = Base64.encodeBytes(input.getBytes());
            hconn.setRequestProperty("Authorization", "Basic " + output);
            hconn.connect();
            if (istream != null) {
                BufferedOutputStream ostream = new BufferedOutputStream(hconn.getOutputStream(), 1024);
                byte buffer[] = new byte[1024];
                while (true) {
                    int n = istream.read(buffer);
                    if (n < 0) {
                        break;
                    }
                    ostream.write(buffer, 0, n);
                }
                ostream.flush();
                ostream.close();
                istream.close();
            }
            reader = new InputStreamReader(hconn.getInputStream());
            StringBuffer buff = new StringBuffer();
            String error = null;
            boolean first = true;
            while (true) {
                int ch = reader.read();
                if (ch < 0) {
                    break;
                } else if ((ch == '\r') || (ch == '\n')) {
                    String line = buff.toString();
                    buff.setLength(0);
                    logger.fine("Return msg = " + line);
                    if (first) {
                        if (!line.startsWith("OK -")) {
                            error = line;
                        }
                        first = false;
                    }
                } else {
                    buff.append((char) ch);
                }
            }
            if (buff.length() > 0) {
                logger.fine(buff.toString());
            }
            if (error != null) {
                throw new TaskExecutionException(error);
            }
        } catch (Throwable t) {
            throw new TaskExecutionException(t);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
            if (istream != null) {
                try {
                    istream.close();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }
}
