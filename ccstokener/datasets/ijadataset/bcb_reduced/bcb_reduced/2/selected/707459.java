package com.googlecode.greenbridge.storyharvester.impl;

import com.googlecode.greenbridge.storyharvester.ScenarioNarrative;
import com.googlecode.greenbridge.storyharvester.StoryHarvester;
import com.googlecode.greenbridge.storyharvester.StoryNarrative;
import com.googlecode.greenbridge.util.JavaLanguageSupport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author ryan
 */
public class JiraStoryHarvester implements StoryHarvester {

    private String jiraFilterURL;

    private String jiraRootUrl;

    private String jiraUser;

    private String jiraPassword;

    private Integer scenarioTypeId;

    private Integer storyTypeId;

    private SimpleDateFormat jiraUpdatedParser;

    private SimpleDateFormat updateDateToVersion;

    public JiraStoryHarvester() {
        jiraUpdatedParser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z (z)");
        updateDateToVersion = new SimpleDateFormat("yyyyMMddHHmmss");
    }

    @Override
    public List<StoryNarrative> gather() throws Exception {
        Document d = loadDocument();
        List<Element> nodes = findAllNodes(d);
        List<Element> xmlstories = filterStories(nodes);
        Map<Integer, StoryNarrative> stories = convertToStories(xmlstories);
        List<Element> xmlscenarios = filterScenarios(nodes);
        convertToScenarioAndAddToStory(xmlscenarios, stories);
        return new ArrayList<StoryNarrative>(stories.values());
    }

