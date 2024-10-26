package org.jrecruiter.service.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.jrecruiter.common.ApiKeysHolder;
import org.jrecruiter.common.CollectionUtils;
import org.jrecruiter.common.GoogleMapsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gunnar Hillert
 * @version $Id: DataServiceImpl.java 605 2010-08-31 05:31:30Z ghillert $
 */
@Service("dataService")
@Transactional
public class DataServiceImpl implements org.jrecruiter.service.DataService {

    /**
     * Logger Declaration.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DataServiceImpl.class);

    @Autowired
    private ApiKeysHolder apiKeysHolder;

    /** {@inheritDoc} */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Image getGoogleMapImage(final BigDecimal latitude, final BigDecimal longitude, final Integer zoomLevel) {
        if (longitude == null) {
            throw new IllegalArgumentException("Longitude cannot be null.");
        }
        if (latitude == null) {
            throw new IllegalArgumentException("Latitude cannot be null.");
        }
        if (zoomLevel == null) {
            throw new IllegalArgumentException("ZoomLevel cannot be null.");
        }
        final URI url = GoogleMapsUtils.buildGoogleMapsStaticUrl(latitude, longitude, zoomLevel);
        BufferedImage img;
        try {
            URLConnection conn = url.toURL().openConnection();
            img = ImageIO.read(conn.getInputStream());
        } catch (UnknownHostException e) {
            LOGGER.error("Google static MAPS web service is not reachable (UnknownHostException).", e);
            img = new BufferedImage(GoogleMapsUtils.defaultWidth, 100, BufferedImage.TYPE_INT_RGB);
            final Graphics2D graphics = img.createGraphics();
            final Map<Object, Object> renderingHints = CollectionUtils.getHashMap();
            renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.addRenderingHints(renderingHints);
            graphics.setBackground(Color.WHITE);
            graphics.setColor(Color.GRAY);
            graphics.clearRect(0, 0, GoogleMapsUtils.defaultWidth, 100);
            graphics.drawString("Not Available", 30, 30);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return img;
    }
}
