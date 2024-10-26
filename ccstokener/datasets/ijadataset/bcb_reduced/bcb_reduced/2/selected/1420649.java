package djudge.judge.interfaces;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import utils.FileTools;
import utils.HtmlTools;
import utils.XmlTools;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import djudge.dservice.DServiceTask;
import djudge.judge.CheckParams;
import djudge.judge.Judge;
import djudge.judge.JudgeTaskDescription;
import djudge.judge.JudgeTaskResult;
import djudge.judge.ProblemDescription;
import djudge.judge.executor.ExecutorLimits;

public class Judge2AppEngineLink extends Thread implements JudgeLinkInterface {

    private static final Logger log = Logger.getLogger(Judge2AppEngineLink.class);

    private String rootUrl = "http://localhost:8080/";

    private String logDir = "appengine-local";

    private boolean isActive = true;

    private int judgeLinkId = 0;

    private JudgeLinkCallbackInterface callback = new JudgeLinkCallbackInterface() {

        @Override
        public void reportConnectionLost(int judgeId, String error) {
        }

        @Override
        public void reportConnectionRecovered(int judgeId, String error) {
        }

        @Override
        public void reportError(int judgeId, String error) {
        }

        @Override
        public void reportSubmissionReceived(int judgeId, DServiceTask submissionsId) {
        }

        @Override
        public void reportSubmissionReportSent(int judgeId, JudgeTaskResult submissionsId) {
        }

        @Override
        public void reportSubmissionJudged(int judgeId, JudgeTaskResult res) {
        }
    };

    private String fetchXml() {
        String content = null;
        URLConnection connection = null;
        try {
            connection = new URL(rootUrl + "djudge?action=get").openConnection();
            Scanner scanner;
            scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            content = scanner.next();
            callback.reportConnectionRecovered(judgeLinkId, "");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            log.error("fechXML", e);
            callback.reportError(judgeLinkId, e.getMessage());
        } catch (ConnectException ce) {
            callback.reportConnectionLost(judgeLinkId, ce.getMessage());
        } catch (IOException e1) {
            e1.printStackTrace();
            callback.reportError(judgeLinkId, e1.getMessage());
        }
        return content;
    }

    @SuppressWarnings("deprecation")
    private boolean postXml(Document xml, String id) {
        try {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            DataInputStream input;
            url = new URL(rootUrl + "djudge");
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            printout = new DataOutputStream(urlConn.getOutputStream());
            String content = "action=post&id=" + id + "&xml=" + URLEncoder.encode(XmlTools.formatDoc(xml), "UTF-8");
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            FileTools.writeFileContent("./dumps/sources/judge/" + logDir + "/" + id + ".xml", XmlTools.formatDoc(xml));
            input = new DataInputStream(urlConn.getInputStream());
            while (null != input.readLine()) ;
            input.close();
            callback.reportConnectionRecovered(judgeLinkId, "");
            return true;
        } catch (MalformedURLException me) {
            System.err.println("MalformedURLException: " + me);
            me.printStackTrace();
            log.error("fechXML", me);
            callback.reportError(judgeLinkId, me.getMessage());
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
            ioe.printStackTrace();
            callback.reportConnectionLost(judgeLinkId, ioe.getMessage());
        }
        return false;
    }

    public void run() {
        while (true) {
            while (!isActive) {
                try {
                    sleep(100);
                } catch (InterruptedException e1) {
                }
            }
            String content = fetchXml();
            System.out.println(content);
            if (null != content && content.length() > 100) {
                try {
                    Document doc = XmlTools.getDocumentFromString(content);
                    Element elem = doc.getDocumentElement();
                    String id = elem.getAttribute("id");
                    String problemId = elem.getAttribute("problem_id");
                    String contestId = elem.getAttribute("contest_id");
                    String timeLimit = elem.getAttribute("time_limit");
                    String memoryLimit = elem.getAttribute("memory_limit");
                    String languageId = elem.getAttribute("language_id");
                    String firstTestOmly = elem.getAttribute("first_test_only");
                    String sc = elem.getAttribute("source_code");
                    String sourceCode = StringEscapeUtils.unescapeXml(sc).replace((char) 234, '\n');
                    FileTools.writeFileContent("./dumps/sources/judge/" + logDir + "/" + id + ".txt", sourceCode);
                    System.out.println(contestId);
                    System.out.println(problemId);
                    System.out.println(languageId);
                    System.out.println(timeLimit);
                    System.out.println(memoryLimit);
                    System.out.println(firstTestOmly);
                    System.out.println(id);
                    System.out.println(sourceCode);
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("id", "" + id);
                    map.put("contest", contestId);
                    map.put("problem", problemId);
                    map.put("language", languageId);
                    map.put("source", sourceCode);
                    map.put("clientData", id);
                    DServiceTask task = new DServiceTask(map);
                    CheckParams params = new CheckParams();
                    params.setFlagFirstTestOnly(Boolean.parseBoolean(firstTestOmly));
                    Integer tl = Integer.parseInt(timeLimit);
                    Integer ml = Integer.parseInt(memoryLimit);
                    params.setLimits(new ExecutorLimits(tl, ml));
                    callback.reportSubmissionReceived(judgeLinkId, task);
                    JudgeTaskResult res = Judge.judgeTask(new JudgeTaskDescription(task), params);
                    callback.reportSubmissionJudged(judgeLinkId, res);
                    FileTools.writeFileContent("./dumps/sources/judge/" + logDir + "/" + id + "-report.html", HtmlTools.problemToHtml(res.res, new ProblemDescription(task.getContest(), task.getProblem())));
                    if (postXml(res.res.getXML(), id)) {
                        res.submittedTime = new Date();
                        callback.reportSubmissionReportSent(judgeLinkId, res);
                    }
                } catch (Exception e) {
                    callback.reportError(judgeLinkId, e.getMessage());
                    e.printStackTrace();
                    log.error("judge", e);
                }
            } else {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public static void main(String[] args) {
        new Judge2AppEngineLink().start();
    }

    @Override
    public void startLink() {
        isActive = true;
        if (!isAlive()) start();
    }

    @Override
    public void stopLink() {
        isActive = false;
    }

    @Override
    public void initLink(String[] params, JudgeLinkCallbackInterface judgeLinkCallbackInterface, int id) {
        rootUrl = params[0];
        logDir = params[1];
        callback = judgeLinkCallbackInterface;
        judgeLinkId = id;
    }

    @Override
    public boolean getRunning() {
        return isActive;
    }
}
