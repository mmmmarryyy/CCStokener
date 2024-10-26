package com.saic.ship.fsml;

import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import org.apache.log4j.*;

/**
 * FsmlValidatorUI is a graphical tool that allows the user to check format of 
 * an FSML file. The tool allows the user to specify a data file and a 
 * corresponding FSML file, and then attempts to process the data file with 
 * the DefaultFsmlProcessor.  If no exceptions are thrown, the tool will 
 * indicate that the FSML file is valid; otherwise, the stack trace is 
 * displayed to the user.
 *
 * @author   Created By: <a href="mailto:bcanaday@ship.saic.com"> Brent Canaday </a>
 * @author   Last Revised By: $Author: bergquistj $
 * @see      FsmlValidation
 * @since    Created On: 2005/03/01
 * @since    Last Revised on: $Date: 2005/03/03 19:59:23 $
 * @version  $Revision: 1.5 $
 *
 * @todo re-factor this class using log4j
 */
public class FsmlValidatorUI extends JFrame {

    /**
   * Logger for this class
   */
    private static Logger logger = Logger.getLogger(FsmlValidatorUI.class);

    /**
   * Main method will instantiate and run FsmlValidatorUI.
   *
   * @param args a <code>String[]</code> value
   */
    public static void main(String[] args) {
        FsmlValidatorUI frame = new FsmlValidatorUI();
        frame.setVisible(true);
    }

    /**
   * Map to store parameters
   *
   */
    private HashMap hmapParameters = null;

    private JPanel jpnlContent = new JPanel();

    /**data file*/
    private JLabel jlblDataFile = new JLabel("Data File");

    private JTextField jtxtDataFile = new JTextField();

    private Action actionBrowseDataFile = new AbstractAction("Browse") {

        public void actionPerformed(ActionEvent e) {
            browseDataFile_actionPerformed();
        }
    };

    private JButton jbtnBrowseDataFile = new JButton(actionBrowseDataFile);

    /**fsml file*/
    private JLabel jlblFsmlFile = new JLabel("FSML File");

    private JTextField jtxtFsmlFile = new JTextField();

    private Action actionBrowseFsmlFile = new AbstractAction("Browse") {

        public void actionPerformed(ActionEvent e) {
            browseFsmlFile_actionPerformed();
        }
    };

    private JButton jbtnBrowseFsmlFile = new JButton(actionBrowseFsmlFile);

    /**validate*/
    private Action actionValidate = new AbstractAction("Validate") {

        public void actionPerformed(ActionEvent e) {
            validate_actionPerformed();
        }
    };

    private JButton jbtnValidate = new JButton(actionValidate);

    /**output text area*/
    private JTextArea jtaErrors = new JTextArea();

    private JScrollPane jscrLog = new JScrollPane(jtaErrors);

    private StringBuffer sbLog = new StringBuffer();

    /**file chooser*/
    private JFileChooser jfc = new JFileChooser(new File(System.getProperty("user.home")));

