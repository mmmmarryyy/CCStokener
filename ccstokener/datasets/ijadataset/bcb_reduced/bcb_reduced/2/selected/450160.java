package com.xmultra.processor.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.xmultra.XmultraConfig;
import com.xmultra.log.Console;
import com.xmultra.log.ErrorLogEntry;
import com.xmultra.log.MessageLogEntry;
import com.xmultra.manager.watch.jcw.JcronMessage;
import com.xmultra.processor.DestinationWriter;
import com.xmultra.processor.Processor;
import com.xmultra.util.CallbackRegistry;
import com.xmultra.util.DateUtils;
import com.xmultra.util.FileUtils;
import com.xmultra.util.InitMapHolder;
import com.xmultra.util.InvalidConfigFileFormatException;
import com.xmultra.util.NameSelector;
import com.xmultra.util.Strings;
import com.xmultra.util.SyncFlag;
import com.xmultra.util.XmlParseUtils;
import com.xmultra.watcher.WakeAble;

/**
 *  HttpGetProcessor gets the documents through HTTP protocol
 *
 *  @author Albert Tzeng
 *  @author Bob Hucker
 *  @version $Revision: #1 $
 *  @since      1.2
 *
 */
public class HttpGetProcessor extends Processor {

    /**
    * Updated automatically by source control management.
    */
    public static final String VERSION = "@version $Revision: #1 $";

    private String srcDir = null;

    private File srcDoneLocFile = null;

    private File destLocation = null;

    private File badLocFile = null;

    private String address = null;

    private String srcFile = null;

    private String srcQuery = null;

    private String getDstDir = null;

    private String dstFileNameType = null;

    private String dstFileName = null;

    private Node sourceLocationNode;

    private DestinationWriter destWriter = null;

    private Strings strings = null;

    private long timeBetweenRetries;

    private int noRetries = 5;

    private int noRetriesSoFar = 0;

    private SyncFlag retrySyncFlag;

    private boolean notifyAndStartWaitingFlag;

    private static final String REQUEST_HEADER_FIELD = "RequestHeaderField";

    private static final String HEADER_NAME = "Name";

    private static final String HEADER_VALUE = "Value";

    private ArrayList<String[]> requestList = new ArrayList<String[]>();

    private static final String[] MESSAGE_INSERT_ATTRIBUTE_NAMES = { GetMessageConfig.INSERTION_PATTERN_ATTRIBUTE, GetMessageConfig.INSERT_VALUE_ATTRIBUTE, GetMessageConfig.INSERT_POS_CASE_SENSITIVE, GetMessageConfig.INSERTION_RANGE };

