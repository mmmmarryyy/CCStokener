package com.ideo.jso;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.log4j.Logger;
import javax.servlet.jsp.PageContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.Writer;
import java.io.File;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Manages initializations (XML parsing), and stores the groups parsed. 
 * @author Julien Maupoux
 *
 */
public class InclusionController {

    private static final Logger log = Logger.getLogger(InclusionController.class);

    private static InclusionController ourInstance = new InclusionController();

    private static final String CONFIGURATION_FILE_NAME = "jso.xml";

    /**
     * Milliseconds
     */
    private static final long MODIFICATION_TIME_VERIFICATION_FREQUENCE = 1000;

    private long configurationFilesMaxModificationTime;

    private long lastModificationTimeVerification;

    private Map groups = new HashMap();

    /**
     * Singleton. get the instance
     * @return
     */
    public static InclusionController getInstance() {
        if (ourInstance.needToConfigure()) ourInstance.configure();
        return ourInstance;
    }

    private InclusionController() {
        configure();
    }

    private boolean needToConfigure() {
        try {
            return configurationFilesMaxModificationTime < findMaxConfigModificationTime();
        } catch (IOException e) {
            return false;
        }
    }

    private long findMaxConfigModificationTime() throws IOException {
        if (lastModificationTimeVerification + MODIFICATION_TIME_VERIFICATION_FREQUENCE > System.currentTimeMillis()) return configurationFilesMaxModificationTime;
        long res = 0;
        Enumeration resources = getConfigResources();
        while (resources.hasMoreElements()) {
            URL url = (URL) resources.nextElement();
            try {
                File file = new File(new URI(url.toString()));
                if (file.exists() && res < file.lastModified()) res = file.lastModified();
            } catch (URISyntaxException e) {
            } catch (IllegalArgumentException e) {
            }
        }
        lastModificationTimeVerification = System.currentTimeMillis();
        return res;
    }

    /**
     * Display all the inclusions HTML tags for the requested groups 
     * @param pageContext
     * @param out the Writer
     * @param groupNames the groups to include
     * @param exploded if the inclusion should be the classic way or the merged one 
     * @throws IOException
     */
    public void printImports(PageContext pageContext, Writer out, String groupNames, boolean exploded) throws IOException {
        List includeGroups = new ArrayList();
        StringTokenizer st = new StringTokenizer(groupNames, ",;", false);
        while (st.hasMoreTokens()) {
            String groupName = st.nextToken().trim();
            Group g = (Group) groups.get(groupName);
            if (g == null) throw new RuntimeException("Group '" + groupName + "' not found in " + CONFIGURATION_FILE_NAME);
            log.debug("Loading group : " + groupName + ".");
            includeGroups.add(g);
        }
        for (int i = 0; i < includeGroups.size(); i++) {
            Group group = (Group) includeGroups.get(i);
            group.printIncludeJSTag(pageContext, out, exploded);
            group.printIncludeCSSTag(pageContext, out);
        }
    }

    /**
     * Parses the configuration files.
     */
    private synchronized void configure() {
        final Map res = new HashMap();
        try {
            final Enumeration resources = getConfigResources();
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            while (resources.hasMoreElements()) {
                final URL url = (URL) resources.nextElement();
                DefaultHandler saxHandler = new DefaultHandler() {

                    private Group group;

                    private StringBuffer tagContent = new StringBuffer();

                    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                        if ("group".equals(qName)) {
                            group = new Group(attributes.getValue("name"));
                            String minimizeJs = attributes.getValue("minimize");
                            String minimizeCss = attributes.getValue("minimizeCss");
                            group.setMinimize(!"false".equals(minimizeJs));
                            group.setMinimizeCss("true".equals(minimizeCss));
                        } else if ("js".equals(qName) || "css".equals(qName) || "group-ref".equals(qName)) tagContent.setLength(0);
                    }

                    public void characters(char ch[], int start, int length) throws SAXException {
                        tagContent.append(ch, start, length);
                    }

                    public void endElement(String uri, String localName, String qName) throws SAXException {
                        if ("group".equals(qName)) res.put(group.getName(), group); else if ("js".equals(qName)) group.getJsNames().add(tagContent.toString()); else if ("css".equals(qName)) group.getCssNames().add(tagContent.toString()); else if ("group-ref".equals(qName)) {
                            String name = tagContent.toString();
                            Group subGroup = (Group) res.get(name);
                            if (subGroup == null) throw new RuntimeException("Error parsing " + url.toString() + " <group-ref>" + name + "</group-ref> unknown");
                            group.getSubgroups().add(subGroup);
                        }
                    }
                };
                try {
                    saxParser.parse(url.openStream(), saxHandler);
                } catch (Throwable e) {
                    log.warn(e.toString(), e);
                    log.warn("Exception " + e.toString() + " ignored, let's move on..");
                }
            }
            configurationFilesMaxModificationTime = findMaxConfigModificationTime();
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        this.groups = res;
    }

    private Enumeration getConfigResources() throws IOException {
        return Thread.currentThread().getContextClassLoader().getResources(CONFIGURATION_FILE_NAME);
    }

    public Group getGroup(String name) {
        return (Group) groups.get(name);
    }
}
