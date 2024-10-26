package hu.sztaki.lpds.submitter.grids.boinc;

import eu.edges_grid.wsdl._3gbridge.Job;
import eu.edges_grid.wsdl._3gbridge.JobIDList;
import eu.edges_grid.wsdl._3gbridge.JobList;
import eu.edges_grid.wsdl._3gbridge.JobOutput;
import eu.edges_grid.wsdl._3gbridge.JobStatus;
import eu.edges_grid.wsdl._3gbridge.LogicalFile;
import eu.edges_grid.wsdl._3gbridge.OutputList;
import eu.edges_grid.wsdl._3gbridge.StatusList;
import hu.sztaki.lpds.information.com.ServiceType;
import hu.sztaki.lpds.information.local.InformationBase;
import hu.sztaki.lpds.information.local.PropertyLoader;
import hu.sztaki.lpds.submitter.admin.Monitor;
import hu.sztaki.lpds.submitter.admin.ServiceCallBean;
import hu.sztaki.lpds.submitter.admin.StringBean;
import hu.sztaki.lpds.submitter.grids.confighandler.JobConfig;
import hu.sztaki.lpds.submitter.service.valery.Base;
import hu.sztaki.lpds.wfs.utils.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * @author smith@sztaki.hu
 */
public class BoincDataBaseHandler extends Thread {

    private static BoincDataBaseHandler instance = new BoincDataBaseHandler();

    private static final String propertyPrefix = "wsdl.3GBridge.";

    private Hashtable<String, BridgeHandler> clients = new Hashtable<String, BridgeHandler>();

    private String serviceURL;

    private int slp = 60000;

    private int maxJob = 300;

    private long tim0;

    private Object stat;

    private String gridName;

    public static BoincDataBaseHandler getI() {
        return instance;
    }

    public Hashtable<String, BridgeHandler> getClients() {
        return clients;
    }

    public void propertyTrigger() {
        List<String> grids = PropertyLoader.getInstance().getPropertiesKey(propertyPrefix);
        for (String t : grids) {
            if (clients.get(t) == null) {
                try {
                    BridgeHandler tmp = new BridgeHandler();
                    URL wsdl = new URL(PropertyLoader.getInstance().getProperty(t));
                    tmp.setWsdl(wsdl);
                    clients.put(t.substring(propertyPrefix.length()), tmp);
                    System.out.println("NEW BRIDGE:" + t.substring(propertyPrefix.length()));
                } catch (Exception e) {
                    System.out.println("wsdl hiba:" + t);
                    e.printStackTrace();
                }
            }
        }
        ServiceType st = InformationBase.getI().getService("submitter", "submitter", new Hashtable(), new Vector());
        serviceURL = st.getSecureServiceUrl();
        slp = Integer.parseInt(PropertyLoader.getInstance().getProperty("boinc.getstatus.sleep"));
    }

    public BoincDataBaseHandler() {
        propertyTrigger();
        start();
    }

    private void doing(Object pObject) {
        tim0 = System.currentTimeMillis();
        stat = pObject;
    }

    private void monitor(String pQueue, byte pType) {
        tim0 = System.currentTimeMillis() - tim0;
        if (pType == Monitor.CALL) ((ServiceCallBean) stat).setMs(tim0);
        Monitor.getI().add(pQueue, pType, stat);
    }

