package uk.ac.imperial.ma.metric.apps.metricGUI;

import java.awt.event.ActionListener;
import uk.ac.imperial.ma.metric.util.Task;
import uk.ac.imperial.ma.metric.err.ExceptionProcessor;
import uk.ac.imperial.ma.metric.util.TaskMonitor;
import uk.ac.imperial.ma.metric.util.Loader;
import uk.ac.imperial.ma.metric.gui.MetricFrame;
import uk.ac.imperial.ma.metric.nav.NavigationTreeNode;
import java.net.URL;
import java.net.JarURLConnection;
import java.io.InputStream;
import uk.ac.imperial.ma.metric.nav.NavigationTreeBuilder;
import uk.ac.imperial.ma.metric.nav.NavigationTreeView;
import uk.ac.imperial.ma.metric.nav.NavigationTreeViewSetter;
import javax.swing.JTree;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import uk.ac.imperial.ma.metric.services.CommandLineArgumentProcessor;

/**
 * This is the root class of the metric application. The {@link MetricAppLauncher}
 * class constructs an instance of this class and then calls the <code>run()</code>
 * method.
 *
 * @version 0.3.0 28 July 2003
 * @author Daniel J. R. May
 */
public class MetricMain implements ActionListener, Task {

    /** The location of the <code>contents.xml.gz</code> file from the <code>CLASSPATH</code> */
    public static final String DEFAULT_CONTENTS_FILE = "uk/ac/imperial/ma/metric/xml/contents/contents.xml.gz";

    /** The location of the <code>about.html</code> file which is displayed when a user clicks on the
     * <code>Help &gt; About</code> menu item of the metric frame. */
    public static final String ABOUT_JARURL = "MetricGUI.jar!/uk/ac/imperial/ma/metric/html/about/about.html";

    public static final String GLOSSARY_JARURL = "MetricGUI.jar!/uk/ac/imperial/ma/metric/html/help/glossary/glossary.html";

    public static final String MANUAL_JARURL = "MetricGUI.jar!/uk/ac/imperial/ma/metric/html/help/manual/manual.html";

    /** The location of the <code>welcome.html</code> file which is displayed in the main body of the
     * metric frame on start up. */
    public static final String WELCOME_JARURL = "MetricGUI.jar!/uk/ac/imperial/ma/metric/html/welcome/welcome.html";

    public static final String CLIENT_POLICY = "MetricGUI.jar!/uk/ac/imperial/ma/metric/rmi/client/client.policy";

    /** The text that appears in the tab which contains the <code>welcome.html</code> page in the metric frame. */
    public static final String WELCOME_TITLE = "Welcome";

    /** The tooltip that appears in the tab which contains the <code>welcome.html</code> page in the metric frame. */
    public static final String WELCOME_TIP = "Welcome document and messages.";

    /** The title of the thread group which is intended to contain all of this applications child threads */
    public static final String METRIC_MAIN_THREAD_GROUP_NAME = "MetricMain";

    /** Error code for a clean exit */
    public static final int EXIT_CODE_OK = 0;

    /** Error code for an exit when other threads are still running */
    public static final int EXIT_CODE_ERROR_THREADS_STILL_RUNNING = 1;

    /** The {@link ExceptionProcessor} for this application. */
    public final ExceptionProcessor EXCEPTION_PROCESSOR = new ExceptionProcessor();

    /** This {@link ThreadGroup} is intended to contain all of this applications child threads */
    public final ThreadGroup METRIC_MAIN_THREAD_GROUP = new ThreadGroup(METRIC_MAIN_THREAD_GROUP_NAME);

    /** This {@link uk.ac.imperial.ma.metric.util.TaskMonitor TaskMonitor} can (and should) be used
     * to monitor the progress of any task undertaken by the metric application. */
    public final TaskMonitor TASK_MONITOR = new TaskMonitor();

    /** This class takes care of loading resources for this application. */
    public final Loader LOADER = new Loader();

    /** This is the frame contains the GUI that the user interacts with. */
    public MetricFrame metricFrame;

    /** The root node of the navigation tree as built by {@link NavigationTreeBuilder}. */
    private NavigationTreeNode ntnRoot;

    public NavigationTreeViewSetter navigationTreeViewSetter;

    private String contentsFile;

    private String navigationView;

    private boolean isAlive;

    private boolean cancel;

    public int value;

    private int min;

    private int max;

    private String name;

    public String status;

    /**
     * This constructor is used if a different XML contents file is required.
     *
     * @param args the command line arguments.
     */
    public MetricMain(String[] args) {
        try {
            CommandLineArgumentProcessor.init(args, CommandLineArgumentProcessor.NAME_EQUALS_VALUE);
            metricFrame = new MetricFrame(this);
            EXCEPTION_PROCESSOR.setParentFrame(metricFrame);
            TASK_MONITOR.setTaskMonitorGui(metricFrame.taskMonitorGui);
            LOADER.setMetricFrame(metricFrame);
            this.contentsFile = CommandLineArgumentProcessor.getValue("contentsFile");
            this.navigationView = CommandLineArgumentProcessor.getValue("navigationView");
            String welcomeJarURL = CommandLineArgumentProcessor.getValue("welcomePage");
            metricFrame.getNotesPanel().addNotes(WELCOME_TITLE, WELCOME_TIP, LOADER.relativeJarURLStringToURL(welcomeJarURL));
            metricFrame.setNotesWorkingDividerLocation(0);
            metricFrame.setVisible(true);
        } catch (Exception e) {
            EXCEPTION_PROCESSOR.process(e);
        }
    }

