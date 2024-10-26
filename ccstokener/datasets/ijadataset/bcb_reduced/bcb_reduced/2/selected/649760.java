package rath.jmsn.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import rath.jmsn.MainFrame;
import rath.msnm.util.StringUtil;

/**
 *
 * @author Jangho Hwang, rath@linuxkorea.co.kr
 * @version 1.0.000, 2002/03/13
 */
public class Msg {

    static Properties current = null;

    static Properties usProp = null;

    static Locale currentLocale = null;

    static HashMap localMap = new HashMap();

    static {
        Locale korea = new Locale("ko", "KR", "");
        localMap.put(korea, getMessageBundle(Msg.class.getResource("/resources/text/message.properties.ko_KR"), korea));
        Locale koreaUTF8 = new Locale("ko", "KR", "UTF-8");
        localMap.put(koreaUTF8, getMessageBundle(Msg.class.getResource("/resources/text/message.properties.ko_KR.UTF-8"), koreaUTF8));
        Locale english = new Locale("en", "US", "");
        localMap.put(english, getMessageBundle(Msg.class.getResource("/resources/text/message.properties.en_US"), english));
        Locale fr = new Locale("fr", "FR", "");
        localMap.put(fr, getMessageBundle(Msg.class.getResource("/resources/text/message.properties.fr_FR"), fr));
        Locale it = new Locale("it", "IT", "");
        localMap.put(it, getMessageBundle(Msg.class.getResource("/resources/text/message.properties.it_IT"), it));
        Locale de = new Locale("de", "DE", "");
        localMap.put(de, getMessageBundle(Msg.class.getResource("/resources/text/message.properties.de_DE"), it));
        Locale def = Locale.getDefault();
        String encoding = System.getProperty("file.encoding").toLowerCase();
        if (encoding.equals("utf8") || encoding.equals("utf-8")) currentLocale = new Locale(def.getLanguage(), def.getCountry(), "UTF-8"); else currentLocale = new Locale(def.getLanguage(), def.getCountry());
        usProp = (Properties) localMap.get(english);
        Properties defProp = (Properties) localMap.get(currentLocale);
        if (defProp == null) {
            current = (Properties) localMap.get(english);
            currentLocale = english;
        } else current = defProp;
    }

    private static Properties getMessageBundle(URL url, Locale locale) {
        Properties prop = new Properties();
        String variant = locale.getVariant();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            InputStream in = url.openStream();
            int buf;
            while ((buf = in.read()) != -1) {
                if (buf == 10) {
                    byte[] b = bos.toByteArray();
                    bos.reset();
                    String line = null;
                    if (variant == null || variant.equals("")) line = new String(b); else line = new String(b, variant);
                    if (line.trim().length() == 0 || line.charAt(0) == '#') continue;
                    int i0 = line.indexOf('=');
                    if (i0 != -1) {
                        String key = line.substring(0, i0).trim();
                        String value = line.substring(i0 + 1).trim();
                        value = StringUtil.replaceString(value, "\\n", "\n");
                        prop.setProperty(key, value);
                    }
                } else bos.write(buf);
            }
            in.close();
            return prop;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * 지원되는 Locale을 반환한다. 대신 Country code가 같은 Locale들은 걸러진다.
	 * @return
	 */
    public static Set getAvailableLocales() {
        return localMap.keySet();
    }

    public static synchronized void setLocale(Locale locale) {
        Locale.setDefault(locale);
        Properties prop = (Properties) localMap.get(locale);
        currentLocale = locale;
        current = prop == null ? (Properties) localMap.get(new Locale("en", "US", "")) : prop;
        MainFrame.INSTANCE.updateUIAll();
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static String get(String key) {
        String msg = current.getProperty(key);
        if (msg == null) msg = usProp.getProperty(key);
        return msg;
    }

    public static String get(String key, String param) {
        return MessageFormat.format(get(key), new Object[] { param });
    }

    public static String get(String key, String param1, String param2) {
        return MessageFormat.format(get(key), new Object[] { param1, param2 });
    }

    public static String get(String key, Object[] params) {
        return MessageFormat.format(get(key), params);
    }
}
