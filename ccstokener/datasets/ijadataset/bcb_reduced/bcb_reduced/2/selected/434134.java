package websphinx.workbench;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import rcm.awt.ClosableFrame;
import rcm.awt.Constrain;
import rcm.awt.PopupDialog;
import rcm.awt.TabPanel;
import websphinx.Access;
import websphinx.CrawlEvent;
import websphinx.CrawlListener;
import websphinx.Crawler;
import websphinx.DownloadParameters;
import websphinx.EventLog;
import websphinx.LinkListener;

public class Workbench extends Panel implements CrawlListener {

    Crawler crawler;

    String currentFilename = "";

    Panel workbenchPanel;

    GridBagConstraints workbenchConstraints;

    WorkbenchVizPanel vizPanel;

    GridBagConstraints vizConstraints;

    WebGraph graph;

    WebOutline outline;

    Statistics statistics;

    EventLog logger;

    MenuBar menubar;

    Menu fileMenu;

    MenuItem newCrawlerItem;

    MenuItem openCrawlerItem;

    MenuItem saveCrawlerItem;

    MenuItem createCrawlerItem;

    MenuItem exitItem;

    Panel menuPanel;

    Button newCrawlerButton;

    Button openCrawlerButton;

    Button saveCrawlerButton;

    Button createCrawlerButton;

    WorkbenchTabPanel configPanel;

    Panel simplePanel;

    Panel crawlPanel;

    Panel limitsPanel;

    Panel headersPanel;

    Panel classifiersPanel;

    Panel linksPanel;

    Panel actionPanel;

    CrawlerEditor crawlerEditor;

    ClassifierListEditor classifierListEditor;

    DownloadParametersEditor downloadParametersEditor;

    HeadersEditor headersEditor;

    LinkPredicateEditor linkPredicateEditor;

    PagePredicateEditor pagePredicateEditor;

    ActionEditor actionEditor;

    SimpleCrawlerEditor simpleCrawlerEditor;

    boolean advancedMode = false;

    boolean tornOff = false;

    Button startButton, pauseButton, stopButton, clearButton;

    boolean allowExit;

    Frame workbenchFrame;

    Frame vizFrame;

    static final int MARGIN = 8;

    public Workbench() {
        this(makeDefaultCrawler());
        return;
    }

    private static Crawler makeDefaultCrawler() {
        Crawler c = new Crawler();
        c.setDomain(Crawler.SUBTREE);
        return c;
    }

    public Workbench(String filename) throws Exception {
        this(loadCrawler(new FileInputStream(filename)));
    }

    public Workbench(URL url) throws Exception {
        this(loadCrawler(url.openStream()));
    }

    public Workbench(Crawler _crawler) {
        Browser browser = Context.getBrowser();
        setLayout(new BorderLayout());
        setBackground(Color.lightGray);
        setLayout(new GridLayout(2, 1));
        add(workbenchPanel = new Panel());
        workbenchPanel.setLayout(new GridBagLayout());
        makeMenus();
        Constrain.add(workbenchPanel, menuPanel, Constrain.labelLike(0, 0));
        configPanel = new WorkbenchTabPanel();
        Constrain.add(workbenchPanel, configPanel, Constrain.areaLike(0, 1));
        simplePanel = makeSimplePanel();
        crawlPanel = makeCrawlPanel();
        linksPanel = makeLinksPanel();
        actionPanel = makeActionPanel();
        classifiersPanel = makeClassifiersPanel();
        limitsPanel = makeLimitsPanel();
        headersPanel = makeHeadersPanel();
        Constrain.add(workbenchPanel, makeButtonPanel(), Constrain.fieldLike(0, 2));
        add(vizPanel = new WorkbenchVizPanel(this));
        graph = new WebGraph();
        graph.setBackground(Color.white);
        if (browser != null) graph.addLinkViewListener(browser);
        vizPanel.addTabPanel("Graph", true, graph);
        outline = new WebOutline();
        outline.setBackground(Color.white);
        if (browser != null) outline.addLinkViewListener(browser);
        vizPanel.addTabPanel("Outline", true, outline);
        statistics = new Statistics();
        Panel p = new Panel();
        p.setLayout(new FlowLayout());
        p.add(statistics);
        vizPanel.addTabPanel("Statistics", true, p);
        logger = new EventLog();
        setCrawler(_crawler);
    }

