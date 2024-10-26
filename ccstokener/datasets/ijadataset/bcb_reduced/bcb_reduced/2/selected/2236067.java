package eu.more.core.internal.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.TimerTask;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class KeepAliveThread extends TimerTask {

    Thread runner;

    private boolean debug = true;

    private URL url = null;

    private Bundle bundle = null;

    private String messageId = null;

    public KeepAliveThread(URL t_url, Bundle t_bundle, String t_messageId) {
        runner = new Thread(this);
        runner.setPriority(Thread.NORM_PRIORITY);
        runner.setName("KeepAliveThread");
        url = t_url;
        setBundle(t_bundle);
        messageId = t_messageId;
        ProxyUtils.addTimer(messageId, this);
        log("Starting keep alive for URL: " + t_url);
        runner.start();
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public void run() {
        try {
            URLConnection con1 = url.openConnection();
            con1.connect();
            @SuppressWarnings("unused") InputStream is1 = con1.getInputStream();
        } catch (IOException e) {
            if (e.getMessage().contains("response code: 500")) {
                log("Server {" + url.toString() + "} is still alive!");
            } else {
                log("Server {" + url.toString() + "} is dead! ... Stopping thread, uninstalling and removing service!");
                ProxyUtils.removeProxyInstance(messageId);
                runner.stop();
            }
        }
    }

    private void log(String s) {
        if (debug) System.out.println(s);
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void removeBundle() {
        if (getBundle().getState() == getBundle().ACTIVE) try {
            getBundle().uninstall();
        } catch (BundleException e) {
            e.printStackTrace();
        }
    }
}
