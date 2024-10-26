package org.jcvi.vics.shared.utils;

import junit.framework.TestCase;
import org.jcvi.vics.model.TimebasedIdentifierGenerator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Oct 12, 2007
 * Time: 9:29:26 AM
 *
 */
public class IdGeneratorTest extends TestCase {

    private String httpRequest = "http://guid:8080/guid/GuidClientServer?Request=GET&Size=";

    private int liveThreads;

    private int blockSize = 100;

    private int threads = 10;

    private long timebasedBlockSize = 300;

    private int timebasedThreads = 10;

    private SortedSet<Long> ids;

    private HashSet<Long> hibIds;

    private List<String> errorMsgList;

    protected void setUp() {
        ids = new TreeSet<Long>();
        hibIds = new HashSet<Long>();
        liveThreads = 0;
        errorMsgList = new ArrayList<String>();
    }

    public void testGuidClientServer() throws InterruptedException {
        List<Thread> retrievers = new ArrayList<Thread>();
        for (int i = 0; i < threads; i++) {
            retrievers.add(new Thread(new IdRetriever(this), String.valueOf(i)));
        }
        launchAndWaitForCompletion(retrievers);
        verifyGuidClientServerIdsContent();
    }

    public void testTimebasedIdentifierGenerator() throws InterruptedException {
        List<Thread> generators = new ArrayList<Thread>();
        for (int i = 0; i < timebasedThreads; i++) {
            generators.add(new Thread(new IdGenerator(this), String.valueOf(i)));
        }
        launchAndWaitForCompletion(generators);
    }

    private void launchAndWaitForCompletion(List<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) {
            liveThreads++;
            thread.start();
        }
        while (liveThreads > 0) {
            if (liveThreads < timebasedThreads) {
                synchronized (this) {
                    wait();
                    if (!errorMsgList.isEmpty()) {
                        fail(errorMsgList.toString());
                    }
                }
            }
        }
    }

    private void verifyGuidClientServerIdsContent() {
        System.out.println("ids=" + ids);
        Long previousId = ids.first();
        int i = 0;
        for (Long id : ids) {
            if (i != 0) {
                long difference = id.longValue() - previousId.longValue();
                if (difference < blockSize) {
                    fail("Overlap occurred between " + id + " and " + previousId + " difference =" + difference);
                }
            }
            previousId = id;
            i++;
        }
    }

    private class IdRetriever implements Runnable {

        private IdGeneratorTest test;

        public IdRetriever(IdGeneratorTest test) {
            this.test = test;
        }

        public void run() {
            long id = 0;
            try {
                URL url = new URL(httpRequest + blockSize);
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                try {
                    String str = in.readLine();
                    if (str == null) {
                        throw new IllegalStateException("Parsing error");
                    }
                    if (str.matches(".*SUCCESS.*")) {
                        str = in.readLine();
                        if (str.matches(".*Start:.*")) {
                            str = in.readLine();
                            id = Long.parseLong(str);
                        } else {
                            throw new IllegalStateException("Invalid response for blocksize(" + blockSize + "):" + str);
                        }
                    } else {
                        throw new IllegalStateException("Invalid response for blocksize(" + blockSize + "):" + str);
                    }
                } finally {
                    in.close();
                }
                if (id <= 0) throw new IllegalStateException("Invalid GUID start value " + id);
                synchronized (ids) {
                    boolean absent = ids.add(new Long(id));
                    if (!absent) {
                        logErrorMessage(id + " already exists for thread " + Thread.currentThread().getName());
                    }
                }
            } catch (Exception e) {
                logErrorMessage("Unexpected IdGenerator thread failure" + e.getMessage());
                e.printStackTrace();
            } finally {
                synchronized (test) {
                    test.liveThreads--;
                    test.notify();
                }
            }
        }

        private void logErrorMessage(String errorMsg) {
            synchronized (errorMsgList) {
                errorMsgList.add(errorMsg);
            }
        }
    }

    private class IdGenerator implements Runnable {

        private IdGeneratorTest test;

        public IdGenerator(IdGeneratorTest test) {
            this.test = test;
        }

        public void run() {
            try {
                List<Long> ids = TimebasedIdentifierGenerator.generateIdList(timebasedBlockSize);
                System.out.println("Generated " + ids.size() + " ids: " + ids.get(0) + " -" + ids.get(ids.size() - 1));
                synchronized (hibIds) {
                    for (Long id : ids) {
                        boolean absent = hibIds.add(id);
                        if (!absent) {
                            logErrorMessage(id + " is a duplicate - thread " + Thread.currentThread().getName());
                        }
                    }
                }
            } catch (Exception e) {
                logErrorMessage("Unexpected IdGenerator thread failure" + e.getMessage());
                e.printStackTrace();
            } finally {
                synchronized (test) {
                    test.liveThreads--;
                    test.notify();
                }
            }
        }

        private void logErrorMessage(String errorMsg) {
            synchronized (errorMsgList) {
                errorMsgList.add(errorMsg);
            }
        }
    }
}
