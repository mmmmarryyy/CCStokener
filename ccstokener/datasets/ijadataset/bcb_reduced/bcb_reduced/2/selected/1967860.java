package org.kaleidofoundry.core.store;

import static org.kaleidofoundry.core.store.FileStoreConstants.FtpStorePluginName;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import org.kaleidofoundry.core.context.RuntimeContext;
import org.kaleidofoundry.core.i18n.InternalBundleHelper;
import org.kaleidofoundry.core.lang.annotation.Immutable;
import org.kaleidofoundry.core.lang.annotation.NotNull;
import org.kaleidofoundry.core.plugin.Declare;
import org.kaleidofoundry.core.util.StringHelper;

/**
 * FTP read only store implementation <br/>
 * <b>This implementation is only for read only use</b> - the methods store, remove, move will throws {@link ResourceException}<br/>
 * <br/>
 * You can create your own store, by extending this class and overriding methods :
 * <ul>
 * <li>{@link #doRemove(URI)}</li>
 * <li>{@link #doStore(URI, ResourceHandler)}</li>
 * </ul>
 * Then, annotate {@link Declare} your new class to register your implementation
 * 
 * @author Jerome RADUGET
 * @see FileStoreContextBuilder enum of context configuration properties available
 */
@Immutable
@Declare(FtpStorePluginName)
public class FtpStore extends AbstractFileStore implements FileStore {

    /**
    * @param context
    */
    public FtpStore(@NotNull final RuntimeContext<FileStore> context) {
        super(context);
    }

    /**
    * @param baseUri
    * @param context
    */
    public FtpStore(final String baseUri, final RuntimeContext<FileStore> context) {
        super(baseUri, context);
    }

    /**
    * @see AbstractFileStore#AbstractFileStore()
    */
    FtpStore() {
        super();
    }

    @Override
    public FileStoreType[] getStoreType() {
        return new FileStoreType[] { FileStoreTypeEnum.ftp };
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    protected ResourceHandler doGet(final URI resourceUri) throws ResourceException {
        if (resourceUri.getHost() == null) {
            throw new IllegalStateException(InternalBundleHelper.StoreMessageBundle.getMessage("store.uri.ftp.illegal", resourceUri.toString()));
        }
        try {
            final URL configUrl = resourceUri.toURL();
            final URLConnection urlConnection;
            Proxy httpProxy = null;
            if (!StringHelper.isEmpty(context.getString(FileStoreContextBuilder.ProxySet))) {
                if (context.getBoolean(FileStoreContextBuilder.ProxySet)) {
                    final String proxyHost = context.getString(FileStoreContextBuilder.ProxyHost);
                    final String proxyPort = context.getString(FileStoreContextBuilder.ProxyPort);
                    if (!StringHelper.isEmpty(proxyHost)) {
                        httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, !StringHelper.isEmpty(proxyPort) ? Integer.parseInt(proxyPort) : 80));
                        if (!StringHelper.isEmpty(context.getString(FileStoreContextBuilder.NonProxyHosts))) {
                            System.getProperties().put("ftp.nonProxyHosts", context.getProperty(FileStoreContextBuilder.NonProxyHosts));
                        }
                        if (!StringHelper.isEmpty(context.getString(FileStoreContextBuilder.ProxyUser)) && !StringHelper.isEmpty(context.getString(FileStoreContextBuilder.ProxyPassword))) {
                            Authenticator.setDefault(new Authenticator() {

                                @Override
                                protected PasswordAuthentication getPasswordAuthentication() {
                                    return new PasswordAuthentication(context.getString(FileStoreContextBuilder.ProxyUser), context.getString(FileStoreContextBuilder.ProxyPassword).toCharArray());
                                }
                            });
                        }
                    }
                }
            }
            if (httpProxy == null) {
                urlConnection = configUrl.openConnection();
            } else {
                urlConnection = configUrl.openConnection(httpProxy);
            }
            setUrlConnectionSettings(urlConnection);
            urlConnection.connect();
            try {
                return createResourceHandler(resourceUri.toString(), urlConnection.getInputStream());
            } catch (final FileNotFoundException fnfe) {
                throw new ResourceNotFoundException(resourceUri.toString());
            }
        } catch (final MalformedURLException mure) {
            throw new IllegalStateException(InternalBundleHelper.StoreMessageBundle.getMessage("store.uri.malformed", resourceUri.toString()));
        } catch (final ConnectException ce) {
            throw new ResourceException("store.connection.error", ce, resourceUri.toString());
        } catch (final IOException ioe) {
            if (ioe instanceof ResourceException) {
                throw (ResourceException) ioe;
            } else {
                throw new ResourceException(ioe, resourceUri.toString());
            }
        }
    }

    @Override
    protected void doRemove(final URI resourceUri) throws ResourceNotFoundException, ResourceException {
        throw new ResourceException("store.readonly.illegal", context.getName());
    }

    @Override
    protected void doStore(final URI resourceUri, final ResourceHandler resource) throws ResourceException {
        throw new ResourceException("store.readonly.illegal", context.getName());
    }
}
