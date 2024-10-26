package org.mss.quartzjobs.jobs;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.mss.Activator;
import com.mss.commonsio.FileUtil;
import com.mss.date.DateUtil;
import com.mss.interfaces.IReflectionutil;
import com.mss.interfaces.IXMLUtil;
import com.mss.parsers.XPCOMUtil;
import com.mss.parsers.XMLUtil;
import com.mss.properties.StaticPropertiesMap;
import com.mss.reflection.ReflectionUtil;
import com.mss.wertpapier.templates.Fields;
import com.mss.wertpapier.templates.Template;

public class QuartzDownloadJob implements StatefulJob {

    private static IXMLUtil xmlutil = new XMLUtil();

    private static XPCOMUtil idomutil = new XPCOMUtil();

    private static DateUtil dateutil = new DateUtil();

    private static IReflectionutil reflection = new ReflectionUtil();

    private static JobReports jobreports = new JobReports();

    /**
 *  Execute Job based on Job Scheduler
 *  JobName: Name of Template
 *  GroupName: Name of Template
 *  
 *  JobReport: For each execution of the job, the following data is captured:
 *  
 *  1. JobName, Job Starting Time
 *  2. URL to Capture
 *  3. Expected Number of fields to capture : Actual Number of fields captured : Matching Quote
 *  4. If Errors occure -> store error
 */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        DateUtil dateutil = new DateUtil();
        JobReport jobreport = new JobReport();
        jobreport.starttime = dateutil.castUtilDatetoSQLDate(dateutil.getDateNow1());
        String templatename = context.getJobDetail().getName();
        System.out.println("---" + context.getJobDetail().getFullName() + " executing.[" + new Date() + "]");
        System.out.println(" Job Instance " + context.getJobInstance());
        System.out.println(" Job Fire Time " + context.getFireTime());
        System.out.println(" Job Next Fire Time " + context.getFireTime());
        Template myTemplate = Activator.istotemplate.getTemplate(templatename);
        myTemplate.displayTemplate();
        Activator.wp_lists.objectmap = new HashMap<Object, Object>();
        if (myTemplate.urlstaticmap.size() > 0) {
            downloadStaticUrls(myTemplate);
        }
        if (myTemplate.urldynamicmap.size() > 0) {
            downloadDynamicUrls(myTemplate);
        }
    }

    /**
	 * Static URLS
	 * URLS to capture are stored in the <urlstaticmap> tag in mss_template
	 * 
	 * 
	 * @param myTemplate
	 */
    public void downloadStaticUrls(Template myTemplate) {
        System.out.println(" Step 0: Enter downloadStaticUrls for Template: " + myTemplate.name);
        System.out.println(" Step 1: Number of URLS in Template           : " + myTemplate.urlstaticmap.size());
        int urlcounter = 0;
        Iterator iterator = myTemplate.urlstaticmap.entrySet().iterator();
        while (iterator.hasNext()) {
            urlcounter++;
            JobReport myJobReport = new JobReport();
            myJobReport.starttime = dateutil.getDateNow1();
            Map.Entry e = (Map.Entry) iterator.next();
            String url = e.getValue().toString();
            System.out.println("         Start Processing URL      : " + urlcounter);
            url = myTemplate.url + url;
            myJobReport.url = url;
            String strFileName = "";
            String downloaderror = "";
            try {
                strFileName = download(url);
                downloaderror = " Download of " + strFileName + " OK ";
            } catch (IOException e1) {
                downloaderror = e.toString();
                downloaderror = " Download of " + strFileName + " Error " + e1.toString();
                e1.printStackTrace();
            } finally {
                myJobReport.downloaderror = downloaderror;
                System.out.println(downloaderror);
            }
            Document xmlRootDocument = null;
            String parseerror = "";
            try {
                xmlRootDocument = xmlutil.parsetidy(strFileName);
                parseerror = " ParseTidy of " + strFileName + " OK ";
            } catch (Exception e1) {
                e1.printStackTrace();
                parseerror = " ParseTidy of " + strFileName + " Error " + e1.toString();
            } finally {
                myJobReport.parseerror = parseerror;
                System.out.println(parseerror);
            }
            System.out.println(" Table Name, Table Counter " + myTemplate.table_name + myTemplate.table_counter);
            short tabletype = Short.valueOf(myTemplate.table_type);
            String strtabletype = "";
            if (tabletype == 1) strtabletype = " HORIZONTAL "; else strtabletype = " VERTICAL ";
            System.out.println(" Table Type " + strtabletype);
            ArrayList extractionlist = xmlutil.initextractTable(xmlRootDocument, myTemplate, tabletype);
            for (int objectcounter = 0; objectcounter < extractionlist.size(); objectcounter++) {
                Map objectproperties = (Map) extractionlist.get(objectcounter);
                Object newobject = reflection.createObjectfromMap(objectproperties, null);
                if (newobject != null) Activator.wp_lists.addObject(newobject.getClass().toString(), newobject);
            }
            myJobReport.enddtime = dateutil.getDateNow1();
            myJobReport.duration = myJobReport.enddtime.getTime() - myJobReport.starttime.getTime();
            System.out.println(" Job Duration Time " + myJobReport.duration);
            jobreports.addReport(myJobReport);
        }
        try {
            Activator.wp_lists.systemoutListe();
            Activator.wp_lists.saveObjectstoDatabase("create");
            String datenow = dateutil.getDateNow();
            String strReportName = StaticPropertiesMap.XML_DIRECTORY + myTemplate.name + "_" + datenow + ".xml";
            jobreports.saveTemplatetoXML(strReportName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 * download dynamic urls, involving database access
	 * 
	 * Creates Dynamic URLs consisting of static basic url and database fields.
	 * 
	 *  For each entry, the key must be stored in field tablekey
	 * 
	 * 
	 */
    public void downloadDynamicUrls(Template myTemplate) {
        System.out.println(" Step 0: Enter downloadDynamicUrls for Template: " + myTemplate.name);
        System.out.println(" Step 1: Number of URLS in Template           : " + myTemplate.urldynamicmap.size());
        String[][] urlrowarray = createdynamicURLArray(myTemplate);
        String baseurl = "";
        System.out.println(" Step 2: Create Dynamic URLS                 : ");
        showdynamicURLArray(urlrowarray);
        int databasefields = 0;
        int databasefieldsvalue = 0;
        for (int row = 2; row < urlrowarray[0].length; row++) {
            JobReport myJobReport = new JobReport();
            myJobReport.starttime = dateutil.getDateNow1();
            String url = createOneURL(urlrowarray, row);
            String tablekey = getKeysfromUrl(urlrowarray, row);
            System.out.println(row + " Get URL                 : " + url);
            if (url != null) {
                url = url.substring(0, url.length() - 1);
                myJobReport.url = url;
                String strFileName = "";
                String downloaderror = "";
                try {
                    strFileName = download(url);
                    downloaderror = " Download of " + strFileName + " OK ";
                } catch (IOException e1) {
                    downloaderror = e1.toString();
                    downloaderror = " Download of " + strFileName + " Error " + e1.toString();
                    e1.printStackTrace();
                } finally {
                    myJobReport.downloaderror = downloaderror;
                    System.out.println(downloaderror);
                }
                Document xmlRootDocument = null;
                String parseerror = "";
                try {
                    xmlRootDocument = xmlutil.parsetidy(strFileName);
                    parseerror = " ParseTidy of " + strFileName + " OK ";
                } catch (Exception e1) {
                    e1.printStackTrace();
                    parseerror = " ParseTidy of " + strFileName + " Error " + e1.toString();
                } finally {
                    myJobReport.parseerror = parseerror;
                    System.out.println(parseerror);
                }
                short tabletype = Short.valueOf(myTemplate.table_type);
                String strtabletype = "";
                if (tabletype == 1) strtabletype = " HORIZONTAL "; else strtabletype = " VERTICAL ";
                ArrayList extractionlist = xmlutil.initextractTable(xmlRootDocument, myTemplate, tabletype);
                System.out.println(" Number of Parsed Objects : " + extractionlist.size());
                for (int objectcounter = 0; objectcounter < extractionlist.size(); objectcounter++) {
                    Map objectproperties = (Map) extractionlist.get(objectcounter);
                    Object newobject = reflection.createObjectfromMap(objectproperties, tablekey);
                    if (newobject != null) Activator.wp_lists.addObject(newobject.getClass().toString(), newobject);
                }
                myJobReport.enddtime = dateutil.getDateNow1();
                myJobReport.duration = myJobReport.enddtime.getTime() - myJobReport.starttime.getTime();
                System.out.println(" Job Duration Time " + myJobReport.duration);
                jobreports.addReport(myJobReport);
            }
        }
        try {
            Activator.wp_lists.systemoutListe();
            Activator.wp_lists.saveObjectstoDatabase("create");
            String datenow = dateutil.getDateNow();
            String strReportName = StaticPropertiesMap.XML_DIRECTORY + myTemplate.name + "_" + datenow + ".xml";
            jobreports.saveTemplatetoXML(strReportName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Check Download Files
	 * @param strURL
	 * @return
	 * @throws IOException
	 */
    public byte[] download1(String strURL) throws IOException {
        URL url = new URL(strURL);
        URLConnection connection = url.openConnection();
        DataInputStream in = new DataInputStream(connection.getInputStream());
        int size = connection.getContentLength();
        System.out.println(size);
        byte[] bStream = new byte[size];
        int bytes_read = 0;
        while (bytes_read < size) {
            bytes_read += in.read(bStream, bytes_read, size);
        }
        return bStream;
    }

    /**
	 * <key (ISIN, WKN oder z.B. ReutersK�rzel)>_<name>_ddmmyyyymmss_fileformat
	 * 
	 * @param strURL
	 * @param strSource
	 * @param strDirectory
	 * @throws IOException
	 */
    public String download(String strURL) throws IOException {
        System.out.println(" Download URL " + strURL);
        String strOutputFile = StaticPropertiesMap.DOWNLOAD_DIRECTORY + File.separator + "now";
        System.out.println(" Store File in " + strOutputFile);
        URL url = new URL(strURL);
        FileUtil fileUtil = new FileUtil();
        File outputfile = new File(strOutputFile);
        fileUtil.copyURLtoFile(url, outputfile);
        System.out.println(" Successfully stored into filename " + strOutputFile);
        return strOutputFile;
    }

    /**
	 * There are different kinds of parsers:
	 * 1. One exact value: just match the node hash of the Element
	 * 2. Tables: you have the element hash of the parent element (Table, TR, TD) -> Matching through attributes, through array (use 5th, 7th, 9th TD Element)
	 * 
	 * Table: Assumption: All Fields elements represent TD Elements
	 * 					   
	 */
    public String strTextValue = null;

    public void findTextValue(Node node, String texthash, Fields field) {
        int type = node.getNodeType();
        switch(type) {
            case Node.DOCUMENT_NODE:
                {
                    findTextValue(((Document) node).getDocumentElement(), texthash, field);
                    break;
                }
            case Node.ELEMENT_NODE:
                {
                    NamedNodeMap attrs = node.getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        Node attr = attrs.item(i);
                    }
                    NodeList children = node.getChildNodes();
                    if (children != null) {
                        int len = children.getLength();
                        for (int i = 0; i < len; i++) findTextValue(children.item(i), texthash, field);
                    }
                    break;
                }
            case Node.ENTITY_REFERENCE_NODE:
                {
                    break;
                }
            case Node.CDATA_SECTION_NODE:
                {
                    break;
                }
            case Node.TEXT_NODE:
                {
                    String mytexthash = String.valueOf(node.hashCode());
                    if (mytexthash.equalsIgnoreCase(texthash)) {
                        strTextValue = node.getNodeValue().trim();
                        System.out.println(" TextNode " + texthash + " matches value " + strTextValue);
                        return;
                    }
                    break;
                }
            case Node.PROCESSING_INSTRUCTION_NODE:
                {
                    String data = node.getNodeValue().trim();
                    {
                    }
                    break;
                }
        }
        if (type == Node.ELEMENT_NODE) {
        }
    }

    /**
	 * Creates one URL from urlarray
	 * 
	 * First Line of array contains static entries
	 * Line 2 - n contains databasevalues
	 * 
	 * If url cannot be created, return null value
	 * 
	 * @param urlrowarray
	 * @param row
	 * @return
	 */
    private String getKeysfromUrl(String[][] urlrowarray, int row) {
        String key = "";
        int databasefields = 0;
        int databasefieldsvalue = 0;
        for (int column = 2; column < urlrowarray.length; column++) {
            if (urlrowarray[column][0] != null) {
                databasefields++;
                String value = urlrowarray[column][row];
                if (value != null && row > 1) {
                    databasefieldsvalue++;
                    System.out.println(urlrowarray[column][row]);
                    key = key + ";" + urlrowarray[column][row];
                }
            }
        }
        return key;
    }

    /**
	 * Creates one URL from urlarray
	 * 
	 * First Line of array contains static entries
	 * Line 2 - n contains databasevalues
	 * 
	 * If url cannot be created, return null value
	 * 
	 * @param urlrowarray
	 * @param row
	 * @return
	 */
    public String createOneURL(String[][] urlrowarray, int row) {
        String url = "";
        String baseurl = urlrowarray[1][1];
        int databasefields = 0;
        int databasefieldsvalue = 0;
        for (int column = 2; column < urlrowarray.length; column++) {
            if (urlrowarray[column][0] != null) {
                databasefields++;
                String value = urlrowarray[column][row];
                if (value != null && row > 1) {
                    databasefieldsvalue++;
                    System.out.println(urlrowarray[column][row]);
                    url = url + urlrowarray[column][row];
                }
            } else {
                url = url + urlrowarray[column][1];
            }
        }
        if (databasefields == databasefieldsvalue && databasefields > 0) {
            url = baseurl + url;
        } else {
            url = null;
        }
        return url;
    }

    /**
	 * Show URLArray
	 * @param urlrowarray
	 */
    public void showdynamicURLArray(String[][] urlrowarray) {
        System.out.println(" URL Array: rows " + urlrowarray[0].length + " columns " + urlrowarray.length);
        for (int row = 0; row < urlrowarray[0].length; row++) {
            String rowstr = "";
            for (int column = 0; column < urlrowarray.length; column++) {
                rowstr = rowstr + urlrowarray[column][row];
            }
            System.out.println(row + " " + rowstr);
        }
        System.out.println(" End showdynamicurl ");
    }

    /**
	 * Creates URL Array from the static components provided in urldynamicmap and the dynamic parts provided 
	 * in the database
	 * 
	 * Max length of Array is 100 -> Correction: Here really big numbers can occur!!
	 * @return
	 */
    public String[][] createdynamicURLArray(Template myTemplate) {
        Iterator iterator = myTemplate.urldynamicmap.entrySet().iterator();
        String param = "";
        String fields = "";
        String databasefield_param = "";
        String static_param = "";
        int urlfieldscount = (myTemplate.urldynamicmap.size()) * 2;
        String[][] urlrowarray = new String[urlfieldscount][StaticPropertiesMap.MAX_DYNAMICURLS];
        String separator = "";
        int dbrows = 0;
        for (int urlcount = 0; urlcount < myTemplate.urldynamicmap.size(); urlcount++) {
            String key = String.valueOf(urlcount);
            fields = myTemplate.urldynamicmap.get(key).toString();
            if (urlcount == 0) {
                String fieldarray[] = fields.split(";");
                try {
                    param = fieldarray[0] + "=";
                } catch (Exception e) {
                    param = "";
                }
                try {
                    static_param = fieldarray[1];
                } catch (Exception e) {
                    static_param = "";
                }
                urlrowarray[urlcount * 2][1] = param;
                urlrowarray[(urlcount * 2) + 1][1] = static_param;
            } else {
                String fieldarray[] = fields.split(";");
                try {
                    param = fieldarray[0];
                } catch (Exception e) {
                    param = "";
                }
                try {
                    static_param = fieldarray[1];
                } catch (Exception e) {
                    static_param = "";
                }
                try {
                    databasefield_param = fieldarray[2];
                } catch (Exception e) {
                    databasefield_param = "";
                }
                urlrowarray[urlcount * 2][1] = param;
                urlrowarray[(urlcount * 2) + 1][1] = static_param + separator;
                if (databasefield_param.length() > 0) {
                    urlrowarray[(urlcount * 2) + 1][0] = databasefield_param;
                    String urlerror = "";
                    ResultSet resultset = null;
                    try {
                        resultset = Activator.dbutils.executeStatement(databasefield_param);
                        resultset.last();
                        dbrows = resultset.getRow();
                        resultset.first();
                        urlerror = " Construction of found " + dbrows + " rows, OK ";
                        while (resultset.next()) {
                            int row = resultset.getRow();
                            String fieldvalue = resultset.getString(1);
                            urlrowarray[(urlcount * 2) + 1][row + 2] = fieldvalue;
                        }
                    } catch (SQLException sqle) {
                        urlerror = sqle.toString();
                        urlerror = " Construction of url Error " + sqle.toString();
                        sqle.printStackTrace();
                    } finally {
                        try {
                            resultset.close();
                            Activator.dbutils.closeConnection();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return urlrowarray;
    }

    /**
	 * 
	 * @param template
	 */
    public void store(Template template, Document document) {
    }

    public void report() {
    }

    public static void main(String[] args) {
        try {
            QuartzDownloadJob myDownloadJob = new QuartzDownloadJob();
            JobExecutionContext context;
            myDownloadJob.execute(null);
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
}
