package edu.pitt.dbmi.odie.gapp.gwt.server.util.oba;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import edu.pitt.dbmi.odie.gapp.gwt.server.user.ODIE_ServerSideLoginInfo;
import edu.pitt.dbmi.odie.gapp.gwt.server.util.rest.ODIE_NcboRestUtils;

public class ODIE_ObaCoder extends DefaultHandler {

    /**
	 * Field logger.
	 */
    private static final Logger logger = Logger.getLogger(ODIE_ObaCoder.class);

    public static final String COMMA = ",";

    public static final String COLON = ":";

    private static final long RECOVERY_TIME_IN_MILLISECONDS = 5000;

    public static final int MAX_READ_ATTEMPTS = 5;

    private int currentLineOffset = 0;

    private int currentAnnotationOffset = 0;

    private int batchSize = 5;

    private StringBuffer accumulator;

    private String obaUrl = "http://rest.bioontology.org/obs/annotator?email=";

    private boolean longestOnly = true;

    private boolean wholeWordOnly = true;

    private boolean scored = true;

    private final ArrayList<String> localOntologyIDs = new ArrayList<String>();

    private final ArrayList<String> localSemanticTypeIDs = new ArrayList<String>();

    private int levelMin = 0;

    private int levelMax = 0;

    private final ArrayList<String> mappingTypes = new ArrayList<String>();

    private final ArrayList<ODIE_ObaAnnotationBean> annotations = new ArrayList<ODIE_ObaAnnotationBean>();

    private final ArrayList<ODIE_ObaAnnotationBean> annotationsForLine = new ArrayList<ODIE_ObaAnnotationBean>();

    private ODIE_ObaAnnotationBean annotation;

    private Collection<ODIE_ObaSemanticTypeBean> semanticTypes = new ArrayList<ODIE_ObaSemanticTypeBean>();

    private ODIE_ObaSemanticTypeBean semanticTypeBean;

    private ArrayList<String> synonyms = new ArrayList<String>();

    private String synonym;

    private String documentText;

