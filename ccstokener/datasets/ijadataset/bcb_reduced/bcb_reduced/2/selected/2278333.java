package net.sf.japi.swing.app;

import java.awt.Container;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import net.sf.japi.swing.action.ActionBuilder;
import net.sf.japi.swing.action.ActionBuilderFactory;
import net.sf.japi.swing.action.ActionMethod;
import net.sf.japi.swing.ToolBarLayout;
import net.sf.japi.swing.about.AboutDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Base class for applications.
 * @param <D> The document type that is managed by this application.
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 * @since 0.1
 */
public abstract class Application<D> implements CanLoad<D>, InternalFrameListener, DocumentListener<D> {

    /** The application frame. */
    @NotNull
    private final JFrame appFrame;

    /** The ActionBuilder. */
    @NotNull
    private final ActionBuilder actionBuilder;

    /** Whether or not to ask the user before quitting. */
    private final boolean askForQuit = true;

    /** The About Dialog. */
    private AboutDialog aboutDialog = null;

    /** The file chooser for loading and saving. */
    private final JFileChooser fileChooser;

    /** The MDI handler. */
    private final JDesktopPane desktop;

    /** The current document frame. */
    @Nullable
    private DocumentFrame<D> currentDocumentFrame = null;

    /** The current document. */
    @Nullable
    private Document<D> currentDocument = null;

    /** The opened documents. */
    @NotNull
    private final Collection<Document<D>> documents = new ArrayList<Document<D>>();

    /** Maps Window Actions to JMenuItems for later removal. */
    private final Map<Action, JMenuItem> windowActionMap = new HashMap<Action, JMenuItem>();

    /** Creates an Application. */
    protected Application() {
        final ActionBuilderFactory actionBuilderFactory = ActionBuilderFactory.getInstance();
        actionBuilder = actionBuilderFactory.getActionBuilder(getClass());
        actionBuilder.addParent(actionBuilderFactory.getActionBuilder("net.sf.japi.swing.app"));
        appFrame = new JFrame(actionBuilder.getString("application.title"));
        final Container cont = appFrame.getContentPane();
        cont.setLayout(new ToolBarLayout());
        appFrame.setJMenuBar(actionBuilder.createMenuBar(true, "application", this));
        fileChooser = new JFileChooser();
        desktop = new JDesktopPane();
        cont.add(desktop);
        updateActionStates();
    }

    /** Invoke this as soon as the application is ready to display. */
    protected void show() {
        if (!appFrame.isVisible()) {
            appFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            appFrame.addWindowListener(new ApplicationQuitter<D>(this));
            appFrame.pack();
            appFrame.setVisible(true);
        }
    }

    /** Returns the application frame.
     * @return The application frame.
     */
    @NotNull
    protected JFrame getAppFrame() {
        return appFrame;
    }

    /** Returns the ActionBuilder.
     * @return The Actionbuilder.
     */
    @NotNull
    protected ActionBuilder getActionBuilder() {
        return actionBuilder;
    }

    /** Action New. */
    @ActionMethod
    public void fileNew() {
        addDocument(createNew());
    }

    /** Action Open.
     * @throws Exception in case of I/O problems.
     */
    @ActionMethod
    public void fileOpen() throws Exception {
        fileChooser.setMultiSelectionEnabled(true);
        if (fileChooser.showOpenDialog(appFrame) == JFileChooser.APPROVE_OPTION) {
            final File[] files = fileChooser.getSelectedFiles();
            fileLoop: for (final File file : files) {
                final String uri = file.toURI().toString();
                for (final Document<D> doc : documents) {
                    if (uri.equals(doc.getUri())) {
                        doc.getFirstFrame().setSelected(true);
                        continue fileLoop;
                    }
                }
                addDocument(load(uri));
            }
        }
    }

    /** Adds the specified document.
     * @param doc Document to add.
     */
    public void addDocument(@NotNull final Document<D> doc) {
        documents.add(doc);
        doc.addDocumentListener(this);
        final DocumentFrame<D> docFrame = doc.createDocumentFrame();
        docFrame.addInternalFrameListener(this);
        desktop.add(docFrame);
        docFrame.pack();
        docFrame.setVisible(true);
    }

    /** Action Save.
     * @throws Exception in case of I/O problems while saving.
     */
    @ActionMethod
    public void fileSave() throws Exception {
        final Document<D> docToSave = currentDocument;
        if (docToSave != null) {
            final String uri = docToSave.getUri();
            if (uri != null) {
                save(docToSave, uri);
                docToSave.setChanged(false);
                updateActionStates();
            } else {
                fileSaveAs();
            }
        }
    }

    /** Action Save As.
     * @throws Exception in case of I/O problems while saving.
     */
    @ActionMethod
    public void fileSaveAs() throws Exception {
        saveAs(currentDocument);
        updateActionStates();
    }

