package hu.sztaki.lpds.submitter.grids;

import dci.data.Item;
import dci.data.Item.*;
import hu.sztaki.lpds.dcibridge.config.Conf;
import hu.sztaki.lpds.dcibridge.service.Base;
import hu.sztaki.lpds.dcibridge.service.Job;
import hu.sztaki.lpds.dcibridge.service.LB;
import hu.sztaki.lpds.dcibridge.util.BinaryHandler;
import hu.sztaki.lpds.dcibridge.util.InputHandler;
import hu.sztaki.lpds.dcibridge.util.OutputHandler;
import hu.sztaki.lpds.dcibridge.util.XMLHandler;
import hu.sztaki.lpds.metabroker.client.ResourceBean;
import hu.sztaki.lpds.submitter.grids.glite.config.GLiteConfig;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Vector;
import java.util.List;
import java.util.Collections;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import javax.xml.namespace.QName;
import org.ggf.schemas.bes._2006._08.bes_factory.ActivityStateEnumeration;
import org.ggf.schemas.jsdl._2005._11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl._2005._11.jsdl.JobDefinitionType;
import org.ggf.schemas.jsdl._2005._11.jsdl_posix.POSIXApplicationType;
import org.glite.jdl.JobAd;
import org.glite.wms.wmproxy.AuthenticationFaultException;
import org.glite.wms.wmproxy.AuthorizationFaultException;
import org.glite.wms.wmproxy.JobIdStructType;
import org.glite.wms.wmproxy.ServerOverloadedFaultException;
import org.glite.wms.wmproxy.ServiceException;
import org.glite.wms.wmproxy.StringAndLongList;
import org.glite.wms.wmproxy.StringAndLongType;
import org.glite.wms.wmproxy.WMProxyAPI;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.io.urlcopy.UrlCopy;
import org.globus.util.GlobusURL;
import org.globus.util.Util;
import org.ietf.jgss.GSSCredential;
import uri.mbschedulingdescriptionlanguage.OtherType;
import uri.mbschedulingdescriptionlanguage.SDLType;

/**
  * edgi plugin
  */
public class Grid_edgi extends Middleware {

    private final boolean DEBUG = false;

    static final String JDKEY = "edgi.key";

    static final Object lLock = new Object();

    static final Object vomsLock = new Object();

    static Hashtable userproxy = new Hashtable();

    private WMProxyAPI client = null;

    /**
     * Constructor
     */
    public Grid_edgi() throws Exception {
        THIS_MIDDLEWARE = Base.MIDDLEWARE_EDGI;
        threadID++;
        setName("guse/dci-bridge:Middleware handler(edgi) - " + threadID);
    }

    /**
     * Update middleware configuration
     * @throws Configuration/infrastructure exception
     */
    @Override
    public void setConfiguration() throws Exception {
        for (Item itemElement : Conf.getMiddleware(THIS_MIDDLEWARE).getItem()) {
            Edgi edgiElement = itemElement.getEdgi();
            String urlElement = edgiElement.getUrl();
            List<Item.Edgi.Job> jobList = edgiElement.getJob();
            jobList.clear();
            queryEDGIApplications(urlElement, jobList);
        }
        Base.writeLogg(THIS_MIDDLEWARE, new LB(THIS_MIDDLEWARE + " plugin: configuration updated successfully"));
    }

