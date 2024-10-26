package co.edu.unal.ungrid.grid.master;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.Vector;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.transaction.Transaction;
import co.edu.unal.space.util.SpaceProxy;

public class SpaceJobRunner {

    private class GenerateThread extends Thread {

        public GenerateThread() {
            setDaemon(true);
        }

        public void run() {
            if (m_bRegisterJob) {
                regStart(System.currentTimeMillis());
            }
            if (m_bDebug) System.out.println("GenerateThread::run(): m_job=" + m_job.getClass().getName());
            m_job.generateTasks(m_proxy, m_txn);
        }
    }

    private class CollectThread extends Thread {

        public CollectThread() {
            setDaemon(true);
        }

        public void run() {
            if (m_bDebug) System.out.println("CollectThread::run(): m_job=" + m_job.getClass().getName());
            m_job.collectResults(m_proxy, m_txn);
            if (m_bRegisterJob) {
                regEnd(System.currentTimeMillis());
            }
        }
    }

    public SpaceJobRunner(SpaceProxy proxy, AbstractGridJob job) {
        assert proxy != null;
        assert job != null;
        m_proxy = proxy;
        m_job = job;
        m_lJobLease = DEF_JOB_LEASE;
        Transaction.Created c = getTransaction();
        if (c != null) {
            m_txn = c.transaction;
            m_lease = c.lease;
        }
        setRegisterJob(true);
    }

    public void setLease(long l) {
        m_lJobLease = l;
    }

    public void setRegisterJob(boolean b) {
        m_bRegisterJob = b;
    }

    private Transaction.Created getTransaction() {
        Transaction.Created c = null;
        if (m_bUseTxn) {
            try {
                c = m_proxy.getTransaction(m_lJobLease);
            } catch (LeaseDeniedException exc) {
                System.out.println("SpaceJobRunner::getTransaction(): exc=" + exc);
            } catch (RemoteException exc) {
                System.out.println("SpaceJobRunner::getTransaction(): exc=" + exc);
            }
        }
        return c;
    }

    public void suspend(SpaceProxy proxy, Transaction txn) {
        m_job.suspend(proxy, txn);
    }

    public void resume(SpaceProxy proxy, Transaction txn) {
        m_job.resume(proxy, txn);
    }

    public void cancel(SpaceProxy proxy, Transaction txn) {
        m_job.cancel(proxy, txn);
    }

    public ObjectInputStream sendObject(String sMethod, Serializable obj) {
        String sServlet = System.getProperty("servlet.jstat");
        if (sServlet != null) {
            try {
                URL url = new URL(sServlet);
                URLConnection conn = url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
                out.writeObject(sMethod);
                out.writeObject(obj);
                out.flush();
                out.close();
                return new ObjectInputStream(conn.getInputStream());
            } catch (Exception exc) {
                System.out.println("Error on SpaceJobRunner::sendObjectPOST() --> " + exc);
            }
        } else {
        }
        return null;
    }

    public void regStart(long t) {
        Vector<Object> v = new Vector<Object>(2);
        v.add(m_job.getId());
        v.add(new Long(t));
        ObjectInputStream ois = sendObject(JSTM, v);
        if (ois != null) {
            try {
                Integer r = (Integer) ois.readObject();
                if (m_bDebug) System.out.println("SpaceJobRunner::regStart(): r=" + r.intValue());
            } catch (Exception exc) {
                System.out.println("SpaceJobRunner::regStart(): exc=" + exc);
            }
        }
    }

    public void regEnd(long t) {
        Vector<Object> v = new Vector<Object>(2);
        v.add(m_job.getId());
        v.add(new Long(t));
        ObjectInputStream ois = sendObject(JETM, v);
        if (ois != null) {
            try {
                Integer r = (Integer) ois.readObject();
                if (m_bDebug) System.out.println("SpaceJobRunner::regEnd(): r=" + r.intValue());
            } catch (Exception exc) {
                System.out.println("SpaceJobRunner::regEnd(): exc=" + exc);
            }
        }
    }

    public void regOutPath(String sPath) {
        Vector<Object> v = new Vector<Object>(2);
        v.add(m_job.getId());
        v.add(sPath);
        ObjectInputStream ois = sendObject(JPTH, v);
        if (ois != null) {
            try {
                Integer r = (Integer) ois.readObject();
                if (m_bDebug) System.out.println("SpaceJobRunner::regOutPath(): r=" + r.intValue());
            } catch (Exception exc) {
                System.out.println("SpaceJobRunner::regOutPath(): exc=" + exc);
            }
        }
    }

    protected String osFormat(String sPath) {
        String fs = System.getProperty("file.separator");
        sPath = sPath.replace(UNX_SEP_CHAR, fs.charAt(0));
        sPath = sPath.replace(WIN_SEP_CHAR, fs.charAt(0));
        return sPath;
    }

    protected boolean createTargetPath() {
        String sTgtPath = System.getProperty("target.path");
        if (sTgtPath == null) {
            sTgtPath = "target";
        }
        String sOutPath = m_job.getOutputPath();
        if (sOutPath != null) {
            if (!sOutPath.endsWith("/") && !sOutPath.endsWith("\\")) {
                sOutPath += "/";
            }
            File f = new File(osFormat(sTgtPath + "/" + sOutPath + "/."));
            if (m_bDebug) System.out.println("SpaceJobRunner::createPath(): path=" + f.getAbsolutePath());
            String sPath = f.getParent();
            if (sPath != null) {
                File fpf = new File(sPath);
                if (fpf.exists()) {
                    return true;
                }
                if (fpf.mkdirs()) {
                    regOutPath(fpf.getAbsolutePath());
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public void run() {
        if (m_bDebug) System.out.println("SpaceJobRunner::run(txn=" + (m_txn != null ? "yes" : "no") + "): starting job " + m_job.getId());
        if (createTargetPath()) {
            GenerateThread gt = new GenerateThread();
            gt.start();
            CollectThread ct = new CollectThread();
            ct.start();
            try {
                ct.join();
            } catch (Exception exc) {
                System.err.println("SpaceJobRunner::run(): exc=" + exc);
            } finally {
                if (m_bDebug) System.out.println("SpaceJobRunner: finishing job " + m_job.getId() + "\n");
            }
        } else {
            System.err.println("SpaceJobRunner::run(): createPath() failed, path=" + m_job.getOutputPath());
        }
    }

    public static void runJob(final SpaceProxy proxy, final AbstractGridJob job) {
        SpaceJobRunner jr = new SpaceJobRunner(proxy, job);
        jr.run();
    }

    protected SpaceProxy m_proxy;

    protected AbstractGridJob m_job;

    protected Transaction m_txn;

    protected Lease m_lease;

    protected long m_lJobLease;

    private boolean m_bRegisterJob;

    private static final long DEF_JOB_LEASE = 0L;

    public static final String JSTM = "jst";

    public static final String JETM = "jet";

    public static final String JPTH = "jop";

    public static final String WIN_SEP = "\\";

    public static final char WIN_SEP_CHAR = WIN_SEP.charAt(0);

    public static final String UNX_SEP = "/";

    public static final char UNX_SEP_CHAR = UNX_SEP.charAt(0);

    private static final boolean m_bUseTxn = !true;

    private static final boolean m_bDebug = !true;
}
