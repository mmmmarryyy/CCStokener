package com.directthought.elasticweb.nettica;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import ch.inventec.Base64Coder;

public class NetticaRESTAPI {

    private static final String NETTICA_URL = "https://www.nettica.com/Domain/Update.aspx";

    private String username;

    private String password;

    public NetticaRESTAPI(String username, String password) {
        this.username = username;
        this.password = Base64Coder.encodeString(password);
    }

    public void addARecord(String domainName, String hostname, String ipv4) throws NetticaException {
        try {
            String query = "?U=" + username + "&P=" + password + "&FQDN=" + domainName + "&A=" + hostname + "&N=" + ipv4;
            URL url = new URL("http", "localhost", 80, "Domain/Update.aspx" + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int response = conn.getResponseCode();
            if (response > 200) {
                throw new NetticaException(decodeStatus(response));
            }
        } catch (MalformedURLException ex) {
            throw new NetticaException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new NetticaException(ex.getMessage(), ex);
        }
    }

    public void deleteARecord(ARecord record) throws NetticaException {
        deleteARecord(record.domainName, record.hostname, record.data);
    }

    public void deleteARecord(String domainName, String hostname, String ipv4) throws NetticaException {
        return;
    }

    public List<ARecord> listARecord(String domainName) throws NetticaException {
        return null;
    }

    public ServiceInfo getServiceInfo() throws NetticaException {
        return null;
    }

    private String decodeStatus(int status) {
        switch(status) {
            case 200:
                return "Success";
            case 401:
                return "Access denied";
            case 404:
                return "Not found";
            case 431:
                return "Record already exists";
            case 432:
                return "Invalid record type. Must be \"A\",\"MX\",\"CNAME\",\"F\",\"TXT\",\"SRV\"";
            case 450:
                return "No service";
            case 460:
                return "Your service has expired";
            default:
                return "Unknown status code";
        }
    }

    public class ARecord {

        String hostname;

        String domainName;

        String data;

        public ARecord(String hostname, String domainName, String data, int ttl) {
            this.hostname = hostname;
            this.domainName = domainName;
            this.data = data;
        }
    }

    public class ServiceInfo {

        int remainingCredits;

        int totalCredits;

        String renewalDate;

        public ServiceInfo(int remainingCredits, int totalCredits, String renewalDate) {
            this.remainingCredits = remainingCredits;
            this.totalCredits = totalCredits;
            this.renewalDate = renewalDate;
        }
    }
}
