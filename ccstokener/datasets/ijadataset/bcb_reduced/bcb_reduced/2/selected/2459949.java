package rbe;

import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.Date;
import java.util.Vector;
import rbe.util.Debug;
import rbe.util.StringPattern;
import rbe.util.StrStrPattern;
import rbe.util.CharStrPattern;
import rbe.util.CharSetStrPattern;

public class EB extends Thread {

    public static volatile boolean terminate = false;

    int[][] transProb;

    EBTransition[][] trans;

    EBTransition curTrans;

    int curState;

    String nextReq;

    String html;

    public String prevHTML;

    int maxTrans;

    String name;

    byte[] buffer = new byte[4096];

    int cid;

    String sessionID;

    int shopID;

    String fname = null;

    String lname = null;

    public RBE rbe;

    long usmd;

    boolean toHome;

    boolean stagger = true;

    public boolean waitKey = true;

    public Random rand = new Random();

    public double tt_scale = 1.0;

    public static int DEBUG = 0;

    public static final int NO_TRANS = 0;

    public static final int MIN_PROB = 1;

    public static final int MAX_PROB = 9999;

    public static final int ID_UNKNOWN = -1;

    private final StrStrPattern imgPat = new StrStrPattern("<IMG");

    private final StrStrPattern inputPat = new StrStrPattern("<INPUT TYPE=\"IMAGE\"");

    private final StrStrPattern srcPat = new StrStrPattern("SRC=\"");

    private final CharStrPattern quotePat = new CharStrPattern('\"');

    public EB(RBE rbe, int[][] prob, EBTransition[][] trans, int max, String name) {
        int i, j;
        int s;
        int prev;
        s = prob.length;
        Debug.info(s > 0, "No states in prob.");
        Debug.info(trans.length == s, "Number of states in prob (" + s + ") does not equal number of states in trans (" + trans.length + ")");
        for (j = 0; j < s; j++) {
            Debug.info(trans[j].length == s, "Transition matrix is not square.");
            Debug.info(prob[j].length == s, "Transition matrix is not square.");
            prev = 0;
            for (i = 0; i < s; i++) {
                if (prob[j][i] == NO_TRANS) {
                    Debug.info(trans[j][i] == null, "Transition method specified " + "for impossible transition." + i + ", " + j + " " + trans[j][i]);
                } else {
                    Debug.info(prob[j][i] <= MAX_PROB, "Transition probability for prob[" + j + "][" + i + "] (" + prob[j][i] + ") is larger than " + MAX_PROB);
                    Debug.info(prob[j][i] >= MIN_PROB, "Transition probability for prob[" + j + "][" + i + "] (" + prob[j][i] + ") is less than " + MIN_PROB);
                    Debug.info(trans[j][i] != null, "No transition method for possible transition [" + j + "][" + i + "]");
                    Debug.info(prob[j][i] > prev, "Transition [" + j + "][" + i + "] has probability (" + prob[j][i] + " not greater than previous " + "probability (" + prev + ")");
                    prev = prob[j][i];
                }
            }
            Debug.info(prev == MAX_PROB, "Final probability for state [" + j + "] ( " + prev + ") is not " + MAX_PROB);
        }
        this.rbe = rbe;
        this.transProb = prob;
        this.trans = trans;
        this.name = name;
        maxTrans = max;
        initialize();
    }

    public final int states() {
        return (transProb.length);
    }

    public void initialize() {
        curState = 0;
        nextReq = null;
        html = null;
        prevHTML = null;
        cid = ID_UNKNOWN;
        sessionID = null;
        shopID = ID_UNKNOWN;
        usmd = System.currentTimeMillis() + usmd();
        fname = null;
        lname = null;
    }

