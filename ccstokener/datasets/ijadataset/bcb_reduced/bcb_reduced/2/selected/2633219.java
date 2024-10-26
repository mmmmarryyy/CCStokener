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

public class EnsemblLookup extends JFrame implements ActionListener {

    public void actionPerformed(ActionEvent ae) {
        String location = ae.getActionCommand(), refStr, startStr, stopStr;
        location = location.substring(location.indexOf("at ") + 3);
        refStr = location.substring(0, location.indexOf(":"));
        location = location.substring(location.indexOf(":") + 1);
        startStr = location.substring(0, location.indexOf("-"));
        stopStr = location.substring(location.indexOf("-") + 1);
        String selString;
        for (int s = 0; s < selPanel.chrField.getModel().getSize(); s++) {
            if (refStr.equals(selPanel.chrField.getModel().getElementAt(s))) {
                selPanel.chrField.setSelectedIndex(s);
                break;
            }
        }
        selPanel.setStart(Integer.parseInt(startStr));
        selPanel.setStop(Integer.parseInt(stopStr));
        selPanel.refreshButton.doClick();
    }

    SelectionPanel selPanel;

    JFrame owner;

    public EnsemblLookup(JFrame owner, SelectionPanel selPanel) {
        this.owner = owner;
        this.selPanel = selPanel;
        setSize(700, 400);
        setLocation((int) (owner.getLocation().getX()), (int) (owner.getLocation().getY() + owner.getSize().getHeight()));
    }

    JTextArea commentText;

    JScrollPane scrollPane;

    String keyword;

    public void doQuery(String keyword) {
        doQuery(keyword, 0);
    }

    public void doQuery(String keyword, final int page) {
        this.keyword = keyword;
        keyword = keyword.replace(' ', '+');
        commentText = new JTextArea(10, 80);
        final Vector commentVector = new Vector();
        int matchingDocCount = 0;
        int hitCount = 0;
        getContentPane().removeAll();
        Vector linkVector = new Vector();
        try {
            String featureid = keyword;
            URL connectURL = new URL("http://www.ensembl.org/Homo_sapiens/textview?idx=External&q=" + keyword + "&page=" + page);
            InputStream urlStream = connectURL.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream));
            String line, link, content, label, head = null;
            Box tabBox = null;
            String linkstr;
            String comment = "";
            int EnsExtCount;
            int EnsGeneCount;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("documents match your query") != -1) {
                    matchingDocCount = Integer.parseInt(line.substring(line.indexOf("<B>") + 3, line.indexOf("</B>")));
                    continue;
                }
                if (line.indexOf("matches in the Ensembl External index") != -1) {
                }
                if (line.indexOf("matches in the Ensembl Gene index:") != -1) {
                }
                if (line.indexOf("Homo_sapiens/geneview?gene") != -1) {
                    if (line.indexOf("www.ensembl.org") != -1) {
                        line = line.substring(line.indexOf("www.ensembl.org"));
                        line = line.substring(line.indexOf("</A>") + 4);
                    }
                    int linkStart = line.indexOf("Homo_sapiens/geneview?gene");
                    if (linkStart == -1) break;
                    linkstr = "http://www.ensembl.org/" + line.substring(linkStart, line.indexOf("\">"));
                    line = line.substring(line.indexOf("</A>") + 4);
                    StringBuffer chars = new StringBuffer(line.length());
                    boolean inTag = false;
                    boolean inEntity = false;
                    boolean firstBRTossed = false;
                    line = line.substring(line.indexOf("<"));
                    for (int ch = 0; ch < line.length(); ch++) {
                        if (line.charAt(ch) == '<') {
                            inTag = true;
                            if ((line.charAt(ch + 1) == 'b' || line.charAt(ch + 1) == 'B') && (line.charAt(ch + 2) == 'r' || line.charAt(ch + 2) == 'R')) {
                                if (firstBRTossed) {
                                    chars.append("\n");
                                } else {
                                    firstBRTossed = true;
                                }
                            }
                        }
                        if (line.charAt(ch) == '&') inEntity = true;
                        if (!inTag && !inEntity) chars.append(line.charAt(ch));
                        if (line.charAt(ch) == ';') inEntity = false;
                        if (line.charAt(ch) == '>') inTag = false;
                    }
                    comment = chars.toString();
                    commentVector.add(comment);
                    linkVector.add(linkstr);
                    hitCount++;
                }
            }
            if (hitCount == 0) commentText.setText("No Matches Found for " + keyword);
            commentText.setLineWrap(true);
            commentText.setWrapStyleWord(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No Matches Found for " + keyword);
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
                    extractEnsemblCoords((String) lst.getModel().getElementAt(index));
                }
            }
        };
        lst.addMouseListener(mouseListener);
        lst.setSelectedIndex(0);
        scrollPane = new JScrollPane(commentText);
        JPanel pagePanel = new JPanel();
        final JButton prevBttn = new JButton("<=");
        final JButton nextBttn = new JButton("=>");
        prevBttn.setEnabled(page > 1);
        nextBttn.setEnabled(page + hitCount < matchingDocCount);
        ActionListener pageHandler = new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                if (ae.getSource() == prevBttn) {
                    doQuery(EnsemblLookup.this.keyword, page - 20);
                } else {
                    doQuery(EnsemblLookup.this.keyword, page + 20);
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
        setTitle("Results for " + keyword + "  Displaying " + (page + 1) + ((hitCount > 1) ? (" - " + (page + hitCount)) : "") + " of " + matchingDocCount);
        show();
    }

    void extractEnsemblCoords(String geneviewLink) {
        try {
            URL connectURL = new URL(geneviewLink);
            InputStream urlStream = connectURL.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("View gene in genomic location") != -1) {
                    line = line.substring(line.indexOf("contigview?"));
                    String chr, start, stop;
                    chr = line.substring(line.indexOf("chr=") + 4);
                    chr = chr.substring(0, chr.indexOf("&"));
                    start = line.substring(line.indexOf("vc_start=") + 9);
                    start = start.substring(0, start.indexOf("&"));
                    stop = line.substring(line.indexOf("vc_end=") + 7);
                    stop = stop.substring(0, stop.indexOf("\""));
                    String selString;
                    for (int s = 0; s < selPanel.chrField.getModel().getSize(); s++) {
                        if (chr.equals(selPanel.chrField.getModel().getElementAt(s))) {
                            selPanel.chrField.setSelectedIndex(s);
                            break;
                        }
                    }
                    selPanel.setStart(Integer.parseInt(start));
                    selPanel.setStop(Integer.parseInt(stop));
                    selPanel.refreshButton.doClick();
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Problems retrieving Geneview from Ensembl");
            e.printStackTrace();
        }
    }
}
