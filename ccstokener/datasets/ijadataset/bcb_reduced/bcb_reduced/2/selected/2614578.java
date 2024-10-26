package org.virbo.autoplot;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author  jbf
 */
public class BookmarksManager extends javax.swing.JDialog {

    DefaultListModel model;

    /** Creates new form BookmarksManager */
    public BookmarksManager(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setList(Arrays.asList(new String[] { "one", "two", "three" }));
        jList.addMouseListener(mouseListener);
    }

    private MouseListener mouseListener = new MouseListener() {

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
                Point p = SwingUtilities.convertPoint(jList, e.getPoint(), BookmarksManager.this);
                itemActionsMenu.show(jList, jList.getX() + e.getX(), jList.getY() + e.getY());
            }
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    };

    public void setList(List list) {
        model = new DefaultListModel();
        for (int i = 0; i < list.size(); i++) {
            model.addElement(list.get(i));
        }
        jList.setModel(model);
    }

    public List getList() {
        return Arrays.asList(model.toArray());
    }

    void doImportUrl() {
        String ansr = null;
        URL url = null;
        boolean okay = false;
        while (okay == false) {
            String s;
            if (ansr == null) {
                s = JOptionPane.showInputDialog(this, "Enter the URL of a bookmarks file:", "");
            } else {
                s = JOptionPane.showInputDialog(this, "Whoops, Enter the URL of a bookmarks file:", ansr);
            }
            if (s == null) {
                return;
            } else {
                try {
                    url = new URL(s);
                    okay = true;
                } catch (MalformedURLException ex) {
                }
            }
        }
        try {
            Document doc = AutoplotUtil.readDoc(url.openStream());
            List<Bookmark> book = Bookmark.parseBookmarks(doc);
            this.setList(book);
        } catch (SAXException ex) {
            Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initComponents() {
        itemActionsMenu = new javax.swing.JPopupMenu();
        deleteMenuItem = new javax.swing.JMenuItem();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList = new javax.swing.JList();
        jLabel1 = new javax.swing.JLabel();
        dismissButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        titleTextField = new javax.swing.JTextField();
        URLTextField = new javax.swing.JTextField();
        importButton = new javax.swing.JButton();
        importFromWebButton = new javax.swing.JButton();
        ExportButton = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        importMenuItem = new javax.swing.JMenuItem();
        importUrlMenuItem = new javax.swing.JMenuItem();
        resetToDefaultMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();
        closeMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem.setText("delete");
        deleteMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMenuItemActionPerformed(evt);
            }
        });
        itemActionsMenu.add(deleteMenuItem);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        jList.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        jList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {

            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jList);
        jLabel1.setText("Bookmarks Manager");
        dismissButton.setText("OK");
        dismissButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dismissButtonActionPerformed(evt);
            }
        });
        jLabel2.setText("Title:");
        jLabel3.setText("URL:");
        titleTextField.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                titleTextFieldFocusLost(evt);
            }
        });
        URLTextField.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                URLTextFieldFocusLost(evt);
            }
        });
        importButton.setText("Import...");
        importButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });
        importFromWebButton.setText("Import From Web...");
        importFromWebButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importFromWebButtonActionPerformed(evt);
            }
        });
        ExportButton.setText("Export...");
        ExportButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportButtonActionPerformed(evt);
            }
        });
        jMenu1.setText("File");
        importMenuItem.setText("Import...");
        importMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(importMenuItem);
        importUrlMenuItem.setText("Import From Web...");
        importUrlMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importUrlMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(importUrlMenuItem);
        resetToDefaultMenuItem.setText("Reset to Default");
        resetToDefaultMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetToDefaultMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(resetToDefaultMenuItem);
        exportMenuItem.setText("Export...");
        exportMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(exportMenuItem);
        closeMenuItem.setText("Close");
        closeMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(closeMenuItem);
        jMenuBar1.add(jMenu1);
        setJMenuBar(jMenuBar1);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 390, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().add(importButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(importFromWebButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(ExportButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 30, Short.MAX_VALUE).add(dismissButton).addContainerGap()).add(layout.createSequentialGroup().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(jLabel3).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(URLTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(jLabel2).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(titleTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 336, Short.MAX_VALUE))).add(26, 26, 26)).add(layout.createSequentialGroup().add(jLabel1).addContainerGap(294, Short.MAX_VALUE)))));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(jLabel1).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 216, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel2).add(titleTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel3).add(URLTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 28, Short.MAX_VALUE).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(dismissButton).add(importButton).add(importFromWebButton).add(ExportButton)).addContainerGap()));
        pack();
    }

    private void resetToDefaultMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        String surl = System.getProperty("autoplot.default.bookmarks", "http://www.autoplot.org/data/demos.xml");
        int r = JOptionPane.showConfirmDialog(this, "Reset your bookmarks to " + surl + "?");
        if (r == JOptionPane.OK_OPTION) {
            try {
                URL url = new URL(surl);
                Document doc = AutoplotUtil.readDoc(url.openStream());
                List<Bookmark> book = Bookmark.parseBookmarks(doc);
                this.setList(book);
            } catch (SAXException ex) {
                Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void ExportButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doExport();
    }

    private void importFromWebButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doImportUrl();
    }

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doImport();
    }

    private void importUrlMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        doImportUrl();
    }

    private void closeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void exportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        doExport();
    }

    private void importMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        doImport();
    }

    private void jListValueChanged(javax.swing.event.ListSelectionEvent evt) {
        Bookmark b = (Bookmark) jList.getSelectedValue();
        if (b != null) {
            titleTextField.setText(b.getTitle());
            URLTextField.setText(b.getUrl());
        } else {
            titleTextField.setText("");
            URLTextField.setText("");
        }
    }

    private void URLTextFieldFocusLost(java.awt.event.FocusEvent evt) {
        Bookmark b = (Bookmark) jList.getSelectedValue();
        b.setUrl(URLTextField.getText());
        jList.repaint();
    }

    private void titleTextFieldFocusLost(java.awt.event.FocusEvent evt) {
        Bookmark b = (Bookmark) jList.getSelectedValue();
        b.setTitle(titleTextField.getText());
        jList.repaint();
    }

    private void deleteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        int[] remove = jList.getSelectedIndices();
        for (int i = remove.length - 1; i >= 0; i--) {
            this.model.remove(remove[i]);
        }
    }

    private void dismissButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new BookmarksManager(new javax.swing.JFrame(), true).setVisible(true);
            }
        });
    }

    private void doImport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".xml");
            }

            public String getDescription() {
                return "bookmarks files (*.xml)";
            }
        });
        int r = chooser.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            try {
                List<Bookmark> recent = Bookmark.parseBookmarks(AutoplotUtil.readDoc(new FileInputStream(chooser.getSelectedFile())));
                setList(recent);
            } catch (SAXException ex) {
                Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void doExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".xml");
            }

            public String getDescription() {
                return "bookmarks files (*.xml)";
            }
        });
        int r = chooser.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            try {
                String format = Bookmark.formatBooks((List<Bookmark>) getList());
                FileOutputStream out = new FileOutputStream(chooser.getSelectedFile());
                out.write(format.getBytes());
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private javax.swing.JButton ExportButton;

    private javax.swing.JTextField URLTextField;

    private javax.swing.JMenuItem closeMenuItem;

    private javax.swing.JMenuItem deleteMenuItem;

    private javax.swing.JButton dismissButton;

    private javax.swing.JMenuItem exportMenuItem;

    private javax.swing.JButton importButton;

    private javax.swing.JButton importFromWebButton;

    private javax.swing.JMenuItem importMenuItem;

    private javax.swing.JMenuItem importUrlMenuItem;

    private javax.swing.JPopupMenu itemActionsMenu;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JList jList;

    private javax.swing.JMenu jMenu1;

    private javax.swing.JMenuBar jMenuBar1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JMenuItem resetToDefaultMenuItem;

    private javax.swing.JTextField titleTextField;
}