    public void run() {
        long wirt_t1;
        long wirt_t2;
        long wirt;
        long tt = 0L;
        boolean sign = true;
        wirt_t1 = System.currentTimeMillis();
        if (DEBUG > 0) {
            System.out.println("usmd " + usmd);
        }
        try {
            while ((maxTrans == -1) || (maxTrans > 0)) {
                if (terminate) {
                    System.out.println("EB " + name + "commiting suicide!");
                    return;
                }
                if (nextReq != null) {
                    if (toHome) {
                        RBE.CompleteSession++;
                        if (RBE.PrintControl) System.out.println(name + " completed user session.");
                        initialize();
                        continue;
                    }
                    if (nextReq.equals("")) {
                        rbe.stats.error("Restarting new user session due to error.", " <???>");
                        RBE.CompleteSession++;
                        RBE.errorSession++;
                        if (RBE.PrintControl) System.out.println(name + " completed user session.");
                        initialize();
                        continue;
                    }
                    if (nextReq.startsWith("file:")) {
                        int q = nextReq.indexOf('?');
                        if (q == -1) {
                            q = nextReq.length();
                        }
                        nextReq = nextReq.substring(0, q);
                    }
                    URL httpReq = new URL(nextReq);
                    if (DEBUG > 0) {
                        System.out.println("" + name + "Making request.");
                    }
                    wirt_t1 = System.currentTimeMillis();
                    sign = getHTML(httpReq);
                    if (sign == false) {
                        RBE.CompleteSession++;
                        RBE.errorSession++;
                        initialize();
                        continue;
                    }
                    if (DEBUG > 0) {
                        System.out.println("" + name + "Received HTML.");
                    }
                    wirt_t2 = System.currentTimeMillis();
                    rbe.stats.interaction(curState, wirt_t1, wirt_t2, tt);
                    if (DEBUG > 2) {
                        System.out.println("Post process: " + curTrans);
                    }
                    curTrans.postProcess(this, html);
                } else {
                    html = null;
                    wirt_t2 = wirt_t1;
                }
                nextState();
                if (nextReq != null) {
                    tt = thinkTime();
                    wirt_t1 = wirt_t2 + tt;
                    if (terminate) {
                        System.out.println("EB " + name + "commiting suicide!");
                        return;
                    }
                    try {
                        if (waitKey) {
                            rbe.getKey();
                        } else {
                            sleep(tt);
                        }
                    } catch (InterruptedException inte) {
                        System.out.println("EB " + name + " Caught an interrupted exception!");
                        return;
                    }
                    if (maxTrans > 0) maxTrans--;
                } else System.out.println("ERROR: nextReq == null!");
            }
        } catch (MalformedURLException murl) {
            murl.printStackTrace();
            return;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
    }

    boolean getHTML(URL url) {
        html = "";
        int r;
        BufferedInputStream in = null;
        BufferedInputStream imgIn = null;
        boolean retry;
        Vector imageRd = new Vector(0);
        do {
            retry = false;
            try {
                in = new BufferedInputStream(url.openStream(), 4096);
            } catch (IOException ioe) {
                rbe.stats.error("Unable to open URL.", url.toExternalForm());
                ioe.printStackTrace();
                retry = true;
                continue;
            }
            try {
                while ((r = in.read(buffer, 0, buffer.length)) != -1) {
                    if (r > 0) {
                        html = html + new String(buffer, 0, r);
                    }
                }
            } catch (IOException ioe) {
                rbe.stats.error("Unable to read HTML from URL.", url.toExternalForm());
                retry = true;
                continue;
            }
            if (retry) {
                try {
                    if (waitKey) {
                        rbe.getKey();
                    } else {
                        sleep(1000L);
                    }
                } catch (InterruptedException inte) {
                    System.out.println("In getHTML, caught interrupted exception!");
                    return true;
                }
            }
        } while (retry);
        try {
            in.close();
        } catch (IOException ioe) {
            rbe.stats.error("Unable to close URL.", url.toExternalForm());
        }
        if (DEBUG > 0) {
        }
        if (DEBUG > 10) {
            System.out.println(html);
        }
        int cur = 0;
        if (!RBE.getImage) return true;
        findImg(html, url, imgPat, srcPat, quotePat, imageRd);
        findImg(html, url, inputPat, srcPat, quotePat, imageRd);
        if (DEBUG > 2) {
            System.out.println("Found " + imageRd.size() + " images.");
        }
        while (imageRd.size() > 0) {
            int max = imageRd.size();
            int min = Math.max(max - rbe.maxImageRd, 0);
            int i;
            try {
                for (i = min; i < max; i++) {
                    ImageReader rd = (ImageReader) imageRd.elementAt(i);
                    if (!rd.readImage()) {
                        if (DEBUG > 2) {
                            System.out.println("Read " + rd.tot + " bytes from " + rd.imgURLStr);
                        }
                        imageRd.removeElementAt(i);
                        i--;
                        max--;
                    }
                }
            } catch (InterruptedException inte) {
                System.out.println("In getHTML, caught interrupted exception!");
                return true;
            }
        }
        return true;
    }

    private void findImg(String html, URL url, StringPattern imgPat, StringPattern srcPat, StringPattern quotePat, Vector imageRd) {
        int cur = 0;
        while ((cur = imgPat.find(html, cur)) > -1) {
            cur = srcPat.find(html, imgPat.end() + 1);
            quotePat.find(html, srcPat.end() + 1);
            String imageURLString = html.substring(srcPat.end() + 1, quotePat.start());
            if (DEBUG > 2) {
                System.out.println("Found image " + imageURLString + " " + name);
            }
            imageRd.addElement(new ImageReader(rbe, url, imageURLString, buffer));
            cur = quotePat.start() + 1;
        }
    }

    long thinkTime() {
        if (stagger) {
            long r = rbe.nextInt(rand, 20000) + 100;
            stagger = false;
            if (DEBUG > 0) {
                System.out.println("Think time staggering to " + r + "ms.");
            }
            return ((long) (r * tt_scale));
        } else {
            long r = rbe.negExp(rand, 7000L, 0.36788, 70000L, 4.54e-5, 7000.0);
            r = (long) (tt_scale * r);
            if (DEBUG > 0) {
            }
            return (r);
        }
    }

    long usmd() {
        return (rbe.negExp(rand, 0L, 1.0, 3600000L, 0.0183156, 900000.0));
    }

    void nextState() {
        int i = nextInt(MAX_PROB - MIN_PROB + 1) + MIN_PROB;
        int j;
        for (j = 0; j < transProb[curState].length; j++) {
            if (DEBUG > 2) {
                System.out.print(" " + j + ", " + transProb[curState][j]);
            }
            if (transProb[curState][j] >= i) {
                rbe.stats.transition(curState, j);
                curTrans = trans[curState][j];
                nextReq = curTrans.request(this, html);
                toHome = trans[curState][j].toHome();
                if (DEBUG > 2) {
                    System.out.println(name + " from " + curState + " to " + j + " Rand Value " + i + " via:\n     " + nextReq);
                }
                curState = j;
                return;
            }
        }
        Debug.fail("Should not be here.");
    }

    public int nextInt(int range) {
        int i = Math.abs(rand.nextInt());
        return (i % (range));
    }

    String addIDs(String i) {
        if (sessionID != null) {
            i = rbe.addSession(i, rbe.field_sessionID, "" + sessionID);
        }
        if (cid != ID_UNKNOWN) {
            i = rbe.addField(i, rbe.field_cid, "" + cid);
        }
        if (shopID != ID_UNKNOWN) {
            i = rbe.addField(i, rbe.field_shopID, "" + shopID);
        }
        return (i);
    }

    public int findID(String html, StrStrPattern tag) {
        int id;
        int i = tag.find(html);
        if (i == -1) {
            return (EB.ID_UNKNOWN);
        }
        i = i + tag.length();
        int j = CharSetStrPattern.digit.find(html.substring(i));
        if (j == -1) {
            return (EB.ID_UNKNOWN);
        }
        j = j + i;
        int k = CharSetStrPattern.notDigit.find(html.substring(j));
        if (k == -1) {
            k = html.length();
        } else {
            k = k + j;
        }
        id = Integer.parseInt(html.substring(j, k));
        return (id);
    }

    public String findSessionID(String html, StrStrPattern tag, StrStrPattern etag) {
        int id;
        int i = tag.find(html);
        if (i == -1) {
            return (null);
        }
        i = i + tag.length();
        int j = etag.find(html, i);
        if (j == -1) {
            return (null);
        }
        return (html.substring(i, j));
    }
}
