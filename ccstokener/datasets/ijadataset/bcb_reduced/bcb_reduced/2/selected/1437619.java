package http;

import java.io.InputStream;
import java.net.URL;
import java.util.*;
import net.htmlparser.jericho.Source;

public class EbayUtil {

    public static final String KEYWORD = "keyword";

    public static final String URL = "url";

    public static final Integer SIZE = 20;

    public static String search(Map params) {
        String keyword = (String) params.get(KEYWORD);
        String urlFull = URL + "&keywords=" + keyword;
        InputStream input = null;
        try {
            input = new URL(urlFull).openStream();
        } finally {
            if (input != null) input.close();
        }
        return null;
    }
}
