package org.ensembl.draw;

import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import org.apache.xerces.dom.*;
import org.apache.xerces.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class UCSCLookup extends JFrame implements ActionListener {

    SelectionPanel selPanel;

    JFrame owner;

    JTextArea commentText;

    public UCSCLookup(JFrame owner, SelectionPanel selPanel) {
        this.owner = owner;
        this.selPanel = selPanel;
        setSize(800, 500);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getScreenDevices()[0].getConfigurations()[0];
        Rectangle bounds = gc.getBounds();
        setLocation(bounds.x + (bounds.width - getSize().width) / 2, bounds.y + (bounds.width - getSize().height) / 4);
    }

    void moveViewer(String refStr, int start, int stop) {
        String selString;
        for (int s = 0; s < selPanel.chrField.getModel().getSize(); s++) {
            if (refStr.equals(selPanel.chrField.getModel().getElementAt(s))) {
                selPanel.chrField.setSelectedIndex(s);
                break;
            }
        }
        selPanel.setStart(start);
        selPanel.setStop(stop);
        selPanel.refreshButton.doClick();
    }

    public void actionPerformed(ActionEvent ae) {
    }

    String chooseHGVersion(String version) {
        String line = "";
        try {
            URL connectURL = new URL("http://genome.ucsc.edu/cgi-bin/hgGateway?db=" + version);
            InputStream urlStream = connectURL.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream));
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("hgsid") != -1) {
                    line = line.substring(line.indexOf("hgsid"));
                    line = line.substring(line.indexOf("VALUE=\"") + 7);
                    line = line.substring(0, line.indexOf("\""));
                    return line;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return line;
    }

    public void doQuery(String keyword) {
        Vector citations = null;
        commentText = new JTextArea(10, 80);
        keyword = keyword.replace(' ', '+');
        getContentPane().removeAll();
        try {
            String hgsid = chooseHGVersion(selPanel.dsn);
            URL connectURL = new URL("http://genome.ucsc.edu/cgi-bin/hgTracks?hgsid=" + hgsid + "&position=" + keyword);
            generateHitList(connectURL, keyword);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void presentMultipleAlignments(String keyword, String headingsLine, BufferedReader reader) {
        try {
            final Hashtable linkTable = new Hashtable();
            String line;
            JTextField txtField = new JTextField();
            Box bx = new Box(BoxLayout.Y_AXIS);
            String txt = "";
            System.out.println("Headings = " + headingsLine);
            String headings = headingsLine.substring(headingsLine.indexOf("<PRE>") + 5);
            txt = txt + headings + "\n";
            JLabel headingslabel = new JLabel("  " + headings);
            Font currentFont = headingslabel.getFont();
            headingslabel.setFont(new Font("Monospaced", Font.PLAIN, currentFont.getSize()));
            JLabel instructionLabel = new JLabel("This aligns in multiple positions.");
            instructionLabel.setFont(new Font("Monospaced", Font.PLAIN, currentFont.getSize()));
            bx.add(headingslabel);
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("HREF=") != -1) {
                    line = line.substring(line.indexOf("position=") + "position=".length());
                    String location = line.substring(0, line.indexOf("\""));
                    line = line.substring(line.indexOf(">") + 1);
                    String alignDescript = line.substring(0, line.indexOf("<"));
                    txt = txt + alignDescript + "\n";
                    JButton alignBttn = new JButton(alignDescript);
                    alignBttn.setFont(new Font("Monospaced", Font.PLAIN, currentFont.getSize()));
                    bx.add(alignBttn);
                    linkTable.put(alignBttn, location);
                    alignBttn.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent ae) {
                            String location = (String) linkTable.get(ae.getSource());
                            String refStr, startStr, stopStr;
                            System.out.println("location = " + location);
                            location = location.substring(location.indexOf("chr"));
                            refStr = location.substring(0, location.indexOf(":"));
                            System.out.println("refstr = " + refStr);
                            location = location.substring(location.indexOf(":") + 1);
                            startStr = location.substring(0, location.indexOf("-"));
                            location = location.substring(location.indexOf("-") + 1);
                            stopStr = location.substring(0, location.indexOf("&"));
                            moveViewer(refStr, Integer.parseInt(startStr), Integer.parseInt(stopStr));
                        }
                    });
                }
            }
            JFrame frame = new JFrame();
            frame.setTitle("Multiple Alignments for " + keyword);
            txtField.setText(txt);
            frame.getContentPane().add(bx);
            frame.setSize(600, 300);
            frame.setLocation((int) (owner.getLocation().getX()), (int) (owner.getLocation().getY() + owner.getSize().getHeight()));
            frame.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void readUCSCLocation(String keyword, BufferedReader reader) {
        try {
            System.out.println("Processing a form ...");
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("This aligns in multiple positions.") != -1) {
                    presentMultipleAlignments(keyword, line, reader);
                    return;
                }
                if (line.indexOf("doesn't align anywhere") != -1) {
                    JOptionPane.showMessageDialog(this, keyword + " doesn't align anywhere in the draft genome.");
                    return;
                }
                if (line.indexOf("NAME=\"position\"") != -1) {
                    line = line.substring(line.indexOf("NAME=\"position\""));
                    line = line.substring(line.indexOf("VALUE=\"") + 7);
                    String ref = line.substring(0, line.indexOf(":"));
                    line = line.substring(line.indexOf(":") + 1);
                    String start = line.substring(0, line.indexOf("-"));
                    line = line.substring(line.indexOf("-") + 1);
                    String stop = line.substring(0, line.indexOf("\""));
                    moveViewer(ref, Integer.parseInt(start), Integer.parseInt(stop));
                    break;
                }
            }
            System.out.println("Done processing a form");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void generateHitList(URL connectURL, final String keyword) {
        final int page = 0;
        int matchingDocCount = 0;
        JTabbedPane tabs = new JTabbedPane();
        GridBagLayout gbLayout = null;
        Vector linkVector = new Vector();
        final Vector commentVector = new Vector();
        JScrollPane scrollPane = null;
        JPanel hitPanel = null;
        String line = null, link, content, label, head = null;
        int hitCount = 0;
        try {
            InputStream urlStream = connectURL.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream));
            GridBagConstraints cons = new GridBagConstraints();
            cons.anchor = GridBagConstraints.WEST;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("<FORM") != -1) {
                    readUCSCLocation(keyword, reader);
                    return;
                }
                if (line.indexOf("<H2>") != -1 || (hitCount == 500)) {
                    if (hitPanel != null) {
                        cons.gridx = 0;
                        cons.weighty = 100;
                        cons.gridy = hitCount;
                        cons.fill = GridBagConstraints.HORIZONTAL;
                        JLabel bttn = new JLabel("");
                        gbLayout.setConstraints(bttn, cons);
                        hitPanel.add(bttn);
                        tabs.add(head, new JScrollPane(hitPanel));
                    }
                    hitPanel = new JPanel();
                    gbLayout = new GridBagLayout();
                    hitPanel.setLayout(gbLayout);
                    hitPanel.setAlignmentY(0.0f);
                    if (line.indexOf("<H2>") != -1) {
                        head = line.substring(line.indexOf("H2>") + 3);
                        head = head.substring(0, head.indexOf("</H2>"));
                    } else {
                        head = "more";
                    }
                    hitCount = 0;
                }
                if (line.indexOf("HREF=") != -1) {
                    link = line.substring(line.indexOf("cgi-bin/") + 8, line.indexOf("\">"));
                    line = line.substring(line.indexOf("\">") + 2);
                    String linklabel = line.substring(0, line.indexOf("</A"));
                    content = line.substring(line.indexOf("</A>") + 4);
                    content = line.substring(line.indexOf("- ") + 2);
                    commentVector.add(content);
                    linkVector.add(linklabel);
                    hitCount++;
                }
            }
            if (hitPanel != null && hitCount != 0) {
                cons.gridx = 0;
                cons.weighty = 100;
                cons.weightx = 0;
                cons.gridy = hitCount;
                cons.fill = GridBagConstraints.HORIZONTAL;
                JLabel bttn = new JLabel("");
                gbLayout.setConstraints(bttn, cons);
                hitPanel.add(bttn);
                tabs.add(head, new JScrollPane(hitPanel));
            }
        } catch (Exception e) {
            System.out.println(">" + line + "<");
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No Features Found for " + keyword);
            return;
        }
        final JList lst = new JList(linkVector);
        lst.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                int ind = lst.getSelectedIndex();
                commentText.setText((String) commentVector.elementAt(ind));
                commentText.select(0, 0);
            }
        });
        MouseListener mouseListener = new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = lst.locationToIndex(e.getPoint());
                    try {
                        String location = (String) lst.getModel().getElementAt(index), refStr, startStr, stopStr;
                        if (location.indexOf("at chr") != -1) {
                            location = location.substring(location.indexOf("at ") + 3);
                            refStr = location.substring(0, location.indexOf(":"));
                            location = location.substring(location.indexOf(":") + 1);
                            startStr = location.substring(0, location.indexOf("-"));
                            stopStr = location.substring(location.indexOf("-") + 1);
                            moveViewer(refStr, Integer.parseInt(startStr), Integer.parseInt(stopStr));
                        } else {
                            String hgsid = chooseHGVersion(selPanel.dsn);
                            URL connectURL = new URL("http://genome.ucsc.edu/cgi-bin/hgTracks?hgsid=" + hgsid + "&position=" + location);
                            InputStream urlStream = connectURL.openStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream));
                            readUCSCLocation(location, reader);
                        }
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            }
        };
        lst.addMouseListener(mouseListener);
        lst.setSelectedIndex(0);
        setTitle("Results for " + keyword);
        getContentPane().add(lst);
        scrollPane = new JScrollPane(commentText);
        JPanel pagePanel = new JPanel();
        final JButton prevBttn = new JButton("<=");
        final JButton nextBttn = new JButton("=>");
        prevBttn.setEnabled(page > 1);
        nextBttn.setEnabled(page + hitCount < matchingDocCount);
        ActionListener pageHandler = new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                if (ae.getSource() == prevBttn) {
                    System.out.println("prev!!!!");
                    doQuery(keyword);
                } else {
                    System.out.println("next!!!!");
                    doQuery(keyword);
                }
            }
        };
        pagePanel.add(prevBttn);
        prevBttn.addActionListener(pageHandler);
        pagePanel.add(nextBttn);
        nextBttn.addActionListener(pageHandler);
        JPanel hitsAndTextPanel = new JPanel();
        hitsAndTextPanel.setLayout(new GridLayout(2, 1));
        hitsAndTextPanel.add(new JScrollPane(lst));
        hitsAndTextPanel.add(scrollPane);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(hitsAndTextPanel, BorderLayout.CENTER);
        getContentPane().add(pagePanel, BorderLayout.SOUTH);
        setTitle("Results for " + keyword);
        setLocation((int) (owner.getLocation().getX()), (int) (owner.getLocation().getY() + owner.getSize().getHeight()));
        show();
    }
}
