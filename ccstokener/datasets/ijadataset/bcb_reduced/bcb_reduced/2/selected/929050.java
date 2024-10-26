package vbullmin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.eclipse.swt.SWT;
import vbullmin.gui.GUI;

/**
 * Retrieve an URL
 * @author Onur Aslan
 */
public class Get {

    /**
   * cookies
   */
    public static String cookies = null;

    /**
   * Charset of the content
   * <p>Default charset is UTF8.<br />
   * Charset getting and setting for this class with control () function.</p>
   */
    public static String charset = "UTF8";

    /**
   * URL
   */
    private String url;

    /**
   * The content of the url
   */
    private String content = "";

    /**
   * Connection
   */
    private URLConnection conn;

    public Get(String url) {
        this.url = url;
        initialize();
    }

    /**
   * Controlling vBulletin and getting charset
   * <p>Parser is parsing content</p>
   */
    public void control() {
        GUI.progress.setMessage(new Parser(content).vbulletin());
        String charset = new Parser(content).charset();
        GUI.progress.setMessage("charset: " + charset);
        Get.charset = charset;
    }

    /**
   * Get url with java.net.URLConnection and cookies
   * @return Status of the URL getting
   */
    public int getUrl() {
        try {
            final URL url = new URL(this.url);
            conn = url.openConnection();
            if (cookies != null) {
                conn.setRequestProperty("Cookie", cookies);
            }
            InputStreamReader inputstream = new InputStreamReader(conn.getInputStream(), charset);
            charset = inputstream.getEncoding();
            BufferedReader input = new BufferedReader(inputstream);
            String line;
            while ((line = input.readLine()) != null) {
                content += line + "\n";
            }
            return 0;
        } catch (MalformedURLException e) {
            return 1;
        } catch (IOException e2) {
            return 2;
        }
    }

    /**
   * Returning content via string
   * @return Content of the url
   */
    public String toString() {
        return content;
    }

    /**
   * Initialization Get
   */
    private void initialize() {
        GUI.display.syncExec(new Runnable() {

            public void run() {
                final int RETRY = 5;
                for (int i = 0; ; i++) {
                    if (i < RETRY) {
                        if (getUrl() == 0) return;
                    } else {
                        int answer = GUI.dialog(GUI.progress.shell, url + " not retrieving! Maybe your URL not a " + "vBulletin or there is a network connection error. " + "Do you want to retry?", SWT.ICON_INFORMATION | SWT.YES | SWT.NO);
                        if (answer == SWT.NO) {
                            GUI.dialog(GUI.progress.shell, "Program aborted by user", SWT.ICON_INFORMATION | SWT.OK);
                            System.exit(0);
                        }
                    }
                }
            }
        });
    }

    /**
   * Finalize deleting content
   */
    protected void finalize() {
        content = null;
        url = null;
    }
}
