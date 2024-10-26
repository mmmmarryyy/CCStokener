package edu.ucdavis.genomics.metabolomics.sjp.parser.nist.webbook;

import edu.ucdavis.genomics.metabolomics.sjp.handler.FileDownloadHandler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author Gert Wohlgemuth
 *
 * l?dt JCAMP files herunter indem er die ermmitelten urls parst
 * und speichert sie im angeebenen verzeichnes. Dieses gilt allerdings nur f?r die nistms
 * webbook seiten!
 */
public class JCAMPDownloader extends FileDownloadHandler {

    /**
     * @see edu.ucdavis.genomics.metabolomics.binbase.parser.handler.FileDownloader#handleUrl(java.net.URL)
     */
    protected void handleUrl(URL url) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String s;
        boolean moreResults = false;
        while ((s = in.readLine()) != null) {
            if (s.indexOf("<h1>Search Results</h1>") > -1) {
                System.err.println("found severals result");
                moreResults = true;
            } else if (s.indexOf("Download <a href=") > -1) {
                if (s.indexOf("in JCAMP-DX format.") > -1) {
                    System.err.println("download masspec");
                    super.handleUrl(new URL((url.getProtocol() + "://" + url.getHost() + s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\""))).replaceAll("amp;", "")));
                }
                moreResults = false;
            }
            if (moreResults == true) {
                if (s.indexOf("<li><a href=\"/cgi/cbook.cgi?ID") > -1) {
                    System.err.println("\tdownloading new url " + new URL((url.getProtocol() + "://" + url.getHost() + s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\""))).replaceAll("amp;", "")));
                    this.handleUrl(new URL((url.getProtocol() + "://" + url.getHost() + s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\""))).replaceAll("amp;", "")));
                }
            }
        }
    }
}
