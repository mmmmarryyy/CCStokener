package hu.sztaki.lpds.submitter.grids;

import dci.data.Item;
import dci.data.Item.*;
import eu.edges_grid.wsdl._3gbridge.G3BridgeSubmitter_Service;
import eu.edges_grid.wsdl._3gbridge.GridAlgList;
import eu.edges_grid.wsdl._3gbridge.JobIDList;
import eu.edges_grid.wsdl._3gbridge.JobList;
import eu.edges_grid.wsdl._3gbridge.JobOutput;
import eu.edges_grid.wsdl._3gbridge.JobStatus;
import eu.edges_grid.wsdl._3gbridge.LogicalFile;
import eu.edges_grid.wsdl._3gbridge.OutputList;
import eu.edges_grid.wsdl._3gbridge.StatusList;
import hu.sztaki.lpds.dcibridge.config.Conf;
import hu.sztaki.lpds.dcibridge.service.Base;
import hu.sztaki.lpds.dcibridge.service.Job;
import hu.sztaki.lpds.dcibridge.service.LB;
import hu.sztaki.lpds.dcibridge.util.BinaryHandler;
import hu.sztaki.lpds.dcibridge.util.InputHandler;
import hu.sztaki.lpds.dcibridge.util.OutputHandler;
import hu.sztaki.lpds.dcibridge.util.XMLHandler;
import hu.sztaki.lpds.submitter.grids.boinc.BridgeHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import javax.xml.namespace.QName;
import org.ggf.schemas.bes._2006._08.bes_factory.ActivityStateEnumeration;
import org.ggf.schemas.jsdl._2005._11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl._2005._11.jsdl_posix.POSIXApplicationType;

/**
 * @author krisztian karoczkai
 */
public class Grid_boinc extends Middleware {

    protected Hashtable<String, BridgeHandler> clients = new Hashtable<String, BridgeHandler>();

    protected static final int maxJob = 2000;

    /**
 * Constructor, Loading of the configuration
 */
    public Grid_boinc() {
        THIS_MIDDLEWARE = Base.MIDDLEWARE_BOINC;
        threadID++;
        setName("guse/dci-bridge:Middleware handler(boinc) - " + threadID);
    }

    protected G3BridgeSubmitter_Service create3GBridgeClient(URL pWSDL) throws Exception {
        G3BridgeSubmitter_Service res;
        QName name = new QName("http://www.edges-grid.eu/wsdl/3GBridge.wsdl", "G3BridgeSubmitter");
        res = new G3BridgeSubmitter_Service(pWSDL, name);
        return res;
    }

    @Override
    public void setConfiguration() throws Exception {
        Base.writeLogg(THIS_MIDDLEWARE, new LB(THIS_MIDDLEWARE + "plugon configuring"));
        for (Item it : Conf.getMiddleware(THIS_MIDDLEWARE).getItem()) {
            Item grp = Conf.getItem(THIS_MIDDLEWARE, it.getName());
            Boinc t = grp.getBoinc();
            if (clients.get(grp.getName()) == null) {
                Base.writeLogg(THIS_MIDDLEWARE, new LB("new " + THIS_MIDDLEWARE + " grid:" + grp.getName()));
                BridgeHandler tmp = new BridgeHandler();
                URL wsdl = new URL(t.getWsdl());
                tmp.setClient(create3GBridgeClient(wsdl));
                clients.put(grp.getName(), tmp);
                Base.writeLogg(THIS_MIDDLEWARE, new LB("call service:" + t.getWsdl()));
                GridAlgList boincJobs = tmp.getClient().getG3BridgeSubmitter().getGridAlgs(t.getId());
                Boinc.Job job;
                for (String jobName : boincJobs.getGridalgs()) {
                    Base.writeLogg(THIS_MIDDLEWARE, new LB(THIS_MIDDLEWARE + "grid:" + grp.getName() + "/" + jobName));
                    job = new Boinc.Job();
                    job.setName(jobName);
                    job.setState(false);
                    t.getJob().add(job);
                }
                Base.writeLogg(THIS_MIDDLEWARE, new LB("end of call service:"));
            } else {
                Base.writeLogg(THIS_MIDDLEWARE, new LB("refresh " + THIS_MIDDLEWARE + " grid:" + grp.getName()));
                BridgeHandler tmp = clients.get(grp.getName());
                try {
                    URL wsdl = new URL(t.getWsdl());
                    tmp.setClient(create3GBridgeClient(wsdl));
                    GridAlgList boincJobs = tmp.getClient().getG3BridgeSubmitter().getGridAlgs(t.getId());
                    Boinc.Job job;
                    for (String jobName : boincJobs.getGridalgs()) {
                        Base.writeLogg(THIS_MIDDLEWARE, new LB("Boinc grid:" + grp.getName() + "/" + jobName));
                        if (!Conf.isJobInBoinc(t, jobName)) {
                            Base.writeLogg(THIS_MIDDLEWARE, new LB("newJOB:" + t.getId() + "/" + jobName));
                            job = new Boinc.Job();
                            job.setName(jobName);
                            job.setState(false);
                            t.getJob().add(job);
                        }
                    }
                    Base.writeLogg(THIS_MIDDLEWARE, new LB("end of call service:"));
                } catch (Exception e) {
                    Base.writeLogg(THIS_MIDDLEWARE, new LB(e));
                }
            }
            System.gc();
        }
    }

