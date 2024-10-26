package uc;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import reports.utility.datamodel.technicalprocessing.CATALOGUERECORD;
import reports.utility.datamodel.technicalprocessing.CATALOGUERECORD_MANAGER;
import tools.HibernateUtil;
import tools.ServletConnector;

/**
 *
 * @author Administrator
 */
public class PersistToUnionCatalogue {

    String ucServerURL = "";

    private int totalCountStatus = 0;

    private int currentRecordStatus = 0;

    private String logFilePath = reports.utility.NewGenLibDesktopRoot.getRoot();

    PrintWriter pw = null;

    private int statusA = 0;

    private int statusB = 0;

    private int statusC = 0;

    private int statusD = 0;

    private int statusE = 0;

    private int statusF = 0;

    private boolean selectiveUploadStatus = false;

    private java.awt.Component comp = null;

    private boolean process = true;

    private int initialUploadStatus = 0;

    /** Creates a new instance of PersistToUnionCatalogue */
    public PersistToUnionCatalogue() {
    }

    public void persistRecords(String alreadyExecutedId, String thisLibraryUCID, String ucServerURL, String localhostServer, java.awt.Component comp, int totalCount) {
        setInitialUploadStatus(0);
        this.comp = comp;
        this.ucServerURL = ucServerURL;
        CATALOGUERECORD_MANAGER catmanager = new CATALOGUERECORD_MANAGER();
        try {
            String query = "select cataloguerecordid, bibiliographic_level_id, material_type_id from cataloguerecord where cataloguerecordid>" + alreadyExecutedId;
            java.sql.Connection con = reports.utility.database.PostgresConnectionPool.getInstance().getConnection();
            java.sql.Statement stmt = con.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery(query);
            while (rs.next() && isProcess() == true) {
                System.out.println("inside while loop");
                setTotalCountStatus(getTotalCountStatus() + 1);
                setCurrentRecordStatus(0);
                String entryId = "1";
                String entryLibId = thisLibraryUCID;
                String ownerLibId = thisLibraryUCID;
                String libraryId = "1";
                String holdingLibId = thisLibraryUCID;
                String biblevel = rs.getString(2);
                String mattypeid = rs.getString(3);
                String catrecid = rs.getString(1);
                setCurrentRecordStatus(getCurrentRecordStatus() + 1);
                String urlStr = "http://" + localhostServer + ":8080/newgenlibctxt/oai2.0?verb=GetRecord&metadataPrefix=marc21&identifier=CAT";
                urlStr += "_" + catrecid + "_1";
                URL url = new URL(urlStr);
                URLConnection urlcon = url.openConnection();
                try {
                    InputStream is = urlcon.getInputStream();
                    SAXBuilder sb = new SAXBuilder();
                    Document doc = sb.build(is);
                    setCurrentRecordStatus(getCurrentRecordStatus() + 1);
                    Hashtable hta = new Hashtable();
                    hta.put("USE_CAT_ID", catrecid);
                    hta.put("entryId", entryId);
                    hta.put("entryLibId", entryLibId);
                    hta.put("ownerLibId", ownerLibId);
                    hta.put("libraryId", libraryId);
                    hta.put("holdingLibId", holdingLibId);
                    hta.put("bibliographiclevel", biblevel);
                    hta.put("materialType", mattypeid);
                    setCurrentRecordStatus(getCurrentRecordStatus() + 1);
                    sendRecordToUCSever(hta, doc);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            comp.setEnabled(true);
            setInitialUploadStatus(1);
        } catch (Exception expu) {
            expu.printStackTrace();
            comp.setEnabled(true);
            setInitialUploadStatus(2);
        }
    }

    public void sendRecordToUCSever(Hashtable ht, Document docOfMarc) {
        try {
            Element eleroot = new Element("OperationId");
            eleroot.setAttribute("no", "11");
            Document doctar = new Document(eleroot);
            Enumeration enumvals = ht.keys();
            while (enumvals.hasMoreElements()) {
                String key = enumvals.nextElement().toString();
                String value = ht.get(key).toString();
                Element elex = new Element(key);
                elex.setText(value);
                eleroot.addContent(elex);
            }
            Namespace dns = docOfMarc.getRootElement().getNamespace();
            Namespace oaimarc = Namespace.getNamespace("http://www.loc.gov/MARC21/slim");
            try {
                Element elepart = (Element) docOfMarc.getRootElement().getChild("GetRecord", dns).getChild("record", dns).getChild("metadata", dns).getChild("record", oaimarc).clone();
                eleroot.addContent(elepart);
            } catch (Exception e) {
                Element elepart = (Element) docOfMarc.getRootElement().getChild("GetRecord", dns).getChild("Record", dns).getChild("metadata", dns).getChild("record", oaimarc).clone();
                eleroot.addContent(elepart);
            }
            String tarxml = (new XMLOutputter()).outputString(doctar);
            System.out.println("tar xml = " + tarxml);
            ServletConnector sc = new ServletConnector(ucServerURL);
            String resxml = sc.sendRequest("PrimaryCataloguingServlet", tarxml);
            System.out.println("res xml = " + resxml);
            setCurrentRecordStatus(getCurrentRecordStatus() + 1);
            if (pw != null) {
                SAXBuilder sb = new SAXBuilder();
                Document retdoc = null;
                try {
                    retdoc = sb.build(new StringReader(resxml));
                } catch (Exception expx) {
                    expx.printStackTrace();
                }
                if (retdoc != null) {
                    String status = retdoc.getRootElement().getChildTextTrim("status");
                    String addedStatus = retdoc.getRootElement().getChildTextTrim("addedstatus");
                    System.out.println("the addedStatus = " + addedStatus);
                    if (addedStatus == null || addedStatus.equals("E")) {
                        statusE++;
                    } else if (addedStatus.equals("A")) {
                        statusA++;
                    } else if (addedStatus.equals("B")) {
                        statusB++;
                    } else if (addedStatus.equals("C")) {
                        statusC++;
                    } else if (addedStatus.equals("D")) {
                        statusD++;
                    } else if (addedStatus.equals("F")) {
                        statusF++;
                    }
                    if (status == null || status.equals("N")) {
                        pw.println(ht.get("USE_CAT_ID").toString());
                    }
                } else {
                    pw.println(ht.get("USE_CAT_ID").toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void persistRecordsSelectiveMode(String thisLibraryUCID, String ucServerURL, String from, String to, String localhostServer, java.awt.Component comp) {
        this.comp = comp;
        setSelectiveUploadStatus(false);
        this.ucServerURL = ucServerURL;
        org.hibernate.Session session = HibernateUtil.getSessionFactory().openSession();
        CATALOGUERECORD_MANAGER catmanager = new CATALOGUERECORD_MANAGER();
        List list = catmanager.getCatalogueRecordsBetweenDates(session, from, to);
        for (int i = 0; i < list.size(); i++) {
            if (isProcess() == true) {
                setTotalCountStatus(getTotalCountStatus() + 1);
                setCurrentRecordStatus(0);
                CATALOGUERECORD catrec = (CATALOGUERECORD) list.get(i);
                String entryId = "1";
                String entryLibId = thisLibraryUCID;
                String ownerLibId = thisLibraryUCID;
                String libraryId = "1";
                String holdingLibId = thisLibraryUCID;
                String biblevel = catrec.getBibiliographic_level_id().toString();
                String mattypeid = catrec.getMaterial_type_id().toString();
                String catrecid = catrec.getPrimaryKey().getCataloguerecordid().toString();
                setCurrentRecordStatus(getCurrentRecordStatus() + 1);
                String urlStr = "http://" + localhostServer + ":8080/newgenlibctxt/oai2.0?verb=GetRecord&metadataPrefix=marc21&identifier=CAT";
                urlStr += "_" + catrecid + "_1";
                try {
                    URL url = new URL(urlStr);
                    URLConnection urlcon = url.openConnection();
                    InputStream is = urlcon.getInputStream();
                    SAXBuilder sb = new SAXBuilder();
                    Document doc = sb.build(is);
                    setCurrentRecordStatus(getCurrentRecordStatus() + 1);
                    Hashtable hta = new Hashtable();
                    hta.put("USE_CAT_ID", catrecid);
                    hta.put("entryId", entryId);
                    hta.put("entryLibId", entryLibId);
                    hta.put("ownerLibId", ownerLibId);
                    hta.put("libraryId", libraryId);
                    hta.put("holdingLibId", holdingLibId);
                    hta.put("bibliographiclevel", biblevel);
                    hta.put("materialType", mattypeid);
                    setCurrentRecordStatus(getCurrentRecordStatus() + 1);
                    sendRecordToUCSever(hta, doc);
                } catch (Exception expu) {
                    expu.printStackTrace();
                }
            }
        }
        session.close();
        setSelectiveUploadStatus(true);
        comp.setEnabled(true);
    }

    public int getTotalCountStatus() {
        return totalCountStatus;
    }

    public void setTotalCountStatus(int totalCountStatus) {
        this.totalCountStatus = totalCountStatus;
    }

    public int getCurrentRecordStatus() {
        return currentRecordStatus;
    }

    public void setCurrentRecordStatus(int currentRecordStatus) {
        this.currentRecordStatus = currentRecordStatus;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        try {
            pw = new PrintWriter(new FileWriter(logFilePath + "/log_conv.txt"), true);
        } catch (Exception exp) {
        }
        this.logFilePath = logFilePath;
    }

    public int getStatusA() {
        return statusA;
    }

    public int getStatusB() {
        return statusB;
    }

    public int getStatusC() {
        return statusC;
    }

    public int getStatusD() {
        return statusD;
    }

    public int getStatusE() {
        return statusE;
    }

    public int getStatusF() {
        return statusF;
    }

    public boolean isSelectiveUploadStatus() {
        return selectiveUploadStatus;
    }

    public void setSelectiveUploadStatus(boolean selectiveUploadStatus) {
        this.selectiveUploadStatus = selectiveUploadStatus;
    }

    public boolean isProcess() {
        return process;
    }

    public void setProcess(boolean process) {
        this.process = process;
    }

    public int getInitialUploadStatus() {
        return initialUploadStatus;
    }

    public void setInitialUploadStatus(int initialUploadStatus) {
        this.initialUploadStatus = initialUploadStatus;
    }
}
