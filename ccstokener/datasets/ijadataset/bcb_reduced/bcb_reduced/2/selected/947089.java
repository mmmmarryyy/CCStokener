package com.timezonepatch.factory;

import com.timezonepatch.model.TimeZonePatchList;
import com.wutka.jox.*;
import java.net.URL;

/**
 * A Factory of TimeZonePatchList.
 * This factory implementation makes use of JOX library
 * which is able to build a TimeZonePatchList
 * from a defined timezonepatch.xml file
 * @see TimeZonePatchList
 * @author Paulo Caroli
 */
public class XMLTimeZonePatchListFactory extends TimeZonePatchListFactory {

    private static final String XML_FILE = "timezonepatch.xml";

    public static TimeZonePatchList getTimeZonePatchList() throws TimeZonePatchListFactoryException {
        JOXBeanInputStream joxIn = null;
        ;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            URL url = cl.getResource(XML_FILE);
            joxIn = new JOXBeanInputStream(url.openStream());
            TimeZonePatchList joxTimeZonePatchList = (TimeZonePatchList) joxIn.readObject(TimeZonePatchList.class);
            joxTimeZonePatchList.valid();
            return joxTimeZonePatchList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new TimeZonePatchListFactoryException("not able to read xml file properly. " + e.getMessage());
        } finally {
            try {
                joxIn.close();
            } catch (Exception e) {
            }
        }
    }
}
