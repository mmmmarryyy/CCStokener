package org.lindenb.tool.oneshot;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import org.lindenb.io.IOUtils;
import org.lindenb.util.Compilation;

/**
 * Simple Utility to get all the e-mail on my account on geni and
 * to find my relatives on other social networks
 * @author pierre
 *
 */
public class Geni01 {

    private int familyId = -1;

    private String _session_id = null;

    private static final int WAIT_SECONDS = 3;

    private int pageCount = -1;

    private HashSet<String> href(String urlstr) throws IOException {
        HashSet<String> hrefs = new HashSet<String>();
        URL url = new URL(urlstr);
        URLConnection con = url.openConnection();
        con.setRequestProperty("Cookie", "_session_id=" + _session_id);
        InputStreamReader r = new InputStreamReader(con.getInputStream());
        StringWriter b = new StringWriter();
        IOUtils.copyTo(r, b);
        r.close();
        try {
            Thread.sleep(WAIT_SECONDS * 1000);
        } catch (Exception err) {
        }
        String tokens[] = b.toString().replace("\n", " ").replaceAll("[\\<\\>]", "\n").split("[\n]");
        for (String s1 : tokens) {
            if (!(s1.startsWith("a") && s1.contains("href"))) continue;
            String tokens2[] = s1.split("[\\\"\\\']");
            for (String s2 : tokens2) {
                if (!(s2.startsWith("mailto:") || s2.matches("/profile/index/[0-9]+"))) continue;
                hrefs.add(s2);
            }
        }
        return hrefs;
    }

    private void run() throws IOException {
        HashSet<String> profiles = new HashSet<String>();
        HashSet<String> mails = new HashSet<String>();
        for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
            HashSet<String> set1 = href("http://www.geni.com/tree/family_list/" + familyId + "?filter=my_tree&tree_hide_deceased=1&page=" + pageIndex);
            for (String s2 : set1) {
                if (!s2.matches("/profile/index/[0-9]+")) continue;
                String url = "http://www.geni.com" + s2;
                profiles.add(url);
            }
            pageIndex++;
        }
        for (String url : profiles) {
            HashSet<String> set1 = href(url);
            for (String s2 : set1) {
                if (!s2.startsWith("mailto:")) continue;
                mails.add(s2.substring(7));
            }
        }
        for (String mail : mails) {
            System.out.println(mail);
        }
    }

    public static void main(String[] args) {
        try {
            Geni01 main = new Geni01();
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println("-h this screen");
                    System.err.println("-n number of pages");
                    System.err.println("-f family ID");
                    System.err.println("-c cookie-value of \"_session_id\"");
                    return;
                } else if (args[optind].equals("-n")) {
                    main.pageCount = Integer.parseInt(args[++optind]);
                } else if (args[optind].equals("-f")) {
                    main.familyId = Integer.parseInt(args[++optind]);
                } else if (args[optind].equals("-c")) {
                    main._session_id = args[++optind];
                } else if (args[optind].equals("--")) {
                    ++optind;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("bad argument " + args[optind]);
                    System.exit(-1);
                } else {
                    break;
                }
                ++optind;
            }
            if (main.familyId <= 0) {
                System.err.println("Unedfined family Id.");
                System.exit(-1);
            } else if (main.pageCount <= 0) {
                System.err.println("Unedfined page count.");
                System.exit(-1);
            } else if (main._session_id == null) {
                System.err.println("Unedfined _session_id.");
                System.exit(-1);
            } else if (optind == args.length) {
                System.err.println("Bad number of arguments.");
            }
            main.run();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}
