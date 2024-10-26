package pl.edu.mimuw.xqtav.proc;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.Logger;
import pl.edu.mimuw.xqtav.XQMultiDocument;
import pl.edu.mimuw.xqtav.xqgen.XQGeneratorException;
import pl.edu.mimuw.xqtav.xqgen.api.GenerationEventType;
import pl.edu.mimuw.xqtav.xqgen.api.GenerationProgressMonitor;
import pl.edu.mimuw.xqtav.xqgen.xqgenerator_1.rec.XQRecursiveGenerator;
import pl.edu.mimuw.xqtav.xqgen.xqgenerator_1.rec.XQueryMultiWriter;

/**
 * @author gk
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XQWizardGenerator extends JDialog implements ActionListener, GenerationProgressMonitor {

    public static final long serialVersionUID = 1;

    /**
	 * @throws java.awt.HeadlessException
	 */
    public XQWizardGenerator() throws HeadlessException {
        super();
        initializeWizard();
    }

    /**
	 * @param arg0
	 * @throws java.awt.HeadlessException
	 */
    public XQWizardGenerator(Dialog arg0) throws HeadlessException {
        super(arg0);
        initializeWizard();
    }

    /**
	 * @param arg0
	 * @param arg1
	 * @throws java.awt.HeadlessException
	 */
    public XQWizardGenerator(Dialog arg0, boolean arg1) throws HeadlessException {
        super(arg0, arg1);
        initializeWizard();
    }

    /**
	 * @param arg0
	 * @throws java.awt.HeadlessException
	 */
    public XQWizardGenerator(Frame arg0) throws HeadlessException {
        super(arg0);
        initializeWizard();
    }

    /**
	 * @param arg0
	 * @param arg1
	 * @throws java.awt.HeadlessException
	 */
    public XQWizardGenerator(Frame arg0, boolean arg1) throws HeadlessException {
        super(arg0, arg1);
        initializeWizard();
    }

    /**
	 * @param arg0
	 * @param arg1
	 * @throws java.awt.HeadlessException
	 */
    public XQWizardGenerator(Dialog arg0, String arg1) throws HeadlessException {
        super(arg0, arg1);
        initializeWizard();
    }

    /**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @throws java.awt.HeadlessException
	 */
    public XQWizardGenerator(Dialog arg0, String arg1, boolean arg2) throws HeadlessException {
        super(arg0, arg1, arg2);
        initializeWizard();
    }

    /**
	 * @param arg0
	 * @param arg1
	 * @throws java.awt.HeadlessException
	 */
    public XQWizardGenerator(Frame arg0, String arg1) throws HeadlessException {
        super(arg0, arg1);
        initializeWizard();
    }

    /**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @throws java.awt.HeadlessException
	 */
    public XQWizardGenerator(Frame arg0, String arg1, boolean arg2) throws HeadlessException {
        super(arg0, arg1, arg2);
        initializeWizard();
    }

    /**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @throws java.awt.HeadlessException
	 */
    public XQWizardGenerator(Dialog arg0, String arg1, boolean arg2, GraphicsConfiguration arg3) throws HeadlessException {
        super(arg0, arg1, arg2, arg3);
        initializeWizard();
    }

    /**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
    public XQWizardGenerator(Frame arg0, String arg1, boolean arg2, GraphicsConfiguration arg3) {
        super(arg0, arg1, arg2, arg3);
        initializeWizard();
    }

    protected JPanel scuflSelectionPanel = null;

    protected JPanel generationPanel = null;

    protected JPanel currentPanel = null;

    protected JRadioButton rb_uri = null;

    protected JRadioButton rb_file = null;

    protected JTextField tf_uri = null;

    protected JTextField tf_file = null;

    protected JButton bt_file = null;

    protected JTextArea eventList = null;

    protected JProgressBar progressBar = null;

    protected JScrollPane eventScrollPane = null;

    protected JButton approveButton = null;

    protected JButton discardButton = null;

    protected JButton backButton = null;

    public void initializeWizard() {
        this.setTitle("Generate XQuery from SCUFL");
        scuflSelectionPanel = new JPanel();
        scuflSelectionPanel.setLayout(new GridLayout(4, 1));
        JLabel label = new JLabel("Select SCUFL document");
        label.setFont(new Font("Arial", Font.BOLD, 15));
        label.setHorizontalAlignment(JLabel.CENTER);
        scuflSelectionPanel.add(label);
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2));
        rb_uri = new JRadioButton("Enter by URI");
        rb_uri.setActionCommand("rb_uri");
        rb_uri.addActionListener(this);
        rb_uri.setSelected(true);
        panel.add(rb_uri);
        tf_uri = new JTextField("http://xqtav.sourceforge.net");
        tf_uri.setColumns(30);
        panel.add(tf_uri);
        rb_file = new JRadioButton("Choose file");
        rb_file.setActionCommand("rb_file");
        rb_file.addActionListener(this);
        panel.add(rb_file);
        JPanel jp = new JPanel();
        tf_file = new JTextField("");
        tf_file.setColumns(15);
        jp.add(tf_file);
        bt_file = new JButton("Browse...");
        bt_file.setActionCommand("choose_file");
        bt_file.addActionListener(this);
        jp.add(bt_file);
        panel.add(jp);
        scuflSelectionPanel.add(panel);
        label = new JLabel("Select the filename or URI and click 'GENERATE' to start.");
        scuflSelectionPanel.add(label);
        validateSelection();
        jp = new JPanel();
        JButton button = new JButton("Cancel");
        button.setActionCommand("select_cancel");
        button.addActionListener(this);
        jp.add(button);
        button = new JButton("GENERATE");
        button.setActionCommand("generate");
        button.addActionListener(this);
        jp.add(button);
        scuflSelectionPanel.add(jp);
        generationPanel = new JPanel();
        generationPanel.setLayout(new BorderLayout());
        label = new JLabel("Process SCUFL");
        label.setFont(new Font("Arial", Font.BOLD, 15));
        label.setHorizontalAlignment(JLabel.CENTER);
        generationPanel.add(label, BorderLayout.NORTH);
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setOrientation(JProgressBar.VERTICAL);
        generationPanel.add(progressBar, BorderLayout.EAST);
        eventList = new JTextArea();
        eventList.setColumns(80);
        eventList.setRows(30);
        eventScrollPane = new JScrollPane(eventList);
        generationPanel.add(eventScrollPane, BorderLayout.CENTER);
        jp = new JPanel();
        backButton = new JButton("Back");
        backButton.setActionCommand("back");
        backButton.addActionListener(this);
        jp.add(backButton);
        approveButton = new JButton("Approve");
        approveButton.setActionCommand("approve");
        approveButton.addActionListener(this);
        jp.add(approveButton);
        discardButton = new JButton("Discard");
        discardButton.setActionCommand("discard");
        discardButton.addActionListener(this);
        jp.add(discardButton);
        generationPanel.add(jp, BorderLayout.SOUTH);
        approveButton.setEnabled(false);
        discardButton.setEnabled(false);
        backButton.setEnabled(false);
        setPanel(scuflSelectionPanel);
        this.setModal(true);
    }

    public void setPanel(JPanel panel) {
        if (currentPanel != null) this.remove(currentPanel);
        this.getContentPane().add(panel);
        currentPanel = panel;
        this.pack();
    }

    public XQMultiDocument document = null;

    public boolean last_uri = false;

    public boolean last_file = false;

    public void validateSelection() {
        boolean cur_uri = rb_uri.isSelected();
        boolean cur_file = rb_file.isSelected();
        if (cur_uri ^ last_uri) {
            if (cur_uri) {
                tf_uri.setEnabled(true);
                rb_file.setSelected(false);
                last_file = false;
                tf_file.setEnabled(false);
                bt_file.setEnabled(false);
                last_uri = true;
                return;
            } else {
                tf_uri.setEnabled(false);
                rb_file.setSelected(true);
                last_file = true;
                tf_file.setEnabled(true);
                bt_file.setEnabled(true);
                last_uri = false;
                return;
            }
        }
        if (cur_file ^ last_file) {
            if (cur_file) {
                tf_uri.setEnabled(false);
                rb_uri.setSelected(false);
                last_uri = false;
                tf_file.setEnabled(true);
                bt_file.setEnabled(true);
                last_file = true;
                return;
            } else {
                tf_uri.setEnabled(true);
                rb_uri.setSelected(true);
                last_uri = true;
                tf_file.setEnabled(false);
                bt_file.setEnabled(false);
                last_file = false;
                return;
            }
        }
    }

    protected class Log4XQW extends Log4JLogger {

        public static final long serialVersionUID = 1;

        public XQWizardGenerator wizgen = null;

        /**
		 * 
		 */
        public Log4XQW(XQWizardGenerator wiz) {
            wizgen = wiz;
        }

        public void debug(Object arg0, Throwable arg1) {
            registerProgress(GenerationEventType.INFO, arg0.toString(), 30);
        }

        public void debug(Object arg0) {
            registerProgress(GenerationEventType.INFO, arg0.toString(), 30);
        }

        public void error(Object arg0, Throwable arg1) {
            registerProgress(GenerationEventType.ERROR, arg0.toString(), 30);
        }

        public void error(Object arg0) {
            registerProgress(GenerationEventType.ERROR, arg0.toString(), 30);
        }

        public void fatal(Object arg0, Throwable arg1) {
            registerProgress(GenerationEventType.ERROR, arg0.toString(), 30);
        }

        public void fatal(Object arg0) {
            registerProgress(GenerationEventType.ERROR, arg0.toString(), 30);
        }

        public Logger getLogger() {
            return super.getLogger();
        }

        public void info(Object arg0, Throwable arg1) {
            registerProgress(GenerationEventType.INFO, arg0.toString(), 30);
        }

        public void info(Object arg0) {
            registerProgress(GenerationEventType.INFO, arg0.toString(), 30);
        }

        public boolean isDebugEnabled() {
            return false;
        }

        public boolean isErrorEnabled() {
            return true;
        }

        public boolean isFatalEnabled() {
            return true;
        }

        public boolean isInfoEnabled() {
            return true;
        }

        public boolean isTraceEnabled() {
            return true;
        }

        public boolean isWarnEnabled() {
            return true;
        }

        public void trace(Object arg0, Throwable arg1) {
            registerProgress(GenerationEventType.INFO, arg0.toString(), 30);
        }

        public void trace(Object arg0) {
            registerProgress(GenerationEventType.INFO, arg0.toString(), 30);
        }

        public void warn(Object arg0, Throwable arg1) {
            registerProgress(GenerationEventType.WARNING, arg0.toString(), 30);
        }

        public void warn(Object arg0) {
            registerProgress(GenerationEventType.WARNING, arg0.toString(), 30);
        }

        public int hashCode() {
            return super.hashCode();
        }

        protected void finalize() throws Throwable {
            super.finalize();
        }

        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public boolean equals(Object arg0) {
            return super.equals(arg0);
        }

        public String toString() {
            return super.toString();
        }
    }

    public void generateXQuery() throws Exception {
        backButton.setEnabled(false);
        discardButton.setEnabled(false);
        approveButton.setEnabled(false);
        progressBar.setValue(0);
        eventList.setText("");
        registerProgress(GenerationEventType.INFO, "Reading input file", 1);
        InputStream is = null;
        if (rb_uri.isSelected()) {
            URL url = new URL(tf_uri.getText().trim());
            URLConnection conn = url.openConnection();
            conn.connect();
            is = conn.getInputStream();
        } else {
            is = new FileInputStream(tf_file.getText().trim());
        }
        XQueryMultiWriter xmw = new XQueryMultiWriter();
        XQRecursiveGenerator xrq = new XQRecursiveGenerator("", xmw);
        xrq.setInputStream(is);
        xrq.setLogger(new Log4XQW(this));
        xrq.generateXQueryWithPM(this);
        XQMultiDocument xmd = XQMultiDocument.fromXQueryMultiWriter(xmw);
        registerProgress(GenerationEventType.SUCCESS, "Finished generating successfully", 100);
        document = xmd;
        backButton.setEnabled(true);
        discardButton.setEnabled(true);
        approveButton.setEnabled(true);
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand() == null) {
            return;
        }
        String cmd = event.getActionCommand();
        if (cmd.equals("rb_uri") || cmd.equals("rb_file")) {
            validateSelection();
            return;
        }
        if (cmd.equals("generate")) {
            setPanel(generationPanel);
            try {
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            generateXQuery();
                        } catch (Exception e) {
                            registerProgress(GenerationEventType.ERROR, "Exception: " + e, 0);
                            backButton.setEnabled(true);
                            discardButton.setEnabled(true);
                        }
                    }
                }).start();
            } catch (Exception e) {
                registerProgress(GenerationEventType.ERROR, "Failed to start generation thread: " + e, 0);
                backButton.setEnabled(true);
                discardButton.setEnabled(true);
            }
            return;
        }
        if (cmd.equals("select_cancel")) {
            document = null;
            this.setVisible(false);
            return;
        }
        if (cmd.equals("back")) {
            setPanel(scuflSelectionPanel);
            return;
        }
        if (cmd.equals("approve")) {
            this.setVisible(false);
            return;
        }
        if (cmd.equals("discard")) {
            document = null;
            this.setVisible(false);
            return;
        }
        if (cmd.equals("choose_file")) {
            JFileChooser jfc = new JFileChooser();
            jfc.setDialogType(JFileChooser.OPEN_DIALOG);
            jfc.setAcceptAllFileFilterUsed(true);
            jfc.setDialogTitle("Select SCUFL file");
            jfc.setMultiSelectionEnabled(false);
            jfc.setFileFilter(new FileFilter() {

                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith(".xml") || f.getName().endsWith(".XML");
                }

                public String getDescription() {
                    return "SCUFL diagrams (.xml)";
                }
            });
            int ret = jfc.showDialog(this, "Load");
            if (ret == JFileChooser.APPROVE_OPTION) {
                File f = jfc.getSelectedFile();
                tf_file.setText(f.getAbsolutePath());
                return;
            }
            return;
        }
        System.err.println("Unknown action command: " + cmd);
    }

    public void registerProgress(int type, String data, int progress) {
        if (currentPanel != generationPanel) {
            throw new XQGeneratorException("Bad progress call");
        }
        StringBuffer sb = new StringBuffer();
        switch(type) {
            case GenerationEventType.INFO:
                {
                    sb.append("INFO: ");
                    break;
                }
            case GenerationEventType.WARNING:
                {
                    sb.append("WARNING: ");
                    break;
                }
            case GenerationEventType.ERROR:
                {
                    sb.append("ERROR: ");
                    break;
                }
            case GenerationEventType.SUCCESS:
                {
                    sb.append("SUCCESS: ");
                    break;
                }
            default:
                {
                    sb.append("?: ");
                }
        }
        sb.append(data);
        sb.append("\n");
        progressBar.setValue(progress % 101);
        eventList.append(sb.toString());
    }
}
