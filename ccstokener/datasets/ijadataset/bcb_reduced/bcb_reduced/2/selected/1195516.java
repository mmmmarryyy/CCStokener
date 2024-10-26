package cn.chengdu.in.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import cn.chengdu.in.android.config.Config;

/**
 * @author Declan.Z(declan.zhang@gmail.com)
 * @date 2011-2-15
 */
public class RemoteResourceFetcher extends Observable {

    public static final String TAG = "RemoteResourceFetcher";

    public static final boolean DEBUG = Config.DEBUG;

    private DiskCache mResourceCache;

    private HashMap<String, SoftReference<Bitmap>> mCache;

    private ExecutorService mExecutor;

    private HttpClient mHttpClient;

    private ConcurrentHashMap<Request, Callable<Request>> mActiveRequestsMap = new ConcurrentHashMap<Request, Callable<Request>>();

    public RemoteResourceFetcher(DiskCache diskCache, HashMap<String, SoftReference<Bitmap>> cache) {
        mResourceCache = diskCache;
        mCache = cache;
        mHttpClient = createHttpClient();
        mExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void notifyObservers(Object data) {
        setChanged();
        super.notifyObservers(data);
    }

    public Future<Request> fetch(Uri uri, String hash) {
        Request request = new Request(uri, hash);
        synchronized (mActiveRequestsMap) {
            Callable<Request> fetcher = newRequestCall(request);
            if (mActiveRequestsMap.putIfAbsent(request, fetcher) == null) {
                if (DEBUG) Log.d(TAG, "issuing new request for: " + uri);
                return mExecutor.submit(fetcher);
            } else {
                if (DEBUG) Log.d(TAG, "Already have a pending request for: " + uri);
            }
        }
        return null;
    }

    public void shutdown() {
        mExecutor.shutdownNow();
    }

    private Callable<Request> newRequestCall(final Request request) {
        return new Callable<Request>() {

            public Request call() {
                InputStream is = null;
                try {
                    if (DEBUG) Log.d(TAG, "Requesting: " + request.uri);
                    HttpGet httpGet = new HttpGet(request.uri.toString());
                    httpGet.addHeader("Accept-Encoding", "gzip");
                    HttpResponse response = mHttpClient.execute(httpGet);
                    String mimeType = response.getHeaders("Content-Type")[0].getValue();
                    if (DEBUG) Log.d(TAG, "mimeType:" + mimeType);
                    if (mimeType.startsWith("image")) {
                        HttpEntity entity = response.getEntity();
                        is = getUngzippedContent(entity);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        if (mResourceCache.store(request.hash, bitmap)) {
                            mCache.put(request.uri.toString(), new SoftReference<Bitmap>(bitmap));
                            if (DEBUG) Log.d(TAG, "Request successful: " + request.uri);
                        } else {
                            mResourceCache.invalidate(request.hash);
                        }
                    }
                } catch (IOException e) {
                    if (DEBUG) Log.d(TAG, "IOException", e);
                } finally {
                    if (DEBUG) Log.e(TAG, "Request finished: " + request.uri);
                    mActiveRequestsMap.remove(request);
                    if (is != null) {
                        notifyObservers(request.uri);
                    }
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        if (DEBUG) e.printStackTrace();
                    }
                }
                return request;
            }
        };
    }

    /**
     * Gets the input stream from a response entity. If the entity is gzipped
     * then this will get a stream over the uncompressed data.
     * 
     * @param entity
     *            the entity whose content should be read
     * @return the input stream to read from
     * @throws IOException
     */
    public static InputStream getUngzippedContent(HttpEntity entity) throws IOException {
        InputStream responseStream = entity.getContent();
        if (responseStream == null) {
            return responseStream;
        }
        Header header = entity.getContentEncoding();
        if (header == null) {
            return responseStream;
        }
        String contentEncoding = header.getValue();
        if (contentEncoding == null) {
            return responseStream;
        }
        if (contentEncoding.contains("gzip")) {
            responseStream = new GZIPInputStream(responseStream);
        }
        return responseStream;
    }

    /**
     * Create a thread-safe client. This client does not do redirecting, to
     * allow us to capture correct "error" codes.
     * 
     * @return HttpClient
     */
    public static final DefaultHttpClient createHttpClient() {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        HttpConnectionParams.setConnectionTimeout(params, 10 * 1000);
        HttpConnectionParams.setSoTimeout(params, 10 * 1000);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        final SchemeRegistry supportedSchemes = new SchemeRegistry();
        final SocketFactory sf = PlainSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("http", sf, 80));
        final ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, supportedSchemes);
        return new DefaultHttpClient(ccm, params);
    }

    private static class Request {

        Uri uri;

        String hash;

        public Request(Uri requestUri, String requestHash) {
            uri = requestUri;
            hash = requestHash;
        }

        @Override
        public int hashCode() {
            return hash.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Request other = (Request) obj;
            if (hash == null) {
                if (other.hash != null) return false;
            } else if (!hash.equals(other.hash)) return false;
            return true;
        }
    }
}
