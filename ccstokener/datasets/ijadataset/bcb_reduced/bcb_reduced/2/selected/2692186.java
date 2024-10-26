package org.blueoxygen.cimande.rest.client;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.util.EntityUtils;

public class MainFrame extends JFrame {

    private JPanel contentPane;

    private JLabel lblUrl;

    private JLabel lblMethod;

    private JLabel lblHeaders;

    private JTextField txtUrl;

    private JComboBox cmbMethod;

    private JTextField txtHeaders;

    private JScrollPane scrollPane;

    private JTable table;

    private JPanel panelRequest;

    private JPanel panelResponse;

    private JButton btnSend;

    private JPopupMenu popupMenu;

    private JMenuItem mntmAddField;

    private JMenuItem mntmRemoveField;

    private DefaultTableModel tableModel = new DefaultTableModel();

    private JScrollPane scrollPane_1;

    private JScrollPane scrollPane_2;

    private JTextArea txtrStatus;

    private JTextArea txtrResponse;

    /**
	 * Launch the application.
	 */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    MainFrame frame = new MainFrame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
	 * Create the frame.
	 */
    public MainFrame() {
        setResizable(false);
        initComponents();
        tableModel = (DefaultTableModel) table.getModel();
        getRootPane().setDefaultButton(btnSend);
    }

