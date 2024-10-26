package newgen.presentation.administration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;
import newgen.presentation.NewGenMain;
import newgen.presentation.component.NGLResourceBundle;
import newgen.presentation.component.Utility;
import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.html.gui.SelectionChangeEvent;
import org.lobobrowser.html.gui.SelectionChangeListener;
import org.lobobrowser.html.test.SimpleHtmlRendererContext;
import org.lobobrowser.html.test.SimpleUserAgentContext;
import org.w3c.dom.html2.HTMLElement;

/**
 *
 * @author siddartha
 */
public class OpenerIF extends javax.swing.JInternalFrame {

    final HtmlPanel panel = new HtmlPanel();

    private String libName;

    private String loginId;

    private String loginName;

    private String loginTime;

    private String serverTimeDiff;

    private DefaultTableModel pendingJobs;

    private Vector frequentlyUsedScreens;

    private String frequentlyUsedScreensHTMLCode;

    private Object[] openCatalog;

    private String pendingJobsHTMLCode;

    private String openCatalogHTMLCode;

    private String onlineInformationHTML = "Unable to load data. Check Internet connectivity";

    private String newsHTML = "Unable to load data. Check Internet connectivity";

    private String verusSubscriptionId = "";

    private Thread th = null;

    private String verusSubscriptionIdHTML = "";

    private String openCatalogApplicationLaunchLink = "";

    /** Creates new form OpenerIF */
    public OpenerIF(String libName, String loginId, String loginName, String loginTime, String serverTimeDiff, DefaultTableModel pendingJobs, Vector frequentlyUsedScreens, Object[] openCatalog) {
        th = new Thread(new NGLDashBoardFileDownloadThread(this));
        th.start();
        Logger.getLogger("org.lobobrowser").setLevel(Level.OFF);
        this.libName = libName;
        this.loginId = loginId;
        this.loginName = loginName;
        this.serverTimeDiff = serverTimeDiff;
        this.pendingJobs = pendingJobs;
        this.frequentlyUsedScreens = frequentlyUsedScreens;
        this.openCatalog = openCatalog;
        getContentPane().add(panel);
        System.out.println("------------->" + OpenerIF.class.getResource("Home.html").toString());
        fillData();
        setVisible(true);
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setTitle(NGLResourceBundle.getInstance().getString("Home"));
    }

    public void clickAction(String vals) {
        if (vals == null) {
            return;
        }
        if (vals.equals("morePendingTasks")) {
            NewGenMain.getAppletInstance().showPendingTasksIF();
        } else if (vals.equals("uploadToOpenCatalog")) {
            Utility.getInstance().showBrowser(openCatalogApplicationLaunchLink);
        } else if (vals.equals("customerSupport")) {
            NewGenMain.getAppletInstance().showChatIF();
        } else if (vals.startsWith("class")) {
            String[] spliited = vals.split("\\|");
            NewGenMain.getAppletInstance().showScreenByName(spliited[1]);
        }
    }

    public OpenerIF() {
        th = new Thread(new NGLDashBoardFileDownloadThread(this));
        th.start();
        Logger.getLogger("org.lobobrowser").setLevel(Level.OFF);
        panel.addSelectionChangeListener(new SelectionChangeListener() {

            @Override
            public void selectionChanged(SelectionChangeEvent sce) {
                String linkSelected = panel.getSelectionNode().getNodeValue();
                clickAction(linkSelected);
            }
        });
        getContentPane().add(panel);
        setVisible(true);
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setTitle(NGLResourceBundle.getInstance().getString("Home"));
    }

