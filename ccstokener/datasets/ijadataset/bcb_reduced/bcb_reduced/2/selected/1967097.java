package com.bluestone.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import com.bluestone.BaseAction;
import com.bluestone.assertor.IAssertor;
import com.bluestone.context.ConfigContext;
import com.bluestone.context.IContext;
import com.bluestone.scripts.ActionScript;
import com.bluestone.scripts.Function;
import com.bluestone.util.Util;

/**
 * CallFuntionAction can execute a function.
 * @author <a href="mailto:bluesotne.master@gmail.com">daniel.q</a>
 */
public class HttpPostAction extends BaseAction {

    private String encoding = "UTF-8";

    private static boolean threadResult = true;

    private static int threadCount = 0;

    private static int threadCompleteCount = 0;

    int postcount = 0;

    int successcount = 0;

    int failurecount = 0;

    long totaltime = 0;

    long averagetime = 0;

    long averagedata = 0;

    long averagerows = 0;

    long starttime = 0;

    long endtime = 0;

    public HttpPostAction(ActionScript action) {
        super(action);
        threadResult = true;
        threadCount = 0;
        threadCompleteCount = 0;
    }

    synchronized void completeThread(boolean result) {
        threadCompleteCount++;
        if (!result) {
            threadResult = false;
        }
    }

    /**
	 * This action can execute a function.
	 * @return false if the target is not exsit or if the function execute failure.
	 */
    public boolean execute(IContext context) {
        String threads = action.getPara("threadcount");
        if (threads != null && threads.length() > 0) {
            starttime = System.currentTimeMillis();
            ArrayList threadList = new ArrayList();
            threadCount = Integer.parseInt(threads);
            for (int i = 0; i < threadCount; i++) {
                PostThread thread = new PostThread(this, i);
                threadList.add(thread);
                thread.start();
            }
            while (true) {
                if (threadCompleteCount == threadCount) {
                    break;
                }
            }
            endtime = System.currentTimeMillis();
            for (int i = 0; i < threadList.size(); i++) {
                PostThread thread = (PostThread) threadList.get(i);
                postcount = postcount + thread.postcount;
                successcount = successcount + thread.successcount;
                failurecount = failurecount + thread.failurecount;
                averagetime = averagetime + thread.averagetime;
                averagedata = averagedata + thread.averagedatasize;
                averagerows = averagerows + thread.averagerows;
            }
            averagetime = averagetime / threadList.size();
            averagedata = averagedata / threadList.size();
            averagerows = averagerows / threadList.size();
            printme();
            return threadResult;
        }
        String postStr = action.getPara("value");
        if (postStr.startsWith("@")) {
            postStr = Util.getString(postStr);
        }
        postStr = Util.loadFile(postStr);
        postStr = Util.getDynamicString(postStr);
        return singleThread(postStr, 20000, null);
    }

    boolean singleThread(String postStr, long timeout, PostThread thread) {
        long temp = System.currentTimeMillis();
        String target = action.getPara("target");
        if (target == null || action.getPara("value") == null) {
            return false;
        }
        try {
            String urlStr = target;
            if (urlStr.startsWith("@")) {
                urlStr = Util.getString(urlStr);
            }
            Util.getLogger().info("request from bluestone:" + postStr);
            postStr = URLEncoder.encode(postStr, encoding);
            postStr = "xmlin=" + postStr + "&origin=AIVR";
            String response = getHttpPostResponseStr(urlStr, postStr, encoding, timeout);
            if (thread != null) {
                thread.setReceivedData(response.length());
                String rows = Util.getNodeValue(response, "TotalRows");
                if (rows != null && rows.length() > 0) {
                    thread.setReceivedRows(Integer.parseInt(rows));
                }
            }
            Util.getLogger().info("response from app server:" + response);
            String assertorName = action.getPara("assertor");
            if (assertorName != null && assertorName.length() > 0) {
                IAssertor assertor = (IAssertor) ConfigContext.getInstance().getAssertors().get(assertorName);
                assertor.putData(response);
            }
        } catch (Exception me) {
            Util.getLogger().error(me);
            me.printStackTrace();
            Util.getLogger().error("can't get response from " + target);
            return false;
        }
        temp = System.currentTimeMillis() - temp;
        String maxtime = action.getPara("maxtime");
        if (maxtime != null && maxtime.length() > 0) {
            long max = Long.parseLong(maxtime) * 1000;
            if (temp > max) {
                Util.getLogger().error("HttpPost execute time:" + temp + " great than " + max);
                return false;
            } else {
                Util.getLogger().info("HttpPost execute time:" + temp + " less than " + max);
            }
        }
        return true;
    }

