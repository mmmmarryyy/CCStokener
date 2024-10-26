package samples.testcase;

import java.net.*;
import java.io.*;
import java.util.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import org.opendte.node.*;
import org.opendte.controller.*;
import org.opendte.stats.*;

public class HTTPTest extends DefaultTestcase implements Runnable {

    private int iClNumberOfThreads, iClNumberOfCycles;

    private URL urlClDestinationURL;

    private Thread clThread[];

    private int iActiveCount;

    private Properties clProperties;

    private Counter cntErrors;

    private Average avgCalls;

    private TimedCounter calls;

    public HTTPTest() {
        System.out.println("Classloader of HTTPTest: " + Thread.currentThread().getContextClassLoader());
        URLClassLoader urlcl = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        URL urls[] = urlcl.getURLs();
        for (int i = 0; i < urls.length; i++) System.out.println("--> " + urls[i]);
        System.out.println("HTTPTest created");
        cntErrors = Node.getStatisticsCollector().getCounter("errors");
        avgCalls = Node.getStatisticsCollector().getAverage("requests");
        calls = (TimedCounter) Node.getStatisticsCollector().getCollector("calls", TimedCounter.class);
    }

    public void tcInit(java.util.Properties props) {
        try {
            clProperties = props;
            iClNumberOfThreads = Integer.parseInt(clProperties.getProperty("HTTP.Threads", "1"));
            iClNumberOfCycles = Integer.parseInt(clProperties.getProperty("HTTP.Cycles", "1"));
            urlClDestinationURL = new URL(clProperties.getProperty("HTTP.Destination", "http://www.yahoo.com"));
            clThread = new Thread[iClNumberOfThreads];
            for (int i = 0; i < iClNumberOfThreads; i++) clThread[i] = new Thread(this);
            setState(NodeState.INITIALIZED);
            System.out.println("HTTPTest initialized");
        } catch (Exception e) {
            e.printStackTrace();
            setState(NodeState.FAILED);
        }
    }

    public void tcStart() {
        calls.start();
        setState(NodeState.RUNNING);
        System.out.println("HTTPTest started");
        for (int i = 0; i < iClNumberOfThreads; i++) {
            clThread[i].start();
        }
        Thread t = new Thread() {

            public void run() {
                for (int i = 0; i < iClNumberOfThreads; i++) {
                    try {
                        clThread[i].join();
                    } catch (InterruptedException iex) {
                        iex.printStackTrace();
                    }
                }
                calls.stop();
                setState(NodeState.FINISHED);
            }
        };
        t.start();
    }

    public void tcStop() {
        setState(NodeState.STOPPED);
        for (int i = 0; i < iClNumberOfThreads; i++) {
            clThread[i] = null;
        }
        System.out.println("HTTPTest stopped");
    }

    public NodeState getNodeState() {
        long total = cntErrors.getCount() + avgCalls.getCount();
        String info = total + " calls, " + cntErrors.getCount() + " errors";
        clNodeState.setInfo(info);
        return clNodeState;
    }

    public void run() {
        for (int i = 0; i < iClNumberOfCycles; i++) {
            try {
                long lStartTime = System.currentTimeMillis();
                InputStream in = urlClDestinationURL.openStream();
                byte buf[] = new byte[1024];
                int num;
                while ((num = in.read(buf)) > 0) ;
                in.close();
                long lStopTime = System.currentTimeMillis();
                Node.getLogger().write((lStopTime - lStartTime) + " ms");
                avgCalls.update(lStopTime - lStartTime);
                System.out.print("*");
                System.out.flush();
                calls.update();
            } catch (Exception e) {
                cntErrors.update();
                System.out.print("X");
                System.out.flush();
            }
        }
    }
}