    /**
   * Constructor calls private init method. Throws execption if encountered.
   *
   */
    public FsmlValidatorUI() {
        try {
            init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
   * Initialize the FsmlValidatorUI. Basically, set up the user interface 
   * components. 
   *
   */
    private void init() {
        this.setTitle("FSML Validator Dialog");
        GridBagConstraints gbc = new GridBagConstraints();
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        try {
            this.jpnlContent.setLayout(new GridBagLayout());
            this.jpnlContent.add(this.jlblDataFile, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, gbc.WEST, gbc.NONE, new Insets(5, 5, 5, 5), 0, 0));
            jtxtDataFile.setPreferredSize(new Dimension(200, 21));
            this.jpnlContent.add(this.jtxtDataFile, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, gbc.WEST, gbc.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
            this.jpnlContent.add(this.jbtnBrowseDataFile, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, gbc.EAST, gbc.NONE, new Insets(5, 5, 5, 5), 0, 0));
            this.jpnlContent.add(this.jlblFsmlFile, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, gbc.WEST, gbc.NONE, new Insets(5, 5, 5, 5), 0, 0));
            jtxtFsmlFile.setPreferredSize(new Dimension(200, 21));
            this.jpnlContent.add(this.jtxtFsmlFile, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, gbc.WEST, gbc.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
            this.jpnlContent.add(this.jbtnBrowseFsmlFile, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, gbc.EAST, gbc.NONE, new Insets(5, 5, 5, 5), 0, 0));
            this.jpnlContent.add(this.jbtnValidate, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, gbc.EAST, gbc.NONE, new Insets(15, 5, 5, 5), 0, 0));
            this.jpnlContent.add(new JSeparator(), new GridBagConstraints(0, 3, 4, 1, 1.0, 0.0, gbc.WEST, gbc.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
            jtaErrors.setBorder(BorderFactory.createLoweredBevelBorder());
            JPanel testPanel = new JPanel();
            testPanel.setBorder(BorderFactory.createTitledBorder("Output Panel"));
            testPanel.setLayout(new BorderLayout());
            testPanel.add(this.jscrLog, BorderLayout.CENTER);
            testPanel.setPreferredSize(new Dimension(700, 400));
            this.getContentPane().add(this.jpnlContent, BorderLayout.NORTH);
            this.getContentPane().add(testPanel, BorderLayout.CENTER);
            this.pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
   * Get a file from the filesystem. Returns a URL for the file. 
   *
   * @param path a <code>String</code> value
   * @return an <code>URL</code> value
   */
    private URL getFile(String path) {
        File file = null;
        URL url = null;
        try {
            if (path.length() == 0 || path.equals("")) {
                log("A Data File and FSML File must be entered.");
                return null;
            }
            log("Checking for local file " + path + " ...");
            file = new File(path);
            if (file.exists()) {
                log("File Found.");
                url = file.toURL();
                return url;
            } else {
                try {
                    log("Attempting to create and open a connection to URL " + path + "...");
                    url = new URL(path);
                    InputStream in = url.openStream();
                    log("Successful connecting to " + url.getPath() + ".");
                    return url;
                } catch (MalformedURLException malex) {
                    log("Malformed URL " + path + ".");
                    return null;
                } catch (IOException ex) {
                    log("Failed to open stream to URL " + url.getPath() + ".");
                    return null;
                } catch (Exception ex) {
                    log("Failed to locate file " + url.getPath() + ".");
                    return null;
                }
            }
        } catch (Exception ex) {
            log("Failed to locate file " + path + ".");
            return null;
        }
    }

    /**
   * Processes a data file and FSML file together using the 
   * DefaultFsmlProcessor. Requries that both data and fsml files be passed.
   *
   * @param data an <code>URL</code> value
   * @param fsml an <code>URL</code> value
   */
    private void processFile(URL data, URL fsml) {
        DefaultFsmlProcessor processor = new DefaultFsmlProcessor();
        FsmlDocument fsmlDoc = new FsmlDocument(fsml.toExternalForm());
        try {
            fsmlDoc.init();
        } catch (Exception ex) {
            ex.printStackTrace();
            fsmlDoc.getErrorHandler().reportErrors();
        }
        hmapParameters = new LinkedHashMap();
        processor.setParameters(hmapParameters);
        try {
            processor.readFile(fsmlDoc, data.openStream());
            Iterator paramNames = hmapParameters.keySet().iterator();
            String paramName;
            log("\n<<< Processing Data File >>>\n");
            while (paramNames.hasNext()) {
                paramName = paramNames.next().toString();
                log(paramName + "=" + hmapParameters.get(paramName));
                logger.debug(paramName + "=" + hmapParameters.get(paramName));
            }
        } catch (Exception ex) {
            Iterator paramNames = hmapParameters.keySet().iterator();
            String paramName;
            while (paramNames.hasNext()) {
                paramName = paramNames.next().toString();
                log(paramName + "=" + hmapParameters.get(paramName));
                logger.debug(paramName + "=" + hmapParameters.get(paramName));
            }
            log("\n<<< Warning!  Error processing file. >>>\n");
            log("The following exception was caught...\n\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.close();
            log(sw.toString());
            ex.printStackTrace();
        }
    }

    /**
   * Internal method for logging errors and exceptions. Written before we 
   * used log4j. Class could probably be refactored to use log4j, but it ain't
   * broke yet, so we're not fixing it. 
   *
   * @param message a <code>String</code> value
   */
    private void log(String message) {
        sbLog.append("\n");
        sbLog.append(message);
        jtaErrors.setText(sbLog.toString());
    }

    /**
   * Clear the log data. 
   *
   */
    private void clearLog() {
        sbLog = new StringBuffer();
        jtaErrors.setText(sbLog.toString());
    }

    /**
   * Action for what to do when the user browses exits the file browsing 
   * dialog. Basically gets the file and reads its text into a JTextField.
   *
   */
    private void browseDataFile_actionPerformed() {
        File dataFile = null;
        int returnVal = jfc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            dataFile = jfc.getSelectedFile();
            try {
                jtxtDataFile.setText(dataFile.getCanonicalPath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
   * Action for what do do after the user has selected the fsml file. Basically
   * gets the file and reads its text into a JTextField.
   *
   */
    private void browseFsmlFile_actionPerformed() {
        File fsmlFile = null;
        int returnVal = jfc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fsmlFile = jfc.getSelectedFile();
            try {
                jtxtFsmlFile.setText(fsmlFile.getCanonicalPath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
   * Action for what to do after the user clicks 'validate'. This will 
   * instantiate the necessary FSML classes and attempt to run the fsml and
   * the data file through the DefaultFsmlProcessor. 
   *
   */
    private void validate_actionPerformed() {
        File fileData = null;
        URL urlData = null;
        File fileFsml = null;
        URL urlFsml = null;
        clearLog();
        log("<<< Attempting to find Data File. >>>");
        urlData = getFile(this.jtxtDataFile.getText());
        log("\n<<< Attempting to find FSML File. >>>");
        urlFsml = getFile(this.jtxtFsmlFile.getText());
        if ((urlData != null) && (urlFsml != null)) {
            processFile(urlData, urlFsml);
        }
    }

    /**
   * Class to capture logging information on an output stream.
   *
   */
    class LogOutputStream extends OutputStream {

        public LogOutputStream() {
        }

        public void close() {
        }

        public void flush() {
        }

        public void write(byte[] b) {
        }

        public void write(byte[] bytes, int offset, int len) {
        }

        public void write(int b) {
        }
    }
}
