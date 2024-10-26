package com.talios.jira.idea;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.search.TodoPattern;
import com.intellij.ide.todo.TodoConfiguration;
import com.talios.jira.JiraException;
import com.talios.jira.browser.IssueBrowserPanel;
import com.talios.jira.browser.JiraItem;
import com.talios.jira.browser.JiraMonitorPanel;
import com.talios.jira.browser.JiraProjectDetails;
import com.talios.jira.browser.signals.JiraServerDetails;
import com.talios.jira.feeds.FeedBuilder;
import com.talios.jira.feeds.FeedException;
import com.talios.jira.feeds.MyIssuesFeedBuilder;
import com.talios.jira.rpc.JiraRpcClient;
import com.talios.jira.settings.SettingsForm;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @jira INTPLUG-133 Settings dialog always asks to save the changes
 */
public class JiraConfigurationComponent implements ProjectComponent, Configurable, JDOMExternalizable {

    private static final Logger LOGGER = Logger.getInstance("Jira Browser");

    public static final String KEY_NAME = "Talios.JiraConfigurationComponent";

    public static final String TOOL_WINDOW_ID = "Jira Browser";

    private JiraItem itemWorkedOn;

    private Project project;

    private ToolWindow toolWindow;

    private SettingsForm settings;

    private JiraMonitorPanel jiraMonitor;

    private List issueBrowsers;

    private JiraConfiguration configuration = new JiraConfiguration();

    public JiraConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JiraConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * @jira INTPLUG-4 Support for Multiple Projects
     */
    public JiraConfigurationComponent(Project project) {
        LOGGER.info("Jira config componet create");
        this.project = project;
        issueBrowsers = new ArrayList();
    }

    public String getComponentName() {
        return KEY_NAME;
    }

    public JiraMonitorPanel getJiraMonitor() {
        return jiraMonitor;
    }

    /**
     * @jira INTPLUG-34 Issue table doesn't seem to refreshIssues on project load anymore
     */
    public void projectClosed() {
        boolean enabled = getConfiguration().getEnableIssueTracking();
        if (enabled) {
            unRegisterToolWindow();
        }
    }

    /**
     * Project opened.
     *
     * @jira INTPLUG-23 NullPointer on getAutoRefresh()
     * @jira INTPLUG-34 Issue table doesn't seem to refreshIssues on project load anymore
     */
    public void projectOpened() {
        boolean enabled = getConfiguration().getEnableIssueTracking();
        if (enabled) {
            registerToolWindow();
            openSearch(new MyIssuesFeedBuilder(project), false, false);
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            final FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
            fileEditorManager.addFileEditorManagerListener(new FileEditorManagerListener() {

                public void fileOpened(FileEditorManager fileEditorManager, VirtualFile virtualFile) {
                    com.intellij.openapi.editor.Document document = fileDocumentManager.getDocument(virtualFile);
                    if (document != null) {
                        document.addDocumentListener(new JiraAnnotationListener(JiraConfigurationComponent.this));
                    }
                }

                public void fileClosed(FileEditorManager fileEditorManager, VirtualFile virtualFile) {
                }

                public void selectionChanged(FileEditorManagerEvent fileEditorManagerEvent) {
                }
            });
            refreshIssues();
        }
    }

