package org.exist.webtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLTestCase;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Utilitaire de test HTTP Get, aussi bien pour les requ�tes XQuery que pour les
 * pipelines Cocoon. Cela permet de comparer le r�sultat XML (ou HTML) d'un URL
 * avec un r�sultat de r�f�rence stock� dans un fichier. Il permet aussi de
 * cr�er ces r�sultats de r�f�rence. Il est tr�s utile dans une phase de
 * restructuration de code et/ou d'optimisation, pour s'assurer que les
 * r�sultats n'ont pas chang�.
 * 
 * On s'appuie sur l'utilitaire XMLUnit ( http://XMLUnit.sf.net ) qui lui-m�me
 * s'appuie sur JUnit, le standard Java des utilitaires de tests unitaires, qui
 * est bien int�gr� � eclipse. Cet outil est un projet eclipse autosuffisant.
 */
public abstract class XMLGetTest extends XMLTestCase {

    /** Settings */
    private String urlPrefix = "http://127.0.0.1:8080/exist/xquery";

    private String referenceResultsDirectory = "";

    private String sourceDirectory = "";

    /** implementation fields */
    protected transient DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    protected transient DocumentBuilder newDocumentBuilder;

    private String ignoredXPath = "";

    protected XMLGetTest() {
        super();
        try {
            newDocumentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /** the most important method: check given URL with respect to 
	 * allready present Reference File; if XML comparison result is not identical,
	 * it also saves the actual result in a directory called "actual-results". */
    protected final void checkURLWithReferenceFile(String relativeURL) throws Exception, IOException, ParserConfigurationException {
        String referenceFile = computeFileNameFromURL(relativeURL);
        checkURLWithReferenceFile(relativeURL, referenceFile);
    }

    protected final void checkURLWithReferenceFile(String relativeURL, String referenceFile) throws SAXException, IOException, ParserConfigurationException {
        Document actualDocument = sendRequestToWebServer(relativeURL);
        Document referenceDocument;
        try {
            referenceDocument = newDocumentBuilder.parse(new File(referenceFile));
        } catch (SAXException e) {
            System.out.println("!!!!! Problem parsing reference File: " + e);
            return;
        } catch (IOException e) {
            System.out.println("!!!!! Problem accessing reference File: " + e);
            return;
        }
        Diff myDiff = new Diff(referenceDocument, actualDocument);
        boolean identical = myDiff.identical();
        boolean ko = false;
        if (!identical) {
            DetailedDiff diffs = new DetailedDiff(myDiff);
            List difflist = diffs.getAllDifferences();
            for (Iterator iter = difflist.iterator(); iter.hasNext(); ) {
                Difference diff = (Difference) iter.next();
                System.out.println("  difference>> " + diff);
                if (diff.getTestNodeDetail().getXpathLocation().equals(getIgnoredXPath())) {
                } else {
                    ko = true;
                }
            }
        }
        identical = identical || !ko;
        if (!identical) {
            File actualResultsDirectory = new File("actual-results");
            actualResultsDirectory.mkdir();
            File currentResultsDirectory = new File("actual-results" + File.separator + referenceResultsDirectory);
            currentResultsDirectory.mkdir();
            if (saveURLToFile(new URL(getURLPrefix() + relativeURL), new File(actualResultsDirectory.getAbsolutePath() + File.separator + computeFileNameFromURL(relativeURL)))) System.out.println("saved wrong result for " + relativeURL + " in " + currentResultsDirectory);
            System.out.println(myDiff);
        }
        assertTrue(relativeURL + " XQuery result should be identical to reference result", identical);
    }

    public final DetailedDiff detailedDiff(String relativeURL) {
        String referenceFile = computeFileNameFromURL(relativeURL);
        Document actualDocument = null;
        try {
            actualDocument = sendRequestToWebServer(relativeURL);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (SAXException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        Document referenceDocument;
        try {
            referenceDocument = newDocumentBuilder.parse(new File(referenceFile));
        } catch (SAXException e) {
            System.out.println("!!!!! Problem parsing reference File: " + e);
            return null;
        } catch (IOException e) {
            System.out.println("!!!!! Problem accessing reference File: " + e);
            return null;
        }
        Diff myDiff = new Diff(referenceDocument, actualDocument);
        return new DetailedDiff(myDiff);
    }

    /** send Request To Web Server and parse the resulting XML.  */
    private Document sendRequestToWebServer(String relativeURL) throws MalformedURLException, SAXException, IOException {
        URL url = new URL(getURLPrefix() + relativeURL);
        System.out.print("Query " + url.toString());
        long t0 = System.currentTimeMillis();
        Document actualDocument = null;
        try {
            actualDocument = newDocumentBuilder.parse(url.toString());
        } catch (SAXException e) {
            System.out.print("\n!!!!! SAXException: " + url + "  \n - " + e);
        } catch (IOException e) {
            System.out.print("\n!!!!! IOException: " + url + "  \n - " + e);
        }
        System.out.println(" - elapsed: " + (System.currentTimeMillis() - t0) + " ms");
        return actualDocument;
    }

    /**
	 * create a Reference File from the given URL, ensuring also that there is a reference
	 * implementation URL whose name is obtained by adding -orig
	 * ( delegated to @link #computeReferenceURL(String) )
	 */
    protected final void createReferenceFiles(String relativeURL) {
        createReferenceSourceFile(relativeURL);
        downloadURLToFile(relativeURL, System.getProperty("user.dir"));
    }

    /** create Reference Source File, if not allready present */
    private void createReferenceSourceFile(String relativeURL) {
        if (getSourceDirectory().equals("")) {
            return;
        } else {
            String sourceFileName = relativeURL.split("\\?")[0];
            System.out.println("createReferenceSourceFile: sourceFileName: " + sourceFileName);
            String referenceSourceFileName = computeReferenceURL(sourceFileName);
            File f = new File(getSourceDirectory() + File.separator + referenceSourceFileName);
            if (!f.isFile()) {
                File fExisting = new File(getSourceDirectory() + File.separator + sourceFileName);
                try {
                    saveURLToFile(fExisting.toURL(), f);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("createReferenceSourceFile: " + f + " allready present on disk.");
            }
        }
    }

    /** download given relative URL To a File whose name is
	 * computed from the URL. */
    private final void downloadURLToFile(String relativeURL, String directory) {
        String referenceURL = getURLPrefix() + computeReferenceURL(relativeURL);
        String referenceFileName = computeFileNameFromURL(relativeURL);
        File referenceFile = new File(directory + File.separator + referenceFileName);
        if (referenceFile.exists()) {
            System.out.println("reference File allready present: " + referenceFile.getAbsolutePath() + " (or not writable)");
            return;
        }
        URL url;
        try {
            url = new URL(referenceURL);
            File directoryFile = new File(directory + File.separator + getReferenceResultsDirectory());
            directoryFile.mkdir();
            long t0 = System.currentTimeMillis();
            if (saveURLToFile(url, referenceFile)) {
                System.out.print("Done reference: " + referenceFileName);
                System.out.println(" - elapsed: " + (System.currentTimeMillis() - t0) + " ms");
            }
            ;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /** plain reusable method (inspired by Java in a Nutshell) */
    public static boolean saveURLToFile(URL url, File file) {
        InputStream in = null;
        OutputStream out = null;
        boolean result = false;
        try {
            in = url.openStream();
            out = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = in.read(buffer)) != -1) out.write(buffer, 0, bytes_read);
            result = true;
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            try {
                in.close();
                out.close();
            } catch (Exception e) {
            }
        }
        return result;
    }

    /** compute a name for the Reference URL, by adding -orig in the URL */
    protected abstract String computeReferenceURL(String relativeURL);

    /** compute a name for the Reference File, by replacing the 
	 * unsuitable characters in the URL */
    private String computeFileNameFromURL(String relativeURL) {
        Matcher m2 = Pattern.compile("/").matcher(relativeURL);
        String referenceFile = getReferenceResultsDirectory() + File.separator + m2.replaceAll("_");
        Matcher m3 = Pattern.compile("\\?").matcher(referenceFile);
        referenceFile = m3.replaceFirst("_");
        referenceFile = Pattern.compile("%").matcher(referenceFile).replaceAll("_");
        return referenceFile;
    }

    /** Set URL Reference Results Directory for subsequent calls to
	 * @link #createReferenceFile(String) or 
	 * @link #checkURLWithReferenceFile(String) */
    public void setReferenceResultsDirectory(String databaseName) {
        this.referenceResultsDirectory = databaseName;
    }

    public String getReferenceResultsDirectory() {
        return referenceResultsDirectory;
    }

    /** Set URL Prefix for all the requests being tested, e.g.
	 * "http://127.0.0.1:8082/exist/xquery" */
    public void setURLPrefix(String baseURL) {
        this.urlPrefix = baseURL;
    }

    public String getURLPrefix() {
        return urlPrefix;
    }

    /** set the Source Directory for the XQuery sources 
	 * (e.g. $EXIST_HOME/webapp/xquery ) */
    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public void setIgnoredXPath(String ignoredXPath) {
        this.ignoredXPath = ignoredXPath;
    }

    public String getIgnoredXPath() {
        return ignoredXPath;
    }
}
