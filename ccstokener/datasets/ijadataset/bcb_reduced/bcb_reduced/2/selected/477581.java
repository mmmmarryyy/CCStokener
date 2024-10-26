package net.sf.groofy.updater;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.groofy.GroofyApp;
import net.sf.groofy.i18n.Messages;
import net.sf.groofy.logger.GroofyLogger;
import net.sf.groofy.util.Platform;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author agomez (abelgomez@users.sourceforge.net)
 *
 */
public class SourceforgeUpdater implements IUpdater {

    /**
	 * 
	 */
    private static final String SF_DOWNLOADS_PATTERN = "http://sourceforge\\.net/projects/groofy/files/([^/]+/)*([^/]+)/download";

    private static String filesRss = "http://sourceforge.net/api/file/index/project-id/550022/rss";

    private URL updateUrl = null;

    @Override
    public boolean isUpdateAvailable() {
        try {
            Document doc = getRssDocument();
            NodeList nodeList = doc.getElementsByTagName("link");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getParentNode().getNodeName().equals("item")) {
                    String link = URLDecoder.decode(node.getTextContent(), "UTF-8");
                    if (StringUtils.isNotBlank(link)) {
                        String filename = extractFilename(link);
                        if (StringUtils.isNotBlank(filename)) {
                            String versionNumber = extractVersionNumber(filename);
                            if (StringUtils.isNotBlank(versionNumber) && versionNumber.compareTo(GroofyApp.version) > 0) {
                                updateUrl = new URL(link);
                                return true;
                            }
                        }
                    }
                }
            }
            nodeList.getLength();
        } catch (UnknownHostException e) {
            GroofyLogger.getInstance().logError(String.format(Messages.getString("SourceforgeUpdater.UnableRetrieveUpdates"), filesRss));
        } catch (ParserConfigurationException e) {
            GroofyLogger.getInstance().logException(e);
        } catch (SAXException e) {
            GroofyLogger.getInstance().logException(e);
        } catch (IOException e) {
            GroofyLogger.getInstance().logException(e);
        }
        return false;
    }

    @Override
    public URL getUpdateUrl() {
        if (isUpdateAvailable()) {
            return updateUrl;
        } else {
            return null;
        }
    }

    private String extractVersionNumber(String filename) {
        if (StringUtils.isNotBlank(filename)) {
            Pattern filePattern = Pattern.compile(String.format("%s-((?:\\d\\.)+\\d[a-z]?)-(?:multiplatform|%s).(?:zip|jar)", GroofyApp.programName, Platform.getPlatformId()));
            Matcher filenameMatcher = filePattern.matcher(filename);
            if (filenameMatcher.matches() && filenameMatcher.groupCount() == 1) {
                return filenameMatcher.group(1);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String extractFilename(String url) throws UnsupportedEncodingException {
        Pattern pattern = Pattern.compile(SF_DOWNLOADS_PATTERN);
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            return matcher.group(matcher.groupCount());
        } else {
            return null;
        }
    }

    @Override
    public void launchUpdate() {
        if (getUpdateUrl() != null && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(getUpdateUrl().toURI());
            } catch (IOException e) {
                GroofyLogger.getInstance().logException(e);
            } catch (URISyntaxException e) {
                GroofyLogger.getInstance().logException(e);
            }
        }
    }

    private Document getRssDocument() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        URL url = new URL(filesRss);
        return builder.parse(url.openStream());
    }
}