    public Frame makeFrame() {
        if (workbenchFrame == null) {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            workbenchFrame = new WorkbenchFrame(this);
            workbenchFrame.setForeground(getForeground());
            workbenchFrame.setBackground(getBackground());
            workbenchFrame.setFont(getFont());
            workbenchFrame.setTitle("Crawler Workbench: " + (crawler != null ? crawler.getName() : ""));
            workbenchFrame.setLayout(new GridLayout(1, 1));
            workbenchFrame.add(this);
            workbenchPanel.remove(menuPanel);
            workbenchFrame.setMenuBar(menubar);
            workbenchFrame.reshape(0, 0, Math.min(550, screen.width), screen.height - 50);
        }
        return workbenchFrame;
    }

    public void setAllowExit(boolean yes) {
        allowExit = yes;
    }

    public boolean getAllowExit() {
        return allowExit;
    }

    public synchronized void setAdvancedMode(boolean adv) {
        if (advancedMode == adv) return;
        configureCrawler();
        advancedMode = adv;
        setCrawler(crawler);
        configPanel.advancedButton.setLabel(advancedMode ? "<< Simple" : ">> Advanced");
        validate();
    }

    public boolean getAdvancedMode() {
        return advancedMode;
    }

    static void setVisible(Component comp, boolean visible) {
        comp.setVisible(visible);
    }

    static void setEnabled(Component comp, boolean enabled) {
        comp.setEnabled(enabled);
    }

    static void setEnabled(MenuItem item, boolean enabled) {
        item.setEnabled(enabled);
    }

    Panel makeMenus() {
        menubar = new MenuBar();
        menuPanel = new Panel();
        menuPanel.setLayout(new FlowLayout());
        menubar.add(fileMenu = new Menu("File"));
        fileMenu.add(newCrawlerItem = new MenuItem("New Crawler"));
        menuPanel.add(newCrawlerButton = new Button("New"));
        fileMenu.add(openCrawlerItem = new MenuItem("Open Crawler..."));
        menuPanel.add(openCrawlerButton = new Button("Open..."));
        fileMenu.add(saveCrawlerItem = new MenuItem("Save Crawler..."));
        menuPanel.add(saveCrawlerButton = new Button("Save..."));
        fileMenu.add(createCrawlerItem = new MenuItem("Create Crawler From Class..."));
        menuPanel.add(createCrawlerButton = new Button("Create..."));
        fileMenu.add(exitItem = new MenuItem("Exit"));
        return menuPanel;
    }

    private Panel makeSimplePanel() {
        return simpleCrawlerEditor = new SimpleCrawlerEditor();
    }

    private Panel makeCrawlPanel() {
        return crawlerEditor = new CrawlerEditor();
    }

    private Panel makeLinksPanel() {
        Panel panel = new Panel();
        panel.setLayout(new GridBagLayout());
        Constrain.add(panel, new Label("Follow:"), Constrain.labelLike(0, 0));
        Constrain.add(panel, linkPredicateEditor = new LinkPredicateEditor(), Constrain.areaLike(1, 0));
        return panel;
    }

    private Panel makeActionPanel() {
        Panel panel = new Panel();
        panel.setLayout(new GridBagLayout());
        Constrain.add(panel, new Label("Action:"), Constrain.labelLike(0, 0));
        Constrain.add(panel, actionEditor = new ActionEditor(), Constrain.areaLike(1, 0));
        Constrain.add(panel, new Label("on pages:"), Constrain.labelLike(0, 1));
        Constrain.add(panel, pagePredicateEditor = new PagePredicateEditor(), Constrain.areaLike(1, 1));
        return panel;
    }

    private Panel makeClassifiersPanel() {
        classifierListEditor = new ClassifierListEditor();
        return classifierListEditor;
    }

    private Panel makeLimitsPanel() {
        downloadParametersEditor = new DownloadParametersEditor();
        return downloadParametersEditor;
    }

    private Panel makeHeadersPanel() {
        headersEditor = new HeadersEditor();
        return headersEditor;
    }

