package org.jcompany.apache.tomcat;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.apache.catalina.util.Base64;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

public class PlcAbstractTomcatManager {

    private IOutputListener listener;

    /**
	 * Resultado do comando.
	 */
    StringBuffer buff = new StringBuffer();

    /**
	 * manager webapp's encoding.
	 */
    private static String CHARSET = "utf-8";

    /**
	 * The charset used during URL encoding.
	 */
    protected String charset = "ISO-8859-1";

    public String getCharset() {
        return (this.charset);
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
	 * The login password for the <code>Manager</code> application.
	 */
    protected String password = null;

    public String getPassword() {
        return (this.password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
	 * The URL of the <code>Manager</code> application to be used.
	 */
    protected String url = "http://localhost:8080/manager";

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
	 * Execute the specified command, based on the configured properties. The input stream will be closed upon
	 * completion of this task, whether it was executed successfully or not.
	 * 
	 * @param command
	 *            Command to be executed
	 * @param istream
	 *            InputStream to include in an HTTP PUT, if any
	 * @param contentType
	 *            Content type to specify for the input, if any
	 * @param contentLength
	 *            Content length to specify for the input, if any
	 * 
	 * @exception BuildException
	 *                if an error occurs
	 * @throws PlcTomcatException
	 */
    public void execute(String command, InputStream istream, String contentType, int contentLength) throws PlcTomcatException {
        URLConnection conn = null;
        InputStreamReader reader = null;
        try {
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
            hconn.setRequestProperty("User-Agent", "Catalina-Ant-Task/1.0");
            String input = username + ":" + password;
            String output = new String(Base64.encode(input.getBytes()));
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
            reader = new InputStreamReader(hconn.getInputStream(), CHARSET);
            buff = new StringBuffer();
            String error = null;
            int msgPriority = Project.MSG_INFO;
            boolean first = true;
            while (true) {
                int ch = reader.read();
                if (ch < 0) {
                    break;
                } else if ((ch == '\r') || (ch == '\n')) {
                    if (buff.length() > 0) {
                        String line = buff.toString();
                        buff.setLength(0);
                        if (first) {
                            if (!line.startsWith("OK -")) {
                                error = line;
                                msgPriority = Project.MSG_ERR;
                            }
                            first = false;
                        }
                        if (listener != null) listener.out(line);
                    }
                } else {
                    buff.append((char) ch);
                }
            }
            if (buff.length() > 0) {
            }
            if (error != null) {
                throw new PlcTomcatException(error);
            }
        } catch (Throwable t) {
            throw new PlcTomcatException(t);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable u) {
                    ;
                }
                reader = null;
            }
            if (istream != null) {
                try {
                    istream.close();
                } catch (Throwable u) {
                    ;
                }
                istream = null;
            }
        }
    }

    /**
	 * Execute the specified command, based on the configured properties.
	 * 
	 * @param command
	 *            Command to be executed
	 * @throws PlcTomcatException
	 * 
	 * @exception BuildException
	 *                if an error occurs
	 */
    public void execute(String command) throws PlcTomcatException {
        execute(command, null, null, -1);
    }

    public IOutputListener getListener() {
        return listener;
    }

    public void setListener(IOutputListener listener) {
        this.listener = listener;
    }
}
