package com.busfm.download;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import org.apache.http.conn.ConnectTimeoutException;
import com.busfm.model.DownloadEntity;
import com.busfm.model.SongEntity;
import com.busfm.util.Constants;
import com.busfm.util.LogUtil;
import com.busfm.util.Utilities;
import android.os.Process;
import android.util.Log;

/**
 * <p>
 * Title:DownloadThread
 * </p>
 * <p>
 * Description:DownloadThread
 * </p>
 * <p>
 * Copyright (c) 2011 www.bus.fm Inc. All rights reserved.
 * </p>
 * <p>
 * Company: bus.fm
 * </p>
 * 
 * 
 * @author jingguo0@gmail.com
 * 
 */
public class DownloadThread extends Thread {

    private String DOUT = ".";

    private DownloadJob mDownloadJob;

    int RETRY_COUNT = 3;

    boolean mFirst = true;

    boolean mStop = false;

    DataOutputStream output;

    private long flagPos;

    public DownloadThread(DownloadJob downloadJob) {
        mDownloadJob = downloadJob;
    }

    @Override
    public void run() {
        int retryCount = 0;
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (!mStop) {
            try {
                downloadFile(mDownloadJob);
            } catch (SocketTimeoutException ste) {
                LogUtil.e(Constants.TAG, ste.getMessage());
                if (++retryCount > RETRY_COUNT) {
                    mDownloadJob.notifyDownloadFatal();
                    break;
                }
            } catch (ConnectTimeoutException cte) {
                LogUtil.e(Constants.TAG, cte.getMessage());
                if (++retryCount > RETRY_COUNT) {
                    mDownloadJob.notifyDownloadFatal();
                    break;
                }
            } catch (Exception e) {
                LogUtil.e(Constants.TAG, e.getMessage());
                mDownloadJob.notifyDownloadFatal();
                break;
            }
        }
        if (0 != retryCount) {
            LogUtil.e(Constants.TAG, "retry " + retryCount + " times!");
        }
    }

    private boolean downloadFile(DownloadJob job) throws IOException {
        DownloadEntity downloadEntity = job.getDownloadEntity();
        job.notifyDownloadStarted();
        URL url = new URL(downloadEntity.path);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setConnectTimeout(30000);
        httpURLConnection.setReadTimeout(30000);
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setDoOutput(true);
        httpURLConnection.connect();
        job.setTotalSize(httpURLConnection.getContentLength());
        String path = job.getDestination() + File.separator + downloadEntity.title + DOUT + Utilities.getDownloadFileFormat(downloadEntity.title);
        try {
            boolean success = (new File(path)).mkdirs();
            if (success) {
                Log.i(Constants.TAG, "Directory: " + path + " created");
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Error creating folder", e);
            return false;
        }
        FileOutputStream fileOutputStream = new FileOutputStream(new File(path, downloadEntity.title));
        InputStream in = httpURLConnection.getInputStream();
        byte[] buffer = new byte[1024];
        int lenght = 0;
        while ((lenght = in.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, lenght);
            job.setDownloadedSize(job.getDownloadedSize() + lenght);
        }
        if (job.getDownloadedSize() == job.getTotalSize()) {
            job.notifyDownloadCompleted();
        } else {
            job.notifyDownloadFatal();
        }
        fileOutputStream.close();
        return false;
    }

    public void stopDownloadThread() {
        mStop = true;
    }
}
