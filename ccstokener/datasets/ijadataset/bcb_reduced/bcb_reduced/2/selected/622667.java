package com.ad_oss.merkat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import android.os.AsyncTask;
import android.util.Log;

public class APKDownloader extends AsyncTask<MarketData, Integer, String> {

    private final int ProgressRange;

    private final APKMonitor mMonitor;

    interface APKMonitor {

        int getProgressRange();

        void preDownloadAPK(APKDownloader me);

        void postDownloadAPK(APKDownloader me);

        void progressAPK(APKDownloader me, Integer integer);
    }

    public APKDownloader(APKMonitor monitor) {
        mMonitor = monitor;
        ProgressRange = mMonitor.getProgressRange();
    }

    @Override
    protected String doInBackground(MarketData... market) {
        publishProgress(-1);
        InputStream input = null;
        OutputStream output = null;
        long lenghtOfFile = 0;
        int lengthRead = 0;
        try {
            HttpGet newReq = new HttpGet(market[0].apkURL);
            HttpResponse response = HttpManager.execute(newReq);
            Log.i(Main.TAG, "req:" + response.getStatusLine().getStatusCode());
            while (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY || response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY) {
                Log.i(Main.TAG, "redirect to:" + response.getFirstHeader("Location").getValue());
                newReq = new HttpGet(response.getFirstHeader("Location").getValue());
                response = HttpManager.execute(newReq);
                Log.i(Main.TAG, "req:" + response.getStatusLine().getStatusCode());
            }
            lenghtOfFile = response.getEntity().getContentLength();
            input = response.getEntity().getContent();
            output = new FileOutputStream(market[0].getFile());
            lengthRead = copy(input, output, lenghtOfFile);
        } catch (MalformedURLException e) {
            Log.w(Main.TAG, "error downloading " + market[0].apkURL, e);
        } catch (IOException e) {
            Log.w(Main.TAG, "error downloading " + market[0].apkURL, e);
        } finally {
            Log.v(Main.TAG, "failed to download " + market[0].apkURL + " " + lengthRead + "/" + lenghtOfFile);
            if (lenghtOfFile != 0 && lengthRead != lenghtOfFile) {
                Log.w(Main.TAG, "failed to download " + market[0].apkURL + " " + lengthRead + "/" + lenghtOfFile);
                try {
                    if (input != null) input.close();
                    if (output != null) output.close();
                    market[0].getFile().delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.v(Main.TAG, "copied " + market[0].apkURL + " to " + market[0].getFile());
        return null;
    }

    private int copy(InputStream inFile, OutputStream outFile, long lenghtOfFile) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        Log.v(Main.TAG, "copy " + inFile + " to " + outFile + " len:" + lenghtOfFile);
        int progress = -1;
        long total = 0;
        try {
            out = new BufferedOutputStream(outFile);
            if (lenghtOfFile == -1 || lenghtOfFile == 0) {
                byte buf[] = new byte[1500];
                while (true) {
                    int data = inFile.read(buf);
                    if (isCancelled() || data == -1) {
                        Log.v(Main.TAG, "stop reading " + inFile + " after " + total + " data:" + data);
                        break;
                    }
                    out.write(buf, 0, data);
                    total += data;
                }
            } else {
                in = new BufferedInputStream(inFile);
                while (true) {
                    int data = in.read();
                    if (isCancelled() || data == -1) break;
                    total++;
                    if (progress != (total * ProgressRange / lenghtOfFile)) {
                        progress = (int) (total * ProgressRange / lenghtOfFile);
                        publishProgress(progress);
                    }
                    out.write(data);
                    if (data == 10) {
                    }
                }
            }
        } catch (FileNotFoundException e) {
            progress = 0;
            total = 0;
            Log.w(Main.TAG, "Error while loading " + inFile, e);
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            publishProgress(progress);
        }
        Log.v(Main.TAG, "finished loading " + total);
        return (int) total;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mMonitor.preDownloadAPK(this);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        mMonitor.postDownloadAPK(this);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        mMonitor.progressAPK(this, values[0]);
    }
}
