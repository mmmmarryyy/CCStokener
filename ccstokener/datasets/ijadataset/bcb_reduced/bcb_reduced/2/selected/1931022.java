package org.mcisb.jws;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.mcisb.util.*;

/**
 * Reads a file generated from schema images that contains coordinates.
 * 
 * A Map of Shape objects to String (id) objects can be returned.
 * 
 * @author Neil Swainston
 */
public class CoordinateGrabber {

    /**
	 * 
	 */
    private static final int X = 0;

    /**
	 * 
	 */
    private static final int Y = 1;

    /**
	 * 
	 */
    private static final String INT_REGEX = "\\d*";

    /**
	 * 
	 */
    private static final String DOUBLE_REGEX = "\\d*\\.\\d*";

    /**
	 * 
	 */
    private static final String TEXT_REGEX = "(?<=text: \")[^\"]*";

    /**
	 * 
	 */
    private static final String NAME_REGEX = "(?<=name: \")[^\"]*";

    /**
	 * 
	 */
    private static final String ORIGIN_REGEX = "(?<=(origin: \\{))" + DOUBLE_REGEX + "\\, " + DOUBLE_REGEX;

    /**
	 * 
	 */
    private static final String SIZE_REGEX = "(?<=(size: \\{))" + DOUBLE_REGEX + "\\, " + DOUBLE_REGEX;

    /**
	 * 
	 */
    private static final String COLOUR_REGEX = "(?<=(gradient color: \\{))" + DOUBLE_REGEX + "\\, " + DOUBLE_REGEX + "\\, " + DOUBLE_REGEX;

    /**
	 * 
	 */
    private static final String TRUE_ORIGIN_REGEX = "(?<=(true, origin: \\{))" + DOUBLE_REGEX + "\\, " + DOUBLE_REGEX;

    /**
	 * 
	 */
    private static final String STOICHIOMETRY_COLOR = "0.666682, 0.666682, 0.666682";

    /**
	 * 
	 * @param url
	 * @return Map<Shape,String>
	 * @throws IOException
	 */
    public static Map<Shape, String> getShapeToId(final URL url) throws IOException {
        final String CIRCLE = "Circle";
        final Map<Shape, String> shapeToId = new LinkedHashMap<Shape, String>();
        BufferedReader reader = null;
        try {
            final Point offsetPoint = getOffsetPoint(url);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                final String id = (String) CollectionUtils.getFirst(RegularExpressionUtils.getMatches(line, TEXT_REGEX));
                final String shapeString = (String) CollectionUtils.getFirst(RegularExpressionUtils.getMatches(line, NAME_REGEX));
                final String origin = (String) CollectionUtils.getFirst(RegularExpressionUtils.getMatches(line, ORIGIN_REGEX));
                final String size = (String) CollectionUtils.getFirst(RegularExpressionUtils.getMatches(line, SIZE_REGEX));
                final String colour = (String) CollectionUtils.getFirst(RegularExpressionUtils.getMatches(line, COLOUR_REGEX));
                if (origin != null && size != null && id != null) {
                    if (id.matches(INT_REGEX) && colour.equals(STOICHIOMETRY_COLOR)) {
                        continue;
                    }
                    final Point originPoint = getPoint(origin);
                    originPoint.setLocation(originPoint.x - offsetPoint.x, originPoint.y - offsetPoint.y);
                    final Point sizePoint = getPoint(size);
                    Shape shape = null;
                    if (shapeString != null && shapeString.equals(CIRCLE)) {
                        shape = new Ellipse2D.Double(originPoint.x, originPoint.y, sizePoint.x, sizePoint.y);
                    } else {
                        shape = new Rectangle(originPoint.x, originPoint.y, sizePoint.x, sizePoint.y);
                    }
                    shapeToId.put(shape, id);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return shapeToId;
    }

    /**
	 * 
	 * @param url
	 * @return Point
	 * @throws IOException
	 */
    private static Point getOffsetPoint(final URL url) throws IOException {
        final int BORDER_HEIGHT = 2;
        final int BORDER_WIDTH = 3;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                final String origin = (String) CollectionUtils.getFirst(RegularExpressionUtils.getMatches(line, TRUE_ORIGIN_REGEX));
                if (origin != null) {
                    final Point offsetPoint = getPoint(origin);
                    offsetPoint.setLocation(offsetPoint.x - BORDER_WIDTH, offsetPoint.y - BORDER_HEIGHT);
                    return offsetPoint;
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return new Point();
    }

    /**
	 * 
	 * @param coordinates
	 * @return
	 */
    private static Point getPoint(final String coordinates) {
        final String SEPARATOR = ",";
        final String[] tokens = coordinates.split(SEPARATOR);
        return new Point(getValue(tokens[X]), getValue(tokens[Y]));
    }

    /**
	 * 
	 * @param token
	 * @return int
	 */
    private static int getValue(final String token) {
        return Long.valueOf(Math.round(Double.parseDouble(token))).intValue();
    }
}
