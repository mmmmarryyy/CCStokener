package project2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Downloads and parses the top 4 documents pertaining each relevant query and
 * constructs the content summary.
 */
public class RetrieveDocuments {

    public List<ContentSummary> contentSummaryList = new ArrayList<ContentSummary>();

    public static String site;

    /**
	 * Given category c and the classification 'classifier' returns the
	 * corresponding query list.
	 */
    public List<String> getQueries(String c, String classifier) {
        if (c.toLowerCase().equals("root")) {
            List<String> q = Rules.retrieveDocRules.get(c);
            if (classifier.contains("computers")) {
                q.addAll(Rules.retrieveDocRules.get("computers"));
            } else if (classifier.contains("health")) {
                q.addAll(Rules.retrieveDocRules.get("health"));
            } else if (classifier.contains("sports")) {
                q.addAll(Rules.retrieveDocRules.get("sports"));
            }
            return q;
        }
        return Rules.retrieveDocRules.get(c);
    }

    /** Retrieves the top 4 required documents from each relevant query. */
    public void retrieveDocs(String database, String classifier, Map<String, QueryInfo> queryInfoMap) {
        site = database;
        for (String c : classifier.split("/")) {
            if (Rules.retrieveDocRules.get(c).isEmpty()) {
                continue;
            }
            System.out.println("Creating Content Summary for: " + c);
            ContentSummary contentSummary = new ContentSummary(c, database);
            contentSummaryList.add(contentSummary);
            List<String> qList = getQueries(c, classifier);
            int querySize = qList.size();
            int i = 1;
            for (String q : qList) {
                System.out.println(i + "/" + querySize + "\n");
                QueryInfo queryInfo = queryInfoMap.get(q);
                if (queryInfo == null) {
                    System.out.println("**** null " + q);
                    continue;
                }
                for (String url : queryInfo.urls) {
                    try {
                        System.out.println("Getting page: " + url + "\n");
                        downloadFile(url);
                        contentSummary.updateSummary(url);
                    } catch (MalformedURLException e) {
                    } catch (IOException e) {
                    }
                }
                i++;
            }
        }
        for (ContentSummary c : contentSummaryList) {
            c.writeToFile();
        }
    }

    /** Download the file and stores it in the cache. */
    public void downloadFile(String url) throws MalformedURLException, IOException {
        File f = new File(getFilePath(url));
        if (f.exists()) {
            return;
        }
        BufferedInputStream in = new java.io.BufferedInputStream(new URL(url).openStream());
        createURLFolders(url);
        f.createNewFile();
        FileOutputStream fos = new java.io.FileOutputStream(f);
        BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
        byte data[] = new byte[1024];
        while (in.read(data, 0, 1024) >= 0) {
            bout.write(data);
        }
        bout.close();
        in.close();
    }

    public void createURLFolders(String url) {
        String path = getPath(url);
        new File(path).mkdirs();
    }

    /** Returns file location with out the file name. */
    public static String getPath(String url) {
        url = url.replaceAll("http://", "");
        int lastIndexOf = url.lastIndexOf("/");
        String pageName = url.substring(lastIndexOf, url.length());
        if (!pageName.equals("/")) {
            url = url.replace(pageName, "");
        }
        String path = "cache" + File.separatorChar + site + "_documents" + File.separatorChar;
        for (String p : url.split("/")) {
            path += p + File.separatorChar;
        }
        return path;
    }

    /** Returns the file path with the file name corresponding to the url. */
    public static String getFilePath(String url) {
        return getPath(url) + File.separatorChar + Math.abs(url.hashCode()) + ".html";
    }
}
