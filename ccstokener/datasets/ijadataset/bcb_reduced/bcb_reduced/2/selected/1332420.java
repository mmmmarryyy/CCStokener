package org.openfrag.OpenCDS.ui.help;

import org.openfrag.OpenCDS.ui.main.MainFrame;
import org.openfrag.OpenCDS.core.util.Constants;
import org.openfrag.OpenCDS.core.download.FTP;
import org.openfrag.OpenCDS.core.lang.DynamicLocalisation;
import org.openfrag.OpenCDS.ui.util.Util;
import org.openfrag.OpenCDS.core.net.mail.SendMail;
import javax.swing.JOptionPane;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

/**
 * The report bug class.
 *
 * @author  Lars 'Levia' Wesselius
*/
public class ReportBug extends JDialog implements ActionListener {

    private MainFrame m_MainFrame;

    private JTextField m_Subject;

    private JTextField m_Email;

    private JTextArea m_Description;

    private JButton m_Submit;

    private JButton m_Cancel;

    /**
     * The report bug dialog constructor.
    */
    public ReportBug(MainFrame mainFrame, boolean modal) {
        super(mainFrame.getFrame(), mainFrame.getLocalisation().getMessage("ReportBug.Caption"), modal);
        m_MainFrame = mainFrame;
        initialize();
    }

    /**
     * Initializes the dialog.
    */
    public void initialize() {
        DynamicLocalisation loc = m_MainFrame.getLocalisation();
        this.setSize(450, 200);
        this.setLocationRelativeTo(null);
        this.setIconImage(new ImageIcon(ReportBug.class.getResource("/org/openfrag/OpenCDS/ui/icons/help-reportbug.png")).getImage());
        JPanel contentPane = new JPanel();
        contentPane.setOpaque(true);
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 3, 5));
        this.setContentPane(contentPane);
        JLabel emailLabel = new JLabel(loc.getMessage("ReportBug.EmailAddress"), SwingConstants.LEFT);
        m_Email = new JTextField();
        m_Email.setPreferredSize(new Dimension(450, 20));
        m_Email.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subjectLabel = new JLabel(loc.getMessage("ReportBug.Subject"), SwingConstants.LEFT);
        m_Subject = new JTextField();
        m_Subject.setPreferredSize(new Dimension(450, 20));
        m_Subject.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel descrLabel = new JLabel(loc.getMessage("ReportBug.Description"), SwingConstants.LEFT);
        m_Description = new JTextArea();
        m_Description.setLineWrap(true);
        m_Description.setWrapStyleWord(true);
        m_Description.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane scrollPane = new JScrollPane(m_Description, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(450, 100));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        m_Submit = new JButton(loc.getMessage("ReportBug.Submit"));
        m_Submit.setAlignmentX(Component.LEFT_ALIGNMENT);
        m_Submit.addActionListener(this);
        m_Cancel = new JButton(loc.getMessage("Cancel"));
        m_Cancel.addActionListener(this);
        m_Cancel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(m_Submit);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(m_Cancel);
        buttonPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(emailLabel, BorderLayout.PAGE_START);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(m_Email, BorderLayout.LINE_START);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(subjectLabel, BorderLayout.PAGE_START);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(m_Subject, BorderLayout.LINE_START);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(descrLabel, BorderLayout.PAGE_END);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(scrollPane, BorderLayout.PAGE_END);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(buttonPane, BorderLayout.PAGE_END);
        this.pack();
        this.setVisible(true);
    }

    /**
     * The action listeners function
    */
    public void actionPerformed(ActionEvent event) {
        String e = event.getActionCommand();
        DynamicLocalisation loc = m_MainFrame.getLocalisation();
        if (e.equals(loc.getMessage("ReportBug.Submit"))) {
            submitReport();
        } else if (e.equals(loc.getMessage("Cancel"))) {
            this.dispose();
        }
    }

    /**
     * Submits a report.
    */
    public void submitReport() {
        String subject = m_Subject.getText();
        String description = m_Description.getText();
        String email = m_Email.getText();
        if (subject.length() == 0) {
            Util.flashComponent(m_Subject, Color.RED);
            return;
        }
        if (description.length() == 0) {
            Util.flashComponent(m_Description, Color.RED);
            return;
        }
        DynamicLocalisation loc = m_MainFrame.getLocalisation();
        if (email.length() == 0 || email.indexOf("@") == -1 || email.indexOf(".") == -1 || email.startsWith("@")) {
            email = "anonymous@blaat.com";
        }
        try {
            subject = URLEncoder.encode("Bug report: " + subject, "UTF-8");
            description = URLEncoder.encode("A bug report has been submitted: \n\n" + description, "UTF-8");
            URL url = new URL("http://www.leviaswrath.org/mail.php" + "?from=" + email + "&to=" + Constants.BUG_EMAIL + "&subject=" + subject + "&body=" + description);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseMessage().equals("OK")) {
                JOptionPane.showMessageDialog(this, loc.getMessage("ReportBug.SentMessage"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        dispose();
    }
}
