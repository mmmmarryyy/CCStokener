package fr.inserm.u794.lindenb.filemanager;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import org.lindenb.io.IOUtils;

/**
 * @author lindenb
 *
 */
public class SpreadSheetFrame extends AbstractIFrame {

    private BigTableModel tableModel;

    private JTable table;

    /**
	 * @param owner
	 * @param title
	 */
    public SpreadSheetFrame(FileManager owner, File file, Delimiter delim) {
        super(owner, file.getPath());
        JPanel pane = new JPanel(new BorderLayout());
        super.contentPane.add(pane);
        this.tableModel = new BigTableModel(file, delim);
        this.table = new JTable(tableModel);
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.tableModel.setTable(this.table);
        pane.add(new JScrollPane(this.table));
        addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                tableModel.close();
            }
        });
        JMenu menu = new JMenu("Tools");
        getJMenuBar().add(menu);
        menu.add(new AbstractAction("NCBI") {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Pattern delim = Pattern.compile("[ ]");
                    BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("/home/lindenb/jeter.txt.gz"))));
                    String line = null;
                    URL url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi");
                    URLConnection conn = url.openConnection();
                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write("db=snp&retmode=xml");
                    while ((line = r.readLine()) != null) {
                        String tokens[] = delim.split(line, 2);
                        if (!tokens[0].startsWith("rs")) continue;
                        wr.write("&id=" + tokens[0].substring(2).trim());
                    }
                    wr.flush();
                    r.close();
                    InputStream in = conn.getInputStream();
                    IOUtils.copyTo(in, System.err);
                    in.close();
                    wr.close();
                } catch (IOException err) {
                    err.printStackTrace();
                }
            }
        });
    }
}
