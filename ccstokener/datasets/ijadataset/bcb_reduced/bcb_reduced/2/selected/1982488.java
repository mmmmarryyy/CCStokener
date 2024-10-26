package lokahi.core.controller;

import org.apache.log4j.Logger;
import lokahi.dao.Job;
import lokahi.core.api.jobpool.JobPool;
import lokahi.core.controller.AdminBean;
import lokahi.core.controller.task.SendJobTask;
import lokahi.core.common.exception.UnsupportedParameterException;
import lokahi.messaging.Message;
import lokahi.messaging.MessageFactory;
import lokahi.util.PropertiesFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CheckJvmThread extends Thread {

    public static final Logger logger = Logger.getLogger(CheckJvmThread.class);

    private static final String hostname;

    private static ThreadPoolExecutor tpe;

    private static String jvmHttp;

    private static lokahi.core.api.state.State state;

    private static final String myContext;

    private static final String protocol;

    private static final String statusPage;

    public static final String contentMatch = PropertiesFile.getConstantValue("tmc.thread.contentmatch");

    private static volatile boolean runThread = false;

    private static final AdminBean adminBean;

    private static final int sleeptime = PropertiesFile.getIntValue("tmc.thread.waittime");

    static {
        final int corePoolSize = 5;
        final int maximumPoolSize = 20;
        final long keepAliveTime = 300;
        tpe = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(25));
        state = lokahi.core.api.state.State.getState(0);
        myContext = PropertiesFile.getConstantValue("application.root");
        protocol = PropertiesFile.getConstantValue("tmc.thread.protocol");
        statusPage = PropertiesFile.getConstantValue("tmc.thread.statuspage");
        adminBean = new AdminBean();
        String hostName = "";
        try {
            InetAddress inet = InetAddress.getLocalHost();
            hostName = inet.getHostName();
            if (logger.isDebugEnabled()) {
                logger.debug("hostname=" + hostName);
            }
        } catch (UnknownHostException e) {
            if (logger.isInfoEnabled()) {
                logger.info("UnknownHostException: " + e.getMessage());
            }
        }
        hostname = hostName;
    }

    /**
   * Returns a handle to the main thread. sets the jvmHttp value to the value passed
   */
    public static CheckJvmThread getThreadInstance(String val) {
        jvmHttp = val;
        return new CheckJvmThread();
    }

    /**
   * private constructor. Instantiates the classes and variables used by this thread
   */
    public CheckJvmThread() {
    }

    /**
   * This method is called when the context shuts down. We should perform all the cleanup
   * operations related to the thread here.
   */
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
   * This method returns the value of the variable 'runThread'
   */
    public synchronized boolean runTheThread() {
        if (logger.isDebugEnabled()) {
            logger.debug("runThread=" + runThread);
        }
        return runThread;
    }

    /**
   * This method returns the value of the variable 'runThread'
   */
    public static String isTheThreadRunning(String test) {
        CheckJvmThread thread = new CheckJvmThread();
        if (logger.isDebugEnabled()) {
            logger.debug("runThread=" + thread.runTheThread());
            logger.debug("test=" + test);
        }
        String ret = "FAILED";
        if (thread.runTheThread()) {
            ret = thread.getContentMatch();
        }
        return ret;
    }

    public String getContentMatch() {
        return contentMatch;
    }

    /**
   * This method sets the value of the variable 'runThread' to true. The calling servlet/jsp MUST
   * call the start() method of this class after calling startThread(). The reason start() (actually run..)
   * cannot be called from here is because startThread() is a static method and run() is not.
   */
    public static synchronized void startThread() {
        runThread = true;
        if (logger.isDebugEnabled()) {
            logger.debug("runThread=" + runThread);
        }
    }

    /**
   * This method sets the value of the variable 'runThread' to false
   */
    public static synchronized void stopThread() {
        runThread = false;
        if (logger.isDebugEnabled()) {
            logger.debug("runThread=" + runThread);
        }
    }

    /**
   * This method determines whether or not to run the designated process(e.g. thread) based on whether
   * thread is primary or backup or alternate.
   */
    public void run() {
        while (runTheThread()) {
            if (logger.isDebugEnabled()) {
                logger.debug(" java.lang.Runtime.getRuntime().freeMemory()=" + Runtime.getRuntime().freeMemory());
                logger.debug(" java.lang.Runtime.getRuntime().totalMemory()=" + Runtime.getRuntime().totalMemory());
            }
            if (isPrimary()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(this.getName() + " - Performing my function here.");
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("adminBean.getInstance()=" + adminBean.getInstance());
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("state=" + state);
                }
                doPrimaryFunction();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("About to SLEEP.");
            }
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("sleeptime=" + sleeptime);
                }
                sleep(sleeptime * 1000);
            } catch (InterruptedException e) {
                if (logger.isInfoEnabled()) {
                    logger.info("InterruptedException: " + e.getMessage());
                }
            }
        }
        tpe.shutdown();
        if (logger.isInfoEnabled()) {
            logger.info("CheckJvmThread stopped running");
        }
    }

    public static void iterateJobPools(lokahi.core.api.state.State s) {
        if (s != null) {
            Collection<JobPool> c = null;
            try {
                c = JobPool.getJobPoolsLimited(s);
                if (logger.isDebugEnabled()) {
                    logger.debug("c.size()=" + c.size());
                }
            } catch (SQLException e) {
                if (logger.isInfoEnabled()) {
                    logger.info("Exception: " + e.getMessage());
                }
            }
            if (c != null) {
                for (final JobPool jp : c) {
                }
            }
        }
    }

    public static void doPrimaryFunction() {
        Collection<Job> jobs;
        try {
            jobs = Job.getJobs(adminBean.getInstance(), state);
            if (jobs != null && !jobs.isEmpty()) {
                try {
                    for (final Job j : jobs) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("SENDING j=" + j);
                        }
                        SendJobTask t = new SendJobTask(j);
                        try {
                            tpe.execute(t);
                        } catch (RejectedExecutionException e) {
                            logger.info("Exception: " + e.getLocalizedMessage());
                            logger.error("Exception:", e);
                        }
                        if (logger.isInfoEnabled()) {
                            logger.info("tpe.getTaskCount()=" + tpe.getTaskCount());
                            logger.info("tpe.getTaskCount()=" + tpe.getTaskCount());
                            logger.info("tpe.getActiveCount()=" + tpe.getActiveCount());
                            logger.info("tpe.getCompletedTaskCount()=" + tpe.getCompletedTaskCount());
                            logger.info("tpe.tpe.getLargestPoolSize()=" + tpe.getLargestPoolSize());
                            logger.info("tpe.getQueue().remainingCapacity()=" + tpe.getQueue().remainingCapacity());
                        }
                    }
                } catch (RuntimeException ex) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Exception: " + ex.getMessage());
                        logger.info("Exception: " + ex.getClass());
                        logger.info("Exception: " + ex.getStackTrace());
                    }
                }
            }
        } catch (SQLException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Exception: " + e.getMessage());
            }
        }
    }

    /**
   * This method checks if the machine:jvm supplied is available, by doing a http request.
   *
   * @param pair - machine:jvm whose availability is to be checked
   * @return - boolean true if the machine:jvm pair is available and false if not
   */
    private boolean pingMachine(String pair) {
        boolean ret = false;
        if (pair != null && !"".equals(pair)) {
            String uri = protocol + "://" + pair + myContext + '/' + statusPage;
            if (logger.isDebugEnabled()) {
                logger.debug("uri=" + uri);
            }
            String res = usingSoap(uri);
            if (res.equalsIgnoreCase(contentMatch)) {
                ret = true;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Returning " + ret);
        }
        return ret;
    }

    /**
   * This method determines if this thread must run the desired process. It first checks if the variable 'runThread'
   * is true. If not, returns false. If it is true, then it checks its own status.
   * If it is primary, returns true, if not, it checks if the primary/backup  is available, based on its own status
   * (backup/alternate respectively). It changes its status based on the availability of primary/backup
   *
   * @return - boolean true if the thread should run and false if not.
   */
    private boolean isPrimary() {
        boolean shouldIRun = false;
        if (!runTheThread()) {
            if (logger.isInfoEnabled()) {
                logger.info("I am not even supposed to be running! ");
            }
        } else {
            Hashtable admin_jvms = adminBean.getAllAdminInfo();
            if (admin_jvms == null) admin_jvms = new Hashtable();
            String primaryTomcatWorker = (admin_jvms.get("PRIMARY") != null) ? (String) admin_jvms.get("PRIMARY") : "";
            String backupTomcatWorker = (admin_jvms.get("BACKUP") != null) ? (String) admin_jvms.get("BACKUP") : "";
            String alternateTomcatWorkers = (admin_jvms.get("ALTERNATE") != null) ? (String) admin_jvms.get("ALTERNATE") : "";
            String myTomcatWorker = hostname + ":" + jvmHttp;
            if (logger.isDebugEnabled()) {
                logger.debug("myTomcatWorker=" + myTomcatWorker);
                logger.debug("primaryTomcatWorker=" + primaryTomcatWorker);
                logger.debug("backupTomcatWorker=" + backupTomcatWorker);
                logger.debug("alternateTomcatWorkers=" + alternateTomcatWorkers);
            }
            if (myTomcatWorker.equalsIgnoreCase(primaryTomcatWorker)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Thread is primary");
                }
                shouldIRun = true;
            } else {
                if (myTomcatWorker.equalsIgnoreCase(backupTomcatWorker)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Thread is the backup");
                    }
                    if (pingMachine(primaryTomcatWorker)) {
                        iterateJobPools(state);
                        iterateJobPools(lokahi.core.api.state.State.getState(1));
                    } else {
                        admin_jvms.put("PRIMARY", myTomcatWorker);
                        admin_jvms.put("BACKUP", primaryTomcatWorker);
                        adminBean.changeJvmAdminRole(admin_jvms);
                    }
                } else {
                    boolean amIAlt = false;
                    if (alternateTomcatWorkers != null && !alternateTomcatWorkers.equals("")) {
                        String temp[] = adminBean.split(alternateTomcatWorkers, ",");
                        int t = 0;
                        while (t < temp.length && amIAlt != true) {
                            if (temp[t] != null && temp[t].equalsIgnoreCase(myTomcatWorker)) {
                                amIAlt = true;
                            }
                            t++;
                        }
                    }
                    if (!amIAlt) {
                        if (admin_jvms.get("ALTERNATE") != null) admin_jvms.put("ALTERNATE", (String) admin_jvms.get("ALTERNATE") + "," + myTomcatWorker); else admin_jvms.put("ALTERNATE", myTomcatWorker);
                        adminBean.changeJvmAdminRole(admin_jvms);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Thread is the alt");
                        }
                        if (pingMachine(backupTomcatWorker)) {
                        } else {
                            admin_jvms.put("BACKUP", myTomcatWorker);
                            admin_jvms.put("ALTERNATE", alternateTomcatWorkers + ',' + backupTomcatWorker);
                            adminBean.changeJvmAdminRole(admin_jvms);
                        }
                    }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("runThread=" + this.runTheThread());
            logger.debug("Returning " + shouldIRun);
        }
        return shouldIRun;
    }

    /**
   * This method contacts the supplied machine:jvmagent with the URL supplied and returns the result of
   * the operation
   *
   * @param uri - URL String to hit the target JVM with
   * @return - String containing the result of the operation, directly as received from the target JVM
   */
    private String getResultOfURL(String uri) {
        String ret = "Nothing happened!!";
        try {
            URL url = new URL(uri);
            InputStream is = url.openStream();
            BufferedReader inStream = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = inStream.readLine()) != null) {
                ret = line;
            }
            inStream.close();
            is.close();
        } catch (MalformedURLException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Exception: " + e.getMessage());
            }
            ret = e.getMessage();
        } catch (IOException e) {
            if (logger.isInfoEnabled()) {
                logger.info("IOException: " + e.getMessage());
            }
            ret = e.getMessage();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Returning " + ret);
        }
        return ret;
    }

    private String usingSoap(String uri) {
        String namespace = "TmcController";
        String method = "isTheThreadRunning";
        String ret = "";
        Message m = MessageFactory.getTransport();
        try {
            ret = m.send(new String[] { "TEST" }, false, new String[] { uri, namespace, method });
        } catch (UnsupportedParameterException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Exception: " + e.getMessage());
            }
        }
        return ret;
    }
}
