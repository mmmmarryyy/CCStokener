package sequime.io.read.uniprot;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.security.auth.kerberos.KerberosKey;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.knime.core.data.collection.SetCell;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import sequime.io.read.ena.ENADataHolder;
import sequime.io.read.ena.ENABrowserNodeDialog.ena_details;

public class UniprotPanel extends JPanel {

    JTextField query;

    JTable results;

    JTable IDs;

    public UniprotPanel() {
        setLayout(new BorderLayout());
        JPanel qpanel = new JPanel();
        JLabel quey_label = new JLabel("Uniprot query (e.g. human thrombin)");
        qpanel.add(quey_label);
        query = new JTextField(30);
        qpanel.add(query);
        JButton search = new JButton("Search");
        qpanel.add(search);
        add(qpanel, BorderLayout.NORTH);
        results = new JTable(new TModel());
        search.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ((TModel) results.getModel()).replaceData(retrieveData(query.getText()));
            }
        });
        query.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ((TModel) results.getModel()).replaceData(retrieveData(query.getText()));
            }
        });
        JScrollPane spane = new JScrollPane(results);
        this.add(spane, BorderLayout.CENTER);
        final IDModel M = new IDModel();
        IDs = new JTable(M);
        JScrollPane idpane = new JScrollPane(IDs);
        add(idpane, BorderLayout.WEST);
        JButton sel = new JButton("add to selection");
        sel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                int[] ri = results.getSelectedRows();
                String[] tt = new String[ri.length];
                for (int i = 0; i < ri.length; i++) {
                    tt[i] = (String) results.getModel().getValueAt(ri[i], 0);
                }
                M.add(tt);
                System.out.println("Updated IDs: \n" + Arrays.toString(tt));
            }
        });
        add(sel, BorderLayout.SOUTH);
    }

    private List<String[]> retrieveData(String query) {
        List<String[]> data = new Vector<String[]>();
        query = query.replaceAll("\\s", "+");
        String q = "http://www.uniprot.org/uniprot/?query=" + query + "&format=tab&columns=id,protein%20names,organism";
        try {
            URL url = new URL(q);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] st = line.split("\t");
                String[] d = new String[] { st[0], st[1], st[2] };
                data.add(d);
            }
            reader.close();
            if (data.size() == 0) {
                JOptionPane.showMessageDialog(this, "No data found for query");
            }
        } catch (MalformedURLException e) {
            System.err.println("Query " + q + " caused exception: ");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Query " + q + " caused exception: ");
            e.printStackTrace();
        }
        return data;
    }

    private class TModel implements TableModel {

        String[] cnames = new String[] { "Accession", "Organism", "Name" };

        List<String[]> data = new Vector<String[]>();

        List<TableModelListener> listeners = new Vector<TableModelListener>();

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data.get(rowIndex)[columnIndex];
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return cnames[columnIndex];
        }

        @Override
        public int getColumnCount() {
            return cnames.length;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        public void replaceData(List<String[]> nData) {
            data.clear();
            data.addAll(nData);
            for (TableModelListener L : listeners) L.tableChanged(new TableModelEvent(this));
        }
    }

    private class IDModel implements TableModel {

        String[] cnames = new String[] { "Accession No." };

        List<String> data = new Vector<String>();

        List<TableModelListener> listeners = new Vector<TableModelListener>();

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex < data.size()) {
                data.set(rowIndex, aValue.toString());
            } else {
                data.add(aValue.toString());
            }
            for (TableModelListener L : listeners) L.tableChanged(new TableModelEvent(this));
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return cnames[columnIndex];
        }

        @Override
        public int getColumnCount() {
            return cnames.length;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        public void add(String[] ids) {
            for (String string : ids) {
                if (!data.contains(string)) {
                    setValueAt(string, data.size(), 0);
                }
            }
        }
    }

    public String getSelectedIDs() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < IDs.getRowCount(); i++) {
            sb.append(IDs.getValueAt(i, 0).toString());
            if (i + 1 < IDs.getRowCount()) sb.append(",");
        }
        return sb.toString();
    }

    public void putIDs(String s) {
        String[] st = s.split(",");
        ((IDModel) IDs.getModel()).add(st);
    }

    public static void main(String[] args) {
    }
}
