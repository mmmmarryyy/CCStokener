package ar.edu.unicen.exa.wac.commands;

import java.io.InputStream;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import ar.edu.unicen.exa.wac.mappers.ParameterMapper;
import com.sun.corba.se.impl.ior.ByteBuffer;
import com.thoughtworks.selenium.Selenium;

/**
 * Este comando retorna, en su ejecuci�n, un buffer de bytes con el resultado
 * de la respuesta de un comando GET HTTP. Para ello, utiliza una URL de donde obtendr� los
 * datos, y una lista de par�metros para armar el query. Por ejemplo si necesitamos
 * obtener una imagen cuya direccion es "http://www.example.com?img=32&var=hello",
 * se crea un comando de este tipo con la URL "http://www.example.com", y se le asignan
 * los par�metros img y var, donde los valores de los mismos los obtiene de sus getCommand's
 * asociados.
 *
 */
public class GetHttpStreamCommand extends SeleniumGetCommand {

    private String url = null;

    private List<ParameterMapper> params;

    @Override
    public Object get(Selenium selenium, String locator) {
        setUrl(getUrl() + "?");
        for (ParameterMapper p : params) setUrl(getUrl() + (p.getId() + "=" + p.getValue(selenium) + "&"));
        HttpClient httpclient = new DefaultHttpClient();
        ByteBuffer bb = new ByteBuffer();
        try {
            HttpGet httpget = new HttpGet(getUrl());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                byte[] tmp = new byte[2048];
                while ((instream.read(tmp)) != -1) {
                    for (int i = 0; i < 2048; i++) bb.append(tmp[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bb;
    }

    public void setParams(List<ParameterMapper> params) {
        this.params = params;
    }

    public List<ParameterMapper> getParams() {
        return params;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
