package org.proteomecommons.MSExpedite.app;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.image.BufferedImage;
import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;
import org.proteomecommons.MSExpedite.app.PageOutOfMemoryException;
import org.proteomecommons.MSExpedite.app.TreeComponent.INodeSelectedListener;
import org.proteomecommons.MSExpedite.app.TreeComponent.NodeSelectedEvent;
import org.proteomecommons.io.UnknownFileFormatException;
import org.xml.sax.SAXException;

/**
 *
 * @author takis
 */
public class DataExchange extends DragSourceAdapter implements DragGestureListener, INodeSelectedListener, DropTargetListener {

    private ISessionHandler handler;

    private Cursor cursor;

    private static DataFlavor urlFlavor;

    private static DataExchange singleton = null;

    static {
        try {
            urlFlavor = new DataFlavor("application/x-java-url;class=java.net.URL");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    static {
        System.getProperties().put("java.protocol.handler.pkgs", "HTTPClient");
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        }
    }

    public DataExchange(ISessionHandler handler) {
        singleton = this;
        setHandler(handler);
    }

    public void setHandler(ISessionHandler handler) {
        this.handler = handler;
    }

    public static DataExchange getInstance() {
        return singleton;
    }

    public void load(final URL url) throws Exception {
        InputStream is = getInputStream(url);
        String urlString = url.toString();
        String del = "//";
        int index = urlString.indexOf(del);
        String filename = urlString.substring(index + del.length());
        del = ".";
        index = filename.indexOf(del);
        filename = filename.substring(0, index);
        String ext = filename.substring(index + 1, filename.length());
        File file = File.createTempFile(filename, ext);
        OutputStream os = new FileOutputStream(file);
        int length = 0;
        byte data[] = new byte[1024];
        while ((length = is.read(data)) != -1) {
            os.write(data, 0, length);
        }
        os.flush();
        os.close();
        is.close();
        load(file, url);
    }

    public void load(final File f[], final Object owner) {
        System.gc();
        MSESystem.showProgressIndicator();
        Thread t = new Thread(new Runnable() {

            public void run() {
                String s = "";
                for (int i = 0; i < f.length; i++) {
                    try {
                        IContext context = handler.newContext();
                        IOGraph.loadData(f[i].getAbsolutePath(), owner, handler, context);
                        MSESystem.setLastDirVisited(context, f[i].getAbsolutePath());
                    } catch (CloneNotSupportedException ex) {
                        ex.printStackTrace();
                        MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Internal Error Due To Cloning", "", ex, null);
                    } catch (FileNotFoundException ex) {
                        ex.printStackTrace();
                        s += "\n" + f[i].getAbsolutePath() + " Reason = " + ex.toString();
                    } catch (IntrospectionException ex) {
                        ex.printStackTrace();
                        s += "\n" + f[i].getAbsolutePath() + " Reason = " + ex.toString();
                    } catch (SAXException ex) {
                        ex.printStackTrace();
                        s += "\n" + f[i].getAbsolutePath() + " Reason = " + ex.toString();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        s += "\n" + f[i].getAbsolutePath() + " Reason = " + ex.toString();
                    } catch (EmptyPeakListFileException ex) {
                        ex.printStackTrace();
                        s += "\n" + f[i].getAbsolutePath() + " Reason = " + ex.toString();
                    } catch (UnknownFileFormatException ex) {
                        ex.printStackTrace();
                        s += "\n" + f[i].getAbsolutePath() + " Reason = " + ex.toString();
                    } catch (PageOutOfMemoryException ex) {
                        s += "\n" + f[i].getAbsolutePath() + " Reason = " + ex.toString();
                    }
                    MSESystem.hideProgressIndicator();
                    if (s.length() != 0) {
                        MSESystem.notifyUser(JOptionPane.WARNING_MESSAGE, "MS-Expedite encountered difficulties during read operations.", s, null, null);
                    }
                }
            }
        });
        t.start();
    }

    private void load(final File f, final Object owner) {
        if (f.isFile()) {
            System.gc();
            MSESystem.showProgressIndicator();
            Thread t = new Thread(new Runnable() {

                public void run() {
                    try {
                        IOGraph.loadData(f.getAbsolutePath(), owner, handler, handler.newContext());
                        MSESystem.setLastDirVisited(owner, f.getAbsolutePath());
                    } catch (CloneNotSupportedException ex) {
                        ex.printStackTrace();
                        MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Internal Error Due To Cloning", "", ex, null);
                    } catch (FileNotFoundException ex) {
                        ex.printStackTrace();
                        MSESystem.notifyUser(JOptionPane.WARNING_MESSAGE, "Unable To Find Selected File", "", ex, null);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Unable To Complete I/O operation", "", ex, null);
                    } catch (SAXException ex) {
                        ex.printStackTrace();
                        MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Internal Error Of XML parsers", "", ex, null);
                    } catch (IntrospectionException ex) {
                        ex.printStackTrace();
                        MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Internal Error Of Betwixt", "", ex, null);
                    } catch (UnknownFileFormatException ex) {
                        ex.printStackTrace();
                        MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "This file is of unknown format", "", ex, null);
                    } catch (EmptyPeakListFileException ex) {
                        ex.printStackTrace();
                        MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, f.getAbsolutePath() + " does not seem to contain any peak lists!", "", ex, null);
                    } catch (PageOutOfMemoryException ex) {
                        ex.printStackTrace();
                        MSESystem.notifyUser(JOptionPane.WARNING_MESSAGE, "Not Enough Memory", "There is not enough memory to load this spectrum; Close some opened spectra and tried again", ex, null);
                    }
                    MSESystem.hideProgressIndicator();
                }
            });
            t.start();
        }
    }

    public void nodeSelected(NodeSelectedEvent e) {
        if (!e.isLeaf()) return;
        if (e.getObject() instanceof File) {
            final File f = (File) e.getObject();
            if (f.isFile()) {
                System.gc();
                MSESystem.showProgressIndicator();
                Thread t = new Thread(new Runnable() {

                    public void run() {
                        try {
                            IOGraph.loadData(f.getAbsolutePath(), "TreeComponent", handler, handler.newContext());
                            MSESystem.setLastDirVisited(f, f.getAbsolutePath());
                        } catch (CloneNotSupportedException ex) {
                            ex.printStackTrace();
                            MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Internal Error Due To Cloning", "", ex, null);
                        } catch (FileNotFoundException ex) {
                            ex.printStackTrace();
                            MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Unable To Find Selected File", "", ex, null);
                        } catch (SAXException ex) {
                            ex.printStackTrace();
                            MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Internal Failure of XML Parsers", "", ex, null);
                        } catch (IntrospectionException ex) {
                            ex.printStackTrace();
                            MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Internal Failure of betwixt", "", ex, null);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Unable To Proceed with I/O operation", "", ex, null);
                        } catch (EmptyPeakListFileException ex) {
                            MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, "Unable To Open Selected File", "", ex, null);
                            return;
                        } catch (UnknownFileFormatException ex) {
                            ex.printStackTrace();
                            MSESystem.notifyUser(JOptionPane.ERROR_MESSAGE, f.getAbsolutePath() + " does not seem to contain any peak lists!", "", ex, null);
                            return;
                        } catch (PageOutOfMemoryException ex) {
                            ex.printStackTrace();
                            MSESystem.notifyUser(JOptionPane.WARNING_MESSAGE, "Not Enough Memory", "There is not enough memory to load this spectrum; Close some opened spectra and tried again", ex, null);
                            return;
                        }
                        MSESystem.hideProgressIndicator();
                    }
                });
                t.start();
            } else if (f.isDirectory()) {
            }
        }
    }

    public void dragGestureRecognized(DragGestureEvent dge) {
        try {
            IContext context = handler.getContext();
            MetaData metaData = context.getMetaData();
            if (metaData.getFileName().length() == 0) return;
            File proxyDir = File.createTempFile("tmpdir", ".dir", null);
            File oldFile = new File(metaData.getFileName());
            File temp = new File(proxyDir.getParent(), oldFile.getName());
            IOGraph.save(oldFile, temp);
            FileSystemView fsv = FileSystemView.getFileSystemView();
            Icon icn = fsv.getSystemIcon(temp);
            Toolkit tk = Toolkit.getDefaultToolkit();
            Dimension dim = tk.getBestCursorSize(icn.getIconWidth(), icn.getIconHeight());
            BufferedImage buff = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
            icn.paintIcon((JComponent) context.getGraph(), buff.getGraphics(), 0, 0);
            if (DragSource.isDragImageSupported()) {
                dge.startDrag(DragSource.DefaultCopyDrop, buff, new Point(0, 0), new MSFileTransferable(temp), this);
            } else {
                cursor = tk.createCustomCursor(buff, new Point(0, 0), "billybob");
                dge.startDrag(cursor, null, new Point(0, 0), new MSFileTransferable(temp), this);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void dragEnter(DragSourceDragEvent evt) {
        DragSourceContext ctx = evt.getDragSourceContext();
        ctx.setCursor(cursor);
    }

    public void dragExit(DragSourceEvent evt) {
        DragSourceContext ctx = evt.getDragSourceContext();
        ctx.setCursor(DragSource.DefaultCopyNoDrop);
    }

    public void dragEnter(DropTargetDragEvent dtde) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void drop(DropTargetDropEvent dtde) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        Transferable trans = dtde.getTransferable();
        dumpDataFlavors(trans);
        boolean gotData = false;
        try {
            if (trans.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                java.util.List fileList = (java.util.List) trans.getTransferData(DataFlavor.javaFileListFlavor);
                File file[] = new File[fileList.size()];
                for (int i = 0; i < fileList.size(); i++) {
                    file[i] = (File) fileList.get(i);
                }
                load(file, "DragAndDrop");
                gotData = true;
            } else if (trans.isDataFlavorSupported(urlFlavor)) {
                URL url = (URL) trans.getTransferData(urlFlavor);
                load(url);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            dtde.dropComplete(gotData);
        }
    }

    private void dumpDataFlavors(Transferable trans) {
        DataFlavor df[] = trans.getTransferDataFlavors();
        for (int i = 0; i < df.length; i++) {
            System.out.println(df[i]);
        }
    }

    private static InputStream getInputStream(URL url) throws IOException {
        HostnameVerifier hv = new HostnameVerifier() {

            public boolean verify(String urlHostName, SSLSession session) {
                System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
        URLConnection con = url.openConnection();
        return con.getInputStream();
    }
}
