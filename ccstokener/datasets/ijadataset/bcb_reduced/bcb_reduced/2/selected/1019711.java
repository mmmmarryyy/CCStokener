package com.cooliris.media;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.*;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import com.cooliris.cache.CacheService;

public class UriTexture extends Texture {

    public static final int MAX_RESOLUTION = 1024;

    private static final String TAG = "UriTexture";

    protected String mUri;

    protected long mCacheId;

    private static final int MAX_RESOLUTION_A = MAX_RESOLUTION;

    private static final int MAX_RESOLUTION_B = MAX_RESOLUTION;

    public static final String URI_CACHE = CacheService.getCachePath("hires-image-cache");

    private static final String USER_AGENT = "Cooliris-ImageDownload";

    private static final int CONNECTION_TIMEOUT = 20000;

    public static final HttpParams HTTP_PARAMS;

    public static final SchemeRegistry SCHEME_REGISTRY;

    static {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);
        HttpClientParams.setRedirecting(params, true);
        HttpProtocolParams.setUserAgent(params, USER_AGENT);
        HTTP_PARAMS = params;
        SCHEME_REGISTRY = new SchemeRegistry();
        SCHEME_REGISTRY.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    }

    private SingleClientConnManager mConnectionManager;

    static {
        File uri_cache = new File(URI_CACHE);
        uri_cache.mkdirs();
    }

    public UriTexture(String imageUri) {
        mUri = imageUri;
    }

    public void setCacheId(long id) {
        mCacheId = id;
    }

    private static int computeSampleSize(InputStream stream, int maxResolutionX, int maxResolutionY) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, options);
        int maxNumOfPixels = maxResolutionX * maxResolutionY;
        int minSideLength = Math.min(maxResolutionX, maxResolutionY) / 2;
        return Utils.computeSampleSize(options, minSideLength, maxNumOfPixels);
    }

    public static final Bitmap createFromUri(Context context, String uri, int maxResolutionX, int maxResolutionY, long cacheId, ClientConnectionManager connectionManager) throws IOException, URISyntaxException, OutOfMemoryError {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;
        long crc64 = 0;
        Bitmap bitmap = null;
        if (uri.startsWith(ContentResolver.SCHEME_CONTENT)) {
            crc64 = cacheId;
        } else {
            crc64 = Utils.Crc64Long(uri);
        }
        bitmap = createFromCache(crc64, maxResolutionX);
        if (bitmap != null) {
            return bitmap;
        }
        final boolean local = uri.startsWith(ContentResolver.SCHEME_CONTENT) || uri.startsWith("file://");
        BufferedInputStream bufferedInput = null;
        if (uri.startsWith(ContentResolver.SCHEME_CONTENT) || uri.startsWith(ContentResolver.SCHEME_FILE)) {
            bufferedInput = new BufferedInputStream(context.getContentResolver().openInputStream(Uri.parse(uri)), 16384);
        } else {
            bufferedInput = createInputStreamFromRemoteUrl(uri, connectionManager);
        }
        if (bufferedInput != null) {
            options.inSampleSize = computeSampleSize(bufferedInput, maxResolutionX, maxResolutionY);
        } else {
            return null;
        }
        bufferedInput = null;
        if (uri.startsWith(ContentResolver.SCHEME_CONTENT) || uri.startsWith(ContentResolver.SCHEME_FILE)) {
            bufferedInput = new BufferedInputStream(context.getContentResolver().openInputStream(Uri.parse(uri)), 16384);
        } else {
            bufferedInput = createInputStreamFromRemoteUrl(uri, connectionManager);
        }
        if (bufferedInput != null) {
            options.inDither = false;
            options.inJustDecodeBounds = false;
            Thread timeoutThread = new Thread("BitmapTimeoutThread") {

                public void run() {
                    try {
                        Thread.sleep(6000);
                        options.requestCancelDecode();
                    } catch (InterruptedException e) {
                    }
                }
            };
            timeoutThread.start();
            bitmap = BitmapFactory.decodeStream(bufferedInput, null, options);
        }
        if ((options.inSampleSize > 1 || !local) && bitmap != null) {
            writeToCache(crc64, bitmap, maxResolutionX / options.inSampleSize);
        }
        return bitmap;
    }

    private static final BufferedInputStream createInputStreamFromRemoteUrl(String uri, ClientConnectionManager connectionManager) {
        InputStream contentInput = null;
        if (connectionManager == null) {
            try {
                URL url = new URI(uri).toURL();
                URLConnection conn = url.openConnection();
                conn.connect();
                contentInput = conn.getInputStream();
            } catch (Exception e) {
                Log.w(TAG, "Request failed: " + uri);
                e.printStackTrace();
                return null;
            }
        } else {
            final DefaultHttpClient mHttpClient = new DefaultHttpClient(connectionManager, HTTP_PARAMS);
            HttpUriRequest request = new HttpGet(uri);
            HttpResponse httpResponse = null;
            try {
                httpResponse = mHttpClient.execute(request);
                HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    contentInput = entity.getContent();
                }
            } catch (Exception e) {
                Log.w(TAG, "Request failed: " + request.getURI());
                return null;
            }
        }
        if (contentInput != null) {
            return new BufferedInputStream(contentInput, 4096);
        } else {
            return null;
        }
    }

    @Override
    protected Bitmap load(RenderView view) {
        Bitmap bitmap = null;
        if (mUri == null) return bitmap;
        try {
            if (mUri.startsWith("http://")) {
                if (!isCached(Utils.Crc64Long(mUri), MAX_RESOLUTION_A)) {
                    mConnectionManager = new SingleClientConnManager(HTTP_PARAMS, SCHEME_REGISTRY);
                }
            }
            bitmap = createFromUri(view.getContext(), mUri, MAX_RESOLUTION_A, MAX_RESOLUTION_B, mCacheId, mConnectionManager);
        } catch (Exception e2) {
            Log.e(TAG, "Unable to load image from URI " + mUri);
            e2.printStackTrace();
        }
        return bitmap;
    }

    public static final String createFilePathFromCrc64(long crc64, int maxResolution) {
        return URI_CACHE + crc64 + "_" + maxResolution + ".cache";
    }

    public static boolean isCached(long crc64, int maxResolution) {
        String file = null;
        if (crc64 != 0) {
            file = createFilePathFromCrc64(crc64, maxResolution);
            try {
                new FileInputStream(file);
                return true;
            } catch (FileNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    public static Bitmap createFromCache(long crc64, int maxResolution) {
        try {
            String file = null;
            Bitmap bitmap = null;
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inDither = true;
            if (crc64 != 0) {
                file = createFilePathFromCrc64(crc64, maxResolution);
                bitmap = BitmapFactory.decodeFile(file, options);
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    public static String writeHttpDataInDirectory(Context context, String uri, String path) {
        long crc64 = Utils.Crc64Long(uri);
        if (!isCached(crc64, 1024)) {
            Bitmap bitmap;
            try {
                bitmap = UriTexture.createFromUri(context, uri, 1024, 1024, crc64, null);
            } catch (OutOfMemoryError e) {
                return null;
            } catch (IOException e) {
                return null;
            } catch (URISyntaxException e) {
                return null;
            }
            bitmap.recycle();
        }
        String fileString = createFilePathFromCrc64(crc64, 1024);
        try {
            File file = new File(fileString);
            if (file.exists()) {
                String newPath = path + (path.endsWith("/") ? "" : "/") + crc64 + ".jpg";
                File newFile = new File(newPath);
                Utils.Copy(file, newFile);
                return newPath;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void writeToCache(long crc64, Bitmap bitmap, int maxResolution) {
        String file = createFilePathFromCrc64(crc64, maxResolution);
        if (bitmap != null && file != null && crc64 != 0) {
            try {
                File fileC = new File(file);
                fileC.createNewFile();
                final FileOutputStream fos = new FileOutputStream(fileC);
                final BufferedOutputStream bos = new BufferedOutputStream(fos, 16384);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                bos.flush();
                bos.close();
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    public static void invalidateCache(long crc64, int maxResolution) {
        String file = createFilePathFromCrc64(crc64, maxResolution);
        if (file != null && crc64 != 0) {
            try {
                File fileC = new File(file);
                fileC.delete();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void finalize() {
        if (mConnectionManager != null) {
            mConnectionManager.shutdown();
        }
    }
}
