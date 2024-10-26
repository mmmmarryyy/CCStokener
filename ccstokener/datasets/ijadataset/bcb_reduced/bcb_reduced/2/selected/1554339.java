package speedreader;

import java.io.*;
import java.net.*;
import javax.swing.text.*;

/**
 * This class takes a String being the url of
 * some document and loads the document in to a PlainDocument object.
 * <p>Use the getDocument object to retreive the loaded information
 * <p>Observers can register for this object to receive notifications of
 * changes in state. Use the gettors to retreive state information and basic
 * information about the document being loaded.
 *
 * @author  Stephen Roach
 */
public class URLOpener extends Opener {

    /** Creates a new instance of DocumentLoader */
    public URLOpener(String url) {
        super(url);
    }

    /**
     *  Process the URL in a Thread. 
     *  <p>Fetch it and iterate over
     *  an input stream until it's all over. 
     *  <p>We can be interrupted
     *  by the user and we issue regular notification to observers
     *  so the display can be updated and waiting processes know what's
     *  happening.
     */
    public void run() {
        BufferedInputStream bis = null;
        URLConnection url = null;
        String textType = null;
        StringBuffer sb = new StringBuffer();
        try {
            if (!location.startsWith("http://")) {
                location = "http://" + location;
            }
            url = (new URL(location)).openConnection();
            size = url.getContentLength();
            textType = url.getContentType();
            lastModified = url.getIfModifiedSince();
            InputStream is = url.getInputStream();
            bis = new BufferedInputStream(is);
            if (textType.startsWith("text/plain")) {
                int i;
                i = bis.read();
                ++position;
                status = "    Reading From URL...";
                this.setChanged();
                this.notifyObservers();
                while (i != END_OF_STREAM) {
                    sb.append((char) i);
                    i = bis.read();
                    ++position;
                    if (position % (size / 25) == 0) {
                        this.setChanged();
                        this.notifyObservers();
                    }
                    if (abortLoading) {
                        break;
                    }
                }
                status = "    Finished reading URL...";
            } else if (textType.startsWith("text/html")) {
                int i;
                i = bis.read();
                char c = (char) i;
                ++position;
                status = "    Reading From URL...";
                this.setChanged();
                this.notifyObservers();
                boolean enclosed = false;
                if (c == '<') {
                    enclosed = true;
                }
                while (i != END_OF_STREAM) {
                    if (enclosed) {
                        if (c == '>') {
                            enclosed = false;
                        }
                    } else {
                        if (c == '<') {
                            enclosed = true;
                        } else {
                            sb.append((char) i);
                        }
                    }
                    i = bis.read();
                    c = (char) i;
                    ++position;
                    if (size == 0) {
                        if (position % (size / 25) == 0) {
                            this.setChanged();
                            this.notifyObservers();
                        }
                    }
                    if (abortLoading) {
                        break;
                    }
                }
                status = "    Finished reading URL...";
            } else {
                status = "    Unable to read document type: " + textType + "...";
            }
            bis.close();
            try {
                document.insertString(0, sb.toString(), SimpleAttributeSet.EMPTY);
            } catch (BadLocationException ble) {
                ble.printStackTrace();
            }
            finished = true;
            this.setChanged();
            this.notifyObservers();
        } catch (IOException ioe) {
            try {
                document.insertString(0, sb.toString(), SimpleAttributeSet.EMPTY);
            } catch (BadLocationException ble) {
                ble.printStackTrace();
            }
            status = "    IO Error Reading From URL...";
            finished = true;
            this.setChanged();
            this.notifyObservers();
        }
    }
}
