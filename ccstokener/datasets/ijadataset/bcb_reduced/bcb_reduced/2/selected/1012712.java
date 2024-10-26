package org.seaurchin.crawler;

import java.text.*;
import java.util.*;
import java.net.*;
import java.io.*;
import org.seaurchin.database.*;

public class WebCrawler {

    public static final String SEARCH = "Search";

    public static final String STOP = "Stop";

    public static final String DISALLOW = "Disallow:";

    public static final int SEARCH_LIMIT = 20000;

    Vector vectorToSearch = new Vector();

    Vector vectorSearched = new Vector();

    Vector vectorMatches = new Vector();

    public static void main(String argv[]) {
        Properties props = new Properties(System.getProperties());
        props.put("http.proxySet", "true");
        props.put("http.proxyHost", "webcache-cup");
        props.put("http.proxyPort", "8080");
        Properties newprops = new Properties(props);
        System.setProperties(newprops);
        WebCrawler cw = new WebCrawler();
        cw.crawl();
        return;
    }

    public void crawl() {
        File file = new File("c:/Seachurin/conf/FeedList.txt");
        FileWriter fw = null;
        FileWriter fw1 = null;
        BufferedReader in = null;
        try {
            FileReader fr = new FileReader(file);
            in = new BufferedReader(fr);
        } catch (FileNotFoundException e) {
            System.out.println("File Disappeared!");
        }
        File out1 = new File("c:/Seachurin/crawldb/index.txt");
        try {
            fw1 = new FileWriter(out1);
        } catch (IOException e) {
            System.out.println("Can not open stream fw");
        }
        PrintWriter pw1 = new PrintWriter(fw1);
        vectorToSearch.removeAllElements();
        vectorSearched.removeAllElements();
        vectorMatches.removeAllElements();
        URLConnection.setDefaultAllowUserInteraction(false);
        String strURL = null;
        int numberSearched = 0;
        int numberFound = 1;
        try {
            while ((strURL = in.readLine()) != null) vectorToSearch.addElement(strURL);
        } catch (IOException e) {
            System.out.println(" in.readLine() errors!");
        }
        while (vectorToSearch.size() > 0) {
            strURL = (String) vectorToSearch.elementAt(0);
            if (strURL.length() == 0) {
                System.out.println("ERROR: must enter a starting URL");
                return;
            }
            System.out.println("searching " + strURL);
            URL url;
            String str1 = strURL.concat("/siteinfo.xml");
            String str2 = strURL.concat("/index.html");
            String category = null;
            if ((category = checkSiteInfo(str1)) == null) break; else {
            }
            try {
                url = new URL(str2);
            } catch (MalformedURLException e) {
                System.out.println("ERROR: invalid URL " + strURL);
                break;
            }
            vectorToSearch.removeElementAt(0);
            vectorSearched.addElement(str2);
            if (url.getProtocol().compareTo("http") != 0) break;
            if (!robotSafe(url)) break;
            try {
                URLConnection urlConnection = url.openConnection();
                urlConnection.setAllowUserInteraction(false);
                InputStream urlStream = url.openStream();
                String type = urlConnection.getContentType();
                System.out.println(" File Type is: " + type);
                if (type == null) break;
                byte b[] = new byte[1000];
                int numRead = urlStream.read(b);
                String content = new String(b, 0, numRead);
                while (numRead != -1) {
                    numRead = urlStream.read(b);
                    if (numRead != -1) {
                        String newContent = new String(b, 0, numRead);
                        content += newContent;
                    }
                }
                urlStream.close();
                String fileName = "c:/Seachurin/crawldb/doc" + numberFound + ".txt";
                File out = new File(fileName);
                try {
                    fw = new FileWriter(out);
                    PrintWriter pw = new PrintWriter(fw);
                    pw.println(content);
                    fw.close();
                } catch (IOException e) {
                    System.out.println("Can not open stream fw");
                }
                System.out.println(content);
                pw1.print(fileName + "\t");
                pw1.print(str2 + "\t");
                pw1.print(category);
                pw1.println();
                fw1.close();
                numberSearched++;
                String lowerCaseContent = content.toLowerCase();
                int index = 0;
                while ((index = lowerCaseContent.indexOf("<a", index)) != -1) {
                    if ((index = lowerCaseContent.indexOf("href", index)) == -1) break;
                    if ((index = lowerCaseContent.indexOf("=", index)) == -1) break;
                    index++;
                    String remaining = content.substring(index);
                    StringTokenizer st = new StringTokenizer(remaining, "\t\n\r\">#");
                    String strLink = st.nextToken();
                    URL urlLink;
                    try {
                        urlLink = new URL(url, strLink);
                        strLink = urlLink.toString();
                    } catch (MalformedURLException e) {
                        System.out.println("ERROR: bad URL " + strLink);
                        continue;
                    }
                    if (urlLink.getProtocol().compareTo("http") != 0) break;
                    try {
                        URLConnection urlLinkConnection = urlLink.openConnection();
                        urlLinkConnection.setAllowUserInteraction(false);
                        InputStream linkStream = urlLink.openStream();
                        String strType = urlLinkConnection.guessContentTypeFromStream(linkStream);
                        linkStream.close();
                        if (strType == null) break;
                        if ((strType.compareTo("text/html") == 0) || (strType.compareTo("application/pdf") == 0)) {
                            if ((!vectorSearched.contains(strLink)) && (!vectorToSearch.contains(strLink))) {
                                if (robotSafe(urlLink)) vectorToSearch.addElement(strLink);
                            }
                        }
                        if ((strType.compareTo("text/html") == 0) || (strType.compareTo("application/pdf") == 0)) {
                            if (vectorMatches.contains(strLink) == false) {
                                vectorMatches.addElement(strLink);
                                numberFound++;
                                if (numberFound >= SEARCH_LIMIT) break;
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("ERROR: couldn't open URL " + strLink);
                        continue;
                    }
                }
            } catch (IOException e) {
                System.out.println("ERROR: couldn't open URL " + strURL);
                break;
            }
            numberSearched++;
            if (numberSearched >= SEARCH_LIMIT) break;
        }
        if (numberSearched >= SEARCH_LIMIT || numberFound >= SEARCH_LIMIT) System.out.println("reached search limit of " + SEARCH_LIMIT); else System.out.println("done");
    }

    public boolean robotSafe(URL url) {
        String strHost = url.getHost();
        String strRobot = "http://" + strHost + "/robots.txt";
        URL urlRobot;
        try {
            urlRobot = new URL(strRobot);
        } catch (MalformedURLException e) {
            return false;
        }
        String strCommands;
        try {
            InputStream urlRobotStream = urlRobot.openStream();
            byte b[] = new byte[1000];
            int numRead = urlRobotStream.read(b);
            strCommands = new String(b, 0, numRead);
            while (numRead != -1) {
                numRead = urlRobotStream.read(b);
                if (numRead != -1) {
                    String newCommands = new String(b, 0, numRead);
                    strCommands += newCommands;
                }
            }
            urlRobotStream.close();
        } catch (IOException e) {
            return true;
        }
        String strURL = url.getFile();
        int index = 0;
        while ((index = strCommands.indexOf(DISALLOW, index)) != -1) {
            index += DISALLOW.length();
            String strPath = strCommands.substring(index);
            StringTokenizer st = new StringTokenizer(strPath);
            if (!st.hasMoreTokens()) break;
            String strBadPath = st.nextToken();
            if (strURL.indexOf(strBadPath) == 0) return false;
        }
        return true;
    }

    public String checkSiteInfo(String st1) {
        String content = null;
        String name;
        String description;
        String service_type;
        String service_url;
        String access_info;
        PublisherInfo publisher;
        SiteCategories[] categories;
        String contact_name;
        String title;
        String affiliation;
        String phone;
        String email;
        String categoryname;
        String category;
        String taxonomyschema;
        try {
            URL u = new URL(st1);
            URLConnection uc = u.openConnection();
            System.out.println("Content Type is: " + uc.getContentType());
            if (uc.getContentType().compareTo("application/xml") != 0) return null;
            InputStream raw = uc.getInputStream();
            byte b[] = new byte[1000];
            int numRead = raw.read(b);
            content = new String(b, 0, numRead);
            while (numRead != -1) {
                numRead = raw.read(b);
                if (numRead != -1) {
                    String newContent = new String(b, 0, numRead);
                    content += newContent;
                }
            }
        } catch (IOException e) {
            System.out.println("Can not open the URLSTREAM");
        }
        System.out.println(content);
        int index1, index2, index3 = 0;
        index1 = content.indexOf("<Name>");
        index2 = index1 + 6;
        index3 = content.indexOf("</Name>");
        System.out.println("The Name is: " + content.substring(index2, index3));
        name = content.substring(index2, index3);
        String st = content.substring(index3 + 7);
        description = searchString(st, "Description");
        service_type = searchString(st, "ServiceType");
        service_url = searchString(st, "ServiceUrl");
        access_info = searchString(st, "AccessInfo");
        contact_name = searchString(st, "ContactName");
        title = searchString(st, "Title");
        affiliation = searchString(st, "Affiliation");
        phone = searchString(st, "Phone");
        email = searchString(st, "Email");
        categoryname = searchString(st, "Name");
        category = searchString(st, "Category");
        taxonomyschema = searchString(st, "TaxonomySchema");
        persist.BeginTrans();
        SiteInfo bob = new SiteInfo(name, description, service_type, service_url, access_info);
        persist.StoreSiteInfo(bob);
        PublisherInfo bob2 = new PublisherInfo(contact_name, title, affiliation, phone, email);
        persist.StorePublisherInfo(bob2);
        SiteCategories bob3 = new SiteCategories(categoryname, category, taxonomyschema);
        persist.StoreSiteCategories(bob3);
        persist.CommitTrans();
        return category;
    }

    private String searchString(String st, String subStr) {
        int index1, index2, index3 = 0;
        index1 = st.indexOf("<" + subStr + ">");
        index2 = index1 + 2 + subStr.length();
        index3 = st.indexOf("</" + subStr + ">");
        System.out.println("The element is: " + st.substring(index2, index3));
        return st.substring(index2, index3);
    }
}
