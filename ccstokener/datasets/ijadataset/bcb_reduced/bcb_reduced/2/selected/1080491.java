package org.jcvi.vics.web.gwt.download.server;

import org.apache.log4j.Logger;
import org.jcvi.vics.web.gwt.common.client.model.download.DownloadableDataNode;
import org.jcvi.vics.web.gwt.common.client.model.download.DownloadableDataNodeImpl;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Lfoster
 * Date: Oct 5, 2006
 * Time: 2:40:22 PM
 * <p/>
 * Facilities to call from publication sources.
 */
public abstract class AbstractPublicationSource implements PublicationSource {

    protected static final List EMPTY_LIST = new ArrayList();

    protected static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private Logger log = Logger.getLogger(AbstractPublicationSource.class);

    /**
     * Simple helper to use the description as the ONLY attribute.
     *
     * @param dataFile    what gets a description.
     * @param description to describe the data.
     */
    protected void setDescriptiveText(DownloadableDataNodeImpl dataFile, String description) {
        if (description == null) return;
        String[] attributeNames = new String[] { PublicationHelper.DESCRIPTIVE_TEXT };
        String[] attributeValues = new String[] { description };
        dataFile.setAttributes(attributeNames, attributeValues);
    }

    /**
     * Real guts of get subj. doc.
     */
    protected DownloadableDataNode getSubjectDocumentHelper(String location) {
        return getSubjectDocumentHelper(location, false);
    }

    protected DownloadableDataNode getSubjectDocumentHelper(String location, boolean isLocal) {
        long size = isLocal ? getFileSize(location) : getUrlSize(location);
        String[] attributeNames = new String[] { PublicationHelper.DESCRIPTIVE_TEXT };
        String[] attributeValues = new String[] { "PDF" };
        DownloadableDataNode subjectNode = new DownloadableDataNodeImpl(EMPTY_LIST, location, attributeNames, attributeValues, location, size);
        return subjectNode;
    }

    /**
     * Given a URL, get length of its content.
     *
     * @param location where to go look.
     * @return how long is it?
     */
    protected long getUrlSize(String location) {
        long returnValue = 0L;
        try {
            URL url = new URL(location);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            returnValue = conn.getContentLength();
        } catch (IOException ioe) {
            log.error("Failed to find proper size for entity at " + location);
        }
        return returnValue;
    }

    /**
     * Given a URL, get length of its content.
     *
     * @param location where to go look.
     * @return how long is it?
     */
    protected long getFileSize(String location) {
        File file = new File(location);
        return file.length();
    }

    /**
     * Ensure that the directory location prefix is appropriate to whatever OS this
     * application is running under, by replacing all instances of known file
     * separators, with the local file separator.
     *
     * @param prefix raw from XML
     * @return adjusted to OS.
     */
    protected String localizePrefixToOSEnvironment(String prefix) {
        String returnStr = prefix.replace("/", FILE_SEPARATOR).replace("\\", FILE_SEPARATOR);
        if (returnStr.endsWith(FILE_SEPARATOR)) {
            returnStr = returnStr.substring(0, returnStr.length() - FILE_SEPARATOR.length());
        }
        return returnStr;
    }

    /**
     * Figure out what the size of the file is, by whatever means available.
     *
     * @param sizeAttibuteValue where is the size from XML?
     * @param nodeLocation      where is it on disk?
     * @param dataNode          where to set the info.
     * @param path              for reporting purposes.
     */
    protected void resolveFileSize(String sizeAttibuteValue, String nodeLocation, DownloadableDataNodeImpl dataNode, String path) {
        String sizeStr = sizeAttibuteValue;
        if (sizeStr == null) {
            dataNode.setSize(getFileSize(nodeLocation));
            log.warn("Had to setSize by stating file " + path + ", because no size was provided");
        } else {
            try {
                Long longSize = Long.parseLong(sizeStr.trim());
                dataNode.setSize(longSize.longValue());
            } catch (Exception ex) {
                dataNode.setSize(getFileSize(nodeLocation));
                log.warn("Had to setSize by stat-ing file " + path + ", because size provided was invalid: " + sizeStr);
            }
        }
    }
}
