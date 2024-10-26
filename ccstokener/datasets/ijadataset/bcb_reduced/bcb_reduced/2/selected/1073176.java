package org.redpin.android.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.os.AsyncTask;
import android.util.Log;
import android.view.WindowManager.BadTokenException;

/**
 * {@link AsyncTask} for uploading images in the background
 * 
 * @author Pascal Brogle (broglep@student.ethz.ch)
 * @author Luba Rogoleva (lubar@student.ethz.ch)
 *
 */
public class UploadImageTask extends AsyncTask<String, Void, String> {

    private static final String TAG = DownloadImageTask.class.getSimpleName();

    private UploadImageTaskCallback callback;

    private static final String lineEnd = "\r\n";

    private static final String twoHyphens = "--";

    private static final String boundary = "redpin";

    public UploadImageTask() {
    }

    public UploadImageTask(UploadImageTaskCallback callback) {
        this.callback = callback;
    }

    /**
	 * Uploads a local image to the redpin server.
	 * 
	 * @param params
	 *            path of the local image to be uploaded (only first is considered)
	 * @return URL of the uploaded image
	 */
    @Override
    protected String doInBackground(String... params) {
        HttpURLConnection conn = null;
        String localFilePath = params[0];
        if (localFilePath == null) {
            return null;
        }
        try {
            URL url = new URL(ConnectionHandler.getServerURL() + ":" + ConnectionHandler.getServerPort() + "/");
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            DataInputStream fileReader = new DataInputStream(new FileInputStream(localFilePath));
            dos.write(toByte(twoHyphens + boundary + lineEnd));
            dos.write(toByte("Content-Disposition: form-data; name=\"uploadfile\"; filename=\"redpinfile\"" + lineEnd));
            dos.write(toByte("Content-Type: application/octet-stream" + lineEnd));
            dos.write(toByte("Content-Length: " + fileReader.available() + lineEnd));
            dos.write(toByte(lineEnd));
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileReader.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.write(toByte(lineEnd));
            dos.write(toByte(twoHyphens + boundary + twoHyphens + lineEnd));
            dos.flush();
            dos.close();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                int ch;
                StringBuffer b = new StringBuffer();
                while ((ch = is.read()) != -1) {
                    b.append((char) ch);
                }
                return b.toString();
            }
        } catch (MalformedURLException ex) {
            Log.w(TAG, "error: " + ex.getMessage(), ex);
        } catch (IOException ioe) {
            Log.w(TAG, "error: " + ioe.getMessage(), ioe);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    /**
	 * Calls the callback (if supplied) after the image is uploaded
	 * 
	 * @param result
	 *            URL of the uploaded image
	 */
    @Override
    protected void onPostExecute(String result) {
        if (callback != null) {
            try {
                if (result != null) {
                    callback.onImageUploaded(result);
                } else {
                    callback.onImageUploadFailure();
                }
            } catch (BadTokenException e) {
                Log.w(TAG, "Callback failed, caught BadTookenException: " + e.getMessage(), e);
            } catch (Exception e) {
                Log.w(TAG, "Callback failed, caught Exception: " + e.getMessage(), e);
            }
            callback = null;
        }
    }

    /**
	 * Converts string to ASCII byte representation
	 * 
	 * @param s String
	 * @return byte converted string
	 */
    private static byte[] toByte(String s) {
        if (s == null || s.length() == 0) {
            return new byte[0];
        }
        byte[] r = null;
        try {
            r = s.getBytes("US-ASCII");
        } catch (Exception e) {
            r = s.getBytes();
        }
        return r;
    }

    /**
	 * Callback Interface for {@link UploadImageTask}
	 * 
	 * @author Pascal Brogle (broglep@student.ethz.ch)
	 * 
	 */
    public interface UploadImageTaskCallback {

        public void onImageUploaded(String path);

        public void onImageUploadFailure();
    }
}
