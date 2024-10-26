package com.frameworkset.common.tag.tree;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.frameworkset.common.tag.tree.itf.ITree;

/**
 * @author biaoping.yin
 * created on 2005-3-13
 * version 1.0
 */
public class TreeFactory implements Serializable {

    private static final Logger log = Logger.getLogger(TreeFactory.class);

    private static HashMap map;

    static {
        ClassLoader classLoader = TreeFactory.class.getClassLoader();
        try {
            URL url = classLoader.getResource("treedata.properties");
            Properties treeProps = new Properties();
            if (map == null) map = new HashMap();
            if (url != null) {
                InputStream is = url.openStream();
                treeProps.load(is);
                is.close();
                Enumeration keys = treeProps.keys();
                while (keys != null && keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    String className = treeProps.getProperty(key);
                    map.put(key.trim(), className.trim());
                }
            }
        } catch (Exception e) {
            log.info("not found resources file treedata.properties");
        }
    }

    public static ITree getTreeData(String type) {
        ITree ret = null;
        type = type.trim();
        try {
            ret = (ITree) Class.forName(type).newInstance();
            return ret;
        } catch (Exception e) {
            log.info("class " + type + " not found in class path,tree.properties will been checked!!!");
        }
        if (map == null) {
            return null;
        } else {
            Object obj = map.get(type);
            if (obj == null) {
                try {
                    ClassLoader classLoader = TreeFactory.class.getClassLoader();
                    URL url = classLoader.getResource("treedata.properties");
                    Properties treeProps = new Properties();
                    if (url != null) {
                        InputStream is = url.openStream();
                        treeProps.load(is);
                        is.close();
                        obj = treeProps.getProperty(type);
                        if (obj != null) {
                            map.put(type + "", obj);
                        }
                    } else {
                        log.info("not found resources file treedata.properties");
                        return null;
                    }
                } catch (Exception e) {
                    log.info("not found resources file treedata.properties");
                    return null;
                }
            }
            if (obj != null) {
                String className = ((String) obj).trim();
                try {
                    ret = (ITree) Class.forName(className).newInstance();
                } catch (Exception e) {
                    log.info("class " + className + " not found in class path!!!");
                    return null;
                }
            }
            if (ret == null) log.info("type " + type + " not exist please check treedata.properties");
            return ret;
        }
    }
}