    protected Document loadDocument() throws MalformedURLException, DocumentException, IOException {
        if (jiraFilterURL.startsWith("file")) {
            URL url = getSourceURL();
            return parseDocument(url);
        } else {
            HttpClient httpClient = new DefaultHttpClient();
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("os_username", jiraUser));
            formparams.add(new BasicNameValuePair("os_password", jiraPassword));
            formparams.add(new BasicNameValuePair("os_cookie", "true"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            HttpPost post = new HttpPost(getJiraRootUrl() + "/secure/login.jsp");
            post.setEntity(entity);
            HttpResponse response = httpClient.execute(post);
            response.getEntity().consumeContent();
            String url_str = StringEscapeUtils.unescapeXml(jiraFilterURL);
            HttpGet get = new HttpGet(url_str);
            response = httpClient.execute(get);
            return parseDocument(response.getEntity().getContent());
        }
    }

    protected Document parseDocument(InputStream stream) throws DocumentException {
        SAXReader reader = new SAXReader();
        return reader.read(stream);
    }

    protected Document parseDocument(URL url) throws DocumentException {
        SAXReader reader = new SAXReader();
        return reader.read(url);
    }

    protected List<Element> findAllNodes(Document d) {
        XPath xpath = DocumentHelper.createXPath("//item");
        return xpath.selectNodes(d);
    }

    protected void convertToScenarioAndAddToStory(List<Element> scenarios, Map<Integer, StoryNarrative> stories) {
        for (Element scenario : scenarios) {
            Integer parentJiraId = Integer.parseInt(scenario.element("parent").attributeValue("id"));
            ScenarioNarrative narrative = convertToScenario(scenario);
            StoryNarrative story = stories.get(parentJiraId);
            if (story != null) {
                story.getScenarios().add(narrative);
            }
        }
    }

    private ScenarioNarrative convertToScenario(Element xmlScenario) {
        String summary = xmlScenario.elementText("summary");
        String description = xmlScenario.elementText("description");
        ScenarioNarrative narrative = new ScenarioNarrative(parseStoryNarrative(summary, description));
        String jira_key = xmlScenario.elementText("key");
        narrative.setId(safeID(jira_key));
        narrative.setLinkName("See scenario in Jira");
        String link = xmlScenario.elementText("link");
        narrative.setLinkUrl(link);
        String updated = xmlScenario.elementText("updated");
        narrative.setVersion(turnDateToVersion(parseUpdatedDate(updated)));
        return narrative;
    }

    protected Map<Integer, StoryNarrative> convertToStories(List<Element> stories) {
        Map<Integer, StoryNarrative> jiraIdToStory = new HashMap<Integer, StoryNarrative>();
        for (Element story : stories) {
            Integer jiraId = Integer.parseInt(story.element("key").attributeValue("id"));
            StoryNarrative narrative = convertToStory(story);
            jiraIdToStory.put(jiraId, narrative);
        }
        return jiraIdToStory;
    }

    private StoryNarrative convertToStory(Element xmlStory) {
        StoryNarrative narrative = new StoryNarrative();
        String jira_key = xmlStory.elementText("key");
        narrative.setId(safeID(jira_key));
        narrative.setLinkName("See story in Jira");
        String link = xmlStory.elementText("link");
        narrative.setLinkUrl(link);
        String summary = xmlStory.elementText("summary");
        String description = xmlStory.elementText("description");
        narrative.setStoryNarrative(parseStoryNarrative(summary, description));
        String updated = xmlStory.elementText("updated");
        narrative.setVersion(turnDateToVersion(parseUpdatedDate(updated)));
        return narrative;
    }

    protected Date parseUpdatedDate(String updated) {
        try {
            return jiraUpdatedParser.parse(updated);
        } catch (ParseException ex) {
            Logger.getLogger(JiraStoryHarvester.class.getName()).log(Level.SEVERE, null, ex);
            return new Date();
        }
    }

    protected long turnDateToVersion(Date date) {
        return Long.parseLong(updateDateToVersion.format(date));
    }

    protected List<String> parseStoryNarrative(String summary, String description) {
        List<String> narrative = new ArrayList<String>();
        narrative.add(StringEscapeUtils.escapeJava(summary));
        narrative.addAll(removeHtml(description));
        return narrative;
    }

    protected List<String> removeHtml(String summary) {
        String htmld = StringEscapeUtils.unescapeHtml(summary);
        StringReader reader = new StringReader(htmld);
        try {
            return HTMLUtils.extractText(reader);
        } catch (IOException ex) {
            Logger.getLogger(JiraStoryHarvester.class.getName()).log(Level.SEVERE, null, ex);
            return new ArrayList<String>();
        }
    }

    protected List<Element> filterScenarios(List<Element> elements) {
        List<Element> scenarios = new ArrayList<Element>();
        for (Element element : elements) {
            Element parent = element.element("parent");
            if (parent != null) {
                if (scenarioTypeId != null) {
                    Integer type = Integer.valueOf(element.element("type").attribute("id").getText());
                    if (scenarioTypeId.equals(type)) {
                        scenarios.add(element);
                    }
                } else {
                    scenarios.add(element);
                }
            }
        }
        return scenarios;
    }

    protected List<Element> filterStories(List<Element> elements) {
        List<Element> stories = new ArrayList<Element>();
        for (Element element : elements) {
            Element parent = element.element("parent");
            if (parent == null) {
                if (storyTypeId != null) {
                    Integer type = Integer.valueOf(element.element("type").attribute("id").getText());
                    if (storyTypeId.equals(type)) {
                        stories.add(element);
                    }
                } else {
                    stories.add(element);
                }
            }
        }
        return stories;
    }

    protected URL getSourceURL() throws MalformedURLException {
        return new URL(getJiraFilterURL());
    }

    protected String safeID(String title) {
        return JavaLanguageSupport.makeJavaIdentifier(title);
    }

    /**
     * @return the jiraFilterURL
     */
    public String getJiraFilterURL() {
        return jiraFilterURL;
    }

    /**
     * @param jiraFilterURL the jiraFilterURL to set
     */
    public void setJiraFilterURL(String jiraFilterURL) {
        this.jiraFilterURL = jiraFilterURL;
    }

    /**
     * @return the scenarioTypeId
     */
    public Integer getScenarioTypeId() {
        return scenarioTypeId;
    }

    /**
     * @param scenarioTypeId the scenarioTypeId to set
     */
    public void setScenarioTypeId(Integer scenarioTypeId) {
        this.scenarioTypeId = scenarioTypeId;
    }

    /**
     * @return the storyTypeId
     */
    public Integer getStoryTypeId() {
        return storyTypeId;
    }

    /**
     * @param storyTypeId the storyTypeId to set
     */
    public void setStoryTypeId(Integer storyTypeId) {
        this.storyTypeId = storyTypeId;
    }

    /**
     * @return the jiraRootUrl
     */
    public String getJiraRootUrl() {
        return jiraRootUrl;
    }

    /**
     * @param jiraRootUrl the jiraRootUrl to set
     */
    public void setJiraRootUrl(String jiraRootUrl) {
        this.jiraRootUrl = jiraRootUrl;
    }

    /**
     * @return the jiraUser
     */
    public String getJiraUser() {
        return jiraUser;
    }

    /**
     * @param jiraUser the jiraUser to set
     */
    public void setJiraUser(String jiraUser) {
        this.jiraUser = jiraUser;
    }

    /**
     * @return the jiraPassword
     */
    public String getJiraPassword() {
        return jiraPassword;
    }

    /**
     * @param jiraPassword the jiraPassword to set
     */
    public void setJiraPassword(String jiraPassword) {
        this.jiraPassword = jiraPassword;
    }
}
