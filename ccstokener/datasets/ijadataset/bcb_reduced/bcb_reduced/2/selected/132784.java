package org.ezfusion.testsbundle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.servlet.http.HttpServlet;
import org.ezfusion.dataobject.GlobalConfiguration;
import org.ezfusion.serviceint.BundleGenerator;
import org.ezfusion.serviceint.BundleInstallerInt;
import org.ezfusion.serviceint.CommunicationMgrSrv;
import org.ezfusion.serviceint.ConfigurationMgrSrv;
import org.ezfusion.serviceint.Controller;
import org.ezfusion.serviceint.EZServlet;
import org.ezfusion.serviceint.ExeEntityFactoryInt;
import org.ezfusion.serviceint.ExecutionMgrSrv;
import org.ezfusion.serviceint.FusionNodeContainerInt;
import org.ezfusion.serviceint.GlobalAdminMgrSrv;
import org.ezfusion.serviceint.Logger;
import org.ezfusion.serviceint.MsgClient;
import org.ezfusion.serviceint.MsgServer;
import org.ezfusion.serviceint.SensorInt;
import org.ezfusion.serviceint.SystemMgrInt;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {

    BundleContext context;

    private boolean tryGet(String url, Hashtable<String, String> req) throws Exception {
        boolean result = false;
        Enumeration<String> keys = req.keys();
        String key;
        String value;
        String data = "";
        while (keys.hasMoreElements()) {
            key = keys.nextElement();
            value = req.get(key);
            data += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8") + "&";
        }
        URLConnection conn = new URL(url).openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            if (line != null) result = true;
        }
        wr.close();
        rd.close();
        result = true;
        return result;
    }

    private Vector<Test> staticTests() {
        Vector<Test> tests = new Vector<Test>();
        Vector<String> srvClasses = new Vector<String>();
        srvClasses.add(BundleGenerator.class.getName());
        srvClasses.add(BundleInstallerInt.class.getName());
        srvClasses.add(CommunicationMgrSrv.class.getName());
        srvClasses.add(ConfigurationMgrSrv.class.getName());
        srvClasses.add(Controller.class.getName());
        srvClasses.add(ExecutionMgrSrv.class.getName());
        srvClasses.add(ExeEntityFactoryInt.class.getName());
        srvClasses.add(GlobalAdminMgrSrv.class.getName());
        srvClasses.add(Logger.class.getName());
        srvClasses.add(SensorInt.class.getName());
        srvClasses.add(MsgClient.class.getName());
        srvClasses.add(MsgServer.class.getName());
        srvClasses.add(SystemMgrInt.class.getName());
        ServiceReference[] refs;
        for (int i = 0; i < srvClasses.size(); i++) {
            try {
                refs = context.getAllServiceReferences(srvClasses.get(i), null);
                if (refs != null) {
                    if (refs.length == 0) tests.add(new Test("eZFusion service checking", "trying to get service " + srvClasses.get(i), false, "no reference")); else tests.add(new Test("eZFusion service checking", "trying to get service " + srvClasses.get(i), true, ""));
                } else {
                    tests.add(new Test("eZFusion service checking", "trying to get service " + srvClasses.get(i), false, "reference array is null"));
                }
            } catch (Exception e) {
                tests.add(new Test("eZFusion service checking", "trying to get service " + srvClasses.get(i), false, "exception occured " + e.getLocalizedMessage()));
            }
        }
        Hashtable<String, String> allServlets = new Hashtable<String, String>();
        allServlets.put("webMsgServiceServlet", "webmsgservice");
        allServlets.put("ezfileserver", "ezfileserver");
        allServlets.put("ezfusionconfig", "ezconfig");
        allServlets.put("ezfusion", "ezadmin");
        allServlets.put("eztree", "eztree");
        refs = null;
        try {
            refs = context.getAllServiceReferences(HttpServlet.class.getName(), "(system.name=eZFusion)");
        } catch (Exception e) {
            tests.add(new Test("eZFusion servlet checking", "checking registration for servlets", false, "no servlet found due to exception :" + e.getLocalizedMessage()));
        }
        Object srvName;
        EZServlet servlet;
        if ((refs != null) && (refs.length > 0)) {
            Enumeration<String> keys = allServlets.keys();
            String servletName;
            String servletAlias;
            boolean servletReg;
            while (keys.hasMoreElements()) {
                servletName = keys.nextElement();
                servletAlias = allServlets.get(servletName);
                servletReg = false;
                for (int i = 0; i < refs.length; i++) {
                    srvName = refs[i].getProperty("service.name");
                    if (servletName.equalsIgnoreCase(srvName.toString())) {
                        servletReg = true;
                        tests.add(new Test("eZFusion servlet checking", "checking registration for servlet " + servletName, true, ""));
                        try {
                            servlet = (EZServlet) context.getService(refs[i]);
                            if (servletAlias.equalsIgnoreCase(servlet.getAlias())) {
                                tests.add(new Test("eZFusion servlet checking", "checking alias for servlet " + servletName, true, ""));
                            } else {
                                tests.add(new Test("eZFusion servlet checking", "checking alias for servlet " + servletName, false, "Servlet is deployed with alias " + servlet.getAlias()));
                            }
                            context.ungetService(refs[i]);
                        } catch (Exception e2) {
                            tests.add(new Test("eZFusion servlet checking", "checking alias for servlet " + servletName, false, "Unable to check alias due to exception: " + e2.getLocalizedMessage()));
                        }
                    }
                }
                if (!servletReg) {
                    tests.add(new Test("eZFusion servlet checking", "checking registration for servlet " + servletName, false, "servlet not found"));
                    tests.add(new Test("eZFusion servlet checking", "checking alias for servlet " + servletName, false, "servlet not found"));
                }
                try {
                    if (tryGet("http://127.0.0.1:8080/" + servletAlias, new Hashtable<String, String>())) tests.add(new Test("eZFusion servlet checking", "checking deployment for servlet " + servletName + " with alias " + servletAlias, true, "")); else tests.add(new Test("eZFusion servlet checking", "checking deployment for servlet " + servletName + " with alias " + servletAlias, false, "result of tryGet is false"));
                } catch (Exception e) {
                    tests.add(new Test("eZFusion servlet checking", "checking deployment for servlet " + servletName + " with alias " + servletAlias, false, "unable to get alias " + servletAlias + " due to exception: " + e.getLocalizedMessage()));
                }
            }
        } else {
            tests.add(new Test("eZFusion servlet checking", "checking registration for servlets", false, "no servlet registered"));
        }
        return tests;
    }

    private Vector<Test> localTests() {
        Vector<Test> tests = new Vector<Test>();
        String localIP = "";
        Object comMgr;
        ServiceReference[] refs;
        try {
            refs = context.getAllServiceReferences(CommunicationMgrSrv.class.getName(), null);
            comMgr = context.getService(refs[0]);
            Method[] methods = comMgr.getClass().getMethods();
            for (int m = 0; m < methods.length; m++) {
                if (methods[m].getName().equalsIgnoreCase("getIp")) {
                    try {
                        Object result = methods[m].invoke(comMgr, new Object[] { "" });
                        if (result != null) localIP = result.toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            tests.add(new Test("eZFusion network checking", "checking framework ip " + localIP, true, ""));
            context.ungetService(refs[0]);
        } catch (Exception e) {
            localIP = "127.0.0.1";
            tests.add(new Test("eZFusion network checking", "checking framework ip " + localIP, false, "Unable to find local ip due to exception: " + e.getLocalizedMessage()));
        }
        String xmlPath = "http://" + localIP + ":8080/ezconfig?agentname=agent@" + localIP + "&configtest=tracking1.xml&testtype=1";
        try {
            Hashtable<String, String> req = new Hashtable<String, String>();
            req.put("topic", "agent");
            req.put("class", "user");
            req.put("to", "local");
            req.put("manager", "admin");
            req.put("action", "deploy");
            req.put("arg", xmlPath);
            if (tryGet("http://" + localIP + ":8080/ezadmin", req)) {
                tests.add(new Test("eZFusion network checking", "local configuration deployment request", true, ""));
            } else {
                tests.add(new Test("eZFusion network checking", "local configuration deployment request", false, "result of tryGet is false"));
            }
        } catch (Exception e) {
            tests.add(new Test("eZFusion network checking", "local configuration deployment request", false, "request failed due to exception: " + e.getLocalizedMessage()));
        }
        try {
            Thread.sleep(45000);
        } catch (Exception e) {
        }
        GlobalConfiguration gConfig = null;
        ConfigurationMgrSrv cfgMgr;
        try {
            refs = null;
            refs = context.getAllServiceReferences(ConfigurationMgrSrv.class.getName(), null);
            if ((refs != null) && (refs.length > 0)) {
                cfgMgr = (ConfigurationMgrSrv) context.getService(refs[0]);
                gConfig = cfgMgr.getGlobalconfig();
                if ((gConfig != null) && (gConfig.xmlFile.equalsIgnoreCase(xmlPath))) {
                    tests.add(new Test("eZFusion network checking", "local configuration deployment received", true, ""));
                } else {
                    tests.add(new Test("eZFusion network checking", "local configuration deployment received", false, "xml file path are different"));
                }
                context.ungetService(refs[0]);
            } else {
                tests.add(new Test("eZFusion network checking", "local configuration deployment received", false, "no reference to access to configuration manager"));
            }
        } catch (Exception e) {
            tests.add(new Test("eZFusion network checking", "local configuration deployment received", false, "unable to access to configuration manager due exception: " + e.getLocalizedMessage()));
        }
        if (gConfig != null) {
            FusionNodeContainerInt container = null;
            try {
                refs = null;
                refs = context.getAllServiceReferences(FusionNodeContainerInt.class.getName(), null);
                Enumeration<String> fNames = gConfig.mapping.keys();
                String fName;
                boolean fDeployed;
                while (fNames.hasMoreElements()) {
                    fName = fNames.nextElement();
                    fDeployed = false;
                    for (int f = 0; f < refs.length; f++) {
                        try {
                            container = (FusionNodeContainerInt) context.getService(refs[f]);
                            if (fName.equalsIgnoreCase(container.getFunctionName())) {
                                fDeployed = true;
                                tests.add(new Test("eZFusion deployment checking", "function " + fName + " is deployed", true, ""));
                            }
                            context.ungetService(refs[f]);
                        } catch (Exception e) {
                        }
                    }
                    if (!fDeployed) tests.add(new Test("eZFusion deployment checking", "function " + fName + " is deployed", false, "fusion node container not found"));
                }
            } catch (Exception e) {
                tests.add(new Test("eZFusion deployment checking", "functions are deployed", false, "cannot check for fusion node containers due to exception: " + e.getLocalizedMessage()));
            }
        }
        try {
            Thread.sleep(20000);
        } catch (Exception e) {
        }
        try {
            Hashtable<String, String> req = new Hashtable<String, String>();
            req.put("topic", "agent");
            req.put("class", "user");
            req.put("to", "local");
            req.put("manager", "admin");
            req.put("action", "stopconfig");
            if (tryGet("http://" + localIP + ":8080/ezadmin", req)) {
                tests.add(new Test("eZFusion network checking", "resources release request", true, ""));
            } else {
                tests.add(new Test("eZFusion network checking", "resources release request", false, "result of tryGet is false"));
            }
        } catch (Exception e) {
            tests.add(new Test("eZFusion network checking", "resources release request", false, "request failed due to exception: " + e.getLocalizedMessage()));
        }
        try {
            Thread.sleep(20000);
        } catch (Exception e) {
        }
        try {
            refs = null;
            refs = context.getAllServiceReferences(FusionNodeContainerInt.class.getName(), null);
            if ((refs != null) && (refs.length > 0)) {
                for (int f = 0; f < refs.length; f++) {
                    tests.add(new Test("eZFusion resources release checking", "containers removed", false, "container found " + refs[f].getProperty("service.name")));
                }
            } else {
                tests.add(new Test("eZFusion resources release checking", "containers removed", true, ""));
            }
        } catch (Exception e) {
            tests.add(new Test("eZFusion resources release checking", "containers removed", false, "cannot check for resources release due to exception: " + e.getLocalizedMessage()));
        }
        return tests;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        Hashtable<String, Vector<Test>> report;
        ReportOut out1;
        report = new Hashtable<String, Vector<Test>>();
        out1 = new ReportOutToFile(org.ezfusion.Activator.installDir + "ezfusion_tests.txt");
        System.out.println("Tests in progress, should take 120 sec...");
        try {
            report.put("Static tests, no configuration running", staticTests());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            report.put("Local configuration deployment", localTests());
        } catch (Exception e) {
            e.printStackTrace();
        }
        out1.updateReport(report);
        out1.closeReport();
        System.out.println("end of tests.");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
