package net.sf.mustang.geoloc;

import java.net.URL;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

public class HostIp {

    public Location getLocation(String ip) throws Exception {
        URL url = new URL("http://api.hostip.info/?ip=" + ip);
        SAXReader reader = new SAXReader();
        Document doc = reader.read(url.openStream());
        System.out.println(doc.asXML());
        Location location = new Location(doc);
        return location;
    }

    public static void main(String args[]) {
        HostIp hostIp = new HostIp();
        try {
            Location loc = hostIp.getLocation("82.56.92.176");
            System.out.println(loc.getCountryName());
            System.out.println(loc.getCityName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
