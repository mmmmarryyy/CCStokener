package net.sf.osadm.mpso;

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import net.sf.osadm.mpso.extension.MavenRepositoryExtension;
import net.sf.osadm.mpso.extension.MpsExtension;
import net.sf.osadm.mpso.output.TagNames;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2;
import org.xml.sax.ext.Attributes2Impl;

public class XhtmlContentHandlerBasedVisitor implements MavenProjectInformationVisitor {

    private TransformerHandler handler;

    private StreamResult streamResult;

    private TagNames tagNames;

    private int indentation = 0;

    private DateFormat dateFormatCompact = new SimpleDateFormat("yyyyMMdd");

    private DateFormat timestampFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private MavenProjectInformationContext context;

    /**
     * Constructor for creating a MavenProjectInformationVisitor instance, which can create a file,
     * containing a table in XHTML format.
     *  
     * @param xmlWriter - to output is written to this writer
     * @param tagNames - the TagNames instances, containing the specific tag names (XHTML or DocBook)
     * @param context - the Maven Project Information context
     *  
     * @throws TransformerConfigurationException
     * @throws TransformerFactoryConfigurationError
     */
    public XhtmlContentHandlerBasedVisitor(Writer xmlWriter, TagNames tagNames, MavenProjectInformationContext context) throws TransformerConfigurationException, TransformerFactoryConfigurationError {
        super();
        handler = createTransformerHandler();
        streamResult = new StreamResult(xmlWriter);
        handler.setResult(streamResult);
        this.tagNames = tagNames;
        this.context = context;
    }