    private Panel makeButtonPanel() {
        Panel panel = new Panel();
        panel.setLayout(new FlowLayout());
        panel.add(startButton = new Button("Start"));
        panel.add(pauseButton = new Button("Pause"));
        panel.add(stopButton = new Button("Stop"));
        panel.add(clearButton = new Button("Clear"));
        enableButtons(true, false, false, false);
        return panel;
    }

    String getCrawlerClassName(String label) {
        String className = label;
        if (className != null) {
            if (className.equals("Crawler")) className = "websphinx.Crawler"; else if (className.equals("Load Class...")) className = null;
        }
        return className;
    }

    public boolean handleEvent(Event event) {
        if (doEvent(event)) return true; else return super.handleEvent(event);
    }

    boolean doEvent(Event event) {
        if (event.id == Event.ACTION_EVENT) {
            if (event.target instanceof MenuItem) {
                MenuItem item = (MenuItem) event.target;
                if (item == newCrawlerItem) newCrawler(); else if (item == openCrawlerItem) openCrawler(); else if (item == saveCrawlerItem) saveCrawler(); else if (item == createCrawlerItem) createCrawler(null); else if (item == exitItem) close(); else return false;
            } else if (event.target == newCrawlerButton) newCrawler(); else if (event.target == openCrawlerButton) openCrawler(); else if (event.target == saveCrawlerButton) saveCrawler(); else if (event.target == createCrawlerButton) createCrawler(null); else if (event.target == configPanel.advancedButton) setAdvancedMode(!advancedMode); else if (event.target == vizPanel.optionsButton) new WorkbenchControlPanel(graph, outline).show(); else if (event.target == vizPanel.tearoffButton) if (tornOff) dockVisualizations(); else tearoffVisualizations(); else if (event.target == startButton) start(); else if (event.target == pauseButton) pause(); else if (event.target == stopButton) stop(); else if (event.target == clearButton) clear(); else return false;
        } else return false;
        return true;
    }

    protected void finalize() {
    }

    void close() {
        if (!allowExit) return;
        if (Context.isApplication()) {
            Runtime.runFinalizersOnExit(true);
            System.exit(0);
        }
    }

    public void refresh() {
        graph.updateClosure(crawler.getCrawledRoots());
        outline.updateClosure(crawler.getCrawledRoots());
    }

    void connectVisualization(Crawler crawler, Object viz, boolean linksToo) {
        if (viz instanceof CrawlListener) crawler.addCrawlListener((CrawlListener) viz);
        if (linksToo && viz instanceof LinkListener) crawler.addLinkListener((LinkListener) viz);
    }

    void disconnectVisualization(Crawler crawler, Object viz, boolean linksToo) {
        if (viz instanceof CrawlListener) crawler.removeCrawlListener((CrawlListener) viz);
        if (linksToo && viz instanceof LinkListener) crawler.removeLinkListener((LinkListener) viz);
    }

    void showVisualization(Object viz) {
        if (viz == graph) graph.start();
    }

    void hideVisualization(Object viz) {
        if (viz == graph) graph.stop();
    }

    void tearoffVisualizations() {
        if (tornOff) return;
        if (vizFrame == null) {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            vizFrame = new WorkbenchVizFrame(this);
            vizFrame.setForeground(getForeground());
            vizFrame.setBackground(getBackground());
            vizFrame.setFont(getFont());
            vizFrame.setTitle("Visualization: " + (crawler != null ? crawler.getName() : ""));
            vizFrame.setLayout(new GridLayout(1, 1));
            vizFrame.reshape(0, 0, Math.min(550, screen.width), screen.height / 2);
        }
        remove(vizPanel);
        setLayout(new GridLayout(1, 1));
        validate();
        vizFrame.add(vizPanel);
        setVisible(vizFrame, true);
        vizPanel.tearoffButton.setLabel("Glue Back");
        tornOff = true;
    }

    void dockVisualizations() {
        if (!tornOff) return;
        setVisible(vizFrame, false);
        vizFrame.remove(vizPanel);
        setLayout(new GridLayout(2, 1));
        add(vizPanel);
        validate();
        vizPanel.tearoffButton.setLabel("Tear Off");
        tornOff = false;
    }

    void newCrawler() {
        setCrawler(makeDefaultCrawler());
        currentFilename = "";
    }