    /**
     * Initializes the Processor object after it has been created.
     *
     * @param n         This Processor's corresponding element in Xmultra's
     *                  main configuration file. A child of the Processor
     *                  element. Has setup info.
     *
     * @param imh       Holds references to utility and log objects.
     *
     * @param sf        Used to communicate between threads.
     *
     * @param stopFlag  Goes false when the thread running in this
     *                  "run()" method should end.
     *
     * @return True if  init was successful.
     */
    public boolean init(Node n, InitMapHolder imh, SyncFlag sf, SyncFlag stopFlag) {
        if (!super.init(n, imh, sf, stopFlag)) return false;
        initMapHolder = imh;
        retrySyncFlag = new SyncFlag(false);
        msgEntry = new MessageLogEntry(this, VERSION);
        errEntry = new ErrorLogEntry(this, VERSION);
        strings = new Strings();
        initMapHolder.setEntry(this, InitMapHolder.STRINGS, strings);
        String noRetriesStr = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.NUMBER_RETRIES);
        String secsBetweenRetriesStr = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.SECS_BETWEEN_RETRIES);
        try {
            noRetries = Integer.parseInt(noRetriesStr);
            timeBetweenRetries = Long.parseLong(secsBetweenRetriesStr) * XmultraConfig.MS_PER_SECOND;
        } catch (NumberFormatException e) {
            errEntry.setThrowable(e);
            errEntry.setAppContext("init()");
            errEntry.setAppMessage("Bad number format in NumberRetries/" + "SecsBetweenRetries in an HttpProcessor.");
            logger.logError(errEntry);
            return false;
        }
        NodeList httpGetChildList = aProcessorNode.getChildNodes();
        for (int childNoHttpGet = 0; childNoHttpGet < httpGetChildList.getLength(); childNoHttpGet++) {
            Node httpGetChild = httpGetChildList.item(childNoHttpGet);
            String nodeName = httpGetChild.getNodeName();
            if (nodeName.equals(REQUEST_HEADER_FIELD)) {
                String[] headerField = new String[2];
                Element requestElement = (Element) httpGetChild;
                headerField[0] = requestElement.getAttribute(HEADER_NAME);
                headerField[1] = requestElement.getAttribute(HEADER_VALUE);
                requestList.add(headerField);
            }
        }
        return true;
    }

    /**
     * Set up the DestinationWriter.
     *
     * Defining one or more DestinationLocation elements inside the HttpGetProcessor
     * in the system config file allows more flexibility in file naming and routing than
     * is available with a GetDestinationLocation element.
     *
     * To use the DestinationWriter class, configure it as ...
     *   <HttpGetProcessor ...>
     *       <Locations>
     *         <DestinationLocation .../>
     *       </Locations>
     *     </HttpGetProcessor>
     *
     * rather than ...
     * /JcronWatchManager/JcronEntries/JcronEntry/JcronMessages/JcronMessage/GetLocations/GetDestinationLocation
     *
     * @return true if successful; false otherwise.
     */
    private boolean initializeDestWriter() {
        if (this.destWriter == null) {
            this.destWriter = new DestinationWriter();
            try {
                if (!destWriter.init(this.initMapHolder, super.aProcessorNode, new DateUtils())) {
                    throw new InvalidConfigFileFormatException();
                }
            } catch (InvalidConfigFileFormatException e) {
                this.errEntry.setThrowable(e);
                this.errEntry.setAppContext("initializeDestWriter()");
                this.errEntry.setAppMessage("Error reading a DestinationLocation directory");
                this.errEntry.setSubjectSendEmail("Error reading a DestinationLocation directory");
                this.logger.logError(errEntry);
                this.destWriter = null;
                return false;
            }
        }
        return true;
    }

    /**
     * Makes this class runnable. This method is called when the Thread.start()
     * method is invoked. Has the main loop of this thread.
     */
    public void run() {
        if (!processorStopSyncFlag.getFlag()) super.notifyAndStartWaiting();
        noRetriesSoFar = 0;
        notifyAndStartWaitingFlag = true;
        while (!processorStopSyncFlag.getFlag()) {
            boolean messagesProcessed = processList();
            if (processorStopSyncFlag.getFlag()) break;
            if (notifyAndStartWaitingFlag) {
                super.notifyAndStartWaiting();
                noRetriesSoFar = 0;
                notifyAndStartWaitingFlag = true;
            }
        }
        msgEntry.setAppContext("run()");
        logger.logProcess(msgEntry);
        processorSyncFlag.setFlag(false);
        processorStopSyncFlag.setFlag(false);
    }

    /**
     * Process the list of messages
     *
     * @return true if successful; false otherwise
     */
    private boolean processList() {
        String messageClassName = "";
        List list = listHolder.getList();
        int index = listHolder.getIndex();
        try {
            messageClassName = (list.get(index)).getClass().getName();
            if ((!messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE))) {
                srcDoneLocFile = getLocationDirectory(XmultraConfig.SRC_DONE_LOCATION_ELEMENT);
                if (srcDoneLocFile == null) {
                    msgEntry.setAppContext("run()");
                    msgEntry.setMessageText("Src done directory is not defined");
                    logger.logWarning(msgEntry);
                    return false;
                }
                badLocFile = getLocationDirectory(XmultraConfig.BAD_LOCATION_ELEMENT);
                if (badLocFile == null) {
                    msgEntry.setAppContext("run()");
                    msgEntry.setMessageText("Src bad directory is not defined");
                    logger.logWarning(msgEntry);
                    return false;
                }
            }
        } catch (java.lang.IndexOutOfBoundsException e) {
            errEntry.setThrowable(e);
            errEntry.setAppContext("run()");
            errEntry.setAppMessage("index=" + index + " : list size =" + list.size() + " : " + e);
            logger.logError(errEntry);
            return false;
        }
        String directoryDelimiter = "/";
        String url = null;
        for (; index < list.size(); index++) {
            listHolder.setIndex(index);
            super.currentObjBeingProcessed = list.get(index);
            try {
                InsertionConfig insertionConfig = new InsertionConfig();
                if (!getMessage(messageClassName, list, index, insertionConfig)) {
                    continue;
                }
                super.currentObjBeingProcessed = list.get(index);
                if (this.srcDir.equals("") || this.srcFile.equals("")) {
                    directoryDelimiter = "";
                }
                if ((this.address == null) || (this.address.length() < 3)) {
                    continue;
                }
                url = this.address + "/" + this.srcDir + directoryDelimiter + this.srcFile;
                if (this.srcQuery != null && !this.srcQuery.equals("")) {
                    if (this.srcQuery.charAt(0) != '?') {
                        this.srcQuery = "?" + this.srcQuery;
                    }
                    url += this.srcQuery;
                }
                String fileContents = processFile(url);
                if (fileContents == null || fileContents.equals("")) {
                    if (!messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
                        File docFile = (File) super.currentObjBeingProcessed;
                        if (docFile.exists()) {
                            fileUtils.moveFileToDoneLocation(docFile, badLocFile.toString());
                        }
                    }
                    continue;
                }
                addInsertionsAndWriteFile(messageClassName, fileContents, url, insertionConfig);
                if (processorStopSyncFlag.getFlag()) {
                    return true;
                }
                notifyAndStartWaitingFlag = true;
            } catch (java.net.MalformedURLException e) {
                errEntry.setThrowable(e);
                errEntry.setAppContext("run()");
                errEntry.setAppMessage("Error url " + url + " : " + e);
                logger.logError(errEntry);
                if (!messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
                    fileUtils.moveFileToDirectory((File) super.currentObjBeingProcessed, badLocFile.toString());
                    return false;
                }
            } catch (java.net.UnknownHostException e) {
                errEntry.setThrowable(e);
                errEntry.setAppContext("run()");
                errEntry.setAppMessage("bad host url " + url + " : " + e);
                logger.logError(errEntry);
                if (!messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
                    fileUtils.moveFileToDirectory((File) super.currentObjBeingProcessed, badLocFile.toString());
                    return false;
                }
            } catch (EOFException e) {
                if (noRetriesSoFar++ < noRetries) {
                    waitBetweenRetry();
                    notifyAndStartWaitingFlag = false;
                } else {
                    notifyAndStartWaitingFlag = true;
                    errEntry.setThrowable(e);
                    errEntry.setAppContext("run()");
                    errEntry.setAppMessage("Empty response received for URL " + url);
                    errEntry.setSubjectSendEmail("Http unable to get file");
                    logger.logError(errEntry);
                    if (!messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
                        fileUtils.moveFileToDirectory((File) super.currentObjBeingProcessed, badLocFile.toString());
                    }
                }
                return false;
            } catch (java.io.IOException e) {
                if (noRetriesSoFar++ < noRetries) {
                    waitBetweenRetry();
                    notifyAndStartWaitingFlag = false;
                } else {
                    notifyAndStartWaitingFlag = true;
                    errEntry.setThrowable(e);
                    errEntry.setAppContext("run()");
                    errEntry.setAppMessage("Unable to connect to \"" + url + "\" after " + (noRetriesSoFar - 1) + " retries. Max Retries.\n" + "Url:" + url);
                    errEntry.setSubjectSendEmail("Http unable to get file");
                    logger.logError(errEntry);
                    if (!messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
                        fileUtils.moveFileToDirectory((File) super.currentObjBeingProcessed, badLocFile.toString());
                    }
                }
                return false;
            } catch (Exception e) {
                errEntry.setThrowable(e);
                errEntry.setAppContext("run()");
                errEntry.setAppMessage("exception " + url + " : " + e);
                logger.logError(errEntry);
                if (!messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
                    fileUtils.moveFileToDirectory((File) super.currentObjBeingProcessed, badLocFile.toString());
                }
                return false;
            }
            if (!messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
                if (srcDoneLocFile != null) {
                    fileUtils.moveFileToDoneLocation((File) super.currentObjBeingProcessed, srcDoneLocFile.toString());
                } else {
                    ((File) super.currentObjBeingProcessed).delete();
                }
            }
        }
        listHolder.setIndex(index + 1);
        notifyAndStartWaitingFlag = true;
        return true;
    }

    /**
     * Get the message insert information from the system config file.
     *
     * @param messageClassName JcronMessage or ?
     * @param list list of objects to process
     * @param index position in the list
     * @param insertion information about text to be inserted
     */
    private boolean getMessage(String messageClassName, List list, int index, InsertionConfig insertionConfig) {
        if (messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
            this.address = ((JcronMessage) list.get(index)).getAddress();
            this.srcDir = ((JcronMessage) list.get(index)).getSrcDir();
            this.srcFile = ((JcronMessage) list.get(index)).getSrcFile();
            this.srcQuery = ((JcronMessage) list.get(index)).getSrcQuery();
            this.getDstDir = ((JcronMessage) list.get(index)).getDstDir();
            this.dstFileNameType = ((JcronMessage) list.get(index)).getDstFileNameType();
            if (this.dstFileNameType != null && this.dstFileNameType.equalsIgnoreCase(XmultraConfig.DST_FILE_NAME_TYPE_FIXED)) {
                this.dstFileName = ((JcronMessage) list.get(index)).getDstFileName();
                if (this.dstFileName == null || this.dstFileName == "") {
                    errEntry.setThrowable(new Exception("Fixed destination file type requires a file " + "name; check GetDestinationLocation element in " + "system config file."));
                    errEntry.setAppContext("waitBetweenRetry()");
                    logger.logError(errEntry);
                }
            }
            Node messageNode = null;
            Node locationsNode = null;
            Node messageInsertsNode = null;
            try {
                messageNode = ((JcronMessage) list.get(index)).getMessageNode();
                locationsNode = xmlParseUtils.getChildNode(messageNode, XmultraConfig.GET_LOCATIONS_ELEMENT);
                this.sourceLocationNode = xmlParseUtils.getChildNode(locationsNode, XmultraConfig.GET_SRC_LOCATION_ELEMENT);
                messageInsertsNode = xmlParseUtils.getChildNode(messageNode, XmultraConfig.MESSAGE_INSERTS_ELEMENT);
                if (messageInsertsNode != null) {
                    insertionConfig.setMessageConfiguration(messageInsertsNode, xmlParseUtils);
                }
            } catch (Exception e) {
                errEntry.setThrowable(e);
                errEntry.setAppContext("Exception in getMessage()");
                errEntry.setAppMessage("messageNode: " + (messageNode == null ? "null" : "not null") + " locationsNode: " + (locationsNode == null ? "null" : "not null") + " this.sourceLocationNode: " + (this.sourceLocationNode == null ? "null" : "not null") + " messageInsertsNode: " + (messageInsertsNode == null ? "null" : "not null"));
                logger.logError(errEntry);
                return false;
            }
        } else {
            File currentFile = (File) list.get(index);
            if (currentFile == null || !currentFile.isFile()) {
                return false;
            }
            try {
                GetMessage getMessageElement = new GetMessage(initMapHolder, currentFile, true);
                this.address = getMessageElement.getAddress();
                if ((address == null) || (address.length() < 5)) {
                    return false;
                }
                this.srcDir = getMessageElement.getSrcDir();
                this.srcFile = getMessageElement.getSrcFile();
                this.srcQuery = getMessageElement.getSrcQuery();
                this.getDstDir = getMessageElement.getDstDir();
                this.dstFileNameType = getMessageElement.getDstFileNameType();
                this.sourceLocationNode = getMessageElement.getSrcLocationNode();
                Node messageInsertsNode = getMessageElement.getMessageInsertsNode();
                if (messageInsertsNode != null) {
                    insertionConfig.setMessageConfiguration(messageInsertsNode, xmlParseUtils);
                }
            } catch (Exception e) {
                errEntry.setThrowable(e);
                errEntry.setAppContext("Exception in getMessage()");
                logger.logError(errEntry);
                return false;
            }
        }
        if (this.srcDir == null) {
            this.srcDir = "";
        }
        if (this.srcFile == null) {
            this.srcFile = "";
        }
        return true;
    }

    /**
     * Retrieve a document by HTTP.
     *
     * @param url location of document to retrieve
     *
     * @return file contents
     */
    private String processFile(String url) throws IOException, MalformedURLException, UnknownHostException {
        String fileString = null;
        NameSelector myNameSelector = new NameSelector(this.sourceLocationNode, initMapHolder);
        if (!myNameSelector.isIncluded(this.srcFile)) {
            return fileString;
        }
        URL myURL = new URL(url);
        HttpURLConnection urlCon = (HttpURLConnection) myURL.openConnection();
        if (!(urlCon instanceof HttpURLConnection || urlCon instanceof HttpsURLConnection)) {
            errEntry.setThrowable(new Exception("Unknown protocol: expected http or https"));
            errEntry.setAppContext("processFile()");
            errEntry.setAppMessage("Unknown protocol: " + myURL.toString());
            logger.logError(errEntry);
            return "";
        }
        sendRequestHeader(urlCon);
        int bufferSize = urlCon.getContentLength();
        if (bufferSize == -1) {
            bufferSize = 5120;
        }
        StringBuffer myBuffer = new StringBuffer(bufferSize);
        if (urlCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
            String encoding = urlCon.getContentEncoding();
            if (encoding == null) {
                encoding = "UTF-8";
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(urlCon.getInputStream(), encoding));
            String line = in.readLine();
            while (line != null) {
                myBuffer.append(line);
                line = in.readLine();
            }
            in.close();
            fileString = myBuffer.toString();
            if (fileString.equals("")) {
                throw new java.io.EOFException("Empty response file: " + url);
            }
        } else {
            throw new java.io.IOException("Bad response code: " + urlCon.getResponseCode());
        }
        return fileString;
    }

    private void sendRequestHeader(HttpURLConnection urlCon) {
        for (int requestNo = 0; requestNo < requestList.size(); requestNo++) {
            String[] request = requestList.get(requestNo);
            urlCon.setRequestProperty(request[0], request[1]);
        }
    }

    private void addInsertionsAndWriteFile(String messageClassName, String fileContents, String url, InsertionConfig insertionConfig) {
        if (this.getDstDir == null) {
            this.getDstDir = "";
        }
        String fullDstPath = null;
        String fileName = "";
        String query = "";
        String fileExtension = "";
        String urlFileName = new String(url);
        urlFileName = strings.substitute("https?://", "", urlFileName);
        int lastSlashPosition = urlFileName.lastIndexOf("/");
        int queryStringPosition = urlFileName.indexOf("?");
        if (lastSlashPosition > -1 && lastSlashPosition < urlFileName.length() - 1 && (queryStringPosition == -1 || lastSlashPosition < queryStringPosition - 1)) {
            if (queryStringPosition == -1) {
                urlFileName = urlFileName.substring(lastSlashPosition + 1);
            } else {
                query = urlFileName.substring(queryStringPosition);
                urlFileName = urlFileName.substring(lastSlashPosition + 1, queryStringPosition);
            }
            int extensionPosition = urlFileName.lastIndexOf(".");
            if (extensionPosition > 0) {
                fileExtension = urlFileName.substring(extensionPosition);
                urlFileName = urlFileName.substring(0, extensionPosition);
            }
            fileName = urlFileName;
        } else {
            if (queryStringPosition > -1) {
                query = urlFileName.substring(queryStringPosition);
                fileName = urlFileName.substring(0, queryStringPosition);
            } else {
                fileName = urlFileName;
            }
            if (fileName.endsWith("/")) {
                fileName = fileName.substring(0, fileName.length() - 1);
            }
            fileName = strings.substitute("[./\\\\]", "_", fileName);
        }
        if (this.dstFileNameType != null) {
            if (this.dstFileNameType.equalsIgnoreCase(XmultraConfig.DST_FILE_NAME_TYPE_SOURCE)) {
                fileName = urlFileName + fileExtension;
                fileName = strings.substitute("[?]", "_", fileName);
                fileName = strings.substitute("&", "_", fileName);
                fileName = strings.substitute("=", "-", fileName);
            } else if (this.dstFileNameType.equalsIgnoreCase(XmultraConfig.DST_FILE_NAME_TYPE_UNIQUE)) {
                fileName += FileUtils.getUniqueFileName("-", "");
                fileName += fileExtension;
            } else if (this.dstFileNameType.equalsIgnoreCase(XmultraConfig.DST_FILE_NAME_TYPE_FIXED)) {
                fileName = this.dstFileName;
            }
        }
        fileContents = insertMessages(fileContents, insertionConfig);
        File dstFile = null;
        if ((getDstDir == null) || (getDstDir.length() < 1)) {
            destLocation = getLocationDirectory(XmultraConfig.DEST_LOCATION_ELEMENT);
            if (destLocation != null) {
                boolean destinationWriterReady = initializeDestWriter();
                fileName += fileExtension;
                if (destinationWriterReady) {
                    if (destWriter.writeStringToLocations(fileName, this.srcDir, query, fileContents)) {
                        msgEntry.setDocInfo(fileName);
                        msgEntry.setMessageText("HttpGetProcessor wrote '" + fileName + query + "' to destinations.");
                        logger.logProcess(msgEntry);
                    }
                }
            } else {
                msgEntry.setAppContext("addInsertionsAndWriteFile()");
                msgEntry.setDocInfo(fileName);
                msgEntry.setMessageText("HttpGetProcessor has nowhere to write '" + fileName + ".'");
                logger.logWarning(msgEntry);
            }
        } else {
            this.getDstDir = fileUtils.addRoot(this.getDstDir);
            dstFile = new File(this.getDstDir + "/" + fileName);
            fullDstPath = dstFile.getAbsolutePath();
            boolean fileWritten = fileUtils.writeFile(dstFile, fileContents);
            if (fileWritten) {
                msgEntry.setDocInfo(srcFile);
                msgEntry.setMessageText("Document successfully retrieved from '" + url + "' into " + fullDstPath);
                logger.logProcess(msgEntry);
            }
            WakeAble wakeAble = CallbackRegistry.getFromWakeAbleRegistry(dstFile.toString());
            if (wakeAble != null) {
                wakeAble.wakeUp();
            }
        }
        processorSyncFlag.restartWaitUntilFalse();
    }

    /**
     * Add messages specified by MessageInsert elements to file before saving it.
     *
     * @param fileContents text of file
     * @param insertionConfig list of Insertion objects with patterns and values
     */
    private String insertMessages(String fileContents, InsertionConfig insertionConfig) {
        ArrayList<Insertion> insertions = insertionConfig.getInsertions();
        int insertionCount = insertions.size();
        for (int i = 0; i < insertionCount; i++) {
            Insertion insertion = insertions.get(i);
            String pattern = xmlParseUtils.conditionAttributeValue(insertion.getPattern(), false);
            String value = "$&" + xmlParseUtils.conditionAttributeValue(insertion.getValue(), false);
            String substitutionOptions = "m";
            if (insertion.getCaseSensitive().toLowerCase().equals("no")) {
                substitutionOptions += "i";
            }
            if (insertion.getRange().toLowerCase().equals("alloccurrences")) {
                substitutionOptions += "g";
            }
            synchronized (this) {
                fileContents = strings.substituteWithOptions(pattern, value, fileContents, substitutionOptions);
            }
        }
        return fileContents;
    }

    /**
     * Wait for a period of time.
     */
    private void waitBetweenRetry() {
        try {
            retrySyncFlag.waitUntilTrue(timeBetweenRetries);
        } catch (InterruptedException e) {
            errEntry.setThrowable(e);
            errEntry.setAppContext("waitBetweenRetry()");
            logger.logError(errEntry);
        }
    }

    /**
     * Used to cleanup the processor before it is killed. Can be overridden
     * by a processor subclass if cleanup needs to happen.
     */
    public void cleanUp() {
        listHolder.setIndex(listHolder.getIndex() + 1);
        String messageClassName = super.currentObjBeingProcessed.getClass().getName();
        if (messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
            return;
        }
        File docFile = (File) super.currentObjBeingProcessed;
        if (docFile.exists()) {
            fileUtils.moveFileToDoneLocation(docFile, srcDoneLocFile.toString());
        }
    }

    /**
     * Configuration information for a single "message insert" -- a string to be
     * inserted in the content retrieved with an HTTP request.
     */
    protected class Insertion {

        public static final long serialVersionUID = 1L;

        private String pattern;

        private String value;

        private String caseSensitive;

        private String range;

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void setCaseSensitive(String caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        public void setRange(String range) {
            this.range = range;
        }

        public String getPattern() {
            return this.pattern;
        }

        public String getValue() {
            return this.value;
        }

        public String getCaseSensitive() {
            return this.caseSensitive;
        }

        public String getRange() {
            return this.range;
        }
    }

    /**
     * Configuration information for a set of "message inserts" -- string to be
     * inserted in the content retrieved with an HTTP request.
     */
    protected class InsertionConfig {

        public static final long serialVersionUID = 1L;

        private ArrayList<Insertion> insertions = new ArrayList<Insertion>();

        /**
         * Set information for all message insertions.
         *
         * @param XML MessageInserts node containing one or more MessageInsert nodes.
         */
        public void setMessageConfiguration(Node messageInsertsNode, XmlParseUtils xmlParseUtils) {
            if (messageInsertsNode == null) {
                return;
            }
            synchronized (this) {
                String messageInserts[][] = xmlParseUtils.getAttributeValuesFromChildNodes(messageInsertsNode, GetMessageConfig.MESSAGE_INSERT_ELEMENT, HttpGetProcessor.MESSAGE_INSERT_ATTRIBUTE_NAMES);
                if (messageInserts == null) {
                    return;
                }
                for (int i = 0; i < messageInserts.length; i++) {
                    Insertion insertion = new Insertion();
                    insertion.setPattern(messageInserts[i][0]);
                    insertion.setValue(messageInserts[i][1]);
                    insertion.setCaseSensitive(messageInserts[i][2]);
                    insertion.setRange(messageInserts[i][3]);
                    this.insertions.add(insertion);
                }
            }
        }

        /**
         * Get list of Insertion objects for strings inserted into file contents.
         *
         * @return list of Insertion objects
         */
        public ArrayList<Insertion> getInsertions() {
            return insertions;
        }

        /**
         * Get list of Insertion objects for strings inserted into file contents.
         *
         * @return list of Insertion objects
         */
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            Iterator<Insertion> ii = insertions.iterator();
            while (ii.hasNext()) {
                Insertion i = ii.next();
                buffer.append(i.getPattern());
                buffer.append(" -> ");
                buffer.append(i.getValue());
                buffer.append(" /\n");
            }
            return buffer.toString();
        }
    }
}
