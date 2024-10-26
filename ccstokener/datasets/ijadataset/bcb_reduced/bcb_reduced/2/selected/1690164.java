package neembuu.directuploader;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import jpfm.FileType;
import neembuu.accountfs.AccountDirectory;
import neembuu.accountfs.AccountFileContainer;
import neembuu.accountfs.AccountRoot;
import neembuu.mediafire.MediafireUploadDestinationHost;
import neembuu.mediafire.MediafireUploaderInstanceProvider;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

/**
 *
 * @author Shashank Tulsyan
 */
public final class NeembuuUploadManager {

    private static NeembuuUploadManager SINGLETON = new NeembuuUploadManager();

    /**
     * used instead of linkedlist for thread safety
     */
    private final ConcurrentLinkedQueue<UploaderInstanceProvider> providers = new ConcurrentLinkedQueue<UploaderInstanceProvider>();

    private final ConcurrentLinkedQueue<UploadDestinationHost> knownDestinations = new ConcurrentLinkedQueue<UploadDestinationHost>();

    private NeembuuUploadManager() {
        knownDestinations.add(MediafireUploadDestinationHost.getSingleton());
        providers.add(MediafireUploaderInstanceProvider.getInstance());
    }

    /**
     *
     * @return
     * @throws SecurityException In real life we wouldn't want just any class
     * to get access to an uploader. That classs (can be from an external plugin)
     * can abuse this service. The ways to prevent that is
     * #1) loading that into a separate java.lang.Classloader
     *  and making the class, exact same copy, as an interface,
     *  that interface being  loaded in the plugins classloader
     *  and thus restrict access.
     * #2) throw security exception if we don 't like the function
     * that is invoking us. We can do this by checking the invocation StackTrace.
     */
    public static final NeembuuUploadManager getInstance() throws SecurityException {
        return SINGLETON;
    }

    /**
     * Uploaders that we have
     * @return
     */
    public final synchronized List<Class<? extends Uploader>> getAvailableUploaders() {
        LinkedList<Class<? extends Uploader>> uploaders = new LinkedList<Class<? extends Uploader>>();
        for (UploaderInstanceProvider uip : providers) {
            uploaders.add(uip.getUploaderClass());
        }
        return uploaders;
    }

    public final <U extends Uploader> void registerUploader(UploaderInstanceProvider provider) {
        providers.add(provider);
    }

    /**
     * Destinations which we know about. We know more destinations
     * than we can support. Like we know megaupload, but we don 't have
     * the program to upload to megaupload.
     * @return
     */
    public List<UploadDestinationHost> getKnownDestinations() {
        LinkedList<UploadDestinationHost> hosts = new LinkedList<UploadDestinationHost>(this.knownDestinations);
        return hosts;
    }

    private AbstractHttpClient getHttpClientReservedFor(UploadOperation uploadOperation) {
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        DefaultHttpClient httpClient = new DefaultHttpClient(params);
        return httpClient;
    }

    /**
     * in real world situation we would have a manager that sites above
     * this manager and commands it to upload. Not just any function
     * from any class can command a manager class :)
     *
     *
     *
     * This is our guess of lifecycle of ANY uploader : mediafire, hotfiles,
     * rapidshare or anything.
     * This should work in all cases. If it doesn 't we need to redesign it
     */
    private Uploader upload(UploadDestinationHost destinationHost, UploaderAccount account, UploadOperation uploadOperation, UploadProgressListener listener, String destinationFolderName) throws Exception {
        UploaderInstanceProvider uploaderInstanceProvider = getUploaderInstanceProvider(destinationHost);
        if (!uploaderInstanceProvider.canUpload(uploadOperation)) throw new UnsupportedOperationException();
        AbstractHttpClient httpClient = getHttpClientReservedFor(uploadOperation);
        LoginRequest loginRequest = account.getLoginRequest(httpClient.getCookieStore());
        httpClient.execute(loginRequest.getPreLoginRequest()).getEntity().consumeContent();
        HttpResponse response = httpClient.execute(loginRequest.getHttpUriRequest());
        account.setLoginResponse(new HttpLoginResponse(response, httpClient.getCookieStore().getCookies()));
        response.getEntity().consumeContent();
        HttpGet accountRootReq = account.getAccountRootRequest(httpClient.getCookieStore());
        response = httpClient.execute(accountRootReq);
        AccountRoot rootDirectory = account.getAccountRoot(response);
        AccountFileContainer destinationDirectory = null;
        try {
            AccountDirectory accountDirectory = (AccountDirectory) rootDirectory.get(destinationFolderName, FileType.FOLDER);
            if (accountDirectory == null) {
                destinationDirectory = accountDirectory;
            }
        } catch (ClassCastException cce) {
            destinationDirectory = rootDirectory;
        }
        Uploader uploader = uploaderInstanceProvider.getInstance(uploadOperation, account, destinationDirectory);
        return uploader;
    }

    private void uploadDataSendStartVerification(Uploader uploader) {
        AbstractHttpClient httpClient = getHttpClientReservedFor(uploader.getUploadOperation());
    }

    private UploaderInstanceProvider getUploaderInstanceProvider(UploadDestinationHost destinationHost) {
        for (UploaderInstanceProvider uploaderInstanceProvider : providers) {
            if (uploaderInstanceProvider.getUploadDestination().implies(destinationHost)) return uploaderInstanceProvider;
        }
        return null;
    }

    public static void main(String[] args) {
    }
}
