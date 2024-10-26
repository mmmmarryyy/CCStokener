package com.jmonkey.universal.sqltools.dialog;

import com.jmonkey.universal.sqltools.Main;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.applet.Applet;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JDialog;
import javax.swing.JWindow;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;

/**
 *  Description of the Class
 *
 *@author     Brill Pappin
 *@created    September 26, 2001
 *@version    0.1 Revision 0
 */
public final class SubmitBugsDialog extends JDialog {

    private static final String _BUGS_URL = "http://sourceforge.net/tracker/index.php?group_id=4741&atid=104741";

    private Main _OWNER = null;

    private final JProgressBar _PROGRESS = new JProgressBar(1, 100);

    private final JComboBox _CATEGORY = new JComboBox();

    private final JComboBox _GROUP = new JComboBox();

    private final JTextField _SUMMARY = new JTextField();

    private final JTextField _EMAIL = new JTextField();

    private final JTextArea _DETAILS = new JTextArea();

    /**
	 *  Constructor for the AboutDialog object
	 *
	 *@param  owner  Description of Paabout.16rameter
	 *@since
	 */
    public SubmitBugsDialog(Main owner) {
        super(owner, "Submit Bugs...");
        _OWNER = owner;
        doInit();
    }

    /**
	 *  Sets the location of the window relative to the specified component. If the
	 *  component is not currently showing, or <code>c</code> is <code>null</code>,
	 *  the window is centered on the screen. If the bottom of the component is
	 *  offscreen, the window is displayed to the right of the component.
	 *
	 *@param  c  the component in relation to which the window's location is
	 *      determined
	 *@since     1.4
	 */
    public void setLocationRelativeTo(Component c) {
        Container root = null;
        if (c != null) {
            if (c instanceof Window || c instanceof Applet) {
                root = (Container) c;
            } else {
                Container parent;
                for (parent = c.getParent(); parent != null; parent = parent.getParent()) {
                    if (parent instanceof Window || parent instanceof Applet) {
                        root = parent;
                        break;
                    }
                }
            }
        }
        if ((c != null && !c.isShowing()) || root == null || !root.isShowing()) {
            Dimension paneSize = getSize();
            Dimension screenSize = getToolkit().getScreenSize();
            setLocation((screenSize.width - paneSize.width) / 2, (screenSize.height - paneSize.height) / 2);
        } else {
            Dimension invokerSize = c.getSize();
            Point invokerScreenLocation;
            if (root instanceof Applet) {
                invokerScreenLocation = c.getLocationOnScreen();
            } else {
                invokerScreenLocation = new Point(0, 0);
                Component tc = c;
                while (tc != null) {
                    Point tcl = tc.getLocation();
                    invokerScreenLocation.x += tcl.x;
                    invokerScreenLocation.y += tcl.y;
                    if (tc == root) {
                        break;
                    }
                    tc = tc.getParent();
                }
            }
            Rectangle windowBounds = getBounds();
            int dx = invokerScreenLocation.x + ((invokerSize.width - windowBounds.width) >> 1);
            int dy = invokerScreenLocation.y + ((invokerSize.height - windowBounds.height) >> 1);
            Dimension ss = getToolkit().getScreenSize();
            if (dy + windowBounds.height > ss.height) {
                dy = ss.height - windowBounds.height;
                dx = invokerScreenLocation.x < (ss.width >> 1) ? invokerScreenLocation.x + invokerSize.width : invokerScreenLocation.x - windowBounds.width;
            }
            if (dx + windowBounds.width > ss.width) {
                dx = ss.width - windowBounds.width;
            }
            if (dx < 0) {
                dx = 0;
            }
            if (dy < 0) {
                dy = 0;
            }
            setLocation(dx, dy);
        }
    }

    /**
	 *  Description of the Method
	 *
	 *@since
	 */
    public void doExit() {
        setVisible(false);
        dispose();
    }

