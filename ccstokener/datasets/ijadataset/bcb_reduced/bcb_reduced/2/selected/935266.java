package biber.plugin.filerepository;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import biber.api.*;

/**
 * <p>Implements a file repository for Biber.</p>
 *
 * <p>A file (or document) repository is a collection of electronic copies of
 * the entries in the Biber bibliography. With a file repository, one can hold
 * a pdf, postscript, or whatever file associated with a bibliography entry.</p>
 *
 * <p>Physically, the repository is implemented as a directory anywhere in the
 * file system. All documents are stored in this directory and some
 * special-purpose subdirectories. The names of these documents are quite
 * cryptic. That is needed for technical reasons and reminds the user that he
 * or she is not supposed to modify the repository by hand.</p>
 *
 * @author Lutz I�ler
 * @since 2004-06-16
 */
public class FileRepository implements Plugin, Repository {

    public static enum Source {

        FILE, URL
    }

    private Action setFileAction = null;

    private Action setURLAction = null;

    private Action showAction = null;

    private Action removeAction = null;

    private static String repositoryPath = "";

    private static String atticPath = "";

    private static String inboundPath = "";

    private static String outboundPath = "";

    private static Properties mimetypeToExt = null;

    private PluginPeer peer;

    /**
	 * File filter that accepts document files.
	 */
    protected class DocumentFilter extends javax.swing.filechooser.FileFilter {

        private Set<String> extensions = new HashSet<String>();

        public DocumentFilter() {
            extensions.add("pdf");
        }

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String ext = getFileExtension(f);
            return ext != null && extensions.contains(ext.toLowerCase());
        }

