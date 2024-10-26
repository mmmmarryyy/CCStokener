package com.volantis.map.ics.imageprocessor.parameters.impl;

import com.volantis.map.common.param.MissingParameterException;
import com.volantis.map.common.param.MutableParameters;
import com.volantis.map.common.param.ParameterNames;
import com.volantis.map.common.param.ParameterBuilderException;
import com.volantis.map.common.streams.SeekableInputStream;
import com.volantis.map.localization.LocalizationFactory;
import com.volantis.map.operation.ResourceDescriptor;
import com.volantis.map.retriever.*;
import com.volantis.map.retriever.http.MutableHttpHeaders;
import com.volantis.shared.net.url.http.CachedHttpContentState;
import com.volantis.shared.net.url.http.CachedHttpContentStateBuilder;
import com.volantis.shared.net.url.http.HttpResponseHeaderAccessor;
import com.volantis.shared.time.DateFormats;
import com.volantis.shared.time.Time;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.synergetics.mime.DefaultMimeDiscoverer;
import com.volantis.synergetics.mime.MimeDiscoverer;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.*;

/**
 * This param builder is used to load input image and set a number of
 * parameters concerned loaded image into parameter container. Almost all the
 * loading stuff is inherited form the old implementation of the ICS.
 *
 * The following parameters are set:
 *
 * Name			Type	            Description ImageInputStream     InputStream	    -
 * ImageInputStream for input image. InputImageSize	Integer             - Input
 * image size in bytes. ImageBytes
 * image. InputImageMIMEType	String 		    - Input image MIME type.
 */
public class ImageLoader {

    /**
     * Used for logging.
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(ImageLoader.class);

    /**
     * The mime type discoverer.
     */
    private static final MimeDiscoverer DISCOVERER = new DefaultMimeDiscoverer();

    /**
     * The Cache-Control: max-age value to be used for local files. This value
     * is in seconds.
     */
    private static final int LOCAL_FILE_MAX_AGE = 10000;

    /**
     * The headers to ignore when copying or processing incoming requests.
     */
    private static final String[] headersToIgnore = new String[] { "accept", "host", "range", "if-range", "accept-encoding", "if-modified-since" };

    private ImageLoader() {
    }

    /**
     * Load the image specified in the
     * {@link com.volantis.map.common.param.ParameterNames#SOURCE_URL}
     * parameter in the descriptor using the spcecified retriever
     * @param srcURL  the url of the src image
     *@param retriever
     * @param request the servlet request from which the headers should be
 * copied. May be null.
     * @param descriptor @return
     * @throws com.volantis.map.common.param.ParameterBuilderException
     * @throws ResourceRetrieverException
     * @throws MissingParameterException
     */
    public static Representation load(String srcURL, ResourceRetriever retriever, HttpServletRequest request, ResourceDescriptor descriptor) throws ParameterBuilderException, ResourceRetrieverException, MissingParameterException {
        Representation result;
        MutableParameters params = descriptor.getInputParameters();
        try {
            if (!srcURL.startsWith("http:")) {
                result = getLocalImage(srcURL);
            } else {
                result = getRemoteImage(srcURL, retriever, request);
            }
            if (result == null) {
                throw new ParameterBuilderException("Can't load input image");
            }
            String type = result.getFileType();
            params.setParameterValue(ParameterNames.SOURCE_IMAGE_MIME_TYPE, type);
        } catch (IOException ioe) {
            throw new ResourceRetrieverException(ioe);
        }
        return result;
    }

    /**
     * Get an image that lives on a different servlet via the proxy.
     *
     * @param srcURL the source url
     * @param request from which the remote connection will be obtained.
     * Can be null
     *
     * @throws java.io.IOException
     */
    private static Representation getRemoteImage(String srcURL, ResourceRetriever retriever, HttpServletRequest request) throws IOException, MissingParameterException, ResourceRetrieverException {
        MutableHttpHeaders headers = retriever.createMutableHeaders();
        copyHeaders(request, headers);
        return retriever.execute(new URL(srcURL), headers);
    }

    /**
     * Checks whether a given header should be ignored or not, based on values
     * in {@link #headersToIgnore}.
     *
     * @param header The header to check.
     * @return True if the header should be ignored, false otherwise.
     */
    private static boolean shouldBeIgnored(String header) {
        boolean ignore = false;
        int limit = headersToIgnore.length;
        int i = 0;
        while (i < limit && !ignore) {
            if (headersToIgnore[i].equalsIgnoreCase(header)) {
                ignore = true;
            }
            i++;
        }
        return ignore;
    }

    /**
     * Copy the headers from the existing request to the proxy request method.
     * This will not copy certain headers.  Specifically: <ul> <li>accept</li>
     * <li>host</li> <li>range</li> </ul> This is because the servlet sets
     * the accept and host headers with values that it requires.  Additionally
     * the use of the range header can cause incomplete images to be returned
     * and should not be used.
     *
     * @param request The original request containing headers to be copied. Can
     * be null.
     * @param headers The headers object to which appropriate headers should be
     *                copied.
     */
    protected static void copyHeaders(HttpServletRequest request, MutableHttpHeaders headers) {
        if (null != request) {
            Enumeration hdEnum = request.getHeaderNames();
            if (null != hdEnum) {
                while (hdEnum.hasMoreElements()) {
                    String hdr = (String) hdEnum.nextElement();
                    if (!shouldBeIgnored(hdr)) {
                        final String value = request.getHeader(hdr);
                        headers.addHeader(hdr, value);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Sending header: " + hdr + "=" + value);
                        }
                    }
                }
            }
        }
    }

    /**
     * Get an image from a local file.
     *
     * @param sourceURL The file url from which to obtain the source
     * @return ImageInputStream holding the image data.
     *
     * @throws IOException if image creation failed.
     */
    private static Representation getLocalImage(String sourceURL) throws IOException {
        URL url = new URL(sourceURL);
        final SeekableInputStream sis;
        if (url.getProtocol().equals("file")) {
            sis = new DefaultSeekableInputStream(new File(url.getFile()));
        } else {
            sis = new DefaultSeekableInputStream(null, url.openStream(), false);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Loading local file: " + url);
        }
        final String mimeType = DISCOVERER.discoverMimeType(sis);
        CachedHttpContentStateBuilder builder = new CachedHttpContentStateBuilder();
        final Map responses = new HashMap();
        Date now = new Date();
        DateFormat format = DateFormats.RFC_1123_GMT.create();
        responses.put("date", format.format(now));
        responses.put("cache-control", "max-age=" + LOCAL_FILE_MAX_AGE);
        responses.put("expires", format.format(new Date(now.getTime() + LOCAL_FILE_MAX_AGE * 1000)));
        HttpResponseHeaderAccessor accessor = new HttpResponseHeaderAccessor() {

            public String getProtocol() {
                return "HTTP";
            }

            public String getResponseHeaderValue(String s) {
                s = s.toLowerCase(Locale.ENGLISH);
                return (String) responses.get(s);
            }
        };
        builder.setMethodAccessor(accessor);
        Time t = Time.inMilliSeconds(System.currentTimeMillis());
        builder.setRequestTime(t);
        builder.setResponseTime(t);
        final CachedHttpContentState state = builder.build();
        return new Representation() {

            public void close() {
                try {
                    sis.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public CachedHttpContentState getCacheInfo() {
                return state;
            }

            public String getFileType() {
                return mimeType;
            }

            public SeekableInputStream getSeekableInputStream() {
                return sis;
            }
        };
    }
}
