package pedro.soa.alerts;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import pedro.system.PedroException;
import pedro.util.PedroXMLParsingUtility;
import pedro.mda.model.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AlertsBundleReader {

    private final int BUFFER_SIZE = 2048;

    private ArrayList alertFiles;

    private ArrayList alerts;

    private String zipFileName;

    public AlertsBundleReader() {
        alertFiles = new ArrayList();
        alerts = new ArrayList();
    }

    public ArrayList getAlerts() {
        return alerts;
    }

    public void readFile(URL url) throws PedroException, IOException, ParserConfigurationException, SAXException {
        this.zipFileName = url.toString();
        URLConnection urlConnection = url.openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        unzipNativeFormatFile(inputStream);
        parseAlertFiles();
        deleteAlertFiles();
    }

    public void readFile(File zipFile) throws PedroException, IOException, ParserConfigurationException, SAXException {
        this.zipFileName = zipFile.getAbsolutePath();
        FileInputStream inputStream = new FileInputStream(zipFile);
        unzipNativeFormatFile(inputStream);
        parseAlertFiles();
        deleteAlertFiles();
    }

    private Alert parseAlert(File file) throws ParserConfigurationException, IOException, SAXException {
        Alert alert = new Alert();
        alert.setBundleName(zipFileName);
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(file);
        Element alertElement = PedroXMLParsingUtility.getFirstChildElement(document);
        Element alertTypeElement = PedroXMLParsingUtility.getElement(alertElement, "alertType");
        String phrase = PedroXMLParsingUtility.getValue(alertTypeElement);
        AlertActionType alertType = AlertActionType.getActionTypeForPhrase(phrase);
        alert.setAlertType(alertType);
        Element nameElement = PedroXMLParsingUtility.getElement(alertElement, "name");
        String name = PedroXMLParsingUtility.getValue(nameElement);
        alert.setName(name);
        Element messageElement = PedroXMLParsingUtility.getElement(alertElement, "message");
        String message = PedroXMLParsingUtility.getValue(messageElement);
        alert.setMessage(message);
        Element authorElement = PedroXMLParsingUtility.getElement(alertElement, "author");
        String author = PedroXMLParsingUtility.getValue(authorElement);
        alert.setAuthor(author);
        Element institutionElement = PedroXMLParsingUtility.getElement(alertElement, "institution");
        String institution = PedroXMLParsingUtility.getValue(institutionElement);
        alert.setInstitution(institution);
        Element emailAddressElement = PedroXMLParsingUtility.getElement(alertElement, "emailAddress");
        String emailAddress = PedroXMLParsingUtility.getValue(emailAddressElement);
        alert.setEmailAddress(emailAddress);
        Element recordTypeElement = PedroXMLParsingUtility.getElement(alertElement, "recordType");
        String recordClassContext = PedroXMLParsingUtility.getValue(recordTypeElement);
        alert.setRecordClassContext(recordClassContext);
        Element matchingCriteriaElement = PedroXMLParsingUtility.getElement(alertElement, "matchingCriteria");
        MatchingCriteria matchingCriteria = alert.getMatchingCriteria();
        Node currentNode = PedroXMLParsingUtility.getFirstChildElement(matchingCriteriaElement);
        while (currentNode != null) {
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element currentCriterion = (Element) currentNode;
                String tagName = currentCriterion.getTagName();
                if (tagName.equals("listFieldCriterion") == true) {
                    ListFieldMatchingCriterion listFieldMatchingCriterion = parseListFieldCriterion(currentCriterion);
                    matchingCriteria.addMatchingCriterion(listFieldMatchingCriterion);
                } else if (tagName.equals("editFieldCriterion") == true) {
                    EditFieldMatchingCriterion editFieldMatchingCriterion = parseEditFieldCriterion(currentCriterion);
                    matchingCriteria.addMatchingCriterion(editFieldMatchingCriterion);
                }
            }
            currentNode = currentNode.getNextSibling();
        }
        ArrayList criteria = matchingCriteria.getCriteria();
        return alert;
    }

    private EditFieldMatchingCriterion parseEditFieldCriterion(Element element) {
        EditFieldMatchingCriterion criterionModel = new EditFieldMatchingCriterion();
        Element fieldNameElement = PedroXMLParsingUtility.getElement(element, "fieldName");
        String fieldName = PedroXMLParsingUtility.getValue(fieldNameElement);
        criterionModel.setFieldName(fieldName);
        Element operatorElement = PedroXMLParsingUtility.getElement(element, "operator");
        String operatorPhrase = PedroXMLParsingUtility.getValue(operatorElement);
        FieldOperator operator = FieldOperator.getOperatorForPhrase(operatorPhrase);
        criterionModel.setOperator(operator);
        Element comparedValueElement = PedroXMLParsingUtility.getElement(element, "comparedValue");
        String comparedValue = PedroXMLParsingUtility.getValue(comparedValueElement);
        criterionModel.setComparedValue(comparedValue);
        String val = criterionModel.getComparedValue();
        return criterionModel;
    }

    private ListFieldMatchingCriterion parseListFieldCriterion(Element element) {
        ListFieldMatchingCriterion criterionModel = new ListFieldMatchingCriterion();
        Element fieldNameElement = PedroXMLParsingUtility.getElement(element, "fieldName");
        String fieldName = PedroXMLParsingUtility.getValue(fieldNameElement);
        criterionModel.setFieldName(fieldName);
        Element operatorElement = PedroXMLParsingUtility.getElement(element, "operator");
        String operatorPhrase = PedroXMLParsingUtility.getValue(operatorElement);
        FieldOperator operator = FieldOperator.getOperatorForPhrase(operatorPhrase);
        criterionModel.setOperator(operator);
        Element comparedValueElement = PedroXMLParsingUtility.getElement(element, "comparedValue");
        String comparedValue = PedroXMLParsingUtility.getValue(comparedValueElement);
        try {
            int value = Integer.valueOf(comparedValue).intValue();
            criterionModel.setComparedValue(value);
        } catch (Exception err) {
            criterionModel.setComparedValue(0);
        }
        Element comparedChildTypeElement = PedroXMLParsingUtility.getElement(element, "comparedChildType");
        String comparedChildType = PedroXMLParsingUtility.getValue(comparedChildTypeElement);
        criterionModel.setComparedChildType(comparedChildType);
        return criterionModel;
    }

    /**
     * assumes that a file of format "x.pdz" is passed,
     * and that a file "x.pdr" can be extracted
     */
    private void unzipNativeFormatFile(InputStream inputStream) throws IOException, PedroException {
        BufferedOutputStream dest = null;
        ZipInputStream zipIn = new ZipInputStream(inputStream);
        ZipEntry entry;
        while ((entry = zipIn.getNextEntry()) != null) {
            int count = 0;
            byte data[] = new byte[BUFFER_SIZE];
            String currentFileName = entry.getName();
            File currentAlertFile = new File(currentFileName);
            alertFiles.add(currentAlertFile);
            FileOutputStream nativeFileOutputStream = new FileOutputStream(currentFileName);
            dest = new BufferedOutputStream(nativeFileOutputStream, BUFFER_SIZE);
            while ((count = zipIn.read(data, 0, BUFFER_SIZE)) != -1) {
                dest.write(data, 0, count);
            }
            dest.flush();
            dest.close();
        }
        zipIn.close();
    }

    private void parseAlertFiles() throws ParserConfigurationException, IOException, SAXException {
        alerts.clear();
        int numberOfFiles = alertFiles.size();
        for (int i = 0; i < numberOfFiles; i++) {
            File currentFile = (File) alertFiles.get(i);
            Alert alert = parseAlert(currentFile);
            alerts.add(alert);
        }
    }

    private void deleteAlertFiles() {
        int numberOfFiles = alertFiles.size();
        for (int i = 0; i < numberOfFiles; i++) {
            File currentFile = (File) alertFiles.get(i);
            currentFile.delete();
        }
    }
}
