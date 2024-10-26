package Action.lineMode.lineModeCommon;

import java.io.BufferedInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import Action.lineMode.lineModeCommon.relaxer.LineMode;
import Action.lineMode.lineModeCommon.relaxer.FEATURE;
import Action.lineMode.lineModeCommon.relaxer.GROUP;
import Action.lineMode.lineModeCommon.relaxer.SEGMENT;
import Action.lineMode.lineModeCommon.relaxer.TMGFF;
import Action.lineMode.lineModeCommon.relaxer.TYPE;

/**
 *
 * @author kbt
 */
public class ExternalDASSearchAction extends LineModeAction {

    /** Creates a new instance of DASSearchAction */
    private HashMap<String, GROUP> groupMap = new HashMap<String, GROUP>();

    public ExternalDASSearchAction() {
    }

    public TMGFF[] rangeSearch() {
        String baseUrl = this.datasource.getUrl();
        long threshold = Long.parseLong(datasource.getProperty("max_interval"));
        StringBuffer urlBuf = new StringBuffer();
        urlBuf.append(baseUrl);
        if (baseUrl.indexOf("?") < 0) {
            urlBuf.append("?");
        }
        urlBuf.append("segment=");
        String chr = horizontal.getChromosome();
        long start = horizontal.getStart();
        long end = horizontal.getEnd();
        urlBuf.append(chr).append(":").append(start).append(",").append(end);
        String message = "";
        TMGFF tmgff = new TMGFF();
        LineModeDispatcher.setDatasourceConfig(tmgff, datasource);
        if ((end - start) > threshold) {
            tmgff.setMessage("Display Limit exceeded!!!");
            return new TMGFF[] { tmgff };
        }
        try {
            log.debug(urlBuf.toString());
            URL url = new URL(urlBuf.toString());
            parseXmlFileWithDom(urlBuf.toString());
        } catch (Exception e) {
            log.error("Load error: " + urlBuf.toString());
            log.error(e);
            e.printStackTrace();
            message = "DAS1 Server is down!";
        } finally {
        }
        if (message.equals("")) {
            SEGMENT seg = new SEGMENT();
            seg.setId(chr);
            seg.setStart(start);
            seg.setEnd(end);
            Iterator it = groupMap.values().iterator();
            while (it.hasNext()) {
                seg.addGROUP((GROUP) it.next());
            }
            tmgff.addSEGMENT(seg);
            tmgff.setIsShow(true);
        } else {
            tmgff.setMessage(message);
        }
        return new TMGFF[] { tmgff };
    }

    public TMGFF[] positionSearch(String accessionName) {
        TMGFF tmgff = new TMGFF();
        LineModeDispatcher.setDatasourceConfig(tmgff, datasource);
        return new TMGFF[] { tmgff };
    }

    private File URLDownloadToFile(String url) throws IOException {
        String fileName = System.getProperty("catalina.home") + System.getProperty("file.separator") + "webapps/gps/xml/tmp.xml";
        BufferedInputStream bis = new BufferedInputStream((new URL(url)).openConnection().getInputStream());
        int c;
        StringBuffer sb = new StringBuffer();
        while ((c = bis.read()) != -1) {
            sb.append((char) c);
        }
        FileWriter fw = new FileWriter(fileName);
        fw.write(sb.toString());
        bis.close();
        fw.flush();
        fw.close();
        return new File(fileName);
    }

