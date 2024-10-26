package org.eclipse.datatools.connectivity.oda.pentaho.impl;

import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PentahoModel extends PentahoService {

    private String modelName;

    private String[] pentahoModelColumnNames = null;

    private String[] pentahoModelViewNames = null;

    private String[] pentahoModelColumnLabels = null;

    private String[] pentahoModelColumnTypes = null;

    private String[] pentahoResultSetColumnNamesMetaData = null;

    private String[] pentahoResultSetViewNamesMetaData = null;

    private String[] pentahoResultSetColumnLabelsMetaData = null;

    private String[] pentahoResultSetColumnTypesMetaData = null;

    private Integer nbBusinessColumns = 0;

    private Integer nbRowsPentahoResultSet = 0;

    public PentahoModel(String serverName, String userName, String password, String domain, String modelName) {
        this.serverName = serverName;
        this.userName = userName;
        this.password = password;
        this.modelName = modelName;
        this.domain = domain;
    }

    /**
	 * M�thode de g�n�ration des m�tadonn�es d'un Business Model Initialise les
	 * tableaux pentahoModelColumnNames, pentahoModelViewNames,
	 * pentahoModelColumnLabels, pentahoModelColumnTypes
	 */
    public void genereMetaData() {
        try {
            URL url = new URL("http://localhost:8080/pentaho/AdhocWebService?userid=" + userName + "&password=" + password + "&model=" + modelName + "&component=getbusinessmodel&domain=" + domain + "/metadata.xmi");
            URLConnection conn = url.openConnection();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(conn.getInputStream());
            doc.getDocumentElement().normalize();
            NodeList viewNodeLst = doc.getElementsByTagName("view");
            String view_id = null;
            int nbBusinessColumns = getNbBusinessColumns(doc);
            pentahoModelColumnNames = new String[nbBusinessColumns];
            pentahoModelViewNames = new String[nbBusinessColumns];
            pentahoModelColumnLabels = new String[nbBusinessColumns];
            pentahoModelColumnTypes = new String[nbBusinessColumns];
            int colCount = 0;
            for (int s = 0; s < viewNodeLst.getLength(); s++) {
                Node viewNode = viewNodeLst.item(s);
                if (viewNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element fstElmnt = (Element) viewNode;
                    NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("view_id");
                    Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
                    NodeList fstNm = fstNmElmnt.getChildNodes();
                    view_id = (String) ((Node) fstNm.item(0)).getNodeValue();
                }
                NodeList columnNodeList = viewNode.getChildNodes();
                for (int t = 0; t < columnNodeList.getLength(); t++) {
                    Node columnNode = columnNodeList.item(t);
                    if (columnNode.getNodeName().equals("column")) {
                        Element colAElmnt = (Element) columnNode;
                        NodeList colANmElmntLst = colAElmnt.getElementsByTagName("column_id");
                        Element colANmElmnt = (Element) colANmElmntLst.item(0);
                        NodeList colANm = colANmElmnt.getChildNodes();
                        pentahoModelColumnNames[colCount] = ((Node) colANm.item(0)).getNodeValue();
                        Element colBElmnt = (Element) columnNode;
                        NodeList colBNmElmntLst = colBElmnt.getElementsByTagName("column_name");
                        Element colBNmElmnt = (Element) colBNmElmntLst.item(0);
                        NodeList colBNm = colBNmElmnt.getChildNodes();
                        pentahoModelColumnLabels[colCount] = ((Node) colBNm.item(0)).getNodeValue();
                        Element colCElmnt = (Element) columnNode;
                        NodeList colCNmElmntLst = colCElmnt.getElementsByTagName("column_type");
                        Element colCNmElmnt = (Element) colCNmElmntLst.item(0);
                        NodeList colCNm = colCNmElmnt.getChildNodes();
                        pentahoModelColumnTypes[colCount] = ((Node) colCNm.item(0)).getNodeValue();
                        pentahoModelViewNames[colCount] = view_id;
                        colCount++;
                    }
                }
            }
            for (int i = 0; i < pentahoModelColumnNames.length; i++) {
                System.out.println(pentahoModelColumnNames[i] + "|" + pentahoModelColumnLabels[i] + "|" + pentahoModelColumnTypes[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * M�thode permettant de r�cup�rer un tableau avec les intitul�s des
	 * colonnes d'un Business Model
	 * 
	 * @return tableau des lib�ll�s de colonnes
	 */
    public String[] getColumnLabels() {
        return pentahoModelColumnLabels;
    }

    /**
	 * M�thode renvoyant les donn�es suite � une Query
	 * 
	 * @param targetUrl
	 *            url qui permet de r�cup�rer les datas
	 * @param nbColumns
	 *            nombre de colonnes du ResultSet
	 * @return tableau � 2 dimensions String[row][column]=value
	 */
    public String[][] getPentahoResultSet(String targetUrl, int nbColumns) {
        String[][] rowSet = null;
        try {
            URL url = new URL(targetUrl);
            URLConnection conn = url.openConnection();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(conn.getInputStream());
            doc.getDocumentElement().normalize();
            NodeList dataRowNodeList = doc.getElementsByTagName("DATA-ROW");
            nbRowsPentahoResultSet = dataRowNodeList.getLength();
            rowSet = new String[dataRowNodeList.getLength()][nbColumns];
            for (int rowNum = 0; rowNum < dataRowNodeList.getLength(); rowNum++) {
                Node dataRowNode = dataRowNodeList.item(rowNum);
                for (int colNum = 0; colNum < nbColumns; colNum++) {
                    Element fstElmnt = (Element) dataRowNode;
                    NodeList dateItemNodeList = fstElmnt.getElementsByTagName("DATA-ITEM");
                    Element fstNmElmnt = (Element) dateItemNodeList.item(colNum);
                    NodeList fstNm = fstNmElmnt.getChildNodes();
                    rowSet[rowNum][colNum] = (((Node) fstNm.item(0)).getNodeValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rowSet;
    }

    /**
	 * M�thode interne qui permet de retourner le nombre de colonnes m�tiers
	 * pour le Business Model
	 * 
	 * @param doc
	 * @return Integer Nombre de colonnes M�tiers
	 */
    private Integer getNbBusinessColumns(Document doc) {
        NodeList viewNodeLst = doc.getElementsByTagName("view");
        int colCount = 0;
        for (int s = 0; s < viewNodeLst.getLength(); s++) {
            Node viewNode = viewNodeLst.item(s);
            NodeList columnNodeList = viewNode.getChildNodes();
            for (int t = 0; t < columnNodeList.getLength(); t++) {
                Node columnNode = columnNodeList.item(t);
                if (columnNode.getNodeName().equals("column")) {
                    colCount++;
                }
            }
        }
        this.nbBusinessColumns = colCount;
        return colCount;
    }

    /**
	 * M�thode de g�n�ration des m�tadonn�es d'un ResultSet Initialise les
	 * tableaux pentahoResultSetColumnNamesMetaData,
	 * pentahoResultSetViewNamesMetaData, pentahoResultSetColumnLabelsMetaData,
	 * pentahoResultSetColumnTypesMetaData
	 * 
	 * @param String
	 *            [] selectedColumns tableau stockant les colonnes s�lectionn�es
	 */
    public void genereResultSetMetaData(String[] selectedColumns) {
        pentahoResultSetColumnNamesMetaData = new String[selectedColumns.length];
        pentahoResultSetViewNamesMetaData = new String[selectedColumns.length];
        pentahoResultSetColumnLabelsMetaData = new String[selectedColumns.length];
        pentahoResultSetColumnTypesMetaData = new String[selectedColumns.length];
        for (int i = 0; i < selectedColumns.length; i++) {
            pentahoResultSetColumnNamesMetaData[i] = pentahoModelColumnNames[new Integer(selectedColumns[i])];
            pentahoResultSetViewNamesMetaData[i] = pentahoModelViewNames[new Integer(selectedColumns[i])];
            pentahoResultSetColumnLabelsMetaData[i] = pentahoModelColumnLabels[new Integer(selectedColumns[i])];
            pentahoResultSetColumnTypesMetaData[i] = pentahoModelColumnTypes[new Integer(selectedColumns[i])];
        }
    }

    /**
	 * M�thode permettant de constuire le filtre pour la requ�te web
	 * @return String param�tre 'selections'
	 */
    public String getSelectionFilter() {
        String selectionFilter = "";
        for (int i = 0; i < pentahoResultSetColumnNamesMetaData.length; i++) {
            selectionFilter = selectionFilter + "<selection><view>" + this.getPentahoResultSetViewNamesMetaData()[i] + "</view><column>" + this.getPentahoResultSetColumnNamesMetaData()[i] + "</column></selection>";
        }
        return selectionFilter;
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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String[] getPentahoModelColumnNames() {
        return pentahoModelColumnNames;
    }

    public String[] getPentahoModelViewNames() {
        return pentahoModelViewNames;
    }

    public String[] getPentahoModelColumnLabels() {
        return pentahoModelColumnLabels;
    }

    public String[] getPentahoModelColumnTypes() {
        return pentahoModelColumnTypes;
    }

    public Integer getNbBusinessColumns() {
        return nbBusinessColumns;
    }

    public void setNbBusinessColumns(Integer nbBusinessColumns) {
        this.nbBusinessColumns = nbBusinessColumns;
    }

    public String[] getPentahoResultSetColumnNamesMetaData() {
        return pentahoResultSetColumnNamesMetaData;
    }

    public String[] getPentahoResultSetViewNamesMetaData() {
        return pentahoResultSetViewNamesMetaData;
    }

    public String[] getPentahoResultSetColumnLabelsMetaData() {
        return pentahoResultSetColumnLabelsMetaData;
    }

    public String[] getPentahoResultSetColumnTypesMetaData() {
        return pentahoResultSetColumnTypesMetaData;
    }

    public Integer getNbRowsPentahoResultSet() {
        return nbRowsPentahoResultSet;
    }
}
