package org.jampa.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.jampa.controllers.Controller;
import org.jampa.logging.Log;
import org.jampa.preferences.PreferenceConstants;

public class NetworkManager {

    private static NetworkManager _instance = null;

    private static Proxy _proxy = null;

    private static String _url;

    private static int _port;

    private static String _username;

    private static String _password;

    private NetworkManager() {
        initializeProxy();
        Controller.getInstance().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                if ((event.getProperty().equals(PreferenceConstants.PROXY_TYPE)) || (event.getProperty().equals(PreferenceConstants.PROXY_URL)) || (event.getProperty().equals(PreferenceConstants.PROXY_PORT)) || (event.getProperty().equals(PreferenceConstants.PROXY_USERNAME)) || (event.getProperty().equals(PreferenceConstants.PROXY_PASSWORD))) {
                    initializeProxy();
                }
            }
        });
    }

    public static NetworkManager getInstance() {
        if (_instance == null) {
            _instance = new NetworkManager();
        }
        return _instance;
    }

    private void initializeProxy() {
        String proxyType = Controller.getInstance().getPreferenceStore().getString(PreferenceConstants.PROXY_TYPE);
        try {
            if (proxyType.equals(Proxy.Type.DIRECT.toString())) {
                _proxy = null;
            } else {
                _url = Controller.getInstance().getPreferenceStore().getString(PreferenceConstants.PROXY_URL);
                _port = Controller.getInstance().getPreferenceStore().getInt(PreferenceConstants.PROXY_PORT);
                _username = Controller.getInstance().getPreferenceStore().getString(PreferenceConstants.PROXY_USERNAME);
                _password = Controller.getInstance().getPreferenceStore().getString(PreferenceConstants.PROXY_PASSWORD);
                if (proxyType.equals(Proxy.Type.HTTP.toString())) {
                    _proxy = new Proxy(Proxy.Type.HTTP, new Socket(_url, _port).getRemoteSocketAddress());
                } else if (proxyType.equals(Proxy.Type.SOCKS.toString())) {
                    _proxy = new Proxy(Proxy.Type.SOCKS, new Socket(_url, _port).getRemoteSocketAddress());
                }
            }
        } catch (UnknownHostException e) {
            Log.getInstance(NetworkManager.class).warn("Error while creating proxy (UnknownHostException): " + e.getStackTrace());
            _proxy = null;
        } catch (IOException e) {
            Log.getInstance(NetworkManager.class).warn("Error while creating proxy (IOException): " + e.getStackTrace());
            _proxy = null;
        }
    }

    public HttpURLConnection getConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = null;
        if (_proxy == null) {
            connection = (HttpURLConnection) url.openConnection();
        } else {
            URLConnection con = url.openConnection(_proxy);
            String encodedUserPwd = new String(Base64.encodeBase64((_username + ":" + _password).getBytes()));
            con.setRequestProperty("Proxy-Authorization", "Basic " + encodedUserPwd);
            connection = (HttpURLConnection) con;
        }
        return connection;
    }
}
