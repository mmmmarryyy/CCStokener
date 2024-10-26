package marla.ide.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import marla.ide.latex.LatexExporter;
import marla.ide.problem.MarlaException;
import marla.ide.operation.Operation;
import marla.ide.operation.OperationInfoRequiredException;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import marla.ide.problem.DataSource;
import marla.ide.problem.InternalMarlaException;
import marla.ide.problem.Problem;
import marla.ide.resource.BuildInfo;
import marla.ide.resource.Configuration.ConfigType;
import marla.ide.resource.ConfigurationException;
import marla.ide.resource.BackgroundThread;
import marla.ide.resource.Configuration;
import marla.ide.resource.DebugThread;

/**
 * Interactions that are related but not directly tied to the front-end of the
 * user interface.
 *
 * @author Alex Laird
 */
public class Domain {

    /** The name of the application.*/
    public static final String NAME = "maRla IDE";

    /** The version number of the application.*/
    public static final String VERSION = "1.0";

    /** The pre-release version name of the application.*/
    public static final String PRE_RELEASE = "";

    /** The location of the application as it runs.*/
    public static final String CWD = System.getProperty("user.dir");

    /** The name of the operating system being used.*/
    public static final String OS_NAME = System.getProperty("os.name");

    /** The home directory for the current user.*/
    public static final String HOME_DIR = System.getProperty("user.home");

    /** The relative path to the images folder within the source.*/
    public static final String IMAGES_DIR = "/marla/ide/images/";

    /** The full time format for debug output.*/
    public static final SimpleDateFormat FULL_TIME_FORMAT = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");

    /** The logger holds all caught exceptions for recording in the log file.*/
    public static final Queue<Throwable> logger = new ArrayDeque<Throwable>(5);

    /** The extensions file filter for CSV files.*/
    protected ExtensionFileFilter marlaFilter = new ExtensionFileFilter("maRla IDE Project Files (.marla)", new String[] { "MARLA" });

    /** The extensions file filter for PDF files.*/
    protected ExtensionFileFilter pdfFilter = new ExtensionFileFilter("PDF Files (.pdf)", new String[] { "PDF" });

    /** The extensions file filter for LaTeX files.*/
    protected ExtensionFileFilter latexFilter = new ExtensionFileFilter("LaTeX Sweave Files (.Rnw)", new String[] { "RNW" });

    /** Debug mode */
    public static boolean debug = false;

    /** First run of maRla */
    public static boolean firstRun = true;

    /** Domain object currently created. Only one allowed, ever */
    public static Domain currDomain = null;

    /** The reference to the view of the application.*/
    public ViewPanel viewPanel;

    /**
	 * Server to send maRla exceptions to
	 */
    private static String errorServerURL = null;

    /**
	 * Whether to send stack traces to marla servers
	 */
    private static boolean sendErrorReport = true;

    /**
	 * Whether to include, if applicable, the problem XML in the
	 * error report
	 */
    private static boolean includeProbInReport = true;

    /** The last good problem directory.*/
    public static String lastGoodDir = HOME_DIR;

    /** The error file that keeps track of all errors and their occurrences.*/
    protected File logFile;

    /** The desktop object for common desktop operations.*/
    protected Desktop desktop;

    /** The load/save thread that is continually running unless explicitly paused or stopped.*/
    protected BackgroundThread backgroundThread;

    /** The debug redirection thread that is continually running unless explicitly paused or stopped.*/
    protected DebugThread debugThread;

    /** The user can only have one problem open a time, so here is our problem object reference.*/
    protected Problem problem = null;

    /** Set to true when an export is canceled, false otherwise.*/
    protected static boolean cancelExport = false;

    /**
	 * Construct the domain with the view reference.
	 *
	 * @param viewPanel The view panel for this application.
	 */
    public Domain(ViewPanel viewPanel) {
        if (currDomain != null) throw new InternalMarlaException("Multiple domain instances created");
        currDomain = this;
        this.viewPanel = viewPanel;
        if (Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();
        }
        final Domain domain = this;
        Problem.setDomain(domain);
        logFile = new File("log.dat");
    }

    /**
	 * Gets the current instance of Domain (null if there is none)
	 * @return Current Domain
	 */
    public static Domain getInstance() {
        return currDomain;
    }

    /**
	 * Retrieve a pretty string representation of the full exception details.
	 * 
	 * @param ex The thrown exception.
	 * @return A pretty string representation of the exception.
	 */
    public static String prettyExceptionDetails(Exception ex) {
        String string = "Error: " + ex.getClass() + "\n";
        string += "Message: " + ex.getMessage() + "\n--\nTrace:\n";
        Object[] trace = ex.getStackTrace();
        for (int j = 0; j < trace.length; ++j) {
            string += ("  " + trace[j].toString() + "\n");
        }
        string += "\n";
        return string;
    }