    void createCrawler(String className) {
        if (className == null || className.length() == 0) {
            className = PopupDialog.ask(workbenchPanel, "New Crawler", "Create a Crawler of class:", crawler.getClass().getName());
            if (className == null) return;
        }
        try {
            Class crawlerClass = (Class) Class.forName(className);
            Crawler newCrawler = (Crawler) crawlerClass.newInstance();
            setCrawler(newCrawler);
            currentFilename = "";
        } catch (Exception e) {
            PopupDialog.warn(workbenchPanel, "Error", e.toString());
            return;
        }
    }

    void openCrawler() {
        String fn = PopupDialog.askFilename(workbenchPanel, "Open Crawler", "", true);
        if (fn != null) openCrawler(fn);
    }

    void openCrawler(String filename) {
        try {
            setCrawler(loadCrawler(Access.getAccess().readFile(new File(filename))));
            currentFilename = filename;
        } catch (Exception e) {
            PopupDialog.warn(workbenchPanel, "Error", e.toString());
        }
    }

    void openCrawler(URL url) {
        try {
            setCrawler(loadCrawler(Access.getAccess().openConnection(url).getInputStream()));
            currentFilename = "";
        } catch (Exception e) {
            PopupDialog.warn(workbenchPanel, "Error", e.toString());
        }
    }

    static Crawler loadCrawler(InputStream stream) throws Exception {
        ObjectInputStream in = new ObjectInputStream(stream);
        Crawler loadedCrawler = (Crawler) in.readObject();
        in.close();
        return loadedCrawler;
    }

    void saveCrawler() {
        String fn = PopupDialog.askFilename(workbenchPanel, "Save Crawler As", currentFilename, true);
        if (fn != null) saveCrawler(fn);
    }

    void saveCrawler(String filename) {
        configureCrawler();
        try {
            ObjectOutputStream out = new ObjectOutputStream(Access.getAccess().writeFile(new File(filename), false));
            out.writeObject((Object) crawler);
            out.close();
            currentFilename = filename;
        } catch (Exception e) {
            PopupDialog.warn(workbenchPanel, "Error", e.toString());
        }
    }

    void configureCrawler() {
        if (advancedMode) {
            crawlerEditor.getCrawler();
            classifierListEditor.getCrawler();
            DownloadParameters dp = downloadParametersEditor.getDownloadParameters();
            DownloadParameters dpheaders = headersEditor.getHeaderParameters();
            dp = dp.changeAcceptLanguage(dpheaders.getAcceptLanguage()).changeUserAgent(dpheaders.getUserAgent());
            crawler.setDownloadParameters(dp);
            if (advancedMode) {
                crawler.setLinkPredicate(linkPredicateEditor.getLinkPredicate());
                crawler.setPagePredicate(pagePredicateEditor.getPagePredicate());
                crawler.setAction(actionEditor.getAction());
            }
        } else simpleCrawlerEditor.getCrawler();
    }

    void enableButtons(boolean fStart, boolean fPause, boolean fStop, boolean fClear) {
        setEnabled(startButton, fStart);
        setEnabled(pauseButton, fPause);
        setEnabled(stopButton, fStop);
        setEnabled(clearButton, fClear);
    }

    public void setCrawler(Crawler _crawler) {
        if (crawler != _crawler) {
            if (crawler != null) {
                clear();
                disconnectVisualization(crawler, this, false);
                disconnectVisualization(crawler, graph, true);
                disconnectVisualization(crawler, outline, true);
                disconnectVisualization(crawler, statistics, false);
                disconnectVisualization(crawler, logger, true);
            }
            connectVisualization(_crawler, this, false);
            connectVisualization(_crawler, graph, true);
            connectVisualization(_crawler, outline, true);
            connectVisualization(_crawler, statistics, false);
            connectVisualization(_crawler, logger, true);
        }
        crawler = _crawler;
        String name = crawler.getName();
        if (workbenchFrame != null) workbenchFrame.setTitle("Crawler Workbench: " + name);
        if (vizFrame != null) vizFrame.setTitle("Visualization: " + name);
        if (advancedMode) {
            crawlerEditor.setCrawler(crawler);
            classifierListEditor.setCrawler(crawler);
            downloadParametersEditor.setDownloadParameters(crawler.getDownloadParameters());
            if (advancedMode) {
                linkPredicateEditor.setLinkPredicate(crawler.getLinkPredicate());
                pagePredicateEditor.setPagePredicate(crawler.getPagePredicate());
                actionEditor.setAction(crawler.getAction());
            }
        } else simpleCrawlerEditor.setCrawler(crawler);
        if (advancedMode) showAdvancedTabs(); else showSimpleTabs();
    }

