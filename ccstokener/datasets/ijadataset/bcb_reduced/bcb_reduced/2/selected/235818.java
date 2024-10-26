package net.simpleframework.util;

import java.io.IOException;
import java.net.URL;
import org.apache.commons.collections.map.LRUMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class IPAndCity {

    private static LRUMap ips = new LRUMap(1000);

    public static String getCity(final String ip, final boolean more) throws IOException {
        if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
            return "";
        }
        String city = (String) ips.get(ip);
        if (city != null) {
            if (!more) {
                final int e = city.indexOf(" ");
                if (e > 0) {
                    return city.substring(0, e);
                }
            }
            return city;
        }
        try {
            final URL url = new URL("http://www.cz88.net/ip/?ip=" + ip);
            final String htmlString = IoUtils.getStringFromInputStream(url.openStream(), "gbk");
            final Element first = Jsoup.parse(htmlString).select("#InputIPAddrMessage").first();
            if (first != null) {
                ips.put(ip, city = first.text());
                return city;
            }
        } catch (final IOException e) {
        }
        return "";
    }
}