    public void parseXmlFileWithDom(String filename) {
        FEATURE feature = null;
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(filename);
            NodeList features = doc.getElementsByTagName("FEATURE");
            for (int i = 0; i < features.getLength(); i++) {
                NodeList featureChilds = features.item(i).getChildNodes();
                if (!isValidData(featureChilds)) continue;
                feature = new FEATURE();
                feature.setId(((Element) features.item(i)).getAttribute("id"));
                feature.setLabel(((Element) features.item(i)).getAttribute("label"));
                for (int j = 0; j < featureChilds.getLength(); j++) {
                    if (featureChilds.item(j).getNodeName().equalsIgnoreCase("TYPE")) feature.setTYPE(this.makeTYPE("exon", featureChilds.item(j).getFirstChild().getNodeValue())); else if (featureChilds.item(j).getNodeName().equalsIgnoreCase("START")) feature.setSTARTByString(featureChilds.item(j).getFirstChild().getNodeValue()); else if (featureChilds.item(j).getNodeName().equalsIgnoreCase("END")) feature.setENDByString(featureChilds.item(j).getFirstChild().getNodeValue()); else if (featureChilds.item(j).getNodeName().equalsIgnoreCase("ORIENTATION")) feature.setORIENTATIONByString(featureChilds.item(j).getFirstChild().getNodeValue()); else if (featureChilds.item(j).getNodeName().equalsIgnoreCase("GROUP")) {
                        if (((Element) featureChilds.item(j)).getAttribute("type").equalsIgnoreCase("gene")) {
                            String groupId = ((Element) featureChilds.item(j)).getAttribute("id");
                            GROUP group = groupMap.get(groupId);
                            if (group == null) {
                                group = new GROUP();
                                group.setId(groupId);
                                group.setSTART(feature.getSTART());
                                group.setEND(feature.getEND());
                                group.setORIENTATION(feature.getORIENTATION());
                                group.setLabel(groupId);
                                group.setType("gene");
                                if (featureChilds.item(j).hasChildNodes()) {
                                    NodeList groupChilds = featureChilds.item(j).getChildNodes();
                                    for (int k = 0; k < groupChilds.getLength(); k++) {
                                        if (groupChilds.item(k).getNodeName().equalsIgnoreCase("NOTE")) group.setNOTE(groupChilds.item(k).getFirstChild().getNodeValue()); else if (groupChilds.item(k).getNodeName().equalsIgnoreCase("LINK")) group.setLINK(this.makeLINK(((Element) groupChilds.item(k)).getAttribute("href"), groupChilds.item(k).getFirstChild().getNodeValue()));
                                    }
                                } else {
                                    group.setNOTE("");
                                    group.setLINK(this.makeLINK("", ""));
                                }
                                groupMap.put(groupId, group);
                            } else {
                                if (group.getSTART() > feature.getSTART()) group.setSTART(feature.getSTART());
                                if (group.getEND() < feature.getEND()) group.setEND(feature.getEND());
                            }
                            group.addFEATURE(feature);
                        }
                    }
                }
            }
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        }
    }

    private boolean isValidData(NodeList childs) {
        for (int i = 0; i < childs.getLength(); i++) if (childs.item(i).getNodeName().equalsIgnoreCase("TYPE")) return ((Element) childs.item(i)).getAttribute("id").equalsIgnoreCase("exon");
        return false;
    }

    public void parseXmlFile(String filename, DefaultHandler handler, boolean validating) {
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(URLDownloadToFile(filename), handler);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class DasSaxHandler extends DefaultHandler {

        final String FEATURE_TAG = "FEATURE";

        final String TYPE_TAG = "TYPE";

        final String START_TAG = "START";

        final String END_TAG = "END";

        final String ORIENTATION_TAG = "ORIENTATION";

        final String GROUP_TAG = "GROUP";

        private boolean isFeatureTag = false;

        private boolean isTypeTag = false;

        private boolean isStartTag = false;

        private boolean isEndTag = false;

        private boolean isOrientationTag = false;

        private boolean isGroupTag = false;

        private String tagValue = "";

        private GROUP curGroup = null;

        private FEATURE curFeature = null;

        private TYPE curType;

        private String curStart = "";

        private String curEnd = "";

        private String curOrientation = "";

        private String curGroupId = "";

        private String curGroupLabel = "";

        private String curGroupType = "";

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
            if (qName.equalsIgnoreCase(FEATURE_TAG)) {
                isFeatureTag = true;
                if (curFeature != null && curGroup != null) {
                    curFeature.setTYPE(curType);
                    curFeature.setSTARTByString(curStart);
                    curFeature.setENDByString(curEnd);
                    curFeature.setORIENTATIONByString(curOrientation);
                    curGroup.addFEATURE(curFeature);
                }
                curFeature = new FEATURE();
            }
            if (!isFeatureTag) return;
            if (qName.equalsIgnoreCase(TYPE_TAG)) {
                isTypeTag = true;
                curType = new TYPE();
            }
            if (qName.equalsIgnoreCase(START_TAG)) isStartTag = true;
            if (qName.equalsIgnoreCase(END_TAG)) isEndTag = true;
            if (qName.equalsIgnoreCase(ORIENTATION_TAG)) isOrientationTag = true;
            if (qName.equalsIgnoreCase(GROUP_TAG)) {
                isGroupTag = true;
            }
            for (int i = 0; i < atts.getLength(); i++) {
                String name = atts.getQName(i);
                String value = atts.getValue(i);
                if (name.equalsIgnoreCase("id")) {
                    if (isTypeTag) curType.setId(value); else if (isGroupTag) curGroupId = value; else curFeature.setId(value);
                } else if (name.equalsIgnoreCase("label")) {
                    if (isGroupTag) curGroupLabel = value; else curFeature.setLabel(value);
                } else if (name.equalsIgnoreCase("type")) {
                    if (isGroupTag) curGroupType = value;
                }
            }
            System.out.println(curType.getId() + "->" + curGroupType);
            if (qName.equalsIgnoreCase(GROUP_TAG) && curType.getId().equalsIgnoreCase("exon") && curGroupType.equalsIgnoreCase("gene")) {
                curGroup = groupMap.get(curGroupId);
                if (curGroup == null) {
                    curGroup = new GROUP();
                    curGroup.setId(curGroupId);
                    curGroup.setLabel(curGroupLabel);
                    curGroup.setType(curGroupType);
                    groupMap.put(curGroupId, curGroup);
                }
                System.out.println("Group-" + curGroupId);
            }
        }

        public void characters(char[] c, int start, int length) {
            if (!isFeatureTag) return;
            for (int i = 0; i < length; i++, tagValue += c[start++]) ;
        }

        public void endElement(String namespaceURI, String localName, String qName, Attributes atts) {
            if (!isFeatureTag) return;
            if (qName.equalsIgnoreCase(FEATURE_TAG)) {
                isFeatureTag = false;
            } else if (qName.equalsIgnoreCase(TYPE_TAG)) {
                isTypeTag = false;
                if (isFeatureTag && !tagValue.equals("")) {
                    curType.setContent(tagValue);
                }
            } else if (qName.equalsIgnoreCase(START_TAG)) {
                isStartTag = false;
                if (isFeatureTag && !tagValue.equals("")) curStart = tagValue;
            } else if (qName.equalsIgnoreCase(END_TAG)) {
                isEndTag = false;
                if (isFeatureTag && !tagValue.equals("")) curEnd = tagValue;
            } else if (qName.equalsIgnoreCase(ORIENTATION_TAG)) {
                isOrientationTag = false;
                if (isFeatureTag && !tagValue.equals("")) curOrientation = tagValue;
            } else if (qName.equalsIgnoreCase(GROUP_TAG)) {
                isGroupTag = false;
                if (isFeatureTag && !tagValue.equals("")) System.out.println("\tTag values " + tagValue);
            }
            tagValue = "";
        }
    }
}