    private void initComponents() {
        setTitle("Simple Rest Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 900, 600);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("", "[461px,grow]", "[265.00px][grow]"));
        {
            panelRequest = new JPanel();
            panelRequest.setBorder(new TitledBorder(null, "Request", TitledBorder.LEADING, TitledBorder.TOP, null, null));
            contentPane.add(panelRequest, "cell 0 0,grow");
            panelRequest.setLayout(new MigLayout("", "[100.00][519.00,grow][113.00]", "[][][][119.00][37.00,grow]"));
            {
                lblUrl = new JLabel("URL");
                panelRequest.add(lblUrl, "cell 0 0,growx");
            }
            {
                txtUrl = new JTextField();
                panelRequest.add(txtUrl, "cell 1 0 2 1,growx");
                txtUrl.setColumns(10);
            }
            {
                lblMethod = new JLabel("Method");
                panelRequest.add(lblMethod, "cell 0 1,growx");
            }
            {
                cmbMethod = new JComboBox();
                cmbMethod.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        cmbMethodActionPerformed(e);
                    }
                });
                panelRequest.add(cmbMethod, "cell 1 1 2 1,growx");
                cmbMethod.setModel(new DefaultComboBoxModel(new String[] { "GET", "POST", "PUT", "DELETE", "HEAD", "OPTION" }));
            }
            {
                lblHeaders = new JLabel("Headers");
                panelRequest.add(lblHeaders, "cell 0 2,growx");
            }
            {
                txtHeaders = new JTextField();
                panelRequest.add(txtHeaders, "cell 1 2 2 1,growx");
                txtHeaders.setColumns(10);
            }
            {
                scrollPane = new JScrollPane();
                panelRequest.add(scrollPane, "cell 0 3 3 1,grow");
                {
                    table = new JTable();
                    table.setEnabled(false);
                    table.setModel(new DefaultTableModel(new Object[][] {}, new String[] { "FIELD", "VALUE" }));
                    scrollPane.setViewportView(table);
                }
                {
                    popupMenu = new JPopupMenu();
                    popupMenu.addPopupMenuListener(new PopupMenuListener() {

                        public void popupMenuCanceled(PopupMenuEvent e) {
                        }

                        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                        }

                        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                            popupMenuPopupMenuWillBecomeVisible(e);
                        }
                    });
                    addPopup(scrollPane, popupMenu);
                    addPopup(table, popupMenu);
                    {
                        mntmAddField = new JMenuItem("Add Field");
                        mntmAddField.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                mntmAddFieldActionPerformed(e);
                            }
                        });
                        popupMenu.add(mntmAddField);
                    }
                    {
                        mntmRemoveField = new JMenuItem("Remove Field");
                        mntmRemoveField.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                mntmRemoveFieldActionPerformed(e);
                            }
                        });
                        popupMenu.add(mntmRemoveField);
                    }
                }
            }
            {
                btnSend = new JButton("Send");
                btnSend.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        btnSendActionPerformed(e);
                    }
                });
                panelRequest.add(btnSend, "cell 2 4,grow");
            }
        }
        {
            panelResponse = new JPanel();
            panelResponse.setBorder(new TitledBorder(null, "Response", TitledBorder.LEADING, TitledBorder.TOP, null, null));
            contentPane.add(panelResponse, "cell 0 1,grow");
            panelResponse.setLayout(new MigLayout("", "[330.00][grow]", "[grow]"));
            {
                scrollPane_1 = new JScrollPane();
                panelResponse.add(scrollPane_1, "cell 0 0,grow");
                {
                    txtrStatus = new JTextArea();
                    txtrStatus.setFont(new Font("Dialog", Font.PLAIN, 12));
                    txtrStatus.setEditable(false);
                    scrollPane_1.setViewportView(txtrStatus);
                }
            }
            {
                scrollPane_2 = new JScrollPane();
                panelResponse.add(scrollPane_2, "cell 1 0,grow");
                {
                    txtrResponse = new JTextArea();
                    txtrResponse.setEditable(false);
                    txtrResponse.setFont(new Font("Courier New", Font.PLAIN, 12));
                    scrollPane_2.setViewportView(txtrResponse);
                }
            }
        }
    }

    private static void addPopup(Component component, final JPopupMenu popup) {
        component.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            private void showMenu(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    protected void mntmAddFieldActionPerformed(ActionEvent e) {
        tableModel.addRow(new Object[0]);
    }

    protected void popupMenuPopupMenuWillBecomeVisible(PopupMenuEvent e) {
        if (table.isEnabled()) {
            mntmAddField.setEnabled(true);
        } else {
            mntmAddField.setEnabled(false);
        }
        if (table.getSelectedRow() < 0) {
            mntmRemoveField.setEnabled(false);
        } else {
            mntmRemoveField.setEnabled(true);
        }
    }

    protected void mntmRemoveFieldActionPerformed(ActionEvent e) {
        tableModel.removeRow(table.getSelectedRow());
    }

    protected void btnSendActionPerformed(ActionEvent e) {
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        BasicHttpParams httpParams = new BasicHttpParams();
        for (int i = 0; i < table.getRowCount(); i++) {
            String field = (tableModel.getValueAt(i, 0) + "").trim();
            String value = (tableModel.getValueAt(i, 1) + "").trim();
            tableModel.setValueAt(field, i, 0);
            tableModel.setValueAt(value, i, 1);
            if (!field.equalsIgnoreCase("")) {
                formparams.add(new BasicNameValuePair(field, value));
                httpParams.setParameter(field, value);
            }
        }
        UrlEncodedFormEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(formparams);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        txtUrl.setText(txtUrl.getText().trim());
        String uri = txtUrl.getText() + (txtUrl.getText().contains("?") ? "&" : "?") + URLEncodedUtils.format(formparams, "UTF-8");
        HttpUriRequest uriRequest[] = { new HttpGet(uri), new HttpPost(uri), new HttpPut(uri), new HttpDelete(uri), new HttpHead(uri), new HttpOptions(uri) };
        HttpClient client = new DefaultHttpClient();
        HttpUriRequest request = uriRequest[cmbMethod.getSelectedIndex()];
        System.out.println(uri);
        if (request instanceof HttpPost) {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(entity);
            request = httpPost;
        }
        if (request instanceof HttpPut) {
            HttpPut httpPut = new HttpPut();
            httpPut.setEntity(entity);
            request = httpPut;
        }
        String[] headers = txtHeaders.getText().split(";");
        for (String header : headers) {
            String[] h = header.split(":", 2);
            if (h.length == 2) {
                request.addHeader(h[0].trim(), h[1].trim());
            }
        }
        try {
            HttpResponse response = client.execute(request);
            byte[] data = EntityUtils.toByteArray(response.getEntity());
            txtrResponse.setText(new String(data));
            txtrStatus.setText("");
            for (Header header : response.getAllHeaders()) {
                String status = txtrStatus.getText() + header.getName() + ": " + header.getValue() + "\n";
                txtrStatus.setText(status);
            }
        } catch (ClientProtocolException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void cmbMethodActionPerformed(ActionEvent e) {
        if (cmbMethod.getSelectedIndex() == 1 || cmbMethod.getSelectedIndex() == 2) {
            table.setEnabled(true);
        } else {
            table.setEnabled(false);
        }
    }
}
