package net.sf.karatasi.client;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import net.sf.japi.net.rest.Http11Header;
import net.sf.japi.swing.action.ActionBuilder;
import net.sf.japi.swing.action.ActionBuilderFactory;
import net.sf.japi.swing.action.ActionMethod;
import net.sf.karatasi.Util;

/**
 * Main class of the Karatasi Client.
 *
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 */
public class ClientMain {

    /** The ActionBuilder. */
    private final ActionBuilder actionBuilder = ActionBuilderFactory.getInstance().getActionBuilder(ClientMain.class);

    /** The client window. */
    private final JFrame frame;

    /** The JTextArea. */
    private final JTextArea textArea;

    /** The file chooser. */
    private final JFileChooser fileChooser;

    /** The test port. */
    private final int testPort = 8089;

    /** Main method.
     * @param args Command line arguments (ignored).
     */
    public static void main(final String... args) {
        new ClientMain();
    }

    /** Creates a ClientMain. */
    public ClientMain() {
        fileChooser = new JFileChooser();
        frame = new JFrame(actionBuilder.getString("window.title"));
        frame.setJMenuBar(actionBuilder.createMenuBar(true, "main", this));
        frame.getContentPane().add(actionBuilder.createToolBar(this, "main"), BorderLayout.NORTH);
        textArea = new JTextArea();
        textArea.setEditable(false);
        frame.getContentPane().add(textArea);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /** Performs a list operation in the new protocol.
     * @throws IOException In case of I/O problems.
     */
    @ActionMethod
    public void list() throws IOException {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/list?version=1000");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty(Http11Header.AUTHORIZATION, "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        con.setRequestProperty(Http11Header.WWW_AUTHENTICATE, "Basic realm=\"karatasi\"");
        final InputStream in = con.getInputStream();
        final byte[] buf = new byte[4096];
        textArea.setText("");
        for (int bytesRead; (bytesRead = in.read(buf)) != -1; ) {
            textArea.append(new String(buf, 0, bytesRead));
        }
    }

    /** Performs a mirror operation in the new protocol.
     * @throws IOException In case of I/O problems.
     */
    @ActionMethod
    public void mirror() throws IOException {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/mirror");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty(Http11Header.AUTHORIZATION, "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        con.setRequestProperty(Http11Header.WWW_AUTHENTICATE, "Basic realm=\"karatasi\"");
        final InputStream in = con.getInputStream();
        final byte[] buf = new byte[4096];
        textArea.setText("");
        for (int bytesRead; (bytesRead = in.read(buf)) != -1; ) {
            textArea.append(new String(buf, 0, bytesRead));
        }
    }

    /** Performs a download operation in the new protocol.
     * @throws IOException In case of I/O problems.
     */
    @ActionMethod
    public void download() throws IOException {
        final JPanel message = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcLabel = new GridBagConstraints();
        final GridBagConstraints gbcField = new GridBagConstraints();
        gbcLabel.weightx = 0.0;
        gbcField.weightx = 1.0;
        gbcField.fill = GridBagConstraints.HORIZONTAL;
        gbcField.insets = new Insets(2, 2, 2, 2);
        final JTextField deviceField, fullnameField, versionField;
        deviceField = new JTextField();
        fullnameField = new JTextField();
        versionField = new JTextField();
        gbcField.gridwidth = GridBagConstraints.REMAINDER;
        message.add(new JLabel("device"), gbcLabel);
        message.add(deviceField, gbcField);
        message.add(new JLabel("fullname"), gbcLabel);
        message.add(fullnameField, gbcField);
        message.add(new JLabel("version"), gbcLabel);
        message.add(versionField, gbcField);
        final int result = JOptionPane.showConfirmDialog(frame, message, "Download parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        final String device = deviceField.getText();
        final String fullname = fullnameField.getText();
        final String version = versionField.getText();
        final URL url = new URL("http://127.0.0.1:" + testPort + "/databases/" + fullname + "?device=" + device + "&version=" + version);
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty(Http11Header.AUTHORIZATION, "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        con.setRequestProperty(Http11Header.WWW_AUTHENTICATE, "Basic realm=\"karatasi\"");
        final InputStream in = con.getInputStream();
        try {
            final int fileResult = fileChooser.showSaveDialog(frame);
            if (fileResult != JFileChooser.APPROVE_OPTION) {
                return;
            }
            final OutputStream out = new FileOutputStream(fileChooser.getSelectedFile());
            try {
                Util.copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    /** Performs an upload operation in the new protocol.
     * @throws IOException In case of I/O problems.
     */
    @ActionMethod
    public void upload() throws IOException {
        final int fileResult = fileChooser.showOpenDialog(frame);
        if (fileResult != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final InputStream in = new FileInputStream(fileChooser.getSelectedFile());
        try {
            final URL url = new URL("http://127.0.0.1:" + testPort + "/databases/" + fileChooser.getSelectedFile().getName());
            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            con.setDoOutput(true);
            con.setRequestProperty(Http11Header.AUTHORIZATION, "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
            con.setRequestProperty(Http11Header.WWW_AUTHENTICATE, "Basic realm=\"karatasi\"");
            con.setRequestProperty(Http11Header.CONTENT_LENGTH, Long.toString(fileChooser.getSelectedFile().length()));
            con.setRequestProperty(Http11Header.CONTENT_TYPE, "application/octet-stream");
            final OutputStream out = con.getOutputStream();
            try {
                Util.copy(in, out);
                con.connect();
                final InputStream in2 = con.getInputStream();
                try {
                    textArea.setText("");
                    final byte[] buf = new byte[4096];
                    for (int bytesRead; (bytesRead = in2.read(buf)) != -1; ) {
                        textArea.append(new String(buf, 0, bytesRead));
                    }
                } finally {
                    in2.close();
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    /** Quits the test client. */
    @ActionMethod
    public void quit() {
        frame.dispose();
    }
}
