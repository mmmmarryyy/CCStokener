package net.sourceforge.interpay.client;

import java.io.*;
import java.net.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

/**
 * Command line client interface to demostrate the use of payment gateway.
 * It also uses the XSLT to output HTML if the input is Credit Card.
 * Otherwise, the output will be default XML.
 * 
 * @author Kong Wang
 */
public class PaymentClient {

    private static String mid, cardNum, cardExpDate, amount, type, cvv, refid, firstName, lastName, address1, city, state, zip, request, routingNum, accountNum;

    private static Document document = null;

    private static final String gatewayURL = "http://www.someurl.com/payGate";

    private static final String styleSheetLocation = "authorization.xsl";

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 12) {
            usage();
        }
        setArgs(args);
        buildCreditCardXML();
        xmlToString();
        sentRequest();
        System.exit(0);
    }

    /**
     * @throws MalformedURLException
     * @throws IOException
     */
    private static void sentRequest() {
        try {
            URLConnection urlConn;
            URL url = new URL(gatewayURL);
            urlConn = url.openConnection();
            urlConn.setRequestProperty("Content-Type", "text/xml");
            urlConn.setDoOutput(true);
            OutputStream ostream = urlConn.getOutputStream();
            PrintWriter out = new PrintWriter(ostream);
            out.print(request);
            out.close();
            ostream.close();
            InputStream inStream = urlConn.getInputStream();
            File myFile = new File(styleSheetLocation);
            if (type.equals("A") && myFile.exists()) {
                TransformerFactory tFactory = TransformerFactory.newInstance();
                Transformer transformer = tFactory.newTransformer(new StreamSource(styleSheetLocation));
                transformer.transform(new StreamSource(inStream), new StreamResult(System.out));
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(inputLine);
                }
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerConfigurationException
     * @throws TransformerException
     * @throws UnsupportedEncodingException
     */
    private static void xmlToString() {
        try {
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(document);
            trans.transform(source, result);
            request = sw.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args
     */
    private static void setArgs(String[] args) {
        mid = args[0];
        amount = args[1];
        type = args[2];
        refid = args[3];
        firstName = args[4];
        lastName = args[5];
        address1 = args[6];
        city = args[7];
        state = args[8];
        zip = args[9];
        if (type.equals("C")) {
            routingNum = args[10];
            accountNum = args[11];
        } else {
            cardNum = args[10];
            cardExpDate = args[11];
            cvv = args[12];
        }
    }

    /**
     * build DOM tree using JAXP
     */
    private static void buildCreditCardXML() {
        DocumentBuilder builder = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
            document = builder.newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Element root = (Element) document.createElement("request");
        document.appendChild(root);
        insertNode(root, "mid", mid);
        insertNode(root, "type", type);
        insertNode(root, "refid", refid);
        Element account = document.createElement("account");
        root.appendChild(account);
        if (type.equals("C")) {
            insertNode(account, "routingNum", routingNum);
            insertNode(account, "accountNum", accountNum);
        } else {
            insertNode(account, "cardNum", cardNum);
            Element expDate = document.createElement("expDate");
            account.appendChild(expDate);
            String expMonth = cardExpDate.substring(0, 2);
            String expYear = cardExpDate.substring(2, 4);
            insertNode(expDate, "expMonth", expMonth);
            insertNode(expDate, "expYear", expYear);
            insertNode(account, "cvv2", cvv);
        }
        insertNode(account, "amount", amount);
        Element customer = document.createElement("customer");
        account.appendChild(customer);
        Element name = document.createElement("name");
        customer.appendChild(name);
        insertNode(name, "fname", firstName);
        insertNode(name, "lname", lastName);
        Element address = document.createElement("address");
        customer.appendChild(address);
        insertNode(address, "address1", address1);
        insertNode(address, "city", city);
        insertNode(address, "state", state);
        insertNode(address, "zip", zip);
    }

    /**
     * @param element
     */
    private static void insertNode(Element element, String nodeName, String nodeValue) {
        Node item, value;
        item = document.createElement(nodeName);
        value = document.createTextNode(nodeValue);
        item.appendChild(value);
        element.appendChild(item);
    }

    private static void usage() {
        System.out.println("usage: java PaymentClient mid amount type refid firstName lastName address1 city state zip cardNum cardExpDate cvv");
        System.exit(1);
    }
}
