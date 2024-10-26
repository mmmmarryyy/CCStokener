package com.softwaresmithy.lib;

import java.io.IOException;
import java.net.URI;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

public class CheckVersionCompatibility {

    private static final String[] libUrls = new String[] { "http://aqua.queenslibrary.org/rss.ashx", "http://aqua.santacruzpl.org/rss.ashx", "http://lens.lib.uchicago.edu/", "http://search.prairiecat.info/", "http://boss.library.okstate.edu/" };

    private static final String[] badUrls = new String[] { "http://192.69.117.21/ipac20/ipac.jsp", "http://ipac.librarypoint.org/ipac20/ipac.jsp", "http://hip.hamptonpubliclibrary.org/ipac20/ipac.jsp", "http://166.61.230.4/TLCScripts/interpac.dll?SearchForm?Directions=1&Config=pac&Branch=0", "http://www.brrl.lib.va.us/TLCScripts/interpac.dll?SearchForm?Directions=1&config=pac", "http://ccl.campbell.k12.va.us/TLCScripts/interpac.dll?SearchForm?Directions=1&config=pac", "http://64.4.111.3/TLCScripts/interpac.dll?SearchForm&Directions=1&Config=ysm&Branch=,0,&FormId=", "http://www.cclibrary.net/TLCScripts/interpac.dll?SearchForm?Directions=1&config=pac", "http://64.183.158.202/uhtbin/cgisirsi.exe/LWfLcRQ8jp/SIRSI/0/49", "http://ibistro.chesapeake.lib.va.us/uhtbin/cgisirsi.exe/?ps=Ms4aEATF3x/CENTRAL/84480016/60/1180/X", "http://fcplcat.fairfaxcounty.gov/uhtbin/cgisirsi/fhLkl3JhCm/ZTECHOPS/237210105/60/1180/X", "http://catalog.henrico.lib.va.us/uhtbin/cgisirsi/0/0/0/60/1180/X", "http://206.113.146.18/uhtbin/cgisirsi/zMHq0MLxv9/MAIN/284790044/38/0/POWER_SEARCH", "http://librarycatalog.pwcgov.org/polaris/search/default.aspx?ctx=1.1033.0.0.3&type=Advanced" };

    public static void main(String... args) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpHost proxy = new HttpHost("proxy.houston.hp.com", 8080);
        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        System.out.println("AquaBrowser systems, expect true");
        for (String libUrl : libUrls) {
            URI uri = new URI(libUrl);
            URI mangledUri = new URI(uri.getScheme() + "://" + uri.getAuthority() + "/rss.ashx");
            HttpGet get = new HttpGet(mangledUri);
            try {
                HttpResponse resp = client.execute(get);
                StatusLine respStatus = resp.getStatusLine();
                String retStatus = "";
                if (respStatus.getStatusCode() == 200) {
                    retStatus = "Supported";
                } else if (respStatus.getStatusCode() == 503 && respStatus.getReasonPhrase().equals("Rss module not active.")) {
                    retStatus = "AquaBrowser, but unsupported";
                } else {
                    retStatus = "Not AquaBrowser";
                }
                System.out.println("Status: " + retStatus);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (get != null) {
                    get.abort();
                }
            }
        }
        System.out.println("Other systems, expect false");
        for (String libUrl : badUrls) {
            URI uri = new URI(libUrl);
            URI mangledUri = new URI(uri.getScheme() + "://" + uri.getAuthority() + "/rss.ashx");
            HttpGet get = new HttpGet(mangledUri);
            try {
                HttpResponse resp = client.execute(get);
                System.out.println("Status: " + resp.getStatusLine());
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (get != null) {
                    get.abort();
                }
            }
        }
    }
}
