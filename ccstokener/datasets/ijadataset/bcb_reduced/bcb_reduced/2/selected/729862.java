package org.eclipse.datatools.connectivity.oda.pentaho.impl;

import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PentahoService {

    protected String serverName;

    protected String userName;

    protected String password;

    protected String domain;

    private String[][] modelsList = null;

    public PentahoService() {
    }

    public PentahoService(String serverName, String userName, String password, String domain) {
        this.serverName = serverName;
        this.userName = userName;
        this.password = password;
        this.domain = domain;
    }

    /**
	 * M�thode qui renvoie la liste des Business Models
	 * 
	 * @return String[][] tableau � 2 dimensions permettant de stocker le nom et
	 *         le libell�
	 * @throws OdaException
	 */
    public String[][] getAllModels() throws OdaException {
        if (modelsList != null) {
            return modelsList;
        }
        try {
            URL url = new URL(serverName + "/AdhocWebService?userid=" + userName + "&password=" + password + "&domain=" + domain + "&component=listbusinessmodels");
            System.out.println("URL r�cup models: " + url);
            URLConnection conn = url.openConnection();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(conn.getInputStream());
            doc.getDocumentElement().normalize();
            NodeList nodeLst = doc.getElementsByTagName("model");
            modelsList = new String[nodeLst.getLength()][2];
            for (int s = 0; s < nodeLst.getLength(); s++) {
                Node fstNode = nodeLst.item(s);
                if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element Elmnt = (Element) fstNode;
                    NodeList fstNmElmntLst = Elmnt.getElementsByTagName("model_id");
                    Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
                    NodeList fstNm = fstNmElmnt.getChildNodes();
                    System.out.println("Model ID : " + ((Node) fstNm.item(0)).getNodeValue());
                    modelsList[s][0] = ((Node) fstNm.item(0)).getNodeValue();
                    NodeList lstNmElmntLst = Elmnt.getElementsByTagName("model_name");
                    Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
                    NodeList lstNm = lstNmElmnt.getChildNodes();
                    System.out.println("Model NAME : " + ((Node) lstNm.item(0)).getNodeValue());
                    modelsList[s][1] = ((Node) lstNm.item(0)).getNodeValue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new OdaException(e);
        }
        return modelsList;
    }

    /**
	 * M�thode qui instancie un Objet PentahoModel � partir de son nom
	 * @param modelName
	 * @return PentahoModel
	 */
    public PentahoModel getModel(String modelName) {
        return new PentahoModel(serverName, userName, password, domain, modelName);
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getUsername() {
        return userName;
    }

    public void setUsername(String username) {
        this.userName = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
