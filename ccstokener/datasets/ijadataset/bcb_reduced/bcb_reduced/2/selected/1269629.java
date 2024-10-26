package ceirinhashls;

import java.io.*;
import java.text.*;

/**
 *
 * @author  botelhodaniel
 */
public class OpenDCHubList {

    private static MessageFormat mf = new MessageFormat("{0}|{1}|{2}|{3}|||||");

    private static Object[] args;

    /** Creates a new instance of OpenDCHubList */
    public OpenDCHubList() {
    }

    public static void openFile(PublicHubList hublist, String url) {
        BufferedReader fichAl;
        String linha;
        try {
            if (url.startsWith("http://")) fichAl = new BufferedReader(new InputStreamReader((new java.net.URL(url)).openStream())); else fichAl = new BufferedReader(new FileReader(url));
            while ((linha = fichAl.readLine()) != null) {
                try {
                    hublist.addDCHub(new DCHub(linha, DCHub.hublistFormater));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        PublicHubList hublist = new PublicHubList();
        OpenDCHubList.openFile(hublist, "http://devilishly.no.sapo.pt/src/public.config");
        java.util.Iterator it = hublist.iterator();
        while (it.hasNext()) System.out.println((DCHub) it.next());
    }
}
