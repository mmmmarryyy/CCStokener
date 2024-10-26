package org.magicdroid.app.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.zip.GZIPOutputStream;
import org.magicdroid.app.cache.AttributeWrapper;
import org.magicdroid.app.cache.PropertyAttribute;
import org.magicdroid.app.cache.ScopedAttribute;
import org.magicdroid.commons.Injector;
import org.magicdroid.commons.Injector.Inject;
import org.magicdroid.commons.MagicObject;
import org.magicdroid.commons.MagicObject.Interface;
import org.magicdroid.commons.MagicObject.Invocation;
import org.magicdroid.serializer.MagicdroidSerializer;
import org.magicdroid.server.MagicdroidServerService;
import org.magicdroid.services.MagicdroidService;
import org.magicdroid.services.RPCService;

public class RPCServiceImpl implements MagicObject.Concern, RPCService {

    private static final PropertyAttribute SERVER = new PropertyAttribute("org.magicdroid.SERVER");

    private static final PropertyAttribute PORT = new PropertyAttribute("org.magicdroid.PORT");

    private static final ScopedAttribute<Integer> CONNECT_TIMEOUT = new AttributeWrapper.AsInteger(new PropertyAttribute("org.magicdroid.CONNECT_TIMEOUT"));

    private static final class RPCExceptionTunnel extends RuntimeException {

        private final String text;

        public RPCExceptionTunnel(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    @Interface
    private Class<? extends MagicdroidServerService> type;

    private final Injector injector;

    @Inject
    public RPCServiceImpl(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Object invoke(Invocation context) throws Throwable {
        return this.transport(this.type, context.getMethod().getName(), context.getParams());
    }

    @Override
    public Object transport(Class<? extends MagicdroidService> service, String method, Object[] params) throws Throwable {
        MagicdroidSerializer serializer = this.injector.create(MagicdroidSerializer.class);
        return this.internalTransport(service, method, serializer.encode(params));
    }

    @Override
    public Object internalTransport(Class<? extends MagicdroidService> service, String method, String params) throws Throwable {
        MagicdroidSerializer serializer = this.injector.create(MagicdroidSerializer.class);
        try {
            String text = postJSONObject(MessageFormat.format("http://{0}:{1}/ServiceServlet?svc={2}&m={3}", new Object[] { SERVER.get(), PORT.get(), service.getName(), method }), "p", params);
            return serializer.decode(text);
        } catch (RPCExceptionTunnel e) {
            throw (Throwable) serializer.decode(e.getText());
        }
    }

    private String postJSONObject(String url2, String parameterKey, String jsondata) throws RPCExceptionTunnel, CommunicationException {
        try {
            URL url = new URL(url2);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT.get());
            conn.setDoOutput(true);
            RPCUtil.encode(conn.getOutputStream(), jsondata);
            String status = conn.getHeaderField("CALL");
            String line2 = RPCUtil.decode(conn.getInputStream());
            if (status != null && status.equals("EXCEPTION")) throw new RPCExceptionTunnel(line2);
            return line2;
        } catch (IOException e) {
            throw new CommunicationException(url2, e);
        }
    }

    private byte[] countchars(byte[] bytes) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzip = new GZIPOutputStream(output);
            gzip.write(bytes);
            gzip.close();
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
