package com.blazinggames.urcl.data;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Loads a configuration text file storing the parameters into a hashtable.
 * While the parameters are all strings, the class provides get properties
 * methods that converts the input into different types of data while allowing
 * you to provide default values for properties that haven't been defined
 * in the config file.
 * 
 * @author Billy D. Spelchan
 * @version OSR1.00
 */
public class Config {

    public static final int TEST_CONFIG = 0;

    public static final int DATA_CONFIG = 1;

    private static final char COMMAND_SEPARATOR = '=';

    private static final char PARAM_SEPARATOR = ',';

    protected Hashtable _properties;

    protected char _command_separator = COMMAND_SEPARATOR;

    protected char _param_separator = PARAM_SEPARATOR;

    private int _mode = TEST_CONFIG;

    /**
     * Default constructor
    */
    public Config() {
        _properties = new Hashtable();
    }

    /**
     * Format specification constructor. This lets you control the delimiter
     * that separates commands from their value and the delimiter that separates
     * individual parameter components from each other.
     *
     * @param cmd delimiter that separates command from property
     * @param param delimiter that separates individual parameters from each other
    */
    public Config(char cmd, char param) {
        this();
        _command_separator = cmd;
        _param_separator = param;
    }

    /**
     * reads the configuration properties from the indicated file url.
     *
     * @param url the URL to read the file from
     */
    public final void loadConfigURL(URL url) throws Exception {
        int index;
        String prop, param;
        DataInputStream dis = new DataInputStream(url.openStream());
        String s = dis.readLine();
        if (s.startsWith("TEST")) {
            _mode = TEST_CONFIG;
        } else {
            _mode = DATA_CONFIG;
        }
        s = null;
        while ((s = dis.readLine()) != null) {
            if (s.length() > 2) {
                index = s.indexOf(_command_separator);
                if (index > 0) {
                    prop = s.substring(0, index);
                    param = s.substring(index + 1);
                    addProperty(prop, param);
                }
            }
        }
    }

    /**
     * adds a property to the property list
     *
     * @param prop property to add to the list of properties
     * @param param value of property (may be list of values)
    */
    public void addProperty(String prop, String param) {
        _properties.put(prop, param);
    }

    /**
     * Removes the indicated property from the property list
     *
     * @param prop property to remove
     */
    public void removeProperty(String prop) {
        _properties.remove(prop);
    }

    /**
     * converts a string into an array of strings, each string being
     * delimited by the indicated character.
     *
     * @param s string to parse
     * @param ch delimiter character
     * @return string broken into an array of strings 
     */
    public String[] parseString(String s, char ch) {
        int cntr, count = 0;
        int len = s.length();
        char sc;
        for (cntr = 0; cntr < len; ++cntr) {
            sc = s.charAt(cntr);
            if (sc == ch) ++count;
        }
        String rs[] = new String[count + 1];
        int start = 0;
        int last = 0;
        for (cntr = 0; cntr < count; ++cntr) {
            last = s.indexOf(ch, start);
            rs[cntr] = s.substring(start, last);
            start = last + 1;
        }
        rs[count] = s.substring(start);
        return rs;
    }

    /**
     * Converts a string into an integer making sure it is a valid
     * number. If it is not valid returns the provided default value.
     *
     * @param s string to be converted into an integer
     * @param d default value to use if number invalid
     * @return number, or default if value invalid
     */
    public int parseInt(String s, int d) {
        int r;
        try {
            r = Integer.parseInt(s);
        } catch (Exception e) {
            r = d;
        }
        return r;
    }

    /**
     * Gets a property string and returns it as a color.
     * Format of property should be red,green,blue
     * where the color componentss are integers between 0-255.
     *
     * @param key property that holds color info
     * @param d default to use if property is invalid or does not exist
     * @return color using default if property invalid
    */
    public Color getColorProperty(String key, Color d) {
        String temp = (String) _properties.get(key);
        if (temp == null) return d;
        String params[] = parseString(temp, _param_separator);
        if (params.length != 3) return d; else return new Color(parseInt(params[0], 0), parseInt(params[1], 0), parseInt(params[2], 0));
    }

    /**
     * Gets a property string and returns it as a font.
     * Format of property should be family,style,size
     *
     * @param key property that holds font info
     * @param d default to use if property is invalid or does not exist
     * @return font using default if property invalid
     */
    public Font getFontProperty(String key, Font d) {
        String temp = (String) _properties.get(key);
        if (temp == null) return d;
        String params[] = parseString(temp, _param_separator);
        if (params.length != 3) return d; else return new Font(params[0], parseInt(params[1], 0), parseInt(params[2], 12));
    }

    /**
     * Gets a property string and returns it as an integer.
     *
     * @param key property that holds integer
     * @param d default to use if property is invalid or does not exist
     * @return integer value using default if property invalid
     */
    public int getIntProperty(String key, int d) {
        String temp = (String) _properties.get(key);
        if (temp == null) return d; else return parseInt(temp, d);
    }

    /**
     * Gets a property string and returns it as a point.
     * Format of property should be x,y
     *
     * @param key property that holds point info
     * @param d default to use if property is invalid or does not exist
     * @return point using default if property invalid
     */
    public Point getPointProperty(String key, Point d) {
        String temp = (String) _properties.get(key);
        if (temp == null) return d;
        String params[] = parseString(temp, _param_separator);
        if (params.length != 2) return d; else return new Point(parseInt(params[0], 0), parseInt(params[1], 0));
    }

    /**
     * Gets a property string and returns it as a rectangle.
     * Format of property should be x,y,width,height
     *
     * @param key property that holds rectangle info
     * @param d default to use if property is invalid or does not exist
     * @return rectangle using default if property invalid
     */
    public Rectangle getRectangleProperty(String key, Rectangle d) {
        String temp = (String) _properties.get(key);
        if (temp == null) return d;
        String params[] = parseString(temp, _param_separator);
        if (params.length != 4) return d; else return new Rectangle(parseInt(params[0], 0), parseInt(params[1], 0), parseInt(params[2], 0), parseInt(params[3], 0));
    }

    /**
     * Gets a property string
     *
     * @param key property to retrieve
     * @param d default to use if property is invalid or does not exist
     * @return property value as string
     */
    public String getStringProperty(String key, String d) {
        String r = (String) _properties.get(key);
        if (r == null) r = d;
        return r;
    }

    /**
     * returns config mode, wich is either TEST_CONFIG or DATA_CONFIG.
     *
     * @return config mode, wich is either TEST_CONFIG or DATA_CONFIG.
     */
    public int getMode() {
        return _mode;
    }
}
