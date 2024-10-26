package com.defaultcompany.activities.hudson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * 허드슨의 Remote API에 대한 인터페이스를 제공하는 클라이언트 입니다.<br/><br/>
 * 
 * Listing Job : http://<hudson service url:port>/api/xml <br/>
 * Viewing Job : http://<hudson service url:port>/job/<my job name>/api/xml <br/>
 * Running Job : http://<hudson service url:port>/job/<my job name>/build/api/xml <br/>
 * 
 * 
 * 
 * @author alexk
 * {@link http://wiki.hudson-ci.org/display/HUDSON/Remote+access+API}
 * {@link http://wiki.hudson-ci.org/display/HUDSON/Authenticating+scripted+clients}
 */
public class HudsonClient {

    private static String PROTOCOL_PREFIX = "http://";

    DefaultHttpClient httpClient = new DefaultHttpClient();

    String user;

    String password;

    String hostname;

    int port;

    String servicePath;

    SAXBuilder builder = new SAXBuilder();

    public static class Job {

        private String name;

        private String url;

        private String color;

        public Job() {
        }

        public Job(String name, String url, String color) {
            this.name = name;
            this.url = url;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    public static class Build {

        public static final String SUCCESS = "SUCCESS";

        public static final String FAILURE = "FAILURE";

        boolean isBuilding;

        long duration;

        String fullDisplayName;

        String id;

        int number;

        String result;

        String timestemp;

        String url;

        public boolean isBuilding() {
            return isBuilding;
        }

        public void setBuilding(boolean isBuilding) {
            this.isBuilding = isBuilding;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getFullDisplayName() {
            return fullDisplayName;
        }

        public void setFullDisplayName(String fullDisplayName) {
            this.fullDisplayName = fullDisplayName;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getTimestemp() {
            return timestemp;
        }

        public void setTimestemp(String timestemp) {
            this.timestemp = timestemp;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public HudsonClient(String hostname, int port, String servicePath, String user, String password) {
        this.hostname = hostname;
        this.port = port;
        this.servicePath = servicePath;
        this.user = user;
        this.password = password;
    }

    public HudsonClient(String urlString, String user, String password) throws MalformedURLException {
        URL url = new URL(urlString);
        this.hostname = url.getHost();
        this.port = url.getPort();
        this.servicePath = url.getPath();
        this.user = user;
        this.password = password;
    }

    private String encodeUrl(String url) throws UnsupportedEncodingException {
        String url2 = url.replace("+", "%2B");
        String encodedUrl = URLEncoder.encode(url2, "UTF-8");
        encodedUrl = encodedUrl.replace("+", "%20");
        return encodedUrl;
    }

    private String getTriggeringUrl(String jobName) throws UnsupportedEncodingException {
        String url = PROTOCOL_PREFIX + this.hostname + ":" + port + servicePath + "/job/" + encodeUrl(jobName) + "/build";
        return url;
    }

    private String getBuildListUrl(String jobName) throws UnsupportedEncodingException {
        String url = PROTOCOL_PREFIX + this.hostname + ":" + port + servicePath + "/job/" + encodeUrl(jobName) + "/api/xml";
        return url;
    }

    private String getBuildUrl(String jobName, int buildNumber) throws UnsupportedEncodingException {
        String url = PROTOCOL_PREFIX + this.hostname + ":" + port + servicePath + "/job/" + encodeUrl(jobName) + "/" + buildNumber + "/api/xml";
        return url;
    }

    public List<Build> listBuilds(String jobName, int buildNumber) throws Exception {
        throw new Exception("Not implemented");
    }

    public Build getBuild(String jobName, int buildNumber) throws ClientProtocolException, IOException, JDOMException {
        String url = getBuildUrl(jobName, buildNumber);
        HttpGet httpGet = new HttpGet(url);
        HttpResponse resp = httpClient.execute(httpGet);
        int respCode = resp.getStatusLine().getStatusCode();
        if (respCode == 200) {
            BufferedInputStream bis = new BufferedInputStream(resp.getEntity().getContent());
            SAXBuilder builder = new SAXBuilder(false);
            Document doc = builder.build(bis);
            Element root = doc.getRootElement();
            Build build = new Build();
            build.setBuilding(Boolean.parseBoolean(root.getChildText("building")));
            build.setDuration(Long.parseLong(root.getChildText("duration")));
            build.setFullDisplayName(root.getChildText("fullDisplayName"));
            build.setId(root.getChildText("id"));
            build.setNumber(Integer.parseInt(root.getChildText("number")));
            build.setResult(root.getChildText("result"));
            build.setTimestemp(root.getChildText("timpestamp"));
            build.setUrl(root.getChildText("url"));
            return build;
        } else {
            return null;
        }
    }

    /**
	 * 등록된 모든 job을 읽어온다.
	 * @return Job 목록
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JDOMException
	 */
    public List<Job> listJobs() throws ClientProtocolException, IOException, JDOMException {
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(this.hostname, this.port), new UsernamePasswordCredentials(this.user, this.password));
        HttpGet httpGet = new HttpGet(PROTOCOL_PREFIX + this.hostname + ":" + port + servicePath + "/api/xml");
        List<Job> lstRet = new LinkedList<Job>();
        HttpResponse resp = httpClient.execute(httpGet);
        int responseCode = resp.getStatusLine().getStatusCode();
        if (responseCode == 200) {
            String message = resp.getStatusLine().getReasonPhrase();
            HttpEntity entity = resp.getEntity();
            InputStream is = entity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            Document doc = builder.build(br);
            List<Element> lstJobs = XPath.selectNodes(doc, "/hudson/job");
            for (int i = 0; i < lstJobs.size(); i++) {
                Element elJob = lstJobs.get(i);
                Job aJob = new Job(elJob.getChild("name").getValue(), elJob.getChild("url").getValue(), elJob.getChild("color").getValue());
                lstRet.add(aJob);
            }
        } else {
        }
        return lstRet;
    }

    /**
	 * Job을 시작한다.
	 * @param jobName Job이름 ( 허드슨에서는 Job 이름이 unique하게 사용된다. )
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws JDOMException
	 */
    public int triggerJob(String jobName) throws IllegalStateException, IOException, JDOMException {
        String url = getTriggeringUrl(jobName);
        HttpGet httpGet = new HttpGet(url);
        System.out.println(url);
        HttpResponse resp = httpClient.execute(httpGet);
        int responseCode = resp.getStatusLine().getStatusCode();
        httpGet.abort();
        if (responseCode == 200) {
            httpGet = new HttpGet(getBuildListUrl(jobName));
            resp = httpClient.execute(httpGet);
            BufferedInputStream bis = new BufferedInputStream(resp.getEntity().getContent());
            SAXBuilder builder = new SAXBuilder(false);
            Document doc = builder.build(bis);
            Element elBuild = (Element) XPath.selectSingleNode(doc, "/*/build['0']");
            if (elBuild != null) {
                Element elNo = elBuild.getChild("number");
                return Integer.parseInt(elNo.getValue());
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public static void main(String args[]) throws ClientProtocolException, IOException, JDOMException {
        HudsonClient client = new HudsonClient("localhost", 8081, "/hudson", "hudson-admin", "1111");
        List<Job> lstJobs = client.listJobs();
        for (int i = 0; i < lstJobs.size(); i++) {
            Job aJob = lstJobs.get(i);
            System.out.println(aJob.getName() + " : " + aJob.getUrl());
        }
        String jobName = "Maven Test Build";
        int buildNumber = client.triggerJob(jobName);
        Build build = client.getBuild(jobName, buildNumber);
        System.out.println(build.getFullDisplayName() + ":" + build.isBuilding());
    }
}
