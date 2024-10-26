package com.jeffvannest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.cyberneko.html.parsers.DOMParser;
import com.jeffvannest.exception.MySpaceBlogExporterException;
import com.jeffvannest.util.FileUtil;
import com.jeffvannest.util.Rfc822Util;
import com.jeffvannest.util.XMLUtil;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import generated.*;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import org.w3c.dom.Document;

/**
 *
 * @author Jeff Vannest
 */
public class MySpaceBlogExporter extends Thread {

    private String blogUrl;

    private String rssLink;

    private ArrayList<String> chanCats;

    private ArrayList<String> messages;

    private Properties props;

    private final String propsFile = "MySpaceBlogExporter.properties";

    private static String defaultLog = "logs/MySpaceBlogExporter.log";

    private String loggingLevel = "debug";

    private List<String> appenders = Arrays.asList(loggingLevel, "STDOUT", "FILE");

    private Properties loggerProps = null;

    private ArrayList<String> logOutput = new ArrayList<String>();

    private static Logger logger = Logger.getLogger(MySpaceBlogExporter.class);

    private String nowStr = null;

    private File outputFile = null;

    private String outputType = null;

    /**
     *
     */
    public MySpaceBlogExporter() {
        setDefaultLoggerProps();
    }

    private void addChanCat(String category) {
        if (chanCats == null) {
            chanCats = new ArrayList<String>();
        }
        if (!chanCats.contains(category) && !category.equals("")) {
            chanCats.add(category);
        }
    }

    /**
     *
     * @param s
     */
    public void addLogMsg(String s) {
        if (getLogOutput() == null) {
            setLogOutput(new ArrayList<String>());
        }
        getLogOutput().add(s);
        logger.debug(s);
    }

    /**
     *
     */
    public void doExit() {
        if (!this.isInterrupted()) {
            this.interrupt();
        }
    }

    private ArrayList<String> getChanCat() {
        return chanCats;
    }

