package com.aelitis.azureus.plugins.extseed.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.*;
import org.gudy.azureus2.core3.security.SEPasswordListener;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import com.aelitis.azureus.core.util.Java15Utils;
import com.aelitis.azureus.plugins.extseed.ExternalSeedException;

public class ExternalSeedHTTPDownloaderLinear implements ExternalSeedHTTPDownloader {

    private URL original_url;

    private String user_agent;

    private int last_response;

    private int last_response_retry_after_secs;

    private Downloader downloader;

    public ExternalSeedHTTPDownloaderLinear(URL _url, String _user_agent) {
        original_url = _url;
        user_agent = _user_agent;
    }

    public void downloadRange(long offset, int length, ExternalSeedHTTPDownloaderListener listener, boolean con_fail_is_perm_fail) throws ExternalSeedException {
        Request request;
        synchronized (this) {
            if (downloader == null) {
                downloader = new Downloader(listener, con_fail_is_perm_fail);
            }
            request = downloader.addRequest(offset, length, listener);
        }
        while (true) {
            if (request.waitFor(1000)) {
                return;
            }
            if (listener.isCancelled()) {
                throw (new ExternalSeedException("request cancelled"));
            }
        }
    }

    public void deactivate() {
        Downloader to_destroy = null;
        synchronized (this) {
            if (downloader != null) {
                to_destroy = downloader;
                downloader = null;
            }
        }
        if (to_destroy != null) {
            to_destroy.destroy(new ExternalSeedException("deactivated"));
        }
    }

    protected void destoyed(Downloader dead) {
        synchronized (this) {
            if (downloader == dead) {
                downloader = null;
            }
        }
    }

    public void download(int length, ExternalSeedHTTPDownloaderListener listener, boolean con_fail_is_perm_fail) throws ExternalSeedException {
        throw (new ExternalSeedException("not supported"));
    }

    public void downloadSocket(int length, ExternalSeedHTTPDownloaderListener listener, boolean con_fail_is_perm_fail) throws ExternalSeedException {
        throw (new ExternalSeedException("not supported"));
    }

    public int getLastResponse() {
        return (last_response);
    }

    public int getLast503RetrySecs() {
        return (last_response_retry_after_secs);
    }

    protected class Downloader implements SEPasswordListener {

        private ExternalSeedHTTPDownloaderListener listener;

        private boolean con_fail_is_perm_fail;

        private volatile boolean destroyed;

        private List<Request> requests = new ArrayList<Request>();

        private RandomAccessFile raf = null;

        private File scratch_file = null;

        protected Downloader(ExternalSeedHTTPDownloaderListener _listener, boolean _con_fail_is_perm_fail) {
            listener = _listener;
            con_fail_is_perm_fail = _con_fail_is_perm_fail;
            new AEThread2("ES:downloader", true) {

                public void run() {
                    download();
                }
            }.start();
        }

