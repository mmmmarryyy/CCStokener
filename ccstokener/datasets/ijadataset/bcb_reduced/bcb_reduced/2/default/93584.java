import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

/**
 * @author Massimo Bartoletti
 * @version 1.1
 */
public class CGSource extends JPanel {

    private CGDemoModule demo;

    private String filename;

    private String sourceCode = null;

    private boolean rendered = false;

    private JEditorPane sourcePane;

    public CGSource(CGDemoModule demo, String filename) {
        this.demo = demo;
        this.filename = filename;
        setLayout(new BorderLayout());
        setBackground(Color.white);
        sourcePane = new JEditorPane();
        sourcePane.setContentType("text/html");
        sourcePane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(sourcePane);
        add(scrollPane, BorderLayout.CENTER);
    }

    public CGDemoModule getDemo() {
        return demo;
    }

    public String getString(String key) {
        return getDemo().getString(key);
    }

    private static final int MAX_SOURCE_LENGTH = 50000;

    public void loadSourceCode() {
        int length = MAX_SOURCE_LENGTH;
        try {
            File file = new File(filename);
            length = (int) file.length();
        } catch (SecurityException ex) {
        }
        char[] buff = new char[length];
        InputStream is;
        InputStreamReader isr;
        CodeViewer cv = new CodeViewer();
        URL url;
        try {
            url = getClass().getResource(filename);
            is = url.openStream();
            isr = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(isr);
            sourceCode = new String("<html><pre>");
            String line = reader.readLine();
            while (line != null) {
                sourceCode += cv.syntaxHighlight(line) + " \n ";
                line = reader.readLine();
            }
            sourceCode += "</pre></html>";
        } catch (Exception ex) {
            sourceCode = getString("SourceCode.error");
        }
    }

    public String getSourceCode() {
        if (sourceCode == null) loadSourceCode();
        return sourceCode;
    }

    /**
     * Loads and puts the source code text into JEditorPane
     */
    public void setSourceCode() {
        if (!rendered) {
            sourcePane.setText(getString("SourceCode.loading"));
            revalidate();
            repaint();
            SwingUtilities.invokeLater(new CGDemoRunnable(getDemo(), this) {

                public void run() {
                    CGSource src = (CGSource) obj;
                    String code = src.getSourceCode();
                    src.sourcePane.setText(code);
                    src.sourcePane.setCaretPosition(0);
                    src.rendered = true;
                }
            });
        }
    }

    public void dispose() {
        sourcePane.setText("");
        sourceCode = null;
        rendered = false;
    }
}