    public void fillData() {
        try {
            URL urlhome = OpenerIF.class.getResource("Home.html");
            URLConnection uc = urlhome.openConnection();
            InputStreamReader input = new InputStreamReader(uc.getInputStream());
            BufferedReader in = new BufferedReader(input);
            String inputLine;
            String htmlData = "";
            while ((inputLine = in.readLine()) != null) {
                htmlData += inputLine;
            }
            in.close();
            String[] str = new String[9];
            str[0] = getLibName();
            str[1] = getLoginId();
            str[2] = getLoginName();
            str[3] = getVerusSubscriptionIdHTML();
            str[4] = getPendingJobsHTMLCode();
            str[5] = getFrequentlyUsedScreensHTMLCode();
            str[6] = getOpenCatalogHTMLCode();
            str[7] = getNewsHTML();
            str[8] = getOnlineInformationHTML();
            MessageFormat mf = new MessageFormat(htmlData);
            String htmlContent = mf.format(htmlData, str);
            PrintWriter fw = new PrintWriter(System.getProperty("user.home") + "/homeNGL.html");
            fw.println(htmlContent);
            fw.flush();
            fw.close();
            new LocalHtmlRendererContext(panel, new SimpleUserAgentContext(), this).navigate("file:" + System.getProperty("user.home") + "/homeNGL.html");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("Administration");
        setTitle(bundle.getString("Home"));
        addInternalFrameListener(new javax.swing.event.InternalFrameListener() {

            public void internalFrameActivated(javax.swing.event.InternalFrameEvent evt) {
                formInternalFrameActivated(evt);
            }

            public void internalFrameClosed(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameIconified(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameOpened(javax.swing.event.InternalFrameEvent evt) {
            }
        });
        pack();
    }

    private void formInternalFrameActivated(javax.swing.event.InternalFrameEvent evt) {
        fillData();
    }

    /**
     * @return the libName
     */
    public String getLibName() {
        return libName;
    }

    /**
     * @param libName the libName to set
     */
    public void setLibName(String libName) {
        this.libName = libName;
    }

    /**
     * @return the loginId
     */
    public String getLoginId() {
        return loginId;
    }

    /**
     * @param loginId the loginId to set
     */
    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    /**
     * @return the loginName
     */
    public String getLoginName() {
        return loginName;
    }

    /**
     * @param loginName the loginName to set
     */
    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    /**
     * @return the loginTime
     */
    public String getLoginTime() {
        return loginTime;
    }

    /**
     * @param loginTime the loginTime to set
     */
    public void setLoginTime(String loginTime) {
        this.loginTime = loginTime;
    }

    /**
     * @return the serverTimeDiff
     */
    public String getServerTimeDiff() {
        return serverTimeDiff;
    }

    /**
     * @param serverTimeDiff the serverTimeDiff to set
     */
    public void setServerTimeDiff(String serverTimeDiff) {
        this.serverTimeDiff = serverTimeDiff;
    }

    /**
     * @return the pendingJobs
     */
    public DefaultTableModel getPendingJobs() {
        return pendingJobs;
    }

    /**
     * @param pendingJobs the pendingJobs to set
     */
    public void setPendingJobs(DefaultTableModel pendingJobs) {
        String htmlcode = "<ul>";
        Vector rows = pendingJobs.getDataVector();
        for (int i = 0; i < rows.size(); i++) {
            Vector cols = (Vector) rows.elementAt(i);
            htmlcode += "<li>" + cols.elementAt(0) + "(" + cols.elementAt(1) + ")" + "</li>";
        }
        htmlcode += "</ul>";
        setPendingJobsHTMLCode(htmlcode);
        this.pendingJobs = pendingJobs;
    }

    /**
     * @return the frequentlyUsedScreens
     */
    public Vector getFrequentlyUsedScreens() {
        return frequentlyUsedScreens;
    }

    /**
     * @param frequentlyUsedScreens the frequentlyUsedScreens to set
     */
    public void setFrequentlyUsedScreens(Vector frequentlyUsedScreens) {
        System.out.println("frequentlyUsedScreens: " + frequentlyUsedScreens.size());
        String htmlcode = "<ul>";
        int count = 0;
        if (frequentlyUsedScreens.size() > 5) {
            count = 5;
        } else {
            count = frequentlyUsedScreens.size();
        }
        for (int i = 0; i < count; i++) {
            String[] vals = (String[]) frequentlyUsedScreens.elementAt(i);
            htmlcode += "<li><a href='class|" + vals[0] + "'>" + vals[1] + "</a></li>";
            System.out.println("<li><a href='" + vals[0] + "'>" + vals[1] + "</a></li>");
        }
        htmlcode += "</ul>";
        setFrequentlyUsedScreensHTMLCode(htmlcode);
        this.frequentlyUsedScreens = frequentlyUsedScreens;
    }

    /**
     * @return the openCatalog
     */
    public Object[] getOpenCatalog() {
        return openCatalog;
    }

    /**
     * @param openCatalog the openCatalog to set
     */
    public void setOpenCatalog(Object[] openCatalog) {
        if (((Boolean) openCatalog[0]).booleanValue()) {
            setOpenCatalogHTMLCode("Your OpenCatalog database is more than 7 days old.<br> <a href='uploadToOpenCatalog'>Click here to upload your catalog to OpenCatalog</a> ");
            openCatalogApplicationLaunchLink = openCatalog[1].toString();
        } else {
            setOpenCatalogHTMLCode("Your OpenCatalog database is up-to-date");
        }
        this.openCatalog = openCatalog;
    }

    /**
     * @return the pendingJobsHTMLCode
     */
    public String getPendingJobsHTMLCode() {
        return pendingJobsHTMLCode;
    }

    /**
     * @param pendingJobsHTMLCode the pendingJobsHTMLCode to set
     */
    public void setPendingJobsHTMLCode(String pendingJobsHTMLCode) {
        this.pendingJobsHTMLCode = pendingJobsHTMLCode;
    }

    /**
     * @return the openCatalogHTMLCode
     */
    public String getOpenCatalogHTMLCode() {
        return openCatalogHTMLCode;
    }

    /**
     * @param openCatalogHTMLCode the openCatalogHTMLCode to set
     */
    public void setOpenCatalogHTMLCode(String openCatalogHTMLCode) {
        this.openCatalogHTMLCode = openCatalogHTMLCode;
    }

    /**
     * @return the frequentlyUsedScreensHTMLCode
     */
    public String getFrequentlyUsedScreensHTMLCode() {
        return frequentlyUsedScreensHTMLCode;
    }

    /**
     * @param frequentlyUsedScreensHTMLCode the frequentlyUsedScreensHTMLCode to set
     */
    public void setFrequentlyUsedScreensHTMLCode(String frequentlyUsedScreensHTMLCode) {
        this.frequentlyUsedScreensHTMLCode = frequentlyUsedScreensHTMLCode;
    }

    /**
     * @return the onlineInformationHTML
     */
    public String getOnlineInformationHTML() {
        return onlineInformationHTML;
    }

    /**
     * @param onlineInformationHTML the onlineInformationHTML to set
     */
    public void setOnlineInformationHTML(String onlineInformationHTML) {
        this.onlineInformationHTML = onlineInformationHTML;
    }

    /**
     * @return the newsHTML
     */
    public String getNewsHTML() {
        return newsHTML;
    }

    /**
     * @param newsHTML the newsHTML to set
     */
    public void setNewsHTML(String newsHTML) {
        this.newsHTML = newsHTML;
    }

    /**
     * @return the verusSubscriptionId
     */
    public String getVerusSubscriptionId() {
        return verusSubscriptionId;
    }

    /**
     * @param verusSubscriptionId the verusSubscriptionId to set
     */
    public void setVerusSubscriptionId(String verusSubscriptionIdX) {
        if (verusSubscriptionIdX.equals("")) {
            setVerusSubscriptionIdHTML("Please contact Verus Solutions to get your permanent subscription ID");
        } else {
            setVerusSubscriptionIdHTML(verusSubscriptionIdX);
        }
        this.verusSubscriptionId = verusSubscriptionIdX;
    }

    /**
     * @return the verusSubscriptionIdHTML
     */
    public String getVerusSubscriptionIdHTML() {
        return verusSubscriptionIdHTML;
    }

    /**
     * @param verusSubscriptionIdHTML the verusSubscriptionIdHTML to set
     */
    public void setVerusSubscriptionIdHTML(String verusSubscriptionIdHTML) {
        this.verusSubscriptionIdHTML = verusSubscriptionIdHTML;
    }
}

class NGLDashBoardFileDownloadThread implements Runnable {

    private OpenerIF host = null;

    public NGLDashBoardFileDownloadThread(OpenerIF host) {
        this.host = host;
    }

    @Override
    public void run() {
        try {
            URL urlhome = new URL("http://www.verussolutions.biz/NGLDashBoard.xml");
            URLConnection uc = urlhome.openConnection();
            InputStreamReader input = new InputStreamReader(uc.getInputStream());
            BufferedReader in = new BufferedReader(input);
            String inputLine;
            String xmlData = "";
            while ((inputLine = in.readLine()) != null) {
                xmlData += inputLine;
            }
            in.close();
            PrintWriter fw = new PrintWriter(new FileWriter(System.getProperty("user.home") + "/NGLDashBoard.xml"));
            fw.println(xmlData);
            fw.flush();
            fw.close();
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        try {
            Document doc = new SAXBuilder().build(new File(System.getProperty("user.home") + "/NGLDashBoard.xml"));
            String onlinsuppcdat = doc.getRootElement().getChild("OnlineSupportInformation").getText();
            CDATA cdata = new CDATA(onlinsuppcdat);
            host.setOnlineInformationHTML(cdata.getText());
            onlinsuppcdat = doc.getRootElement().getChild("News").getText();
            cdata = new CDATA(onlinsuppcdat);
            host.setNewsHTML(cdata.getText());
            host.fillData();
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }
}

class LocalHtmlRendererContext extends SimpleHtmlRendererContext {

    private OpenerIF host;

    public LocalHtmlRendererContext(HtmlPanel contextComponent, UserAgentContext ucontext, OpenerIF host) {
        super(contextComponent, ucontext);
        this.host = host;
    }

    public void linkClicked(HTMLElement linkNode, URL url, String target) {
        System.out.println("## Link clicked: " + linkNode);
        host.clickAction(linkNode.toString());
    }
}