    private String fixMsplinks(String s) throws MySpaceBlogExporterException {
        String link = null;
        try {
            int i = s.indexOf("a href=\"http://www.msplinks.com");
            while (i > -1) {
                int j = s.indexOf("\"", i + 8);
                link = s.substring(i + 8, j);
                URL url;
                addLogMsg("...resolving encoded link " + link + "...");
                url = new URL(link);
                URLConnection con = url.openConnection();
                con.getContentType();
                link = con.getURL().toString();
                String before = s.substring(0, i + 8);
                String after = s.substring(j);
                String firstPart = before + link;
                s = firstPart + after;
                i = s.indexOf("a href=\"http://www.msplinks.com", firstPart.length());
                if (this.isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (MalformedURLException e) {
            throw new MySpaceBlogExporterException("Unable to resolve encoded link " + link + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MySpaceBlogExporterException("Unable to connect to " + link + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new MySpaceBlogExporterException(e.getMessage(), e);
        }
        return s;
    }

    /**
     *
     * @return String
     */
    public String getBlogUrl() {
        return blogUrl;
    }

    private String getLinkByTextPattern(String html, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        String link = null;
        if (matcher.find()) {
            link = matcher.group(1).trim();
        }
        return StringEscapeUtils.unescapeHtml(link);
    }

    /**
     *
     * @return ArrayList<String>
     */
    public ArrayList<String> getLogOutput() {
        return logOutput;
    }

    /**
     *
     * @return Properties
     */
    public Properties getLoggerProps() {
        return loggerProps;
    }

    /**
     *
     * @return String
     */
    public String getLoggingLevel() {
        return loggingLevel;
    }

    /**
     *
     * @return ArrayList<String>
     */
    public ArrayList<String> getMessages() {
        return messages;
    }

    /**
     *
     * @param html
     * @param factGen
     * @param factExp
     * @return Collection<RssItem>
     * @throws MySpaceBlogExporterException
     */
    public Collection<RssItem> getMSBlogsFromPage(String html, ObjectFactory factGen, org.wordpress.export._1.ObjectFactory factExp) throws MySpaceBlogExporterException {
        Collection<RssItem> items = null;
        try {
            items = new ArrayList<RssItem>();
            RssItem item = null;
            DateFormat df1 = new SimpleDateFormat("[dd MMM yyyy | EEEE]");
            DateFormat df2 = new SimpleDateFormat("MMMM dd, yyyy - EEEE");
            DateFormat df3 = new SimpleDateFormat("EEEE, dd/MM/yyyy");
            DateFormat df4 = new SimpleDateFormat("dd MMM yy EEEE");
            DateFormat df5 = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
            org.purl.rss._1_0.modules.content.ObjectFactory factContent = new org.purl.rss._1_0.modules.content.ObjectFactory();
            org.wordpress.export._1_0.excerpt.ObjectFactory factExcerpt = new org.wordpress.export._1_0.excerpt.ObjectFactory();
            String timestamp;
            String title = "";
            String category = "";
            String body = "";
            Pattern pattern = Pattern.compile(props.getProperty("timestampPattern").replaceAll("\"", "\\\""), Pattern.DOTALL);
            Matcher matcherTimestamp = pattern.matcher(html);
            while (matcherTimestamp.find()) {
                String remainder = "";
                timestamp = StringEscapeUtils.unescapeHtml(matcherTimestamp.group(1)).trim();
                remainder = matcherTimestamp.group(2);
                pattern = Pattern.compile(props.getProperty("subjectPattern").replaceAll("\"", "\\\""), Pattern.DOTALL);
                Matcher matcherSubject = pattern.matcher(remainder);
                if (matcherSubject.find()) {
                    title = StringEscapeUtils.unescapeHtml(matcherSubject.group(1)).trim();
                    remainder = matcherSubject.group(2);
                    pattern = Pattern.compile(props.getProperty("categoryPattern").replaceAll("\"", "\\\""), Pattern.DOTALL);
                    Matcher matcherCategory = pattern.matcher(remainder);
                    if (matcherCategory.find()) {
                        category = StringEscapeUtils.unescapeHtml(matcherCategory.group(1)).trim();
                        remainder = matcherCategory.group(2);
                    }
                }
                pattern = Pattern.compile(props.getProperty("bodyPattern").replaceAll("\"", "\\\""), Pattern.DOTALL);
                Matcher matcherBody = pattern.matcher(remainder);
                if (matcherBody.find()) {
                    body = matcherBody.group(1).replaceAll("\n", " ").replaceAll("\r", " ");
                }
                item = factGen.createRssItem();
                addLogMsg("Adding blog \"" + title + "\" on " + timestamp);
                item.getTitleOrDescriptionOrLink().add(factGen.createRssItemTitle(title));
                Date date = null;
                try {
                    date = df1.parse(timestamp);
                } catch (Exception ignore) {
                }
                if (date == null) try {
                    date = df2.parse(timestamp);
                } catch (Exception ignore) {
                }
                if (date == null) try {
                    date = df3.parse(timestamp);
                } catch (Exception ignore) {
                }
                if (date == null) try {
                    date = df4.parse(timestamp);
                } catch (Exception ignore) {
                }
                if (date == null) try {
                    date = df5.parse(timestamp);
                } catch (Exception ignore) {
                }
                item.getTitleOrDescriptionOrLink().add(factGen.createRssItemPubDate(Rfc822Util.getFullDate(date)));
                Category cat1 = factGen.createCategory();
                cat1.setValue("<![CDATA[" + category + "]]>");
                item.getTitleOrDescriptionOrLink().add(cat1);
                Category cat2 = factGen.createCategory();
                cat2.setNicename(getNiceName(category));
                cat2.setValue("<![CDATA[" + category + "]]>");
                cat2.setDomain("category");
                item.getTitleOrDescriptionOrLink().add(cat2);
                addChanCat(category);
                item.getTitleOrDescriptionOrLink().add(factGen.createRssItemDescription(""));
                item.getTitleOrDescriptionOrLink().add(factContent.createEncoded("<![CDATA[" + body + "]]>"));
                item.getTitleOrDescriptionOrLink().add(factExcerpt.createEncoded("<![CDATA[]]>"));
                item.getTitleOrDescriptionOrLink().add(factExp.createPostDate(XMLUtil.getW3CDateTime(date)));
                item.getTitleOrDescriptionOrLink().add(factExp.createPostDateGmt(XMLUtil.getW3CDateTime(date)));
                item.getTitleOrDescriptionOrLink().add(factExp.createCommentStatus("open"));
                item.getTitleOrDescriptionOrLink().add(factExp.createPingStatus("open"));
                item.getTitleOrDescriptionOrLink().add(factExp.createPostName(getNiceName(title)));
                item.getTitleOrDescriptionOrLink().add(factExp.createStatus("publish"));
                item.getTitleOrDescriptionOrLink().add(factExp.createPostParent("0"));
                item.getTitleOrDescriptionOrLink().add(factExp.createMenuOrder("0"));
                item.getTitleOrDescriptionOrLink().add(factExp.createPostType("post"));
                item.getTitleOrDescriptionOrLink().add(factExp.createPostPassword(""));
                item.getTitleOrDescriptionOrLink().add(factExp.createIsSticky(Boolean.FALSE));
                items.add(item);
            }
        } catch (Exception e) {
            throw new MySpaceBlogExporterException("Unable to get blogs from this page: " + e.getMessage(), e);
        }
        return items;
    }

    /**
     *
     * @param name
     * @return String
     */
    public static String getNiceName(String name) {
        CharacterIterator it = new StringCharacterIterator(name.toLowerCase());
        StringBuilder sb = new StringBuilder();
        for (char ch = it.first(); ch != CharacterIterator.DONE; ch = it.next()) {
            if (ch == ' ' || ch == '-' || ch == '_') {
                sb.append('-');
            } else if (("abcdefghijklmnopqrstuvwxyz0123456789").indexOf(ch) > -1) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     *
     * @return File
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     *
     * @return String
     */
    public String getOutputType() {
        return outputType;
    }

    /**
     *
     * @return Properties
     */
    public Properties getProps() {
        return props;
    }

    /**
     * 
     * @param pageUrl
     * @return String
     * @throws MySpaceBlogExporterException
     */
    public String getRssLink(String pageUrl) throws MySpaceBlogExporterException {
        if (rssLink != null) {
            return rssLink;
        }
        try {
            DOMParser parser = new DOMParser();
            parser.parse(pageUrl);
            Document doc = parser.getDocument();
            rssLink = XMLUtil.getLinkByText(doc, props.getProperty("rssLinkText"));
            if (rssLink == null) {
                String html = urlToString(pageUrl);
                if (html.indexOf("profile is private") > 0) {
                    throw new Exception("Only pubic blogs are supported. Update the blog to be public and try again.");
                } else {
                    throw new Exception("Cannot find the '" + props.getProperty("rssLinkText") + "' link on the blog page.");
                }
            }
            addLogMsg("Blog RSS feed located at " + rssLink);
            return rssLink;
        } catch (FileNotFoundException e) {
            throw new MySpaceBlogExporterException("The page link " + pageUrl + " is not valid. Please check the link and try again", e);
        } catch (Exception e) {
            throw new MySpaceBlogExporterException("Unable to get the RSS link from page " + pageUrl + ": " + e.getMessage());
        }
    }

    private static String marshalWp(final Object object) throws MySpaceBlogExporterException {
        StringWriter writer = new StringWriter();
        try {
            ClassLoader cl = MySpaceBlogExporter.class.getClassLoader();
            JAXBContext context = JAXBContext.newInstance("generated:org.purl.dc.elements._1:org.purl.rss._1_0.modules.content:org.wordpress.export._1:org.wordpress.export._1_0.excerpt", cl);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new PrefixMapperImpl());
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(object, writer);
        } catch (Exception e) {
            throw new MySpaceBlogExporterException("Unable to convert the blog to a WordPress object: " + e.getMessage(), e);
        }
        return writer.toString();
    }

    private void printStopMsg() {
        addLogMsg("====================================================");
        addLogMsg("Stopping MySpace Blog Exporter at " + new Date());
        addLogMsg("====================================================");
    }

    /**
     *
     * @return Properties
     * @throws MySpaceBlogExporterException
     */
    public Properties readProps() throws MySpaceBlogExporterException {
        Properties myProps = new Properties();
        FileInputStream fis = null;
        InputStream in = null;
        try {
            fis = new FileInputStream(propsFile);
            myProps.load(fis);
            fis.close();
        } catch (IOException ioe) {
            try {
                in = Thread.currentThread().getContextClassLoader().getResourceAsStream(propsFile);
                myProps.load(in);
                in.close();
            } catch (Exception e) {
                throw new MySpaceBlogExporterException("ERROR: The " + propsFile + " cannot be found.", e);
            }
        } finally {
            try {
                fis.close();
            } catch (Exception ignore) {
            }
            try {
                in.close();
            } catch (Exception ignore) {
            }
        }
        return myProps;
    }

    public void run() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
            nowStr = sdf.format(new Date());
            if (getLoggerProps().get("log4j.appender.FILE.File").equals(defaultLog)) {
                getLoggerProps().remove("log4j.appender.FILE.File");
                getLoggerProps().put("log4j.appender.FILE.File", "logs/MySpaceBlogExporter-" + nowStr + ".log");
            }
            PropertyConfigurator.configure(getLoggerProps());
            addLogMsg("====================================================");
            addLogMsg("Starting MySpace Blog Exporter at " + nowStr);
            addLogMsg("====================================================");
            if (getBlogUrl() == null) {
                throw new MySpaceBlogExporterException("The Blog URL must be set.");
            }
            if (getOutputFile() == null) {
                throw new MySpaceBlogExporterException("The Output File must be set.");
            }
            if (getOutputType() == null) {
                throw new MySpaceBlogExporterException("The Output Type must be set.");
            }
            if (!getOutputFile().getParentFile().exists()) {
                throw new MySpaceBlogExporterException("The location of the Output File does not exist.");
            }
            if (this.isInterrupted()) {
                throw new InterruptedException();
            }
            if (getProps() == null) {
                setProps(readProps());
            }
            addLogMsg("Exporting blog at \"" + getBlogUrl() + "\"");
            addLogMsg("Exporting to file \"" + getOutputFile() + "\"");
            addLogMsg("Exporting to type \"" + getOutputType() + "\"");
            String blogString = toWordPressFile();
            if (this.isInterrupted()) {
                throw new InterruptedException();
            }
            if (getOutputType().equals("blogger")) {
                addLogMsg("Converting to the Blogger format.");
                File file = FileUtil.findFile("wordpress_to_blogger.xslt", "transform");
                if (file == null) {
                    throw new MySpaceBlogExporterException("Cannot locate the Blogger transform file.");
                }
                blogString = XMLUtil.transform(blogString, file.getAbsolutePath());
            }
            if (this.isInterrupted()) {
                throw new InterruptedException();
            }
            addLogMsg("Write the file contents to the file system");
            FileOutputStream fos = new FileOutputStream(getOutputFile());
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            Writer writer = new BufferedWriter(osw);
            writer.append(blogString);
            writer.flush();
            osw.flush();
            fos.flush();
            writer.close();
            osw.close();
            fos.close();
            writer = null;
            osw = null;
            fos = null;
            addLogMsg("Done!");
        } catch (InterruptedException e) {
            addLogMsg("*** ERROR");
            addLogMsg("*** ERROR: The MySpace Blog Exporter thread has been interrupted.");
            addLogMsg("*** ERROR");
        } catch (Exception e) {
            MySpaceBlogExporterException pre = null;
            if (e instanceof MySpaceBlogExporterException) {
                pre = (MySpaceBlogExporterException) e;
            } else {
                pre = new MySpaceBlogExporterException(e.getMessage(), e);
            }
            addLogMsg("*** ERROR");
            addLogMsg("*** ERROR: " + pre.getReportableError());
            addLogMsg("*** ERROR");
        } finally {
            printStopMsg();
        }
    }

    /**
     *
     * @param blogUrl
     */
    public void setBlogUrl(String blogUrl) {
        this.blogUrl = blogUrl;
    }

    private void setDefaultLoggerProps() {
        Properties myLoggerProps = new Properties();
        myLoggerProps.put("log4j.appender.STDOUT", "org.apache.log4j.ConsoleAppender");
        myLoggerProps.put("log4j.appender.STDOUT.Target", "System.out");
        myLoggerProps.put("log4j.appender.STDOUT.layout", "org.apache.log4j.PatternLayout");
        myLoggerProps.put("log4j.appender.STDOUT.layout.ConversionPattern", "%d{MM/dd/yy HH:mm:ss} %m%n");
        myLoggerProps.put("log4j.appender.FILE", "org.apache.log4j.FileAppender");
        myLoggerProps.put("log4j.appender.FILE.File", defaultLog);
        myLoggerProps.put("log4j.appender.FILE.layout", "org.apache.log4j.PatternLayout");
        myLoggerProps.put("log4j.appender.FILE.layout.ConversionPattern", "%d{MM/dd/yy HH:mm:ss} %m%n");
        myLoggerProps.put("log4j.rootLogger", loggingLevel + ", STDOUT, FILE");
        setLoggerProps(myLoggerProps);
    }

    /**
     *
     * @param s
     */
    public void setLogAppenderFile(String s) {
        Properties myLoggerProps = getLoggerProps();
        myLoggerProps.remove("log4j.appender.FILE.File");
        myLoggerProps.put("log4j.appender.FILE.File", s);
        myLoggerProps.remove("log4j.rootLogger");
        myLoggerProps.put("log4j.rootLogger", appenders.toString().replaceFirst("\\[", "").replaceFirst("\\]", ""));
        setLoggerProps(myLoggerProps);
    }

    /**
     *
     */
    public void setLogAppenderFileOnly() {
        appenders = Arrays.asList(loggingLevel, "FILE");
        Properties myLoggerProps = getLoggerProps();
        myLoggerProps.remove("log4j.rootLogger");
        myLoggerProps.put("log4j.rootLogger", appenders.toString().replaceFirst("\\[", "").replaceFirst("\\]", ""));
        setLoggerProps(myLoggerProps);
    }

    /**
     *
     */
    public void setLogAppenderFileAndStdOut() {
        appenders = Arrays.asList(loggingLevel, "STDOUT", "FILE");
        Properties myLoggerProps = getLoggerProps();
        myLoggerProps.remove("log4j.rootLogger");
        myLoggerProps.put("log4j.rootLogger", appenders.toString().replaceFirst("\\[", "").replaceFirst("\\]", ""));
        setLoggerProps(myLoggerProps);
    }

    /**
     *
     */
    public void setLogAppenderStdOutOnly() {
        appenders = Arrays.asList(loggingLevel, "STDOUT");
        Properties myLoggerProps = getLoggerProps();
        myLoggerProps.remove("log4j.rootLogger");
        myLoggerProps.put("log4j.rootLogger", appenders.toString().replaceFirst("\\[", "").replaceFirst("\\]", ""));
        setLoggerProps(myLoggerProps);
    }

    /**
     *
     * @param loggerProps
     */
    public void setLoggerProps(Properties loggerProps) {
        this.loggerProps = loggerProps;
    }

    /**
     *
     * @param loggingLevel
     */
    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
        appenders = Arrays.asList(this.loggingLevel, "STDOUT", "FILE");
        Properties myLoggerProps = getLoggerProps();
        myLoggerProps.remove("log4j.rootLogger");
        myLoggerProps.put("log4j.rootLogger", appenders.toString().replaceFirst("\\[", "").replaceFirst("\\]", ""));
        setLoggerProps(myLoggerProps);
    }

    /**
     *
     * @param logOutput
     */
    public void setLogOutput(ArrayList<String> logOutput) {
        this.logOutput = logOutput;
    }

    /**
     *
     * @param outputFile
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     *
     * @param s
     */
    public void setOutputFile(String s) {
        outputFile = new File(s);
    }

    /**
     *
     * @param outputType
     */
    public void setOutputType(String outputType) throws MySpaceBlogExporterException {
        this.outputType = outputType.toLowerCase();
        if (!outputType.equals("wordpress") && !outputType.equals("blogger")) {
            throw new MySpaceBlogExporterException("The " + outputType + " output type is not supported. Only Wordpress and Blogger are supported.");
        }
    }

    /**
     *
     * @param props
     */
    public void setProps(Properties props) {
        this.props = props;
    }

    private String toWordPressFile() throws MySpaceBlogExporterException, InterruptedException {
        String blogString = null;
        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(getRssLink(getBlogUrl()))));
            if (this.isInterrupted()) {
                throw new InterruptedException();
            }
            ObjectFactory factGen = new ObjectFactory();
            org.wordpress.export._1.ObjectFactory factExp = new org.wordpress.export._1.ObjectFactory();
            Rss rss = factGen.createRss();
            rss.setVersion(new BigDecimal("2.0", new MathContext(2)));
            rss.setChannel(factGen.createRssChannel());
            rss.getChannel().getTitleOrLinkOrDescription().add(factGen.createRssChannelTitle(feed.getTitle()));
            rss.getChannel().getTitleOrLinkOrDescription().add(factGen.createRssChannelLink(feed.getLink()));
            rss.getChannel().getTitleOrLinkOrDescription().add(factGen.createRssChannelDescription(feed.getDescription()));
            rss.getChannel().getTitleOrLinkOrDescription().add(factGen.createRssChannelPubDate(Rfc822Util.getFullDate(feed.getPublishedDate())));
            rss.getChannel().getTitleOrLinkOrDescription().add(factGen.createRssChannelGenerator("MySpaceBlogExporter"));
            rss.getChannel().getTitleOrLinkOrDescription().add(factGen.createRssChannelLanguage(feed.getLanguage()));
            rss.getChannel().getTitleOrLinkOrDescription().add(factExp.createWxrVersion("1.0"));
            Image image = factGen.createImage();
            image.setTitle(feed.getImage().getTitle());
            image.setUrl(feed.getImage().getUrl());
            image.setLink(feed.getImage().getLink());
            rss.getChannel().getTitleOrLinkOrDescription().add(factGen.createRssChannelImage(image));
            String pageUrl = getBlogUrl();
            while (pageUrl != null) {
                if (this.isInterrupted()) {
                    throw new InterruptedException();
                }
                String html = urlToString(pageUrl);
                if (this.isInterrupted()) {
                    throw new InterruptedException();
                }
                rss.getChannel().getItem().addAll(getMSBlogsFromPage(html, factGen, factExp));
                pageUrl = getLinkByTextPattern(html, props.getProperty("olderPostsPattern"));
                addLogMsg("Next page = " + pageUrl);
            }
            addLogMsg("Done adding blogs");
            if (this.isInterrupted()) {
                throw new InterruptedException();
            }
            addLogMsg("Add all categories that we collected...");
            org.wordpress.export._1.Category chanCat = null;
            for (String category : getChanCat()) {
                chanCat = new org.wordpress.export._1.Category();
                chanCat.setCategoryNicename(getNiceName(category));
                chanCat.setCategoryParent("");
                chanCat.setCatName(category);
                rss.getChannel().getTitleOrLinkOrDescription().add(chanCat);
                addLogMsg("...added " + chanCat.getCatName());
            }
            if (this.isInterrupted()) {
                throw new InterruptedException();
            }
            QName rssQname = new QName(props.getProperty("rssLinkText"));
            JAXBElement<Rss> jaxRss = new JAXBElement<Rss>(rssQname, Rss.class, rss);
            addLogMsg("Convert the collected blogs and categories to text");
            blogString = marshalWp(jaxRss);
            jaxRss = null;
            rssQname = null;
            if (this.isInterrupted()) {
                throw new InterruptedException();
            }
            addLogMsg("Fix the blog data blocks. This may take some time; please be patient!");
            blogString = XMLUtil.fixCdata(blogString);
            if (this.isInterrupted()) {
                throw new InterruptedException();
            }
            addLogMsg("Fix the MySpace encoded \"www.msplinks.com\" references. This may take some time; please be patient!");
            blogString = fixMsplinks(blogString);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            MySpaceBlogExporterException e2 = null;
            if (e instanceof MySpaceBlogExporterException) {
                e2 = (MySpaceBlogExporterException) e;
            } else {
                e2 = new MySpaceBlogExporterException(e.getMessage(), e);
            }
            throw e2;
        }
        return blogString;
    }

    private String urlToString(String pageUrl) throws MalformedURLException, IOException {
        URL url = new URL(pageUrl);
        URLConnection con = url.openConnection();
        Pattern p = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
        Matcher m = p.matcher(con.getContentType());
        String charset = m.matches() ? m.group(1) : "ISO-8859-1";
        Reader r = new InputStreamReader(con.getInputStream(), charset);
        StringBuilder buf = new StringBuilder();
        while (true) {
            int ch = r.read();
            if (ch < 0) {
                break;
            }
            buf.append((char) ch);
        }
        r.close();
        return buf.toString();
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("ERROR: Two parameters are required: [MS Blog URL] [output file] [output format]");
            System.err.println("Supported output formats are WordPress and Blogger.");
            System.exit(1);
        }
        try {
            MySpaceBlogExporter exp = new MySpaceBlogExporter();
            exp.setBlogUrl(args[0]);
            exp.setOutputFile(args[1]);
            exp.setOutputType(args[2]);
            exp.run();
        } catch (Exception e) {
            if (!(e instanceof MySpaceBlogExporterException)) {
                e.printStackTrace();
            }
        }
    }
}
