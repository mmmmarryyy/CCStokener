package br.com.guaraba.wally.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import br.com.guaraba.wally.android.exception.ServiceException;

public class HTTPUtils {

    /**
	 * http://localhost:8080/symabroker-app/symaBroker!autenticar.action?login=
	 * siboglo&senha=123
	 * http://localhost:8080/symabroker-app/symaBroker!getPapeis
	 * .action?token=49249318e97786ea5dd729ca89a6e906
	 * 
	 * @param url
	 * @param parameters
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ServiceException 
	 */
    public static String post(String url, Map<String, String> parameters) throws URISyntaxException, ClientProtocolException, IOException, ServiceException, HttpHostConnectException {
        DefaultHttpClient client = new DefaultHttpClient();
        String xmlData = null;
        URI uri = new URI(url);
        List<NameValuePair> requestParameters = null;
        HttpPost method = new HttpPost(uri);
        method.addHeader("User-Agent", "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.8.1.10) Gecko/20071115 Firefox/2.0.0.10");
        method.addHeader("Pragma", "no-cache");
        method.addHeader("Content-Type", "application/x-www-form-urlencoded");
        if (parameters != null && parameters.size() > 0) {
            requestParameters = new ArrayList<NameValuePair>();
            for (String parameter : parameters.keySet()) {
                requestParameters.add(new BasicNameValuePair(parameter, parameters.get(parameter)));
            }
        }
        HttpEntity entity = new UrlEncodedFormEntity(requestParameters, "UTF-8");
        method.setEntity(entity);
        HttpResponse res = client.execute(method);
        InputStream ips = res.getEntity().getContent();
        xmlData = geraString(ips);
        return xmlData;
    }

    public static String geraString(InputStream stream) throws ServiceException {
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        try {
            String cur;
            while ((cur = buffer.readLine()) != null) {
                sb.append(cur + "\n");
            }
        } catch (IOException e) {
            throw new ServiceException("Erro ao efetuar post.");
        }
        try {
            stream.close();
        } catch (IOException e) {
            throw new ServiceException("Erro ao efetuar post.");
        }
        return sb.toString();
    }
}