    /** Saves a document with a new location.
     * @param docToSaveAs Doc to save with a new location.
     * @throws Exception in case of I/O problems while saving.
     */
    public void saveAs(final Document<D> docToSaveAs) throws Exception {
        if (docToSaveAs != null) {
            fileChooser.setMultiSelectionEnabled(false);
            final String currentUri = docToSaveAs.getUri();
            if (currentUri != null) {
                fileChooser.setSelectedFile(new File(new URI(currentUri)));
            } else {
                fileChooser.setSelectedFile(new File(actionBuilder.getString("unnamedFile.name")));
            }
            if (fileChooser.showSaveDialog(appFrame) == JFileChooser.APPROVE_OPTION) {
                final File file = fileChooser.getSelectedFile();
                final String uri = file.toURI().toString();
                save(docToSaveAs, uri);
                docToSaveAs.setChanged(false);
                docToSaveAs.setUri(uri);
                updateActionStates();
            }
        }
    }

    /** Action Save All.
     * @throws Exception in case of I/O problems during saving.
     */
    @ActionMethod
    public void fileSaveAll() throws Exception {
        for (final Document<D> docToSave : documents) {
            final String uri = docToSave.getUri();
            if (uri != null) {
                save(docToSave, uri);
                docToSave.setChanged(false);
            } else {
                saveAs(docToSave);
            }
        }
        updateActionStates();
    }

    /** Action Close. */
    @ActionMethod
    public void fileClose() {
        final Document<D> currentDocument = this.currentDocument;
        if (currentDocument != null && close(currentDocument)) {
            documents.remove(currentDocument);
        }
        updateActionStates();
    }

    /** Action New Window. */
    @ActionMethod
    public void winNew() {
        final Document<D> doc = currentDocument;
        if (doc == null) {
            return;
        }
        final DocumentFrame<D> docFrame = doc.createDocumentFrame();
        docFrame.addInternalFrameListener(this);
        desktop.add(docFrame);
        docFrame.pack();
        docFrame.setVisible(true);
        updateActionStates();
    }

    /** Action Close Window. */
    @ActionMethod
    public void winClose() {
        final DocumentFrame<D> frameToClose = currentDocumentFrame;
        if (frameToClose == null) {
            return;
        }
        close(frameToClose);
        updateActionStates();
    }

    /** Closes the specified frame.
     * The frame is not definitely closed.
     * The user might be asked and prevent the frame from being closed.
     * @param frameToClose The frame to close.
     * @return <code>true</code> if the frame was closed, otherwise <code>false</code>.
     */
    private boolean close(@NotNull final DocumentFrame<D> frameToClose) {
        final Document<D> doc = frameToClose.getDocument();
        if (doc.getFrameCount() > 1) {
            doc.removeDocumentFrame(frameToClose);
            desktop.remove(frameToClose);
            desktop.selectFrame(true);
            return true;
        } else {
            close(doc);
            documents.remove(doc);
            return false;
        }
    }

