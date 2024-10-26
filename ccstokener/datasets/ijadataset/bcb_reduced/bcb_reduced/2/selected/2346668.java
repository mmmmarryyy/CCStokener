package orxatas.travelme.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import android.util.Log;
import android.os.AsyncTask;

/**
 * Clase que envía una petición préviamente configuarada.
 * */
abstract class AsyncInternetConnection extends AsyncTask<Void, Void, String> {

    /**
	 * URL de la petición.
	 * */
    private final String url;

    private String urlFormated;

    private List<NameValuePair> GETparamList = null;

    private List<NameValuePair> POSTparamList = null;

    public AsyncInternetConnection(String url) {
        this.url = url;
    }

    /**
	 * Establece los parámetros GET de la petición.
	 * */
    public void addGETParams(List<NameValuePair> paramList) {
        GETparamList = paramList;
    }

    /**
	 * Establece los parámetros POST de la petición.
	 * */
    public void addPOSTParams(List<NameValuePair> paramList) {
        POSTparamList = paramList;
    }

    /**
	 * Formatea la petición y su URL.
	 * */
    private void formatCall() {
        if (GETparamList != null && GETparamList.size() > 0) {
            if (!url.endsWith("?")) urlFormated = url + "?";
            urlFormated += URLEncodedUtils.format(GETparamList, "utf-8");
        }
        Log.d("URLWS", urlFormated);
    }

    protected abstract void onPostExecute(String response);

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        formatCall();
    }

    private String processAnswer(HttpResponse response) {
        if (response != null) {
            try {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream stream;
                    stream = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    stream.close();
                    String responseString = sb.toString();
                    return responseString;
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    @Override
    protected String doInBackground(Void... params) {
        HttpPost prequest = null;
        HttpGet grequest = null;
        if (POSTparamList != null && POSTparamList.size() > 0) {
            prequest = new HttpPost(urlFormated);
            try {
                prequest.setEntity(new UrlEncodedFormEntity(POSTparamList));
            } catch (UnsupportedEncodingException e) {
                Log.e("HTTPPOST", "Error en la codificación de los parámetros.");
                e.printStackTrace();
            }
        } else {
            grequest = new HttpGet(urlFormated);
        }
        try {
            HttpResponse response = new DefaultHttpClient().execute((prequest != null) ? prequest : grequest);
            return processAnswer(response);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
