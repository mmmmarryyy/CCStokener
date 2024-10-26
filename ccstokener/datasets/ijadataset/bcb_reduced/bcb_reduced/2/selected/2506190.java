package com.googlecode.jwsm.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;

public class JWSMClient {

    private String baseURL;

    public JWSMClient(String baseURL) {
        if (!baseURL.endsWith("/")) {
            baseURL = baseURL + "/";
        }
        this.baseURL = baseURL;
    }

    public Object invoke(String service, String method, Object... args) throws IOException {
        URL url = new URL(baseURL + "/ServiceRequest/" + service + "/" + method);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestProperty("User-Agent", "JWSM Client");
        connection.setDoInput(true);
        if (args.length > 0) {
            connection.setDoOutput(true);
            ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
            for (Object o : args) {
                oos.writeObject(o);
            }
            oos.flush();
        }
        System.out.println("Response: " + connection.getResponseCode() + "/" + connection.getResponseMessage());
        Object response = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
            response = ois.readObject();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    public static final <T> T generateClient(Class<T> c, String baseURL, String service) {
        JWSMProxyClient handler = new JWSMProxyClient(baseURL, service);
        T t = (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, handler);
        return t;
    }
}

class JWSMProxyClient implements InvocationHandler {

    private JWSMClient client;

    private String service;

    public JWSMProxyClient(String baseURL, String service) {
        client = new JWSMClient(baseURL);
        this.service = service;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return client.invoke(service, method.getName(), args);
    }
}
