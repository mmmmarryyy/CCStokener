package atheneum.client;

import atheneum.shared.AthenBarcode;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 *
 * @author Paul Smith
 * @deprecated use AthenResponseViewer instead
 */
public class AthenItemInformationComponent extends JPanel implements AthenSearchListener {

    private enum AthenOutputMode {

        HTML, XML
    }

    ;

    private JEditorPane m_information;

    private JComboBox m_outputMode;

    private Transformer m_t;

    private String m_xml, m_html;

    public AthenItemInformationComponent() {
        super(new BorderLayout());
        m_information = new JEditorPane();
        m_information.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(m_information, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        m_outputMode = new JComboBox(AthenOutputMode.values());
        m_outputMode.addActionListener(new AthenOutputModeHandler());
        add(m_outputMode, BorderLayout.SOUTH);
        InputStream tStream = null;
        StreamSource tSource = null;
        try {
            tStream = new FileInputStream("atheneum.xsl");
            tSource = new StreamSource(tStream);
            m_t = TransformerFactory.newInstance().newTransformer(tSource);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.toString(), "", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (tStream != null) {
                try {
                    tStream.close();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.toString(), "", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public void search(AthenBarcode barcode) {
        if (barcode == null) {
            throw new NullPointerException("barcode was null");
        }
        String page = AthenClientUtils.getServicesPath() + "/search";
        switch(barcode.getType()) {
            case UPC:
                page += "?upc=" + barcode;
                break;
            case ISBN13:
                page += "?isbn13=" + barcode;
                break;
            case ISBN10:
                page += "?isbn10=" + barcode;
                break;
        }
        m_xml = "";
        m_html = "";
        URL url;
        InputStreamReader input = null;
        StringBuffer xml = new StringBuffer(0);
        char[] buffer = new char[1024];
        StringReader sourceReader = null;
        StringWriter resultWriter = null;
        StreamSource source = null;
        StreamResult result = null;
        try {
            url = new URL(page);
            input = new InputStreamReader(url.openStream());
            while (true) {
                int read = input.read(buffer);
                if (read == -1) {
                    break;
                }
                xml.append(buffer, 0, read);
            }
            input.close();
            input = null;
            m_xml = xml.toString();
            if (m_t != null) {
                sourceReader = new StringReader(m_xml);
                resultWriter = new StringWriter();
                source = new StreamSource(sourceReader);
                result = new StreamResult(resultWriter);
                m_t.transform(source, result);
                m_html = resultWriter.toString();
                sourceReader.close();
                resultWriter.close();
                sourceReader = null;
                resultWriter = null;
            }
            display();
        } catch (Exception ex) {
            ex.printStackTrace();
            m_information.setContentType("text/plain");
            m_information.setText(ex.toString() + "\n\n" + m_xml);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
                if (sourceReader != null) {
                    sourceReader.close();
                }
                if (resultWriter != null) {
                    resultWriter.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                m_information.setContentType("text/plain");
                m_information.setText(ex.toString() + "\n\n" + m_xml);
            }
        }
    }

    private void display() {
        switch((AthenOutputMode) m_outputMode.getSelectedItem()) {
            case XML:
                m_information.setContentType("text/xml");
                m_information.setText(m_xml);
                break;
            case HTML:
            default:
                if (m_html != null && m_html.length() > 0) {
                    m_information.setContentType("text/html");
                    m_information.setText(m_html);
                } else {
                    m_information.setContentType("text/xml");
                    m_information.setText(m_xml);
                }
        }
    }

    private class AthenOutputModeHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            display();
        }
    }
}
