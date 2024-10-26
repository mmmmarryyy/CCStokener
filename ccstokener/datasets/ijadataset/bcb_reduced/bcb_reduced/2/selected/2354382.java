package zing.config;

import java.util.*;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.net.URL;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import zing.tests.ITest;

/**
 * Class for performing common assertions. Provides an extension to Assert
 * class provided by JUnit framework.			 
 */
public class AssertHelper {

    private static Stack nodestack = new Stack();

    /**
     * Asserts if two collections are equal ignoring their sort order.
     *
     * @param collExp Denotes expected collection object.
     * @param collAct Denotes actual collection object.
     */
    public static void assertEqualsIgnoreOrder(Collection collExp, Collection collAct) {
        if (!((collExp == null && collAct == null) || (collExp != null && collAct != null && collExp.size() == collAct.size() && collExp.containsAll(collAct)))) {
            throw new AssertionFailedError();
        }
    }

    /**
     * Asserts if two arrays of objects are equal.
     *
     * @param objExp Denotes expected array of object.
     * @param objAct Denotes actual array of object.
     */
    public static void assertEquals(Object[] objExp, Object[] objAct) {
        if (objExp == null && objAct != null || objExp != null && objAct == null) {
            throw new AssertionFailedError();
        }
        if (objExp != null && objAct != null) {
            if (objExp.length == objAct.length) {
                for (int i = 0; i < objExp.length; i++) {
                    Assert.assertEquals(objExp[i], objAct[i]);
                }
            } else {
                throw new AssertionFailedError();
            }
        }
    }

    /**
     * Asserts if given exception is valid for the scenario.
     *
     * @param hmpExceptions Denotes hashmap of exception codes for the scenario.
     * @param e Denotes actual exception.
     * @throws Exception In case of error.
     * @deprecated see assertException( String expected, Exception e ).
     */
    public static void assertEx(HashMap hmpExceptions, Exception e) throws Exception {
        if (null != hmpExceptions) {
            if (!hmpExceptions.containsValue(e.getClass().getName())) {
                throw new AssertionFailedError();
            }
        } else {
            throw e;
        }
    }

    /**
     * Asserts if given exception is valid for the scenario.
     *
     * @param expected Denotes hashmap of exception codes for the scenario.
     * @param e Denotes actual exception.
     * @throws Exception In case of error. .
     */
    public static void assertException(String expected, Exception e) throws Exception {
        if (null != expected) {
            if (expected.startsWith(ConfigReader.EXCEPTION_VALUE_PREFIX)) {
                assertEquals(expected.substring(ConfigReader.EXCEPTION_VALUE_PREFIX.length()), e);
            } else if (!expected.equals(e.getClass().getName())) {
                throw new AssertionFailedError();
            }
        } else {
            throw e;
        }
    }

