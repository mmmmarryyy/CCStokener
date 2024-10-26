package org.jproxyfy.ipinfo.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.log4j.Logger;
import org.jproxyfy.enums.CountryCode;
import org.jproxyfy.ipinfo.IpInfo;
import org.jproxyfy.ipinfo.IpInfoProvider;

/**
 * This class will provides IP info using the the service provided by
 * http://ipinfodb.com/
 * 
 * @author devender
 * 
 */
public class IpInfoProviderImpl implements IpInfoProvider {

    private static final Logger logger = Logger.getLogger(IpInfoProviderImpl.class);

    @Override
    public IpInfo get(String ip) {
        try {
            URL url = new URL("http://ipinfodb.com/ip_query.php?ip=" + ip);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer buffer = new StringBuffer();
                String temp = "";
                while (null != temp) {
                    buffer.append(temp);
                    temp = in.readLine();
                }
                return parse(ip, buffer.toString());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private IpInfo parse(String ip, String string) {
        if (null == string) {
            return null;
        } else {
            IpInfo info = new IpInfo(ip, 80);
            if (string.contains("<Response>")) {
                info.setStatus(getTagValue(string, "<Status>", "</Status>"));
                String cc = getTagValue(string, "<CountryCode>", "</CountryCode>");
                CountryCode countryCode = CountryCode.valueForCode(cc);
                logger.info("Got ip for country " + cc + " our code is " + countryCode.name());
                info.setCountryCode(countryCode);
                info.setReagionCode(getTagValue(string, "<RegionCode>", "</RegionCode>"));
                info.setRegionName(getTagValue(string, "<RegionName>", "</RegionName>"));
                info.setCity(getTagValue(string, "<City>", "</City>"));
            }
            return info;
        }
    }

    private static String getTagValue(String xml, String beginTag, String endTag) {
        if (xml.contains(beginTag)) {
            int s = xml.indexOf(beginTag);
            int e = xml.indexOf(endTag);
            String sub = xml.substring(s, e).replaceAll(beginTag, "").replaceAll(endTag, "");
            return sub;
        } else {
            return null;
        }
    }
}