        public String getDescription() {
            return "Document files (*." + extensions.toString() + ")";
        }
    }

    /**
	 * File filter that accepts files with the given filename and any extension.
	 */
    protected class DocumentFileFilter implements java.io.FilenameFilter {

        private String filename;

        public DocumentFileFilter(String filename) {
            this.filename = filename;
        }

        public boolean accept(File dir, String name) {
            return name.matches(filename + "(\\..*)?");
        }
    }

    /**
	 * <p>Sets the path that is used for the repository.</p>
	 *
	 * @param pathname The path to use for the repository
	 */
    public void setPath(String pathname) {
        if (pathname == null) {
            pathname = System.getProperty("user.dir");
            if (pathname == null) {
                pathname = ".";
            }
        }
        if (!pathname.endsWith(File.separator)) {
            pathname = pathname.concat(File.separator);
        }
        repositoryPath = pathname;
        new File(repositoryPath).mkdirs();
        atticPath = pathname + "attic/";
        new File(atticPath).mkdirs();
        inboundPath = pathname + "inbound/";
        new File(inboundPath).mkdirs();
        outboundPath = pathname + "outbound/";
        new File(outboundPath).mkdirs();
    }

    /**
	 * <p>Returns the extension of the specified file.</p>
	 *
	 * @param f A file
	 *
	 * @return the extension of the file, or <code>null</code> if the file has
	 *         no extension.
	 */
    public static String getFileExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0) {
            if (i < s.length() - 1) {
                ext = s.substring(i + 1);
            } else {
                ext = "";
            }
        }
        return ext;
    }

    /**
	 * <p>Returns the default file extension for the specified mimetype. If the
	 * mimetype is unknown, "unknown" is returned.</p>
	 *
	 * @param mimetype A mimetype
	 *
	 * @return the default file extension for the specified mimetype, or
	 *         "unknown" if the extension is not known.
	 */
    public String getExtensionForMimetype(String mimetype) {
        if (mimetypeToExt == null) {
            mimetypeToExt = new Properties();
            try {
                URL url = peer.getResource("filerepository", "mimetypes.properties");
                if (url != null) {
                    mimetypeToExt.load(url.openStream());
                }
            } catch (java.io.IOException ex) {
                return peer.getString("filerepository", "Mimetype.Extension.unknown");
            }
        }
        return mimetypeToExt.getProperty(mimetype, peer.getString("filerepository", "Mimetype.Extension.unknown"));
    }

    /**
	 * <p>Returns the document for the specified entry.</p>
	 *
	 * @param entry An entry
	 *
	 * @return the document for the entry, or <code>null</code> if there is no
	 *         document.
	 */
    protected File findDocument(Entry entry) {
        File[] search = { new File(repositoryPath), new File(inboundPath) };
        for (int i = 0; i < search.length; i++) {
            File[] files = search[i].listFiles(new DocumentFileFilter(makeFilename(entry)));
            if (files.length > 0) {
                return files[0];
            }
        }
        return null;
    }

    /**
	 * <p>Constructs the basic filename (w/o extension) for a given entry.</p>
	 *
	 * @param entry An entry
	 *
	 * @return the filename string.
	 */
    protected String makeFilename(Entry entry) {
        return entry.getID() == null ? "" : entry.getID().replaceAll("[\\\\/\\:\\*\\?\"<>\\|]", "").trim();
    }

    /**
	 * <p>Sets the repository file for this entry. If there is already a
	 * repository file for this entry, moves the old file to the attic. If the
	 * attic already contains an old file for this entry, a uniqueness
	 * identifier is appended to the filename of the new attic file.</p>
	 *
	 * <p>In all cases, the file name and the file contents are retrieved from
	 * the specified URL. This means that HTML content or files from the
	 * internet can be handled directly by this method.</p>
	 *
	 * @param parent The component to use as parent displaying dialogs
	 * @param source The source type to retrieve the entry from
	 * @param entry An entry
	 *
	 * @return <code>true</code> if the operation was successful.
	 */
    protected boolean setDocument(Component parent, Source source, Entry entry) {
        URL url = null;
        if (source == Source.FILE) {
            JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
            chooser.setDialogTitle(peer.getString("filerepository", "FileRepository.FileChooser.caption"));
            chooser.addChoosableFileFilter(new DocumentFilter());
            int returnVal = chooser.showDialog(parent, peer.getString("filerepository", "FileRepository.FileChooser.approve"));
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (file != null) {
                    try {
                        url = file.toURL();
                    } catch (MalformedURLException ex) {
                    }
                }
            }
        } else if (source == Source.URL) {
            String urlString = JOptionPane.showInputDialog(parent, peer.getString("filerepository", "FileRepository.URLChooser.message") + ":", peer.getString("filerepository", "FileRepository.SetFromURL.caption"), JOptionPane.QUESTION_MESSAGE);
            if (urlString != null) {
                try {
                    url = new URL(urlString);
                } catch (MalformedURLException ex) {
                }
            }
        }
        if (url == null) {
            updateActions(entry);
            return false;
        }
        try {
            URLConnection con = url.openConnection();
            con.connect();
            String ext = getExtensionForMimetype(con.getContentType());
            discard(entry);
            File file = new File(inboundPath + makeFilename(entry) + "." + ext);
            java.io.OutputStream out = new java.io.FileOutputStream(file);
            byte[] buf = new byte[4096];
            InputStream in = con.getInputStream();
            int len;
            do {
                len = in.read(buf);
                if (len != -1) {
                    out.write(buf, 0, len);
                }
            } while (len != -1);
            con = null;
            System.gc();
            out.close();
            updateActions(entry);
            return true;
        } catch (IOException ex) {
            updateActions(entry);
            return false;
        }
    }

    /**
	 * <p>Removes a document from the repository. The file is moved to the
	 * outbound directory.</p>
	 *
	 * @param entry An entry
	 *
	 * @return <code>true</code> iff the removing was successful.
	 */
    protected boolean removeFromRepository(Entry entry) {
        boolean removeOk = true;
        File[] files = new File(repositoryPath).listFiles(new DocumentFileFilter(makeFilename(entry)));
        for (int j = 0; j < files.length; j++) {
            removeOk = removeOk && files[j].renameTo(new File(outboundPath + files[j].getName()));
        }
        return removeOk;
    }

    /**
	 * <p>Removes a document from the inbound. The file is deleted.</p>
	 *
	 * @param entry An entry
	 *
	 * @return <code>true</code> iff the removing was successful.
	 */
    public boolean removeFromInbound(Entry entry) {
        boolean removeOk = true;
        File[] files = new File(inboundPath).listFiles(new DocumentFileFilter(makeFilename(entry)));
        for (int j = 0; j < files.length; j++) {
            removeOk = removeOk && files[j].delete();
        }
        return removeOk;
    }

    /**
	 * <p>Updates the enabled states of the actions provided by this
	 * repository.</p>
	 *
	 * @entry The entry to update the actions for
	 */
    protected void updateActions(Entry entry) {
        boolean fileStored = findDocument(entry) != null;
        showAction.setEnabled(fileStored);
        removeAction.setEnabled(fileStored);
    }

    /**
	 * <p>Returns the actions that are supported by this repository. The first
	 * action in the returned array is the default action (and is expected to
	 * be a "show document" action).</p>
	 *
	 * <p>The actions should expect the entry they are invoked on in the
	 * {@see #ENTRY} property.</p>
	 *
	 * @return The supported actions, or <code>null</code> if this repository
	 *         does not support any actions.
	 */
    public Action[] getActions(Entry entry) {
        setFileAction.putValue(ENTRY, entry);
        setURLAction.putValue(ENTRY, entry);
        showAction.putValue(ENTRY, entry);
        removeAction.putValue(ENTRY, entry);
        updateActions(entry);
        Action[] actions = { showAction, setFileAction, setURLAction, removeAction };
        return actions;
    }

    /**
	 * <p>Removes the repository file for this entry. Actually, the file is not
	 * removed but moved to the attic. If the attic already contains an old file
	 * for this entry, a uniqueness identifier is appended to the filename of
	 * the new attic file.</p>
	 *
	 * @param parent The component to use as parent displaying dialogs
	 * @param entry An entry
	 *
	 * @return <code>true</code> if the operation was successful.
	 */
    public boolean remove(Entry entry) {
        return removeFromRepository(entry) && removeFromInbound(entry);
    }

    /**
	 * <p>Commits all file repository changes for this entry. That means the
	 * following:</p>
	 *
	 * <p><ul>
	 *   <li>Documents in the inbound are moved to the repository. If there are
	 *       any documents for this entry in the repository, these are moved to
	 *       the outbound first.</li>
	 *   <li>Documents in the outbound are moved to the attic.</li>
	 * </ul></p>
	 *
	 * @param entry An entry
	 *
	 * @return <code>true</code> iff the commit was successful.
	 */
    public boolean commit(Entry entry) {
        File[] files = new File(inboundPath).listFiles(new DocumentFileFilter(makeFilename(entry)));
        if (files.length > 0) {
            removeFromRepository(entry);
            for (int i = 0; i < files.length; i++) {
                files[i].renameTo(new File(repositoryPath + files[i].getName()));
            }
        }
        files = new File(outboundPath).listFiles(new DocumentFileFilter(makeFilename(entry)));
        for (int i = 0; i < files.length; i++) {
            String atticName = atticPath + files[i].getName();
            File atticFile = new File(atticName);
            int atticNum = 1;
            while (atticFile.exists()) {
                atticFile = new File(atticName + "~" + atticNum);
                atticNum++;
            }
            files[i].renameTo(atticFile);
        }
        return true;
    }

    /**
	 * <p>Discards all file repository changes for this entry. That means the
	 * following:</p>
	 *
	 * <p><ul>
	 *   <li>Documents in the inbound are deleted.</li>
	 *   <li>Documents in the outbound are moved back to the repository.</li>
	 * </ul></p>
	 *
	 * @param entry An entry
	 *
	 * @return <code>true</code> iff the discard operation was successful.
	 */
    public boolean discard(Entry entry) {
        File[] files = new File(inboundPath).listFiles(new DocumentFileFilter(makeFilename(entry)));
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
        files = new File(outboundPath).listFiles(new DocumentFileFilter(makeFilename(entry)));
        for (int i = 0; i < files.length; i++) {
            files[i].renameTo(new File(repositoryPath + files[i].getName()));
        }
        return true;
    }

    public Icon getIcon(Entry entry) {
        String fn = findDocument(entry) == null ? "nofile.png" : "file.png";
        URL iconURL = peer.getResource("filerepository", fn);
        if (iconURL == null) {
            return null;
        } else {
            return new ImageIcon(iconURL);
        }
    }

    public boolean available(Entry entry) {
        return findDocument(entry) != null;
    }

    public boolean initialize(final PluginPeer peer) {
        this.peer = peer;
        String path = System.getProperty("user.home");
        if (path == null) {
            path = System.getProperty("user.dir");
            if (path == null) {
                path = "./";
            }
        }
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        path += peer.getString("filerepository", "FileRepository.DisplayName") + File.separator;
        this.setPath(path);
        peer.addRepository(this);
        setFileAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                setDocument(JOptionPane.getFrameForComponent((javax.swing.JComponent) e.getSource()), Source.FILE, (Entry) getValue(Repository.ENTRY));
            }
        };
        setFileAction.putValue(Action.NAME, peer.getString("filerepository", "FileRepository.SetFromFile.caption"));
        setURLAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                setDocument(JOptionPane.getFrameForComponent((javax.swing.JComponent) e.getSource()), Source.URL, (Entry) getValue(Repository.ENTRY));
            }
        };
        setURLAction.putValue(Action.NAME, peer.getString("filerepository", "FileRepository.SetFromURL.caption"));
        showAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                File file = findDocument((Entry) getValue(Repository.ENTRY));
                if (file != null) {
                    String command = "rundll32 url.dll,FileProtocolHandler " + file.getAbsolutePath();
                    try {
                        Process p = Runtime.getRuntime().exec(command);
                    } catch (Exception ex) {
                    }
                }
            }
        };
        showAction.putValue(Action.NAME, peer.getString("filerepository", "FileRepository.Show.caption"));
        removeAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent((javax.swing.JComponent) e.getSource()), peer.getString("filerepository", "FileRepository.Remove.confirm"), peer.getString("filerepository", "FileRepository.Remove.caption"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    Entry entry = (Entry) getValue(Repository.ENTRY);
                    remove(entry);
                    updateActions(entry);
                }
            }
        };
        removeAction.putValue(Action.NAME, peer.getString("filerepository", "FileRepository.Remove.caption"));
        return true;
    }

    public Properties getProperties() {
        Properties props = new Properties();
        props.setProperty(PLUGIN_NAME, "FileRepository");
        props.setProperty(PLUGIN_DESCRIPTION, peer.getString("filerepository", "FileRepository.description"));
        return props;
    }
}
