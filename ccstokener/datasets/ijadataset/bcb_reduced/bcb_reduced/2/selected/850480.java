package jaxlib.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import jaxlib.util.CheckArg;

/**
 * Default implementation of the {@code URLConnectionFactory} interface.
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: DefaultURLConnectionFactory.java 3036 2012-01-10 02:24:08Z joerg_wassmer $
 */
public class DefaultURLConnectionFactory extends Object implements URLConnectionFactory, Serializable {

    /**
   * @since JaXLib 1.0
   */
    private static final long serialVersionUID = 1L;

    private transient volatile Integer connectTimeout;

    private transient volatile Integer readTimeout;

    private transient volatile Boolean useCaches;

    public DefaultURLConnectionFactory() {
        super();
    }

    /**
   * @serialData
   */
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        this.connectTimeout = (Integer) in.readObject();
        this.readTimeout = (Integer) in.readObject();
        this.useCaches = (Boolean) in.readObject();
    }

    /**
   * @serialData
   */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.connectTimeout);
        out.writeObject(this.readTimeout);
        out.writeObject(this.useCaches);
    }

    public void configureURLConnection(URLConnection co) throws IOException {
        Integer connectTimeout = getConnectTimeout();
        if (connectTimeout != null) co.setConnectTimeout(connectTimeout);
        Integer readTimeout = getReadTimeout();
        if (readTimeout != null) co.setReadTimeout(readTimeout);
        Boolean useCaches = getUseCaches();
        if (useCaches != null) co.setUseCaches(useCaches);
    }

    public Integer getConnectTimeout() {
        return this.connectTimeout;
    }

    public Integer getReadTimeout() {
        return this.readTimeout;
    }

    public Boolean getUseCaches() {
        return this.useCaches;
    }

    public URLConnection createURLConnection(URL url) throws IOException {
        Proxy proxy = selectProxy(url);
        URLConnection co = (proxy == null) ? url.openConnection() : url.openConnection(proxy);
        configureURLConnection(co);
        return co;
    }

    public Proxy selectProxy(URL url) throws IOException {
        return null;
    }

    public void setConnectTimeout(Integer millis) {
        if (millis != null) CheckArg.gt(millis.intValue(), -1, "millis");
        this.connectTimeout = millis;
    }

    public void setReadTimeout(Integer millis) {
        if (millis != null) CheckArg.gt(millis.intValue(), -1, "millis");
        this.readTimeout = millis;
    }

    public void setUseCaches(Boolean useCaches) {
        this.useCaches = useCaches;
    }
}
