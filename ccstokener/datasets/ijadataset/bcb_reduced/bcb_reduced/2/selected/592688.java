package piramide.multimodal.downloader.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import android.content.Context;

public class RestClient {

    private final URLBuilder urlBuilder;

    private final Context activity;

    public RestClient(Context context) {
        this.urlBuilder = new URLBuilder(context);
        this.activity = context;
    }

    public String[] listApplications() throws DownloaderException {
        final String url = this.urlBuilder.buildApplicationsURL();
        return json2list(url);
    }

    public String[] listVersions(String application) throws DownloaderException {
        final String url = this.urlBuilder.buildVersionsURL(application);
        return json2list(url);
    }

    public void downloadAndInstallApplication(String application, String version, Map<String, Object> userVariables) throws DownloaderException {
        final String applicationURL = this.urlBuilder.buildApplicationURL(application, version, userVariables);
        final ApplicationDownloader downloader = new ApplicationDownloader(this.activity, applicationURL);
        downloader.download();
        downloader.install();
    }

    private String[] json2list(String url) throws DownloaderException {
        try {
            final HttpClient client = new DefaultHttpClient();
            final HttpResponse response = client.execute(new HttpGet(url));
            final InputStream is = response.getEntity().getContent();
            final int size = (int) response.getEntity().getContentLength();
            final byte[] buffer = new byte[size];
            final BufferedInputStream bis = new BufferedInputStream(is);
            int position = 0;
            while (position < size) {
                final int read = bis.read(buffer, position, buffer.length - position);
                if (read <= 0) break;
                position += read;
            }
            final String strResponse = new String(buffer);
            final JSONArray arr = new JSONArray(strResponse);
            final String[] returnValue = new String[arr.length()];
            for (int i = 0; i < arr.length(); ++i) returnValue[i] = arr.getString(i);
            return returnValue;
        } catch (ClientProtocolException e) {
            throw new DownloaderException(e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new DownloaderException(e.getMessage(), e);
        } catch (IOException e) {
            throw new DownloaderException(e.getMessage(), e);
        } catch (JSONException e) {
            throw new DownloaderException(e.getMessage(), e);
        }
    }
}
