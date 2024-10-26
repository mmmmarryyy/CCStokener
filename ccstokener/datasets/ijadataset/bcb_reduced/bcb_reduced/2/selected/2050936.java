package Crawler;

import java.util.*;
import java.net.*;
import java.io.*;
import Parser.*;
import Database.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import Analysis.*;

/**
 *
 * @author  Belay
 */
public class CrawlerUI extends javax.swing.JFrame {

    public Vector error_pages;

    public Vector error_pages_reason;

    public Vector crawled_list;

    public Vector link_list;

    public boolean stop_thread;

    private StringBuffer buffer;

    private CrawlingList links_queue;

    private HtmlParser parser;

    private Thread crawling_thread;

    /** Creates new form Crawler */
    public CrawlerUI() {
        super("Sanskrit Search Engine: Crawler");
        initComponents();
        jTabbedPane1.setTitleAt(0, "Crawling Status");
        jTabbedPane1.setTitleAt(1, "Crawling List");
        jTabbedPane1.setTitleAt(2, "Error Pages");
        jTabbedPane1.setTitleAt(3, "About");
        error_pages = new Vector();
        error_pages_reason = new Vector();
        crawled_list = new Vector();
        link_list = new Vector();
        readLinkListFiles();
    }

    public void OutPut(String text) {
        jTextArea1.append(text);
    }

    public void readLinkListFiles() {
        try {
            FileInputStream fr = new FileInputStream("c:\\CrawlingInfo\\links_to_crawl.txt");
            InputStreamReader isr = new InputStreamReader(fr, "UTF8");
            BufferedReader in = new BufferedReader(isr);
            String link;
            link_list.clear();
            while ((link = in.readLine()) != null) {
                link_list.add(link);
            }
            in.close();
            fr = new FileInputStream("c:\\CrawlingInfo\\crawled_list.txt");
            isr = new InputStreamReader(fr, "UTF8");
            in = new BufferedReader(isr);
            crawled_list.clear();
            while ((link = in.readLine()) != null) {
                crawled_list.add(link.trim());
            }
            in.close();
            fr = new FileInputStream("c:\\CrawlingInfo\\error_pages_list.txt");
            isr = new InputStreamReader(fr, "UTF8");
            in = new BufferedReader(isr);
            error_pages.clear();
            while ((link = in.readLine()) != null) {
                error_pages.add(link.trim());
            }
            in.close();
            links_queue = new CrawlingList();
            links_queue.addLink(new Vector(link_list));
            links_queue.addToCrawledList(new Vector(crawled_list));
            links_queue.addToErorPagesList(new Vector(error_pages));
            jList1.setListData(link_list);
            jList2.setListData(crawled_list);
            jList3.setListData(error_pages);
            jButton3.setEnabled(false);
        } catch (Exception e) {
            jTextArea1.append("Error occurs:\n" + e);
        }
    }

    public void restartCrawling() {
        try {
            FileInputStream fr = new FileInputStream("c:\\CrawlingInfo\\starting_links_to_crawl.txt");
            InputStreamReader isr = new InputStreamReader(fr, "UTF8");
            BufferedReader in = new BufferedReader(isr);
            String link;
            link_list.clear();
            while ((link = in.readLine()) != null) {
                link_list.add(link.trim());
            }
            in.close();
            crawled_list.clear();
            error_pages.clear();
            links_queue = new CrawlingList();
            links_queue.addLink(link_list);
            links_queue.addToCrawledList(crawled_list);
            links_queue.addToErorPagesList(error_pages);
            jList1.setListData(link_list);
            jList2.setListData(crawled_list);
            jList3.setListData(error_pages);
            jButton3.setEnabled(true);
            jButton1.setEnabled(false);
            jButton2.setEnabled(false);
            jButton4.setEnabled(false);
            crawling_thread = new Thread(new Crawler(), "Crawler");
            stop_thread = false;
            crawling_thread.start();
        } catch (Exception e) {
            jTextArea1.append("Error occurs:\n" + e);
        }
    }