    @Override
    public void addJob(Job pJob) throws Exception {
        Base.writeLogg(THIS_MIDDLEWARE, new LB("new job:" + pJob.getId()));
        String grid = pJob.getConfiguredResource().getVo();
        clients.get(grid).getSubmitQueue().add(pJob);
        pJob.setResource(grid);
        pJob.setMiddlewareId(Base.dcis[8]);
    }

    @Override
    protected synchronized void abort(Job pJob) throws Exception {
        String grid = pJob.getConfiguredResource().getVo();
        BridgeHandler bridge = clients.get(grid);
        List<Job> tmpSubmit = bridge.getSubmitQueue();
        Job t;
        boolean next = true;
        if (clients.get(grid).getSubmitQueue().remove(pJob)) return;
        List<String> tmpStatus = bridge.getStatusQueue().getJobid();
        next = true;
        String bridgeID = pJob.getMiddlewareId();
        for (int i = 0; i < tmpStatus.size() && next; i++) {
            if (bridgeID.equals(tmpStatus.get(i))) {
                bridge.getStatusQueue().getJobid().remove(i);
                bridge.getDeleteQueue().getJobid().add(bridgeID);
                next = false;
            }
        }
    }

    @Override
    protected void submit(Job pJob) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void getOutputs(Job pJob) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void getStatus(Job pJob) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void run() {
        Base.writeLogg(THIS_MIDDLEWARE, new LB("starting thread"));
        Enumeration<String> enm;
        BridgeHandler bridge;
        String bridgeID;
        boolean event;
        while (true) {
            enm = clients.keys();
            event = false;
            while (enm.hasMoreElements()) {
                bridgeID = enm.nextElement();
                if (Conf.getItem(THIS_MIDDLEWARE, bridgeID).isEnabled()) {
                    Base.writeLogg(THIS_MIDDLEWARE, new LB("event:" + bridgeID));
                    bridge = clients.get(bridgeID);
                    try {
                        if (bridge.getSubmitQueue().size() > 0) {
                            submit(bridge);
                            event = true;
                        }
                        if (bridge.isEnabledNextEvent(LAST_ACTIVATE_TIMESTAMP)) {
                            getStatus(bridge);
                            download(bridge);
                            delete(bridge);
                            bridge.setEventTimeStamp();
                            event = true;
                        }
                    } catch (Exception e) {
                        Base.writeLogg(THIS_MIDDLEWARE, new LB(e));
                        try {
                            setConfiguration();
                        } catch (Exception e0) {
                            Base.writeLogg(THIS_MIDDLEWARE, new LB(e0));
                        }
                    }
                }
            }
            if (!event) {
                Base.writeLogg(THIS_MIDDLEWARE, new LB("sleep:" + LAST_ACTIVATE_TIMESTAMP));
                try {
                    sleep(LAST_ACTIVATE_TIMESTAMP);
                } catch (InterruptedException e) {
                    Base.writeLogg(THIS_MIDDLEWARE, new LB(e));
                }
            }
        }
    }

