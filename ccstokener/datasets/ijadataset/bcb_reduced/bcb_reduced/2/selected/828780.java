package com.google.code.guidatv.server.service.impl.italy.generalistic.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.google.code.guidatv.model.Channel;
import com.google.code.guidatv.model.Transmission;
import com.google.code.guidatv.server.service.GuidaTvException;

public class TelecomTransmissionDaoImpl implements TelecomTransmissionDao {

    private static final class NamespaceContextImpl implements NamespaceContext {

        private static final String URI = "http://www.w3.org/1999/xhtml";

        private static final String PREFIX = "ns";

        private List<String> prefixes;

        public NamespaceContextImpl() {
            prefixes = new ArrayList<String>();
            prefixes.add(PREFIX);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Iterator getPrefixes(String namespaceURI) {
            if (URI.equals(namespaceURI)) {
                return prefixes.iterator();
            }
            return null;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            if (URI.equals(namespaceURI)) {
                return PREFIX;
            }
            return null;
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (PREFIX.equals(prefix)) {
                return URI;
            }
            return null;
        }
    }

    private static final long ONE_DAY = 1000 * 60 * 60 * 24;

    private Map<String, String> channel2url;

    private Map<String, String> channel2path;

    public TelecomTransmissionDaoImpl() {
        channel2url = new HashMap<String, String>();
        channel2url.put("La7", "http://www.la7.it/guidatv/index");
        channel2url.put("La7d", "http://www.la7.it/guidatv/indexd");
        channel2path = new HashMap<String, String>();
        channel2path.put("La7", "//ns:DIV[@id='palinsesto_la7']/ns:DIV/ns:DIV//ns:LI");
        channel2path.put("La7d", "//ns:DIV[@id='palinsesto_la7d']/ns:DIV/ns:DIV//ns:LI");
    }

    @Override
    public List<Transmission> getTransmissions(Channel channel, Date day) {
        String baseUrl = channel2url.get(channel.getCode());
        String path = channel2path.get(channel.getCode());
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Rome");
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTime(day);
        cal.setTimeZone(timeZone);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Calendar nowCal = Calendar.getInstance(timeZone);
        nowCal.set(Calendar.HOUR_OF_DAY, 0);
        nowCal.set(Calendar.MINUTE, 0);
        nowCal.set(Calendar.SECOND, 0);
        nowCal.set(Calendar.MILLISECOND, 0);
        long daysAfterToday = (cal.getTimeInMillis() - nowCal.getTimeInMillis()) / ONE_DAY;
        if (daysAfterToday < 0 || daysAfterToday > 6) {
            return null;
        }
        String htmlNumber;
        if (daysAfterToday == 0) {
            htmlNumber = "";
        } else {
            htmlNumber = "_" + Long.toString(daysAfterToday);
        }
        Reader reader = null;
        try {
            URL url = new URL(baseUrl + htmlNumber + ".html");
            InputStream is = url.openStream();
            reader = new InputStreamReader(is, "UTF-8");
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(reader));
            Document document = parser.getDocument();
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new NamespaceContextImpl());
            DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getInstance();
            numberFormat.applyPattern("00");
            XPathExpression dayExpression = xpath.compile(path);
            NodeList dayNodeList = (NodeList) dayExpression.evaluate(document, XPathConstants.NODESET);
            return getTransmissions(day, dayNodeList, numberFormat, xpath);
        } catch (MalformedURLException e) {
            throw new GuidaTvException(e);
        } catch (IOException e) {
            throw new GuidaTvException(e);
        } catch (SAXException e) {
            throw new GuidaTvException(e);
        } catch (XPathExpressionException e) {
            throw new GuidaTvException(e);
        } catch (ParseException e) {
            throw new GuidaTvException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<Transmission> getTransmissions(Date day, NodeList nodeList, DecimalFormat numberFormat, XPath xpath) throws ParseException, XPathExpressionException {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        String name = null;
        String description = null;
        String link = null;
        Date start = null;
        cal.setTime(day);
        List<Transmission> transmissions = new ArrayList<Transmission>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            if (item instanceof Element) {
                NodeList elementChildren = item.getChildNodes();
                for (int k = 0; k < elementChildren.getLength(); k++) {
                    Node itemChild = elementChildren.item(k);
                    if (itemChild instanceof Element) {
                        Element elementChild = (Element) itemChild;
                        if ("DIV".equals(elementChild.getTagName())) {
                            String clazz = elementChild.getAttribute("class");
                            if ("sx".equals(clazz)) {
                                NodeList children = elementChild.getChildNodes();
                                for (int j = 0; j < children.getLength(); j++) {
                                    Node child = children.item(j);
                                    if (child instanceof Element) {
                                        Element childElement = (Element) child;
                                        if ("P".equals(childElement.getTagName())) {
                                            Node firstChild = childElement.getFirstChild();
                                            String timeString = ((Text) firstChild).getWholeText().trim();
                                            String hour = timeString.substring(0, 2);
                                            String minutes = timeString.substring(3, 5);
                                            cal.setTime(day);
                                            cal.set(Calendar.HOUR_OF_DAY, 0);
                                            cal.set(Calendar.MINUTE, 0);
                                            cal.set(Calendar.SECOND, 0);
                                            cal.set(Calendar.MILLISECOND, 0);
                                            int hours = numberFormat.parse(hour).intValue();
                                            int minute = numberFormat.parse(minutes).intValue();
                                            if (hours < 6 || (hours == 6 && minute == 0 && i != 0)) {
                                                cal.add(Calendar.DAY_OF_YEAR, 1);
                                            }
                                            cal.set(Calendar.HOUR_OF_DAY, hours);
                                            cal.set(Calendar.MINUTE, minute);
                                            start = cal.getTime();
                                        } else if ("H5".equals(childElement.getTagName())) {
                                            NodeList children2 = childElement.getChildNodes();
                                            for (int l = 0; l < children2.getLength(); l++) {
                                                Node child2 = children2.item(l);
                                                if (child2 instanceof Text) {
                                                    String textNodeContent = getCleanedTextContent((Text) child2);
                                                    if (textNodeContent.length() > 0) {
                                                        name = textNodeContent;
                                                    }
                                                } else if (child2 instanceof Element) {
                                                    Element childElement2 = (Element) child2;
                                                    if ("A".equals(childElement2.getTagName())) {
                                                        link = childElement2.getAttribute("href");
                                                        if (link != null && !link.startsWith("http")) {
                                                            link = "http://www.la7.it" + link;
                                                        }
                                                        Node anchorContent = childElement2.getFirstChild();
                                                        if (anchorContent instanceof Text) {
                                                            name = getCleanedTextContent((Text) anchorContent);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if ("dx".equals(clazz)) {
                                NodeList children = elementChild.getChildNodes();
                                StringBuilder builder = new StringBuilder();
                                for (int j = 0; j < children.getLength(); j++) {
                                    Node child = children.item(j);
                                    if (child instanceof Text) {
                                        if (builder.length() > 0) {
                                            builder.append(" ");
                                        }
                                        builder.append(((Text) child).getWholeText());
                                    }
                                }
                                description = builder.toString().replaceAll("^\\s*|\\s*$", "");
                            }
                        }
                    }
                }
            }
            transmissions.add(new Transmission(name, description, start, null, link));
            name = null;
            description = null;
            link = null;
            start = null;
        }
        return transmissions;
    }

    private String getCleanedTextContent(Text textNode) {
        String textNodeContent = textNode.getWholeText();
        return textNodeContent.replaceAll("^\\s*|\\s*$", "");
    }
}