    private String currentStartElement = "";

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        ODIE_ObaCoder coder = new ODIE_ObaCoder();
        ODIE_ServerSideLoginInfo loginInfo = new ODIE_ServerSideLoginInfo();
        String emailAddress = args[0];
        loginInfo.setEmailAddress(emailAddress);
        coder.initialize(loginInfo);
        coder.addLocalOntologyId("NCI");
        coder.addLocalOntologyId("13578");
        coder.addLocalOntologyId("MSH");
        coder.addLocalSemanticTypeId("T047");
        coder.addLocalSemanticTypeId("T048");
        coder.addLocalSemanticTypeId("T191");
        coder.addMappingType("inter-cui");
        coder.setDocumentText("melanoma");
        coder.execute();
        logger.debug(coder);
    }

    public ODIE_ObaCoder() {
    }

    public void initialize(ODIE_ServerSideLoginInfo loginInfo) {
        this.obaUrl += loginInfo.getEmailAddress() + "&#34;";
        logger.debug("ObaCoder using coder url ==> " + this.obaUrl);
        ArrayList<String> nullMappingTypes = new ArrayList<String>();
        nullMappingTypes.add("null");
        setMappingTypes(nullMappingTypes);
        ArrayList<String> nciOntologyIds = new ArrayList<String>();
        nciOntologyIds.add("32145");
        this.setLocalOntologyIDs(nciOntologyIds);
    }

    public void execute() {
        this.annotations.clear();
        String[] documentLines = getDocumentText().split("\n");
        Pattern wordPattern = Pattern.compile("^.*\\w[\\w\\d]*.*$");
        int offset = 0;
        for (int idx = 0; idx < documentLines.length; idx++) {
            this.annotationsForLine.clear();
            String documentLine = documentLines[idx];
            Matcher matcher = wordPattern.matcher(documentLine);
            if (matcher.matches()) {
                executeLineOfDocument(documentLine);
                if (getAnnotationsForLine().size() > 0) {
                    for (ODIE_ObaAnnotationBean annot : this.annotationsForLine) {
                        annot.setSPos(annot.getSPos() + offset);
                        annot.setEPos(annot.getEPos() + offset);
                    }
                    this.annotations.addAll(annotationsForLine);
                }
            }
            offset += documentLine.length() + 1;
        }
    }

    public int calculateNumberOfLines() {
        int result = 0;
        if (ODIE_NcboRestUtils.notNull(this.documentText)) {
            result = getDocumentText().split("\n").length;
        }
        return result;
    }

    public void execute(int lineOffset, int annotationOffset) {
        this.currentLineOffset = lineOffset;
        this.currentAnnotationOffset = annotationOffset;
        this.annotations.clear();
        String[] documentLines = getDocumentText().split("\n");
        Pattern wordPattern = Pattern.compile("^.*\\w[\\w\\d]*.*$");
        for (int idx = 0; idx < this.batchSize && this.currentLineOffset < documentLines.length; this.currentLineOffset++, idx++) {
            this.annotationsForLine.clear();
            String documentLine = documentLines[this.currentLineOffset];
            Matcher matcher = wordPattern.matcher(documentLine);
            if (matcher.matches()) {
                executeLineOfDocument(documentLine);
                if (getAnnotationsForLine().size() > 0) {
                    for (ODIE_ObaAnnotationBean annot : this.annotationsForLine) {
                        annot.setSPos(annot.getSPos() + this.currentAnnotationOffset);
                        annot.setEPos(annot.getEPos() + this.currentAnnotationOffset);
                    }
                    this.annotations.addAll(annotationsForLine);
                }
            }
            this.currentAnnotationOffset += documentLine.length() + 1;
        }
    }

    public void executeLineOfDocument(String lineOfDocument) {
        String contents = null;
        URLConnection conn = null;
        boolean isReading = true;
        int readAttempt = 0;
        while (isReading && readAttempt < MAX_READ_ATTEMPTS) {
            try {
                String data = constructData(lineOfDocument);
                contents = sendData(obaUrl, data);
                if (contents != null && contents.length() > 0) {
                    isReading = false;
                }
            } catch (Exception e) {
                logger.warn(e.getMessage());
                isReading = true;
                readAttempt++;
            }
            if (isReading) {
                logger.warn("Coder attempt #" + readAttempt);
            }
        }
        if (contents != null) {
            logger.debug(contents);
            ByteArrayInputStream bis = new ByteArrayInputStream(contents.getBytes());
            try {
                readXmlStream(bis);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        } else {
            logger.warn("Got null contents after read.");
        }
    }

    private String constructData(String lineOfDocument) throws UnsupportedEncodingException {
        String data = "";
        data += URLEncoder.encode("longestOnly", "UTF-8") + "=" + URLEncoder.encode(isLongestOnly() + "", "UTF-8");
        data += "&";
        data += URLEncoder.encode("wholeWordOnly", "UTF-8") + "=" + URLEncoder.encode(isWholeWordOnly() + "", "UTF-8");
        data += "&";
        data += URLEncoder.encode("stopWords", "UTF-8") + "=" + URLEncoder.encode("true", "UTF-8");
        data += "&";
        data += URLEncoder.encode("scored", "UTF-8") + "=" + URLEncoder.encode(isScored() + "", "UTF-8");
        if (localOntologyIDs.size() > 0) {
            data += "&";
            data += URLEncoder.encode("ontologiesToExpand", "UTF-8") + "=" + URLEncoder.encode(implodeListWithSeparator(localOntologyIDs, COMMA), "UTF-8");
            data += "&";
            data += URLEncoder.encode("ontologiesToKeepInResult", "UTF-8") + "=" + URLEncoder.encode(implodeListWithSeparator(localOntologyIDs, COMMA), "UTF-8");
        }
        if (localSemanticTypeIDs.size() > 0) {
            data += "&";
            data += URLEncoder.encode("semanticTypes", "UTF-8") + "=" + URLEncoder.encode(implodeListWithSeparator(localSemanticTypeIDs, COMMA), "UTF-8");
        }
        data += "&";
        data += URLEncoder.encode("levelMin", "UTF-8") + "=" + URLEncoder.encode(getLevelMin() + "", "UTF-8");
        data += "&";
        data += URLEncoder.encode("levelMax", "UTF-8") + "=" + URLEncoder.encode(getLevelMax() + "", "UTF-8");
        data += "&";
        data += URLEncoder.encode("mappingTypes", "UTF-8") + "=" + URLEncoder.encode(implodeListWithSeparator(mappingTypes, COMMA), "UTF-8");
        data += "&";
        data += URLEncoder.encode("textToAnnotate", "UTF-8") + "=" + URLEncoder.encode(lineOfDocument, "UTF-8");
        data += "&";
        data += URLEncoder.encode("format", "UTF-8") + "=" + URLEncoder.encode("asXML", "UTF-8");
        logger.debug(data);
        return data;
    }

    private String sendData(String stURL, String strToPost) throws IOException {
        String resp = "";
        URL url = new URL(stURL);
        HttpURLConnection conHttp = (HttpURLConnection) url.openConnection();
        String encoding = "ISO-8859-1";
        conHttp.setDoOutput(true);
        conHttp.setRequestMethod("POST");
        conHttp.setRequestProperty("Content-Type", "text/xml");
        conHttp.setRequestProperty("Authorization", "Basic " + encoding);
        conHttp.setUseCaches(true);
        OutputStream outputStream = conHttp.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);
        String xmlContent = strToPost;
        writer.println(xmlContent);
        writer.flush();
        outputStream.close();
        int responseCode = conHttp.getResponseCode();
        if (responseCode >= 203 && responseCode <= 505) {
            logger.warn("Got response code of " + responseCode);
        } else {
            InputStream inputStream = conHttp.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String xmlResult = "";
            while ((xmlResult = br.readLine()) != null) {
                resp += xmlResult + "\n";
            }
        }
        return resp;
    }

    private void readXmlStream(InputStream is) throws IOException, SAXException {
        XMLReader parser = new SAXParser();
        parser.setFeature("http://xml.org/sax/features/validation", false);
        parser.setContentHandler(this);
        parser.setErrorHandler(this);
        InputSource input = new InputSource(is);
        input.setEncoding("ISO-8859-1");
        parser.parse(input);
        is.close();
    }

    public static String implodeListWithSeparator(ArrayList<String> inputList, String separatorCharacter) {
        String result = "";
        for (String element : inputList) {
            result = result + element + separatorCharacter;
        }
        if (result.endsWith(separatorCharacter)) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public void startDocument() {
        accumulator = new StringBuffer();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attributes) {
        if (localName.equals("obs.common.beans.AnnotationBean") || localName.endsWith("annotationBean")) {
            this.annotation = new ODIE_ObaAnnotationBean();
        } else if (localName.equals("semanticTypeBean")) {
            this.semanticTypeBean = new ODIE_ObaSemanticTypeBean();
        }
        accumulator.setLength(0);
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        try {
            if (localName.equals("annotationBean")) {
                if (this.annotation.getSPos() != null && this.annotation.getEPos() != null) {
                    this.annotation.getSynonyms().addAll(this.synonyms);
                    this.synonyms.clear();
                    this.annotation.getLocalSemanticTypeIDs().addAll(this.semanticTypes);
                    this.semanticTypes.clear();
                    getAnnotationsForLine().add(this.annotation);
                }
                this.annotation = null;
            } else if (this.annotation != null) {
                if (localName.equals("score")) {
                    this.annotation.setScore(Integer.valueOf(accumulator.toString().trim()));
                } else if (localName.equals("localConceptId")) {
                    this.annotation.setLocalConceptID(accumulator.toString().trim());
                } else if (localName.equals("localOntologyId")) {
                    this.annotation.setLocalOntologyID(accumulator.toString().trim());
                } else if (localName.equals("isTopLevel")) {
                    this.annotation.setTopLevel(Boolean.valueOf(accumulator.toString().trim()));
                } else if (localName.equals("preferredName")) {
                    this.annotation.setPreferredName(accumulator.toString().trim());
                } else if (localName.equals("from")) {
                    this.annotation.setSPos(Long.valueOf(accumulator.toString().trim()));
                } else if (localName.equals("to")) {
                    this.annotation.setEPos(Long.valueOf(accumulator.toString().trim()));
                } else if (localName.equals("string")) {
                    this.synonyms.add(accumulator.toString().trim());
                } else if (this.semanticTypeBean != null) {
                    if (localName.equals("conceptId")) {
                        this.semanticTypeBean.setConceptId(accumulator.toString().trim());
                    } else if (localName.equals("localSemanticTypeId")) {
                        this.semanticTypeBean.setLocalSemanticTypeId(accumulator.toString().trim());
                    } else if (localName.equals("name")) {
                        this.semanticTypeBean.setName(accumulator.toString().trim());
                    } else if (localName.equals("semanticTypeBean")) {
                        this.semanticTypes.add(this.semanticTypeBean);
                        this.semanticTypeBean = null;
                    }
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    public void endDocument() {
        Collections.sort(getAnnotationsForLine(), ODIE_ObaAnnotationBean.obaAnnotationComparator);
    }

    public void warning(SAXParseException exception) {
        logger.warn("WARNING: line " + exception.getLineNumber() + ": " + exception.getMessage());
    }

    public void error(SAXParseException exception) {
        logger.warn("ERROR: line " + exception.getLineNumber() + ": " + exception.getMessage());
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        logger.fatal("FATAL: line " + exception.getLineNumber() + ": " + exception.getMessage());
        throw (exception);
    }

    public String toString() {
        String result = "";
        for (ODIE_ObaAnnotationBean annotation : this.getAnnotations()) {
            result += annotation.toString() + "\n";
        }
        return result;
    }

    public int getCurrentLineOffset() {
        return currentLineOffset;
    }

    public void setCurrentLineOffset(int currentLineOffset) {
        this.currentLineOffset = currentLineOffset;
    }

    public int getCurrentAnnotationOffset() {
        return currentAnnotationOffset;
    }

    public void setCurrentAnnotationOffset(int currentAnnotationOffset) {
        this.currentAnnotationOffset = currentAnnotationOffset;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    private String getId() {
        return null;
    }

    public boolean addLocalOntologyId(String e) {
        return localOntologyIDs.add(e);
    }

    public void clearLocalOntologyIds() {
        localOntologyIDs.clear();
    }

    public boolean addLocalSemanticTypeId(String e) {
        return localSemanticTypeIDs.add(e);
    }

    public void clearLocalSemanticTypeIDs() {
        localSemanticTypeIDs.clear();
    }

    public boolean addMappingType(String e) {
        return mappingTypes.add(e);
    }

    public void clearMappingTypes() {
        mappingTypes.clear();
    }

    public String getDocumentText() {
        return documentText;
    }

    public void setDocumentText(String documentText) {
        this.documentText = documentText;
    }

    public ArrayList<ODIE_ObaAnnotationBean> getAnnotations() {
        return annotations;
    }

    public ArrayList<ODIE_ObaAnnotationBean> getAnnotationsForLine() {
        return annotationsForLine;
    }

    public ArrayList<String> getLocalSemanticTypeIds() {
        return localSemanticTypeIDs;
    }

    public void setLocalSemanticTypeIds(List<String> localSemanticTypeIds) {
        this.localSemanticTypeIDs.clear();
        this.localSemanticTypeIDs.addAll(localSemanticTypeIds);
    }

    public ArrayList<String> getMappingTypes() {
        return mappingTypes;
    }

    public void setMappingTypes(List<String> mappingTypes) {
        this.mappingTypes.clear();
        this.mappingTypes.addAll(mappingTypes);
    }

    public boolean isLongestOnly() {
        return longestOnly;
    }

    public void setLongestOnly(boolean longestOnly) {
        this.longestOnly = longestOnly;
    }

    public boolean isWholeWordOnly() {
        return wholeWordOnly;
    }

    public void setWholeWordOnly(boolean wholeWordOnly) {
        this.wholeWordOnly = wholeWordOnly;
    }

    public boolean isScored() {
        return scored;
    }

    public void setScored(boolean scored) {
        this.scored = scored;
    }

    public ArrayList<String> getLocalOntologyIDs() {
        return localOntologyIDs;
    }

    public void setLocalOntologyIDs(List<String> localOntologyIDs) {
        this.localOntologyIDs.clear();
        this.localOntologyIDs.addAll(localOntologyIDs);
    }

    public ArrayList<String> getLocalSemanticTypeIDs() {
        return localSemanticTypeIDs;
    }

    public void setLocalSemanticTypeIDs(List<String> localSemanticTypeIDs) {
        this.localSemanticTypeIDs.clear();
        this.localSemanticTypeIDs.addAll(localSemanticTypeIDs);
    }

    public int getLevelMin() {
        return levelMin;
    }

    public void setLevelMin(int levelMin) {
        this.levelMin = levelMin;
    }

    public int getLevelMax() {
        return levelMax;
    }

    public void setLevelMax(int levelMax) {
        this.levelMax = levelMax;
    }
}
