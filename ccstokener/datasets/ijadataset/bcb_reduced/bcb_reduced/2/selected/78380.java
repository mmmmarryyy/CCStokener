package bg.plambis.dict.local;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

public class LocalizationUtil {

    private static TextResource resources;

    private static Locale currentLocale;

    public static String getI18nText(String searchedText, Locale locale) {
        if (resources == null || currentLocale == null || !currentLocale.equals(locale)) {
            currentLocale = locale;
            loadResources(locale);
        }
        return resources == null ? searchedText : resources.getValue(searchedText);
    }

    public static boolean loadResources(Locale locale) {
        boolean result = false;
        if (locale.getLanguage() != null && locale.getCountry() != null) {
            result = loadResources("_" + locale.getLanguage() + "_" + locale.getCountry().toUpperCase());
        }
        if (locale.getLanguage() != null && !result) {
            result = loadResources("_" + locale.getLanguage());
        }
        if (!result) {
            result = loadResources("");
        }
        return result;
    }

    private static boolean loadResources(String ext) {
        InputStream in;
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("bg/plambis/dict/local/i18n" + ext + ".xml");
            if (url == null) return false;
            in = url.openStream();
        } catch (IOException e1) {
            e1.printStackTrace();
            return false;
        }
        try {
            Serializer serializer = new Persister();
            resources = serializer.read(TextResource.class, in);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
