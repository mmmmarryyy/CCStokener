import java.applet.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

public class HouseSpider extends Applet implements ActionListener, TextListener, ItemListener {

    protected AppletContext appletContext;

    protected TextField inputLine;

    protected URLLabel versionInfo;

    protected Label statusLine;

    protected ImageButton mainButton, helpButton;

    protected SiteList siteList;

    protected I18n i18n;

    private String indexPath, statusLabel, targetFrameHelp, targetFrameSearch;

    private static String fileExclude[], ignoreWords[];

    private static String urlHome = "http://housespider.sourceforge.net/";

    private static String charset = "ISO-8859-1";

    private URL urlStart, urlHelp;

    private static URL urlExclude[];

    private static int debugLevel;

    private static boolean index, useMetaKey, useMetaDesc, showURL, localSearch = false, standAlone = false, keepIdxPages = false, useHttpURLConnection = true;

    private boolean useStatusLine, useVersionInfo, initRun;

    private static int MAXSITES = 10000;

    private static int MAXEXCLUDE = 100;

    private static int MAXIGNORE = 100;

    private int MAXSEARCH = 100;

    private long startTime;

    public static String[] indexPages = { "index.html", "index.htm", "index.html", "index.php", "index.asp", "default.html", "default.htm", "default.shtml", "default.php", "default.asp" };

    public String[][] getParameterInfo() {
        String[][] info = { { "bgcolour", "Color as hex string", "Background colour of applet" }, { "fgcolour", "Color as hex string", "Foreground colour of applet" }, { "bgtextcolour", "Color as hex string", "Background text colour" }, { "textcolour", "Color as hex string", "Text colour" }, { "urlcolour", "Color as hex string", "Colour for URLs" }, { "urlhcolour", "Color as hex string", "Colour for hover URLs" }, { "urlacolour", "Color as hex string", "Colour for active URLs" }, { "fontsize", "Integer", "Font size" }, { "fontname", "String", "Font name" }, { "Lang", "String", "Language for messages (en [default])" }, { "Charset", "String", "Charset for web pages (ISO-8859-1 [default])" }, { "URLStart", "String", "Starting URL for search" }, { "URLExclude", "String", "URLs to be excluded from search" }, { "URLHelp", "String", "URL to documentation" }, { "FileExclude", "String", "Files to be excluded from search" }, { "IgnoreWords", "String", "Words to be ignored in search" }, { "InitInput", "String", "Initial input/search string" }, { "UseMetaKey", "String", "Search meta keywords too (no/yes)" }, { "UseMetaDesc", "String", "Display meta description in result list (no/yes)" }, { "KeepIdxPages", "String", "Keep or remove default index pages from URLs (no/yes)" }, { "ShowURL", "String", "Display the (relative) url in result list (no/yes)" }, { "Target", "String", "The frame into which any page is opened" }, { "TargetHelp", "String", "The frame into which the help page is opened" }, { "TargetSearch", "String", "The frame into which the goto page is opened" }, { "SaveDir", "String", "Directory to save generate index file" }, { "IndexPath", "String", "Relative path to index file (for reading)" }, { "StatusLine", "String", "Statusline mode (no/yes)" }, { "StatusLabel", "String", "Statuslabel mode (no/yes)" }, { "VersionInfo", "String", "Version information (no/yes)" }, { "Action", "String", "Action performed (index [default]/noindex)" }, { "Debug", "String", "Debugging level (0-4)" }, { "MaxSearch", "String", "Maximum number of hits for search" } };
        return info;
    }

    public String getAppletInfo() {
        return "HouseSpider v4.7";
    }

