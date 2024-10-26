package jblip.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Provides a restricted way of obtaining HTTP connections.
 * 
 * <p>
 * The class restricts access based on two parameters: <i>pool size</i> and
 * <i>timeout</i>. Upon a request to the {@link #getConnection(URL)} method,
 * it tries to obtain a connection from a limited pool of these. If it fails
 * to do so and the given timeout expires, it returns a non-pooled connection
 * instead.</p>
 * 
 * <p>Please note that the only way of increasing the number of available
 * connections is to call {@link #releaseConnection(HttpURLConnection)} when
 * the instance is not longer needed. If you fail to do so, you will eventually
 * need to wait for a new connection until the timeout occurs.
 * </p>
 * 
 * @author Krzysztof Sroka
 */
public class HttpConnectionProvider {

    private static final long DEFAULT_TIMEOUT_MILLIS = 3000L;

    private final Set<HttpURLConnection> pool;

    private final Semaphore pool_semaphore;

    private final long wait_timeout_millis;

    public HttpConnectionProvider(final int size) {
        this(size, DEFAULT_TIMEOUT_MILLIS);
    }

    public HttpConnectionProvider(final int size, final long timeout) {
        if (size <= 0) {
            throw new IllegalArgumentException("Pool size must be positive.");
        }
        if (timeout <= 0L) {
            throw new IllegalArgumentException("Timeout must be positive.");
        }
        this.pool_semaphore = new Semaphore(size, true);
        this.wait_timeout_millis = timeout;
        this.pool = Collections.synchronizedSet(new HashSet<HttpURLConnection>());
    }

    /**
   * Releases a slot occupied by the provided connection.
   * 
   * <p>Has no effect if the connection was not pooled</p>/
   * 
   * @param connection A connection to release slot for.
   */
    public void releaseConnection(HttpURLConnection connection) {
        if (pool.remove(connection)) {
            pool_semaphore.release();
        }
    }

    /**
   * Retrieves a new HTTP connection based on the provided URL.
   * 
   * <p>See the class comment for details on how this is done.</p>
   * 
   * @param url URL used in connection creation (needs to be a HTTP URL)
   * @return a connection to the provided URL
   * @throws IOException if opening a connection fails
   */
    public HttpURLConnection getConnection(URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            if (pool_semaphore.tryAcquire(wait_timeout_millis, TimeUnit.MILLISECONDS)) {
                pool.add(connection);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
