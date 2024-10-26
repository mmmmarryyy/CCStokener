package pogvue.io;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public class FileParse {

    private InputStream inStream;

    private BufferedReader bufReader;

    private URLConnection urlconn;

    private ActionListener l = null;

    private long size = -1;

    private long estimatedSize = -1;

    private long updateSize = 1000;

    private int curlen = 0;

    private long prevchunk = -1;

    private int i = 0;

    String inFile;

    String type;

    public FileParse() {
    }

    public FileParse(String fileStr, String type) throws MalformedURLException, IOException {
        this.inFile = fileStr;
        this.type = type;
        System.out.println("File str " + fileStr);
        if (fileStr.indexOf("http://") == 0) {
            URL url = new URL(fileStr);
            urlconn = url.openConnection();
            inStream = urlconn.getInputStream();
            bufReader = new BufferedReader(new InputStreamReader(inStream));
        } else if (type.equals("File")) {
            File inFile = new File(fileStr);
            size = inFile.length();
            inStream = new FileInputStream(inFile);
            bufReader = new BufferedReader(new InputStreamReader(inStream));
        } else if (type.equals("URL")) {
            URL url = new URL(fileStr);
            urlconn = url.openConnection();
            inStream = urlconn.getInputStream();
            bufReader = new BufferedReader(new InputStreamReader(inStream));
        } else if (type.equals("URLZip")) {
            URL url = new URL(fileStr);
            inStream = new GZIPInputStream(url.openStream(), 16384);
            InputStreamReader zis = new InputStreamReader(inStream);
            bufReader = new BufferedReader(zis, 16384);
        } else {
            System.out.println("Unknown FileParse inType " + type);
        }
    }

    public int getSize() {
        return (int) size;
    }

    public void setEstimatedSize(long size) {
        this.estimatedSize = size;
    }

    public long getEstimatedSize() {
        return estimatedSize;
    }

    public URLConnection getURLConnection() {
        return urlconn;
    }

    public BufferedReader getBufferedReader() {
        return bufReader;
    }

    public InputStream getInputStream() {
        return inStream;
    }

    public String nextLine() throws IOException {
        String next = bufReader.readLine();
        i++;
        if (i == 1 && l != null && size > 0) {
            ActionEvent e = new ActionEvent(this, 0, "Size=" + size);
            l.actionPerformed(e);
        }
        if (i == 1) {
            if (size > 0) {
                updateSize = (long) (size / (100));
            } else if (estimatedSize > 0) {
                updateSize = (long) (estimatedSize / 100);
            }
        }
        if (next != null) {
            curlen += next.length();
            if (l != null) {
                ActionEvent e = null;
                int chunk = (int) (curlen / updateSize);
                if (next != null && chunk != prevchunk) {
                    e = new ActionEvent(this, 0, "Len=" + curlen);
                    l.actionPerformed((ActionEvent) e);
                }
                prevchunk = chunk;
            }
        }
        return next;
    }

    public void setActionListener(ActionListener l) {
        this.l = l;
    }
}
