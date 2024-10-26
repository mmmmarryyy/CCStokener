import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.SoftBevelBorder;

/**
 * @author Markus Plessing 
 */
public class ErrorHandler {

    public static String logFile;

    private static JFrame errorFrame = null;

    private StringBuffer stackTrace = new StringBuffer();

    private JTextArea outputArea;

    private JTextArea infoArea;

    private JFileChooser fc2;

    private JPanel errorConsole;

    private JPanel buttons = new JPanel(new java.awt.BorderLayout());

    private JPanel clicker = new JPanel(new java.awt.BorderLayout());

    private JMenu menu = new JMenu("Datei");

    private JFixedTextField name = new JFixedTextField("", 20, 64, false);

    private JFixedTextField mail = new JFixedTextField("", 20, 64, false);

    private boolean status = false;

    private boolean modal = true;

    private JFrame infoFrame;

    private AbstractAction exit = new AbstractAction("Abbrechen") {

        public void actionPerformed(ActionEvent e) {
            resetAll();
        }
    };

    private JButton cancel = new JButton(exit);

    private JButton click = new JButton("Details anzeigen >>>");

    private JButton send = new JButton("Fehlerdaten übermitteln");

    private JButton info = new JButton("Datenschutz");

    public ErrorHandler() {
    }

    public ErrorHandler(Exception ex) {
        final ErrorHandler stream = this;
        if (errorFrame == null) {
            fc2 = new JFileChooser(new File(System.getProperty("user.dir")));
            StackTraceElement[] elements = ex.getStackTrace();
            for (int i = 0; i < elements.length; i++) {
                stackTrace.append(elements[i]).append("\n");
            }
            GridBagConstraints c = new GridBagConstraints();
            errorConsole = new JPanel(new GridBagLayout());
            errorConsole.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            clicker.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            errorFrame = new JFrame() {

                protected void processWindowEvent(WindowEvent e) {
                    super.processWindowEvent(e);
                    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
                        this.setVisible(false);
                        resetAll();
                    }
                }
            };
            errorFrame.addWindowFocusListener(new WindowFocusListener() {

                public void windowGainedFocus(WindowEvent e) {
                }

                public void windowLostFocus(WindowEvent e) {
                    if (errorFrame != null && modal) errorFrame.toFront();
                }
            });
            errorFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            outputArea = new JTextArea(15, 40);
            JMenuBar menuBar = new JMenuBar();
            menu.setEnabled(false);
            errorFrame.setTitle("Fehlerausgabe");
            if (stackTrace.length() > 0) {
                outputArea.setText(stackTrace.toString());
            } else {
                outputArea.setText(ex.toString());
            }
            JMenuItem item1 = new JMenuItem("Alles markieren");
            JMenuItem item2 = new JMenuItem("Speichern unter");
            JMenuItem item3 = new JMenuItem("Schließen");
            JMenuItem item4 = new JMenuItem("Alles kopieren");
            menu.add(item1);
            menu.add(item4);
            menu.add(item2);
            menu.addSeparator();
            menu.add(item3);
            menuBar.add(menu);
            item1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    outputArea.setSelectionStart(0);
                    outputArea.setSelectionEnd(outputArea.getText().length());
                }
            });
            item2.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    if (fc2.showSaveDialog(errorConsole) != JFileChooser.CANCEL_OPTION) {
                        File file = fc2.getSelectedFile();
                        if (file != null) {
                            try {
                                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                                writer.write(outputArea.getText());
                                writer.close();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            });
            item3.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    errorFrame.dispose();
                    resetAll();
                }
            });
            item4.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    outputArea.setSelectionStart(0);
                    outputArea.setSelectionEnd(outputArea.getText().length());
                    StringSelection ss = new StringSelection(outputArea.getSelectedText());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                }
            });
            errorFrame.setJMenuBar(menuBar);
            click.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {
                    if (status == false) {
                        errorFrame.getContentPane().add(errorConsole, java.awt.BorderLayout.SOUTH);
                        buttons.add(cancel, BorderLayout.WEST);
                        clicker.remove(cancel);
                        ((JButton) e.getSource()).setText("<<< Details ausblenden");
                        menu.setEnabled(true);
                        status = true;
                    } else {
                        errorFrame.getContentPane().remove(errorConsole);
                        buttons.remove(cancel);
                        clicker.add(cancel, BorderLayout.WEST);
                        ((JButton) e.getSource()).setText("Details anzeigen >>>");
                        menu.setEnabled(false);
                        status = false;
                    }
                    errorFrame.pack();
                    Dimension size = errorFrame.getSize();
                    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                    errorFrame.setLocation(screen.width / 2 - size.width / 2, screen.height / 2 - size.height / 2);
                }
            });
            send.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {
                    submitBug(name.getText(), mail.getText(), infoArea.getText(), stackTrace);
                }
            });
            info.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {
                    modal = false;
                    showInfoWindow(e);
                }
            });
            clicker.add(click, java.awt.BorderLayout.EAST);
            errorFrame.getContentPane().setLayout(new java.awt.BorderLayout());
            JTextArea error = new JTextArea(5, 40);
            error.setEditable(false);
            error.setBackground(new Color(204, 204, 204));
            error.setFont(new Font("Times", Font.BOLD, 12));
            error.setText(ex.getLocalizedMessage());
            JScrollPane errorpane = new JScrollPane(error);
            errorpane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black), BorderFactory.createEmptyBorder(5, 5, 5, 5))));
            errorFrame.getContentPane().add(errorpane, BorderLayout.NORTH);
            errorFrame.getContentPane().add(clicker, java.awt.BorderLayout.CENTER);
            infoArea = new JTextArea("", 5, 30);
            JScrollPane infoPane = new JScrollPane(infoArea);
            outputArea.setEditable(false);
            JScrollPane pane = new JScrollPane(outputArea);
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.BOTH;
            errorConsole.add(pane, c);
            JPanel textpanel = new JPanel(new GridLayout(3, 2));
            textpanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            textpanel.add(new JLabel("Dein Name: "));
            textpanel.add(name);
            textpanel.add(new JLabel("Deine Emailadresse: "));
            textpanel.add(mail);
            textpanel.add(new JLabel("Beschreibung: "));
            textpanel.add(new JLabel(""));
            c.gridy = 1;
            errorConsole.add(textpanel, c);
            c.gridy = 2;
            errorConsole.add(infoPane, c);
            info.setIcon(IconUtils.getGeneralIcon("About", 16));
            buttons.add(send, BorderLayout.EAST);
            buttons.add(info, BorderLayout.CENTER);
            clicker.add(cancel, BorderLayout.WEST);
            c.gridy = 3;
            errorConsole.add(buttons, c);
            errorFrame.pack();
            Dimension size = errorFrame.getSize();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            errorFrame.setLocation(screen.width / 2 - size.width / 2, screen.height / 2 - size.height / 2);
            errorFrame.show();
        }
    }

    public static void createLogfile(boolean isOut, boolean isErr) throws FileNotFoundException {
        String date = "" + new java.util.Date();
        String date_ = date.replace(' ', '_');
        java.io.File f = new java.io.File(System.getProperty("user.dir") + "/errorLog/Logfile_" + date_);
        logFile = f.getAbsolutePath();
        java.io.PrintStream printStream = new java.io.PrintStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(logFile)), true);
        if (isOut) System.setOut(printStream);
        if (isErr) System.setErr(printStream);
    }

    /**
     * submitBug : submit the bugrelated Data to our online-database
     * @param str String the street to search for
     */
    public void submitBug(String mail, String user, String inform, StringBuffer trace) {
        URLConnection urlConn = null;
        URL url;
        OutputStreamWriter printout;
        InputStreamReader input;
        BufferedReader reader;
        modal = false;
        try {
            url = new URL("http://interna.sourceforge.net/headers.php");
            urlConn = url.openConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try {
            printout = new OutputStreamWriter(urlConn.getOutputStream());
            String content = "javaversion=" + URLEncoder.encode(System.getProperty("java.version"), "iso-8859-1") + "&country=" + URLEncoder.encode(System.getProperty("user.country"), "iso-8859-1") + "&os=" + URLEncoder.encode(System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version"), "iso-8859-1") + "&vendor=" + URLEncoder.encode(System.getProperty("java.vendor"), "iso-8859-1") + "&pversion=" + URLEncoder.encode(CustAppl.VERSION, "iso-8859-1") + "&report=" + URLEncoder.encode(inform, "iso-8859-1") + "&trace=" + URLEncoder.encode(" ( nicht angezeigt ) ", "iso-8859-1") + "&mail=" + URLEncoder.encode(mail, "iso-8859-1") + "&name=" + URLEncoder.encode(user, "iso-8859-1");
            if (JOptionPane.showConfirmDialog(null, "Diese Daten werden zusätzlich zur\ndetailierten Fehlerbeschreibung übermittelt:\n\n" + URLDecoder.decode(content, "iso-8859-1").replaceAll("&", "\n"), "Abschicken?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                content.replaceFirst("(nicht angezeigt)", URLEncoder.encode(trace.toString(), "iso-8859-1"));
                String base64content = "c=" + Base64.base64Encode(content);
                printout.write(base64content.toCharArray());
                printout.flush();
                printout.close();
                input = new InputStreamReader(urlConn.getInputStream());
                String string;
                reader = new BufferedReader(input);
                while (null != ((string = reader.readLine()))) {
                    System.err.println(string);
                    if (string.equals("OK")) JOptionPane.showMessageDialog(null, "Die Übertragung war erfolgreich!\nVielen Dank für ihre Hilfe.", "Information", JOptionPane.INFORMATION_MESSAGE); else {
                        JOptionPane.showMessageDialog(null, "Übertragungsfehler!", "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
                resetAll();
                input.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void resetAll() {
        if (errorFrame != null) {
            Component[] all = errorFrame.getComponents();
            for (int i = 0; i < all.length; i++) {
                all[i] = null;
            }
            status = false;
            errorFrame.dispose();
            errorFrame = null;
        }
    }

    public void showInfoWindow(MouseEvent e) {
        if (infoFrame == null) {
            String content = "<html>" + "Sämtliche von ihnen angegebenen Informationen<br>oder von uns automatisiert ermittelten Daten<br> " + "werden streng vertraulich behandelt und nur zum<br>Zweck der schnellen Fehlerbehebung genutzt.<br> " + "Wir versichern  Ihnen die Daten nicht an andere<br>Personen oder Firmen weiterzugeben und sie<br> " + "ausserdem zeitnah nach der Fehlerbehebung zu <br>löschen.<br><br>" + "Die Personenbezogenen Daten werden abgefragt<br>um mit Ihnen in Kontakt zu treten sollten noch<br>" + "Fragen bezüglich des gemeldeten Fehlers auftreten.<br>Sofern sie dies vermeiden möchten steht es Ihnen<br>" + "frei die Felder leer zu lassen. <br><br>Wir bedanken uns für Ihre Mithilfe. <br><br>Ihr Interna-Team</html>";
            infoFrame = new JFrame();
            infoFrame.setUndecorated(true);
            infoFrame.setBackground(Color.white);
            JPanel contentPane = new JPanel();
            JLabel contentLabel = new JLabel(content);
            contentPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(SoftBevelBorder.RAISED), BorderFactory.createLineBorder(Color.black)));
            contentPane.add(contentLabel);
            infoFrame.setContentPane(contentPane);
            contentPane.setBackground(Color.white);
            contentLabel.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {
                    if (infoFrame != null) {
                        infoFrame.dispose();
                        infoFrame = null;
                    }
                }
            });
            infoFrame.addWindowFocusListener(new WindowFocusListener() {

                public void windowGainedFocus(WindowEvent e) {
                }

                public void windowLostFocus(WindowEvent e) {
                    if (infoFrame != null) {
                        infoFrame.dispose();
                        infoFrame = null;
                    }
                    modal = true;
                }
            });
            infoFrame.pack();
            Dimension size = infoFrame.getSize();
            infoFrame.setLocation(((JButton) e.getSource()).getLocationOnScreen().x - (size.width / 2), ((JButton) e.getSource()).getLocationOnScreen().y - (size.height - 20));
            infoFrame.show();
        }
    }

    public static void main(String args[]) {
        new ErrorHandler(new Exception("Ich bin eine tolle Exception"));
    }
}
