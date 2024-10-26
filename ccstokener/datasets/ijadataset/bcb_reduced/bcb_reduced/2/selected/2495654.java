package fr.macymed.modulo.platform;

import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/** 
 * <p>
 * This class is used to get connection from a Modulo URL. Such URLs are often provided by the ClassLoaders.
 * </p>
 * @author <a href="mailto:alexandre.cartapanis@macymed.fr">CARTAPANIS Alexandre</a>
 * @version 2.3.4
 * @since Modulo Platform 2.0
 */
public class ModuloURLStreamHandler extends URLStreamHandler {

    /**
     * <p>
     * Creates a new ModuloURLStreamHandler.
     * </p>
     */
    public ModuloURLStreamHandler() {
        super();
    }

    /**
     * <p>
     * Creates new URL connection.
     * </p>
     * <p>
     * This method will return a {@link fr.macymed.modulo.platform.ModuloURLConnection}.
     * </p>
     * @param _url The URL we want a connection for.
     * @return <code>URLConnection</code> - The connection used to access to URL's resources.
     */
    @Override
    protected URLConnection openConnection(URL _url) {
        return new ModuloURLConnection(_url);
    }

    /**
     * <p>
     * Creates new URL connection.
     * </p>
     * <p>
     * This method will return a {@link fr.macymed.modulo.platform.ModuloURLConnection}.
     * </p>
     * <p>
     * WARNING: Because ModuloPlatform manages his own source, the Proxy parameters will not be used. For networking sources, the proxy must have been modified previously.
     * </p>
     * @param _url The URL we want a connection for.
     * @param _proxy The proxy through which the connection will be made. If direct connection is desired, Proxy.NO_PROXY should be specified.
     * @return <code>URLConnection</code> - The connection used to access to URL's resources.
     */
    @Override
    protected URLConnection openConnection(URL _url, Proxy _proxy) {
        return new ModuloURLConnection(_url);
    }

    /**
     * <p>
     * Compares two urls to see whether they refer to the same file, i.e., having the same protocol, host, port, and path.
     * </p>
     * <p>
     * This method overriden super because host can be different but represent the same source.
     * </p>
     * @param _url1 The URL of the first file to compare.
     * @param _url2 The URL of the second file to compare.
     * @return <code>boolean</code> - <code>True</code> if the two specified URLs refers to the same file.
     */
    @Override
    protected boolean sameFile(URL _url1, URL _url2) {
        String p1 = _url1.getProtocol();
        if (Constants.MODULO_PROTOCOL.equals(p1)) {
            if (!p1.equals(_url2.getProtocol())) {
                return false;
            }
            if (!hostsEqual(_url1, _url1)) {
                return false;
            }
            if (!(_url1.getFile() == _url2.getFile() || (_url1.getFile() != null && _url1.getFile().equals(_url2.getFile())))) {
                return false;
            }
            return true;
        }
        return _url1.equals(_url2);
    }

    /**
     * <p>
     * Compares the host components of two URLs.
     * </p>
     * <p>
     * This method overriden super because host can be different but represent the same source.
     * </p>
     * @param _url1 The URL of the first host to compare.
     * @param _url2 The URL of the second host to compare.
     * @return <code>boolean</code> - <code>True</code> if and only if they are equal, <code>false</code> otherwise.
     */
    @Override
    protected boolean hostsEqual(URL _url1, URL _url2) {
        String s1 = _url1.getHost();
        String s2 = _url2.getHost();
        return (s1 == s2) || (s1 != null && s1.equals(s2));
    }

    /**
     * <p>
     * Returns a hash code value for the specified. This method is supported for the benefit of hashtables such as those provided by java.util.Hashtable.
     * </p>
     * @param _url The URL we want a hash code for.
     * @return <code>int</code> - A hash code value for the specified object.
     */
    @Override
    protected int hashCode(URL _url) {
        int h = 0;
        if (Constants.MODULO_PROTOCOL.equals(_url.getProtocol())) {
            String host = _url.getHost();
            if (host != null) {
                h = host.hashCode();
            }
            String file = _url.getFile();
            if (file != null) {
                h += file.hashCode();
            }
            String ref = _url.getRef();
            if (ref != null) {
                h += ref.hashCode();
            }
        } else {
            h = _url.hashCode();
        }
        return h;
    }

    /**
     * <p>
     * Indicates whether some URL is "equal to" other one.
     * </p>
     * @param _url1 The first URL to be tested for equality.
     * @param _url2 The second URL to be tested for equality.
     * @return <code>boolean</code> - True if this object is the same as the obj argument; false otherwise.
     */
    @Override
    protected boolean equals(URL _url1, URL _url2) {
        String ref1 = _url1.getRef();
        String ref2 = _url2.getRef();
        return sameFile(_url1, _url2) && (ref1 == ref2 || (ref1 != null && ref1.equals(ref2)));
    }
}
