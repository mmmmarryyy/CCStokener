package org.eiichiro.jazzmaster.examples.petstore.search;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import org.eiichiro.jazzmaster.examples.petstore.util.PetstoreUtil;

/**
 * This class can crawl a web site indexing appropriate data as best as possible
 * The best way to make use page is indexed with the correct info is to use meta tags in each page
 * and the robots.txt file to index only to appropriate pages
 *
 * @author basler
 */
public class HTMLParser {

    private static final boolean bDebug = false;

    public static void main(String[] args) {
        HTMLParser hp = new HTMLParser();
        hp.runWeb("http://localhost:8080", "petstore", "main.screen");
    }

    /** Creates a new instance of HTMLParser */
    public HTMLParser() {
    }

    public void runWeb(String beginURL, String contextRoot, String pageURI) {
        if (bDebug) System.out.println("WEB Path");
        List<String> vtURLs = new ArrayList<String>();
        List<String> vtRobots = getRobots(beginURL, contextRoot);
        vtURLs.add("/" + contextRoot + "/" + pageURI);
        Indexer indexer = null;
        IndexDocument indexDoc = null;
        try {
            indexer = new Indexer("/tmp/tmp/index");
            for (String sxURL : vtURLs) {
                if (bDebug) System.out.println("\n\n*** INDEXING " + sxURL);
                if (bDebug) System.out.println("Have - " + sxURL);
                boolean bIndexPage = true;
                if (vtRobots != null) {
                    for (String sxRobotURL : vtRobots) {
                        if (bDebug) System.out.println("Comparing to - " + sxRobotURL);
                        if (sxURL.startsWith(sxRobotURL)) {
                            if (bDebug) System.out.println("Found URL - " + sxRobotURL + " - " + sxURL);
                            bIndexPage = false;
                            break;
                        }
                    }
                } else {
                    bIndexPage = true;
                }
                sxURL = beginURL + sxURL;
                ParserDelegator pd = new ParserDelegator();
                CallbackHandler cb = new CallbackHandler(vtURLs, bIndexPage, contextRoot);
                try {
                    URLConnection urlConn = new URL(sxURL).openConnection();
                    urlConn.setUseCaches(false);
                    Date modDate = new Date(urlConn.getLastModified());
                    if (bDebug) System.out.println("\nMatch - " + sxURL + " - Modified Date - " + modDate);
                    BufferedReader bfReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                    pd.parse(bfReader, cb, true);
                    if (bIndexPage) {
                        if (bDebug) System.out.println("Adding Index - " + sxURL + "\nContent:" + cb.getText() + "\nSummary:" + cb.getSummary() + "\nTitle:" + cb.getTitle());
                        indexDoc = new IndexDocument();
                        indexDoc.setUID(sxURL + modDate.toString());
                        indexDoc.setPageURL(sxURL);
                        indexDoc.setModifiedDate(modDate.toString());
                        indexDoc.setContents(cb.getText());
                        indexDoc.setTitle(cb.getTitle());
                        indexDoc.setSummary(cb.getSummary());
                        indexer.addDocument(indexDoc);
                    }
                } catch (Exception ee) {
                    PetstoreUtil.getLogger().log(Level.SEVERE, "Inner Exception" + ee);
                }
            }
        } catch (Exception e) {
            PetstoreUtil.getLogger().log(Level.SEVERE, "Outer Exception" + e);
        } finally {
            try {
                if (indexer != null) {
                    indexer.close();
                }
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
    }

    private List<String> getRobots(String beginURL, String contextRoot) {
        List<String> vtRobots = new ArrayList<String>();
        BufferedReader bfReader = null;
        try {
            URL urlx = new URL(beginURL + "/" + contextRoot + "/" + "robots.txt");
            URLConnection urlConn = urlx.openConnection();
            urlConn.setUseCaches(false);
            bfReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String sxLine = "";
            while ((sxLine = bfReader.readLine()) != null) {
                if (sxLine.startsWith("Disallow:")) {
                    vtRobots.add(sxLine.substring(10));
                }
            }
        } catch (Exception e) {
            PetstoreUtil.getLogger().log(Level.SEVERE, "Exception" + e);
            vtRobots = null;
        } finally {
            try {
                if (bfReader != null) {
                    bfReader.close();
                }
            } catch (Exception ee) {
            }
        }
        return vtRobots;
    }

    private class CallbackHandler extends HTMLEditorKit.ParserCallback {

        private String beginURL, contextRoot;

        private List<String> vtURLs;

        private StringBuffer sbText = new StringBuffer();

        private StringBuffer sbTitle = new StringBuffer();

        private StringBuffer sbSummary = new StringBuffer();

        private int iSummaryMax = 200;

        private boolean bSummary = false, bIndexPage = false;

        private String tag = null;

        CallbackHandler(List<String> vtURLs, boolean bIndexPage, String contextRoot) {
            super();
            this.contextRoot = contextRoot;
            this.bIndexPage = bIndexPage;
            this.vtURLs = vtURLs;
        }

        @Override
        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if (bIndexPage) {
                if (t.toString().toLowerCase().equals("meta")) {
                    String sxName = ((String) a.getAttribute(HTML.Attribute.NAME));
                    if (sxName != null) {
                        sxName = sxName.toLowerCase();
                        if (sxName.equals("summary") || sxName.equals("description")) {
                            String sxContent = ((String) a.getAttribute(HTML.Attribute.CONTENT));
                            bSummary = true;
                            if (sbSummary.length() < iSummaryMax) {
                                if (bDebug) System.out.println("add summary - " + sxContent);
                                sbSummary.append(sxContent);
                            }
                            sbText.append(sxContent);
                        } else if (sxName.equals("keywords")) {
                            sbText.append(((String) a.getAttribute(HTML.Attribute.CONTENT)));
                        }
                    }
                }
            }
        }

        @Override
        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            tag = t.toString().toLowerCase();
            if (tag.equals("a")) {
                String sxURL = (String) a.getAttribute(HTML.Attribute.HREF);
                if (sxURL != null) {
                    if (!sxURL.startsWith("http") && !sxURL.startsWith("#")) {
                        int iPos1 = sxURL.indexOf("#");
                        if (iPos1 != -1) {
                            sxURL = sxURL.substring(0, iPos1);
                        }
                        iPos1 = sxURL.indexOf(";");
                        int iPos2 = sxURL.indexOf("?");
                        if (iPos1 > -1 && iPos2 > -1 && iPos2 > iPos1) {
                            sxURL = sxURL.substring(0, iPos1) + sxURL.substring(iPos2);
                        }
                        if (!sxURL.startsWith("/")) {
                            sxURL = "/" + contextRoot + "/" + sxURL;
                        }
                        if (!vtURLs.contains(sxURL)) {
                            if (bDebug) System.out.println(">>> Adding URL = " + sxURL);
                            vtURLs.add(sxURL);
                        }
                    }
                }
            }
        }

        @Override
        public void handleEndTag(HTML.Tag t, int pos) {
            tag = null;
        }

        @Override
        public void handleText(char[] data, int pos) {
            if (bIndexPage) {
                String cleanData = cleanParseData(data);
                if (cleanData != null) {
                    if (bDebug) System.out.println("Tag - Text - " + tag + " - -->" + cleanData + "<--");
                    if (tag == null || (!tag.equals("title") && !tag.equals("a") && !tag.equals("style"))) {
                        sbText.append(cleanData);
                        sbText.append(" ");
                        if (!bSummary && sbSummary.length() < iSummaryMax) {
                            sbSummary.append(cleanData.substring(0, cleanData.length() > iSummaryMax ? iSummaryMax - sbSummary.length() : cleanData.length()));
                            sbSummary.append(" ");
                        }
                    } else if (tag != null && tag.equals("title")) {
                        sbTitle.append(cleanData);
                        sbTitle.append(" ");
                    }
                }
            }
        }

        public String cleanParseData(char[] data) {
            String sxTemp = new String(data).trim();
            if (sxTemp.length() < 2) {
                sxTemp = null;
            }
            return sxTemp;
        }

        public String getText() {
            return sbText.toString();
        }

        public Reader getTextReader() {
            return new StringReader(sbText.toString());
        }

        public String getTitle() {
            return sbTitle.toString();
        }

        public String getSummary() {
            if (sbSummary.length() < 1) {
                return sbTitle.toString();
            } else {
                return sbSummary.toString();
            }
        }
    }
}
