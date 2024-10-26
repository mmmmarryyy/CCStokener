package com.frameworkset.common.tag.pager;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * @author biaoping.yin
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ListDataInfoFactory {

    private static final Logger log = Logger.getLogger(ListDataInfoFactory.class);

    /**
	 * Describe what the  constructor does
	 * 
	 * 2004-6-22
	 * 
	 */
    private static HashMap map;

    static {
        ClassLoader classLoader = ListDataInfoFactory.class.getClassLoader();
        try {
            URL url = classLoader.getResource("listdata.properties");
            Properties listProps = new Properties();
            if (map == null) map = new HashMap();
            if (url != null) {
                InputStream is = url.openStream();
                listProps.load(is);
                is.close();
                Enumeration keys = listProps.keys();
                while (keys != null && keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    String className = listProps.getProperty(key);
                    map.put(key.trim(), className.trim());
                }
            }
        } catch (Exception e) {
            log.info("not found resources file listdata.properties");
        }
    }

    public static DataInfo getDataInfo(String type) {
        DataInfo data = null;
        type = type.trim();
        try {
            data = (DataInfo) Class.forName(type).newInstance();
            return data;
        } catch (Exception e) {
            log.info("class " + type + " not found in class path!!!");
        }
        if (map == null) {
            return null;
        } else {
            Object obj = map.get(type);
            if (obj == null) {
                try {
                    ClassLoader classLoader = ListDataInfoFactory.class.getClassLoader();
                    URL url = classLoader.getResource("listdata.properties");
                    Properties listProps = new Properties();
                    if (url != null) {
                        InputStream is = url.openStream();
                        listProps.load(is);
                        is.close();
                        obj = listProps.getProperty(type);
                        if (obj != null) {
                            map.put(type, obj);
                        }
                    } else {
                        log.info("not found resources file listdata.properties");
                        return null;
                    }
                } catch (Exception e) {
                    log.info("not found resources file listdata.properties");
                    return null;
                }
            }
            if (obj != null) {
                String className = ((String) obj).trim();
                try {
                    data = (DataInfo) Class.forName(className).newInstance();
                } catch (Exception e) {
                    log.info("class " + className + " not found in class path!!!");
                    return null;
                }
            }
            if (data == null) log.info("type " + type + " not exist please check listdata.properties");
            return data;
        }
    }
}
