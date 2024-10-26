package rj.tools.jcsc.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import rj.tools.argumentprocessor.ArgProcessor;
import rj.tools.jcsc.rules.RulesHandler;
import rj.tools.util.ui.TextFieldButton;
import rj.tools.util.ui.WidgetSizer;

/**
 * <code>RulesDialog</code> is the dialog in which the rules for JCSC can be
 * specified.
 *
 * @author Ralph Jocham
 * @version __0.98.2__
 */
public class RulesDialog extends JFrame {

    private static final String WINDOW_TITLE = "JCSC Rules Editor";

    private static final String DEFAULT = "Default";

    private static final String OTHER = "Other";

    private static final String SAVE = "Save";

    private static final String SAVEAS = "Save As";

    private static final String CLOSE = "Close";

    private boolean mIsStartedStandAlone = false;

    private ButtonGroup mSourceButtonGroup = new ButtonGroup();

    private JRadioButton mDefaultRB = new JRadioButton(DEFAULT);

    private JRadioButton mOtherRB = new JRadioButton(OTHER);

    private TextFieldButton mOtherSourceTF = new TextFieldButton();

    private JButton mSaveButton = new JButton(SAVE);

    private JButton mSaveAsButton = new JButton(SAVEAS);

    private JButton mCloseButton = new JButton(CLOSE);

    private RulesPanel mRulesPanel = new RulesPanel();

    private JFileChooser mFileChooser = new JFileChooser();

    private File mOtherRuleFile;

