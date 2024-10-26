package net.sf.csv2sql.frontends.gui.simple;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.swing.JFrame;

/**
 * @author <a href="mailto:dconsonni@enter.it">Davide Consonni</a>
 */
public class JFrameHelp extends JFrame {

    public JFrameHelp(int demo) {
        initComponents();
        try {
            this.setTitle("Help :: live descriptor example number " + demo);
            jTextPane1.setContentType("text/plain");
            URL url = new URL("http://csvtosql.sourceforge.net/descriptors/" + demo + "/descriptor.xml");
            BufferedReader html = new BufferedReader(new InputStreamReader(url.openStream()));
            String strHtmlrow = new String();
            StringBuffer sb = new StringBuffer();
            while ((strHtmlrow = html.readLine()) != null) {
                sb.append(strHtmlrow + "\n");
            }
            jTextPane1.setText(sb.toString());
            jTextPane1.setCaretPosition(0);
        } catch (FileNotFoundException e) {
            jTextPane1.setText("this example no longer exist. sorry :)");
        } catch (Exception e) {
            jTextPane1.setText("need internet connection.");
        }
    }

    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jTextPane1.setEditable(false);
        jTextPane1.setFont(new java.awt.Font("Verdana", 0, 10));
        jScrollPane1.setViewportView(jTextPane1);
        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - 520) / 2, (screenSize.height - 382) / 2, 520, 382);
    }

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTextPane jTextPane1;
}