    public void init() {
        appletContext = getAppletContext();
        System.out.println("This is " + getAppletInfo() + ".");
        String s;
        urlExclude = new URL[MAXEXCLUDE];
        fileExclude = new String[MAXEXCLUDE];
        ignoreWords = new String[MAXIGNORE];
        s = getParameter("Debug");
        if (s != null) setDebugLevel(s); else setDebugLevel("0");
        int i;
        int bgColour, fgColour, bgtextColour, textColour, URLColour, URLhColour, URLaColour;
        bgColour = 0xFFFFFF;
        fgColour = 0x000000;
        bgtextColour = 0xFFFFFF;
        textColour = 0x000000;
        URLColour = 0x000000;
        URLhColour = 0x0000FF;
        URLaColour = 0xFF0000;
        s = getParameter("bgcolour");
        if (s != null) {
            try {
                bgColour = Integer.parseInt(s, 16);
            } catch (NumberFormatException nfe) {
                System.out.println("Error, bad applet tag: <param name=\"bgcolour\" value=\"" + s + "\">");
                System.out.println("Using instead: white \"FFFFFF\"");
            }
        }
        s = getParameter("fgcolour");
        if (s != null) {
            try {
                fgColour = Integer.parseInt(s, 16);
            } catch (NumberFormatException nfe) {
                System.out.println("Error, bad applet tag: <param name=\"fgcolour\" value=\"" + s + "\">");
                System.out.println("Using instead: black \"000000\"");
            }
        }
        s = getParameter("bgtextcolour");
        if (s != null) {
            try {
                bgtextColour = Integer.parseInt(s, 16);
            } catch (NumberFormatException nfe) {
                System.out.println("Error, bad applet tag: <param name=\"bgtextcolour\" value=\"" + s + "\">");
                System.out.println("Using instead: white \"FFFFFF\"");
            }
        }
        s = getParameter("textcolour");
        if (s != null) {
            try {
                textColour = Integer.parseInt(s, 16);
            } catch (NumberFormatException nfe) {
                System.out.println("Error, bad applet tag: <param name=\"textcolour\" value=\"" + s + "\">");
                System.out.println("Using instead: black \"000000\"");
            }
        }
        s = getParameter("urlcolour");
        if (s != null) {
            try {
                URLColour = Integer.parseInt(s, 16);
            } catch (NumberFormatException nfe) {
                System.out.println("Error, bad applet tag: <param name=\"urlcolour\" value=\"" + s + "\">");
                System.out.println("Using instead: white \"FFFFFF\"");
            }
        }
        s = getParameter("urlhcolour");
        if (s != null) {
            try {
                URLhColour = Integer.parseInt(s, 16);
            } catch (NumberFormatException nfe) {
                System.out.println("Error, bad applet tag: <param name=\"urlhcolour\" value=\"" + s + "\">");
                System.out.println("Using instead: white \"FFFFFF\"");
            }
        }
        s = getParameter("urlacolour");
        if (s != null) {
            try {
                URLaColour = Integer.parseInt(s, 16);
            } catch (NumberFormatException nfe) {
                System.out.println("Error, bad applet tag: <param name=\"urlacolour\" value=\"" + s + "\">");
                System.out.println("Using instead: white \"FFFFFF\"");
            }
        }
        s = getParameter("fontsize");
        int fsize = 12;
        if (s != null) {
            try {
                fsize = Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                System.out.println("Error, bad applet tag: <param name=\"fontsize\" value=\"" + s + "\">");
                System.out.println("Using instead: \"12\"");
            }
        }
        s = getParameter("fontname");
        String fname = "Dialog";
        if (s != null) {
            fname = s;
        }
        s = getParameter("Lang");
        if (s != null) {
            i18n.setLang(s);
        }
        s = getParameter("Charset");
        if (s != null) {
            if (java.nio.charset.Charset.isSupported(s)) {
                charset = s;
            } else {
                System.out.println("Error, unsupported charset: \"" + s + "\"");
                System.out.println("Using instead:  \"" + charset + "\" (default)");
            }
        }
        useStatusLine = true;
        s = getParameter("StatusLine");
        if (s != null) {
            if (s.equals("yes")) useStatusLine = true; else if (s.equals("no")) useStatusLine = false; else {
                System.out.println("Error, bad applet tag: <param name=\"StatusLine\" value=\"" + s + "\">");
                System.out.println("Using instead:  \"yes\"");
            }
        }
        statusLabel = i18n.getStatus() + ": ";
        s = getParameter("StatusLabel");
        if (s != null) {
            if (s.equals("no")) statusLabel = ""; else if (!s.equals("yes")) {
                System.out.println("Error, bad applet tag: <param name=\"StatusLabel\" value=\"" + s + "\">");
                System.out.println("Using instead:  \"yes\"");
            }
        }
        useVersionInfo = true;
        s = getParameter("VersionInfo");
        if (s != null) {
            if (s.equals("yes")) useVersionInfo = true; else if (s.equals("no")) useVersionInfo = false; else {
                System.out.println("Error, bad applet tag: <param name=\"VersionInfo\" value=\"" + s + "\">");
                System.out.println("Using instead:  \"yes\"");
            }
        }
        index = true;
        s = getParameter("Action");
        if (s != null) {
            if (s.equals("index")) index = true; else if (s.equals("noindex")) index = false; else {
                System.out.println("Error, bad applet tag: <param name=\"Action\" value=\"" + s + "\">");
                System.out.println("Using instead:  \"index\"");
            }
        }
        s = getParameter("KeepIdxPages");
        if (s != null) {
            if (s.equals("yes")) keepIdxPages = true; else if (s.equals("no")) keepIdxPages = false; else {
                System.out.println("Error, bad applet tag: <param name=\"KeepIdxPages\" value=\"" + s + "\">");
                System.out.println("Using instead:  \"no\"");
            }
        }
        useMetaKey = true;
        s = getParameter("UseMetaKey");
        if (s != null) {
            if (s.equals("yes")) useMetaKey = true; else if (s.equals("no")) useMetaKey = false; else {
                System.out.println("Error, bad applet tag: <param name=\"UseMetaKey\" value=\"" + s + "\">");
                System.out.println("Using instead:  \"yes\"");
            }
        }
        useMetaDesc = false;
        s = getParameter("UseMetaDesc");
        if (s != null) {
            if (s.equals("yes")) useMetaDesc = true; else if (s.equals("no")) useMetaDesc = false; else {
                System.out.println("Error, bad applet tag: <param name=\"UseMetaDesc\" value=\"" + s + "\">");
                System.out.println("Using instead:  \"no\"");
            }
        }
        showURL = false;
        s = getParameter("ShowURL");
        if (s != null) {
            if (s.equals("yes")) showURL = true; else if (s.equals("no")) showURL = false; else {
                System.out.println("Error, bad applet tag: <param name=\"ShowURL\" value=\"" + s + "\">");
                System.out.println("Using instead:  \"no\"");
            }
        }
        Colouriser colouriser = new Colouriser();
        colouriser.setColours(bgColour, fgColour);
        Image[] mainButtonUpImage = new Image[7];
        Image[] mainButtonDownImage = new Image[3];
        Image mainButtonDisabledImage;
        mainButtonUpImage[0] = colourise(colouriser, ImageLoader.getImage("go.up.gif"));
        mainButtonDownImage[0] = colourise(colouriser, ImageLoader.getImage("go.down.gif"));
        mainButtonUpImage[1] = colourise(colouriser, ImageLoader.getImage("search.up.gif"));
        mainButtonDownImage[1] = colourise(colouriser, ImageLoader.getImage("search.down.gif"));
        mainButtonUpImage[2] = colourise(colouriser, ImageLoader.getImage("stop.up1.gif"));
        mainButtonUpImage[3] = colourise(colouriser, ImageLoader.getImage("stop.up2.gif"));
        mainButtonUpImage[4] = colourise(colouriser, ImageLoader.getImage("stop.up3.gif"));
        mainButtonUpImage[5] = colourise(colouriser, ImageLoader.getImage("stop.up4.gif"));
        mainButtonUpImage[6] = colourise(colouriser, ImageLoader.getImage("stop.up5.gif"));
        mainButtonDownImage[2] = colourise(colouriser, ImageLoader.getImage("stop.down.gif"));
        mainButtonDisabledImage = colourise(colouriser, ImageLoader.getImage("main.disabled.gif"));
        Image[] helpButtonUpImage = new Image[1];
        Image[] helpButtonDownImage = new Image[1];
        Image helpButtonDisabledImage;
        helpButtonUpImage[0] = colourise(colouriser, ImageLoader.getImage("help.up.gif"));
        helpButtonDownImage[0] = colourise(colouriser, ImageLoader.getImage("help.down.gif"));
        helpButtonDisabledImage = colourise(colouriser, ImageLoader.getImage("help.disabled.gif"));
        inputLine = new TextField();
        inputLine.setBackground(new Color(bgtextColour));
        inputLine.setForeground(new Color(textColour));
        mainButton = new ImageButton(mainButtonUpImage, mainButtonDownImage, mainButtonDisabledImage);
        mainButton.setEnabled(false);
        mainButton.setButton(1);
        mainButton.setAnimation(2, 200);
        helpButton = new ImageButton(helpButtonUpImage, helpButtonDownImage, helpButtonDisabledImage);
        helpButton.setEnabled(true);
        siteList = new SiteList();
        siteList.setBackground(new Color(bgtextColour));
        siteList.setForeground(new Color(textColour));
        setBackground(new Color(bgColour));
        setForeground(new Color(fgColour));
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints gc = new GridBagConstraints();
        setLayout(gridbag);
        gc.insets = new Insets(2, 2, 2, 2);
        gc.gridheight = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridwidth = 2;
        gridbag.setConstraints(inputLine, gc);
        gc.fill = GridBagConstraints.NONE;
        gc.gridwidth = 1;
        gc.weightx = 0;
        gc.gridx = 2;
        gridbag.setConstraints(mainButton, gc);
        gc.gridwidth = 1;
        gc.gridx = 3;
        gridbag.setConstraints(helpButton, gc);
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridwidth = 4;
        gc.gridx = 0;
        gc.gridy = 1;
        gridbag.setConstraints(siteList, gc);
        gc.fill = GridBagConstraints.NONE;
        gc.weighty = 0;
        gc.gridy = 2;
        if (useVersionInfo) {
            versionInfo = new URLLabel(this, urlHome, getAppletInfo());
            versionInfo.setBackground(new Color(bgColour));
            versionInfo.setColor("link", new Color(URLColour));
            versionInfo.setColor("hover", new Color(URLhColour));
            versionInfo.setColor("active", new Color(URLaColour));
            gc.gridwidth = 1;
            gc.weightx = 0;
            gc.gridx = 0;
            gridbag.setConstraints(versionInfo, gc);
            gc.gridwidth = 3;
            gc.gridx = 1;
        } else {
            gc.gridwidth = 4;
            gc.gridx = 0;
        }
        if (useStatusLine) {
            statusLine = new Label();
            statusLine.setBackground(new Color(bgColour));
            statusLine.setForeground(new Color(fgColour));
            gc.weightx = 1;
            gc.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(statusLine, gc);
        }
        if (fsize != 12 || !fname.equals("Dialog")) {
            Font newfont = new Font(fname, Font.PLAIN, fsize);
            inputLine.setFont(newfont);
            siteList.setFont(newfont);
            if (useStatusLine) statusLine.setFont(newfont);
            if (useVersionInfo) versionInfo.setFont(new Font(fname, Font.BOLD, fsize));
        }
        add(inputLine);
        add(mainButton);
        add(helpButton);
        add(siteList);
        if (useVersionInfo) {
            add(versionInfo);
        }
        if (useStatusLine) {
            add(statusLine);
        }
        inputLine.addTextListener(this);
        inputLine.addActionListener(this);
        mainButton.addActionListener(this);
        helpButton.addActionListener(this);
        siteList.addActionListener(this);
        siteList.addItemListener(this);
        inputLine.requestFocus();
        s = getParameter("MaxSearch");
        if (s != null) try {
            MAXSEARCH = Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            System.out.println("Error, bad applet tag: <param name=\"MaxSearch\" value=\"" + s + "\">");
            System.out.println("Using instead: \"100\"");
        }
        siteList.setMAX_SEARCH(MAXSEARCH);
        urlStart = getDocumentBase();
        s = getParameter("URLStart");
        if (standAlone) {
            if (s != null) {
                try {
                    urlStart = new URL(s);
                } catch (MalformedURLException mue1) {
                    System.out.println("Fatal error, value for \"URLStart\" is not a valid URL.");
                    System.out.println("Did you give a complete path?");
                }
            } else {
                System.out.println("Fatal error, \"URLStart\" must be given when running as stand-alone application.");
            }
            if (urlStart == null) {
                System.out.println("Exiting.");
                System.exit(0);
            }
        }
        if (s != null) {
            try {
                urlStart = new URL(urlStart, s);
                String urlStartString = urlStart.toString();
                if (!URLExists(urlStart)) {
                    if (standAlone) {
                        System.out.println("Fatal error, \"URLStart\" doesn't seem to exist.");
                        System.out.println("Exiting.");
                        System.exit(0);
                    } else {
                        System.out.println("Suggested URLStart not found.");
                        System.out.println("-> Ignoring URLStart - using document base in stead.");
                        urlStart = getDocumentBase();
                    }
                } else if (urlStartString.substring(0, urlStartString.lastIndexOf('/') + 1).equals("http://")) {
                    urlStart = new URL(urlStartString + "/");
                } else {
                    urlStart = fixIndexPages(urlStart);
                    urlStart = fixTrailingSlash(urlStart);
                }
            } catch (MalformedURLException mue1) {
                System.out.println("Error, bad applet tag: <param name=\"URLStart\" value=\"" + s + "\">");
                System.out.println("-> URLStart is not a valid URL - using document base in stead.");
                urlStart = getDocumentBase();
            } catch (IOException ioe) {
                System.out.println("Couldn't open connection to suggested \"URLStart\".");
                System.out.println("-> Ignoring URLStart - using document base in stead.");
                urlStart = getDocumentBase();
            } catch (RuntimeException re) {
                urlStart = getDocumentBase();
                System.out.println("Fatal runtime error, normally because access to \"URLStart\" is denied.");
                System.out.println("Check that \"URLStart\" and the applet is on the same domain/path.");
                if (debugLevel >= 4) {
                    re.printStackTrace();
                }
                if (standAlone) {
                    System.exit(0);
                } else {
                    setStatusLine("===> FATAL ERROR - check Java Console for messages <===");
                    useStatusLine = false;
                    inputLine.setEnabled(false);
                    mainButton.setEnabled(false);
                }
            }
        }
        if ((urlStart.toString()).startsWith("file")) {
            localSearch = true;
            keepIdxPages = true;
            System.out.println("Doing a local (file) search.");
        }
        siteList.setURLStart(urlStart);
        try {
            urlHelp = new URL("http://housespider.sourceforge.net/doc/ver47/info.html");
        } catch (MalformedURLException mue2) {
        }
        s = getParameter("URLHelp");
        if (s != null) {
            try {
                urlHelp = new URL(urlStart, s);
            } catch (MalformedURLException mue3) {
                System.out.println("Error, bad applet tag: <param name=\"URLHelp\" value=\"" + s + "\">");
                System.out.println("-> URLHelp is not a valid URL.");
                helpButton.setEnabled(false);
            }
        }
        s = getParameter("URLExclude");
        if (s != null) {
            if (s.length() > 0) {
                try {
                    int num = 1, pos = 0, prev = -1;
                    String w;
                    while ((pos = s.indexOf(',', pos)) != -1) {
                        num++;
                        pos++;
                    }
                    if (num > MAXEXCLUDE) {
                        num = MAXEXCLUDE;
                        System.out.println("Warning, too many excluded URLs given.");
                    }
                    for (i = 0; i < num; i++) {
                        pos = s.indexOf(',', ++prev);
                        w = s.substring(prev, pos == -1 ? s.length() : pos);
                        urlExclude[i] = new URL(urlStart, w.trim());
                        prev = pos;
                    }
                } catch (MalformedURLException mue4) {
                    System.out.println("Error, bad applet tag: <param name=\"URLExclude\" value=\"" + s + "\">");
                }
            } else System.out.println("Warning, empty name attribute for applet tag \"URLExclude\".");
        }
        s = getParameter("FileExclude");
        if (s != null) {
            if (s.length() > 0) {
                int num = 1, pos = 0, prev = -1;
                String w;
                while ((pos = s.indexOf(',', pos)) != -1) {
                    num++;
                    pos++;
                }
                if (num > MAXEXCLUDE) {
                    num = MAXEXCLUDE;
                    System.out.println("Warning, too many excluded files given.");
                }
                for (i = 0; i < num; i++) {
                    pos = s.indexOf(',', ++prev);
                    w = s.substring(prev, pos == -1 ? s.length() : pos);
                    fileExclude[i] = new String(w.trim().toLowerCase());
                    prev = pos;
                }
            } else System.out.println("Warning, empty name attribute for applet tag \"FileExclude\".");
        }
        s = getParameter("IgnoreWords");
        if (s != null) {
            if (s.length() > 0) {
                int num = 1, pos = 0, prev = -1;
                String w;
                while ((pos = s.indexOf(',', pos)) != -1) {
                    num++;
                    pos++;
                }
                if (num > MAXIGNORE) {
                    num = MAXIGNORE;
                    System.out.println("Warning, too many excluded files given.");
                }
                for (i = 0; i < num; i++) {
                    pos = s.indexOf(',', ++prev);
                    w = s.substring(prev, pos == -1 ? s.length() : pos);
                    ignoreWords[i] = new String(w.trim().toLowerCase());
                    prev = pos;
                }
            } else System.out.println("Warning, empty name attribute for applet tag \"IgnoreWords\".");
        }
        s = getParameter("Target");
        if (s != null) {
            targetFrameHelp = s;
            targetFrameSearch = s;
        } else {
            targetFrameHelp = "_self";
            targetFrameSearch = "_self";
        }
        s = getParameter("TargetHelp");
        if (s != null) targetFrameHelp = s;
        s = getParameter("TargetSearch");
        if (s != null) targetFrameSearch = s;
        s = getParameter("SaveDir");
        if (s != null) siteList.setSaveDir(s); else siteList.setSaveDir("");
        indexPath = "";
        s = getParameter("IndexPath");
        if (s != null) {
            if (!s.endsWith("/")) {
                indexPath = s + "/";
            } else {
                indexPath = s;
            }
            siteList.setIndexPath(indexPath);
        } else siteList.setIndexPath("");
        if (useStatusLine) {
            boolean indexExists = false;
            try {
                URL indexFileZip = new URL(urlStart, indexPath + "HouseSpider.index.zip");
                URL indexFile = new URL(urlStart, indexPath + "HouseSpider.index");
                if (URLExists(indexFileZip)) {
                    indexExists = true;
                } else {
                    indexExists = URLExists(indexFile);
                }
                URL logFile = new URL(urlStart, indexPath + "HouseSpider.log");
                if (indexExists) {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(logFile.openStream(), "UTF-8"));
                        setStatusLine(in.readLine());
                        in.close();
                    } catch (IOException ioe) {
                        setStatusLine(i18n.getLogNotFound());
                    }
                } else {
                    setStatusLine(i18n.getIndexNotFound());
                }
            } catch (MalformedURLException mue) {
            }
        }
        if (!localSearch) {
            HttpURLConnection conn;
            try {
                URL test = urlStart;
                conn = (HttpURLConnection) test.openConnection();
                conn.setRequestMethod("HEAD");
                if (conn.getResponseCode() == -1) {
                    useHttpURLConnection = false;
                    System.out.println("Warning, bad HTTP response codes by the JVM.");
                    System.out.println("-> HouseSpider may not detect all redirects and not founds");
                }
                conn.disconnect();
            } catch (ClassCastException cce) {
                useHttpURLConnection = false;
                System.out.println("Warning, HttpURLConnection is not supported by the JVM.");
                System.out.println("-> HouseSpider may not detect all redirects and not founds");
            } catch (MalformedURLException mue) {
            } catch (IOException ioe) {
                System.out.println("Warning, couldn't test if HttpURLConnection is supported by the JVM.");
            }
        }
        initRun = true;
        s = getParameter("InitInput");
        if (s != null) {
            if (s.startsWith("wait:")) {
                s = s.substring(5, s.length());
                initRun = false;
            }
            inputLine.setText(s);
        }
        if (debugLevel >= 2) {
            System.out.println("URLStart: " + urlStart.toString());
            System.out.println("URLHelp: " + urlHelp.toString());
            i = 0;
            while (urlExclude[i] != null && i < MAXEXCLUDE) {
                System.out.println("URLExclude[" + i + "]: " + urlExclude[i]);
                i++;
            }
            if (i == 0) {
                System.out.println("URLExclude: Not used.");
            }
            i = 0;
            while (fileExclude[i] != null && i < MAXEXCLUDE) {
                System.out.println("FileExclude[" + i + "]: " + fileExclude[i]);
                i++;
            }
            if (i == 0) {
                System.out.println("FileExclude: Not used.");
            }
        }
    }

    public void start() {
        String search = inputLine.getText();
        validate();
        if (search.length() > 0 && initRun) {
            startTime = System.currentTimeMillis();
            siteList.search(search, this);
        }
        initRun = true;
    }

    public void stop() {
        stopSearch();
        mainButton.setEnabled(false);
        siteList.deselect(siteList.getSelectedIndex());
    }

    public void destroy() {
        removeAll();
        appletContext = null;
        inputLine = null;
        mainButton = null;
        helpButton = null;
        siteList = null;
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (e.getSource() == helpButton) {
            appletContext.showDocument(urlHelp, targetFrameHelp);
        } else if ((e.getSource() == siteList)) {
            appletContext.showDocument(siteList.getSelectedURL(), targetFrameSearch);
        } else if (e.getSource() == mainButton) {
            if (cmd.equals("2")) {
                stopSearch();
            } else if (cmd.equals("1")) {
                startSearch();
            } else if (cmd.equals("0")) {
                appletContext.showDocument(siteList.getSelectedURL(), targetFrameSearch);
            }
        } else if (e.getSource() == inputLine) startSearch();
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            showStatus(siteList.getSelectedURL().toString());
            mainButton.setEnabled(true);
            mainButton.setButton(0);
        } else if (siteList.getSelectedIndex() < 0) {
            showStatus("");
            mainButton.setEnabled(false);
        }
    }

    public void textValueChanged(TextEvent e) {
        mainButton.setButton(1);
        siteList.deselect(siteList.getSelectedIndex());
        showStatus("");
        if (inputLine.getText().length() > 0) mainButton.setEnabled(true); else mainButton.setEnabled(false);
    }

    private void startSearch() {
        String search = inputLine.getText();
        if (search.length() > 0) {
            inputLine.setEnabled(false);
            mainButton.setEnabled(true);
            mainButton.setButton(2);
            helpButton.setEnabled(false);
            siteList.setEnabled(false);
            startTime = System.currentTimeMillis();
            System.out.println("Search started.");
            siteList.search(search, this);
        }
    }

    public void searchDone() {
        inputLine.setEnabled(true);
        mainButton.setEnabled(false);
        helpButton.setEnabled(true);
        siteList.setEnabled(true);
        System.out.println("Search finished.");
        if (debugLevel >= 1) {
            startTime = System.currentTimeMillis() - startTime;
            System.out.println("Search duration: " + startTime + " (ms).");
        }
    }

    private void stopSearch() {
        siteList.stopSearch();
    }

    private Image colourise(Colouriser colouriser, Image sourceImage) {
        ImageProducer filter;
        filter = new FilteredImageSource(sourceImage.getSource(), colouriser);
        Image filteredImage = createImage(filter);
        MediaTracker tracker = new MediaTracker(this);
        tracker.addImage(filteredImage, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ie) {
        }
        if (tracker.isErrorAny() == true) System.out.println("Error, loading \"button\" images used by applet.");
        return (filteredImage);
    }

    public static void setDebugLevel(String level) {
        if (level.equals("0")) debugLevel = 0; else if (level.equals("1")) debugLevel = 1; else if (level.equals("2")) debugLevel = 2; else if (level.equals("3")) debugLevel = 3; else if (level.equals("4")) debugLevel = 4; else {
            System.out.println("Error, bad applet tag: <param name=\"Debug\" value=\"" + level + "\">");
            System.out.println("Using instead: 0");
            debugLevel = 0;
        }
    }

    public static String getCharset() {
        return charset;
    }

    public static boolean getDoIndex() {
        return index;
    }

    public static int getDebugLevel() {
        return debugLevel;
    }

    public static int getMAXSITES() {
        return MAXSITES;
    }

    public static int getMAXEXCLUDE() {
        return MAXEXCLUDE;
    }

    public static URL[] getURLExclude() {
        return urlExclude;
    }

    public static String[] getFileExclude() {
        return fileExclude;
    }

    public static int getMAXIGNORE() {
        return MAXIGNORE;
    }

    public static String[] getIgnoreWords() {
        return ignoreWords;
    }

    public void setStatusLine(String status) {
        int verswidth = 0;
        if (useStatusLine) statusLine.setText(statusLabel + status);
        if (useVersionInfo) {
            verswidth = versionInfo.getSize().width;
        }
        statusLine.setSize(this.getSize().width - verswidth, statusLine.getSize().height);
    }

    public static boolean getUseMetaKey() {
        return useMetaKey;
    }

    public static boolean getUseMetaDesc() {
        return useMetaDesc;
    }

    public static boolean getKeepIdxPages() {
        return keepIdxPages;
    }

    public static boolean getShowURL() {
        return showURL;
    }

    /** URLExists checks if an URL exists using the response code
     * from an HttpURLConnection if possible, otherwise just opens
     * a stream.
     * @return boolean 
     */
    public static boolean URLExists(URL url) {
        int responseCode = -1;
        boolean exists = true;
        try {
            if (useHttpURLConnection && url.getProtocol().equals("http")) {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                responseCode = conn.getResponseCode();
                if (!(responseCode >= 200 && responseCode < 400)) exists = false;
                conn.disconnect();
            } else {
                InputStream testStream = url.openStream();
            }
        } catch (IOException ioe) {
            exists = false;
        }
        return exists;
    }

    /** fixIndexPages returns an URL with default index pages
     * stripped of the end IF keepIdxPages is false.
     * @return URL 
     */
    public static URL fixIndexPages(URL inURL) {
        boolean stripURL = false;
        String strippedURL = inURL.toString();
        if (!strippedURL.endsWith("/") && !keepIdxPages) {
            for (int i = 0; i < indexPages.length; i++) {
                if (strippedURL.endsWith(indexPages[i])) {
                    stripURL = true;
                    break;
                }
            }
        }
        if (stripURL) {
            strippedURL = strippedURL.substring(0, strippedURL.lastIndexOf('/') + 1);
            try {
                URL newURL = new URL(strippedURL);
                if (URLExists(newURL)) return newURL;
            } catch (MalformedURLException mue) {
            }
        }
        return inURL;
    }

    /** fixTrailingSlash returns an URL with missing trailing slashes
     * added. Uses HttpURLConnection if possible, otherwise tries to do
     * clever tricks based on the URL. Does nothing for local files.
     * @return URL 
     */
    public static URL fixTrailingSlash(URL inURL) {
        if (!inURL.toString().endsWith("/") && !localSearch) {
            try {
                if (useHttpURLConnection) {
                    try {
                        HttpURLConnection conn = (HttpURLConnection) inURL.openConnection();
                        conn.setInstanceFollowRedirects(false);
                        conn.setRequestMethod("HEAD");
                        if (conn.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM) return new URL(conn.getHeaderField("Location"));
                        conn.disconnect();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                } else {
                    String inURLString = inURL.toString();
                    if (inURLString.lastIndexOf('.') <= inURLString.lastIndexOf('/')) {
                        URL newURL = new URL(inURLString.concat("/"));
                        if (URLExists(newURL)) return newURL;
                    }
                }
            } catch (MalformedURLException mue) {
            }
        }
        return inURL;
    }

    /**
     * Standalone application support (using HouseSpiderStub).
     */
    public static void main(String argv[]) {
        standAlone = true;
        final HouseSpider applet = new HouseSpider();
        Frame frame = new Frame("HouseSpider");
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent event) {
                applet.stop();
                applet.destroy();
                System.exit(0);
            }
        });
        frame.add("Center", applet);
        applet.setStub(new HouseSpiderStub(argv, applet));
        frame.setSize(500, 300);
        frame.setVisible(true);
        applet.init();
        applet.start();
        frame.pack();
    }
}
