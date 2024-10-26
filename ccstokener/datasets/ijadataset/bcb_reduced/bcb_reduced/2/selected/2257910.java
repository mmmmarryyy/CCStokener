package graphics;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.net.*;
import java.io.*;

public class TextEditor extends JFrame implements ActionListener {

    private static final long serialVersionUID = -533399076994807851L;

    private JTextArea tA;

    private JMenu M_FILE, M_EDIT;

    private JMenuItem MI_OPEN, MI_SAVE, MI_SAVEAS, MI_CLOSE, MI_EXIT;

    private JMenuItem MI_CUT, MI_COPY, MI_PASTE;

    private JPopupMenu pop;

    private JMenuItem PMI_CUT, PMI_COPY, PMI_PASTE;

    public TextEditor() {
        super.setTitle("Document vide - TextEditor");
        initFrame();
    }

    public TextEditor(String urlName) {
        super.setTitle("Source de " + urlName + " - TextEditor");
        initFrame();
        displayHtmlSource(urlName);
    }

    public void initFrame() {
        Container c = getContentPane();
        JMenuBar menuBar = new JMenuBar();
        M_FILE = new JMenu("Fichier");
        MI_OPEN = new JMenuItem("Ouvrir...");
        MI_OPEN.setMnemonic('O');
        M_FILE.add(MI_OPEN);
        MI_SAVE = new JMenuItem("Enregistrer");
        MI_SAVE.setMnemonic('E');
        M_FILE.add(MI_SAVE);
        MI_SAVEAS = new JMenuItem("Enregistrer sous...");
        MI_SAVEAS.setMnemonic('s');
        M_FILE.add(MI_SAVEAS);
        MI_CLOSE = new JMenuItem("Fermer");
        MI_CLOSE.addActionListener(this);
        MI_CLOSE.setMnemonic('F');
        M_FILE.add(MI_CLOSE);
        M_FILE.setMnemonic('F');
        menuBar.add(M_FILE);
        M_EDIT = new JMenu("Edition");
        MI_CUT = new JMenuItem("Couper");
        MI_CUT.addActionListener(this);
        MI_CUT.setMnemonic('C');
        M_EDIT.add(MI_CUT);
        MI_COPY = new JMenuItem("Copier");
        MI_COPY.addActionListener(this);
        MI_COPY.setMnemonic('p');
        M_EDIT.add(MI_COPY);
        MI_PASTE = new JMenuItem("Coller");
        MI_PASTE.addActionListener(this);
        MI_PASTE.setMnemonic('o');
        M_EDIT.add(MI_PASTE);
        M_EDIT.setMnemonic('E');
        menuBar.add(M_EDIT);
        setJMenuBar(menuBar);
        pop = new JPopupMenu();
        PMI_CUT = new JMenuItem("Couper");
        PMI_CUT.addActionListener(this);
        PMI_CUT.setMnemonic('C');
        pop.add(PMI_CUT);
        PMI_COPY = new JMenuItem("Copier");
        PMI_COPY.addActionListener(this);
        PMI_COPY.setMnemonic('P');
        pop.add(PMI_COPY);
        PMI_PASTE = new JMenuItem("Coller");
        PMI_PASTE.addActionListener(this);
        PMI_PASTE.setMnemonic('O');
        pop.add(PMI_PASTE);
        tA = new JTextArea();
        tA.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                checkPopupTrigger(e);
            }

            public void mouseReleased(MouseEvent e) {
                checkPopupTrigger(e);
            }

            private void checkPopupTrigger(MouseEvent e) {
                if (e.isPopupTrigger()) showContext(e);
            }
        });
        JScrollPane scrollPane = new JScrollPane(tA);
        c.add(scrollPane, BorderLayout.CENTER);
        setSize(600, 500);
        setLocation(200, 75);
        show();
    }

    public void actionPerformed(ActionEvent e) {
        if ("Couper" == e.getActionCommand()) tA.cut();
        if ("Copier" == e.getActionCommand()) tA.copy();
        if ("Coller" == e.getActionCommand()) tA.paste();
        if ("Fermer" == e.getActionCommand()) this.setVisible(false);
    }

    public void showContext(MouseEvent e) {
        updatePopupContext();
        pop.show(e.getComponent(), e.getX(), e.getY());
    }

    public void updatePopupContext() {
        if (tA.getSelectedText() == null) {
            PMI_CUT.setEnabled(false);
            PMI_COPY.setEnabled(false);
        } else {
            PMI_CUT.setEnabled(true);
            PMI_COPY.setEnabled(true);
        }
    }

    public void displayHtmlSource(String urlName) {
        String s;
        StringBuffer buf = new StringBuffer();
        try {
            URL url = new URL(urlName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((s = in.readLine()) != null) buf.append(s + "\n");
            tA.setText(buf.toString());
        } catch (MalformedURLException e) {
            System.err.println("Error 1 :)");
        } catch (FileNotFoundException e) {
            System.err.println("Error 2 :)");
        } catch (IOException e) {
            System.err.println("Error 3 :)");
        }
    }

    public void err(String errorMsg) {
        JOptionPane.showMessageDialog(null, errorMsg, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        TextEditor app = new TextEditor();
        app.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
}