    @Override
    public void run() {
        Enumeration<String> enm;
        int itemCount;
        BridgeHandler bridge;
        String guseID;
        String path;
        while (true) {
            enm = clients.keys();
            while (enm.hasMoreElements()) {
                gridName = enm.nextElement();
                bridge = clients.get(gridName);
                Monitor.getI().add("bridge-" + gridName, Monitor.QUEUE, bridge.getQueueSizes());
                try {
                    itemCount = bridge.getStatusQueue().getJobid().size();
                    if (itemCount > 0) getStatus(bridge);
                    itemCount = bridge.getDownloadQueue().getJobid().size();
                    if (itemCount > 0) download(bridge);
                    itemCount = bridge.getSubmitQueue().size();
                    if (itemCount > 0) submit(bridge);
                    itemCount = bridge.getDeleteQueue().getJobid().size();
                    if (itemCount > 0) delete(bridge);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                sleep(slp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void actionJobSubmit(String pPath, JobConfig pJC, String sJOBID) {
        String grid = pJC.getJobPropertyValue("grid");
        BridgeHandler bridge = clients.get(grid);
        bridge.getSubmitQueue().add(pJC);
        writelog(pJC.getId(), "insert boinc queue");
        writelog(pJC.getId(), "jobid:" + pJC);
    }

    public synchronized void actionJobAbort(String sJOBID) {
        String grid = Base.getI().getRunner(sJOBID).getJobConfig().getJobPropertyValue("grid");
        BridgeHandler bridge = clients.get(grid);
        List<JobConfig> tmpSubmit = bridge.getSubmitQueue();
        JobConfig t;
        boolean next = true;
        for (int i = 0; i < tmpSubmit.size() && next; i++) {
            t = tmpSubmit.get(i);
            if (sJOBID.equals(t.getId())) {
                clients.get(grid).getSubmitQueue().remove(i);
                next = false;
            }
        }
        List<String> tmpStatus = bridge.getStatusQueue().getJobid();
        next = true;
        String bridgeID = Base.getI().getRunner(sJOBID).getParameter();
        System.out.println("ABORT:" + sJOBID + "->" + grid + "(" + bridgeID + ")");
        for (int i = 0; i < tmpStatus.size() && next; i++) {
            if (bridgeID.equals(tmpStatus.get(i))) {
                bridge.getStatusQueue().getJobid().remove(i);
                bridge.getDeleteQueue().getJobid().add(bridgeID);
                next = false;
            }
        }
    }

    private void download(BridgeHandler pClinet) {
        long itemCount = pClinet.getDownloadQueue().getJobid().size();
        doing(new ServiceCallBean("3gBridge download", "(count:" + itemCount + ")", (byte) 0));
        OutputList outputs = pClinet.getClient().getG3BridgeSubmitter().getOutput(pClinet.getDownloadQueue());
        monitor("system", Monitor.CALL);
        JobOutput t;
        String guseID, path, bid;
        for (int i = 0; i < outputs.getOutput().size(); i++) {
            bid = pClinet.getDownloadQueue().getJobid().remove(0);
            guseID = pClinet.getMapping().get(bid);
            t = outputs.getOutput().get(i);
            for (LogicalFile f : t.getOutput()) {
                writelog(guseID, "download(" + f.getLogicalName() + "):" + f.getURL());
                try {
                    path = PropertyLoader.getInstance().getProperty("prefix.dir") + "submitter/" + guseID + "/outputs/";
                    download(path + f.getLogicalName(), f.getURL());
                } catch (Exception e0) {
                    e0.printStackTrace();
                    try {
                        Base.getI().getRunner(guseID).getMidlevareObject().actionSetJobStatus(Status.ERROR);
                    } catch (Exception e1) {
                    }
                }
            }
            Base.getI().getRunner(guseID).getMidlevareObject().actionSetJobStatus(Status.FINISH);
            pClinet.getDeleteQueue().getJobid().add(bid);
        }
    }

    private void delete(BridgeHandler pClient) {
        JobIDList tmpList;
        int count = 0;
        String bid;
        while (pClient.getDeleteQueue().getJobid().size() > 0) {
            count = 0;
            tmpList = new JobIDList();
            while (pClient.getDeleteQueue().getJobid().size() > 0 && count < maxJob) {
                count++;
                bid = pClient.getDeleteQueue().getJobid().remove(0);
                tmpList.getJobid().add(bid);
                writelog(pClient.getMapping().get(bid), "delete");
                pClient.getMapping().remove(bid);
            }
            doing(new ServiceCallBean("3gBridge " + gridName + " delete", "count:" + tmpList.getJobid().size(), (byte) 0));
            pClient.getClient().getG3BridgeSubmitter().delete(tmpList);
            monitor("system", Monitor.CALL);
        }
    }

    private void getStatus(BridgeHandler pClient) {
        JobIDList tmpList;
        int count;
        int maxXount = pClient.getStatusQueue().getJobid().size();
        while (maxXount > 0) {
            tmpList = new JobIDList();
            count = 0;
            while (maxXount > 0 && count < maxJob) {
                count++;
                maxXount--;
                tmpList.getJobid().add(pClient.getStatusQueue().getJobid().remove(0));
            }
            doing(new ServiceCallBean("3gBridge " + gridName + " getStatus", "count:" + tmpList.getJobid().size(), (byte) 0));
            StatusList tmp = pClient.getClient().getG3BridgeSubmitter().getStatus(tmpList);
            monitor("system", Monitor.CALL);
            JobStatus st;
            String bridgeID, guseID;
            for (int i = 0; i < tmp.getStatus().size(); i++) {
                bridgeID = tmpList.getJobid().get(i);
                guseID = pClient.getMapping().get(bridgeID);
                st = tmp.getStatus().get(i);
                writelog(guseID, "getStatus:" + st.value());
                if (JobStatus.ERROR.name().equals(st.value())) {
                    try {
                        Base.getI().getRunner(guseID).getMidlevareObject().actionSetJobStatus(Status.ERROR);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    pClient.getDeleteQueue().getJobid().add(bridgeID);
                } else if (JobStatus.FINISHED.name().equals(st.value())) {
                    pClient.getDownloadQueue().getJobid().add(bridgeID);
                } else {
                    try {
                        Base.getI().getRunner(guseID).getMidlevareObject().actionSetJobStatus(Status.RUNNING);
                        pClient.getStatusQueue().getJobid().add(bridgeID);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void submit(BridgeHandler pClient) {
        Monitor.getI().add("bridge-" + gridName, Monitor.QUEUE, pClient.getQueueSizes());
        List<JobConfig> tmp;
        JobConfig item;
        JobList jl;
        int count;
        while (pClient.getSubmitQueue().size() > 0) {
            count = 0;
            jl = new JobList();
            tmp = new ArrayList<JobConfig>();
            while (pClient.getSubmitQueue().size() > 0 && count < maxJob) {
                count++;
                item = pClient.getSubmitQueue().remove(0);
                tmp.add(item);
                try {
                    jl.getJob().add(create3GBridgeJob(item));
                } catch (Exception e) {
                    Base.getI().getRunner(item.getId()).getMidlevareObject().actionSetJobStatus(7);
                    Base.getI().getRunner(item.getId()).getMidlevareObject().actionSetJobResource("input error");
                    e.printStackTrace();
                }
            }
            doing(new ServiceCallBean("3gBridge " + gridName + " submit", "count:" + jl.getJob().size(), (byte) 0));
            JobIDList res = pClient.getClient().getG3BridgeSubmitter().submit(jl);
            monitor("system", Monitor.CALL);
            JobConfig jc;
            String tt;
            for (int i = 0; i < res.getJobid().size(); i++) {
                tt = res.getJobid().get(i);
                jc = tmp.get(i);
                writelog(jc.getId(), "bridge id:" + tt);
                pClient.getMapping().put(tt, jc.getId());
                Base.getI().getRunner(jc.getId()).getMidlevareObject().actionSetJobStatus(2);
                Base.getI().getRunner(jc.getId()).setParameter(tt);
                pClient.getStatusQueue().getJobid().add(tt);
            }
        }
        Monitor.getI().add("bridge-" + gridName, Monitor.QUEUE, pClient.getQueueSizes());
    }

    private Job create3GBridgeJob(JobConfig pJC) throws Exception {
        Job job = new Job();
        job.setAlg(pJC.getJobPropertyValue("resource"));
        job.setGrid(pJC.getJobPropertyValue("grid"));
        job.setArgs(pJC.getJobPropertyValue("params"));
        job.setTag(pJC.getJobData().getWorkflowRuntimeID());
        writelog(pJC.getId(), "alg:" + job.getAlg());
        writelog(pJC.getId(), "resource:" + job.getGrid());
        writelog(pJC.getId(), "args:" + job.getArgs());
        writelog(pJC.getId(), "tag:" + job.getTag());
        Enumeration<String> enm = pJC.getInputs();
        String key;
        String internalFileName;
        while (enm.hasMoreElements()) {
            key = (String) enm.nextElement();
            internalFileName = pJC.getInputPropertyValue(key, "intname");
            LogicalFile input = new LogicalFile();
            input.setLogicalName(internalFileName);
            input.setURL(serviceURL + "/3gbridge/" + pJC.getId() + "/" + internalFileName);
            job.getInputs().add(input);
            writelog(pJC.getId(), "input(" + input.getLogicalName() + "):" + input.getURL());
            String path = PropertyLoader.getInstance().getProperty("prefix.dir") + "submitter/" + pJC.getId() + "/" + input.getLogicalName();
            File f = new File(path);
            if (!f.exists()) throw new Exception("not exist:" + path);
        }
        enm = pJC.getOutputs();
        while (enm.hasMoreElements()) {
            key = enm.nextElement();
            internalFileName = pJC.getOutputPropertyValue(key, "intname");
            job.getOutputs().add(internalFileName);
            writelog(pJC.getId(), "output:" + internalFileName);
        }
        return job;
    }

    private static void download(String pFile, String pURL) throws Exception {
        URL url = new URL(pURL);
        ;
        URLConnection urlConn;
        InputStream is;
        FileOutputStream fos;
        File f = new File(pFile);
        f.createNewFile();
        urlConn = url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setUseCaches(false);
        byte[] b = new byte[5120];
        int ln = 0;
        is = urlConn.getInputStream();
        fos = new FileOutputStream(f);
        while ((ln = is.read(b)) > 0) {
            fos.write(b, 0, ln);
            fos.flush();
        }
        is.close();
        fos.close();
    }

    public String getGridname() {
        return gridName;
    }

    public Object getStat() {
        return stat;
    }

    private void writelog(String jobID, String pValue) {
        String path = PropertyLoader.getInstance().getProperty("prefix.dir") + "submitter/" + jobID + "/outputs/gridnfo.log";
        File f = new File(path);
        try {
            if (!f.exists()) f.createNewFile();
            FileWriter fw = new FileWriter(f, true);
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            fw.write(ts.toString() + " -" + pValue + "\n");
            fw.close();
        } catch (Exception e) {
        }
    }

    private void servicelog(BridgeHandler pClient, String pQueue, String pGUSEID, String pBridgeID) {
        String path = PropertyLoader.getInstance().getProperty("prefix.dir") + "submitter/bridgelog.xml";
        File f = new File(path);
        try {
            if (!f.exists()) {
                f.createNewFile();
                FileWriter fw = new FileWriter(f);
                fw.write("<guse-submitter>");
                fw.close();
            }
            FileWriter fw = new FileWriter(f, true);
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            fw.write("\t<queue name=\"" + pQueue + "\">\n");
            fw.write("\t\t<bridgeid>" + pBridgeID + "</bridgeid>\n");
            fw.write("\t\t<guseid>" + pGUSEID + "</guseid>\n");
            fw.write("\t\t<mapping>" + (pClient.getMapping().get(pBridgeID) != null) + "</mapping>\n");
            fw.write("\t\t<time>" + ts.toString() + "</time>\n");
            fw.write("\t</queue>\n");
            fw.close();
        } catch (Exception e) {
        }
    }
}
