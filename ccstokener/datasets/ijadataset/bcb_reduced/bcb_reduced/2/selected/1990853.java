package org.wd.extractor.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.wd.extractor.util.Constants;

public class TrainInfoService {

    public void process(String number) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(Constants.TRAIN_INFO_URL.value());
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair(Constants.TRAIN_NUMBER_POST_PARAM_NAME.value(), number));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httpPost);
            InputStream is = response.getEntity().getContent();
            Document doc = getDocument(is);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile(Constants.XPATH_TRAIN_STOPS_INFO.value());
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            List<String> list = new ArrayList<String>();
            for (int i = 0; i < nodes.getLength(); i++) {
                list.add(nodes.item(i).getNodeValue());
            }
            parse(list);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Returns well formed document which can be queried using XPATH API
	 * 
	 * @param is
	 * @return
	 */
    private Document getDocument(InputStream is) {
        return null;
    }

    private void parse(List<String> list) {
        if (list.size() >= 9) {
            int stopsBeginIndex = list.indexOf("1");
            List<String[]> route = new ArrayList<String[]>();
            if (!list.toString().contains("Slip Route")) {
                for (int index = stopsBeginIndex; index + 9 <= list.size(); index += 9) {
                    List<String> stop = list.subList(index, index + 9);
                    route.add(stop.toArray(new String[stop.size()]));
                }
                generateXML(route, list);
            } else {
                System.out.println("skipped: " + list);
            }
        }
    }

    private void generateXML(List<String[]> route, List<String> list) {
        List<String> days = list.subList(3, list.indexOf("1"));
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.newDocument();
            Element root = doc.createElement("train");
            doc.appendChild(root);
            Element number = doc.createElement("number");
            Text numberText = doc.createTextNode(list.get(0));
            number.appendChild(numberText);
            Element name = doc.createElement("name");
            Text nameText = doc.createTextNode(list.get(1));
            name.appendChild(nameText);
            Element origin = doc.createElement("origin");
            Text originText = doc.createTextNode(list.get(2));
            origin.appendChild(originText);
            root.appendChild(number);
            root.appendChild(name);
            root.appendChild(origin);
            for (String d : days) {
                Element day = doc.createElement("day");
                Text dayText = doc.createTextNode(d);
                day.appendChild(dayText);
                root.appendChild(day);
            }
            Element stops = doc.createElement("stops");
            for (String[] s : route) {
                Element stop = doc.createElement("stop");
                Element stationIndex = doc.createElement("index");
                stationIndex.appendChild(doc.createTextNode(s[0]));
                Element stationCode = doc.createElement("code");
                stationCode.appendChild(doc.createTextNode(s[1]));
                Element stationName = doc.createElement("name");
                stationName.appendChild(doc.createTextNode(s[2]));
                Element routeNumber = doc.createElement("routenumber");
                routeNumber.appendChild(doc.createTextNode(s[3]));
                Element arrival = doc.createElement("arrival");
                arrival.appendChild(doc.createTextNode(s[4]));
                Element departure = doc.createElement("departure");
                departure.appendChild(doc.createTextNode(s[5]));
                Element halt = doc.createElement("halt");
                halt.appendChild(doc.createTextNode(s[6]));
                Element distance = doc.createElement("distance");
                distance.appendChild(doc.createTextNode(s[7]));
                Element arrivalday = doc.createElement("arrivalday");
                arrivalday.appendChild(doc.createTextNode(s[8]));
                stop.appendChild(stationIndex);
                stop.appendChild(stationCode);
                stop.appendChild(stationName);
                stop.appendChild(routeNumber);
                stop.appendChild(arrival);
                stop.appendChild(departure);
                stop.appendChild(halt);
                stop.appendChild(distance);
                stop.appendChild(arrivalday);
                stops.appendChild(stop);
            }
            root.appendChild(stops);
            Source source = new DOMSource(doc);
            File file = new File("D:/work/train/xml/" + list.get(0) + ".xml");
            Result result = new StreamResult(file);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        System.out.println("obtained: " + list);
    }
}