    /**
	 *  Description of the Method
	 *
	 *@since    0.1.0
	 */
    public void doSubmit() {
        updateProgress("Encoding...", 10);
        StringBuffer buffer = new StringBuffer();
        buffer.append(_BUGS_URL);
        buffer.append("&func=postadd");
        buffer.append("&category_id=" + URLEncoder.encode(((StringCodeListItem) _CATEGORY.getSelectedItem()).CODE));
        buffer.append("&artifact_group_id=" + URLEncoder.encode(((StringCodeListItem) _GROUP.getSelectedItem()).CODE));
        buffer.append("&summary=" + URLEncoder.encode(_SUMMARY.getText().trim()));
        buffer.append("&details=" + URLEncoder.encode(_DETAILS.getText().trim()));
        buffer.append("&user_email=" + URLEncoder.encode(_EMAIL.getText().trim()));
        URLConnection connection = null;
        try {
            updateProgress("Connecting...", 30);
            URL submiturl = new URL(buffer.toString());
            connection = submiturl.openConnection();
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection http = (HttpURLConnection) connection;
                http.setRequestMethod("POST");
                http.setFollowRedirects(true);
                updateProgress("Sending...", 50);
                http.connect();
                int rcode = http.getResponseCode();
                String rmsg = http.getResponseMessage();
                updateProgress("Done...", 100);
                switch(rcode) {
                    case HttpURLConnection.HTTP_OK:
                        JOptionPane.showMessageDialog(this, "Your bug report was submit with no errors.\n" + rcode + ": " + rmsg, "Submit Successful", JOptionPane.INFORMATION_MESSAGE);
                        break;
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        JOptionPane.showMessageDialog(this, "The server is haing a problem at this time\nTry you submission later.\n" + rcode + ": " + rmsg, "Server Problem", JOptionPane.ERROR_MESSAGE);
                        break;
                    case HttpURLConnection.HTTP_FORBIDDEN:
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        JOptionPane.showMessageDialog(this, "The bug submission URL is our of date.\nYou may need to update your version of this software\n" + rcode + ": " + rmsg, "Bad URL", JOptionPane.ERROR_MESSAGE);
                        break;
                    default:
                        JOptionPane.showMessageDialog(this, rcode + ": " + rmsg, "Unknown Response", JOptionPane.WARNING_MESSAGE);
                        break;
                }
            } else {
                JOptionPane.showMessageDialog(this, "Wrong Connection Type=" + connection.getClass().getName() + "\nPlease update the software...", "Out Of Date", JOptionPane.ERROR_MESSAGE);
            }
        } catch (MalformedURLException mfurle0) {
            JOptionPane.showMessageDialog(this, "The URL used to submit the report in invalid.\n" + mfurle0.getMessage(), "URL Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ioe0) {
            JOptionPane.showMessageDialog(this, "I/O Error.\n" + ioe0.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
        doExit();
    }

    /**
	 *  Description of the Method
	 *
	 *@param  status  Description of Parameter
	 *@param  value   Description of Parameter
	 *@since          0.1.0
	 */
    public final void updateProgress(String status, int value) {
        _PROGRESS.setString(status);
        _PROGRESS.setValue(value);
    }

    /**
	 *  Description of the Method
	 *
	 *@since
	 */
    private void doInit() {
        setSize(300, 300);
        setLocation(100, 100);
        setLocationRelativeTo(_OWNER);
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                doExit();
            }
        });
        JPanel content = (JPanel) getContentPane();
        content.setLayout(new BorderLayout());
        _PROGRESS.setStringPainted(true);
        _PROGRESS.setBorderPainted(false);
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 2, 5, 5));
        buttons.add(new JButton(new CancelAction(null)));
        buttons.add(new JButton(new SubmitAction(null)));
        JPanel progress = new JPanel();
        progress.setLayout(new BorderLayout());
        progress.add(buttons, BorderLayout.EAST);
        progress.add(_PROGRESS, BorderLayout.CENTER);
        JPanel inputs = new JPanel();
        inputs.setLayout(new BorderLayout());
        JPanel catInputs = new JPanel();
        catInputs.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Category"));
        catInputs.setLayout(new BorderLayout());
        catInputs.add(_CATEGORY, BorderLayout.CENTER);
        _CATEGORY.setToolTipText("This drop-down box represents the Category of the\ntracker items which is a particular section of a project.");
        _CATEGORY.addItem(new StringCodeListItem("100", "None"));
        _CATEGORY.addItem(new StringCodeListItem("379437", "Documentation"));
        _CATEGORY.addItem(new StringCodeListItem("379435", "JDBC Drivers"));
        _CATEGORY.addItem(new StringCodeListItem("379436", "OS Related Problem"));
        _CATEGORY.addItem(new StringCodeListItem("379434", "Program Runtime"));
        _CATEGORY.addItem(new StringCodeListItem("102744", "User Interface"));
        JPanel grpInputs = new JPanel();
        grpInputs.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Group"));
        grpInputs.setLayout(new BorderLayout());
        grpInputs.add(_GROUP, BorderLayout.CENTER);
        _GROUP.setToolTipText("This drop-down box represents the Group of the tracker items\nwhich is a list of project admin-defined options.");
        _GROUP.addItem(new StringCodeListItem("100", "None"));
        _GROUP.addItem(new StringCodeListItem("165269", "Crashes Program"));
        _GROUP.addItem(new StringCodeListItem("165268", "Doesn't Work as Designed"));
        _GROUP.addItem(new StringCodeListItem("101645", "Interface Usability"));
        _GROUP.addItem(new StringCodeListItem("165270", "Not Intuitive"));
        _GROUP.addItem(new StringCodeListItem("165272", "Security Problem"));
        _GROUP.addItem(new StringCodeListItem("101646", "Typeo, Bad Information"));
        _GROUP.addItem(new StringCodeListItem("165271", "Uneeded Extra Step"));
        JPanel catGrpGrid = new JPanel();
        catGrpGrid.setBorder(BorderFactory.createEmptyBorder());
        catGrpGrid.setLayout(new GridLayout(1, 2, 5, 5));
        catGrpGrid.add(catInputs);
        catGrpGrid.add(grpInputs);
        JPanel sumInputs = new JPanel();
        sumInputs.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Summary"));
        sumInputs.setLayout(new BorderLayout());
        sumInputs.add(_SUMMARY, BorderLayout.CENTER);
        _SUMMARY.setToolTipText("The summary text-box represents a short tracker item summary. Useful\nwhen browsing through several tracker items.");
        JPanel summspacer = new JPanel();
        summspacer.setBorder(BorderFactory.createEmptyBorder());
        summspacer.setLayout(new GridLayout(2, 1, 0, 0));
        summspacer.add(catGrpGrid);
        summspacer.add(sumInputs);
        JPanel emlInputs = new JPanel();
        emlInputs.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Email Address"));
        emlInputs.setLayout(new BorderLayout());
        emlInputs.add(_EMAIL, BorderLayout.CENTER);
        _EMAIL.setToolTipText("Please enter your e-mail address here, in case we need to contact you\nto clarify your bug submission.");
        JPanel dtlInputs = new JPanel();
        dtlInputs.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Details"));
        dtlInputs.setLayout(new BorderLayout());
        dtlInputs.add(_DETAILS, BorderLayout.CENTER);
        _DETAILS.setBorder(BorderFactory.createLoweredBevelBorder());
        _DETAILS.setToolTipText("Enter Bug Details here...");
        inputs.add(summspacer, BorderLayout.NORTH);
        inputs.add(dtlInputs, BorderLayout.CENTER);
        inputs.add(emlInputs, BorderLayout.SOUTH);
        content.add(inputs, BorderLayout.CENTER);
        content.add(progress, BorderLayout.SOUTH);
    }

    /**
	 *  Description of the Class
	 *
	 *@author     <A HREF="mailto:brillpappin@hotmail.com">Brill Pappin</A>
	 *@created    December 7, 2001
	 *@version    0.1 Revision 0
	 */
    class StringCodeListItem {

        private String CODE = null;

        private String TEXT = null;

        /**
		 *  Constructor for the StringCodeListItem object
		 *
		 *@param  code  Description of Parameter
		 *@param  text  Description of Parameter
		 *@since        0.1.0
		 */
        StringCodeListItem(String code, String text) {
            super();
            CODE = code;
            TEXT = text;
        }

        /**
		 *  Description of the Method
		 *
		 *@return    Description of the Returned Value
		 *@since     0.1.0
		 */
        public String toString() {
            return TEXT;
        }
    }

    /**
	 *  Description of the Class
	 *
	 *@author     <A HREF="mailto:brillpappin@hotmail.com">Brill Pappin</A>
	 *@created    December 7, 2001
	 *@version    0.1 Revision 0
	 */
    private class CancelAction extends AbstractAction {

        /**
		 *  Constructor for the CancelAction object
		 *
		 *@param  icon  Description of Parameter
		 *@since        0.1.0
		 */
        protected CancelAction(ImageIcon icon) {
            super("Cancel", icon);
        }

        /**
		 *  Description of the Method
		 *
		 *@param  e  Description of Parameter
		 *@since     0.1.0
		 */
        public void actionPerformed(ActionEvent e) {
            doExit();
        }
    }

    /**
	 *  Description of the Class
	 *
	 *@author     <A HREF="mailto:brillpappin@hotmail.com">Brill Pappin</A>
	 *@created    December 7, 2001
	 *@version    0.1 Revision 0
	 */
    private class SubmitAction extends AbstractAction {

        /**
		 *  Constructor for the SubmitAction object
		 *
		 *@param  icon  Description of Parameter
		 *@since        0.1.0
		 */
        protected SubmitAction(ImageIcon icon) {
            super("Submit", icon);
        }

        /**
		 *  Description of the Method
		 *
		 *@param  e  Description of Parameter
		 *@since     0.1.0
		 */
        public void actionPerformed(ActionEvent e) {
            doSubmit();
        }
    }
}
