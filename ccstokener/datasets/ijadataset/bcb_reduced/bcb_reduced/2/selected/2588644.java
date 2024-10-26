package org.test.id.prover;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import com.microsoft.uprove.IssuerParameters;
import com.microsoft.uprove.ProverProtocolParameters;
import com.microsoft.uprove.UProveKeyAndToken;
import static org.test.id.util.Converter.*;

public class Prover {

    URL issuer;

    IssuerParameters ip;

    JTextArea iparea;

    public Prover() {
        final JFrame f = new JFrame("Prover");
        f.getContentPane().setLayout(new BorderLayout());
        final JPanel urlPane = new JPanel(new BorderLayout());
        final JTextField urlField = new JTextField("http://testidissuer.appspot.com/testidissuer/");
        urlField.setToolTipText("<html>Hier steht die URL des Issuers. <br>Für das Testsystem ist die schon korrekt eingegeben. <br>Wer aber etwas mehr rumspielen möchte, kann sie ja ändern ;-) </html>");
        urlPane.add(urlField, "Center");
        JButton urlload = new JButton("load");
        urlPane.add(urlload, "East");
        f.getContentPane().add(urlPane, "North");
        iparea = new JTextArea("no issuer loaded", 3, 30);
        iparea.setToolTipText("Hier stehen ein paar Infos über den Issuer, die sind eigentlich unwichtig.");
        f.getContentPane().add(new JScrollPane(iparea), "Center");
        JPanel buttons = new JPanel(new GridLayout(2, 1));
        final JButton tokenButton = new JButton("get Token");
        tokenButton.setToolTipText("<html>Hier klicken, wenn man sich ein neuen Token vom Issuer ausstellen lassen möchte. <br>Man benötigt einen Token, um Nachrichten an den Verifier zu schicken.</html>");
        tokenButton.setEnabled(false);
        buttons.add(tokenButton);
        JButton loadButton = new JButton("load Token");
        loadButton.setToolTipText("<html>Hier klicken, wenn man schon ein Token generiert (und gespeichert) hat und das wiederverwenden will.</html>");
        buttons.add(loadButton);
        f.getContentPane().add(buttons, "South");
        urlload.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    issuer = new URL(urlField.getText());
                    loadIssuerParameters(f);
                    if (ip != null) tokenButton.setEnabled(true); else tokenButton.setEnabled(false);
                } catch (MalformedURLException e1) {
                    JOptionPane.showMessageDialog(f, "Malformed URL!");
                }
            }
        });
        tokenButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getTokenDialog(f);
            }
        });
        loadButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TokenUI.loadTokenUI(f);
            }
        });
        f.pack();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }

    private void loadIssuerParameters(JFrame f) {
        try {
            URL ipURL = new URL(issuer, "parameters");
            BufferedReader br = new BufferedReader(new InputStreamReader(ipURL.openStream()));
            ip = readIp(br);
            ip.validate();
            iparea.setText(new String(ip.getParametersUID()) + "\n" + new String(ip.getSpecification()) + "\n" + new String(ip.getHashAlgorithmUID()));
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(f, e.getMessage());
            ip = null;
            iparea.setText("no issuer loaded");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(f, e.getMessage());
            ip = null;
            iparea.setText("no issuer loaded");
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(f, e.getMessage());
            ip = null;
            iparea.setText("no issuer loaded");
        }
    }

    private void getTokenDialog(JFrame f) {
        final JDialog d = new JDialog(f, "Attribute des Token");
        d.getContentPane().setLayout(new BorderLayout());
        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        final JTextField realname = new JTextField();
        final JTextField service = new JTextField();
        final JTextField time = new JTextField();
        final JTextField pseudonym = new JTextField();
        JLabel realnameLabel = new JLabel("Realname");
        realnameLabel.setToolTipText("<html>Mit dem \"Realname\" identifiziert man sich beim Issuer. <br>Dieser wird also zum Issuer gesendet.</html>");
        realname.setToolTipText("<html>Mit dem \"Realname\" identifiziert man sich beim Issuer. <br>Dieser wird also zum Issuer gesendet.</html>");
        JLabel serviceLabel = new JLabel("Service");
        serviceLabel.setToolTipText("<html>Mit \"Service\" gibt man an für welchen Dienst (z.B. forum, lqfb, lqfb.themenbereich, ...) man eine Authentifizierung möchte. <br>Auch dies wird zum Issuer gesendet.</html>");
        service.setToolTipText("<html>Mit \"Service\" gibt man an für welchen Dienst (z.B. forum, lqfb, lqfb.themenbereich, ...) man eine Authentifizierung möchte. <br>Auch dies wird zum Issuer gesendet.</html>");
        JLabel timeLabel = new JLabel("Zeitraum");
        timeLabel.setToolTipText("<html>Mit \"Zeitraum\" gibt man an für welchen Zeitraum man eine Authentifizierung möchte. <br>Der Issuer wird prüfen, dass es keinen Zeitpunkt gibt, wo man zwei Authentifizierungen gleichzeitig hat. <br>Auch dies wird somit zum Issuer gesendet.</html>");
        time.setToolTipText("<html>Mit \"Zeitraum\" gibt man an für welchen Zeitraum man eine Authentifizierung möchte. <br>Der Issuer wird prüfen, dass es keinen Zeitpunkt gibt, wo man zwei Authentifizierungen gleichzeitig hat. <br>Auch dies wird somit zum Issuer gesendet.</html>");
        JLabel pseudonymLabel = new JLabel("Pseudonym");
        pseudonymLabel.setToolTipText("<html>Mit \"Pseudonym\" gibt man das Pseudonym an mit dem man agieren möchte. <br>Das Pseudonym kann freigewählt werden, eventuell müssen Konflikte mit bereits existierenden Pseudonymen vermieden werden. <br>Das Pseudonym wird nicht dem Issuer übermittelt, wird aber dennoch vom Issuer signiert.</html>");
        pseudonym.setToolTipText("<html>Mit \"Pseudonym\" gibt man das Pseudonym an mit dem man agieren möchte. <br>Das Pseudonym kann freigewählt werden, eventuell müssen Konflikte mit bereits existierenden Pseudonymen vermieden werden. <br>Das Pseudonym wird nicht dem Issuer übermittelt, wird aber dennoch vom Issuer signiert.</html>");
        inputPanel.add(realnameLabel);
        inputPanel.add(realname);
        inputPanel.add(serviceLabel);
        inputPanel.add(service);
        inputPanel.add(timeLabel);
        inputPanel.add(time);
        inputPanel.add(pseudonymLabel);
        inputPanel.add(pseudonym);
        d.getContentPane().add(inputPanel, "Center");
        JButton send = new JButton("Anfrage absenden");
        send.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    getToken(realname.getText(), service.getText(), time.getText(), pseudonym.getText());
                    d.dispose();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(d, "Failed to get Token\n" + e1.getMessage());
                }
            }
        });
        d.getContentPane().add(send, "South");
        d.pack();
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setVisible(true);
    }

    public void getToken(String realname, String service, String time, String pseudonym) throws IOException {
        URL m1url = new URL(issuer, "message1?realname=" + realname + "&service=" + service + "&time=" + time);
        System.out.println("---->");
        System.out.println("Request: " + m1url);
        BufferedReader br = new BufferedReader(new InputStreamReader(m1url.openConnection().getInputStream()));
        String num = br.readLine();
        String tokenInformation = br.readLine();
        String message1 = br.readLine();
        br.close();
        System.out.println("<----");
        System.out.println(num);
        System.out.println(tokenInformation);
        System.out.println(message1);
        byte[][] attributes = buildAttributes(realname, service, time);
        ProverProtocolParameters proverProtocolParams = new ProverProtocolParameters();
        proverProtocolParams.setIssuerParameters(ip);
        proverProtocolParams.setNumberOfTokens(1);
        proverProtocolParams.setTokenAttributes(attributes);
        proverProtocolParams.setTokenInformation(convert(tokenInformation));
        proverProtocolParams.setProverInformation(pseudonym.getBytes("UTF-8"));
        com.microsoft.uprove.Prover prover = proverProtocolParams.generate();
        byte[][] message2 = prover.generateSecondMessage(convertArray(message1));
        String msg2str = num + "\n" + convert(message2);
        URL m3url = new URL(issuer, "message3");
        System.out.println("---->");
        System.out.println("Request: " + m3url);
        System.out.println(msg2str);
        HttpURLConnection conn = (HttpURLConnection) m3url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        PrintWriter pw = new PrintWriter(conn.getOutputStream());
        pw.print(msg2str);
        pw.flush();
        BufferedReader br3 = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String message3 = br3.readLine();
        conn.disconnect();
        System.out.println("<----");
        System.out.println(message3);
        UProveKeyAndToken[] upkt = prover.generateTokens(convertArray(message3));
        System.out.println(upkt.length + " token generated:");
        System.out.println("IssuerParametersUID: " + new String(upkt[0].getToken().getIssuerParametersUID()));
        System.out.println("TokenInformation: " + new String(upkt[0].getToken().getTokenInformation()));
        System.out.println("ProverInformation: " + new String(upkt[0].getToken().getProverInformation()));
        System.out.println("Token Private Key: " + convert(upkt[0].getTokenPrivateKey()));
        System.out.println("Token Public Key: " + convert(upkt[0].getToken().getPublicKey()));
        System.out.println("SigmaC: " + convert(upkt[0].getToken().getSigmaC()));
        System.out.println("SigmaR: " + convert(upkt[0].getToken().getSigmaR()));
        System.out.println("SigmaZ: " + convert(upkt[0].getToken().getSigmaZ()));
        for (int i = 0; i < upkt.length; i++) {
            new TokenUI(upkt[i], realname, service, time, ip);
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        new Prover();
    }
}