    private void registerToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        if (toolWindow == null) {
            if (jiraMonitor == null) {
                jiraMonitor = new JiraMonitorPanel(project);
            }
            toolWindow = toolWindowManager.registerToolWindow(TOOL_WINDOW_ID, jiraMonitor.getTabs(), ToolWindowAnchor.BOTTOM);
            toolWindow.setIcon(IconLoader.getIcon("/jira.png", JiraConfigurationComponent.class));
            jiraMonitor.setToolWindow(toolWindow);
        }
    }

    private void unRegisterToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        if (toolWindowManager.getToolWindow(TOOL_WINDOW_ID) != null) {
            toolWindowManager.unregisterToolWindow(TOOL_WINDOW_ID);
            toolWindow = null;
        }
    }

    /**
     * @jira INTPLUG-34 Issue table doesn't seem to refreshIssues on project load anymore
     */
    public void disposeComponent() {
    }

    /**
     * @jira INTPLUG-34 Issue table doesn't seem to refreshIssues on project load anymore
     * @jira INTPLUG-4 Support for Multiple Projects
     */
    public void initComponent() {
    }

    public String getDisplayName() {
        return "Jira Configuration";
    }

    /**
     * @jira INTPLUG-4 Support for Multiple Projects
     */
    public void reset() {
        if (settings != null) {
            settings.setData(configuration);
        }
    }

    /**
     * @jira INTPLUG-9 Trap bad settings
     * @jira INTPLUG-4 Support for Multiple Projects
     */
    public void apply() throws ConfigurationException {
        boolean enabled = configuration.getEnableIssueTracking();
        boolean stateChanged = enabled != settings.getEnableIssueTracking().isSelected();
        settings.getData(configuration);
        if (stateChanged) {
            if (configuration.getEnableIssueTracking()) {
                registerToolWindow();
                openSearch(new MyIssuesFeedBuilder(project), false, false);
            } else {
                unRegisterToolWindow();
            }
            for (Object o : getConfiguration().getServerList()) {
                JiraServerDetails serverDetails = (JiraServerDetails) o;
                serverDetails.refreshLists(project);
            }
        }
        if (configuration.getEnableIssueTracking()) {
            checkSettings();
        }
    }

    /**
     * @jira INTPLUG-9 Trap bad settings
     * @jira INTPLUG-40 Add settings check for invalid project id.
     * @jira INTPLUG-4 Support for Multiple Projects
     */
    private void checkSettings() throws ConfigurationException {
        List serverList = getConfiguration().getServerList();
        for (Object aServerList : serverList) {
            JiraServerDetails jiraServerDetails = (JiraServerDetails) aServerList;
            URL url = null;
            try {
                if (jiraServerDetails.getBaseurl() == null || "".equals(jiraServerDetails.getBaseurl())) {
                    throw new ConfigurationException("BaseURL is empty.");
                }
                url = new URL(jiraServerDetails.getBaseurl());
                String content = getURLContent(url.openConnection().getInputStream());
                if (content.indexOf("Atlassian JIRA") == -1) {
                    throw new ConfigurationException("URL (" + url.toString() + ") Doesn't put to an installation of Atlassian JIRA");
                }
                try {
                    jiraServerDetails.getRpcClient(true).login();
                } catch (JiraException e) {
                    throw new ConfigurationException("Jira Server ( " + url.toString() + " ) is earlier than 2.6 or has RPC disabled.");
                }
            } catch (MalformedURLException e) {
                throw new ConfigurationException("Malformed URL: " + url);
            } catch (IOException e) {
                throw new ConfigurationException("Unable to contact server: " + url);
            }
            try {
                MyIssuesFeedBuilder feed = new MyIssuesFeedBuilder(new JiraServerDetails[] { jiraServerDetails });
                feed.buildFeedData();
            } catch (FeedException feedException) {
                throw new ConfigurationException(feedException.getMessage());
            }
        }
    }

    private String getURLContent(InputStream contentStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(contentStream));
        String inputLine;
        StringBuffer buffer = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            buffer.append(inputLine);
            buffer.append('\n');
        }
        in.close();
        return buffer.toString();
    }

    public String getHelpTopic() {
        return "Configured Jira Settings";
    }

    public JComponent createComponent() {
        if (settings == null) {
            settings = new SettingsForm(project);
        }
        return settings.getMainPanel();
    }

    public void disposeUIResources() {
    }

    /**
     * @jira INTPLUG-133 Settings dialog always asks to save the changes
     */
    public boolean isModified() {
        return settings.isModified(configuration);
    }

    public Icon getIcon() {
        return IconLoader.getIcon("/jirabig.png", JiraConfigurationComponent.class);
    }

    /**
     * @jira INTPLUG-4 Support for Multiple Projects
     */
    public void writeExternal(Element element) throws WriteExternalException {
        getConfiguration().writeExternal(element);
    }

    /**
     * @jira INTPLUG-4 Support for Multiple Projects
     */
    public void readExternal(Element element) throws InvalidDataException {
        getConfiguration().readExternal(element);
    }

    public JiraItem getItemWorkedOn() {
        return itemWorkedOn;
    }

    public void setItemWorkedOn(JiraItem itemWorkedOn) {
        this.itemWorkedOn = itemWorkedOn;
    }

    public String getBrowserExecutable() {
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File(PathManager.getConfigPath() + "/options/ide.general.xml"));
            XPath xpath = XPath.newInstance("/application/component/option[@name='browserPath']");
            Element node = (Element) xpath.selectSingleNode(doc);
            return node.getAttribute("value").getValue();
        } catch (IOException e) {
            return "";
        } catch (JDOMException e) {
            return "";
        }
    }

    public Project getProject() {
        return project;
    }

    /**
     * @jira INTPLUG-62 Add Search by Saved Filters feature
     */
    public boolean supportsRpc() {
        for (Object o : getConfiguration().getServerList()) {
            JiraServerDetails jiraServerDetails = (JiraServerDetails) o;
            JiraRpcClient client = jiraServerDetails.getRpcClient();
            try {
                if (client.login()) {
                    return true;
                }
            } catch (JiraException e) {
                return false;
            }
        }
        return false;
    }

    public JiraServerDetails findServerForIssueKey(String issueKey) {
        LOGGER.info("Finding Server for issue " + issueKey);
        for (Object o1 : getConfiguration().getServerList()) {
            JiraServerDetails jiraServerDetails = (JiraServerDetails) o1;
            for (Object o : jiraServerDetails.getProjectList()) {
                JiraProjectDetails jiraProjectDetails = (JiraProjectDetails) o;
                if (issueKey.indexOf(jiraProjectDetails.getProjectKey()) != -1) {
                    return jiraServerDetails;
                }
            }
        }
        return null;
    }

    public JiraProjectDetails findProjectForIssueKey(String issueKey) {
        LOGGER.info("Finding Server for issue " + issueKey);
        for (Object o1 : getConfiguration().getServerList()) {
            JiraServerDetails jiraServerDetails = (JiraServerDetails) o1;
            for (Object o : jiraServerDetails.getProjectList()) {
                JiraProjectDetails jiraProjectDetails = (JiraProjectDetails) o;
                if (issueKey.indexOf(jiraProjectDetails.getProjectKey()) != -1) {
                    return jiraProjectDetails;
                }
            }
        }
        return null;
    }

    public void closeSearchTab(IssueBrowserPanel issueBrowserPanel) {
        if (issueBrowserPanel != null) {
            int index = getJiraMonitor().getTabs().indexOfComponent(issueBrowserPanel.getMainPanel());
            if (index != -1) {
                getJiraMonitor().getTabs().remove(index);
            }
            issueBrowsers.remove(issueBrowserPanel);
            if (getJiraMonitor().getTabs().getTabCount() == 0 && toolWindow != null) {
                unRegisterToolWindow();
            }
        }
    }

    public void openSearch(FeedBuilder feedBuilder, boolean readOnly) {
        openSearch(feedBuilder, readOnly, true);
    }

    private void openSearch(FeedBuilder feedBuilder, boolean readOnly, boolean showSearch) {
        registerToolWindow();
        IssueBrowserPanel issueBrowserPanel = new IssueBrowserPanel(project, feedBuilder, readOnly);
        issueBrowserPanel.refresh();
        getJiraMonitor().getTabs().add(feedBuilder.getSearchFor(), issueBrowserPanel.getMainPanel());
        getJiraMonitor().getTabs().setSelectedComponent(issueBrowserPanel.getMainPanel());
        issueBrowsers.add(issueBrowserPanel);
        if (showSearch) {
            registerToolWindow();
            toolWindow.show(new Runnable() {

                public void run() {
                }
            });
        }
    }

    public void refreshIssues() {
        LOGGER.info("Refreshing issues and server information...");
        for (Object issueBrowser : issueBrowsers) {
            IssueBrowserPanel issueBrowserPanel = (IssueBrowserPanel) issueBrowser;
            issueBrowserPanel.refresh();
        }
        List serverList = getConfiguration().getServerList();
        for (Object aServerList : serverList) {
            JiraServerDetails serverDetails = (JiraServerDetails) aServerList;
            serverDetails.refreshLists(project);
        }
    }

    private class IssueRefreshThread extends Thread {

        private boolean terminate;

        public void terminate() {
            terminate = true;
        }

        public void run() {
            try {
                while (!terminate) {
                    refreshIssues();
                    int refreshTimeout = JiraConfigurationComponent.this.getConfiguration().getRefreshTimeout();
                    LOGGER.info("Sleeping update thread for " + refreshTimeout + " minutes.");
                    sleep(60000 * refreshTimeout);
                }
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
