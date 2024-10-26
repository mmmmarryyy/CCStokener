package com.rif.client.service.http.transport;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import com.rif.client.service.Constant;
import com.rif.client.service.dataexchange.IClientDataExchange;
import com.rif.client.service.definition.TransportClientModel;
import com.rif.client.service.http.dataexchange.HttpDataClientExchangeImpl;
import com.rif.client.service.transport.ITransportClient;
import com.rif.common.request.IServiceRequest;
import com.rif.common.util.ByteUtil;

/**
 * @author bruce.liu (mailto:jxta.liu@gmail.com)
 * 2011-7-29 下午10:48:54
 */
public class HttpTransportClient implements ITransportClient {

    private static final String SUFFIX_RIF = ".rif";

    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private static final String CONTENT_LENGTH = "Content-length";

    private static final String CONTENT_TYPE = "Content-type";

    private IClientDataExchange dataExchange;

    public HttpTransportClient() {
        this.dataExchange = new HttpDataClientExchangeImpl();
    }

    @Override
    public boolean accept(String type) {
        if (null != type && Constant.TYPE_HTTP.equalsIgnoreCase(type)) {
            return true;
        }
        return false;
    }

    @Override
    public byte[] transport(TransportClientModel model, final IServiceRequest request, byte[] message) throws Throwable {
        byte[] sendMessage = this.dataExchange.exchangeSend(message, request);
        byte[] receiveMessage = this.dataExchange.exchangeReceive(doSend(getURL(model, request), sendMessage));
        return receiveMessage;
    }

    private URL getURL(TransportClientModel model, final IServiceRequest request) throws MalformedURLException {
        String url = null;
        if (model.getUrl().endsWith(File.separator)) {
            url = model.getUrl() + request.getServiceName() + SUFFIX_RIF;
        } else {
            url = model.getUrl() + File.separator + request.getServiceName() + SUFFIX_RIF;
        }
        return new URL(url);
    }

    private byte[] doSend(URL url, byte[] message) throws Throwable {
        URLConnection conn = null;
        DataOutputStream out = null;
        InputStream in = null;
        try {
            conn = url.openConnection();
            conn.setUseCaches(true);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
            conn.setRequestProperty(CONTENT_LENGTH, "" + message.length);
            out = new DataOutputStream(conn.getOutputStream());
            out.write(message);
            out.flush();
            in = conn.getInputStream();
            byte[] responseMessage = ByteUtil.getBytes(in);
            return responseMessage;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Throwable ignore) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Throwable ignore) {
            }
        }
    }

    public IClientDataExchange getDataExchange() {
        return dataExchange;
    }

    public void setDataExchange(IClientDataExchange dataExchange) {
        this.dataExchange = dataExchange;
    }
}
