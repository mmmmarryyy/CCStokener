package open.gps.gopens.location.log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import open.gps.gopens.location.log.utils.RecordedGeoPoint;
import open.gps.gopens.location.log.utils.RecordedRouteGPXFormatter;
import open.gps.gopens.location.log.utils.Util;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import android.util.Log;

public class GpxToPHPUploader {

    protected static final String UPLOADSCRIPT_URL = "http://www.PLACEYOURDOMAINHERE.com/anyfolder/gpxuploader/upload.php";

    public static void uploadAsync(final ArrayList<RecordedGeoPoint> recordedGeoPoints) {
        new Thread(new Runnable() {

            public void run() {
                try {
                    if (!Util.isSufficienDataForUpload(recordedGeoPoints)) return;
                    final InputStream gpxInputStream = new ByteArrayInputStream(RecordedRouteGPXFormatter.create(recordedGeoPoints).getBytes());
                    final HttpClient httpClient = new DefaultHttpClient();
                    final HttpPost request = new HttpPost(UPLOADSCRIPT_URL);
                    httpClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
                    final HttpResponse response = httpClient.execute(request);
                    final int status = response.getStatusLine().getStatusCode();
                    if (status != HttpStatus.SC_OK) {
                        Log.e("GPXUploader", "status != HttpStatus.SC_OK");
                    } else {
                        final Reader r = new InputStreamReader(new BufferedInputStream(response.getEntity().getContent()));
                        final char[] buf = new char[8 * 1024];
                        int read;
                        final StringBuilder sb = new StringBuilder();
                        while ((read = r.read(buf)) != -1) sb.append(buf, 0, read);
                        Log.d("GPXUploader", "Response: " + sb.toString());
                    }
                } catch (Exception e) {
                }
            }
        }).start();
    }
}