        protected void download() {
            boolean connected = false;
            String outcome = "";
            try {
                InputStream is = null;
                try {
                    SESecurityManager.setThreadPasswordHandler(this);
                    synchronized (this) {
                        if (destroyed) {
                            return;
                        }
                        scratch_file = AETemporaryFileHandler.createTempFile();
                        raf = new RandomAccessFile(scratch_file, "rw");
                    }
                    HttpURLConnection connection;
                    int response;
                    connection = (HttpURLConnection) original_url.openConnection();
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("User-Agent", user_agent);
                    int time_remaining = listener.getPermittedTime();
                    if (time_remaining > 0) {
                        Java15Utils.setConnectTimeout(connection, time_remaining);
                    }
                    connection.connect();
                    time_remaining = listener.getPermittedTime();
                    if (time_remaining < 0) {
                        throw (new IOException("Timeout during connect"));
                    }
                    Java15Utils.setReadTimeout(connection, time_remaining);
                    connected = true;
                    response = connection.getResponseCode();
                    last_response = response;
                    last_response_retry_after_secs = -1;
                    if (response == 503) {
                        long retry_after_date = new Long(connection.getHeaderFieldDate("Retry-After", -1L)).longValue();
                        if (retry_after_date <= -1) {
                            last_response_retry_after_secs = connection.getHeaderFieldInt("Retry-After", -1);
                        } else {
                            last_response_retry_after_secs = (int) ((retry_after_date - System.currentTimeMillis()) / 1000);
                            if (last_response_retry_after_secs < 0) {
                                last_response_retry_after_secs = -1;
                            }
                        }
                    }
                    is = connection.getInputStream();
                    if (response == HttpURLConnection.HTTP_ACCEPTED || response == HttpURLConnection.HTTP_OK || response == HttpURLConnection.HTTP_PARTIAL) {
                        byte[] buffer = new byte[64 * 1024];
                        int requests_outstanding = 1;
                        while (!destroyed) {
                            int permitted = listener.getPermittedBytes();
                            if (requests_outstanding == 0 || permitted < 1) {
                                permitted = 1;
                                Thread.sleep(100);
                            }
                            int len = is.read(buffer, 0, Math.min(permitted, buffer.length));
                            if (len <= 0) {
                                break;
                            }
                            synchronized (this) {
                                try {
                                    raf.write(buffer, 0, len);
                                } catch (Throwable e) {
                                    outcome = "Write failed: " + e.getMessage();
                                    ExternalSeedException error = new ExternalSeedException(outcome, e);
                                    error.setPermanentFailure(true);
                                    throw (error);
                                }
                            }
                            listener.reportBytesRead(len);
                            requests_outstanding = checkRequests();
                        }
                        checkRequests();
                    } else {
                        outcome = "Connection failed: " + connection.getResponseMessage();
                        ExternalSeedException error = new ExternalSeedException(outcome);
                        error.setPermanentFailure(true);
                        throw (error);
                    }
                } catch (IOException e) {
                    if (con_fail_is_perm_fail && !connected) {
                        outcome = "Connection failed: " + e.getMessage();
                        ExternalSeedException error = new ExternalSeedException(outcome);
                        error.setPermanentFailure(true);
                        throw (error);
                    } else {
                        outcome = "Connection failed: " + Debug.getNestedExceptionMessage(e);
                        if (last_response_retry_after_secs >= 0) {
                            outcome += ", Retry-After: " + last_response_retry_after_secs + " seconds";
                        }
                        ExternalSeedException excep = new ExternalSeedException(outcome, e);
                        if (e instanceof FileNotFoundException) {
                            excep.setPermanentFailure(true);
                        }
                        throw (excep);
                    }
                } catch (ExternalSeedException e) {
                    throw (e);
                } catch (Throwable e) {
                    if (e instanceof ExternalSeedException) {
                        throw ((ExternalSeedException) e);
                    }
                    outcome = "Connection failed: " + Debug.getNestedExceptionMessage(e);
                    throw (new ExternalSeedException("Connection failed", e));
                } finally {
                    SESecurityManager.unsetThreadPasswordHandler();
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Throwable e) {
                        }
                    }
                }
            } catch (ExternalSeedException e) {
                if (!connected && con_fail_is_perm_fail) {
                    e.setPermanentFailure(true);
                }
                destroy(e);
            }
        }

        protected Request addRequest(long offset, int length, ExternalSeedHTTPDownloaderListener listener) throws ExternalSeedException {
            Request request;
            synchronized (this) {
                if (destroyed) {
                    throw (new ExternalSeedException("downloader destroyed"));
                }
                request = new Request(offset, length, listener);
                requests.add(request);
            }
            checkRequests();
            return (request);
        }

        protected int checkRequests() {
            try {
                synchronized (this) {
                    if (raf == null) {
                        return (requests.size());
                    }
                    long pos = raf.getFilePointer();
                    Iterator<Request> it = requests.iterator();
                    while (it.hasNext()) {
                        Request request = it.next();
                        long end = request.getOffset() + request.getLength();
                        if (pos >= end) {
                            ExternalSeedHTTPDownloaderListener listener = request.getListener();
                            try {
                                raf.seek(request.getOffset());
                                int total = 0;
                                while (total < request.getLength()) {
                                    byte[] buffer = listener.getBuffer();
                                    int buffer_len = listener.getBufferLength();
                                    raf.read(buffer, 0, buffer_len);
                                    total += buffer_len;
                                    listener.done();
                                }
                            } finally {
                                raf.seek(pos);
                            }
                            request.complete();
                            it.remove();
                        }
                    }
                    return (requests.size());
                }
            } catch (Throwable e) {
                Debug.out(e);
                destroy(new ExternalSeedException("read failed", e));
                return (0);
            }
        }

        protected void destroy(ExternalSeedException error) {
            synchronized (this) {
                if (destroyed) {
                    return;
                }
                destroyed = true;
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (Throwable e) {
                    }
                }
                if (scratch_file != null) {
                    scratch_file.delete();
                }
                for (Request r : requests) {
                    r.destroy(error);
                }
                requests.clear();
            }
            ExternalSeedHTTPDownloaderLinear.this.destoyed(this);
        }

        public PasswordAuthentication getAuthentication(String realm, URL tracker) {
            return (null);
        }

        public void setAuthenticationOutcome(String realm, URL tracker, boolean success) {
        }

        public void clearPasswords() {
        }
    }

    protected class Request {

        private long offset;

        private int length;

        private ExternalSeedHTTPDownloaderListener listener;

        private AESemaphore sem = new AESemaphore("ES:wait");

        private volatile ExternalSeedException exception;

        protected Request(long _offset, int _length, ExternalSeedHTTPDownloaderListener _listener) {
            offset = _offset;
            length = _length;
            listener = _listener;
        }

        protected long getOffset() {
            return (offset);
        }

        protected int getLength() {
            return (length);
        }

        protected ExternalSeedHTTPDownloaderListener getListener() {
            return (listener);
        }

        protected void complete() {
            sem.release();
        }

        protected void destroy(ExternalSeedException e) {
            exception = e;
            sem.release();
        }

        public boolean waitFor(int timeout) throws ExternalSeedException {
            if (!sem.reserve(timeout)) {
                return (false);
            }
            if (exception != null) {
                throw (exception);
            }
            return (true);
        }
    }
}
