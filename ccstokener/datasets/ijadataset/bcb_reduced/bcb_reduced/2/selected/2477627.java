package wtanaka.praya.zhongwen;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import wtanaka.debug.Debug;
import wtanaka.praya.Protocol;

/**
 * This is the thread that polls the webpage looking for messages.
 *
 * <p>
 * Return to <A href="http://sourceforge.net/projects/praya/">
 * <IMG src="http://sourceforge.net/sflogo.php?group_id=2302&type=1"
 *   alt="Sourceforge" width="88" height="31" border="0"></A>
 * or the <a href="http://praya.sourceforge.net/">Praya Homepage</a>
 *
 * @author $Author: wtanaka $
 * @version $Name:  $ $Date: 2001/10/22 06:27:02 $
 **/
public class ZhongWenWatcher extends Thread {

    public static final int SLEEP_TIME = 8000;

    private volatile boolean m_shouldBeRunning = true;

    private Protocol m_source;

    private static final Object DEFAULT_MEMENTO = "";

    /**
    * @bug This is subject to an attack by a malicious chat
    * participant.  It would be better to store the entire last view
    * taken, as to more intelligently figure out where to pick up
    * where we left off.
    **/
    private String m_mostRecentKnownLine = (String) DEFAULT_MEMENTO;

    public ZhongWenWatcher(Protocol source, Object memento) {
        m_source = source;
        if (memento instanceof String) m_mostRecentKnownLine = (String) memento;
    }

    public ZhongWenWatcher(Protocol source) {
        this(source, DEFAULT_MEMENTO);
    }

    private static String getURL() {
        return "http://zhongwen.com/cgi-bin/chatfresh.cgi?ran=" + System.currentTimeMillis() / 1000;
    }

    public void abort() {
        m_shouldBeRunning = false;
        interrupt();
    }

    public Object getMemento() {
        return m_mostRecentKnownLine;
    }

    private static String stripTags(String input) {
        while (input.indexOf("<") >= 0) {
            int beginStrip = input.indexOf("<");
            int endStrip = input.indexOf(">", beginStrip);
            if (endStrip < 0) {
                input = input.substring(0, beginStrip);
            } else {
                input = input.substring(0, beginStrip) + input.substring(endStrip + 1);
            }
        }
        return input;
    }

    public void run() {
        Thread.currentThread().setName("zhongwen.com watcher");
        String url = getURL();
        try {
            while (m_shouldBeRunning) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream(), "ISO8859_1"));
                    String line;
                    Vector chatLines = new Vector();
                    boolean startGrabbing = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.indexOf("</style>") >= 0) {
                            startGrabbing = true;
                        } else if (startGrabbing) {
                            if (line.equals(m_mostRecentKnownLine)) {
                                break;
                            }
                            chatLines.addElement(line);
                        }
                    }
                    reader.close();
                    for (int i = chatLines.size() - 1; i >= 0; --i) {
                        String chatLine = (String) chatLines.elementAt(i);
                        m_mostRecentKnownLine = chatLine;
                        if (chatLine.indexOf(":") >= 0) {
                            String from = chatLine.substring(0, chatLine.indexOf(":"));
                            String message = stripTags(chatLine.substring(chatLine.indexOf(":")));
                            m_source.pushMessage(new ZhongWenMessage(m_source, from, message));
                        } else {
                            m_source.pushMessage(new ZhongWenMessage(m_source, null, stripTags(chatLine)));
                        }
                    }
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedIOException e) {
                } catch (InterruptedException e) {
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            m_source.disconnect();
            throw e;
        } catch (Error e) {
            m_source.disconnect();
            throw e;
        }
    }
}
