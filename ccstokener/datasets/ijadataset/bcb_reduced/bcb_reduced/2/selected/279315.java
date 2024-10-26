package com.diccionarioderimas.gui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import com.diccionarioderimas.Base64;
import com.diccionarioderimas.Utils;
import com.diccionarioderimas.gui.MainWindow;

public class ContactDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 5078184818007035976L;

    private JTextField email;

    private JTextField name;

    private JComboBox category;

    private JTextField subject;

    private JTextArea message;

    private JButton ok, cancel;

    public ContactDialog(JFrame parent) {
        super(parent, "Contacto", true);
        email = new JTextField();
        name = new JTextField();
        category = new JComboBox(new String[] { "Comentario", "Sugerencia", "Problema" });
        subject = new JTextField();
        message = new JTextArea();
        ok = new JButton("Enviar");
        cancel = new JButton("Cancelar");
        JLabel emailL = new JLabel("Email:");
        JLabel nameL = new JLabel("Nombre:");
        JLabel categoryL = new JLabel("Categor�a:");
        JLabel subjectL = new JLabel("T�tulo:");
        JScrollPane scroll = new JScrollPane(message);
        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(null);
        getContentPane().add(email);
        getContentPane().add(name);
        getContentPane().add(category);
        getContentPane().add(subject);
        getContentPane().add(scroll);
        getContentPane().add(emailL);
        getContentPane().add(nameL);
        getContentPane().add(categoryL);
        getContentPane().add(subjectL);
        getContentPane().add(ok);
        getContentPane().add(cancel);
        emailL.setBounds(100 - emailL.getPreferredSize().width, 15, 100, 30);
        nameL.setBounds(100 - nameL.getPreferredSize().width, 45, 100, 30);
        categoryL.setBounds(100 - categoryL.getPreferredSize().width, 75, 100, 30);
        subjectL.setBounds(100 - subjectL.getPreferredSize().width, 105, 100, 30);
        email.setBounds(110, 15, 200, 25);
        name.setBounds(110, 45, 200, 25);
        category.setBounds(110, 75, 200, 25);
        subject.setBounds(110, 105, 200, 25);
        scroll.setBounds(40, 145, 276, 145);
        ok.setBounds(115, 320, 100, 25);
        cancel.setBounds(215, 320, 100, 25);
        ok.addActionListener(this);
        cancel.addActionListener(this);
        setResizable(false);
        setSize(new Dimension(380, 400));
    }

    public void setVisible(boolean v) {
        if (v == true) {
            Dimension dim = getToolkit().getScreenSize();
            setLocation((dim.width - 380) / 2, (dim.height - 400) / 2);
        }
        super.setVisible(v);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cancel) {
            email.setText("");
            name.setText("");
            category.setSelectedIndex(0);
            subject.setText("");
            message.setText("");
            setVisible(false);
        } else {
            StringBuffer errors = new StringBuffer();
            if (email.getText().trim().equals("")) errors.append("El campo 'Email' es obligatorio<br/>");
            if (name.getText().trim().equals("")) errors.append("El campo 'Nombre' es obligatorio<br/>");
            if (subject.getText().trim().equals("")) errors.append("El campo 'T�tulo' es obligatorio<br/>");
            if (message.getText().trim().equals("")) errors.append("No hay conrtenido en el mensaje<br/>");
            if (errors.length() > 0) {
                JOptionPane.showMessageDialog(this, "<html><b>Error</b><br/>" + errors.toString() + "</html>", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                try {
                    StringBuffer params = new StringBuffer();
                    params.append("name=").append(URLEncoder.encode(name.getText(), "UTF-8")).append("&category=").append(URLEncoder.encode((String) category.getSelectedItem(), "UTF-8")).append("&title=").append(URLEncoder.encode(subject.getText(), "UTF-8")).append("&email=").append(URLEncoder.encode(email.getText(), "UTF-8")).append("&id=").append(URLEncoder.encode(MainWindow.getUserPreferences().getUniqueId() + "", "UTF-8")).append("&body=").append(URLEncoder.encode(message.getText(), "UTF-8"));
                    URL url = new URL("http://www.cronopista.com/diccionario2/sendMessage.php");
                    URLConnection connection = url.openConnection();
                    Utils.setupProxy(connection);
                    connection.setDoOutput(true);
                    OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                    out.write(params.toString());
                    out.close();
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String decodedString;
                    while ((decodedString = in.readLine()) != null) {
                        System.out.println(decodedString);
                    }
                    in.close();
                    email.setText("");
                    name.setText("");
                    category.setSelectedIndex(0);
                    subject.setText("");
                    message.setText("");
                    setVisible(false);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "<html><b>Error</b><br/>Ha ocurrido un error enviando tu mensaje.<br/>" + "Por favor, int�ntalo m�s tarde o ponte en contacto conmigo a trav�s de www.cronopista.com</html>", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
