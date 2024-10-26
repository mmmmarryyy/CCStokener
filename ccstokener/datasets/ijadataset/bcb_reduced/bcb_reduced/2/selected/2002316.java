package edu.psu.its.lionshare.gui.library;

import edu.psu.its.lionshare.util.RunnableProcessor;
import edu.psu.its.lionshare.share.DataObjectGroup;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.net.URL;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class FileDropTargetListener implements DropTargetListener {

    static DataFlavor urlFlavor;

    static {
        try {
            urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
    }

    public void dragEnter(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
    }

    public void drop(DropTargetDropEvent dtde) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        Transferable trans = dtde.getTransferable();
        boolean gotData = false;
        try {
            if (trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String file = (String) trans.getTransferData(DataFlavor.stringFlavor);
                URL url = new URL(file);
                final DataObjectGroup group = LibraryMediator.instance().getSelectedGroup();
                if (isDroppable(url, group)) {
                    handleFileDrop(url, group);
                    gotData = true;
                } else gotData = false;
            } else if (trans.isDataFlavorSupported(urlFlavor)) {
                URL url = (URL) trans.getTransferData(urlFlavor);
                final DataObjectGroup group = LibraryMediator.instance().getSelectedGroup();
                if (isDroppable(url, group)) {
                    handleFileDrop(url, group);
                    gotData = true;
                } else gotData = false;
            } else if (trans.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                LibrarySharingMediator sharing = LibraryMediator.instance().getShareMediator();
                final DataObjectGroup group = sharing.getGroupAt(dtde.getLocation());
                List<File> files = (List<File>) trans.getTransferData(DataFlavor.javaFileListFlavor);
                if (group != null && files != null && group.isModifiable()) {
                    for (File file : files) {
                        try {
                            handleFileDrop(file.toURL(), group);
                            gotData = true;
                        } catch (Exception exp) {
                            gotData = false;
                        }
                    }
                }
            }
        } catch (Exception exp) {
        } finally {
            dtde.dropComplete(gotData);
        }
    }

    public boolean isDroppable(URL url, DataObjectGroup group) {
        if (group != null && url != null) {
            try {
                String url_string = url.getPath();
                Pattern p0 = Pattern.compile("%20");
                Matcher m0 = p0.matcher(url_string);
                url_string = m0.replaceAll(" ");
                File io_file = new File(url_string);
                if (!url.sameFile(io_file.toURL()) && group.getLocation() == null) {
                    return false;
                }
            } catch (Exception exp) {
                exp.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void handleFileDrop(final URL url, final DataObjectGroup group) {
        Runnable run = new Runnable() {

            public void run() {
                try {
                    if (group != null && url != null) {
                        String url_string = url.getPath();
                        Pattern p0 = Pattern.compile("%20");
                        Matcher m0 = p0.matcher(url_string);
                        url_string = m0.replaceAll(" ");
                        File io_file = new File(url_string);
                        if (!url.sameFile(io_file.toURL()) && group.getLocation() != null) {
                            String location = group.getLocation().toString();
                            File directory = new File(location);
                            File existing = new File(url.getFile());
                            String url_file_name = existing.getName();
                            p0 = Pattern.compile("%20");
                            m0 = p0.matcher(url_file_name);
                            url_file_name = m0.replaceAll(" ");
                            io_file = new File(directory, url_file_name);
                            if (!io_file.exists()) {
                                BufferedInputStream input = new BufferedInputStream(url.openStream());
                                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(io_file));
                                byte[] bytes = new byte[4096];
                                while (true) {
                                    int read = input.read(bytes);
                                    if (read < 0) break;
                                    out.write(bytes, 0, read);
                                }
                                input.close();
                                out.flush();
                                out.close();
                            }
                        }
                        LibraryButtonActions.addFiles(new File[] { io_file }, group);
                    }
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
            }
        };
        RunnableProcessor.getInstance().execute(run);
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }
}