    /**
	 * Show a standard input dialog and return the value the user enters.
	 * 
	 * @param parent The parent of the dialog to be shown.
	 * @param message The standard message to be shown.
	 * @param title The title of the dialog.
	 * @param oldValue The old value to put in as the default input.
	 * @return The input entered by the user on close of the dialog.
	 */
    public static Object showInputDialog(Component parent, Object message, String title, String oldValue) {
        return JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE, null, null, oldValue);
    }

    /**
	 * Show a standard input dialog with combo options and return the value the user enters.
	 * 
	 * @param parent The parent of the dialog to be shown.
	 * @param message The standard message to be shown.
	 * @param items The list of items to display in the JComboBox.
	 * @param title The title of the dialog.
	 * @param initialSelection The first item to have selected in the JComboBox, if any.
	 * @return The input entered by the user on close of the dialog.
	 */
    public static Object showComboDialog(Component parent, Object message, Object[] items, String title, Object initialSelection) {
        return JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE, null, items, initialSelection);
    }

    /**
	 * Show the standard multi-line dialog and return the value the user enters.
	 * 
	 * @param parent The parent of the dialog to be shown.
	 * @param message The standard message to be shown.
	 * @param title The title of the dialog.
	 * @param oldValue The old value to put in as the default input.
	 * @return The input entered by the user on close of the dialog.
	 */
    public static String showMultiLineInputDialog(Component parent, String message, String title, String oldValue) {
        return InputDialog.launchInputDialog(ViewPanel.getInstance(), parent, message, title, oldValue);
    }

    /**
	 * Show the standard confirmation dialog.
	 * 
	 * @param parent The parent of the dialog to be shown.
	 * @param message The standard message to be shown.
	 * @param title The title of the dialog.
	 * @param optionType The confirmation option types to be shown (constant variables from the JOptionPane).
	 * @return The response from the confirmation dialog.
	 */
    public static int showConfirmDialog(Component parent, String message, String title, int optionType) {
        return showConfirmDialog(parent, message, title, optionType, JOptionPane.QUESTION_MESSAGE);
    }

    /**
	 * Display a confirmation dialog and return the option chosen.
	 * 
	 * @param parent The parent of the dialog to be shown.
	 * @param message The standard message to be shown.
	 * @param title The title of the dialog.
	 * @param optionType The confirmation option types to be shown (constant variables from the JOptionPane).
	 * @return The response from the confirmation dialog.
	 */
    public static int showConfirmDialog(Component parent, String message, String title, int optionType, int iconType) {
        return JOptionPane.showConfirmDialog(parent, message, title, optionType, iconType);
    }

    /**
	 * Display a standard error dialog.
	 * 
	 * @param parent The parent of the dialog to be shown.
	 * @param message The standard message to be shown.
	 * @param title The title of the dialog.
	 */
    public static void showErrorDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
	 * Display an error dialog with an inner details collapsable scroll pane.
	 * 
	 * @param parent The parent of the dialog to be shown.
	 * @param message The standard message to be shown.
	 * @param details The inner status message to be shown/hidden.
	 * @param title The title of the dialog.
	 */
    public static void showErrorDialog(Component parent, String message, String details, String title) {
        JOptionPane.showMessageDialog(parent, Domain.createDetailedDisplayObject(message, details), title, JOptionPane.ERROR_MESSAGE);
    }

    /**
	 * Display a standard warning dialog.
	 * 
	 * @param parent The parent of the dialog to be shown.
	 * @param message The standard message to be shown.
	 * @param title The title of the dialog.
	 */
    public static void showWarningDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    /**
	 * Display a warning dialog with an inner details collapsable scroll pane.
	 * 
	 * @param parent The parent of the dialog to be shown.
	 * @param message The standard message to be shown.
	 * @param details The inner status message to be shown/hidden.
	 * @param title The title of the dialog.
	 */
    public static void showWarningDialog(Component parent, String message, String details, String title) {
        JOptionPane.showMessageDialog(parent, Domain.createDetailedDisplayObject(message, details), title, JOptionPane.WARNING_MESSAGE);
    }

    /**
	 * Display a standard information dialog.
	 * 
	 * @param parent The parent of the dialog to be shown.
	 * @param message The standard message to be shown.
	 * @param title The title of the dialog.
	 */
    public static void showInformationDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    /**
	 * Gets the currently set server to send error reports to
	 * @return Current error server
	 */
    public static String getErrorServer() {
        return errorServerURL;
    }

    /**
	 * Sets the URL to send error reports to. URL is checked and must respond properly
	 * @param newServer URL to send reports to
	 * @return Previously set value for server
	 */
    public static String setErrorServer(String newServer) {
        String old = errorServerURL;
        try {
            URL url = new URL(newServer);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder page = new StringBuilder();
            String line = null;
            while ((line = rd.readLine()) != null) {
                page.append(line);
            }
            rd.close();
            if (!page.toString().equals("maRla")) throw new ConfigurationException("URL given for error server is invalid", ConfigType.ErrorServer);
        } catch (UnknownHostException ex) {
            System.out.println("Accepting setting for error sever, unable to check");
        } catch (MalformedURLException ex) {
            throw new ConfigurationException("URL given for error server ('" + newServer + "') appears invalid", ConfigType.ErrorServer, ex);
        } catch (IOException ex) {
            throw new ConfigurationException("URL given for error server could not be reached", ConfigType.ErrorServer, ex);
        }
        errorServerURL = newServer;
        return old;
    }

    /**
	 * Gets whether log writer will send exception reports to maRla servers
	 * @return true if exceptions are sent, false if they are only logged locally
	 */
    public static boolean getSendReport() {
        return sendErrorReport;
    }

    /**
	 * Sets whether the log file writer should also send exceptions
	 * to maRla servers
	 * @param send true to send, false otherwise
	 * @return Previously set value for sending
	 */
    public static boolean setSendReport(boolean send) {
        boolean old = sendErrorReport;
        sendErrorReport = send;
        return old;
    }

    /**
	 * Gets the current debug mode
	 * @return Current debug mode. True if debugging, false otherwise
	 */
    public static boolean isDebugMode() {
        return debug;
    }

    /**
	 * Sets the current debug mode
	 * @param newMode New debug mode
	 * @return Previous debug mode. True if debugging, false otherwise
	 */
    public static boolean isDebugMode(boolean newMode) {
        boolean old = debug;
        debug = newMode;
        if (getInstance() != null) {
            JScrollPane debugPane = getInstance().viewPanel.debugScrollPane;
            JSplitPane split = getInstance().viewPanel.workspaceSplitPane;
            if (debug) {
                if (debugPane.getParent() != split) split.add(debugPane);
                split.setDividerLocation(split.getHeight() - 100);
                getInstance().debugThread.enableDebugRedirect();
                System.out.println("Sending debug output to interface");
                System.out.println(Domain.NAME + " " + Domain.VERSION + " " + Domain.PRE_RELEASE);
                System.out.println("Revision " + BuildInfo.revisionNumber + ", built " + BuildInfo.timeStamp + "\n");
            } else {
                if (debugPane.getParent() == split) split.remove(debugPane);
                split.setDividerLocation(-1);
                getInstance().debugThread.disableDebugRedirect();
            }
        }
        return old;
    }

    /**
	 * Returns whether this is the first time maRla has run
	 * @return true if first run, false otherwise
	 */
    public static boolean isFirstRun() {
        return firstRun;
    }

    /**
	 * Sets whether this is the first run of maRla
	 * @param newMode New value for first run
	 * @return true if first run was set, false otherwise
	 */
    public static boolean isFirstRun(boolean newMode) {
        boolean old = firstRun;
        firstRun = newMode;
        return old;
    }

    /**
	 * Gets the last used browse location
	 * @return Last location the user browsed to in a dialog
	 */
    public static String lastBrowseLocation() {
        return lastGoodDir;
    }

    /**
	 * Sets a new location that was "good" (selected and used) for the
	 * browse dialogs.
	 * @param newLoc New path to use
	 * @return Previously set path
	 */
    public static String lastBrowseLocation(String newLoc) {
        String old = lastGoodDir;
        File loc = new File(newLoc);
        if (loc.isDirectory()) lastGoodDir = loc.toString(); else lastGoodDir = loc.getParent();
        return old;
    }

    /**
	 * Set the title of the progress frame.
	 *
	 * @param string The string to set the title with.
	 */
    public static void setProgressTitle(final String string) {
        if (MainFrame.progressFrame != null) {
            MainFrame.progressFrame.setTitle(string);
        }
    }

    /**
	 * Show or hide the progress bar. Must be set to false if you wish
	 * to set values manually.
	 *
	 * @param visible True for an indeterminate progress bar, false otherwise.
	 */
    public static void setProgressVisible(final boolean visible) {
        if (MainFrame.progressFrame != null) {
            MainFrame.progressFrame.setLocationRelativeTo(Domain.getTopWindow());
            MainFrame.progressFrame.setVisible(visible);
        }
    }

    /**
	 * Enable or disable the indeterminate state of the progress bar. Must be
	 * set to false if you wish to set values manually.
	 *
	 * @param indeterminate True for an indeterminate progress bar, false otherwise.
	 */
    public static void setProgressIndeterminate(final boolean indeterminate) {
        if (MainFrame.progressFrame != null) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    MainFrame.progressFrame.progressBar.setIndeterminate(indeterminate);
                }
            });
        }
    }

    /**
	 * Set the string shown within the progress bar.
	 *
	 * @param string The string shown within the progress bar.
	 */
    public static void setProgressString(final String string) {
        if (MainFrame.progressFrame != null) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    MainFrame.progressFrame.progressBar.setString(string);
                }
            });
        }
    }

    /**
	 * Set the status label below the progress bar.
	 *
	 * @param status The status to be set.
	 */
    public static void setProgressStatus(String status) {
        if (MainFrame.progressFrame != null) MainFrame.progressFrame.statusLabel.setText(status);
    }

    /**
	 * The minimum value a progress can be.  Minimum has been set to 0 by default,
	 * so only use this accessor if you want to change that value.
	 *
	 * @param minValue The minimum value of the progress bar.
	 */
    public static void setProgressMinValue(int minValue) {
        if (MainFrame.progressFrame != null) MainFrame.progressFrame.progressBar.setMinimum(minValue);
    }

    /**
	 * The maximum value a progress can be.  Maximum has been set to 100 by default,
	 * so only use this accessor if you want to change that value.
	 *
	 * @param maxValue The maximum value of the progress bar.
	 */
    public static void setProgressMaxValue(int maxValue) {
        if (MainFrame.progressFrame != null) MainFrame.progressFrame.progressBar.setMaximum(maxValue);
    }

    /**
	 * The current value of the progress bar.  Make sure min and max values have
	 * been set first so the progress bar scales properly.  To make use of the
	 * scalable progress bar, ensure indeterminate is set to false.
	 *
	 * @param value The value of the progress bar.
	 */
    public static void setProgressValue(final int value) {
        if (MainFrame.progressFrame != null) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    MainFrame.progressFrame.progressBar.setValue(value);
                }
            });
        }
    }

    /**
	 * Update the status shown in the workspace panel.
	 *
	 * @param status The status to set in the workspace panel.
	 */
    public void setWorkspaceStatus(String status) {
        viewPanel.statusLabel.setText("<html>" + status + "</html>");
        viewPanel.statusLabel.setSize(viewPanel.statusLabel.getPreferredSize());
    }

    /**
	 * Gets whether log writer will include the current problem in its reports
	 * to maRla servers
	 * @return true if the problem is included, false otherwise
	 */
    public static boolean getReportIncludesProblem() {
        return includeProbInReport;
    }

    /**
	 * Sets whether reports sent to the maRla servers include the XML for the
	 * current problem
	 * @param include true to include the current problem in the report, false
	 *		otherwise
	 * @return Previously set value for including problem
	 */
    public static boolean setReportIncludesProblem(boolean include) {
        boolean old = includeProbInReport;
        includeProbInReport = include;
        return old;
    }

    /**
	 * Writes any current log file out to disk and clears the logger (so
	 * that the same exceptions won't be written again)
	 */
    public void flushLog() {
        flushLog(logger, debug, logFile, errorServerURL, (includeProbInReport ? problem : null));
    }

    /**
	 * Writes the given log to the various sources
	 * @param log Exception log to write
	 * @param toConsole if true, sends trace to standard out
	 * @param logFile if not null, writes tace to given file
	 * @param errorServer if not null, sends trace to given error server
	 * @param currProb if not null, sends given problem along with report to server
	 */
    public static synchronized void flushLog(Queue<Throwable> log, boolean toConsole, File logFile, String errorServer, Problem currProb) {
        if (log.isEmpty()) return;
        PrintWriter out = null;
        if (logFile != null) {
            try {
                out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
                Date date = new Date();
                out.write("Date: " + FULL_TIME_FORMAT.format(date) + "\n");
            } catch (IOException ex) {
                out = null;
                System.err.println("Unable to write error log file: " + ex.getMessage());
            }
        }
        String probCache = null;
        String confCache = null;
        if (currProb != null) {
            try {
                Document doc = new Document(currProb.toXml());
                Format formatter = Format.getPrettyFormat();
                formatter.setEncoding("UTF-8");
                XMLOutputter xml = new XMLOutputter(formatter);
                probCache = xml.outputString(doc);
            } catch (MarlaException ex) {
                probCache = "Unable to get problem XML: " + ex.getMessage();
            }
        }
        try {
            confCache = Configuration.getInstance().getConfigXML();
        } catch (MarlaException ex) {
            confCache = "Unable to get config XML: " + ex.getMessage();
        }
        while (!log.isEmpty()) {
            Throwable ex = log.remove();
            if (toConsole) ex.printStackTrace(System.out);
            if (out != null) ex.printStackTrace(out);
            if (errorServer != null) sendExceptionToServer(errorServer, ex, confCache, probCache);
        }
        if (out != null) {
            out.flush();
            out.close();
        }
    }

    /**
	 * Sends the given exception to the error server
	 */
    private static void sendExceptionToServer(String server, Throwable ex, String config, String prob) {
        try {
            StringBuilder dataSB = new StringBuilder();
            dataSB.append(URLEncoder.encode("secret", "UTF-8"));
            dataSB.append('=');
            dataSB.append(URLEncoder.encode("badsecurity", "UTF-8"));
            dataSB.append('&');
            dataSB.append(URLEncoder.encode("version", "UTF-8"));
            dataSB.append('=');
            dataSB.append(URLEncoder.encode(BuildInfo.revisionNumber, "UTF-8"));
            dataSB.append('&');
            dataSB.append(URLEncoder.encode("os", "UTF-8"));
            dataSB.append('=');
            dataSB.append(URLEncoder.encode(System.getProperty("os.name") + " " + System.getProperty("os.version"), "UTF-8"));
            dataSB.append('&');
            dataSB.append(URLEncoder.encode("user", "UTF-8"));
            dataSB.append('=');
            dataSB.append(URLEncoder.encode(System.getProperty("user.name"), "UTF-8"));
            dataSB.append('&');
            dataSB.append(URLEncoder.encode("msg", "UTF-8"));
            dataSB.append('=');
            dataSB.append(URLEncoder.encode(ex.getMessage(), "UTF-8"));
            ByteArrayOutputStream trace = new ByteArrayOutputStream();
            ex.printStackTrace(new PrintStream(trace));
            dataSB.append('&');
            dataSB.append(URLEncoder.encode("trace", "UTF-8"));
            dataSB.append('=');
            dataSB.append(URLEncoder.encode(trace.toString(), "UTF-8"));
            if (config != null) {
                dataSB.append('&');
                dataSB.append(URLEncoder.encode("config", "UTF-8"));
                dataSB.append('=');
                dataSB.append(URLEncoder.encode(config, "UTF-8"));
            }
            if (prob != null) {
                dataSB.append('&');
                dataSB.append(URLEncoder.encode("problem", "UTF-8"));
                dataSB.append('=');
                dataSB.append(URLEncoder.encode(prob, "UTF-8"));
            }
            URL url = new URL(errorServerURL);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(dataSB.toString());
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = null;
            String line = null;
            while ((line = rd.readLine()) != null) {
                if (response == null) response = line; else System.out.println(line);
            }
            wr.close();
            rd.close();
            if (response.equals("success")) System.out.println("Exception sent to maRla development team"); else System.out.println("Unable to send exception to development team: " + response);
        } catch (IOException ex2) {
            System.out.println("Unable to send exception to development team: " + ex2.getMessage());
        }
    }

    /**
	 * Retrieves the reference to the currently open problem.
	 *
	 * @return The currently open problem object.
	 */
    public Problem getProblem() {
        return problem;
    }

    /**
	 * If a problem has been defined, this will call the problems save method,
	 * otherwise it does nothing.
	 */
    public void save() {
        if (problem != null) {
            try {
                problem.save();
            } catch (MarlaException ex) {
                logger.add(ex);
                Domain.showErrorDialog(Domain.getTopWindow(), ex.getMessage(), Domain.prettyExceptionDetails(ex), "Unable to Save");
            }
        }
    }

    /**
	 * Returns true if the problem is being edited, false if it is a new problem.
	 *
	 * @return True for editing, false for new problem.
	 */
    public boolean isEditing() {
        if (viewPanel.newProblemWizardDialog.newProblem != null) {
            return false;
        } else {
            return true;
        }
    }

    /**
	 * Tells the view to rebuild and repaint the given tree.
	 * @param dataSet
	 */
    public void rebuildTree(DataSource dataSet) {
        viewPanel.rebuildTree(dataSet);
    }

    /**
	 * Tells the view to rebuild and repaint the entire workspace.
	 */
    public void rebuildWorkspace() {
        viewPanel.rebuildWorkspace();
    }

    /**
	 * Validates the undo/redo menu items in MainFrame to see if they should be
	 * enabled or disabled.
	 */
    public void validateUndoRedoMenuItems() {
        if (viewPanel.undoRedo.hasUndo()) {
            viewPanel.mainFrame.undoMenuItem.setEnabled(true);
        } else {
            viewPanel.mainFrame.undoMenuItem.setEnabled(false);
        }
        if (viewPanel.undoRedo.hasRedo()) {
            viewPanel.mainFrame.redoMenuItem.setEnabled(true);
        } else {
            viewPanel.mainFrame.redoMenuItem.setEnabled(false);
        }
    }

    /**
	 * Marks that a change is beginning, so the step should be saved in undo/redo.
	 * @param changeMsg Message describing the change
	 */
    public void changeBeginning(String changeMsg) {
        if (viewPanel.newProblemWizardDialog.newProblem == null && problem != null) viewPanel.undoRedo.addUndoStep(problem.clone(), changeMsg);
        validateUndoRedoMenuItems();
    }

    /**
	 * Mark the View as unsaved.
	 */
    public void markUnsaved() {
        if (isEditing()) {
            viewPanel.saveButton.setEnabled(true);
        }
    }

    /**
	 * Mark the View as saved.
	 */
    public void markSaved() {
        viewPanel.saveButton.setEnabled(false);
    }

    /**
	 * Allows the user to specify a new file to save the problem as.
	 */
    protected void saveAs() {
        if (problem != null) {
            viewPanel.fileChooserDialog.setDialogTitle("Save Problem As");
            viewPanel.fileChooserDialog.setDialogType(JFileChooser.SAVE_DIALOG);
            viewPanel.fileChooserDialog.resetChoosableFileFilters();
            viewPanel.fileChooserDialog.setFileFilter(marlaFilter);
            viewPanel.fileChooserDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
            viewPanel.fileChooserDialog.setCurrentDirectory(new File(problem.getFileName()));
            viewPanel.fileChooserDialog.setSelectedFile(new File(problem.getFileName()));
            int response = viewPanel.fileChooserDialog.showSaveDialog(Domain.getTopWindow());
            while (response == JFileChooser.APPROVE_OPTION) {
                File file = viewPanel.fileChooserDialog.getSelectedFile();
                if (file.getName().indexOf(".") == -1) {
                    file = new File(viewPanel.fileChooserDialog.getSelectedFile().toString() + ".marla");
                }
                if (!file.toString().toLowerCase().endsWith(".marla")) {
                    Domain.showWarningDialog(Domain.getTopWindow(), "The extension for the file must be .marla.", "Invalid Extension");
                    viewPanel.fileChooserDialog.setSelectedFile(new File(viewPanel.fileChooserDialog.getSelectedFile().toString().substring(0, viewPanel.fileChooserDialog.getSelectedFile().toString().lastIndexOf(".")) + ".marla"));
                    response = viewPanel.fileChooserDialog.showSaveDialog(Domain.getTopWindow());
                    continue;
                }
                boolean continueAllowed = true;
                if (file.exists()) {
                    response = Domain.showConfirmDialog(Domain.getTopWindow(), "The selected file already exists.\nWould you like to overwrite the existing file?", "Overwrite Existing File", JOptionPane.YES_NO_OPTION);
                    if (response != JOptionPane.YES_OPTION) {
                        continueAllowed = false;
                    }
                }
                if (continueAllowed) {
                    problem.setFileName(file.toString());
                    viewPanel.mainFrame.setTitle(viewPanel.mainFrame.getDefaultTitle() + " - " + problem.getFileName().substring(problem.getFileName().lastIndexOf(System.getProperty("file.separator")) + 1, problem.getFileName().lastIndexOf(".")));
                    save();
                    break;
                } else {
                    continue;
                }
            }
        }
    }

    /**
	 * Export a data set to a CSV file.
	 */
    protected void exportDataSet() {
        Object[] dataSets = new Object[problem.getDataCount()];
        for (int i = 0; i < problem.getDataCount(); ++i) {
            dataSets[i] = problem.getData(i).getName();
        }
        final Object resp = Domain.showComboDialog(Domain.getTopWindow(), "Select the data set you would like to export:", dataSets, "Export Data Set", null);
        if (resp != null) {
            viewPanel.fileChooserDialog.setDialogTitle("Export Data Set");
            viewPanel.fileChooserDialog.setDialogType(JFileChooser.SAVE_DIALOG);
            viewPanel.fileChooserDialog.resetChoosableFileFilters();
            viewPanel.fileChooserDialog.setFileFilter(viewPanel.newProblemWizardDialog.csvFilter);
            viewPanel.fileChooserDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
            viewPanel.fileChooserDialog.setCurrentDirectory(new File(Domain.lastGoodDir));
            viewPanel.fileChooserDialog.setSelectedFile(new File(Domain.lastGoodDir, resp.toString()));
            int response = viewPanel.fileChooserDialog.showSaveDialog(Domain.getTopWindow());
            while (response == JFileChooser.APPROVE_OPTION) {
                File file = viewPanel.fileChooserDialog.getSelectedFile();
                if (file.getName().indexOf(".") == -1) {
                    file = new File(viewPanel.fileChooserDialog.getSelectedFile().toString() + ".csv");
                }
                final File finalFile = file;
                if (!finalFile.toString().toLowerCase().endsWith(".csv")) {
                    Domain.showWarningDialog(Domain.getTopWindow(), "The extension for the file must be .csv.", "Invalid Extension");
                    viewPanel.fileChooserDialog.setSelectedFile(new File(viewPanel.fileChooserDialog.getSelectedFile().toString().substring(0, viewPanel.fileChooserDialog.getSelectedFile().toString().lastIndexOf(".")) + ".csv"));
                    response = viewPanel.fileChooserDialog.showSaveDialog(Domain.getTopWindow());
                    continue;
                }
                boolean continueAllowed = true;
                if (file.exists()) {
                    response = Domain.showConfirmDialog(Domain.getTopWindow(), "The selected file already exists.\nWould you like to overwrite the existing file?", "Overwrite Existing File", JOptionPane.YES_NO_OPTION);
                    if (response != JOptionPane.YES_OPTION) {
                        continueAllowed = false;
                    }
                }
                if (continueAllowed) {
                    Domain.setProgressTitle("Exporting");
                    Domain.setProgressVisible(true);
                    Domain.setProgressIndeterminate(true);
                    Domain.setProgressString("");
                    Domain.setProgressStatus("Beginning CSV export...");
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            String filePath = null;
                            try {
                                problem.getData(resp.toString()).exportFile(finalFile.getCanonicalPath());
                                filePath = finalFile.getCanonicalPath();
                                if (desktop != null) {
                                    Domain.setProgressStatus("Opening CSV...");
                                    desktop.open(new File(finalFile.getCanonicalPath()));
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ex) {
                                    }
                                }
                            } catch (IOException ex) {
                                if (filePath != null) {
                                    Domain.setProgressIndeterminate(false);
                                    Domain.showInformationDialog(Domain.getTopWindow(), "The file was exported successfully.\nLocation: " + filePath, "Export Successful");
                                } else {
                                    Domain.logger.add(ex);
                                    Domain.showErrorDialog(Domain.getTopWindow(), ex.getMessage(), Domain.prettyExceptionDetails(ex), "PDF Export Failed");
                                }
                            } finally {
                                Domain.setProgressVisible(false);
                                Domain.setProgressIndeterminate(false);
                            }
                        }
                    }).start();
                    break;
                } else {
                    continue;
                }
            }
        }
    }

    /**
	 * Export the current problem to a PDF file through Latex's commands.
	 */
    protected void exportToPdf() {
        if (problem != null) {
            viewPanel.fileChooserDialog.setDialogTitle("Export to PDF");
            viewPanel.fileChooserDialog.setDialogType(JFileChooser.SAVE_DIALOG);
            viewPanel.fileChooserDialog.resetChoosableFileFilters();
            viewPanel.fileChooserDialog.setFileFilter(pdfFilter);
            viewPanel.fileChooserDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
            viewPanel.fileChooserDialog.setCurrentDirectory(new File(problem.getFileName().substring(0, problem.getFileName().lastIndexOf(".")) + ".pdf"));
            viewPanel.fileChooserDialog.setSelectedFile(new File(problem.getFileName().substring(0, problem.getFileName().lastIndexOf(".")) + ".pdf"));
            int response = viewPanel.fileChooserDialog.showSaveDialog(Domain.getTopWindow());
            while (response == JFileChooser.APPROVE_OPTION) {
                File file = viewPanel.fileChooserDialog.getSelectedFile();
                if (file.getName().indexOf(".") == -1) {
                    file = new File(viewPanel.fileChooserDialog.getSelectedFile().toString() + ".pdf");
                }
                final File finalFile = file;
                if (!finalFile.toString().toLowerCase().endsWith(".pdf")) {
                    Domain.showWarningDialog(Domain.getTopWindow(), "The extension for the file must be .pdf.", "Invalid Extension");
                    viewPanel.fileChooserDialog.setSelectedFile(new File(viewPanel.fileChooserDialog.getSelectedFile().toString().substring(0, viewPanel.fileChooserDialog.getSelectedFile().toString().lastIndexOf(".")) + ".pdf"));
                    response = viewPanel.fileChooserDialog.showSaveDialog(Domain.getTopWindow());
                    continue;
                }
                boolean continueAllowed = true;
                if (finalFile.exists()) {
                    response = Domain.showConfirmDialog(Domain.getTopWindow(), "The selected file already exists.\nWould you like to overwrite the existing file?", "Overwrite Existing File", JOptionPane.YES_NO_OPTION);
                    if (response != JOptionPane.YES_OPTION) {
                        continueAllowed = false;
                    }
                }
                if (continueAllowed) {
                    Domain.setProgressTitle("Exporting");
                    Domain.setProgressVisible(true);
                    Domain.setProgressIndeterminate(true);
                    Domain.setProgressString("");
                    Domain.setProgressStatus("Beginning PDF export...");
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            String filePath = null;
                            try {
                                for (int i = 0; i < problem.getDataCount(); i++) {
                                    List<Operation> ops = problem.getData(i).getAllLeafOperations();
                                    for (Operation op : ops) {
                                        ensureRequirementsMet(op);
                                        if (Domain.cancelExport) {
                                            break;
                                        }
                                    }
                                }
                                if (!Domain.cancelExport) {
                                    LatexExporter exporter = new LatexExporter(problem);
                                    File genFile = new File(exporter.exportPDF(finalFile.getPath()));
                                    filePath = genFile.getCanonicalPath();
                                    if (desktop != null) {
                                        Domain.setProgressStatus("Opening PDF...");
                                        desktop.open(genFile);
                                        try {
                                            Thread.sleep(3000);
                                        } catch (InterruptedException ex) {
                                        }
                                    }
                                } else {
                                    Domain.cancelExport = false;
                                }
                            } catch (IOException ex) {
                                if (filePath != null) {
                                    Domain.setProgressIndeterminate(false);
                                    Domain.showInformationDialog(Domain.getTopWindow(), "The file was exported successfully.\nLocation: " + filePath, "Export Successful");
                                } else {
                                    Domain.logger.add(ex);
                                    Domain.showErrorDialog(Domain.getTopWindow(), ex.getMessage(), Domain.prettyExceptionDetails(ex), "PDF Export Failed");
                                }
                            } catch (MarlaException ex) {
                                Domain.logger.add(ex);
                                Domain.showErrorDialog(Domain.getTopWindow(), ex.getMessage(), Domain.prettyExceptionDetails(ex), "PDF Export Failed");
                            } finally {
                                Domain.setProgressVisible(false);
                                Domain.setProgressIndeterminate(false);
                            }
                        }
                    }).start();
                    break;
                } else {
                    continue;
                }
            }
        }
    }

    /**
	 * Export the current problem for Latex.
	 */
    protected void exportForLatex() {
        if (problem != null) {
            viewPanel.fileChooserDialog.setDialogTitle("Export for LaTeX");
            viewPanel.fileChooserDialog.setDialogType(JFileChooser.SAVE_DIALOG);
            viewPanel.fileChooserDialog.resetChoosableFileFilters();
            viewPanel.fileChooserDialog.setFileFilter(latexFilter);
            viewPanel.fileChooserDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
            viewPanel.fileChooserDialog.setCurrentDirectory(new File(problem.getFileName().substring(0, problem.getFileName().lastIndexOf(".")) + ".Rnw"));
            viewPanel.fileChooserDialog.setSelectedFile(new File(problem.getFileName().substring(0, problem.getFileName().lastIndexOf(".")) + ".Rnw"));
            int response = viewPanel.fileChooserDialog.showSaveDialog(Domain.getTopWindow());
            while (response == JFileChooser.APPROVE_OPTION) {
                File file = viewPanel.fileChooserDialog.getSelectedFile();
                if (file.getName().indexOf(".") == -1) {
                    file = new File(viewPanel.fileChooserDialog.getSelectedFile().toString() + ".Rnw");
                }
                final File finalFile = file;
                if (!finalFile.toString().toLowerCase().endsWith(".rnw")) {
                    Domain.showWarningDialog(Domain.getTopWindow(), "The extension for the file must be .Rnw.", "Invalid Extension");
                    viewPanel.fileChooserDialog.setSelectedFile(new File(viewPanel.fileChooserDialog.getSelectedFile().toString().substring(0, viewPanel.fileChooserDialog.getSelectedFile().toString().lastIndexOf(".")) + ".Rnw"));
                    response = viewPanel.fileChooserDialog.showSaveDialog(Domain.getTopWindow());
                    continue;
                }
                boolean continueAllowed = true;
                if (finalFile.exists()) {
                    response = Domain.showConfirmDialog(Domain.getTopWindow(), "The selected file already exists.\nWould you like to overwrite the existing file?", "Overwrite Existing File", JOptionPane.YES_NO_OPTION);
                    if (response != JOptionPane.YES_OPTION) {
                        continueAllowed = false;
                    }
                }
                if (continueAllowed) {
                    Domain.setProgressTitle("Exporting");
                    Domain.setProgressVisible(true);
                    Domain.setProgressIndeterminate(true);
                    Domain.setProgressString("");
                    Domain.setProgressStatus("Beginning LaTeX export...");
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            String filePath = null;
                            try {
                                for (int i = 0; i < problem.getDataCount(); i++) {
                                    List<Operation> ops = problem.getData(i).getAllLeafOperations();
                                    for (Operation op : ops) {
                                        ensureRequirementsMet(op);
                                    }
                                }
                                LatexExporter exporter = new LatexExporter(problem);
                                File genFile = new File(exporter.cleanExport(finalFile.getPath()));
                                filePath = genFile.getCanonicalPath();
                                if (desktop != null) {
                                    Domain.setProgressStatus("Opening LaTeX RNW...");
                                    desktop.open(genFile);
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException ex) {
                                    }
                                } else {
                                    throw new IOException();
                                }
                            } catch (IOException ex) {
                                if (filePath != null) {
                                    Domain.setProgressIndeterminate(false);
                                    Domain.showInformationDialog(Domain.getTopWindow(), "The file was exported successfully.\nLocation: " + filePath, "Export Successful");
                                } else {
                                    Domain.logger.add(ex);
                                    Domain.showErrorDialog(Domain.getTopWindow(), ex.getMessage(), Domain.prettyExceptionDetails(ex), "Export Failed");
                                }
                            } catch (MarlaException ex) {
                                Domain.logger.add(ex);
                                Domain.showErrorDialog(Domain.getTopWindow(), ex.getMessage(), Domain.prettyExceptionDetails(ex), "Export Failed");
                            } finally {
                                Domain.setProgressVisible(false);
                                Domain.setProgressIndeterminate(false);
                            }
                        }
                    }).start();
                    break;
                } else {
                    continue;
                }
            }
        }
    }

    /**
	 * Presents a file chooser to allow the user to specify a problem to load.
	 */
    public void load() {
        try {
            viewPanel.fileChooserDialog.setDialogTitle("Browse Problem Location");
            viewPanel.fileChooserDialog.setDialogType(JFileChooser.OPEN_DIALOG);
            viewPanel.fileChooserDialog.resetChoosableFileFilters();
            viewPanel.fileChooserDialog.setFileFilter(marlaFilter);
            viewPanel.fileChooserDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
            String curDir = lastGoodDir;
            if (problem != null) {
                curDir = problem.getFileName();
            }
            viewPanel.fileChooserDialog.setSelectedFile(new File(""));
            viewPanel.fileChooserDialog.setCurrentDirectory(new File(curDir));
            int response = viewPanel.fileChooserDialog.showOpenDialog(Domain.getTopWindow());
            while (response == JFileChooser.APPROVE_OPTION) {
                File file = viewPanel.fileChooserDialog.getSelectedFile();
                if (!file.isFile()) {
                    Domain.showWarningDialog(Domain.getTopWindow(), "The specified file does not exist.", "Does Not Exist");
                    int lastIndex = viewPanel.fileChooserDialog.getSelectedFile().toString().lastIndexOf(".");
                    if (lastIndex == -1) {
                        lastIndex = viewPanel.fileChooserDialog.getSelectedFile().toString().length();
                    }
                    viewPanel.fileChooserDialog.setSelectedFile(new File(viewPanel.fileChooserDialog.getSelectedFile().toString().substring(0, lastIndex) + ".marla"));
                    response = viewPanel.fileChooserDialog.showOpenDialog(Domain.getTopWindow());
                    continue;
                }
                if (!file.toString().toLowerCase().endsWith(".marla")) {
                    Domain.showWarningDialog(Domain.getTopWindow(), "The extension for the file must be .marla.", "Invalid Extension");
                    viewPanel.fileChooserDialog.setSelectedFile(new File(viewPanel.fileChooserDialog.getSelectedFile().toString().substring(0, viewPanel.fileChooserDialog.getSelectedFile().toString().lastIndexOf(".")) + ".marla"));
                    response = viewPanel.fileChooserDialog.showOpenDialog(Domain.getTopWindow());
                    continue;
                }
                boolean wantsClose = true;
                if (problem != null) {
                    wantsClose = viewPanel.closeProblem(false, false);
                    if (wantsClose) {
                        viewPanel.undoRedo.clearHistory();
                    }
                }
                if (wantsClose) {
                    if (file.isDirectory()) {
                        lastGoodDir = file.toString();
                    } else {
                        lastGoodDir = file.getParent();
                    }
                    problem = Problem.load(file.toString());
                    viewPanel.openProblem(false, false);
                }
                break;
            }
        } catch (MarlaException ex) {
            Domain.logger.add(ex);
            Domain.showWarningDialog(Domain.getTopWindow(), ex.getMessage(), Domain.prettyExceptionDetails(ex), "Error Loading Save File");
        }
    }

    /**
	 * Ensure the requirements are met for the given operation.
	 *
	 * @param op The operation to check for.
	 * @return True if requirements were met and should continue, false otherwise.
	 */
    public boolean ensureRequirementsMet(Operation op) {
        boolean isSolved = false;
        boolean allSolved = true;
        while (!isSolved) {
            try {
                op.checkCache();
                isSolved = true;
            } catch (OperationInfoRequiredException ex) {
                ViewPanel.getRequiredInfoDialog(ex.getOperation(), true);
                if (Domain.cancelExport) {
                    allSolved = false;
                    break;
                }
            }
        }
        return allSolved;
    }

    /**
	 * Consructs an error object that can be passed as the Message part of a
	 * JOptionPane.  This object will a show/hide button for further details
	 * of a given error message.  The standard message display is passed as
	 * message, and the hidden, scrollable message display is the innerMessage.
	 * 
	 * @param message The standard error message.
	 * @param innerMessage The scrollable inner message.
	 * @return  The object to be placed in the JOptionPane message.
	 */
    private static Object createDetailedDisplayObject(String message, String innerMessage) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setText(innerMessage);
        textArea.setBackground(panel.getBackground());
        textArea.setRows(8);
        textArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(textArea);
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        final JButton button = new JButton("Show Details");
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                if (scrollPane.getParent() == null) {
                    gbc.gridy = 2;
                    gbc.weightx = 1;
                    gbc.weighty = 1;
                    panel.add(scrollPane, gbc);
                    button.setText("Hide Details");
                } else {
                    panel.remove(scrollPane);
                    button.setText("Show Details");
                }
                JDialog dialog = null;
                Container parent = panel.getParent();
                while (parent != null && !(parent instanceof JDialog)) {
                    parent = parent.getParent();
                    if (parent instanceof JDialog) {
                        dialog = (JDialog) parent;
                        break;
                    }
                }
                if (dialog != null) {
                    int height;
                    if (scrollPane.getParent() != null) {
                        height = dialog.getHeight() + scrollPane.getPreferredSize().height;
                    } else {
                        height = dialog.getHeight() - scrollPane.getPreferredSize().height;
                    }
                    dialog.setSize(dialog.getWidth(), height);
                }
            }
        });
        buttonPanel.add(button);
        buttonPanel.add(new JLabel(""));
        buttonPanel.add(new JLabel(""));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JLabel(message), gbc);
        gbc.gridy = 1;
        panel.add(buttonPanel, gbc);
        return panel;
    }

    /**
	 * Checks what window is the top-most being displayed right now and returns that.
	 *
	 * @return The top-most window displayed.
	 */
    public static Container getTopWindow() {
        if (MainFrame.progressFrame.isVisible()) {
            return MainFrame.progressFrame;
        } else {
            ViewPanel instance = ViewPanel.getInstance();
            if (instance != null) {
                if (instance.settingsDialog.isVisible()) {
                    return instance.settingsDialog;
                } else if (instance.newProblemWizardDialog.isVisible()) {
                    return instance.newProblemWizardDialog;
                } else {
                    return instance;
                }
            } else {
                return null;
            }
        }
    }

    /**
	 * Gets the current window frame for maRla
	 * @return Current MainFrame
	 */
    public MainFrame getMainFrame() {
        return viewPanel.mainFrame;
    }
}
