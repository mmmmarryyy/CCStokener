package edu.fudan.cse.medlab.event.moresite;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * ��ȡGoogle��PageRank�Ĳ��Է���,����Ϊgoogle���ṩ��ip��ַ <br />
 * 64.233.161.100 <br />
 * 64.233.161.101 <br />
 * 64.233.177.17 <br />
 * 64.233.183.91 <br />
 * 64.233.185.19 <br />
 * 64.233.189.44 <br />
 * 66.102.1.103 <br />
 * 66.102.9.115 <br />
 * 66.249.81.101 <br />
 * 66.249.89.83 <br />
 * 66.249.91.99 <br />
 * 66.249.93.190 <br />
 * 72.14.203.107 <br />
 * 72.14.205.113 <br />
 * 72.14.255.107
 */
public class PageRankService {

    private static int dataCenterIdx = 0;

    /**
     * List of available google datacenter IPs and addresses
     */
    public static final String[] GOOGLE_PR_DATACENTER_IPS = new String[] { "64.233.161.100", "64.233.161.101", "64.233.183.91", "64.233.189.44", "66.102.1.103", "66.102.9.115", "66.249.81.101", "66.249.89.83", "66.249.91.99", "66.249.93.190", "72.14.203.107", "72.14.205.113", "toolbarqueries.google.com" };

    /**
     * Default constructor
     */
    public PageRankService() {
    }

    /**
     * ����ָ���������ȡprֵ
     *
     * @param domain
     *            ����
     * @return int prֵ,����-1��˵��û�и������¼
     */
    public int getPR(String domain) {
        int result = -1;
        GetGooglePrHash gHash = new GetGooglePrHash();
        String googlePrResult = "";
        long hash = gHash.hash(("info:" + domain).getBytes());
        String urlstring = "http://" + GOOGLE_PR_DATACENTER_IPS[dataCenterIdx] + "/search?client=navclient-auto&hl=en&" + "ch=6" + hash + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:" + domain;
        URL url = null;
        HttpURLConnection uconn = null;
        BufferedReader bin = null;
        InputStream is = null;
        try {
            while (true) {
                url = new URL(urlstring);
                uconn = (HttpURLConnection) url.openConnection();
                is = uconn.getInputStream();
                if (is == null) continue; else break;
            }
            byte[] buff = new byte[1024];
            int read = is.read(buff);
            while (read > 0) {
                googlePrResult = new String(buff, 0, read);
                read = is.read(buff);
            }
            if (googlePrResult == "" && !domain.startsWith("http://")) {
                PageRankService prService = new PageRankService();
                domain = "http://" + domain;
                return prService.getPR(domain);
            }
            googlePrResult = googlePrResult.split(":")[2].trim();
            result = new Long(googlePrResult).intValue();
        } catch (Exception e) {
        }
        dataCenterIdx++;
        if (dataCenterIdx == GOOGLE_PR_DATACENTER_IPS.length) {
            dataCenterIdx = 0;
        }
        return result;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        PageRankService prService = new PageRankService();
        String domain = "www.google.com";
        if (args.length > 0) {
            domain = args[0];
        }
        System.out.println("Checking " + domain);
        System.out.println("Google PageRank: " + prService.getPR(domain));
        System.out.println("Took: " + (System.currentTimeMillis() - start) + "ms");
    }
}