    /** Closes the specified document.
     * The document is not yet removed from the list of opened documents.
     * That must be done by the caller.
     * @param doc Document to close.
     * @return <code>true</code> if <var>doc</var> was closed, otherwise <code>false</code>.
     */
    private boolean close(@NotNull final Document<D> doc) {
        if (doc.hasChanged()) {
            final int result = actionBuilder.showConfirmDialog(desktop.getSelectedFrame(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, "documentNotSaved", doc.getTitle());
            switch(result) {
                case JOptionPane.YES_OPTION:
                    break;
                case JOptionPane.NO_OPTION:
                    break;
                case JOptionPane.CANCEL_OPTION:
                    return false;
                default:
                    assert false : "Switch that misses cases.";
            }
        } else {
            while (doc.getFrameCount() > 0) {
                final DocumentFrame<D> frameToClose = doc.getFirstFrame();
                doc.removeDocumentFrame(frameToClose);
                desktop.remove(frameToClose);
            }
            desktop.selectFrame(true);
            return true;
        }
        return false;
    }

    /** Action Close All. */
    @ActionMethod
    public void fileCloseAll() {
        final Iterator<Document<D>> iterator = documents.iterator();
        while (iterator.hasNext()) {
            final Document<D> doc = iterator.next();
            if (close(doc)) {
                iterator.remove();
            }
        }
        updateActionStates();
    }

    /** Action Quit. */
    @ActionMethod
    public void fileQuit() {
        if (!isUserPermissionForQuitRequired() || isQuitAllowedByUser()) {
            appFrame.dispose();
        }
    }

    /** Action About. */
    @ActionMethod
    public void about() {
        synchronized (this) {
            if (aboutDialog == null) {
                aboutDialog = new AboutDialog(actionBuilder);
            }
        }
        aboutDialog.showAboutDialog(appFrame);
    }

    /** Returns whether or not asking the user before quitting is requried.
     * Asking the user may be required if the user configured to be always asked, or if there are unsaved changes.
     * @return <code>true</code> if asking the user is required.
     */
    public boolean isUserPermissionForQuitRequired() {
        return askForQuit || hasUnsavedChanges();
    }

    /** Returns whether or not the user allowed quitting.
     * @return <code>true</code> if the user allowed quitting, otherwise <code>false</code>
     */
    public boolean isQuitAllowedByUser() {
        return actionBuilder.showQuestionDialog(appFrame, "reallyQuit");
    }

    /** Returns whether or not one or more documents have unsaved changes.
     * @return <code>true</code> if one or more documents have unsaved changes, <code>false</code> if all changes are saved.
     */
    public boolean hasUnsavedChanges() {
        return true;
    }

    public void internalFrameOpened(final InternalFrameEvent e) {
        final JMenu windows = (JMenu) actionBuilder.find(appFrame.getJMenuBar(), "window");
        assert windows != null;
        final DocumentFrame<D> docFrame = (DocumentFrame<D>) e.getInternalFrame();
        final Action winAction = docFrame.getWindowAction();
        windowActionMap.put(winAction, windows.add(winAction));
    }

    public void internalFrameClosing(final InternalFrameEvent e) {
        close((DocumentFrame<D>) e.getInternalFrame());
    }

    public void internalFrameClosed(final InternalFrameEvent e) {
        final JMenu windows = (JMenu) actionBuilder.find(appFrame.getJMenuBar(), "window");
        assert windows != null;
        final DocumentFrame<D> docFrame = (DocumentFrame<D>) e.getInternalFrame();
        final Action winAction = docFrame.getWindowAction();
        windows.remove(windowActionMap.get(winAction));
    }

    public void internalFrameIconified(final InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(final InternalFrameEvent e) {
    }

    public void internalFrameActivated(final InternalFrameEvent e) {
        setActiveDocumentImpl((DocumentFrame<D>) e.getInternalFrame());
    }

    public void internalFrameDeactivated(final InternalFrameEvent e) {
    }

    /** Updates the states of the actions.
     * @param docFrame Update the application to match this document frame and its document.
     */
    private void setActiveDocumentImpl(@Nullable final DocumentFrame<D> docFrame) {
        currentDocumentFrame = docFrame;
        currentDocument = docFrame != null ? docFrame.getDocument() : null;
        updateActionStates();
    }

    /** Updates the states of all actions. */
    private synchronized void updateActionStates() {
        final Document<D> currentDocument = this.currentDocument;
        final boolean hasCurrentDocumentFrame = currentDocumentFrame != null;
        final boolean hasCurrentDocument = currentDocument != null;
        final boolean hasDocumentsWithUnsavedChanges = hasDocumentsWithUnsavedChanges();
        actionBuilder.setActionEnabled("fileSave", hasCurrentDocument && currentDocument.hasChanged());
        actionBuilder.setActionEnabled("fileSaveAs", hasCurrentDocument);
        actionBuilder.setActionEnabled("fileSaveAll", hasDocumentsWithUnsavedChanges);
        actionBuilder.setActionEnabled("fileClose", hasCurrentDocument);
        actionBuilder.setActionEnabled("fileCloseAll", documents.size() > 0);
        actionBuilder.setActionEnabled("winNew", hasCurrentDocument);
        actionBuilder.setActionEnabled("winClose", hasCurrentDocumentFrame);
    }

    private boolean hasDocumentsWithUnsavedChanges() {
        for (final Document<D> doc : documents) {
            if (doc.hasChanged()) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @NotNull
    public abstract Document<D> load(@NotNull final String uri) throws Exception;

    /** Saves the specified document at the specified uri.
     * @param doc Document to save.
     * @param uri Uri at which to save the document.
     * @throws Exception in case of problems while saving.
     */
    public abstract void save(@NotNull final Document<D> doc, @NotNull final String uri) throws Exception;

    /** Creates a new empty document.
     * @return A new empty document.
     */
    public abstract Document<D> createNew();

    /** Opens the specified uri for writing.
     * @param uri URI to open
     * @return OutputStream for writing to that uri.
     * @throws IOException in case the uri couldn't be opened for writing.
     * @throws URISyntaxException in case the syntax of the uri was not correct.
     */
    public static OutputStream openUriForwriting(@NotNull final String uri) throws IOException, URISyntaxException {
        final URI theUri = new URI(uri);
        final String scheme = theUri.getScheme();
        if (scheme == null || "file".equals(scheme)) {
            return new FileOutputStream(new File(theUri));
        }
        final URL url = theUri.toURL();
        final URLConnection con = url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        return con.getOutputStream();
    }

    /** Opens the specified uri for reading.
     * @param uri URI to open
     * @return InputStream for reading from that uri.
     * @throws IOException in case the uri couldn't be opened for reading.
     * @throws URISyntaxException in case the syntax of the uri was not correct.
     */
    public static InputStream openUriForReading(@NotNull final String uri) throws IOException, URISyntaxException {
        final URI theUri = new URI(uri);
        final URL url = theUri.toURL();
        return url.openStream();
    }

    public void documentUriChanged(@NotNull final DocumentEvent<D> e) {
    }

    public void documentTitleChanged(@NotNull final DocumentEvent<D> e) {
    }

    public void documentContentChanged(@NotNull final DocumentEvent<D> e) {
        if (e.getSource() == currentDocument) {
            updateActionStates();
        }
    }
}
