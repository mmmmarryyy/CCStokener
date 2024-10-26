package com.jspx.network.download.impl;

import com.jspx.core.sign.DownStateType;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.net.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileSplitterFetch extends Thread {

    private static final Log log = LogFactory.getLog(FileSplitterFetch.class);

    private URL url;

    public long getCompleted() {
        return completed;
    }

    public void setCompleted(long completed) {
        this.completed = completed;
    }

    private long completed = 0;

    public int getStateType() {
        return stateType;
    }

    public void setStateType(int stateType) {
        this.stateType = stateType;
    }

    private int stateType = DownStateType.WAITING;

    public long getStartPos() {
        return startPos;
    }

    private long startPos;

    public long getEndPos() {
        return endPos;
    }

    private long endPos;

    private int nThreadID;

    public boolean isbDownOver() {
        return bDownOver;
    }

    private boolean bDownOver = false;

    private boolean bStop = false;

    private FileAccess fileAccess = null;

    public FileSplitterFetch(URL url, String sName, long nStart, long nEnd, int id) throws IOException {
        this.url = url;
        this.startPos = nStart;
        this.endPos = nEnd;
        nThreadID = id;
        fileAccess = new FileAccess(sName, startPos);
    }

    public void run() {
        if (url == null) {
            stateType = DownStateType.ERROR;
            return;
        }
        while (startPos < endPos && !bStop) {
            if (stateType == DownStateType.STATE_STOPPED) {
                splitterStop();
                return;
            }
            while (stateType == DownStateType.PAUSE) {
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stateType = DownStateType.DOWNLOADING;
            try {
                HttpURLConnection uc = (HttpURLConnection) url.openConnection();
                uc.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.00; Windows 98)");
                uc.setRequestProperty("JCache-Controlt", "no-cache");
                String sProperty = "bytes=" + startPos + "-";
                uc.setRequestProperty("RANGE", sProperty);
                if (uc instanceof HttpsURLConnection) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) uc;
                    httpsConnection.setHostnameVerifier(new HostnameVerifier() {

                        public boolean verify(String host, SSLSession session) {
                            return true;
                        }
                    });
                }
                InputStream input = uc.getInputStream();
                byte[] b = new byte[1024];
                int nRead;
                while ((nRead = input.read(b, 0, 1024)) > 0 && startPos < endPos && !bStop) {
                    startPos += fileAccess.write(b, 0, nRead);
                    completed = completed + nRead;
                    stateType = DownStateType.DOWNLOADING;
                }
                log.info("Thread " + nThreadID + " is over!");
                bDownOver = true;
                stateType = DownStateType.FINISH;
                uc.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                log.info(e);
                stateType = DownStateType.ERROR;
            }
        }
        splitterStop();
    }

    public void splitterStop() {
        bStop = true;
        if (!isInterrupted()) {
            interrupt();
        }
    }
}