    private void initComponents() {
        jDesktopPane1 = new javax.swing.JDesktopPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jButton5 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jButton6 = new javax.swing.JButton();
        jTextField2 = new javax.swing.JTextField();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jList3 = new javax.swing.JList();
        jPanel4 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);
        jLabel1.setText("Information");
        jLabel2.setText("This page displays the status of the crawler while retriving pages form a URL.");
        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(22, 22, 22).add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 572, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(jPanel1Layout.createSequentialGroup().add(44, 44, 44).add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 107, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(jPanel1Layout.createSequentialGroup().add(68, 68, 68).add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 448, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))).addContainerGap(91, Short.MAX_VALUE)));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(22, 22, 22).add(jLabel1).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 49, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(17, 17, 17).add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 273, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap(91, Short.MAX_VALUE)));
        jTabbedPane1.addTab("tab1", jPanel1);
        jList1.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        jScrollPane2.setViewportView(jList1);
        jList2.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        jScrollPane3.setViewportView(jList2);
        jLabel3.setText("List of URL's to crawl");
        jLabel4.setText("List of crawled URL\"s");
        jButton5.setText("Add URL");
        jButton6.setText("Load");
        jButton7.setText("Refresh Lists");
        jButton7.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jButton8.setText("Save");
        jButton8.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel2Layout.createSequentialGroup().add(74, 74, 74).add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel2Layout.createSequentialGroup().add(jButton7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 196, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(29, 29, 29).add(jButton8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 167, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(jLabel4).add(jLabel3).add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane3).add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 560, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup().add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jButton5).add(42, 42, 42).add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 194, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(23, 23, 23).add(jButton6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 71, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))).addContainerGap(37, Short.MAX_VALUE)));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel2Layout.createSequentialGroup().addContainerGap().add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jButton7).add(jButton8)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jLabel3).add(16, 16, 16).add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(26, 26, 26).add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jButton5).add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jButton6)).add(43, 43, 43).add(jLabel4).add(22, 22, 22).add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 114, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap(20, Short.MAX_VALUE)));
        jTabbedPane1.addTab("tab2", jPanel2);
        jList3.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        jScrollPane4.setViewportView(jList3);
        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel3Layout.createSequentialGroup().add(33, 33, 33).add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 612, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap(40, Short.MAX_VALUE)));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel3Layout.createSequentialGroup().add(38, 38, 38).add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 408, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap(28, Short.MAX_VALUE)));
        jTabbedPane1.addTab("tab3", jPanel3);
        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(0, 685, Short.MAX_VALUE));
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(0, 474, Short.MAX_VALUE));
        jTabbedPane1.addTab("tab4", jPanel4);
        jTabbedPane1.setBounds(10, 10, 690, 500);
        jDesktopPane1.add(jTabbedPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jButton1.setText("Start");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jButton1.setBounds(60, 530, 130, 30);
        jDesktopPane1.add(jButton1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jButton4.setText("Close");
        jButton4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jButton4.setBounds(510, 530, 140, 30);
        jDesktopPane1.add(jButton4, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jButton2.setText("Restart");
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jButton2.setBounds(210, 530, 130, 30);
        jDesktopPane1.add(jButton2, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jButton3.setText("Stop");
        jButton3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jButton3.setBounds(360, 530, 130, 30);
        jDesktopPane1.add(jButton3, javax.swing.JLayeredPane.DEFAULT_LAYER);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jDesktopPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 715, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jDesktopPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 577, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE));
        pack();
    }

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {
        links_queue.saveList();
    }

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {
        jList1.setListData(links_queue.getLinksTocrawl());
        jList2.setListData(links_queue.getCrawledList());
        jList3.setListData(links_queue.getErrorPages());
    }

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {
        Runtime.getRuntime().exit(0);
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        restartCrawling();
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
        stop_thread = true;
        jButton3.setEnabled(false);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        jButton3.setEnabled(true);
        jButton1.setEnabled(false);
        jButton2.setEnabled(false);
        jButton4.setEnabled(false);
        crawling_thread = new Thread(new Crawler(), "Crawler");
        stop_thread = false;
        crawling_thread.start();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new CrawlerUI().setVisible(true);
            }
        });
    }

    public class Crawler implements Runnable {

        public Crawler() {
            Properties prop = System.getProperties();
            System.setProperties(prop);
            parser = new HtmlParser();
        }

        public void run() {
            String strUrlDoc;
            String temp_charset = "UTF8";
            String size;
            jTextArea1.append("Crawler started:" + (new Date()).toString() + "\n");
            while ((strUrlDoc = links_queue.getLink()) != null && !stop_thread) {
                temp_charset = "UTF8";
                jTextArea1.append(strUrlDoc + "\n");
                try {
                    buffer = new StringBuffer();
                    URL urlToCrawl = new URL(strUrlDoc);
                    URLConnection urlToCrawlConnection = urlToCrawl.openConnection();
                    urlToCrawlConnection.setRequestProperty("User-Agent", "USER_AGENT");
                    urlToCrawlConnection.setRequestProperty("Referer", "REFERER");
                    urlToCrawlConnection.setUseCaches(false);
                    String content_type = urlToCrawlConnection.getContentType();
                    String charset = temp_charset;
                    if (content_type != null) {
                        StringTokenizer tokens = new StringTokenizer(content_type, " \'\"=");
                        while (tokens.hasMoreTokens()) {
                            if ((tokens.nextToken().toLowerCase().trim()).equals("charset")) {
                                charset = tokens.nextToken().toLowerCase().trim();
                                System.out.println("charset found");
                                break;
                            }
                        }
                    }
                    long date_modified = urlToCrawlConnection.getLastModified();
                    long date_last_crawled = 0;
                    size = String.valueOf(urlToCrawlConnection.getContentLength());
                    if (!isModified(strUrlDoc, date_modified)) {
                        int buffSize = 51200;
                        byte[] buff = new byte[buffSize];
                        charset = charset.toUpperCase();
                        InputStreamReader isr = new InputStreamReader(urlToCrawlConnection.getInputStream(), charset);
                        BufferedReader in = new BufferedReader(isr);
                        int c;
                        while ((c = in.read()) > -1) buffer.append((char) c);
                        if (charset.startsWith("UTF")) links_queue.addLink(parser.parseUTF(buffer.toString(), strUrlDoc, urlToCrawl.getHost(), date_modified, size)); else links_queue.addLink(parser.parse(buffer.toString(), strUrlDoc, urlToCrawl.getHost(), date_modified, size));
                    }
                    links_queue.addToCrawledList(strUrlDoc);
                } catch (MalformedURLException e) {
                    jTextArea1.append(e.toString() + "1\n");
                    links_queue.addToCrawledList(strUrlDoc);
                    links_queue.addToErorPagesList(strUrlDoc);
                } catch (UnknownServiceException e) {
                    jTextArea1.append(e.toString() + "2\n");
                    links_queue.addToCrawledList(strUrlDoc);
                    links_queue.addToErorPagesList(strUrlDoc);
                } catch (IOException e) {
                    jTextArea1.append(e.toString() + "3\n");
                    e.printStackTrace();
                    links_queue.addToCrawledList(strUrlDoc);
                    links_queue.addToErorPagesList(strUrlDoc);
                } catch (Exception e) {
                    jTextArea1.append(e.toString() + "4\n");
                    e.printStackTrace();
                    links_queue.addToCrawledList(strUrlDoc);
                    links_queue.addToErorPagesList(strUrlDoc);
                }
            }
            try {
                jTextArea1.append("Optimizing index...\n");
                Parser.HtmlIndexer.optimizeIndex();
                jTextArea1.append("Saving links queue...\n");
                links_queue.saveList();
                jTextArea1.append("Finished.\n");
            } catch (Exception e) {
                System.out.println("Exception here " + e.toString());
            }
            jButton3.setEnabled(false);
            jButton1.setEnabled(true);
            jButton2.setEnabled(true);
            jButton4.setEnabled(true);
        }

        public boolean isModified(String url, long date_last_modified) throws Exception {
            long date_modified = 0;
            String search_query = "url:" + url;
            IndexSearcher is = new IndexSearcher("c:\\luceneindex");
            Analyzer analyzer = new AmharicAnalyzer();
            QueryParser parser = new QueryParser("url", analyzer);
            Query query = parser.parse(search_query);
            Hits hits = is.search(query);
            if (hits.length() > 0) {
                Long l = Long.getLong(hits.doc(0).get("last_updated"));
                if (l == null) return false;
                date_modified = l.longValue();
                if (date_last_modified > date_modified) {
                    IndexReader reader = is.getIndexReader();
                    reader.deleteDocument(hits.id(0));
                    return true;
                }
            }
            is.close();
            return false;
        }
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JButton jButton3;

    private javax.swing.JButton jButton4;

    private javax.swing.JButton jButton5;

    private javax.swing.JButton jButton6;

    private javax.swing.JButton jButton7;

    private javax.swing.JButton jButton8;

    private javax.swing.JDesktopPane jDesktopPane1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JList jList1;

    private javax.swing.JList jList2;

    private javax.swing.JList jList3;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JScrollPane jScrollPane4;

    private javax.swing.JTabbedPane jTabbedPane1;

    private javax.swing.JTextArea jTextArea1;

    private javax.swing.JTextField jTextField1;

    private javax.swing.JTextField jTextField2;
}
