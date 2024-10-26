package com.objecteffects.clublist.geo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * I'm not sure why this is in a separate class; it could go inside
 * GeoCodeAddress.
 * 
 * @author Rusty Wright
 */
public final class GeoCodeFetch {

    private final transient Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String urlXmlPath = "http://maps.googleapis.com/maps/api/geocode/xml?sensor=false";

    /**
     * @param address
     * @return String result from geocoder
     * @throws EncoderException
     */
    public String fetch(final String address) throws EncoderException {
        final String escapedAddress = new URLCodec().encode(address);
        final String requestUrl = GeoCodeFetch.urlXmlPath + "&" + "address=" + escapedAddress;
        this.log.debug("requestUrl: {}", requestUrl);
        try {
            final StringBuffer sb = new StringBuffer();
            final URL url = new URL(requestUrl);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                this.log.debug("line: {}", line);
                sb.append(line);
            }
            reader.close();
            return (sb.toString());
        } catch (final MalformedURLException ex) {
            this.log.error(ExceptionUtils.getStackTrace(ex));
        } catch (final IOException ex) {
            this.log.error(ExceptionUtils.getStackTrace(ex));
        }
        return ("");
    }
}
