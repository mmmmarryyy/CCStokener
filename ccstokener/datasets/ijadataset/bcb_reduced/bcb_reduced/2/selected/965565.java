package org.gtdfree.gui;

import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.Messages;
import org.gtdfree.model.GTDDataXMLTools;
import org.gtdfree.model.GTDModel;

/**
 * @author ikesan
 *
 */
public class ImportExampleDialog {

    class ImportThread extends Thread {

        private boolean server;

        private ProgressMonitor monitor;

        private GTDModel model;

        private Frame owner;

        public ImportThread(boolean server, Frame owner) {
            this.server = server;
            this.owner = owner;
        }

        @Override
        public void run() {
            try {
                monitor = new ProgressMonitor(owner, Messages.getString("ImportExampleDialog.Imp"), "", 0, 3);
                monitor.setMillisToDecideToPopup(0);
                monitor.setMillisToPopup(0);
                importExample(server);
                if (!monitor.isCanceled()) {
                    monitor.close();
                    engine.getGlobalProperties().putProperty("examplesImported", true);
                    JOptionPane.showMessageDialog(owner, Messages.getString("ImportExampleDialog.Imp.OK"), Messages.getString("ImportExampleDialog.Import"), JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e) {
                org.apache.log4j.Logger.getLogger(this.getClass()).error("Import error.", e);
                monitor.close();
                JOptionPane.showMessageDialog(owner, Messages.getString("ImportExampleDialog.Imp.Fail") + " " + e.getMessage(), Messages.getString("ImportExampleDialog.Imp.Fail.title"), JOptionPane.ERROR_MESSAGE);
            }
        }

        private void importExample(boolean server) throws IOException, XMLStreamException, FactoryConfigurationError {
            InputStream example = null;
            if (server) {
                monitor.setNote(Messages.getString("ImportExampleDialog.Cont"));
                monitor.setProgress(0);
                String page = engine.getConfiguration().getProperty("example.url");
                URL url = new URL(page);
                BufferedReader rr = new BufferedReader(new InputStreamReader(url.openStream()));
                try {
                    sleep(3000);
                } catch (InterruptedException e1) {
                    Logger.getLogger(this.getClass()).debug("Internal error.", e1);
                }
                if (monitor.isCanceled()) {
                    return;
                }
                try {
                    while (rr.ready()) {
                        if (monitor.isCanceled()) {
                            return;
                        }
                        String l = rr.readLine();
                        if (example == null) {
                            int i = l.indexOf("id=\"example\"");
                            if (i > 0) {
                                l = l.substring(i + 19);
                                l = l.substring(0, l.indexOf('"'));
                                url = new URL(l);
                                example = url.openStream();
                            }
                        }
                    }
                } catch (IOException ex) {
                    throw ex;
                } finally {
                    if (rr != null) {
                        try {
                            rr.close();
                        } catch (Exception e) {
                            Logger.getLogger(this.getClass()).debug("Internal error.", e);
                        }
                    }
                }
            } else {
                InputStream is = ApplicationHelper.class.getClassLoader().getResourceAsStream("gtd-free-example.xml");
                if (is != null) {
                    example = is;
                }
            }
            if (example != null) {
                if (monitor.isCanceled()) {
                    try {
                        example.close();
                    } catch (IOException e) {
                        Logger.getLogger(this.getClass()).debug("Internal error.", e);
                    }
                    return;
                }
                monitor.setNote(Messages.getString("ImportExampleDialog.Read"));
                monitor.setProgress(1);
                model = new GTDModel(null);
                GTDDataXMLTools.importFile(model, example);
                try {
                    example.close();
                } catch (IOException e) {
                    Logger.getLogger(this.getClass()).debug("Internal error.", e);
                }
                if (monitor.isCanceled()) {
                    return;
                }
                monitor.setNote(Messages.getString("ImportExampleDialog.Imp.File"));
                monitor.setProgress(2);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            if (monitor.isCanceled()) {
                                return;
                            }
                            engine.getGTDModel().importData(model);
                        }
                    });
                } catch (InterruptedException e1) {
                    Logger.getLogger(this.getClass()).debug("Internal error.", e1);
                } catch (InvocationTargetException e1) {
                    Logger.getLogger(this.getClass()).debug("Internal error.", e1);
                }
            } else {
                throw new IOException("Failed to obtain remote example file.");
            }
        }
    }

    public static void main(String[] args) {
        ImportExampleDialog id = new ImportExampleDialog();
        id.getDialog(null).setVisible(true);
    }

    private JDialog dialog;

    private JRadioButton serverRadio;

    private JRadioButton localRadio;

    private GTDFreeEngine engine;

    public JDialog getDialog(final Frame owner) {
        if (dialog == null) {
            dialog = new JDialog(owner, true);
            dialog.setTitle(Messages.getString("ImportExampleDialog.Imp.title"));
            JPanel p = new JPanel();
            p.setLayout(new GridBagLayout());
            int row = 0;
            JLabel l = new JLabel(Messages.getString("ImportExampleDialog.Imp.desc"));
            p.add(l, new GridBagConstraints(0, row++, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 11, 11), 0, 0));
            ButtonGroup bg = new ButtonGroup();
            serverRadio = new JRadioButton();
            serverRadio.setText(Messages.getString("ImportExampleDialog.Imp.Ser"));
            bg.add(serverRadio);
            p.add(serverRadio, new GridBagConstraints(0, row++, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 11, 0, 11), 0, 0));
            l = new JLabel(Messages.getString("ImportExampleDialog.Imp.Ser.desc"));
            l.setFont(l.getFont().deriveFont(Font.ITALIC));
            p.add(l, new GridBagConstraints(0, row++, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 33, 4, 33), 0, 0));
            localRadio = new JRadioButton();
            localRadio.setText(Messages.getString("ImportExampleDialog.Imp.Loc"));
            bg.add(localRadio);
            p.add(localRadio, new GridBagConstraints(0, row++, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 11, 0, 11), 0, 0));
            l = new JLabel(Messages.getString("ImportExampleDialog.Imp.Loc..desc"));
            l.setFont(l.getFont().deriveFont(Font.ITALIC));
            p.add(l, new GridBagConstraints(0, row++, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 33, 4, 33), 0, 0));
            serverRadio.setSelected(true);
            JButton b = new JButton();
            b.setText(Messages.getString("ImportExampleDialog.Import"));
            b.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                    ImportThread im = new ImportThread(serverRadio.isSelected(), owner);
                    im.start();
                }
            });
            p.add(b, new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 11, 11, 4), 0, 0));
            b = new JButton();
            b.setText(Messages.getString("ImportExampleDialog.Cancel"));
            b.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            p.add(b, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(11, 4, 11, 11), 0, 0));
            dialog.setContentPane(p);
            dialog.pack();
            dialog.setResizable(false);
            dialog.setLocationRelativeTo(owner);
        }
        return dialog;
    }

    public void setEngine(GTDFreeEngine e) {
        engine = e;
    }
}