    /**
     * This constructor is used if no .
     *
     * @param args the command line arguments.
     */
    public MetricMain() {
        try {
            metricFrame = new MetricFrame(this);
            EXCEPTION_PROCESSOR.setParentFrame(metricFrame);
            TASK_MONITOR.setTaskMonitorGui(metricFrame.taskMonitorGui);
            LOADER.setMetricFrame(metricFrame);
            this.contentsFile = DEFAULT_CONTENTS_FILE;
            this.navigationView = "imperial";
            String welcomeJarURL = WELCOME_JARURL;
            metricFrame.getNotesPanel().addNotes(WELCOME_TITLE, WELCOME_TIP, LOADER.relativeJarURLStringToURL(welcomeJarURL));
            metricFrame.setNotesWorkingDividerLocation(0);
            metricFrame.setVisible(true);
        } catch (Exception e) {
            EXCEPTION_PROCESSOR.process(e);
        }
    }

    /**
     * This method is called by the <code>MetricAppLauncher</code> immediately after it has called
     * the <code>MetricMain</code> constructor.
     */
    public void run() {
        try {
            isAlive = true;
            cancel = false;
            value = 0;
            min = 0;
            max = 90;
            name = "Building navigation tree.";
            status = "Loading contents.xml.gz";
            Thread.yield();
            URL urlContents = LOADER.getURL(contentsFile);
            System.out.println("Contents file: " + urlContents.toString());
            JarURLConnection jurlc = (JarURLConnection) urlContents.openConnection();
            InputStream is = jurlc.getInputStream();
            value = 10;
            status = "Uncompressing contents.xml.gz";
            Thread.yield();
            NavigationTreeBuilder navigationTreeBuilder = new NavigationTreeBuilder(this, is);
            navigationTreeBuilder.run();
            value = 60;
            status = "Generating tree view.";
            Thread.yield();
            ntnRoot = navigationTreeBuilder.getNavigationTreeRoot();
            navigationTreeViewSetter = new NavigationTreeViewSetter(ntnRoot, metricFrame.getNavigationPanel());
            value = 70;
            status = "Displaying tree";
            Thread.yield();
            if (navigationView.equals("edexcel")) {
                navigationTreeViewSetter.setNavigationTreeView(NavigationTreeView.EDEXCEL_SYLLABUS_ACTIVITIES_ONLY);
            } else if (navigationView.equals("topic")) {
                metricFrame.navViewPanel.jbutTopic.setSelected(true);
                metricFrame.navViewPanel.enableControls();
                navigationTreeViewSetter.setNavigationTreeView(NavigationTreeView.SUBJECT_ACTIVITIES_ONLY);
            } else {
                navigationTreeViewSetter.setNavigationTreeView(NavigationTreeView.DEFAULT);
            }
            metricFrame.setNavVsWorkingAndNotesDividerLocation();
            value = 80;
            status = "Finishing up.";
            Thread.yield();
            metricFrame.setUpSearchPanel(navigationTreeBuilder.getNameSearchHashMap(), navigationTreeBuilder.getSynopsisSearchHashMap());
        } catch (Exception e) {
            EXCEPTION_PROCESSOR.process(e);
        } finally {
            isAlive = false;
        }
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void cancel() {
        cancel = true;
    }

    public boolean getCanceled() {
        return cancel;
    }

    public int getValue() {
        return value;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    /**
     * This method calls <code>System.exit(int exitCode)</code> with
     * the exit code given as a parameter
     *
     * @param exitCode use one of this classes static fields.
     */
    public void exit(final int exitCode) {
        System.out.println("Exiting with exit code: " + exitCode);
        System.exit(exitCode);
    }

    /**
     * The <code>ActionListener</code> interface method. This is called when
     * the user wants to quit the metric application.
     *
     * This method checks that other application threads are not still running before exiting.
     * If there are other threads confirmation of exit is requested.
     */
    public void actionPerformed(final ActionEvent ae) {
        if (METRIC_MAIN_THREAD_GROUP.activeCount() > 0) {
            System.out.println("Question exit as there is more than one active thread.");
            if (JOptionPane.showConfirmDialog(null, "There are other threads still running. Exit program anyway?", "Confirm Exit", JOptionPane.WARNING_MESSAGE, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
                exit(EXIT_CODE_ERROR_THREADS_STILL_RUNNING);
            }
        } else {
            exit(EXIT_CODE_OK);
        }
    }
}
