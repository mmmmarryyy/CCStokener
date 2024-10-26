package net.sourceforge.dictport.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.border.*;

/**
 * License UI Dialog
 *
 * @author Kaloian Doganov &lt;kaloian@europe.com&gt;
 */
final class LicenseDialog extends JDialog {

    private Action fCloseAction;

    /**
	 * Create LicenseDialog dialog with GNU General Public License.
	 *
	 * @param aOwner Dialog owner.
	 */
    LicenseDialog(Dialog aOwner) {
        super(aOwner, "Dictionary: License", true);
        initActions();
        initComponents();
        setResizable(false);
        pack();
    }

    private void initActions() {
        fCloseAction = new CloseAction();
    }

    private void initComponents() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(Main.kGap, Main.kGap, 0, Main.kGap));
        JTextArea licenseArea = new JTextArea();
        StringBuffer license = new StringBuffer(18000);
        int cols = resourceToString("gpl.txt", license);
        licenseArea.setColumns(cols);
        licenseArea.setRows(25);
        licenseArea.setEditable(false);
        licenseArea.setText(license.toString());
        licenseArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(licenseArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        licenseArea.setCaretPosition(0);
        root.add(scroll, "Center");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton(fCloseAction);
        buttonPanel.add(closeButton);
        getRootPane().setDefaultButton(closeButton);
        root.add(buttonPanel, "South");
        root.getActionMap().put("close", fCloseAction);
        InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        inputMap.put(esc, "close");
        setContentPane(root);
    }

    /** Center window when showing. */
    public void show() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle window = this.getBounds();
        window.x = (screen.width - window.width) / 2;
        window.y = (screen.height - window.height) / 2;
        setBounds(window);
        super.show();
    }

    /**
	 * Read resource content to StringBuffer.
	 *
	 * @param aFile resource name.
	 * @param aBuffer buffer where to load the contents of the resource.
	 * @return Maxumum columns.
	 */
    private int resourceToString(String aFile, StringBuffer aBuffer) {
        int cols = 0;
        URL url = getClass().getResource(aFile);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            do {
                line = in.readLine();
                if (line != null) {
                    if (line.length() > cols) cols = line.length();
                    aBuffer.append(line);
                    aBuffer.append('\n');
                }
            } while (line != null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cols;
    }

    private final class CloseAction extends AbstractAction {

        CloseAction() {
            super("Close");
        }

        public void actionPerformed(ActionEvent aEvent) {
            dispose();
        }
    }
}
