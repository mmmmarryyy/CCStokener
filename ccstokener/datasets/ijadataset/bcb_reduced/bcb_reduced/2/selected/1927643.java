package com.izforge.izpack.installer;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.*;
import java.util.Locale;

/**
 * Dialogs for password authentication and firewall specification, when needed, during web
 * installation.
 *
 * @author Chadwick McHenry
 * @author <a href="vralev@redhat.com">Vladimir Ralev</a>
 * @version 1.0
 */
public class WebAccessor {

    private Thread openerThread = null;

    private InputStream iStream = null;

    private Exception exception = null;

    private Object soloCancelOption = null;

    private Component parent = null;

    private JDialog dialog = null;

    private boolean tryProxy = false;

    private JPanel passwordPanel = null;

    private JLabel promptLabel;

    private JTextField nameField;

    private JPasswordField passField;

    private JPanel proxyPanel = null;

    private JLabel errorLabel;

    private JTextField hostField;

    private JTextField portField;

    private String url;

    private int contentLength = -1;

    /**
     * Not yet Implemented: placeholder for headless installs.
     *
     * @throws UnsupportedOperationException
     */
    public WebAccessor() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a WebAccessor that prompts for proxies and passwords using a JDialog.
     *
     * @param parent determines the frame in which the dialog is displayed; if the parentComponent
     *               has no Frame, a default Frame is used
     */
    public WebAccessor(Component parent) {
        this.parent = parent;
        Locale l = null;
        if (parent != null) {
            parent.getLocale();
        }
        soloCancelOption = UIManager.get("OptionPane.cancelButtonText", l);
        Authenticator.setDefault(new MyDialogAuthenticator());
    }

    /**
     * Opens a URL connection and returns it's InputStream for the specified URL.
     *
     * @param url the url to open the stream to.
     * @return an input stream ready to read, or null on failure
     */
    public InputStream openInputStream(URL url) {
        setUrl(url.toExternalForm());
        OPEN_URL: while (true) {
            startOpening(url);
            Thread.yield();
            int retry = 28;
            while (exception == null && iStream == null && retry > 0) {
                try {
                    Thread.sleep(200);
                    retry--;
                } catch (Exception e) {
                    System.out.println("In openInputStream: " + e);
                }
            }
            if (iStream != null) {
                break;
            }
            if (!tryProxy) {
                break;
            }
            JPanel panel = getProxyPanel();
            errorLabel.setText("Unable to connect: " + exception.getMessage());
            while (true) {
                int result = JOptionPane.showConfirmDialog(parent, panel, "Proxy Configuration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) {
                    break OPEN_URL;
                }
                String host = null;
                String port = null;
                try {
                    InetAddress addr = InetAddress.getByName(hostField.getText());
                    host = addr.getHostName();
                } catch (Exception x) {
                    errorLabel.setText("Unable to resolve Host");
                    Toolkit.getDefaultToolkit().beep();
                }
                try {
                    if (host != null) {
                        port = Integer.valueOf(portField.getText()).toString();
                    }
                } catch (NumberFormatException x) {
                    errorLabel.setText("Invalid Port");
                    Toolkit.getDefaultToolkit().beep();
                }
                if (host != null && port != null) {
                    System.getProperties().put("proxySet", "true");
                    System.getProperties().put("proxyHost", host);
                    System.getProperties().put("proxyPort", port);
                    break;
                }
            }
        }
        if (iStream == null) {
            openerThread.interrupt();
        }
        return iStream;
    }

    private void startOpening(final URL url) {
        final WebAccessor wa = this;
        openerThread = new Thread() {

            public void run() {
                iStream = null;
                try {
                    tryProxy = false;
                    URLConnection connection = url.openConnection();
                    if (connection instanceof HttpURLConnection) {
                        HttpURLConnection htc = (HttpURLConnection) connection;
                        contentLength = htc.getContentLength();
                    }
                    InputStream i = connection.getInputStream();
                    iStream = new LoggedInputStream(i, wa);
                } catch (ConnectException x) {
                    tryProxy = true;
                    exception = x;
                } catch (Exception x) {
                    exception = x;
                } finally {
                    if (dialog != null) {
                        Thread.yield();
                        dialog.setVisible(false);
                    }
                }
            }
        };
        openerThread.start();
    }

    /**
     * Only to be called after an initial error has indicated a connection problem
     */
    private JPanel getProxyPanel() {
        if (proxyPanel == null) {
            proxyPanel = new JPanel(new BorderLayout(5, 5));
            errorLabel = new JLabel();
            JPanel fields = new JPanel(new GridLayout(2, 2));
            String h = (String) System.getProperties().get("proxyHost");
            String p = (String) System.getProperties().get("proxyPort");
            hostField = new JTextField(h != null ? h : "");
            portField = new JTextField(p != null ? p : "");
            JLabel host = new JLabel("Host: ");
            JLabel port = new JLabel("Port: ");
            fields.add(host);
            fields.add(hostField);
            fields.add(port);
            fields.add(portField);
            JLabel exampleLabel = new JLabel("e.g. host=\"gatekeeper.example.com\" port=\"80\"");
            proxyPanel.add(errorLabel, BorderLayout.NORTH);
            proxyPanel.add(fields, BorderLayout.CENTER);
            proxyPanel.add(exampleLabel, BorderLayout.SOUTH);
        }
        proxyPanel.validate();
        return proxyPanel;
    }

    private JPanel getPasswordPanel() {
        if (passwordPanel == null) {
            passwordPanel = new JPanel(new BorderLayout(5, 5));
            promptLabel = new JLabel();
            JPanel fields = new JPanel(new GridLayout(2, 2));
            nameField = new JTextField();
            passField = new JPasswordField();
            JLabel name = new JLabel("Name: ");
            JLabel pass = new JLabel("Password: ");
            fields.add(name);
            fields.add(nameField);
            fields.add(pass);
            fields.add(passField);
            passwordPanel.add(promptLabel, BorderLayout.NORTH);
            passwordPanel.add(fields, BorderLayout.CENTER);
        }
        passField.setText("");
        return passwordPanel;
    }

    /**
     * Authenticates via dialog when needed.
     */
    private class MyDialogAuthenticator extends Authenticator {

        public PasswordAuthentication getPasswordAuthentication() {
            JPanel p = getPasswordPanel();
            String prompt = getRequestingPrompt();
            InetAddress addr = getRequestingSite();
            if (addr != null) {
                prompt += " (" + addr.getHostName() + ")";
            }
            promptLabel.setText(prompt);
            int result = JOptionPane.showConfirmDialog(parent, p, "Enter Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return null;
            }
            return new PasswordAuthentication(nameField.getText(), passField.getPassword());
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getContentLength() {
        return contentLength;
    }
}
