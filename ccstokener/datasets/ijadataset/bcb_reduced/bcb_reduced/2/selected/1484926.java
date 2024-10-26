package newgen.presentation.administration;

/**
 *
 * @author root
 */
public class DataEntryTest {

    String serverURL = "192.168.0.6";

    /** Creates a new instance of DataEntryTest */
    public DataEntryTest() {
        try {
            for (int i = 0; i < 1000; i++) {
                sendRequest("PrimaryCataloguingServlet", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><OperationId no=\"11\"><entryId>1</entryId><entryLibId>1</entryLibId><ownerLibId>1</ownerLibId><libraryId>1</libraryId><holdingLibId>1</holdingLibId><bibliographiclevel>3</bibliographiclevel><materialType>1</materialType><marc:record xmlns:marc=\"http://www.loc.gov/MARC21/slim\"><marc:leader>00427nam#a0000145ua#1243</marc:leader><marc:controlfield tag=\"001\">123543</marc:controlfield><marc:controlfield tag=\"008\" /><marc:datafield tag=\"901\" ind1=\"0\" ind2=\"0\"><marc:subfield code=\"a\">120" + i + "</marc:subfield><marc:subfield code=\"b\">BANARAS LIBRARY</marc:subfield><marc:subfield code=\"c\">120" + i + "</marc:subfield><marc:subfield code=\"d\">stacks</marc:subfield><marc:subfield code=\"k\">B</marc:subfield><marc:subfield code=\"m\">1</marc:subfield><marc:subfield code=\"l\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;&lt;Root /&gt;</marc:subfield></marc:datafield><marc:datafield tag=\"904\" ind1=\"0\" ind2=\"0\"><marc:subfield code=\"a\">UC</marc:subfield></marc:datafield><marc:datafield tag=\"905\" ind1=\"0\" ind2=\"0\"><marc:subfield code=\"a\">1/1/2001 05:09:01</marc:subfield></marc:datafield><marc:datafield tag=\"906\" ind1=\"0\" ind2=\"0\"><marc:subfield code=\"a\">eng</marc:subfield></marc:datafield><marc:datafield tag=\"245\" ind1=\"0\" ind2=\"0\"><marc:subfield code=\"a\">Tiltle 120" + i + " </marc:subfield></marc:datafield></marc:record></OperationId>");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String sendRequest(java.lang.String servletName, java.lang.String request) {
        String reqxml = "";
        org.jdom.Document retdoc = null;
        String myurl = java.util.prefs.Preferences.systemRoot().get("serverurl", "");
        String myport = java.util.prefs.Preferences.systemRoot().get("portno", "8080");
        if (this.serverURL == null) {
            try {
                java.net.URL codebase = newgen.presentation.NewGenMain.getAppletInstance().getCodeBase();
                if (codebase != null) serverURL = codebase.getHost(); else serverURL = "localhost";
            } catch (Exception exp) {
                exp.printStackTrace();
                serverURL = "localhost";
            }
            newgen.presentation.component.IPAddressPortNoDialog ipdig = new newgen.presentation.component.IPAddressPortNoDialog(myurl, myport);
            ipdig.show();
            serverURL = myurl = ipdig.getIPAddress();
            myport = ipdig.getPortNo();
            java.util.prefs.Preferences.systemRoot().put("serverurl", serverURL);
            java.util.prefs.Preferences.systemRoot().put("portno", myport);
            System.out.println(serverURL);
        }
        try {
            System.out.println("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URL url = new java.net.URL("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URLConnection urlconn = (java.net.URLConnection) url.openConnection();
            urlconn.setDoOutput(true);
            urlconn.setRequestProperty("Content-type", "text/xml; charset=UTF-8");
            java.io.OutputStream os = urlconn.getOutputStream();
            String req1xml = request;
            java.util.zip.CheckedOutputStream cos = new java.util.zip.CheckedOutputStream(os, new java.util.zip.Adler32());
            java.util.zip.GZIPOutputStream gop = new java.util.zip.GZIPOutputStream(cos);
            java.io.OutputStreamWriter dos = new java.io.OutputStreamWriter(gop, "UTF-8");
            System.out.println(req1xml);
            dos.write(req1xml);
            dos.flush();
            dos.close();
            System.out.println("url conn: " + urlconn.getContentEncoding() + "  " + urlconn.getContentType());
            java.io.InputStream ios = urlconn.getInputStream();
            java.util.zip.CheckedInputStream cis = new java.util.zip.CheckedInputStream(ios, new java.util.zip.Adler32());
            java.util.zip.GZIPInputStream gip = new java.util.zip.GZIPInputStream(cis);
            java.io.InputStreamReader br = new java.io.InputStreamReader(gip, "UTF-8");
            retdoc = (new org.jdom.input.SAXBuilder()).build(br);
        } catch (java.net.ConnectException conexp) {
            javax.swing.JOptionPane.showMessageDialog(null, newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("ConnectExceptionMessage"), "Critical error", javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (Exception exp) {
            exp.printStackTrace(System.out);
        }
        System.out.println(reqxml);
        return "";
    }

    public static void main(String[] args) {
        new DataEntryTest();
    }
}
