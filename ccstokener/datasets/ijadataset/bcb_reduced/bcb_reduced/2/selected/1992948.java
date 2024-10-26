package csiebug.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * @author George_Tsai
 * @version 2010/9/10
 */
public class PropertiesUtility {

    /**
	 * 從classPath load properties
	 * @param classPath
	 * @return
	 * @throws IOException
	 */
    public static Properties load(String classPath) throws IOException {
        AssertUtility.notNullAndNotSpace(classPath);
        Properties props = new Properties();
        URL url = ClassLoader.getSystemResource(classPath);
        props.load(url.openStream());
        return props;
    }

    /**
	 * 從檔案 load properties
	 * @param properties
	 * @return
	 * @throws IOException
	 */
    public static Properties load(File properties) throws IOException {
        AssertUtility.notNull(properties);
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(properties);
        try {
            props.load(fis);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return props;
    }

    /**
	 * 用prefix取得所有相關的properties(key值是去掉prefix的)
	 * @param props
	 * @param prefix
	 * @return
	 * @throws UnsupportedEncodingException
	 */
    public static Properties getSubProperties(Properties props, String prefix) throws UnsupportedEncodingException {
        Properties subProps = new Properties();
        Iterator<Entry<Object, Object>> iterator = props.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Object, Object> entry = iterator.next();
            if (entry.getKey().toString().startsWith(prefix)) {
                subProps.put(entry.getKey().toString().replace(prefix, ""), new String(entry.getValue().toString().getBytes("ISO-8859-1"), "UTF-8"));
            }
        }
        return subProps;
    }

    /**
	 * 將properties轉成Map物件
	 * @param props
	 * @return
	 */
    public static Map<String, String> toMap(Properties props) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        Iterator<Entry<Object, Object>> iterator = props.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Object, Object> entry = iterator.next();
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return map;
    }
}