    /**
 * Deleting of jobs
 * @param pClient Bridge Client instance
 */
    private void delete(BridgeHandler pClinet) {
        JobIDList tmpList;
        int count = 0;
        String bid;
        while (pClinet.getDeleteQueue().getJobid().size() > 0) {
            count = 0;
            tmpList = new JobIDList();
            while (pClinet.getDeleteQueue().getJobid().size() > 0 && count < maxJob) {
                count++;
                bid = pClinet.getDeleteQueue().getJobid().remove(0);
                tmpList.getJobid().add(bid);
                pClinet.getMapping().remove(bid);
            }
            pClinet.getClient().getG3BridgeSubmitter().delete(tmpList);
        }
    }

    /**
 * Query of Boinc states from the 3GBridge
 * @param pClient Bridge Client peldany
 */
    private synchronized void getStatus(BridgeHandler pClient) {
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
            Base.writeLogg(THIS_MIDDLEWARE, new LB("event:getStatus(" + tmpList.getJobid().size() + ")"));
            StatusList tmp = pClient.getClient().getG3BridgeSubmitter().getStatus(tmpList);
            JobStatus st;
            String bridgeID, guseID;
            for (int i = 0; i < tmp.getStatus().size(); i++) {
                bridgeID = tmpList.getJobid().get(i);
                guseID = pClient.getMapping().get(bridgeID);
                st = tmp.getStatus().get(i);
                Base.writeJobLogg(guseID, LB.INFO, "job status:" + st.value());
                if (Base.getI().getJob(guseID).getFlag() == Middleware.ABORT) {
                    Base.getI().getJob(guseID).setPubStatus(ActivityStateEnumeration.CANCELLED);
                    pClient.getDeleteQueue().getJobid().add(bridgeID);
                    Base.getI().removeJob(guseID);
                    System.out.println("ABORT:" + bridgeID + " >>> " + guseID);
                } else if (JobStatus.ERROR.name().equals(st.value())) {
                    Base.getI().getJob(guseID).setPubStatus(ActivityStateEnumeration.FAILED);
                    pClient.getDeleteQueue().getJobid().add(bridgeID);
                    Base.getI().removeJob(guseID);
                } else if (JobStatus.FINISHED.name().equals(st.value())) {
                    pClient.getDownloadQueue().getJobid().add(bridgeID);
                } else pClient.getStatusQueue().getJobid().add(bridgeID);
            }
        }
    }

    /**
 * Submission of Boinc jobs
 * @param pClient Bridge Client instances
 */
    private synchronized void submit(BridgeHandler pClient) {
        List<Job> tmp;
        Job item;
        JobList jl;
        int count;
        while (pClient.getSubmitQueue().size() > 0) {
            count = 0;
            jl = new JobList();
            tmp = new ArrayList<Job>();
            while (pClient.getSubmitQueue().size() > 0 && count < maxJob) {
                count++;
                item = pClient.getSubmitQueue().remove(0);
                if (item.getFlag() != Middleware.ABORT) {
                    tmp.add(item);
                    try {
                        eu.edges_grid.wsdl._3gbridge.Job boincjob = create3GBridgeJob(item);
                        Base.writeJobLogg(item.getId(), LB.INFO, "alg=" + boincjob.getAlg());
                        Base.writeJobLogg(item.getId(), LB.INFO, "grid=" + boincjob.getGrid());
                        for (LogicalFile bt : boincjob.getInputs()) Base.writeJobLogg(item.getId(), LB.INFO, "input:" + bt.getLogicalName() + "=>" + bt.getURL());
                        for (String bt : boincjob.getOutputs()) Base.writeJobLogg(item.getId(), LB.INFO, "output:" + bt);
                        jl.getJob().add(boincjob);
                    } catch (Exception e) {
                        Base.getI().getJob(item.getId()).setPubStatus(ActivityStateEnumeration.FAILED);
                        Base.getI().getJob(item.getId()).setResource("input error");
                        Base.writeJobLogg(item, e, "input error");
                    }
                }
            }
            Base.writeLogg(THIS_MIDDLEWARE, new LB("event:submit(" + jl.getJob().size() + ")"));
            JobIDList res = pClient.getClient().getG3BridgeSubmitter().submit(jl);
            Job jc;
            String tt;
            for (int i = 0; i < res.getJobid().size(); i++) {
                tt = res.getJobid().get(i);
                jc = tmp.get(i);
                pClient.getMapping().put(tt, jc.getId());
                Base.writeJobLogg(jc.getId(), LB.INFO, "3gBridgeID", tt);
                Base.getI().getJob(jc.getId()).setPubStatus(ActivityStateEnumeration.RUNNING);
                pClient.getStatusQueue().getJobid().add(tt);
            }
        }
    }

    /**
 * Creating Job instances for submission in the 3GBridge
 * @param pClient Bridge Client instance
 */
    protected eu.edges_grid.wsdl._3gbridge.Job create3GBridgeJob(Job pJC) {
        eu.edges_grid.wsdl._3gbridge.Job job = new eu.edges_grid.wsdl._3gbridge.Job();
        job.setAlg(pJC.getJSDL().getJobDescription().getJobIdentification().getJobName());
        job.setGrid(Conf.getItem(Base.MIDDLEWARE_BOINC, pJC.getConfiguredResource().getVo()).getBoinc().getId());
        POSIXApplicationType pType = XMLHandler.getData(pJC.getJSDL().getJobDescription().getApplication().getAny(), POSIXApplicationType.class);
        String params = "";
        for (String s : BinaryHandler.getCommandLineParameter(pType)) params = params.concat(" " + s);
        job.setArgs(params);
        job.setTag("DCI-Bridge:" + pJC.getId());
        List<DataStagingType> ports = InputHandler.getInputs(pJC);
        for (DataStagingType jsdlPort : ports) {
            LogicalFile input = new LogicalFile();
            input.setLogicalName(jsdlPort.getFileName());
            input.setURL(jsdlPort.getSource().getURI());
            job.getInputs().add(input);
        }
        ports = OutputHandler.getOutputs(pJC.getJSDL());
        for (DataStagingType jsdlPort : ports) {
            if (jsdlPort.getName() != null) job.getOutputs().add(jsdlPort.getFileName());
        }
        return job;
    }

    /**
 * The downloading of the outputs of the jobs has been terminated
 * @param pClient Bridge Client instance
 */
    private void download(BridgeHandler pClient) {
        long itemCount = pClient.getDownloadQueue().getJobid().size();
        OutputList outputs = pClient.getClient().getG3BridgeSubmitter().getOutput(pClient.getDownloadQueue());
        JobOutput t;
        String guseID, path, bid;
        Base.writeLogg(THIS_MIDDLEWARE, new LB("event:download(" + outputs.getOutput().size() + ")"));
        for (int i = 0; i < outputs.getOutput().size(); i++) {
            bid = pClient.getDownloadQueue().getJobid().remove(0);
            guseID = pClient.getMapping().get(bid);
            t = outputs.getOutput().get(i);
            try {
                for (LogicalFile f : t.getOutput()) {
                    path = Base.getI().getJobDirectory(guseID) + "outputs/";
                    downloadFile(path + f.getLogicalName(), f.getURL());
                }
                Base.getI().getJob(guseID).setStatus(ActivityStateEnumeration.FINISHED);
                Base.getI().finishJob(Base.getI().getJob(guseID));
            } catch (Exception e0) {
                Base.writeJobLogg(Base.getI().getJob(guseID), e0, "output download error");
                e0.printStackTrace();
                try {
                    Base.getI().getJob(guseID).setStatus(ActivityStateEnumeration.FAILED);
                } catch (Exception e1) {
                }
            }
            pClient.getDeleteQueue().getJobid().add(bid);
        }
    }

    /**
 * Downloading of output file
 * @param pFile cel file
 * @param pURL forras url
 * @throws java.lang.Exception refering communication/file writing errors
 */
    private static void downloadFile(String pFile, String pURL) throws Exception {
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
}