    public Crawler getCrawler() {
        return crawler;
    }

    private void showAdvancedTabs() {
        if (configPanel.countTabs() != 6) {
            configPanel.removeAllTabPanels();
            configPanel.addTabPanel("Crawl", true, crawlPanel);
            configPanel.addTabPanel("Links", true, linksPanel);
            configPanel.addTabPanel("Pages", true, actionPanel);
            configPanel.addTabPanel("Classifiers", true, classifiersPanel);
            configPanel.addTabPanel("Limits", true, limitsPanel);
            configPanel.addTabPanel("Headers", true, headersPanel);
        }
    }

    private void showSimpleTabs() {
        if (configPanel.countTabs() != 1) {
            configPanel.removeAllTabPanels();
            configPanel.addTabPanel("Crawl", true, simplePanel);
        }
    }

    public void start() {
        configureCrawler();
        if (crawler.getState() == CrawlEvent.STOPPED) crawler.clear();
        Thread thread = new Thread(crawler, crawler.getName());
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        crawler.stop();
    }

    public void pause() {
        crawler.pause();
    }

    public void clear() {
        crawler.clear();
    }

    /**
     * Notify that the crawler started
     */
    public void started(CrawlEvent event) {
        enableButtons(false, true, true, false);
    }

    /**
     * Notify that the crawler ran out of links to crawl
     */
    public void stopped(CrawlEvent event) {
        enableButtons(true, false, false, true);
    }

    /**
     * Notify that the crawler's state was cleared.
     */
    public void cleared(CrawlEvent event) {
        enableButtons(true, false, false, false);
    }

    /**
     * Notify that the crawler timed out.
     */
    public void timedOut(CrawlEvent event) {
        enableButtons(true, false, false, true);
    }

    /**
     * Notify that the crawler was paused.
     */
    public void paused(CrawlEvent event) {
        enableButtons(true, false, true, true);
    }

    public static void main(String[] args) throws Exception {
        Workbench w = (args.length == 0) ? new Workbench() : new Workbench(args[0]);
        w.setAllowExit(true);
        Frame f = w.makeFrame();
        f.show();
    }
}

class WorkbenchFrame extends ClosableFrame {

    Workbench workbench;

    public WorkbenchFrame(Workbench workbench) {
        super();
        this.workbench = workbench;
    }

    public void close() {
        workbench.close();
    }

    public boolean handleEvent(Event event) {
        if (workbench.doEvent(event)) return true; else return super.handleEvent(event);
    }
}

class WorkbenchVizFrame extends ClosableFrame {

    Workbench workbench;

    public WorkbenchVizFrame(Workbench workbench) {
        super(true);
        this.workbench = workbench;
    }

    public void close() {
        workbench.dockVisualizations();
        super.close();
    }

    public boolean handleEvent(Event event) {
        if (workbench.doEvent(event)) return true; else return super.handleEvent(event);
    }
}

class WorkbenchTabPanel extends TabPanel {

    Button advancedButton;

    public WorkbenchTabPanel() {
        super();
        add(advancedButton = new Button("Advanced >>"));
    }
}

class WorkbenchVizPanel extends TabPanel {

    Workbench workbench;

    Button optionsButton;

    Button tearoffButton;

    public WorkbenchVizPanel(Workbench workbench) {
        this.workbench = workbench;
        add(optionsButton = new Button("Options..."));
        add(tearoffButton = new Button("Tear Off"));
    }

    public void select(int num) {
        Component prior = getSelectedComponent();
        super.select(num);
        Component now = getSelectedComponent();
        if (prior == now) return;
        if (prior != null) workbench.hideVisualization(prior);
        if (now != null) {
            workbench.showVisualization(now);
            now.requestFocus();
        }
    }
}