    /**
     * Method to assert only those elements that are specified in the output
     * xml.
     * @param xmlpath path of the output xml.
     * @param actualObject actual output.
     * @throws AssertionFailedError in case of exception.
     */
    public static void assertEquals(String xmlpath, Object actualObject) throws Exception {
        InputStreamReader isr;
        try {
            isr = new FileReader(xmlpath);
        } catch (FileNotFoundException e) {
            URL url = AssertHelper.class.getClassLoader().getResource(xmlpath);
            if (null != url) {
                try {
                    isr = new InputStreamReader(url.openStream());
                } catch (Exception e1) {
                    throw new AssertionFailedError("Unable to find output xml : " + xmlpath);
                }
            } else {
                throw new AssertionFailedError("Could not read output xml : " + xmlpath);
            }
        }
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(isr));
        Document document = parser.getDocument();
        try {
            assertEqual(document.getDocumentElement(), actualObject);
        } catch (AssertionFailedError e) {
            String message = null;
            if (null != e.getCause()) {
                message = e.getCause().getMessage();
            } else {
                message = e.getMessage();
            }
            StringBuffer sbf = new StringBuffer(message + " \n " + xmlpath);
            Iterator iter = nodestack.iterator();
            while (iter.hasNext()) {
                sbf.append(" -> " + ((Object[]) iter.next())[0]);
                iter.remove();
            }
            AssertionFailedError a = new AssertionFailedError(sbf.toString());
            a.setStackTrace(e.getStackTrace());
            throw a;
        } catch (Exception e) {
            String message = null;
            if (null != e.getCause()) {
                message = e.getCause().getMessage();
            } else {
                message = e.getMessage();
            }
            StringBuffer sbf = new StringBuffer(message + " \n " + xmlpath);
            Iterator iter = nodestack.iterator();
            while (iter.hasNext()) {
                sbf.append(" -> " + ((Object[]) iter.next())[0]);
                iter.remove();
            }
            Exception ex = new Exception(sbf.toString());
            ex.setStackTrace(e.getStackTrace());
            throw ex;
        }
    }

    /**
     * Asserts a node in xml against a corresponding attribute of an object.
     *
     * @param node in xml.
     * @param actualObject actual object.
     */
    private static void assertEqual(Node node, Object actualObject) {
        nodestack.push(new Object[] { node.getNodeName(), actualObject });
        if (isSimpleDataType(actualObject)) {
            assertEqual(actualObject, node);
        } else if (actualObject instanceof Collection) {
            assertCollection(actualObject, node);
        } else {
            if (node.hasChildNodes()) {
                NodeList list = node.getChildNodes();
                for (int i = 0; i < list.getLength(); i++) {
                    node = list.item(i);
                    if (node.getNodeName().equals("#text") || node.getNodeName().equals("#comment")) continue;
                    Object obj = getObjectProperty(node, actualObject);
                    assertEqual(node, obj);
                }
            } else if (null != node.getAttributes().getNamedItem("reference")) {
                int count = (new StringTokenizer(node.getAttributes().getNamedItem("reference").getNodeValue(), "/")).countTokens();
                Object objExpected = ((Object[]) nodestack.get(nodestack.size() - count - 1))[1];
                if (objExpected != actualObject) {
                    String actual = "null";
                    if (null != actualObject) actual = actualObject.getClass() + "@" + Integer.toHexString(actualObject.hashCode());
                    throw new AssertionFailedError("expected <" + objExpected.getClass() + "@" + Integer.toHexString(objExpected.hashCode()) + "> but was <" + actual + ">");
                }
            }
        }
        nodestack.pop();
    }

    /**
     * Method to retrieve the attribute from the object with name present
     * in the node.
     * @param node xml node.
     * @param actualObject actual object.
     * @return object attribute with given name.
     */
    private static Object getObjectProperty(Node node, Object actualObject) {
        Method method = null;
        Object obj = null;
        try {
            String attribute = node.getNodeName().substring(0, 1).toUpperCase() + node.getNodeName().substring(1);
            try {
                method = actualObject.getClass().getMethod("get" + attribute, null);
                obj = method.invoke(actualObject, null);
            } catch (NoSuchMethodException e) {
                try {
                    method = actualObject.getClass().getMethod("is" + attribute, null);
                    obj = method.invoke(actualObject, null);
                } catch (NoSuchMethodException e1) {
                    Field field = actualObject.getClass().getField(node.getNodeName());
                    obj = field.get(actualObject);
                }
            }
        } catch (Exception e) {
            nodestack.push(new Object[] { node.getNodeName() });
            throw new AssertionFailedError("Attribute does not exist or is not accessible ");
        }
        return obj;
    }

    /**
     * Asserts the collection object ignoring the order.
     * @param obj Collection.
     * @param rootnode xml representation of the collection.
     */
    private static void assertCollection(Object obj, Node rootnode) {
        List list = new ArrayList();
        if (obj instanceof List) {
            list.addAll((List) obj);
        }
        NodeList nodelist = rootnode.getChildNodes();
        Node node;
        ArrayList nodes = new ArrayList();
        for (int i = 0; i < nodelist.getLength(); i++) {
            node = nodelist.item(i);
            if (!(node.getNodeName().equals("#text") || node.getNodeName().equals("#comment"))) {
                nodes.add(node);
            }
        }
        Assert.assertEquals("Size of " + rootnode.getNodeName(), nodes.size(), list.size());
        boolean matchFound = false;
        Node failedNode = null;
        for (int i = 0; i < nodes.size(); i++) {
            node = (Node) nodes.get(i);
            matchFound = false;
            for (int j = 0; j < list.size(); j++) {
                int size = nodestack.size();
                try {
                    assertEqual(node, list.get(j));
                } catch (AssertionFailedError afe) {
                    while (nodestack.size() != size) {
                        nodestack.pop();
                    }
                    continue;
                }
                list.remove(j);
                matchFound = true;
                break;
            }
            if (!matchFound) {
                failedNode = node;
            }
        }
        if (failedNode != null) {
            assertEqual(failedNode, list.get(0));
        }
    }

    /**
     * Asserts simple object against the node value.
     * @param obj simple object.
     * @param node containing value.
     */
    private static void assertEqual(Object obj, Node node) {
        if (!node.getFirstChild().getNodeValue().trim().equals(obj.toString())) {
            throw new AssertionFailedError("expected <" + node.getFirstChild().getNodeValue().trim() + "> but was <" + obj.toString() + ">");
        }
    }

    /**
     * Checks if object is a simple object.
     * @param obj to be checked.
     * @return boolean.
     */
    private static boolean isSimpleDataType(Object obj) {
        return obj instanceof String || obj instanceof Long || obj instanceof Integer || obj instanceof Float || obj instanceof Double || obj instanceof java.util.Date || obj instanceof java.sql.Date || obj instanceof java.sql.Timestamp || obj instanceof Byte || obj instanceof Boolean;
    }

    /**
     * Method used to assert the expected output with the actual output.
     * @param strKey Denotes the name against which expected output is configured.
     * @param objActual Denotes the actual output.
     * @param iTest Denotes the test on which assertion is being called.
     * @throws Exception in case of any problem reading expected data, validation failure.
     */
    public static void assertOutput(String strKey, Object objActual, ITest iTest) throws Exception {
        Object objExpected;
        if (objActual instanceof Exception) {
            String strExpected = null;
            if (null != iTest.getTestCaseConfig().getExceptions()) {
                strExpected = (String) iTest.getTestCaseConfig().getExceptions().get(strKey);
            }
            assertException(strExpected, (Exception) objActual);
        } else {
            String assertionType = null;
            HashMap hmpExpectedData = iTest.getOutputData();
            objExpected = null;
            assertionType = ConfigReader.ASSERTION_EQUALITY;
            if (hmpExpectedData.containsKey(strKey + ":" + ConfigReader.ASSERTION_EQUALITY)) {
                objExpected = iTest.getOutputData(strKey + ":" + ConfigReader.ASSERTION_EQUALITY);
                assertionType = ConfigReader.ASSERTION_EQUALITY;
            } else if (hmpExpectedData.containsKey(strKey + ":" + ConfigReader.ASSERTION_INSPECTION)) {
                objExpected = iTest.getOutputData(strKey + ":" + ConfigReader.ASSERTION_INSPECTION);
                assertionType = ConfigReader.ASSERTION_INSPECTION;
            } else {
                throw new AssertionFailedError("No output configured for key " + strKey);
            }
            if (assertionType.equals(zing.config.ConfigReader.ASSERTION_INSPECTION)) {
                assertEquals((String) objExpected, objActual);
            } else if (objExpected instanceof Collection && objActual instanceof Collection) {
                assertEqualsIgnoreOrder((Collection) objExpected, (Collection) objActual);
            } else if (objExpected instanceof Object[] && objActual instanceof Object[]) {
                assertEquals((Object[]) objExpected, (Object[]) objActual);
            } else {
                Assert.assertEquals(objExpected, objActual);
            }
        }
    }
}