    /**
    * <code>main</code> brings up the RulesDialog
    *
    * @param args a <code>String[]</code> value
    */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            ;
        }
        RulesDialog dialog = new RulesDialog();
        ArgProcessor ap = new ArgProcessor();
        ap.defineOptions("h");
        ap.putArguments(args);
        args = ap.getArguments();
        if (ap.hasFlagOption("h")) {
            System.out.println("\nJCSC Ruleseditor Version __0.98.1__:  Usage is one of:");
            System.out.println("Usage:");
            System.out.println("  ruleseditor [-h] [<rule-file>]");
            System.out.println("    -h      : show this output");
            System.out.println("\nWritten by Ralph Jocham in 1999-2005");
            System.out.println("This Software is Free under the GPL (GNU Public Licence)\n");
            System.exit(0);
        }
        if (args.length > 0) {
            if (args.length > 1) {
                JOptionPane.showMessageDialog(dialog, "Several arguments were provided, only the first " + " one is used!", "Startup Warning", JOptionPane.WARNING_MESSAGE);
            }
            dialog.setRules(args[0]);
        }
        dialog.mIsStartedStandAlone = true;
        dialog.setVisible(true);
        dialog.pack();
    }

    /**
    * Creates a new <code>RulesDialog</code> instance.
    */
    public RulesDialog() {
        initInstance();
    }

    /**
    * Creates a new <code>RulesDialog</code> instance.
    *
    * @param parent a <code > Component</code> - the calling component; can be
    * null
    */
    public RulesDialog(Component parent) {
        super();
        initInstance();
        setLocationRelativeTo(parent);
    }

    /**
    * <code>initInstance</code> extracted logic from the constructor which could
    * not stay there because of the invcaiton of the super constructor.
    */
    private void initInstance() {
        setTitle(WINDOW_TITLE + "  (__0.98.1__)");
        initUI();
        setDefaultRules();
        pack();
    }

    private void initUI() {
        setIconImage(new ImageIcon(getClass().getResource("icons/JCSCTitleBar.gif")).getImage());
        mDefaultRB.setSelected(true);
        mOtherSourceTF.setEnabled(false);
        mSaveButton.setEnabled(false);
        mSourceButtonGroup.add(mDefaultRB);
        mSourceButtonGroup.add(mOtherRB);
        getContentPane().setLayout(new BorderLayout());
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        JPanel sourcePanel = new JPanel();
        JPanel space0 = new JPanel();
        sourcePanel.setLayout(gb);
        sourcePanel.setBorder(BorderFactory.createTitledBorder("Rules Source"));
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(0, 10, 0, 10);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(mDefaultRB, c);
        sourcePanel.add(mDefaultRB);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(0, 10, 10, 0);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(mOtherRB, c);
        sourcePanel.add(mOtherRB);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.2;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 10, 0);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(mOtherSourceTF, c);
        sourcePanel.add(mOtherSourceTF);
        c.gridx = 2;
        c.gridy = 1;
        c.weightx = 0.8;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(space0, c);
        sourcePanel.add(space0);
        mRulesPanel.setBorder(BorderFactory.createTitledBorder("Rules"));
        JPanel buttonPanel = new JPanel();
        JPanel space1 = new JPanel();
        JPanel space2 = new JPanel();
        WidgetSizer.alignToMaxWidth(new JComponent[] { mSaveButton, mSaveAsButton, mCloseButton });
        gb = new GridBagLayout();
        buttonPanel.setLayout(gb);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.insets = new Insets(10, 0, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(space1, c);
        buttonPanel.add(space1);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(10, 0, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(space2, c);
        buttonPanel.add(space2);
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(10, 0, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(mSaveButton, c);
        buttonPanel.add(mSaveButton);
        c.gridx = 3;
        c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(10, 0, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(mSaveAsButton, c);
        buttonPanel.add(mSaveAsButton);
        c.gridx = 4;
        c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(10, 0, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(mCloseButton, c);
        buttonPanel.add(mCloseButton);
        getContentPane().add(sourcePanel, BorderLayout.NORTH);
        getContentPane().add(mRulesPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        mDefaultRB.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleSourceRadioButton(e);
            }
        });
        mOtherRB.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleSourceRadioButton(e);
            }
        });
        mOtherSourceTF.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openRulesFile();
            }
        });
        mSaveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        mSaveAsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveAs();
            }
        });
        mCloseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });
        setI18nStrings(getZipI18nStrings());
    }

    private void save() {
        persistRules();
    }

    private void saveAs() {
        mFileChooser.setDialogTitle("Save JCSC Rules File");
        mFileChooser.setFileFilter(new JCSCFileFilter());
        mFileChooser.setApproveButtonText("Save");
        mFileChooser.setCurrentDirectory(deserializeOpenPath(".jcsc.filechooser.save.ser"));
        if (mFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = appendExtension(mFileChooser.getSelectedFile());
            mOtherRuleFile = file;
            mOtherSourceTF.setText(mOtherRuleFile.getAbsolutePath());
            persistRules();
            serializeOpenPath(new File(mOtherRuleFile.getParent()), ".jcsc.filechooser.save.ser");
        }
    }

    private File appendExtension(File file) {
        File tmpFile = file;
        if (!file.getName().endsWith(".jcsc.xml")) {
            tmpFile = new File(file.getAbsolutePath() + ".jcsc.xml");
        }
        return tmpFile;
    }

    private void persistRules() {
        try {
            Map map = mRulesPanel.getRules();
            RulesHandler rh = new RulesHandler();
            rh.setPrintStream(new PrintStream(new FileOutputStream(mOtherRuleFile)));
            rh.persistRules(map);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Cannot access file '" + mOtherRuleFile + "'", "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancel() {
        dispose();
        exitIfStandAlone();
    }

    private void ok() {
        dispose();
        exitIfStandAlone();
    }

    private void exitIfStandAlone() {
        if (mIsStartedStandAlone) {
            System.exit(0);
        }
    }

    private void handleSourceRadioButton(ActionEvent e) {
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        if (e.getSource() == mOtherRB) {
            mOtherSourceTF.setEnabled(true);
            mSaveButton.setEnabled(true);
            if (!mOtherSourceTF.getText().equals("")) {
                setRules(mOtherSourceTF.getText());
            }
        } else {
            mOtherSourceTF.setEnabled(false);
            mSaveButton.setEnabled(false);
            setDefaultRules();
        }
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
    * <code>openRulesFile</code> brings up a file chooser to choose a .jcsc
    * file
    */
    public void openRulesFile() {
        mFileChooser.setDialogTitle("Load JCSC Rules File");
        mFileChooser.setFileFilter(new JCSCFileFilter());
        mFileChooser.setCurrentDirectory(deserializeOpenPath(".jcsc.filechooser.open.ser"));
        if (mFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            mOtherRuleFile = mFileChooser.getSelectedFile();
            mOtherSourceTF.setText(mOtherRuleFile.getAbsolutePath());
            try {
                setRules(new RulesHandler().readRulesFile(mOtherRuleFile.getAbsolutePath()));
                serializeOpenPath(new File(mOtherRuleFile.getParent()), ".jcsc.filechooser.open.ser");
            } catch (FileNotFoundException fne) {
                fne.printStackTrace(System.err);
            } catch (IOException ioe) {
                ioe.printStackTrace(System.err);
            }
        }
    }

    private void serializeOpenPath(File file, String name) {
        try {
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(System.getProperty("user.home") + File.separator + name));
            os.writeObject(file);
        } catch (IOException e) {
            ;
        }
    }

    private File deserializeOpenPath(String name) {
        File file = null;
        boolean isError = false;
        try {
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(System.getProperty("user.home") + File.separator + name));
            file = (File) is.readObject();
        } catch (IOException e) {
            isError = true;
        } catch (ClassNotFoundException e) {
            isError = true;
        }
        if (isError) {
            file = new File("");
        }
        return file;
    }

    /**
    * <code>processWindowEvent</code> - overwrited JFrame method
    *
    * @param e a <code>WindowEvent</code> value
    */
    public void processWindowEvent(WindowEvent e) {
        if (e.paramString().equals("WINDOW_CLOSING")) {
            cancel();
        } else {
            super.processWindowEvent(e);
        }
    }

    private Map getZipI18nStrings() {
        Map map = null;
        boolean isError = false;
        try {
            URL url = getClass().getResource("/rj/tools/jcsc/ui/JcscI18n.xml");
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(url.openStream()));
            Document document = parser.getDocument();
            map = new XMLI18nMap(document, "english");
        } catch (SAXException e) {
            isError = true;
        } catch (IOException e) {
            isError = true;
        }
        if (isError) {
            throw new RuntimeException("'JcscI18n.xml' could not be found in the JCSC.jar file");
        }
        return map;
    }

    /**
    * Set the Map of rules; key=name of rul, value=Rule object
    *
    * @param map
    */
    private void setRules(Map map) {
        mRulesPanel.setRules(map);
    }

    /**
    * Set rules being read from a XML file from the file system. If the file
    * could not be read the default rules xml files is set
    *
    * @param rulesFile
    */
    public void setRules(String rulesFile) {
        try {
            Map cache = new RulesHandler().readRulesFile(rulesFile);
            setRules(cache);
            mOtherRB.setSelected(true);
            mSaveButton.setEnabled(true);
            mOtherSourceTF.setEnabled(true);
            mOtherRuleFile = new File(rulesFile);
            mOtherSourceTF.setText(mOtherRuleFile.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot read the '" + rulesFile + "' rules file. Using 'default.jcsc'", "Startup Error", JOptionPane.ERROR_MESSAGE);
            setDefaultRules();
        }
    }

    private void setDefaultRules() {
        try {
            setRules(new RulesHandler().readZipRulesFile("default.jcsc.xml"));
            mDefaultRB.setSelected(false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot read the 'default.jcsc.xml' rules " + "file from the jcsc.jar", "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void setI18nStrings(Map map) {
        mRulesPanel.setI18nStrings(map);
    }

    private class JCSCFileFilter extends FileFilter {

        /**
       * Whether the given file is accepted by this filter.
       *
       * @param f which extension is checked
       *
       * @return true or false
       */
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().endsWith(".jcsc.xml");
        }

        /**
       * Get the description for the file filter
       *
       * @return description
       */
        public String getDescription() {
            return "The Rules File for JCSC (*.jcsc.xml)";
        }
    }
}
