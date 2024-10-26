package com.outertrack.jspeedstreamer;

import com.outertrack.jspeedstreamer.http.*;
import com.outertrack.jspeedstreamer.utils.CircularDownloadBuffer;
import com.outertrack.jspeedstreamer.utils.MultiLogger;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;

/**
 * This is the 'manager' thread that manages multiple download threads downloading multiple chunks
 * of data at different positions from an HTTP stream and then writing that data to the proxy client
 * 
 * @author conorhunt
 * 
 */
public class ProxyThread extends Thread {

    private static MultiLogger log = MultiLogger.getLogger(ProxyThread.class);

    private Socket socket = null;

    private boolean downloadFinished = false;

    private long contentLength = -1;

    private long bytesSent = 0;

    private int maxBlockSize = 1000000;

    private int blockSize = 200000;

    private long serverReadPosition = 0;

    private int downloadThreadCount = 4;

    DownloadThread downloadThreads[] = null;

    CircularDownloadBuffer buffer = null;

    private int bufferSize = 6000000;

    public ProxyThread(Socket s, HashMap options) {
        this.socket = s;
        Integer value = (Integer) options.get("MAXSEG");
        if (value != null) maxBlockSize = value.intValue();
        value = (Integer) options.get("MINSEG");
        if (value != null) blockSize = value.intValue();
        value = (Integer) options.get("THREADS");
        if (value != null) downloadThreadCount = value.intValue();
        value = (Integer) options.get("BUFFER");
        if (value != null) bufferSize = value.intValue();
    }

    public void run() {
        log.debug("Starting request");
        try {
            socket.setSoTimeout(5000);
            BufferedInputStream clientIn = new BufferedInputStream(socket.getInputStream());
            OutputStream clientOut = socket.getOutputStream();
            HttpRequest request = new HttpRequest(clientIn);
            HttpResponse response = request.execute();
            clientOut.write(response.getResponseBytes());
            clientOut.flush();
            int responseCode = response.getResponseCode();
            contentLength = response.getContentLength();
            log.debug("response code = " + responseCode + " contentLength = " + contentLength);
            if (request.getRequestType().equalsIgnoreCase("GET") && (responseCode == 200 || responseCode == 206) && contentLength > 5000000) {
                response.close();
                doDownload(request, contentLength, clientOut);
            } else if (!(responseCode >= 300 && responseCode < 400) && !request.getRequestType().equalsIgnoreCase("HEAD")) {
                InputStream stream = response.getInputStream();
                BufferedOutputStream bufOut = new BufferedOutputStream(clientOut);
                for (int counter = 0, b = -1; (counter < contentLength || contentLength < 0) && (b = stream.read()) >= 0; counter++) {
                    bufOut.write(b);
                }
                bufOut.flush();
            }
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                downloadFinished = true;
                socket.close();
            } catch (Exception e) {
            }
        }
        log.debug("Finished with request");
    }

    /**
     * This method does the multi threaded downloading
     * 
     * @param request
     * @param contentLength
     * @param clientOut
     */
    public void doDownload(HttpRequest request, long contentLength, OutputStream clientOut) {
        SpeedThread speedThread = new SpeedThread(this);
        buffer = new CircularDownloadBuffer(bufferSize);
        try {
            downloadThreads = new DownloadThread[downloadThreadCount];
            for (int i = 0; i < downloadThreadCount; i++) {
                downloadThreads[i] = new DownloadThread(request, buffer, this);
                downloadThreads[i].start();
            }
            speedThread.start();
            Thread.sleep(2000);
            byte readBuffer[] = new byte[4096];
            int readLength = 4096;
            while (bytesSent < contentLength - 1 && !downloadFinished) {
                if (readLength + bytesSent >= contentLength) readLength = (int) (contentLength - bytesSent - 1);
                int newBytes = buffer.read(readBuffer, readLength);
                clientOut.write(readBuffer, 0, newBytes);
                bytesSent += newBytes;
            }
            clientOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            downloadFinished = true;
            buffer.quit();
        }
        log.debug("Manager - finished sent: " + bytesSent);
    }

    private long lastTime = System.currentTimeMillis();

    private long lastBytesSent = 0;

    /**
     * This method outputs the current speed of the downloading client
     * and the speeds of each of the downloading threads.
     */
    public void outputCurrentSpeeds() {
        long currentTime = System.currentTimeMillis();
        int clientReadSpeed = (int) (((bytesSent - lastBytesSent) / 1000) / ((currentTime - lastTime) / 1000));
        System.out.println("Client read speed - " + clientReadSpeed + "k/s @ " + bytesSent);
        lastTime = currentTime;
        lastBytesSent = bytesSent;
        for (int i = 0; i < downloadThreads.length; i++) {
            int speed = downloadThreads[i].getSpeed();
            long start = downloadThreads[i].getStartPosition();
            long end = downloadThreads[i].getEndPosition();
            long current = downloadThreads[i].getCurrentPosition();
            System.out.println("Dl thread - " + i + " " + (speed / 1000) + "k/s " + start + " -> " + end + " @ " + current);
        }
    }

    public synchronized boolean notifyThreadReady(DownloadThread thread, int bytesDownloaded) {
        if (downloadFinished) return false;
        int threadBlockSize = blockSize;
        if (blockSize * 2 <= maxBlockSize) blockSize *= 2; else blockSize = maxBlockSize;
        if (serverReadPosition + threadBlockSize > contentLength) {
            threadBlockSize = (int) (contentLength - serverReadPosition);
        }
        thread.setBlock(serverReadPosition, serverReadPosition + threadBlockSize);
        serverReadPosition += threadBlockSize;
        return true;
    }

    public boolean isFinished() {
        return downloadFinished;
    }

    /**
     * Download threads call this method to notify the manager that something went wrong
     * and that they have to quit.
     *
     */
    public void notifyThreadQuit() {
        downloadFinished = true;
    }

    /**
     * Simple thread that sleeps 1 second and then outputs the current speeds of the download
     * 
     * @author conorhunt
     *
     */
    private class SpeedThread extends Thread {

        ProxyThread parent = null;

        public SpeedThread(ProxyThread parent) {
            this.parent = parent;
        }

        public void run() {
            while (!parent.isFinished()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                parent.outputCurrentSpeeds();
            }
        }
    }
}
