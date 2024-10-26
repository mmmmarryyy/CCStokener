package master.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Class for getting XML dump files from a Wiki.
 * 
 * @author Janette
 * @version 0.0.1, 2009-06-28
 */
public class WikiDataExport {

    /***************************************************************************
	 * Attributes
	 **************************************************************************/
    private static final String SOURCE_FILE = "C:/Users/Nette/Desktop/XXX/DataSetsList.txt";

    private static final String DESTINATION_FOLDER = "C:/Users/Nette/Desktop/XXX/";

    private static final String HOST_REV = "http://en.wikipedia.org/w/index.php";

    private static final String HOST_API = "http://en.wikipedia.org/w/api.php";

    /***************************************************************************
	 * Constructors
	 **************************************************************************/
    public WikiDataExport() {
    }

    /***************************************************************************
	 * Wiki Data Export Methods
	 **************************************************************************/
    public final void exportData() {
        try {
            final ArrayList<String> pageNames = loadPageNames();
            final File fileExtractTime = new File(DESTINATION_FOLDER + "2 ExtractTimes.txt");
            fileExtractTime.createNewFile();
            final FileWriter fileWriter = new FileWriter(fileExtractTime);
            for (final Iterator<String> i = pageNames.iterator(); i.hasNext(); ) {
                final String pageName = i.next();
                final String time = new java.util.Date().toString() + ": " + pageName;
                System.out.print(time);
                fileWriter.write(time);
                fileWriter.flush();
                final String statisticRevisions = extractPageRevisionsFromWiki(pageName);
                final String statisticLinks = extractPageLinksFromWiki(pageName);
                System.out.println(" (" + statisticRevisions + ", " + statisticLinks + ")");
                fileWriter.write(" (" + statisticRevisions + ", " + statisticLinks + ")\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Export data from a given page and save them in a file.
	 * 
	 * @param pageName
	 * 			name of the page
	 * @return number of revisions and number of users
	 */
    private String extractPageRevisionsFromWiki(final String pageName) {
        Integer numberOfRevisions = 0;
        final HashSet<String> hsUsers = new HashSet<String>();
        try {
            String data = URLEncoder.encode("title", "UTF-8") + "=" + URLEncoder.encode("Special:Export", "UTF-8");
            data += "&" + URLEncoder.encode("pages", "UTF-8") + "=" + URLEncoder.encode(pageName, "UTF-8");
            data += "&" + URLEncoder.encode("history", "UTF-8") + "=" + URLEncoder.encode("true", "UTF-8");
            final URL url = new URL(HOST_REV);
            final URLConnection urlConn = url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setDoInput(true);
            final OutputStreamWriter urlWriter = new OutputStreamWriter(urlConn.getOutputStream());
            urlWriter.write(data);
            urlWriter.flush();
            final BufferedReader urlReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            final File outputFile = new File(DESTINATION_FOLDER + pageName + ".xml");
            outputFile.createNewFile();
            final FileWriter outputFileWriter = new FileWriter(outputFile);
            String line;
            boolean nextIsAuthor = false;
            while ((line = urlReader.readLine()) != null) {
                outputFileWriter.write(line + "\n");
                if (line.contains("<revision>")) {
                    numberOfRevisions++;
                }
                if (nextIsAuthor) {
                    nextIsAuthor = false;
                    if (line.contains("<ip>") && line.contains("</ip")) {
                        hsUsers.add(line);
                    }
                }
                if (line.contains("<contributor>")) {
                    nextIsAuthor = true;
                }
            }
            outputFileWriter.flush();
            urlWriter.close();
            urlReader.close();
            outputFileWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return numberOfRevisions + " revisions, " + hsUsers.size() + " users";
    }

    /**
	 * Export links from a given page.
	 * 
	 * @param pageName
	 * 			name of the page
	 * @return number of links
	 */
    private String extractPageLinksFromWiki(final String pageName) {
        Integer numberOfLinks = 0;
        try {
            String data = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("query", "UTF-8");
            data += "&" + URLEncoder.encode("format", "UTF-8") + "=" + URLEncoder.encode("txt", "UTF-8");
            data += "&" + URLEncoder.encode("titles", "UTF-8") + "=" + URLEncoder.encode(pageName, "UTF-8");
            data += "&" + URLEncoder.encode("prop", "UTF-8") + "=" + URLEncoder.encode("links", "UTF-8");
            data += "&" + URLEncoder.encode("pllimit", "UTF-8") + "=" + URLEncoder.encode("500", "UTF-8");
            String plcontinue = "";
            while (plcontinue != null) {
                final URL url = new URL(HOST_API);
                final URLConnection urlConn = url.openConnection();
                urlConn.setDoOutput(true);
                urlConn.setDoInput(true);
                final OutputStreamWriter urlWriter = new OutputStreamWriter(urlConn.getOutputStream());
                urlWriter.write(data + plcontinue);
                urlWriter.flush();
                final BufferedReader urlReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                String line;
                plcontinue = null;
                while ((line = urlReader.readLine()) != null) {
                    if (line.contains("[title]")) {
                        numberOfLinks++;
                    }
                    if (line.contains("[plcontinue]")) {
                        plcontinue = "&" + URLEncoder.encode("plcontinue", "UTF-8") + "=" + URLEncoder.encode(line.substring(line.indexOf(" => ") + 4, line.length()), "UTF-8");
                    }
                }
                numberOfLinks--;
                urlReader.close();
                urlWriter.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return numberOfLinks + " links";
    }

    /**
	 * Load page names from file.
	 */
    private ArrayList<String> loadPageNames() {
        final ArrayList<String> alPageNames = new ArrayList<String>();
        try {
            final BufferedReader fileResult = new BufferedReader(new FileReader(SOURCE_FILE));
            String thisLine = "";
            while ((thisLine = fileResult.readLine()) != null) {
                alPageNames.add(thisLine);
            }
            fileResult.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return alPageNames;
    }
}