    /**
	 * Make HTTP/POST connection to a network URL and return the response
	 * content as a string.
	 * 
	 * @param urlStr
	 *            the target URL.
	 * @param postStr
	 *            the posted request data string.
	 * @param encoding
	 *            the expected response content encoding, e.g. "UTF-8", and use
	 *            the default encoding if it is null.
	 * @return the response content string.
	 */
    public static String getHttpPostResponseStr(String urlStr, String postStr, String encoding, long timeout) throws Exception, IOException {
        URL url = new URL(urlStr);
        StringBuilder sb = null;
        HttpURLConnection conn = null;
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout((int) timeout);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            if (encoding == null) {
                out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream()), true);
            } else {
                out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), encoding), true);
            }
            out.println(postStr);
            out.flush();
            if (encoding == null) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream(), encoding));
            }
            sb = new StringBuilder(1024);
            readThruBuffer(in, sb);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
            if (conn != null) conn.disconnect();
        }
        return sb.toString();
    }

    private static void readThruBuffer(BufferedReader r, StringBuilder sb) throws IOException {
        char[] buffer = new char[1024];
        int c;
        while ((c = r.read(buffer)) > 0) {
            sb.append(buffer, 0, c);
        }
    }

    void printme() {
        Util.getLogger().error("summary:");
        Util.getLogger().error(" start:" + new Date(starttime));
        Util.getLogger().error("success counts:" + successcount);
        Util.getLogger().error("failure counts:" + failurecount);
        Util.getLogger().error("total counts:" + postcount);
        Util.getLogger().error("average time:" + averagetime);
        Util.getLogger().error("average data:" + averagedata);
        Util.getLogger().error("average rows:" + averagerows);
        Util.getLogger().error("end:" + new Date(endtime));
    }
}

class PostThread extends Thread {

    HttpPostAction action;

    int num = 0;

    HashMap<String, ArrayList> randomParas = new HashMap<String, ArrayList>();

    long starttime = 0;

    long endtime = 0;

    int postcount = 0;

    int successcount = 0;

    int failurecount = 0;

    long totaltime = 0;

    long averagetime = 0;

    long averagedatasize = 0;

    long datasize = 0;

    long datarows = 0;

    long averagerows = 0;

    public PostThread(HttpPostAction action, int num) {
        this.action = action;
        this.num = num;
    }

    public synchronized void setReceivedData(int size) {
        datasize = datasize + size;
    }

    public synchronized void setReceivedRows(int rows) {
        datarows = datarows + rows;
    }

    public String setRandom(String str, String para) {
        String result = str;
        String[] tokens1 = para.split("[,]");
        for (int i = 0; i < tokens1.length; i++) {
            String k = tokens1[i];
            ArrayList ids = randomParas.get(k);
            if (ids.size() > 0) {
                int j = (int) (Math.random() * (ids.size() - 1));
                String id = (String) ids.get(j);
                result = result.replace("{" + k + "}", id);
            }
        }
        return result;
    }

    public void run() {
        starttime = System.currentTimeMillis();
        String postStr = action.getPara("value");
        if (postStr.startsWith("@")) {
            postStr = Util.getString(postStr);
        }
        postStr = Util.loadFile(postStr);
        postStr = Util.getDynamicString(postStr);
        String randomPara = action.getPara("randompara");
        String randomStr = "";
        if (randomPara != null) {
            randomStr = randomPara;
            String[] tokens = randomStr.split("[,=]");
            for (int i = 0; i < tokens.length; i = i + 2) {
                String k = tokens[i];
                String v = tokens[i + 1];
                v = Util.getString(v);
                k = Util.getString(k);
                ArrayList<String> ids = new ArrayList<String>();
                String data = Util.loadFile(v);
                StringTokenizer tokensPara = new StringTokenizer(data, "\r\n");
                while (tokensPara.hasMoreTokens()) {
                    ids.add(tokensPara.nextToken());
                }
                randomParas.put(k, ids);
                if (randomPara.length() > 0) {
                    randomPara = randomPara + "," + k;
                } else {
                    randomPara = randomPara + k;
                }
            }
        }
        long threadTime = Long.parseLong(action.getPara("threadtime")) * 1000;
        Util.getLogger().error("thread" + num + " start");
        while (true) {
            long tmp = System.currentTimeMillis();
            if (randomPara != null && randomPara.length() > 0) {
                randomStr = setRandom(postStr, randomPara);
            } else {
                randomStr = postStr;
            }
            boolean flag = action.singleThread(randomStr, threadTime, this);
            tmp = System.currentTimeMillis() - tmp;
            totaltime = totaltime + tmp;
            postcount++;
            if (flag) {
                successcount++;
            } else {
                failurecount++;
            }
            if ((System.currentTimeMillis() - starttime) > threadTime) {
                action.completeThread(flag);
                break;
            }
        }
        averagetime = totaltime / postcount;
        averagedatasize = datasize / postcount;
        averagerows = datarows / postcount;
        endtime = System.currentTimeMillis();
    }

    void printme() {
        Util.getLogger().error("thread" + num + "  summary:");
        Util.getLogger().error("start:" + new Date(starttime));
        Util.getLogger().error("success counts:" + successcount);
        Util.getLogger().error("failure counts:" + failurecount);
        Util.getLogger().error("total counts:" + postcount);
        Util.getLogger().error("total time:" + totaltime);
        Util.getLogger().error("average time:" + averagetime);
        Util.getLogger().error("average datasize:" + averagedatasize);
        Util.getLogger().error("average datarows:" + averagerows);
        Util.getLogger().error("thread" + num + " end:" + new Date(endtime));
    }
}
