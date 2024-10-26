package com.bambamboo.st.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.bambamboo.st.IProcessor;
import com.bambamboo.st.util.HexString;

public class AutoAckProcessor extends AbstractProcessor implements IProcessor {

    private String ruleFile;

    private HashMap<Message, Message> ackRules;

    public byte[] getResponse(byte[] requestBuf) throws Exception {
        if (ackRules == null) {
            initAckRules();
        }
        for (Map.Entry<AutoAckProcessor.Message, AutoAckProcessor.Message> entry : ackRules.entrySet()) {
            AutoAckProcessor.Message requestMsg = entry.getKey();
            AutoAckProcessor.Message responseMsg = entry.getValue();
            if (requestMsg.isHex()) {
                byte[] byExpected = HexString.parseHexString(requestMsg.getData());
                if (Arrays.equals(requestBuf, byExpected)) {
                    if (responseMsg.isHex()) {
                        return HexString.parseHexString(responseMsg.getData());
                    } else {
                        return responseMsg.getData().getBytes(getCharset());
                    }
                }
            } else {
                byte[] byExpected = requestMsg.getData().getBytes(getCharset());
                if (Arrays.equals(requestBuf, byExpected)) {
                    if (responseMsg.isHex()) {
                        return HexString.parseHexString(responseMsg.getData());
                    } else {
                        return responseMsg.getData().getBytes(getCharset());
                    }
                }
            }
        }
        return null;
    }

    private void initAckRules() throws Exception {
        ackRules = new HashMap<Message, Message>();
        final File fRule = new File(ruleFile);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                File dtdFile = new File(fRule.getParentFile(), "ack.dtd");
                if (dtdFile.exists()) {
                    return new InputSource(new FileInputStream(dtdFile));
                }
                URL url = Thread.currentThread().getContextClassLoader().getResource("ack.dtd");
                if (url != null) {
                    return new InputSource(url.openStream());
                }
                return null;
            }
        });
        Document document = builder.parse(fRule);
        Element rootElement = document.getDocumentElement();
        NodeList ruleNodeList = rootElement.getChildNodes();
        for (int i = 0; i < ruleNodeList.getLength(); i++) {
            Node node = ruleNodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element ruleEle = (Element) node;
                NodeList reqList = ruleEle.getElementsByTagName("request");
                NodeList repList = ruleEle.getElementsByTagName("response");
                Element reqestEle = (Element) reqList.item(0);
                Element repsoneEle = (Element) repList.item(0);
                String reqType = reqestEle.getAttribute("type");
                String reqData = reqestEle.getTextContent().trim();
                String repType = repsoneEle.getAttribute("type");
                String repData = repsoneEle.getTextContent().trim();
                Message reqMsg = new Message(reqData, reqType);
                Message repMsg = new Message(repData, repType);
                addRule(reqMsg, repMsg);
            }
        }
    }

    protected void addRule(Message requestMsg, Message responeMsg) {
        ackRules.put(requestMsg, responeMsg);
    }

    /**
	 * @return the ruleFile
	 */
    public String getRuleFile() {
        return ruleFile;
    }

    /**
	 * @param ruleFile the ruleFile to set
	 */
    public void setRuleFile(String ruleFile) {
        this.ruleFile = ruleFile;
    }

    @Override
    public String toString() {
        return "AutoAckHandler with rules: " + getRuleFile();
    }

    static class Message {

        private String data;

        private String type;

        public Message() {
            super();
        }

        /**
		 * @param data
		 * @param type
		 */
        public Message(String data, String type) {
            super();
            this.data = data;
            this.type = type;
        }

        /**
		 * @return the data
		 */
        public String getData() {
            return data;
        }

        /**
		 * @param data the data to set
		 */
        public void setData(String data) {
            this.data = data;
        }

        /**
		 * @return the type
		 */
        public String getType() {
            return type;
        }

        /**
		 * @param type the type to set
		 */
        public void setType(String type) {
            this.type = type;
        }

        public boolean isHex() {
            return "hex".equalsIgnoreCase(getType());
        }
    }
}