    @Override
    public void runThrough(MavenProjectInformationVisitorAcceptor acceptor) {
        try {
            beforeVisit();
            acceptor.accept(this);
            afterVisit();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void visit(MavenProjectInformation mpi) {
        try {
            writeWhitespace(handler, indentation + 1);
            startElement(TagNames.TR, null);
            for (String columnId : context.getColumnIds()) {
                String text = context.getText(mpi, columnId);
                System.out.println(columnId + "  " + text);
                if (context.isDefaultMpsExtensionId(columnId)) {
                    createEntry(mpi, columnId, text);
                } else if (context.isMpsExtensionId(columnId)) {
                    MpsExtension mpsExtension = context.getMpsExtension(columnId);
                    createEntry(mpi, mpsExtension, columnId, text);
                } else if (context.isMavenRepositoryExtensionId(columnId)) {
                    createEntry(mpi, context.getMavenRepositoryExtension(columnId), columnId, text);
                }
            }
            writeWhitespace(handler, indentation + 1);
            endElement(TagNames.TR);
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private void createEntry(MavenProjectInformation mpi, String columnId, String text) throws SAXException {
        if (DefaultMpsExtensionService.EXTENSION_ID_GROUP_ID.equals(columnId)) {
            writeTagText(TagNames.TD, text);
        } else if (DefaultMpsExtensionService.EXTENSION_ID_ARTIFACT_ID.equals(columnId)) {
            writeTagText(TagNames.TD, text);
        } else if (DefaultMpsExtensionService.EXTENSION_ID_ARTIFACT_VERSION.equals(columnId) || DefaultMpsExtensionService.EXTENSION_ID_ARTIFACT_VERSION_CLEAN.equals(columnId)) {
            if (mpi.getHomePageUrl() != null) {
                writeLink(mpi.getHomePageUrl(), text);
            } else {
                writeTagText(TagNames.TD, text);
            }
        } else if (DefaultMpsExtensionService.EXTENSION_ID_ARTIFACT_IS_SNAPSHOT.equals(columnId)) {
            writeTagText(TagNames.TD, text);
        } else if (DefaultMpsExtensionService.EXTENSION_ID_ARTIFACT_TYPE.equals(columnId)) {
            try {
                String fileExtension = null;
                if ("maven-plugin".equals(text) || "bundle".equals(text)) {
                    fileExtension = "jar";
                } else {
                    fileExtension = text;
                }
                URI jarNexusUrl = context.createNexusUrl(mpi, fileExtension, null);
                if (jarNexusUrl != null) {
                    writeImageLink(jarNexusUrl.toURL(), MavenProjectInformation.getMpsExtensionByType(text));
                } else {
                    writeTagText(TagNames.TD, text);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        } else if (DefaultMpsExtensionService.EXTENSION_ID_SITE_LAST_PUBLISHED.equals(columnId)) {
            writeTagText(TagNames.TD, text);
        } else if (DefaultMpsExtensionService.EXTENSION_ID_SITE_FILE_LOCATION.equals(columnId)) {
            URI baseProjectSiteUri = mpi.getBaseProjectSiteUri();
            String baseProjectUriStr = baseProjectSiteUri.toString();
            if (baseProjectUriStr.lastIndexOf("/") == baseProjectUriStr.length() - 1) {
                baseProjectUriStr = baseProjectUriStr.substring(0, baseProjectUriStr.length() - 1);
            }
            if (baseProjectUriStr.lastIndexOf("/") > 0) {
                baseProjectUriStr = baseProjectUriStr.substring(0, baseProjectUriStr.lastIndexOf("/"));
            }
            try {
                baseProjectSiteUri = new URI(baseProjectUriStr);
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }
            URI projectSiteUri = mpi.getProjectSiteUri();
            try {
                String cleanupPath = FileUtils.trimPath(baseProjectSiteUri, projectSiteUri);
                cleanupPath = cleanupPath.substring(0, cleanupPath.lastIndexOf("/"));
                writeTagText(TagNames.TD, cleanupPath);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                writeTagText(TagNames.TD, text);
            }
        }
    }

    private void createEntry(MavenProjectInformation mpi, MpsExtension mpsExtension, String columnId, String text) throws SAXException {
        if (text == null) {
            writeTagText(TagNames.TD, null);
            return;
        }
        URL href;
        try {
            href = new URL(text);
            if (mpsExtension.getIcon() != null) {
                writeImageLink(href, mpsExtension);
            } else {
                writeLink(href, mpsExtension.getTooltip());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            writeTagText(TagNames.TD, null);
        }
    }

    private void createEntry(MavenProjectInformation mpi, MavenRepositoryExtension mavenRepoExtension, String columnId, String text) throws SAXException {
        if (text == null) {
            writeTagText(TagNames.TD, null);
            return;
        }
        URL href;
        try {
            href = context.createNexusUrl(mpi, mavenRepoExtension.getFileExtension(), mavenRepoExtension.getClassifier()).toURL();
            System.out.println("  Nexus URL created  " + href);
            URLConnection urlConnection = href.openConnection();
            urlConnection.connect();
            String contentEncoding = urlConnection.getContentEncoding();
            System.out.println("  content  " + contentEncoding + "  " + urlConnection.getContentType() + "  " + urlConnection.getContentLength());
            if (urlConnection.getContentType() != null && urlConnection.getContentType().equals(mavenRepoExtension.getContentType()) && urlConnection.getContentLength() > 0) {
                if (mavenRepoExtension.getIcon() != null) {
                    writeImageLink(href, mavenRepoExtension);
                } else {
                    writeLink(href, mavenRepoExtension.getTooltip());
                }
            } else {
                writeTagText(TagNames.TD, null);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            writeTagText(TagNames.TD, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeImage(String tag, String tooltip, String icon) throws SAXException {
        Attributes2Impl attributes = new Attributes2Impl();
        addAttribute(attributes, "width", "20");
        if (icon != null && tooltip != null) {
            addAttribute(attributes, "title", tooltip);
        }
        writeWhitespace(handler, indentation + 2);
        startElement(tag, attributes);
        if (icon != null) {
            Attributes2Impl imageAttributes = createImageAttribute(icon);
            addAttribute(imageAttributes, "width", "16");
            addAttribute(imageAttributes, "height", "16");
            startElementRaw("img", imageAttributes);
            endElementRaw("img");
        } else {
            writeText(tooltip);
        }
        endElement(tag);
    }

    private void writeImageLink(URL href, MpsExtension mpsExtension) throws SAXException {
        if (mpsExtension != null) {
            writeImageLink(href, mpsExtension.getTooltip(), mpsExtension.getIcon());
        } else {
            writeLink(href, href.toString());
        }
    }

    private void writeImageLink(URL href, MavenRepositoryExtension mavenRepoExtension) throws SAXException {
        if (mavenRepoExtension != null) {
            writeImageLink(href, mavenRepoExtension.getTooltip(), mavenRepoExtension.getIcon());
        } else {
            writeLink(href, href.toString());
        }
    }

    private void writeImageLink(URL href, String tooltip, String icon) throws SAXException {
        if (href != null) {
            Attributes2Impl attributes = new Attributes2Impl();
            addAttribute(attributes, "width", "20");
            if (icon != null && tooltip != null) {
                addAttribute(attributes, "title", tooltip);
            }
            writeWhitespace(handler, indentation + 2);
            startElement(TagNames.TD, attributes);
            startElement(TagNames.ANCHOR, getAnchorAttribute(href));
            if (icon != null) {
                Attributes2Impl imageAttributes = createImageAttribute(icon);
                addAttribute(imageAttributes, "width", "16");
                addAttribute(imageAttributes, "height", "16");
                startElementRaw("img", imageAttributes);
                endElementRaw("img");
            } else {
                writeText(tooltip);
            }
            endElement(TagNames.ANCHOR);
            endElement(TagNames.TD);
        } else {
            writeTagText(TagNames.TD, "");
        }
    }

    private Attributes2Impl createImageAttribute(String icon) {
        Attributes2Impl imageAttributes = new Attributes2Impl();
        addAttribute(imageAttributes, "src", "icons/" + icon);
        return imageAttributes;
    }

    private void writeLink(URI uri, String title) throws SAXException {
        if (uri != null) {
            try {
                writeLink(uri.toURL(), title);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                writeTagText(TagNames.TD, "");
            }
        } else {
            writeTagText(TagNames.TD, "");
        }
    }

    private void writeText(String value) throws SAXException {
        if (value == null) {
            return;
        }
        char[] charArray = value.toCharArray();
        handler.characters(charArray, 0, charArray.length);
    }

    private void writeTagText(String tag, String value) throws SAXException {
        writeWhitespace(handler, indentation + 2);
        startElement(tag, null);
        writeText(value);
        endElement(tag);
    }

    private void writeLink(URL href, String value) throws SAXException {
        writeWhitespace(handler, indentation + 2);
        startElement(TagNames.TD, null);
        startElement(TagNames.ANCHOR, getAnchorAttribute(href));
        char[] charArray = value.toCharArray();
        handler.characters(charArray, 0, charArray.length);
        endElement(TagNames.ANCHOR);
        endElement(TagNames.TD);
    }

    private static Attributes2 getAnchorAttribute(URL href) {
        Attributes2Impl attributes = new Attributes2Impl();
        String uri = "";
        String localName = "";
        String qName = "href";
        String type = "CDATA";
        String value = href.toString();
        attributes.addAttribute(uri, localName, qName, type, value);
        return attributes;
    }

    public void beforeVisit() throws SAXException {
        handler.startDocument();
        writeWhitespace(handler, indentation);
        writeComment(handler, " ================================================== ");
        writeWhitespace(handler, indentation);
        writeComment(handler, " WARNING: Generated by Maven Project Sites Overview ");
        writeWhitespace(handler, indentation);
        writeComment(handler, " ================================================== ");
        if (tagNames.get(TagNames.DOCUMENT) != null) {
            writeWhitespace(handler, indentation++);
            startElement(TagNames.DOCUMENT, null);
        }
        if (tagNames.get(TagNames.HEAD) != null) {
            writeWhitespace(handler, indentation);
            startElement(TagNames.HEAD, null);
            writeWhitespace(handler, indentation + 1);
            startElementRaw("title", null);
            writeText("Maven Project Sites Overview");
            endElementRaw("title");
            writeLink("image/x-ico", "icon", "icons/favicon.ico");
            writeLink("image/x-icon", "shortcut icon", "icons/favicon.ico");
            writeLink("text/css", "stylesheet", "css/default.css");
            writeMeta("Date-Revision-yyyymmdd", dateFormatCompact.format(new Date()));
            writeWhitespace(handler, indentation);
            endElement(TagNames.HEAD);
        }
        if (tagNames.get(TagNames.BODY) != null) {
            writeWhitespace(handler, indentation++);
            startElement(TagNames.BODY, null);
            writeWhitespace(handler, indentation);
            startElementRaw("img", createImageAttribute("ac-logo.png"));
            endElementRaw("link");
            writeWhitespace(handler, indentation);
            startElementRaw("hr", null);
            endElementRaw("hr");
            writeWhitespace(handler, indentation);
            startElementRaw("h1", null);
            writeText("Maven Project Sites Overview");
            endElementRaw("h1");
            writeWhitespace(handler, indentation);
            startElementRaw("p", null);
            writeText("Generated " + timestampFormat.format(new Date()));
            endElementRaw("p");
        }
        writeWhitespace(handler, indentation);
        startElement(TagNames.TABLE, null);
        writeWhitespace(handler, ++indentation);
        writeWhitespace(handler, indentation);
        if (tagNames.get(TagNames.TGROUP) != null) {
            startElement(TagNames.TGROUP, null);
            writeWhitespace(handler, ++indentation);
        }
        boolean showColumnTitles = true;
        if (showColumnTitles) {
            if (tagNames.isHeaderBeforeBody()) {
                startElement(TagNames.THEAD, null);
                writeHeader(context);
                writeWhitespace(handler, indentation);
                endElement(TagNames.THEAD);
                writeWhitespace(handler, indentation);
                startElement(TagNames.TBODY, null);
            } else {
                startElement(TagNames.TBODY, null);
                if (tagNames.get(TagNames.THEAD) != null) {
                    writeWhitespace(handler, ++indentation);
                    startElement(TagNames.THEAD, null);
                }
                writeHeader(context);
                if (tagNames.get(TagNames.THEAD) != null) {
                    writeWhitespace(handler, indentation);
                    endElement(TagNames.THEAD);
                    --indentation;
                }
            }
        }
    }

    private void writeLink(String type, String rel, String href) throws SAXException {
        writeWhitespace(handler, indentation + 1);
        startElementRaw("link", createLinkAttributes(type, rel, href));
        endElementRaw("link");
    }

    private void writeMeta(String name, String content) throws SAXException {
        writeWhitespace(handler, indentation + 1);
        startElementRaw("meta", createMetaAttributes(name, content));
        endElementRaw("meta");
    }

    private Attributes createLinkAttributes(String type, String rel, String href) {
        Attributes2Impl attributes = new Attributes2Impl();
        addAttribute(attributes, "type", type);
        addAttribute(attributes, "rel", rel);
        addAttribute(attributes, "href", href);
        return attributes;
    }

    private Attributes createMetaAttributes(String name, String content) {
        Attributes2Impl attributes = new Attributes2Impl();
        addAttribute(attributes, "name", name);
        addAttribute(attributes, "content", content);
        return attributes;
    }

    private void addAttribute(Attributes2Impl attributes, String qName, String value) {
        String uri = "";
        String localName = "";
        String type = "";
        attributes.addAttribute(uri, localName, qName, type, value);
    }

    public void afterVisit() throws SAXException {
        writeWhitespace(handler, indentation);
        endElement(TagNames.TBODY);
        if (tagNames.get(TagNames.TGROUP) != null) {
            writeWhitespace(handler, --indentation);
            endElement(TagNames.TGROUP);
        }
        writeWhitespace(handler, --indentation);
        endElement(TagNames.TABLE);
        if (tagNames.get(TagNames.BODY) != null) {
            writeWhitespace(handler, --indentation);
            startElement(TagNames.BODY, null);
        }
        if (tagNames.get(TagNames.DOCUMENT) != null) {
            writeWhitespace(handler, --indentation);
            startElement(TagNames.DOCUMENT, null);
        }
        writeWhitespace(handler, indentation);
        writeComment(handler, " end-of-file ");
        writeWhitespace(handler, indentation);
        handler.endDocument();
    }

    private void startElement(String tagName, Attributes attributes) throws SAXException {
        handler.startElement("", "", tagNames.get(tagName), attributes);
    }

    private void endElement(String tagName) throws SAXException {
        handler.endElement("", "", tagNames.get(tagName));
    }

    private void startElementRaw(String tagName, Attributes attributes) throws SAXException {
        handler.startElement("", "", tagName, attributes);
    }

    private void endElementRaw(String tagName) throws SAXException {
        handler.endElement("", "", tagName);
    }

    private void writeHeader(MavenProjectInformationContext context) throws SAXException {
        writeWhitespace(handler, indentation + 1);
        startElement(TagNames.TR, null);
        for (String columnId : context.getColumnIds()) {
            String title = null;
            String tooltip = null;
            String icon = null;
            if (context.getMpsExtension(columnId) != null) {
                MpsExtension mpsExtension = context.getMpsExtension(columnId);
                title = mpsExtension.getTitle();
                tooltip = mpsExtension.getTooltip();
                icon = mpsExtension.getIcon();
            } else if (context.getMavenRepositoryExtension(columnId) != null) {
                MavenRepositoryExtension mavenRepoExtension = context.getMavenRepositoryExtension(columnId);
                title = mavenRepoExtension.getTitle();
                tooltip = mavenRepoExtension.getTooltip();
                icon = mavenRepoExtension.getIcon();
            }
            if (icon != null) {
                writeImage(TagNames.TH, tooltip, icon);
            } else if (title != null) {
                writeTagText(TagNames.TH, title);
            } else {
                writeTagText(TagNames.TH, columnId);
            }
        }
        writeWhitespace(handler, indentation + 1);
        endElement(TagNames.TR);
    }

    private void writeRow(List<String> fields) throws SAXException {
        writeWhitespace(handler, indentation + 1);
        startElement(TagNames.TR, null);
        for (String cell : fields) {
            writeWhitespace(handler, indentation + 2);
            startElement(TagNames.TD, null);
            writeText(cell);
            endElement(TagNames.TD);
        }
        writeWhitespace(handler, indentation + 1);
        endElement(TagNames.TR);
    }

    private static void writeWhitespace(TransformerHandler handler, int indentation) throws SAXException {
        StringBuilder whitespace = new StringBuilder("                    ");
        if (indentation > 10) {
            System.err.println("Code correction needed...");
            throw new RuntimeException("Code correction needed...");
        }
        writeWhitespace(handler, "\n" + whitespace.substring(0, 2 * indentation));
    }

    private static void writeComment(TransformerHandler handler, String text) throws SAXException {
        handler.comment(text.toCharArray(), 0, text.length());
    }

    private static void writeWhitespace(TransformerHandler handler, String whitespace) throws SAXException {
        handler.ignorableWhitespace(whitespace.toCharArray(), 0, whitespace.length());
    }

    private static TransformerHandler createTransformerHandler() throws TransformerFactoryConfigurationError, TransformerConfigurationException {
        SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler handler = factory.newTransformerHandler();
        Transformer serializer = handler.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        return handler;
    }
}