    /**
      * Get application names (along with VOs and CEs) from an EDGI application repository accessible at the given repositoryURL (parameter)
      * Add each application as a "job" to the jobList (parameter), and fill in the related VO and CE substructure
      *
      *  Note: Those applications (implementations) are returned only that have:
      *    (1) VO attribute of the form: VOs.voXXX.name=gLite:VVV (XXX is a VO id number, VVV is the VO site), and
      *    (2) this VO has at least one CE site (VOs.voXXX.site=CCC, where XXX is the VO id and CCC is the CE site)
      *
      * @param repositoryURLPar The URL of the repository at which servlets "/mce/getapps", "/mce/getimps", and "/mce/getfileurls" are avaible
      * @param jobList The list of jobs (initialy empty) into which new jobs to be added. This is the output of the method.
      *
      * @throws MalformedURLException, IOException If any problem occurs at opening/reading from the repositoryURL
      */
    private void queryEDGIApplications(final String repositoryURLPar, List<Item.Edgi.Job> jobList) throws MalformedURLException, IOException {
        final boolean POST = true;
        boolean warning;
        final String GET_APPS_SERVLET = "mce/getapps";
        final String GET_APP_RESP_REGEXP = "^\\d+ .+";
        final String GET_IMPS_SERVLET = "mce/getimps";
        final String GET_IMPS_RESP_REGEXP = "^\\d+ \\d+";
        final String GET_IMP_ATTRS_SERVLET = "mce/getimpattr";
        final String GET_IMP_ATTRS_RESP_REGEXP = "^\\d+ .+=.*";
        final String GET_IMP_FILE_URL_SERVLET = "mce/getfileurls";
        final String GET_IMP_FILE_URL_RESP_REGEXP = "^\\d+ .+";
        URL url;
        URLConnection conn;
        BufferedReader rd;
        String line;
        Hashtable<String, String> apps = new Hashtable<String, String>();
        Hashtable<String, String> imps = new Hashtable<String, String>();
        Hashtable<String, Vector<String>> fileUrls = new Hashtable<String, Vector<String>>();
        Hashtable<String, Vector<String>> voIds = new Hashtable<String, Vector<String>>();
        Hashtable<String, String> voNames = new Hashtable<String, String>();
        Hashtable<String, Vector<String>> ceUrls = new Hashtable<String, Vector<String>>();
        Hashtable<String, Vector<String>> appFileUrls = new Hashtable<String, Vector<String>>();
        String repositoryURL = repositoryURLPar.endsWith("/") ? repositoryURLPar : repositoryURLPar + "/";
        if (DEBUG) System.out.println("Getting APPLICATIONS...");
        warning = false;
        url = new java.net.URL(repositoryURL + GET_APPS_SERVLET);
        conn = url.openConnection();
        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            if (!line.matches(GET_APP_RESP_REGEXP)) {
                if (!warning) {
                    Base.writeLogg(THIS_MIDDLEWARE, new LB(THIS_MIDDLEWARE + " plugin: application repository line has invalid syntax: " + line));
                    warning = true;
                }
                continue;
            }
            String appId = line.substring(0, line.indexOf(' '));
            String appName = line.substring(line.indexOf(' ') + 1).trim();
            apps.put(appId, appName);
            if (DEBUG) System.out.println("\t" + appName + " " + appId + " ");
        }
        rd.close();
        if (apps.size() == 0) {
            Base.writeLogg(THIS_MIDDLEWARE, new LB(THIS_MIDDLEWARE + " plugin: no application found in the repository"));
            return;
        }
        StringBuffer appIdListBuffer = new StringBuffer();
        for (String appId : apps.keySet()) appIdListBuffer.append("+" + appId);
        String appIdList = appIdListBuffer.toString();
        appIdList = appIdList.substring(1);
        if (DEBUG) System.out.println("\nGetting application IMPLEMENTATIONS...");
        warning = false;
        if (POST) {
            if (DEBUG) System.out.println("Querying application implementations using POST method");
            url = new java.net.URL(repositoryURL + GET_IMPS_SERVLET + "");
            conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            String pars = "appids=" + appIdList;
            wr.write(pars);
            wr.flush();
        } else {
            url = new java.net.URL(repositoryURL + GET_IMPS_SERVLET + "?appids=" + appIdList);
            conn = url.openConnection();
        }
        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            if (!line.matches(GET_IMPS_RESP_REGEXP)) {
                if (!warning) {
                    Base.writeLogg(THIS_MIDDLEWARE, new LB(THIS_MIDDLEWARE + " plugin: application implementation repository line has invalid syntax: " + line));
                    warning = true;
                }
                continue;
            }
            String appId = line.substring(0, line.indexOf(' '));
            String impId = line.substring(line.indexOf(' ') + 1).trim();
            if (DEBUG) if (imps.containsKey(impId) && !appId.equals(imps.get(impId))) if (DEBUG) System.out.println("WARNING: same implemenation but different apps");
            imps.put(impId, appId);
            if (DEBUG) System.out.println("\t" + apps.get(appId) + " " + impId + "");
        }
        rd.close();
        if (imps.size() == 0) {
            Base.writeLogg(THIS_MIDDLEWARE, new LB(THIS_MIDDLEWARE + " plugin: no implementation found in the repository"));
            return;
        }
        StringBuffer impIdListBuffer = new StringBuffer();
        for (String impId : imps.keySet()) impIdListBuffer.append("+" + impId);
        String impIdList = impIdListBuffer.toString();
        impIdList = impIdList.substring(1);
        if (DEBUG) System.out.println("\nGetting implementation URLS...");
        warning = false;
        if (POST) {
            url = new java.net.URL(repositoryURL + GET_IMP_FILE_URL_SERVLET + "");
            conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            String pars = "impids=" + impIdList;
            wr.write(pars);
            wr.flush();
        } else {
            url = new java.net.URL(repositoryURL + GET_IMP_FILE_URL_SERVLET + "?impids=" + impIdList);
            conn = url.openConnection();
        }
        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            if (!line.matches(GET_IMP_FILE_URL_RESP_REGEXP)) {
                if (!warning) {
                    Base.writeLogg(THIS_MIDDLEWARE, new LB(THIS_MIDDLEWARE + " plugin: application executable repository line has invalid syntax: " + line));
                    warning = true;
                }
                continue;
            }
            String impId = line.substring(0, line.indexOf(' '));
            String fileUrl = line.substring(line.indexOf(' ') + 1).trim();
            if (!fileUrls.containsKey(impId)) {
                Vector<String> tempFileUrls = new Vector<String>();
                tempFileUrls.add(fileUrl);
                fileUrls.put(impId, tempFileUrls);
            } else {
                fileUrls.get(impId).add(fileUrl);
            }
            if (DEBUG) System.out.println("\t" + impId + " " + fileUrl + "");
        }
        rd.close();
        if (DEBUG) System.out.println("\nGetting implementation ATTRIBUTES...");
        warning = false;
        if (POST) {
            url = new java.net.URL(repositoryURL + GET_IMP_ATTRS_SERVLET + "");
            conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            String pars = "impids=" + impIdList;
            wr.write(pars);
            wr.flush();
        } else {
            url = new java.net.URL(repositoryURL + GET_IMP_ATTRS_SERVLET + "?impids=" + impIdList);
            conn = url.openConnection();
        }
        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            if (!line.matches(GET_IMP_ATTRS_RESP_REGEXP)) continue;
            String impId = line.substring(0, line.indexOf(' '));
            line = line.substring(line.indexOf(' ') + 1);
            String attrName = line.substring(0, line.indexOf('='));
            String attrValue = line.substring(line.indexOf('=') + 1).trim();
            if (attrName.matches("^VOs[.]vo\\d+[.]name") && attrValue.matches("^gLite:.+")) {
                String voId = attrName.substring(attrName.indexOf('.') + 1, attrName.lastIndexOf('.'));
                String voName = attrValue.substring(attrValue.indexOf("gLite:") + 6);
                if (DEBUG) System.out.println("\t" + impId + " " + voName + " (" + voId + ")");
                if (voIds.containsKey(impId)) {
                    voIds.get(impId).add(voId);
                } else {
                    Vector<String> tempVoIds = new Vector<String>();
                    tempVoIds.add(voId);
                    voIds.put(impId, tempVoIds);
                }
                String key = impId + "_" + voId;
                voNames.put(key, voName);
            } else if (attrName.matches("^VOs[.]vo\\d+[.]site\\d+") && attrValue.matches(".+")) {
                String voId = attrName.substring(attrName.indexOf('.') + 1, attrName.lastIndexOf('.'));
                String ceUrl = attrValue;
                if (DEBUG) System.out.println("\t" + impId + " " + ceUrl + " (" + voId + ")");
                String key = impId + "_" + voId;
                if (ceUrls.containsKey(key)) {
                    ceUrls.get(key).add(ceUrl);
                } else {
                    Vector<String> tempCeUrls = new Vector<String>();
                    tempCeUrls.add(ceUrl);
                    ceUrls.put(key, tempCeUrls);
                }
            }
        }
        rd.close();
        Hashtable<String, Hashtable<String, Vector<String>>> finalApps = new Hashtable<String, Hashtable<String, Vector<String>>>();
        for (String impId : imps.keySet()) {
            if (voIds.containsKey(impId)) {
                for (String voId : voIds.get(impId)) {
                    String key = impId + "_" + voId;
                    final String appName = apps.get(imps.get(impId));
                    final String voName = voNames.get(key);
                    Hashtable<String, Vector<String>> tempVoHash = null;
                    Vector<String> tempCeList = null;
                    if (ceUrls.containsKey(key)) {
                        for (String ceUrl : ceUrls.get(key)) {
                            if (!finalApps.containsKey(appName)) {
                                tempCeList = new Vector<String>();
                                tempCeList.add(ceUrl);
                                tempVoHash = new Hashtable<String, Vector<String>>();
                                tempVoHash.put(voName, tempCeList);
                                finalApps.put(appName, tempVoHash);
                            } else {
                                tempVoHash = finalApps.get(appName);
                                if (!tempVoHash.containsKey(voName)) {
                                    tempCeList = new Vector<String>();
                                    tempCeList.add(ceUrl);
                                    tempVoHash.put(voName, tempCeList);
                                } else {
                                    tempCeList = tempVoHash.get(voName);
                                    if (!tempCeList.contains(ceUrl)) tempCeList.add(ceUrl); else if (DEBUG) System.out.println("WARNING: Duplicate CE URL: " + ceUrl + " " + appName + "(" + impId + ") VO:" + voName + "(" + voId + ") ");
                                }
                            }
                            if (fileUrls.containsKey(impId)) {
                                Vector<String> fs = fileUrls.get(impId);
                                if (DEBUG) for (String file : fs) {
                                    System.out.println("-----impId:" + impId + "appName:" + appName + "--file:" + file);
                                }
                                appFileUrls.put(appName, fs);
                            }
                        }
                    }
                }
            }
        }
        if (finalApps.size() == 0) {
            Base.writeLogg(THIS_MIDDLEWARE, new LB(THIS_MIDDLEWARE + " plugin: no application found with VO.name (gLite) and CE.site attributes"));
            return;
        }
        if (DEBUG) {
            System.out.println("Listing APPs, VOs, and CEs...");
            for (String app : finalApps.keySet()) {
                System.out.println("\n\"" + app + "\"");
                Hashtable<String, Vector<String>> vos = finalApps.get(app);
                for (String vo : vos.keySet()) {
                    System.out.println("\tVO: " + vo);
                    Vector<String> ces = vos.get(vo);
                    for (String ce : ces) {
                        System.out.println("\t\tCE: " + ce);
                    }
                }
            }
        }
        Vector<String> sortedApps = new Vector<String>();
        for (String appName : finalApps.keySet()) sortedApps.add(appName);
        Collections.sort(sortedApps, new AppNameComparator());
        for (String appName : sortedApps) {
            Item.Edgi.Job job = new Item.Edgi.Job();
            job.setName(appName);
            try {
                Vector<String> exeurls = appFileUrls.get(appName);
                for (String eurl : exeurls) {
                    Edgi.Job.Exeurl exeurl = new Edgi.Job.Exeurl();
                    exeurl.setUrl(eurl);
                    job.getExeurl().add(exeurl);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            jobList.add(job);
            Hashtable<String, Vector<String>> vos = finalApps.get(appName);
            for (String voName : vos.keySet()) {
                Item.Edgi.Job.Vo vo = new Item.Edgi.Job.Vo();
                vo.setName(voName);
                job.getVo().add(vo);
                Vector<String> ces = vos.get(voName);
                for (String ceName : ces) {
                    vo.getCe().add(ceName);
                }
            }
        }
    }

    class AppNameComparator implements Comparator<String> {

        private Collator collator = null;

        AppNameComparator() {
            collator = Collator.getInstance(new Locale("en", "US"));
        }

        public int compare(String o1, String o2) {
            return collator.compare(o1.toUpperCase(), o2.toUpperCase());
        }
    }

    /**
     * Aborts the job
     * @param pJob
     */
    public void abort(Job pJob) {
        String path = Base.getI().getJobDirectory(pJob.getId());
        cleanupJob(pJob);
        errorLog(path + "outputs/", "- - - - - - - - \nABORTED by user");
        pJob.setStatus(ActivityStateEnumeration.CANCELLED);
    }

    /**
     * Abort job, and cleanup.
     * @param pJob
     */
    private void cleanupJob(Job pJob) {
        String path = Base.getI().getJobDirectory(pJob.getId());
        if (pJob.getMiddlewareId() != null) {
            synchronized (lLock) {
                try {
                    client = new WMProxyAPI(GLiteConfig.getI().getWMProxyUrl(pJob.getConfiguredResource().getVo()), path + "/x509up");
                    sysLog(path + "outputs/", " ABORT - jobcancel");
                    client.jobCancel(pJob.getMiddlewareId());
                } catch (Exception e) {
                    try {
                        sysLog(path + "outputs/", "ABORT - jobcancel failed -> jobPurge ...   reason:" + e.getMessage());
                        client.jobPurge(pJob.getMiddlewareId());
                    } catch (Exception ee) {
                    }
                } finally {
                    client = null;
                }
            }
        }
    }

    /**
     * Submits the job
     * @param pJob
     * @throws java.lang.Exception
     */
    protected void submit(Job pJob) throws Exception {
        String path = Base.getI().getJobDirectory(pJob.getId());
        JobDefinitionType jsdl = pJob.getJSDL();
        POSIXApplicationType pType = XMLHandler.getData(jsdl.getJobDescription().getApplication().getAny(), POSIXApplicationType.class);
        String userid = BinaryHandler.getUserName(pType);
        try {
            createJobad(pJob);
            if (!getProxy(path, pJob, userid, false)) {
                System.out.println("failed creating proxy");
                pJob.setStatus(ActivityStateEnumeration.FAILED);
                return;
            }
            doSubmit(pJob);
        } catch (Exception e) {
            pJob.setStatus(ActivityStateEnumeration.FAILED);
            errorLog(path + "outputs/", "Job submit failed. ", e);
            e.printStackTrace();
        }
    }

    /**
     * Is not used, the job output is get after getstatus or send by callback
     * @param pJob
     * @throws java.lang.Exception
     */
    protected void getOutputs(Job pJob) throws Exception {
    }

    /**
     * Get job status
     * @param pJob
     */
    protected void getStatus(Job pJob) {
        gstatus(pJob);
    }

    private boolean cp(String inf, String outf) {
        try {
            File inputFile = new File(inf);
            File outputFile = new File(outf);
            FileReader in = new FileReader(inputFile);
            FileWriter out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
            in.close();
            out.close();
            Util.setFilePermissions(outf, 600);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
 * Try to get proxy from cache, in failed create it
 * @param path
 * @param resource
 * @param userid
 * @param renew if TRUE, the proxy already exists, do not save it again as x509up_o
 * @return
 */
    private boolean getProxy(String path, Job pJob, String userid, Boolean renew) {
        String grid = pJob.getConfiguredResource().getVo();
        String role = "";
        try {
            role = pJob.getJSDL().getJobDescription().getResources().getOtherAttributes().get(QName.valueOf("gliterole"));
        } catch (Exception e) {
        }
        String proxy = path + "x509up";
        boolean success = true;
        synchronized (vomsLock) {
            try {
                if ((System.currentTimeMillis() - Long.parseLong("" + userproxy.get(userid + grid + role))) < 1800000) {
                    sysLog(path + "outputs/", "+--------- getProxy - use cache --------");
                    if (!renew) {
                        try {
                            new File(proxy).renameTo(new File(proxy + "_o"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    cp(getProxyCachedir() + userid + "/x509up." + grid + role, proxy);
                } else {
                    if (!renew) {
                        cp(proxy, proxy + "_o");
                    }
                    success = createProxy(path, grid, userid, role);
                }
            } catch (Exception e) {
                sysLog(path + "outputs/", "+--------- getProxy - not cached --------");
                try {
                    cp(proxy, proxy + "_o");
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
                success = createProxy(path, grid, userid, role);
            }
        }
        return success;
    }

    /**
     * Adds voms extension to the jobs proxy
     * @param localDir
     * @param voname
     * @return boolean
     */
    private boolean addVomsC(String localDir, String voname, String role) {
        try {
            String cmd;
            if ("".equals(role)) {
                cmd = "voms-proxy-init -voms " + voname + ": -noregen -out x509up";
            } else {
                cmd = "voms-proxy-init -voms " + voname + ":/" + voname + "/Role=" + role + " -noregen -out x509up";
            }
            sysLog(localDir + "outputs/", "localDir:" + localDir + " cmd:" + cmd);
            sysLog(localDir + "outputs/", cmd);
            Process p;
            p = Runtime.getRuntime().exec(cmd, null, new File(localDir));
            BufferedReader sin = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            int exitv = p.waitFor();
            if (exitv == 0) {
                sin.close();
                return true;
            } else {
                String sor = "";
                errorLog(localDir + "outputs/", cmd + " \n failed.");
                while ((sor = sin.readLine()) != null) {
                    sysLog(localDir + "outputs/", sor);
                    errorLog(localDir + "outputs/", sor);
                }
                errorLog(localDir + "outputs/", "\n");
                sin.close();
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean createProxy(String path, String grid, String userid, String role) {
        String OutputDir = path + "outputs/";
        String proxy = path + "x509up";
        sysLog(OutputDir, "+--------- getProxy - start - proxy:" + proxy + "-------");
        try {
            GlobusCredential agcred = new GlobusCredential(proxy);
            GlobusGSSCredentialImpl agssproxy = new GlobusGSSCredentialImpl(agcred, GSSCredential.INITIATE_AND_ACCEPT);
            sysLog(OutputDir, "rem.life: " + agssproxy.getRemainingLifetime());
            if (agssproxy.getRemainingLifetime() < 600) {
                throw new Exception("Certificate expired. ");
            }
            if (addVomsC(path, grid, role)) {
                sysLog(OutputDir, "VOMS ext. succesfully added");
            } else {
                throw new Exception("Add VOMS - error. The remaining lifetime of the proxy in the submission time: " + calculateHMS(agssproxy.getRemainingLifetime()));
            }
            synchronized (lLock) {
                client = new WMProxyAPI(GLiteConfig.getI().getWMProxyUrl(grid), proxy);
                sysLog(OutputDir, "getProxyReq .....");
                String rproxy = "";
                int ntry = 0;
                boolean btry = true;
                while (btry) {
                    try {
                        rproxy = client.getProxyReq(userid);
                        btry = false;
                    } catch (AuthenticationFaultException e1) {
                        retryOrThrowException(OutputDir, ntry++, 1, "Authentication Failed: " + e1.getMessage());
                    } catch (AuthorizationFaultException e2) {
                        retryOrThrowException(OutputDir, ntry++, 1, "Authorization Failed: " + e2.getMessage());
                    } catch (ServerOverloadedFaultException e3) {
                        retryOrThrowException(OutputDir, ntry++, 6, "Server is Overloaded: " + e3.getMessage());
                    } catch (ServiceException e4) {
                        if (e4.getMessage().contains("unsupported_certificate")) {
                            throw new Exception("Your proxy is not valid! Please upload your user cert and key from command line to the myproxy server and download the new proxy. " + "You can use the following command:\n\n" + "myproxy-init -s myproxy.server.hostname -l MyProxyAccount -c 0 -t 100" + " \n\n\n WMS Service Error: " + e4.getMessage());
                        }
                        retryOrThrowException(OutputDir, ntry++, 2, "WMS Service Error: " + e4.getMessage());
                    }
                }
                sysLog(OutputDir, "getProxyReq result [" + rproxy + "]");
                sysLog(OutputDir, "grstPutProxy .....");
                client.grstPutProxy(userid, rproxy);
            }
        } catch (Exception exc) {
            errorLog(OutputDir, "Preparation of the proxy for VO " + grid + " failed. ", exc);
            sysLog(OutputDir, exc.toString());
            client = null;
            return false;
        } finally {
            client = null;
        }
        sysLog(OutputDir, "+-----------------------------------getProxy - succes-----------------------------------+");
        if (!new File(getProxyCachedir() + userid).exists()) {
            new File(getProxyCachedir() + userid).mkdirs();
        }
        new File(getProxyCachedir() + userid + "/x509up." + grid + role).delete();
        cp(proxy, getProxyCachedir() + userid + "/x509up." + grid + role);
        userproxy.put("" + userid + grid + role, System.currentTimeMillis());
        sysLog(OutputDir, "userproxy creation times:" + userproxy);
        return true;
    }

    private void retryOrThrowException(String logdir, int numoftry, int maxnumoftry, String excmsg) throws Exception {
        if (numoftry < maxnumoftry) {
            sysLog(logdir, numoftry + "/" + maxnumoftry + " sleep 10sec msg:" + excmsg);
            try {
                Thread.sleep(10000);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } else {
            throw new Exception(excmsg);
        }
    }

    private String calculateHMS(int timeInSeconds) {
        int hours, minutes, seconds;
        hours = timeInSeconds / 3600;
        timeInSeconds = timeInSeconds - (hours * 3600);
        minutes = timeInSeconds / 60;
        timeInSeconds = timeInSeconds - (minutes * 60);
        seconds = timeInSeconds;
        return (hours + ":" + minutes + ":" + seconds);
    }

    private String getProxyCachedir() {
        return Base.getI().getPath() + "proxycache/";
    }

    /**
     * Upload local inputs, and submit.
     * @param pJob
     * @return
     * @throws java.lang.Exception
     */
    private boolean doSubmit(Job pJob) throws Exception {
        POSIXApplicationType pType = XMLHandler.getData(pJob.getJSDL().getJobDescription().getApplication().getAny(), POSIXApplicationType.class);
        String userid = BinaryHandler.getUserName(pType);
        String path = Base.getI().getJobDirectory(pJob.getId());
        String grid = pJob.getConfiguredResource().getVo();
        String proxy = path + "/x509up";
        String OutputDir = path + "outputs/";
        String[] reduced_path = null;
        sysLog(OutputDir, "+--------- doSubmit Contacting:" + GLiteConfig.getI().getWMProxyUrl(grid) + " -------");
        try {
            List<DataStagingType> inputs = InputHandler.getlocalInputs(pJob);
            JobAd jad = new JobAd();
            jad.fromFile(path + "outputs/job.jdl");
            String jdlString = jad.toString();
            int ntry = 0;
            boolean btry = true;
            while (btry) {
                try {
                    sysLog(OutputDir, "jobRegister... try:" + ntry);
                    synchronized (lLock) {
                        if (pJob.getMiddlewareId() == null || "".equals(pJob.getMiddlewareId())) {
                            client = new WMProxyAPI(GLiteConfig.getI().getWMProxyUrl(grid), proxy);
                            JobIdStructType jobIDurl = client.jobRegister(jdlString, userid);
                            if (jobIDurl != null) {
                                pJob.setMiddlewareId(jobIDurl.getId());
                            } else {
                                sysLog(OutputDir, "SUBMIT E R R O R ! - Job submission failed. ");
                                errorLog(OutputDir, "Job submission failed. ");
                                client = null;
                                return false;
                            }
                        }
                        if (inputs.size() > 0) {
                            org.glite.wms.wmproxy.StringList InputSandboxURI = client.getSandboxDestURI(pJob.getMiddlewareId(), "gsiftp");
                            reduced_path = InputSandboxURI.getItem();
                        }
                        btry = false;
                    }
                } catch (ServerOverloadedFaultException e3) {
                    retryOrThrowException(OutputDir, ntry++, 6, "Server is Overloaded: " + e3.getMessage());
                } catch (ServiceException e4) {
                    retryOrThrowException(OutputDir, ntry++, 2, "WMS Service Error: " + e4.getMessage());
                }
            }
            sysLog(OutputDir, " submit successfull jobID:" + pJob.getMiddlewareId());
            FileWriter furl = new FileWriter(path + "outputs/job.url");
            BufferedWriter out = new BufferedWriter(furl);
            out.write("" + pJob.getMiddlewareId());
            out.flush();
            out.close();
            if (inputs.size() > 0) {
                int pos = (reduced_path[0]).indexOf("2811");
                int length = (reduced_path[0]).length();
                String front = (reduced_path[0]).substring(0, pos);
                String rear = (reduced_path[0]).substring(pos + 4, length);
                String TURL = front + "2811/" + rear;
                GlobusCredential gcred = new GlobusCredential(proxy);
                GSSCredential gssproxy = new GlobusGSSCredentialImpl(gcred, GSSCredential.INITIATE_AND_ACCEPT);
                for (DataStagingType inp : inputs) {
                    String toURL = TURL + "/" + inp.getFileName();
                    String fromURL = "file:///" + path + "/" + inp.getFileName();
                    sysLog(OutputDir, "fromURL:" + fromURL + " toURL:" + toURL);
                    try {
                        GlobusURL from = new GlobusURL(fromURL);
                        GlobusURL to = new GlobusURL(toURL);
                        UrlCopy uCopy = new UrlCopy();
                        uCopy.setCredentials(gssproxy);
                        uCopy.setDestinationUrl(to);
                        uCopy.setSourceUrl(from);
                        uCopy.setUseThirdPartyCopy(true);
                        uCopy.copy();
                    } catch (Exception e) {
                        sysLog(OutputDir, "Can not copy the Input files:" + inp.getFileName() + " - " + e.toString());
                        errorLog(OutputDir, "Can not copy the Input files:" + inp.getFileName() + " - " + e.getMessage());
                        client = null;
                        return false;
                    }
                }
            }
            ntry = 0;
            btry = true;
            while (btry) {
                try {
                    synchronized (lLock) {
                        client = new WMProxyAPI(GLiteConfig.getI().getWMProxyUrl(grid), proxy);
                        client.jobStart(pJob.getMiddlewareId());
                        btry = false;
                    }
                } catch (ServerOverloadedFaultException e3) {
                    retryOrThrowException(OutputDir, ntry++, 6, "Server is Overloaded: " + e3.getMessage());
                } catch (ServiceException e4) {
                    retryOrThrowException(OutputDir, ntry++, 2, "WMS Service Error: " + e4.getMessage());
                }
            }
            pJob.setStatus(ActivityStateEnumeration.PENDING);
            pJob.setResource(grid + " WMS");
            for (DataStagingType inp : inputs) {
                try {
                    File delfile = new File(path + "/" + inp.getFileName());
                    if (delfile.exists()) {
                        delfile.delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception exc) {
            errorLog(OutputDir, "Job submit ERROR. \n " + exc.getMessage());
            sysLog(OutputDir, exc.toString());
            exc.printStackTrace();
            pJob.setStatus(ActivityStateEnumeration.FAILED);
            client = null;
            return false;
        } finally {
            client = null;
        }
        sysLog(OutputDir, "+-----------------------------------doSubmit - succes-----------------------------------+");
        return true;
    }

    /**
     * Generates the JDL
     * @param pJob
     * @return
     */
    private JobAd createJobad(Job pJob) {
        JobDefinitionType jsdl = pJob.getJSDL();
        String path = Base.getI().getJobDirectory(pJob.getId());
        POSIXApplicationType pType = XMLHandler.getData(jsdl.getJobDescription().getApplication().getAny(), POSIXApplicationType.class);
        String OutputDir = path + "outputs/";
        JobAd jobad = new JobAd();
        try {
            String params = "";
            for (String t : BinaryHandler.getCommandLineParameter(pType)) {
                params = params.concat(" " + t);
            }
            if (!"".equals(params)) {
                jobad.addAttribute("Arguments", params);
            }
            HashMap<String, String> inputsandbox = new HashMap<String, String>();
            String ar = jsdl.getJobDescription().getResources().getOtherAttributes().get(QName.valueOf("ar"));
            Item.Edgi config = Conf.getItem(Base.MIDDLEWARE_EDGI, "edgi1").getEdgi();
            String exe = null;
            List<dci.data.Item.Edgi.Job> jobsinar = config.getJob();
            for (dci.data.Item.Edgi.Job arjob : jobsinar) {
                if (arjob.getName().equals(jsdl.getJobDescription().getJobIdentification().getJobName())) {
                    List<Edgi.Job.Exeurl> exeurls = arjob.getExeurl();
                    for (Edgi.Job.Exeurl eurl : exeurls) {
                        if (exe == null) {
                            exe = eurl.getUrl();
                        }
                        inputsandbox.put(eurl.getUrl(), "");
                    }
                    break;
                }
            }
            try {
                String exetostart = exe.substring(exe.lastIndexOf("/") + 1);
                jobad.addAttribute("Executable", exetostart);
            } catch (Exception e) {
                errorLog(OutputDir, "Remote exe not found or an error occurred, set it to: " + jsdl.getJobDescription().getJobIdentification().getJobName());
                jobad.addAttribute("Executable", jsdl.getJobDescription().getJobIdentification().getJobName());
            }
            List<DataStagingType> rinputs = InputHandler.getRemoteInputs(pJob);
            for (DataStagingType t : rinputs) {
                inputsandbox.put(t.getSource().getURI(), "");
            }
            List<DataStagingType> inputs = InputHandler.getlocalInputs(pJob);
            for (DataStagingType inp : inputs) {
                inputsandbox.put(inp.getFileName(), "");
            }
            for (String input : inputsandbox.keySet()) {
                jobad.addAttribute("InputSandbox", input);
            }
            for (String outpname : getOutputSandboxFileNames(pJob)) {
                jobad.addAttribute("OutputSandbox", outpname);
            }
            List<DataStagingType> routputs = OutputHandler.getRemoteOutputs(pJob.getJSDL());
            for (DataStagingType t : routputs) {
                jobad.addAttribute("OutputSandbox", t.getSource().getURI());
            }
            SDLType sdlType = XMLHandler.getData(jsdl.getAny(), SDLType.class);
            List ls = sdlType.getConstraints().getOtherConstraint();
            Iterator it = ls.iterator();
            String userreq = "";
            while (it.hasNext()) {
                OtherType value = (OtherType) it.next();
                if (value.getName().indexOf(JDKEY) > -1) {
                    String sKey = value.getName().replaceAll(JDKEY, "");
                    String sValue = value.getValue();
                    sysLog(OutputDir, "KEY: " + sKey);
                    sysLog(OutputDir, " Value: " + sValue);
                    if (!(sValue).trim().equals("")) {
                        try {
                            if (sKey.equals("requirements")) {
                                if (sValue.contains("other.GlueCEStateStatus")) {
                                    userreq = sValue;
                                } else {
                                    userreq = "(other.GlueCEStateStatus==\"Production\") && (" + sValue + ")";
                                }
                            } else if (sKey.equals("Rank")) {
                                jobad.setAttributeExpr(sKey, sValue);
                            } else if (sKey.equals("ShallowRetryCount")) {
                                jobad.addAttribute(sKey, Integer.parseInt(sValue));
                            } else if (sKey.equals("RetryCount")) {
                                jobad.addAttribute(sKey, Integer.parseInt(sValue));
                            } else if (sKey.equals("Environment")) {
                                jobad.setAttribute("Environment", "" + sValue);
                            } else {
                                jobad.addAttribute(sKey, sValue);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
            try {
                String myproxy = sdlType.getConstraints().getMiddleware().get(0).getMyProxy().getServerName();
                jobad.addAttribute("MyProxyServer", myproxy);
            } catch (Exception e) {
            }
            String ds = "";
            String req = "";
            ds = ds.trim().replaceAll(" ", " && ");
            if (!"".equals(ds) && !"".equals(userreq)) {
                req = userreq + " && ( " + ds + " )";
            } else if ("".equals(ds) && !"".equals(userreq)) {
                req = userreq;
            } else if (!"".equals(ds) && "".equals(userreq)) {
                req = "(other.GlueCEStateStatus==\"Production\") && ( " + ds + " )";
            } else {
                req = "other.GlueCEStateStatus==\"Production\"";
            }
            sysLog(OutputDir, "requirements:" + req);
            try {
                jobad.setAttributeExpr("requirements", req);
            } catch (Exception e) {
                sysLog(OutputDir, "requirements: " + e.toString());
                errorLog(OutputDir, "Error in JDL attribute requirements: \"" + req + "\" ", e);
                return null;
            }
            try {
                jobad.setAttributeExpr("Rank", "-other.GlueCEStateEstimatedResponseTime");
            } catch (Exception e) {
                sysLog(OutputDir, "Rank: " + e.toString());
            }
            if (!jobad.hasAttribute("ShallowRetryCount")) {
                jobad.addAttribute("ShallowRetryCount", 0);
            }
            if (!jobad.hasAttribute("RetryCount")) {
                jobad.addAttribute("RetryCount", 0);
            }
            jobad.addAttribute("SubmitTo", pJob.getConfiguredResource().getResource() + "/" + pJob.getConfiguredResource().getJobmanager());
            sysLog(OutputDir, jobad.toLines());
            try {
                FileWriter tmp = new FileWriter(path + "outputs/job.jdl", false);
                BufferedWriter out = new BufferedWriter(tmp);
                out.write(jobad.toLines());
                out.flush();
                out.close();
            } catch (Exception e) {
                sysLog(OutputDir, e.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return jobad;
    }

    /** Converts status string to ActivityStateEnumeration status code
     *  @return status code
     */
    private ActivityStateEnumeration toActivityStatusCode(String st) {
        if (st.equals("Submitted")) return ActivityStateEnumeration.PENDING; else if (st.equals("Waiting")) return ActivityStateEnumeration.PENDING; else if (st.equals("Ready")) return ActivityStateEnumeration.PENDING; else if (st.equals("Scheduled")) return ActivityStateEnumeration.PENDING; else if (st.equals("Running")) return ActivityStateEnumeration.RUNNING; else if (st.equals("Cancelled")) return ActivityStateEnumeration.FAILED; else if (st.equals("Aborted")) return ActivityStateEnumeration.FAILED; else if (st.equals("Done")) return ActivityStateEnumeration.FINISHED; else if (st.equals("Done error")) return ActivityStateEnumeration.PENDING; else if (st.equals("Cleared")) return ActivityStateEnumeration.FAILED; else if (st.equals("getOutput")) return ActivityStateEnumeration.RUNNING; else if (st.equals("submitting")) return ActivityStateEnumeration.PENDING; else if (st.equals("Done (Failed)")) return ActivityStateEnumeration.FAILED;
        return ActivityStateEnumeration.PENDING;
    }

    /**
     * Get job status from grid.
     * Resubmit/ get output if necessary.
     * @param pJob
     */
    private void gstatus(Job pJob) {
        String OutputDir = Base.getI().getJobDirectory(pJob.getId()) + "outputs/";
        String stat = "";
        BufferedReader sin = null;
        BufferedReader sinerr = null;
        try {
            String cmd = "glite-wms-job-status " + pJob.getMiddlewareId();
            Process p;
            p = Runtime.getRuntime().exec(cmd, null, new File(Base.getI().getJobDirectory(pJob.getId())));
            sin = new BufferedReader(new InputStreamReader(p.getInputStream()));
            sinerr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            int exitv = p.waitFor();
            String sor;
            String msg = "";
            sysLog(OutputDir, cmd);
            if (exitv == 0) {
                while ((sor = sin.readLine()) != null) {
                    if (sor.contains("Current Status:")) {
                        stat = sor.substring(16, sor.length()).trim();
                    } else if (sor.contains("Destination:")) {
                        String resource = sor.substring(13, sor.length()).trim();
                        pJob.setResource(resource);
                    }
                    msg += sor + "\n";
                }
                sin.close();
                sysLog(OutputDir, "gstat.exit:" + exitv + " Current Status:" + stat + " Destination:" + pJob.getResource());
                if (stat.equals("Done (Success)")) {
                    if (getOutputFilesAndPurge(pJob)) {
                        pJob.setStatus(ActivityStateEnumeration.FINISHED);
                    } else {
                        pJob.setStatus(ActivityStateEnumeration.FAILED);
                    }
                } else if (stat.equals("Done (Exit Code !=0)") || stat.equals("Done (Failed)")) {
                    sysLog(OutputDir, msg);
                    getOutputFilesAndPurge(pJob);
                    errorLog(OutputDir, msg);
                    pJob.setStatus(ActivityStateEnumeration.FAILED);
                } else if (!stat.equals("")) {
                    pJob.setStatus(toActivityStatusCode(stat));
                }
                if (stat.equals("Aborted") || stat.equals("Cancelled")) {
                    errorLog(OutputDir, msg);
                    sysLog(OutputDir, msg);
                    pJob.setStatus(ActivityStateEnumeration.FAILED);
                }
            } else {
                sysLog(OutputDir, "gstat.exit:" + exitv + " Current Status:" + stat + " Destination:" + pJob.getResource());
                while ((sor = sinerr.readLine()) != null) {
                    msg += sor + "\n";
                }
                sinerr.close();
                if (msg.contains("PROXY_EXPIRED")) {
                    sysLog(OutputDir, "PROXY_EXPIRED -> renew voms");
                    try {
                        String path = Base.getI().getJobDirectory(pJob.getId());
                        String proxy = path + "/x509up";
                        new File(proxy).delete();
                        new File(proxy + "_o").renameTo(new File(proxy));
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                    boolean success = false;
                    String path = Base.getI().getJobDirectory(pJob.getId());
                    JobDefinitionType jsdl = pJob.getJSDL();
                    POSIXApplicationType pType = XMLHandler.getData(jsdl.getJobDescription().getApplication().getAny(), POSIXApplicationType.class);
                    String userid = BinaryHandler.getUserName(pType);
                    success = getProxy(path, pJob, userid, true);
                    if (success) {
                        return;
                    }
                } else if (msg.contains("server closed the connection, probably due to overload") || msg.contains("Connection timed out")) {
                    synchronized (lLock) {
                        try {
                            sysLog(OutputDir, msg + " SLEEP 60 sec");
                            Thread.sleep(60000);
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                    return;
                }
                errorLog(OutputDir, "It was not possible to query the status. \n " + msg);
                sysLog(OutputDir, msg);
                synchronized (lLock) {
                    try {
                        sysLog(OutputDir, "SLEEP 60 sec");
                        Thread.sleep(60000);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
                pJob.setStatus(ActivityStateEnumeration.FAILED);
            }
        } catch (Exception ex) {
            sysLog(OutputDir, "ERROR! gstatus" + ex.getMessage());
            pJob.setStatus(ActivityStateEnumeration.FAILED);
        } finally {
            try {
                sin.close();
            } catch (Exception e) {
            }
            try {
                sinerr.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Get jobs local outputs.
     * @param pJob
     * @return
     */
    private boolean getOutputFilesAndPurge(Job pJob) {
        String grid = pJob.getConfiguredResource().getVo();
        String OutputDir = Base.getI().getJobDirectory(pJob.getId()) + "outputs/";
        String proxy = Base.getI().getJobDirectory(pJob.getId()) + "x509up";
        StringAndLongList result = null;
        StringAndLongType[] list = null;
        int size = 0;
        boolean succes = true;
        try {
            Vector outputf = getOutputSandboxFileNames(pJob);
            size = outputf.size();
            sysLog(OutputDir, "+--------------------------getoutput:" + size + "-------------------------------------------+");
            if (size > 0) {
                int ntry = 0;
                boolean btry = true;
                while (btry) {
                    try {
                        sysLog(OutputDir, "getOutputFileList ...try:" + ntry);
                        synchronized (lLock) {
                            client = new WMProxyAPI(GLiteConfig.getI().getWMProxyUrl(grid), proxy);
                            result = client.getOutputFileList(pJob.getMiddlewareId(), "gsiftp");
                            btry = false;
                        }
                    } catch (ServerOverloadedFaultException e3) {
                        retryOrThrowException(OutputDir, ntry++, 6, "Server is Overloaded: " + e3.getMessage());
                    } catch (ServiceException e4) {
                        retryOrThrowException(OutputDir, ntry++, 2, "WMS Service Error: " + e4.getMessage());
                    }
                }
                if (result != null) {
                    list = (StringAndLongType[]) result.getFile();
                    if (list != null) {
                        if (list.length != size) {
                            sysLog(OutputDir, "Some file(s) listed in the output sandbox were not available..");
                            errorLog(OutputDir, "Some file(s) listed in the output sandbox were not available..");
                        }
                        sysLog(OutputDir, "Downloading output files ...");
                        GlobusCredential gcred = new GlobusCredential(proxy);
                        GSSCredential gssproxy = new GlobusGSSCredentialImpl(gcred, GSSCredential.INITIATE_AND_ACCEPT);
                        for (int i = 0; i < size; i++) {
                            try {
                                int pos = (list[i].getName()).indexOf("2811");
                                int length = (list[i].getName()).length();
                                String front = (list[i].getName()).substring(0, pos);
                                String rear = (list[i].getName()).substring(pos + 4, length);
                                String fromURL = front + "2811/" + rear;
                                String toURL = "file:///" + OutputDir + rear.substring(rear.lastIndexOf("/"));
                                sysLog(OutputDir, i + " get fromURL:" + fromURL + " toURL:" + toURL);
                                GlobusURL from = new GlobusURL(fromURL);
                                GlobusURL to = new GlobusURL(toURL);
                                UrlCopy uCopy = new UrlCopy();
                                uCopy.setCredentials(gssproxy);
                                uCopy.setDestinationUrl(to);
                                uCopy.setSourceUrl(from);
                                uCopy.setUseThirdPartyCopy(true);
                                uCopy.copy();
                            } catch (ArrayIndexOutOfBoundsException ae) {
                                succes = false;
                                sysLog(OutputDir, i + "Can not copy the Output file:" + outputf.get(i) + " - the file does not exist." + ae.getMessage());
                                errorLog(OutputDir, "Can not copy the Output file:" + outputf.get(i) + " - the file does not exist.");
                            } catch (Exception e) {
                                succes = false;
                                sysLog(OutputDir, i + " Can not copy the Output file:" + outputf.get(i) + " - " + e);
                                errorLog(OutputDir, "Can not copy the Output file:" + outputf.get(i) + " - " + e.getMessage());
                            }
                        }
                    } else {
                        sysLog(OutputDir, "No output files for this job!");
                    }
                } else {
                    sysLog(OutputDir, "An empty list has been received");
                }
            }
            sysLog(OutputDir, "+--------------------------getoutput-Success---------------------------------------------+");
        } catch (Exception exc) {
            errorLog(OutputDir, exc.getMessage());
            sysLog(OutputDir, exc.toString());
            sysLog(OutputDir, "+--------------------------getoutput-FAILED-----------------------------------------------+");
            succes = false;
        } finally {
            try {
                sysLog(OutputDir, "jobPurge ...");
                synchronized (lLock) {
                    client = new WMProxyAPI(GLiteConfig.getI().getWMProxyUrl(grid), proxy);
                    client.jobPurge(pJob.getMiddlewareId());
                }
            } catch (Exception exc) {
                sysLog(OutputDir, "jobPurge ERROR:" + exc.toString());
            }
            client = null;
        }
        return succes;
    }

    private Vector<String> getOutputSandboxFileNames(Job pJob) {
        Vector files = new Vector();
        List<DataStagingType> outputs = OutputHandler.getLocalOutputs(pJob.getJSDL());
        for (DataStagingType outp : outputs) {
            if (!("gridnfo.log".equals(outp.getFileName()) || "stdout.log".equals(outp.getFileName()) || "stderr.log".equals(outp.getFileName()) || "guse.jsdl".equals(outp.getFileName()) || "guse.logg".equals(outp.getFileName()))) {
                files.add(outp.getFileName());
            }
        }
        return files;
    }

    @Override
    public void run() {
        Base.writeLogg(THIS_MIDDLEWARE, new LB("starting thread - EDGI"));
        Job tmp = null;
        while (true) {
            try {
                tmp = jobs.take();
                switch(tmp.getFlag()) {
                    case SUBMIT:
                        Base.initLogg(tmp.getId(), "logg.job.submit");
                        submit(tmp);
                        Base.endJobLogg(tmp, LB.INFO, "");
                        tmp.setFlag(GETSTATUS);
                        tmp.setTimestamp(System.currentTimeMillis());
                        tmp.setPubStatus(ActivityStateEnumeration.RUNNING);
                        break;
                    case GETSTATUS:
                        if ((System.currentTimeMillis() - tmp.getTimestamp()) < LAST_ACTIVATE_TIMESTAMP) {
                            try {
                                sleep(1000);
                            } catch (InterruptedException ei) {
                                Base.writeLogg(THIS_MIDDLEWARE, new LB(ei));
                            }
                        } else {
                            Base.initLogg(tmp.getId(), "logg.job.getstatus");
                            getStatus(tmp);
                            Base.endJobLogg(tmp, LB.INFO, "");
                            tmp.setTimestamp(System.currentTimeMillis());
                        }
                        break;
                    case ABORT:
                        tmp.setStatus(ActivityStateEnumeration.CANCELLED);
                        Base.initLogg(tmp.getId(), "logg.job.abort");
                        abort(tmp);
                        Base.endJobLogg(tmp, LB.INFO, "");
                        break;
                }
                if (isEndStatus(tmp)) {
                    Base.initLogg(tmp.getId(), "logg.job.getoutput");
                    getOutputs(tmp);
                    Base.endJobLogg(tmp, LB.INFO, "");
                    Base.getI().finishJob(tmp);
                } else if (isAbortStatus(tmp)) Base.getI().finishJob(tmp); else addJob(tmp);
            } catch (Exception e) {
                if (tmp != null) Base.writeJobLogg(tmp, e, "error.job." + tmp.getFlag());
            }
        }
    }

    /** stderr.log -ba logol
     */
    private void errorLog(String OutputDir, String txt) {
        try {
            FileWriter tmp = new FileWriter(OutputDir + "/stderr.log", true);
            BufferedWriter out = new BufferedWriter(tmp);
            out.newLine();
            out.write(txt);
            out.flush();
            out.close();
        } catch (Exception e) {
            sysLog(OutputDir, e.toString());
        }
    }

    /** stderr.log -ba logol
     */
    private void errorLog(String OutputDir, String pMsg, Exception pEx) {
        try {
            File f = new File(OutputDir + "/stderr.log");
            f.createNewFile();
            FileWriter fw = new FileWriter(f, true);
            fw.write(pMsg + "\n");
            fw.write(pEx.getMessage() + "\n");
            if (pEx.getCause() != null) {
                fw.write(pEx.getCause().getMessage() + "\n");
            }
            fw.write("\n");
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sysLog(String logdir, String txt) {
        try {
            if (Conf.getP().getDebug() > 0) {
                System.out.println("-" + txt);
                FileWriter tmp = new FileWriter(logdir + "/plugin.log", true);
                BufferedWriter out = new BufferedWriter(tmp);
                out.newLine();
                out.write(txt);
                out.flush();
                out.close();
            }
        } catch (Exception e) {
        }
    }
}
