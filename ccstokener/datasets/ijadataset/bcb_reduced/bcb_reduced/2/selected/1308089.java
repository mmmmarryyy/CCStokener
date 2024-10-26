package com.jcompressor.utils;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.jcompressor.exceptions.JcompressorException;

/**
 * @author Scott Carnett
 */
public class JcompressorUtils {

    /**
	 * Gets the configuration path
	 * @param file the configuration file
	 * @return String with the the configuration path
	 */
    public static String getConfigurationPath(final String file) {
        final URL url = JcompressorUtils.getConfigurationUrl(file);
        File config;
        try {
            config = new File(url.toURI());
        } catch (URISyntaxException e) {
            config = new File(url.getPath());
        }
        if (config.exists()) {
            return config.getPath();
        }
        throw new JcompressorException("Configuration not found");
    }

    /**
	 * Gets the configuration url
	 * @param file the configuration file
	 * @return URL with the configuration url
	 */
    public static URL getConfigurationUrl(final String file) {
        try {
            final URL url = JcompressorUtils.getResourceLoader().getResource(file);
            if (url != null) {
                return url;
            }
            return null;
        } catch (Exception e) {
            throw new JcompressorException("Configuration not found", e);
        }
    }

    /**
	 * Opens a url stream
	 * @param url the url
	 * @return {@link InputStream} with the url stream
	 */
    public static InputStream openStream(final URL url) {
        try {
            final URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (Exception e) {
        }
        return null;
    }

    /**
	 * Executes a thread
	 * @param runnable the runnable
	 * @see com.jcompressor.utils.JcompressorUtils#execute(Runnable[])
	 */
    public static void execute(final Runnable runnable) {
        JcompressorUtils.execute(new Runnable[] { runnable });
    }

    /**
	 * Executes some threads
	 * @param runnables the runnables
	 * @see com.jcompressor.utils.JcompressorUtils#execute(Runnable)
	 */
    public static void execute(final Runnable[] runnables) {
        ExecutorService executor = null;
        try {
            executor = Executors.newFixedThreadPool(runnables.length);
            for (final Runnable runnable : runnables) {
                final Future<?> process = executor.submit(runnable);
                if (process != null) {
                    process.get();
                }
            }
        } catch (InterruptedException e) {
            throw new JcompressorException("Compression thread was interrupted", e);
        } catch (ExecutionException e) {
            throw new JcompressorException("Computation error", e);
        } finally {
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    /**
	 * Gets the resource loader
	 * @return {@link ClassLoader} with the resource loader
	 */
    public static ClassLoader getResourceLoader() {
        final ClassLoader resourceLoader = Thread.currentThread().getContextClassLoader();
        if (resourceLoader == null) {
            throw new JcompressorException("Resource Loader is null");
        }
        return resourceLoader;
    }
}
