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
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
import com.google.code.guidatv.model.Transmission;
import com.google.code.guidatv.server.service.GuidaTvException;

public class IrisTransmissionDaoImpl {

    private static final String BASE_URL = "http://www.iris.mediaset.it/palinsesto/palinsesto";

    private static final long ONE_DAY = 1000 * 60 * 60 * 24;

    public Map<Date, List<Transmission>> getTransmissions(Date day) {
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
        if (daysAfterToday < 0 || daysAfterToday > 7) {
            return null;
        }
        Map<Date, List<Transmission>> retValue = new HashMap<Date, List<Transmission>>();
        long htmlNumber = daysAfterToday / 2 + 1;
        Date firstDate;
        Date secondDate;
        if (daysAfterToday % 2 == 0) {
            firstDate = cal.getTime();
            cal.add(Calendar.DATE, 1);
            secondDate = cal.getTime();
        } else {
            secondDate = cal.getTime();
            cal.add(Calendar.DATE, -1);
            firstDate = cal.getTime();
        }
        Reader reader = null;
        try {
            URL url = new URL(BASE_URL + htmlNumber + ".shtml");
            InputStream is = url.openStream();
            reader = new InputStreamReader(is, "UTF-8");
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(reader));
            Document document = parser.getDocument();
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression dayExpression = xpath.compile("//DIV[@class='day']");
            NodeList dayNodeList = (NodeList) dayExpression.evaluate(document, XPathConstants.NODESET);
            DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getInstance();
            numberFormat.applyPattern("00");
            retValue.put(firstDate, getTransmissions(firstDate, (Element) dayNodeList.item(0), numberFormat));
            retValue.put(secondDate, getTransmissions(secondDate, (Element) dayNodeList.item(1), numberFormat));
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
        return retValue;
    }

    private List<Transmission> getTransmissions(Date day, Element dayNode, DecimalFormat numberFormat) throws ParseException {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        String name = null;
        String description = null;
        Date start = null;
        NodeList nodeList = dayNode.getChildNodes();
        cal.setTime(day);
        List<Transmission> transmissions = new ArrayList<Transmission>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            if (item instanceof Element) {
                Element element = (Element) item;
                if ("DIV".equals(element.getTagName())) {
                    String clazz = element.getAttribute("class");
                    if ("ora".equals(clazz)) {
                        NodeList children = element.getChildNodes();
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
                                }
                            }
                        }
                    } else if ("info".equals(clazz)) {
                        NodeList children = element.getChildNodes();
                        for (int j = 0; j < children.getLength(); j++) {
                            Node child = children.item(j);
                            if (child instanceof Element) {
                                Element childElement = (Element) child;
                                Text firstChild = (Text) childElement.getFirstChild();
                                if ("H4".equals(childElement.getTagName())) {
                                    name = firstChild.getWholeText();
                                } else if ("P".equals(childElement.getTagName())) {
                                    description = firstChild.getWholeText();
                                }
                            }
                        }
                    } else if ("clearer".equals(clazz)) {
                        transmissions.add(new Transmission(name, description, start, null, null));
                        name = null;
                        description = null;
                        start = null;
                    }
                }
            }
        }
        return transmissions;
    }
}
