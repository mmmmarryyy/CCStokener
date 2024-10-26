package org.jimcat.gui.dialog.failurefeedbackdialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.jimcat.gui.SwingClient;
import org.jimcat.gui.frame.JimCatDialog;
import org.jimcat.services.failurefeedback.FailureDescription;
import org.jimcat.services.failurefeedback.FailureFeedbackListener;
import org.jvnet.substance.SubstanceLookAndFeel;

/**
 * This class is responsible to display emerged system failures.
 * 
 * It will also implement a way to commit emerged errors to the developer team.
 * 
 * $Id$
 * 
 * @author Michael/Herbert
 */
public class FailureFeedbackDialog extends JimCatDialog implements FailureFeedbackListener, ActionListener {

    /**
	 * commands used to determine action
	 */
    private static final String COMMAND_SEND = "send";

    /**
	 * constant to identify show detail action
	 */
    private static final String COMMAND_DETAIL = "detail";

    /**
	 * constant to identify close action
	 */
    private static final String COMMAND_CLOSE = "close";

    /**
	 * constant to identify exit program action
	 */
    private static final String COMMAND_EXIT_PROGRAM = "exit";

    /**
	 * default height of field in collapsed state
	 */
    private static final int collapsedHeight = 150;

    /**
	 *  default height of field in expanded state 
	 */
    private static final int expandedHeight = 450;

    /**
	 * a reverence to the swing client
	 */
    private SwingClient client;

    /**
	 * label used to show message
	 */
    private JLabel message;

    /**
	 * button used to expand
	 */
    private JButton detail;

    /**
	 * button used to close program
	 */
    private JButton exit;

    /**
	 * close button
	 */
    private JButton close;

    /**
	 * button used to send report
	 */
    private JButton send;

    /**
	 * TextArea used to display failure report
	 */
    private JTextArea report;

    /**
	 * panel containing report
	 */
    private JScrollPane detailPanel;

    /**
	 * current state
	 */
    private boolean isExpanded = false;

    /**
	 * constructor
	 * 
	 * @param client
	 */
    public FailureFeedbackDialog(SwingClient client) {
        super(client.getMainFrame(), true);
        this.client = client;
        initComponents();
    }

    /**
	 * build up gui structure
	 */
    private void initComponents() {
        setTitle("JimCat Error");
        setLayout(new BorderLayout());
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());
        ImageIcon icon = new ImageIcon(getClass().getResource("error.gif"));
        JLabel iconContainer = new JLabel();
        iconContainer.setText("");
        iconContainer.setOpaque(false);
        iconContainer.setHorizontalAlignment(SwingConstants.CENTER);
        iconContainer.setVerticalAlignment(SwingConstants.CENTER);
        iconContainer.setIcon(icon);
        iconContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        header.add(iconContainer, BorderLayout.WEST);
        message = new JLabel();
        message.setBorder(new EmptyBorder(10, 10, 10, 10));
        header.add(message, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BorderLayout());
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        send = new JButton("Send Report");
        send.setActionCommand(COMMAND_SEND);
        send.setToolTipText("Only Shown Information Will Be Sent");
        send.addActionListener(this);
        send.setFocusable(false);
        detail = new JButton("Show Details");
        detail.setActionCommand(COMMAND_DETAIL);
        detail.addActionListener(this);
        detail.setFocusable(false);
        exit = new JButton("Close Program");
        exit.setActionCommand(COMMAND_EXIT_PROGRAM);
        exit.addActionListener(this);
        exit.setFocusable(false);
        close = new JButton("OK");
        close.setActionCommand(COMMAND_CLOSE);
        close.addActionListener(this);
        close.setFocusable(false);
        getRootPane().setDefaultButton(close);
        buttons.add(send);
        buttons.add(detail);
        buttons.add(exit);
        buttons.add(close);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        report = new JTextArea();
        report.setEditable(false);
        report.putClientProperty(SubstanceLookAndFeel.NO_EXTRA_ELEMENTS, Boolean.TRUE);
        detailPanel = new JScrollPane();
        detailPanel.setViewportView(report);
        Dimension size = new Dimension(450, collapsedHeight);
        Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(size);
        setLocation((screensize.width - size.width) / 2, (screensize.height - size.height) / 2);
    }

    /**
	 * react on an error by showing a dialog.
	 * 
	 * @see org.jimcat.services.failurefeedback.FailureFeedbackListener#failureEmerged(org.jimcat.services.failurefeedback.FailureDescription)
	 */
    public void failureEmerged(FailureDescription failure) {
        String errorMsg = failure.getMessage();
        String msg;
        if (errorMsg == null) {
            msg = "An unknown error occured.";
        } else {
            msg = "JimCat discovered an error: " + errorMsg;
        }
        msg = "<html>" + msg;
        msg += "<br><br>";
        msg += "Sorry for the inconvenience caused.<br>";
        msg += "Please help us to improve quality by sending this report.";
        message.setText(msg);
        report.setText(failure.toString());
        client.hideFullScreen();
        if (isExpanded) {
            toggleDetail();
        }
        Rectangle mainScreen = client.getMainFrame().getGraphicsConfiguration().getBounds();
        Dimension size = getSize();
        int x = (mainScreen.width - size.width) / 2 + mainScreen.x;
        int y = (mainScreen.height - size.height) / 2 + mainScreen.y;
        setLocation(x, y);
        setVisible(true);
    }

    /**
	 * react on input
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (COMMAND_DETAIL.equals(command)) {
            toggleDetail();
        } else if (COMMAND_SEND.equals(command)) {
            sendReport();
        } else if (COMMAND_CLOSE.equals(command)) {
            setVisible(false);
        } else if (COMMAND_EXIT_PROGRAM.equals(command)) {
            System.exit(1);
        }
    }

    /**
	 * toggle expansion state
	 */
    private void toggleDetail() {
        Dimension newSize = getSize();
        if (isExpanded) {
            detail.setText("Show Details");
            remove(detailPanel);
            newSize.height = collapsedHeight;
        } else {
            detail.setText("Hide Details");
            add(detailPanel, BorderLayout.CENTER);
            newSize.height = Math.max(expandedHeight, newSize.height);
        }
        isExpanded = !isExpanded;
        setSize(newSize);
        validate();
    }

    /**
	 * send this report
	 */
    private void sendReport() {
        try {
            String data = URLEncoder.encode("message", "UTF-8") + "=" + URLEncoder.encode(report.getText(), "UTF-8");
            URL url = new URL("http://jimcat.org/reportbug.php");
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            wr.close();
            SwingClient.getInstance().showMessage(sb.toString(), "Error report", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            SwingClient.getInstance().showMessage("There was a problem sending your report. Do you have internet connectivity?", "Error report", JOptionPane.ERROR_MESSAGE);
        }
        setVisible(false);
    }
}
