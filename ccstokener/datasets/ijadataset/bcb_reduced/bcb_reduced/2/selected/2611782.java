package jgloss.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.JobAttributes;
import java.awt.PageAttributes;
import java.awt.PrintJob;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.ProgressMonitor;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jgloss.JGloss;
import jgloss.JGlossApp;
import jgloss.Preferences;
import jgloss.dictionary.DictionaryEntry;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;
import jgloss.ui.annotation.Annotation;
import jgloss.ui.annotation.AnnotationListModel;
import jgloss.ui.export.ExportMenu;
import jgloss.ui.html.AnnotationListSynchronizer;
import jgloss.ui.html.JGlossEditor;
import jgloss.ui.html.JGlossEditorKit;
import jgloss.ui.html.JGlossHTMLDoc;
import jgloss.ui.html.SelectedAnnotationHighlighter;
import jgloss.ui.xml.JGlossDocument;
import jgloss.ui.xml.JGlossDocumentBuilder;
import jgloss.util.CharacterEncodingDetector;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Frame which contains everything needed to edit a single JGloss document.
 *
 * @author Michael Koch
 */
public class JGlossFrame extends JPanel implements ActionListener, ListSelectionListener, HyperlinkListener, CaretListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
     * Collection of publically available actions.
     */
    public static class Actions {

        /**
         * Imports a document into an empty JGlossFrame.
         */
        public final Action importDocument;

        /**
         * Imports the clipboard content into an empty JGlossFrame.
         */
        public final Action importClipboard;

        /**
         * Menu listener which will update the state of the import clipboard
         * action when the menu is selected.
         */
        public final ImportClipboardListener importClipboardListener;

        /**
         * Opens a document created by JGloss in an empty JGlossFrame.
         */
        public final Action open;

        /**
         * Listens to open recent selections. Use with 
         * {@link OpenRecentMenu#createDocumentMenu(File,OpenRecentMenu.FileSelectedListener) 
         *  OpenRecentMenu.createDocumentMenu}.
         */
        public final OpenRecentMenu.FileSelectedListener openRecentListener;

        /**
         * Creates a new instance of the actions which will invoke the methods
         * on the specified target. If the target is <CODE>null</CODE>, a new JGlossFrame
         * will be created on each invocation.
         */
        private Actions(final JGlossFrame target) {
            importDocument = new AbstractAction() {

                /**
				 * 
				 */
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                    new Thread("JGloss import") {

                        public void run() {
                            ImportDialog d = new ImportDialog(target != null ? target.frame : null);
                            if (d.doDialog()) {
                                JGlossFrame which;
                                if (target == null || target.model.isEmpty()) which = new JGlossFrame(); else which = target;
                                if (d.selectionIsFilename()) which.importDocument(d.getSelection(), d.isDetectParagraphs(), d.createParser(Dictionaries.getDictionaries(true), ExclusionList.getExclusions()), d.createReadingAnnotationFilter(), d.getEncoding()); else which.importString(d.getSelection(), d.isDetectParagraphs(), JGloss.messages.getString("import.textarea"), JGloss.messages.getString("import.textarea"), d.createParser(Dictionaries.getDictionaries(true), ExclusionList.getExclusions()), d.createReadingAnnotationFilter(), false);
                            }
                        }
                    }.start();
                }
            };
            importDocument.setEnabled(true);
            UIUtilities.initAction(importDocument, "main.menu.import");
            importClipboard = new AbstractAction() {

                /**
				 * 
				 */
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                    JGlossApp.getJGlossFrame().doImportClipboard();
                }
            };
            importClipboard.setEnabled(false);
            UIUtilities.initAction(importClipboard, "main.menu.importclipboard");
            open = new AbstractAction() {

                /**
				 * 
				 */
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                    new Thread("JGloss open") {

                        public void run() {
                            JFileChooser f = new JFileChooser(JGloss.getCurrentDir());
                            f.addChoosableFileFilter(jglossFileFilter);
                            f.setFileHidingEnabled(true);
                            f.setFileView(CustomFileView.getFileView());
                            int r = f.showOpenDialog(target);
                            if (r == JFileChooser.APPROVE_OPTION) {
                                JGloss.setCurrentDir(f.getCurrentDirectory().getAbsolutePath());
                                String path = f.getSelectedFile().getAbsolutePath();
                                for (Iterator<?> i = jglossFrames.iterator(); i.hasNext(); ) {
                                    JGlossFrame next = (JGlossFrame) i.next();
                                    if (path.equals(next.model.getDocumentPath())) {
                                        next.frame.setVisible(true);
                                        return;
                                    }
                                }
                                JGlossFrame which = target == null || target.model.isEmpty() ? new JGlossFrame() : target;
                                which.loadDocument(f.getSelectedFile());
                            }
                        }
                    }.start();
                }
            };
            open.setEnabled(true);
            UIUtilities.initAction(open, "main.menu.open");
            openRecentListener = new OpenRecentMenu.FileSelectedListener() {

                public void fileSelected(final File file) {
                    String path = file.getAbsolutePath();
                    for (Iterator<?> i = jglossFrames.iterator(); i.hasNext(); ) {
                        JGlossFrame next = (JGlossFrame) i.next();
                        if (path.equals(next.model.getDocumentPath())) {
                            next.frame.setVisible(true);
                            return;
                        }
                    }
                    new Thread() {

                        public void run() {
                            JGlossFrame which = target == null || target.model.isEmpty() ? new JGlossFrame() : target;
                            which.loadDocument(file);
                        }
                    }.start();
                }
            };
            importClipboardListener = new ImportClipboardListener(importClipboard);
        }
    }

    /**
     * Static instance of the actions which can be used by other classes. If an action
     * is invoked, a new <CODE>JGlossFrame</CODE> will be created as the target of the
     * action.
     */
    public static final Actions actions = new Actions(null);

    /**
     * Updates the status of the import clipboard action corresponding to certain events.
     * The import clipboard action should only be enabled if the system clipboard contains a
     * string. This listener checks the status of the clipboard and updates the action state if
     * the window the listener is attached to is brought to the foreground and/or if the menu the
     * listener is attached to is expanded.
     */
    private static class ImportClipboardListener extends WindowAdapter implements MenuListener {

        private Action importClipboard;

        public ImportClipboardListener(Action _importClipboard) {
            this.importClipboard = _importClipboard;
        }

        private void checkUpdate() {
            importClipboard.setEnabled(UIUtilities.clipboardContainsString());
        }

        public void windowActivated(WindowEvent e) {
            System.out.println("WINDOW ACTIVATED");
            checkUpdate();
            if (UIUtilities.clipboardContainsString()) {
                this.importClipboard.actionPerformed(null);
            }
        }

        public void menuSelected(MenuEvent e) {
            checkUpdate();
        }

        public void menuDeselected(MenuEvent e) {
        }

        public void menuCanceled(MenuEvent e) {
        }
    }

    /**
     * Open recent menu used for JGloss documents. The instance is shared between instances of
     * <CODE>JGlossFrame</CODE> and {@link LookupFrame LookupFrame}.
     */
    public static final OpenRecentMenu OPEN_RECENT = new OpenRecentMenu(8);

    /**
     * Data model of this frame.
     */
    private JGlossFrameModel model;

    /**
     * JGloss document frame object. The frame keeps the <code>JGlossFrame</code> as sole component
     * in its content pane.
     */
    private JFrame frame;

    /**
     * Reacts to the document closed events.
     */
    private WindowListener windowListener;

    /**
     * Saves changes in window size to the preferences.
     */
    private ComponentListener componentListener;

    /**
     * The document editor.
     */
    private JGlossEditor docpane;

    /**
     * Scrollpane which contains the document editor.
     */
    private JScrollPane docpaneScroller;

    /**
     * Editor kit used in the creation of the document.
     */
    private JGlossEditorKit kit;

    private AnnotationList annotationList;

    private AnnotationEditorPanel annotationEditor;

    private SimpleLookup lookupPanel;

    private JSplitPane[] splitPanes;

    private LookupResultList.Hyperlinker hyperlinker;

    /**
     * Remembers the first dictionary entry shown in the result list of the lookup panel.
     * This is used when a new annotation is created to automatically set the reading and
     * translation.
     */
    private FirstEntryCache firstEntryCache;

    private Position lastSelectionStart;

    private Position lastSelectionEnd;

    /**
     * Defer a window closing event until the frame object is in a safe state. This is used while
     * the <CODE>loadDocument</CODE> method is executing.
     */
    private boolean deferWindowClosing = false;

    private Transformer jglossWriterTransformer;

    /**
     * Manager for the cut/copy/past actions;
     */
    private XCVManager xcvManager;

    /**
     * Saves the document.
     */
    private Action saveAction;

    /**
     * Saves the document after asking the user for a filename.
     */
    private Action saveAsAction;

    /**
     * Prints the document.
     */
    private Action printAction;

    /**
     * Closes this JGlossFrame.
     */
    private Action closeAction;

    /**
     * Action which annotates the current selection.
     */
    private Action addAnnotationAction;

    /**
     * Displays the document title and lets the user change it.
     */
    private Action documentTitleAction;

    /**
     * Open recent menu for this instance of <CODE>JGlossFrame</CODE>.
     */
    private JMenu openRecentMenu;

    /**
     * Submenu containing the export actions.
     */
    private ExportMenu exportMenu;

    /**
     * Menu item which holds the show preferences action.
     */
    private JMenuItem preferencesItem;

    /**
     * Menu item which holds the show about box action.
     */
    private JMenuItem aboutItem;

    /**
     * Listens to changes of the properties.
     */
    private PropertyChangeListener prefsListener;

    /**
     * List of open JGloss documents.
     */
    private static LinkedList<JGlossFrame> jglossFrames = new LinkedList<JGlossFrame>();

    /**
     * Returns the number of currently open JGlossFrames.
     */
    public static int getFrameCount() {
        return jglossFrames.size();
    }

    /**
     * A file filter which will accept JGloss documents.
     */
    public static final javax.swing.filechooser.FileFilter jglossFileFilter = new ExtensionFileFilter("jgloss", JGloss.messages.getString("filefilter.description.jgloss"));

    /**
     * Creates a new JGlossFrame which does not contain a document. The user can add a document
     * by using import or open actions.
     */
    public JGlossFrame() {
        jglossFrames.add(this);
        model = new JGlossFrameModel();
        frame = new JFrame(JGloss.messages.getString("main.title"));
        frame.getContentPane().setBackground(Color.white);
        frame.getContentPane().setLayout(new GridLayout(1, 1));
        frame.setLocation(JGloss.prefs.getInt(Preferences.FRAME_X, 0), JGloss.prefs.getInt(Preferences.FRAME_Y, 0));
        frame.setSize(JGloss.prefs.getInt(Preferences.FRAME_WIDTH, frame.getPreferredSize().width), JGloss.prefs.getInt(Preferences.FRAME_HEIGHT, frame.getPreferredSize().height));
        componentListener = new ComponentAdapter() {

            public void componentMoved(ComponentEvent e) {
                JGloss.prefs.set(Preferences.FRAME_X, frame.getX());
                JGloss.prefs.set(Preferences.FRAME_Y, frame.getY());
            }

            public void componentResized(ComponentEvent e) {
                JGloss.prefs.set(Preferences.FRAME_WIDTH, frame.getWidth());
                JGloss.prefs.set(Preferences.FRAME_HEIGHT, frame.getHeight());
            }
        };
        frame.addComponentListener(componentListener);
        windowListener = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                synchronized (this) {
                    if (deferWindowClosing) {
                        deferWindowClosing = false;
                    } else {
                        if (askCloseDocument()) closeDocument();
                    }
                }
            }
        };
        frame.addWindowListener(windowListener);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        xcvManager = new XCVManager();
        initActions();
        frame.addWindowListener(actions.importClipboardListener);
        annotationList = new AnnotationList();
        annotationList.addListSelectionListener(this);
        frame.setJMenuBar(initMenuBar(actions));
        setBackground(Color.white);
        setLayout(new GridLayout(1, 1));
        annotationEditor = new AnnotationEditorPanel();
        docpane = new JGlossEditor(annotationList);
        docpane.addCaretListener(this);
        new SelectedAnnotationHighlighter(annotationList, docpane);
        xcvManager.addManagedComponent(docpane);
        hyperlinker = new LookupResultList.Hyperlinker(true, true, true, true, true);
        lookupPanel = new SimpleLookup(new Component[] { new JButton(addAnnotationAction) }, hyperlinker);
        lookupPanel.addHyperlinkListener(this);
        firstEntryCache = new FirstEntryCache();
        lookupPanel.addLookupResultHandler(firstEntryCache);
        JScrollPane annotationEditorScroller = new JScrollPane(annotationEditor, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane annotationListScroller = new JScrollPane(annotationList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JLabel rendering = new JLabel(JGloss.messages.getString("main.renderingdocument"), JLabel.CENTER);
        rendering.setBackground(Color.white);
        rendering.setOpaque(true);
        rendering.setFont(rendering.getFont().deriveFont(18.0f));
        docpaneScroller = new JScrollPane(rendering, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        KeystrokeForwarder forwarder = new KeystrokeForwarder();
        docpane.addKeyListener(forwarder);
        forwarder.addTarget(annotationList);
        forwarder.addTarget(lookupPanel.getLookupResultList().getFancyResultPane());
        annotationList.setFocusable(false);
        lookupPanel.getLookupResultList().getFancyResultPane().setFocusable(false);
        lookupPanel.getLookupResultList().getPlainResultPane().setFocusable(false);
        annotationEditorScroller.getHorizontalScrollBar().setFocusable(false);
        annotationListScroller.getHorizontalScrollBar().setFocusable(false);
        docpaneScroller.getHorizontalScrollBar().setFocusable(false);
        annotationEditorScroller.getVerticalScrollBar().setFocusable(false);
        annotationListScroller.getVerticalScrollBar().setFocusable(false);
        docpaneScroller.getVerticalScrollBar().setFocusable(false);
        JSplitPane split1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, docpaneScroller, annotationListScroller);
        split1.setOneTouchExpandable(true);
        JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, annotationEditorScroller, lookupPanel);
        split2.setOneTouchExpandable(true);
        JSplitPane split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, split1, split2);
        split3.setOneTouchExpandable(true);
        this.add(split3);
        splitPanes = new JSplitPane[] { split1, split2, split3 };
        frame.setVisible(true);
    }

    private Actions initActions() {
        saveAction = new AbstractAction() {

            /**
			 * 
			 */
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (model.getDocumentPath() == null) saveDocumentAs(); else saveDocument();
            }
        };
        saveAction.setEnabled(false);
        UIUtilities.initAction(saveAction, "main.menu.save");
        saveAsAction = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                saveDocumentAs();
            }
        };
        saveAsAction.setEnabled(false);
        UIUtilities.initAction(saveAsAction, "main.menu.saveAs");
        printAction = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                doPrint();
            }
        };
        printAction.setEnabled(false);
        UIUtilities.initAction(printAction, "main.menu.print");
        closeAction = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (askCloseDocument()) {
                    closeDocument();
                }
            }
        };
        UIUtilities.initAction(closeAction, "main.menu.close");
        addAnnotationAction = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                annotateDocumentSelection();
            }
        };
        addAnnotationAction.setEnabled(false);
        UIUtilities.initAction(addAnnotationAction, "main.menu.addannotation");
        documentTitleAction = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                String title = model.getHTMLDocument().getTitle();
                if (title == null) title = "";
                Object result = JOptionPane.showInputDialog(frame, JGloss.messages.getString("main.dialog.doctitle"), JGloss.messages.getString("main.dialog.doctitle.title"), JOptionPane.PLAIN_MESSAGE, null, null, title);
                if (result != null) {
                    model.getHTMLDocument().setTitle(result.toString());
                }
            }
        };
        documentTitleAction.setEnabled(false);
        UIUtilities.initAction(documentTitleAction, "main.menu.doctitle");
        return new Actions(this);
    }

    private JMenuBar initMenuBar(Actions actions) {
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu(JGloss.messages.getString("main.menu.file"));
        menu.add(UIUtilities.createMenuItem(actions.importDocument));
        menu.add(UIUtilities.createMenuItem(actions.importClipboard));
        menu.addMenuListener(actions.importClipboardListener);
        menu.addSeparator();
        menu.add(UIUtilities.createMenuItem(actions.open));
        openRecentMenu = OPEN_RECENT.createMenu(actions.openRecentListener);
        menu.add(openRecentMenu);
        menu.addSeparator();
        menu.add(UIUtilities.createMenuItem(saveAction));
        menu.add(UIUtilities.createMenuItem(saveAsAction));
        exportMenu = new ExportMenu();
        exportMenu.setMnemonic(JGloss.messages.getString("main.menu.export.mk").charAt(0));
        menu.add(exportMenu);
        menu.addSeparator();
        menu.add(UIUtilities.createMenuItem(printAction));
        menu.addSeparator();
        menu.add(UIUtilities.createMenuItem(closeAction));
        bar.add(menu);
        menu = new JMenu(JGloss.messages.getString("main.menu.edit"));
        menu.add(UIUtilities.createMenuItem(xcvManager.getCutAction()));
        menu.add(UIUtilities.createMenuItem(xcvManager.getCopyAction()));
        menu.add(UIUtilities.createMenuItem(xcvManager.getPasteAction()));
        menu.addMenuListener(xcvManager.getEditMenuListener());
        menu.addSeparator();
        menu.add(UIUtilities.createMenuItem(addAnnotationAction));
        menu.add(UIUtilities.createMenuItem(documentTitleAction));
        menu.addSeparator();
        preferencesItem = UIUtilities.createMenuItem(PreferencesFrame.showAction);
        menu.add(preferencesItem);
        bar.add(menu);
        bar.add(menu);
        bar.add(annotationList.getMenu());
        menu = new JMenu(JGloss.messages.getString("main.menu.help"));
        aboutItem = UIUtilities.createMenuItem(AboutFrame.getShowAction());
        menu.add(aboutItem);
        bar.add(menu);
        return bar;
    }

    public JGlossFrameModel getModel() {
        return model;
    }

    /**
     * Checks if it is OK to close the document. If the user has changed the document,
     * a dialog will ask the user if the changes should be saved and the appropriate
     * actions will be taken. If after this the document can be closed, the
     * method will return <CODE>true</CODE>.
     *
     * @return <CODE>true</CODE>, if the document can be closed.
     */
    private boolean askCloseDocument() {
        if (!model.isEmpty() && model.isDocumentChanged()) {
            int r = JOptionPane.showOptionDialog(this, JGloss.messages.getString("main.dialog.close.message", new Object[] { model.getDocumentName() }), JGloss.messages.getString("main.dialog.close.title"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] { JGloss.messages.getString("button.save"), JGloss.messages.getString("button.discard"), JGloss.messages.getString("button.cancel") }, JGloss.messages.getString("button.save"));
            switch(r) {
                case 0:
                    if (model.getDocumentPath() == null) saveDocumentAs(); else saveDocument();
                    break;
                case 1:
                    model.setDocumentChanged(false);
                    break;
                case 2:
                default:
                    break;
            }
        }
        return !(!model.isEmpty() && model.isDocumentChanged());
    }

    /**
     * Close the document window and clean up associated resources.
     */
    private void closeDocument() {
        if (model.getDocumentPath() != null) {
            int index = annotationList.getSelectedIndex();
            if (index != -1) {
                StringBuffer history = new StringBuffer();
                String[] oldHistory = JGloss.prefs.getList(Preferences.HISTORY_SELECTION, File.pathSeparatorChar);
                int maxsize = JGloss.prefs.getInt(Preferences.HISTORY_SIZE, 20);
                for (int i = 0; i < oldHistory.length && i < (maxsize - 1) * 2; i += 2) try {
                    if (!oldHistory[i].equals(model.getDocumentPath())) {
                        if (history.length() > 0) history.append(File.pathSeparatorChar);
                        history.append(oldHistory[i]);
                        history.append(File.pathSeparatorChar);
                        history.append(oldHistory[i + 1]);
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {
                }
                if (history.length() > 0) history.insert(0, File.pathSeparatorChar);
                history.insert(0, index);
                history.insert(0, File.pathSeparatorChar);
                history.insert(0, model.getDocumentPath());
                JGloss.prefs.set(Preferences.HISTORY_SELECTION, history.toString());
            }
        }
        frame.setVisible(true);
        this.dispose();
        if (jglossFrames.size() == 0) {
            JGloss.exit();
        }
    }

    /**
     * Imports the content of the clipboard, if it contains plain text.
     */
    private void doImportClipboard() {
        Transferable t = getToolkit().getSystemClipboard().getContents(this);
        if (t != null) {
            try {
                Reader in = null;
                int len = 0;
                String data = (String) t.getTransferData(DataFlavor.stringFlavor);
                len = data.length();
                boolean autodetect = true;
                for (int i = 0; i < data.length(); i++) {
                    if (data.charAt(i) > 255) {
                        autodetect = false;
                        break;
                    }
                }
                if (autodetect) {
                    byte[] bytes = data.getBytes("ISO-8859-1");
                    String enc = CharacterEncodingDetector.guessEncodingName(bytes);
                    if (!enc.equals(CharacterEncodingDetector.ENC_UTF_8)) data = new String(bytes, enc);
                }
                in = new StringReader(data);
                JGlossFrame which = this;
                which.importFromReader(in, JGloss.prefs.getBoolean(Preferences.IMPORTCLIPBOARD_DETECTPARAGRAPHS, true), JGloss.messages.getString("import.clipboard"), JGloss.messages.getString("import.clipboard"), GeneralDialog.getInstance().createReadingAnnotationFilter(), GeneralDialog.getInstance().createImportClipboardParser(Dictionaries.getDictionaries(true), ExclusionList.getExclusions()), len);
                which.model.setDocumentChanged(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog(this, JGloss.messages.getString("error.import.exception", new Object[] { JGloss.messages.getString("import.clipboard"), ex.getClass().getName(), ex.getLocalizedMessage() }), JGloss.messages.getString("error.import.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Sets up everything neccessary to import a file and loads it. If <CODE>filename</CODE> is
     * a URL, it will create a reader which reads from the location the document points to. If it
     * is a path to a local file, it will create a reader which reads from it. 
     * The method will then call <CODE>loadDocument</CODE> with the newly 
     * created reader.
     *
     * @param path URL or path of the file to import.
     * @param detectParagraphs Flag if paragraph detection should be done.
     * @param parser Parser used to annotate the text.
     * @param filter Filter for fetching the reading annotations from a parsed document.
     * @param encoding Character encoding of the file. May be either <CODE>null</CODE> or the
     *                 value of the "encodings.default" resource to use autodetection.
     */
    private void importDocument(String path, boolean detectParagraphs, Parser parser, ReadingAnnotationFilter filter, String encoding) {
        try {
            Reader in = null;
            int contentlength = 0;
            if (JGloss.messages.getString("encodings.default").equals(encoding)) encoding = null;
            String title = "";
            try {
                URL url = new URL(path);
                URLConnection c = url.openConnection();
                contentlength = c.getContentLength();
                String enc = c.getContentEncoding();
                InputStream is = new BufferedInputStream(c.getInputStream());
                if (encoding != null) in = new InputStreamReader(is, encoding); else {
                    in = CharacterEncodingDetector.getReader(is, enc);
                    encoding = ((InputStreamReader) in).getEncoding();
                }
                title = url.getFile();
                if (title == null || title.length() == 0) title = path;
            } catch (MalformedURLException ex) {
                File f = new File(path);
                contentlength = (int) f.length();
                title = f.getName();
                if (title.toLowerCase().endsWith("htm") || title.toLowerCase().endsWith("html")) {
                }
                InputStream is = new BufferedInputStream(new FileInputStream(path));
                if (encoding != null) in = new InputStreamReader(is, encoding); else {
                    in = CharacterEncodingDetector.getReader(is);
                    encoding = ((InputStreamReader) in).getEncoding();
                }
            }
            importFromReader(in, detectParagraphs, path, title, filter, parser, CharacterEncodingDetector.guessLength(contentlength, encoding));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog(this, JGloss.messages.getString("error.import.exception", new Object[] { path, ex.getClass().getName(), ex.getLocalizedMessage() }), JGloss.messages.getString("error.import.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            if (model.getDocumentName() == null) this.dispose();
        }
    }

    /**
     * Creates a new annotated document by reading the original text from a string.
     * The method can only be applied on a <CODE>JGlossFrame</CODE> with no open document.
     *
     * @param text The text which will be imported.
     * @param detectParagraphs Flag if paragraph detection should be done.
     * @param title Title of the newly created document.
     * @param path Path to the document.
     * @param setPath If <CODE>true</CODE>, the document path will
     *        be set to the <CODE>path</CODE> parameter. Use this if path denotes a the file to
     *        which the newly created document should be written. If <CODE>false</CODE>,
     *        <CODE>path</CODE> will only be used in informational messages to the user during import.
     */
    public void importString(String text, boolean detectParagraphs, String path, String title, Parser parser, ReadingAnnotationFilter filter, boolean setPath) {
        try {
            importFromReader(new StringReader(text), detectParagraphs, path, title, filter, parser, text.length());
            model.setDocumentChanged(true);
            if (setPath) this.model.setDocumentPath(path);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog(this, JGloss.messages.getString("error.import.exception", new Object[] { path, ex.getClass().getName(), ex.getLocalizedMessage() }), JGloss.messages.getString("error.import.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads a JGloss document from a local file.
     *
     * @param f File to load.
     */
    public void loadDocument(File f) {
        try {
            model.setDocumentPath(f.getAbsolutePath());
            loadDocument(new FileInputStream(f), f.getName());
            synchronized (this) {
                if (model.isEmpty()) {
                    return;
                }
                OPEN_RECENT.addDocument(f);
                String[] history = JGloss.prefs.getList(Preferences.HISTORY_SELECTION, File.pathSeparatorChar);
                for (int i = 0; i < history.length; i += 2) try {
                    if (history[i].equals(model.getDocumentPath())) {
                        final int index = Integer.parseInt(history[i + 1]);
                        if (index >= 0 && index < annotationList.getModel().getSize()) {
                            Runnable worker = new Runnable() {

                                public void run() {
                                    annotationList.setSelectedIndex(index);
                                }
                            };
                            if (EventQueue.isDispatchThread()) worker.run(); else EventQueue.invokeLater(worker);
                            break;
                        }
                    }
                } catch (NumberFormatException ex) {
                } catch (NullPointerException ex) {
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog(this, JGloss.messages.getString("error.load.exception", new Object[] { model.getDocumentPath(), ex.getClass().getName(), ex.getLocalizedMessage() }), JGloss.messages.getString("error.load.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDocument(InputStream in, String title) throws IOException, SAXException {
        deferWindowClosing = true;
        model.setDocument(new JGlossDocument(new InputSource(in)));
        setupFrame(title);
    }

    private void importFromReader(Reader in, boolean detectParagraphs, String path, String title, ReadingAnnotationFilter filter, Parser parser, int length) throws IOException {
        deferWindowClosing = true;
        final StopableReader stin = new StopableReader(in);
        final ProgressMonitor pm = new ProgressMonitor(this, JGloss.messages.getString("load.progress", new Object[] { path }), null, 0, length);
        final Thread currentThread = Thread.currentThread();
        javax.swing.Timer progressUpdater = new javax.swing.Timer(1000, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                pm.setProgress(stin.getCharCount());
                if (pm.isCanceled() || !deferWindowClosing) {
                    stin.stop();
                    currentThread.interrupt();
                }
            }
        });
        progressUpdater.start();
        try {
            model.setDocument(new JGlossDocumentBuilder().build(stin, detectParagraphs, filter, parser, Dictionaries.getDictionaries(true)));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog(JGlossFrame.this, JGloss.messages.getString("error.import.exception", new Object[] { path, ex.getClass().getName(), ex.getLocalizedMessage() }), JGloss.messages.getString("error.import.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
        progressUpdater.stop();
        in.close();
        pm.close();
        if (model.isEmpty()) {
            deferWindowClosing = false;
        } else {
            setupFrame(title);
        }
    }

    private void setupFrame(String title) throws IOException {
        if (!deferWindowClosing) {
            closeDocument();
            return;
        }
        kit = new JGlossEditorKit(false, false, false);
        final JGlossHTMLDoc htmlDoc = (JGlossHTMLDoc) kit.createDefaultDocument();
        model.setHTMLDocument(htmlDoc);
        DocumentStyleDialog.getDocumentStyleDialog().addStyleSheet(htmlDoc.getStyleSheet());
        htmlDoc.setJGlossDocument(model.getDocument());
        model.setDocumentName(title);
        Runnable worker = new Runnable() {

            public void run() {
                final Cursor currentCursor = getCursor();
                updateTitle();
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                docpane.setEditorKit(kit);
                xcvManager.updateActions(docpane);
                docpane.setStyledDocument(htmlDoc);
                AnnotationListModel annoModel = new AnnotationListModel(htmlDoc.getAnnotationElements());
                new AnnotationListSynchronizer(htmlDoc, annoModel);
                model.setAnnotationListModel(annoModel);
                annotationList.setAnnotationListModel(annoModel);
                annoModel.addAnnotationListener(annotationEditor);
                frame.getContentPane().removeAll();
                frame.getContentPane().add(JGlossFrame.this);
                frame.validate();
                SplitPaneManager splitManager = new SplitPaneManager("view.");
                splitManager.add(splitPanes[0], 1.0);
                splitManager.add(splitPanes[1], 0.3);
                splitManager.add(splitPanes[2], 0.65);
                final Thread renderer = new Thread() {

                    public void run() {
                        try {
                            setPriority(Math.max(getPriority() - 3, Thread.MIN_PRIORITY));
                        } catch (IllegalArgumentException ex) {
                        }
                        JGlossEditor dp = docpane;
                        JScrollPane ds = docpaneScroller;
                        final JViewport port = new JViewport();
                        if (dp != null && ds != null) {
                            synchronized (dp) {
                                dp.setSize(ds.getViewport().getExtentSize().width, dp.getPreferredSize().height);
                                dp.setSize(ds.getViewport().getExtentSize().width, dp.getPreferredSize().height);
                                port.setView(dp);
                            }
                        }
                        Runnable installer = new Runnable() {

                            public void run() {
                                synchronized (frame) {
                                    if (docpaneScroller != null && docpane != null && annotationEditor != null) {
                                        docpaneScroller.setViewport(port);
                                        frame.validate();
                                        Annotation current = (Annotation) annotationList.getSelectedValue();
                                        if (current != null) {
                                            docpane.makeVisible(current.getStartOffset(), current.getEndOffset());
                                        }
                                        setCursor(currentCursor);
                                        docpane.requestFocusInWindow();
                                    }
                                }
                            }
                        };
                        EventQueue.invokeLater(installer);
                    }
                };
                Runnable rendererStart = new Runnable() {

                    public void run() {
                        renderer.start();
                    }
                };
                EventQueue.invokeLater(rendererStart);
                htmlDoc.setStrictParsing(false);
                exportMenu.setContext(model);
                printAction.setEnabled(true);
                if (model.getDocumentPath() == null) saveAction.setEnabled(true);
                saveAsAction.setEnabled(true);
                htmlDoc.addPropertyChangeListener(new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent e) {
                        markChanged();
                    }
                });
                htmlDoc.addDocumentListener(new DocumentListener() {

                    public void insertUpdate(DocumentEvent e) {
                        markChanged();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        markChanged();
                    }

                    public void changedUpdate(DocumentEvent e) {
                    }
                });
            }
        };
        if (EventQueue.isDispatchThread()) {
            worker.run();
        } else {
            try {
                EventQueue.invokeAndWait(worker);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (InvocationTargetException ex2) {
                if (ex2.getCause() instanceof IOException) throw (IOException) ex2.getCause(); else if (ex2.getCause() instanceof RuntimeException) throw (RuntimeException) ex2.getCause(); else ex2.printStackTrace();
            }
        }
        if (!deferWindowClosing) {
            closeDocument();
        }
        deferWindowClosing = false;
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            int colon = e.getDescription().indexOf(':');
            String protocol = e.getDescription().substring(0, colon);
            String refKey = e.getDescription().substring(colon + 1);
            handleHyperlink(protocol, refKey, e.getSourceElement());
        }
    }

    private void handleHyperlink(String protocol, String refKey, Element e) {
        Annotation anno = (Annotation) annotationList.getSelectedValue();
        if (anno == null) return;
        String text = "";
        try {
            text = e.getDocument().getText(e.getStartOffset(), e.getEndOffset() - e.getStartOffset());
        } catch (BadLocationException ex) {
        }
        if (protocol.equals(LookupResultList.Hyperlinker.WORD_PROTOCOL)) anno.setDictionaryForm(text); else if (protocol.equals(LookupResultList.Hyperlinker.READING_PROTOCOL)) {
            anno.setDictionaryFormReading(text);
            anno.setReading(text);
        } else if (protocol.equals(LookupResultList.Hyperlinker.TRANSLATION_PROTOCOL)) anno.setTranslation(text);
    }

    /**
     * React to changes to the selection of the annotation list by selecting the annotation
     * in the annotation editor and lookup panel.
     */
    public void valueChanged(ListSelectionEvent e) {
        if (e.getFirstIndex() >= 0) {
            Annotation anno = (Annotation) annotationList.getSelectedValue();
            if (anno != null) {
                annotationEditor.setAnnotation(anno);
                lookupPanel.search(anno.getDictionaryForm());
            } else {
                annotationEditor.setAnnotation(null);
            }
        }
    }

    /**
     * Reacts to text selection in the annotated document by searching the selected text in
     * the lookup panel.
     */
    public void caretUpdate(CaretEvent e) {
        if (e.getDot() == e.getMark()) {
            addAnnotationAction.setEnabled(false);
        } else {
            addAnnotationAction.setEnabled(true);
            int from;
            int to;
            if (e.getDot() < e.getMark()) {
                from = e.getDot();
                to = e.getMark();
            } else {
                from = e.getMark();
                to = e.getDot();
            }
            if (lastSelectionStart != null && lastSelectionStart.getOffset() == from && lastSelectionEnd != null && lastSelectionEnd.getOffset() == to) return;
            String selection = model.getHTMLDocument().getUnannotatedText(from, to);
            if (selection.length() > 0) lookupPanel.search(selection);
            try {
                lastSelectionStart = model.getHTMLDocument().createPosition(from);
                lastSelectionEnd = model.getHTMLDocument().createPosition(to);
            } catch (BadLocationException ex) {
            }
        }
    }

    /**
     * Runs the print dialog and prints the document.
     */
    private void doPrint() {
        if (kit != null) {
            JobAttributes ja = new JobAttributes();
            PageAttributes pa = new PageAttributes();
            pa.setOrigin(PageAttributes.OriginType.PRINTABLE);
            PrintJob job = getToolkit().getPrintJob(frame, model.getDocumentName(), ja, pa);
            if (job != null) {
                Dimension page = job.getPageDimension();
                Rectangle pagebounds = new Rectangle(0, 0, page.width, page.height);
                docpaneScroller.getViewport().remove(docpane);
                docpane.setLocation(0, 0);
                docpane.setSize(page.width, docpane.getPreferredSize().height);
                docpane.setSize(page.width, docpane.getPreferredSize().height);
                Rectangle docbounds = docpane.getBounds();
                View root = docpane.getUI().getRootView(docpane);
                root.setSize(page.width, page.height);
                int pagecount = 0;
                int firstpage = 0;
                int lastpage = Integer.MAX_VALUE;
                if (ja.getDefaultSelection().equals(JobAttributes.DefaultSelectionType.RANGE)) {
                    firstpage = ja.getFromPage();
                    lastpage = ja.getToPage();
                }
                for (int copies = 0; copies < ja.getCopies(); copies++) {
                    pagebounds.y = 0;
                    while (pagebounds.y < docbounds.height) {
                        int nh = 0;
                        View v = root;
                        Rectangle r = docbounds;
                        int i = 0;
                        while (i < v.getViewCount()) {
                            View cv = v.getView(i);
                            Shape s = v.getChildAllocation(i, r);
                            Rectangle cr;
                            if (s == null) {
                                i++;
                                continue;
                            } else if (s instanceof Rectangle) cr = (Rectangle) s; else cr = s.getBounds();
                            cr.y += 3;
                            if (cr.y < pagebounds.y + page.height) {
                                if (cr.y + cr.height - 1 < pagebounds.y) {
                                } else if (cr.y + cr.height < pagebounds.y + page.height) {
                                    nh = cr.y - pagebounds.y + cr.height;
                                } else if (cv.getElement() instanceof JGlossEditorKit.AnnotationView) {
                                    break;
                                } else {
                                    i = -1;
                                    v = cv;
                                    r = cr;
                                    r.y -= 3;
                                }
                            } else break;
                            i++;
                        }
                        if (nh == 0) {
                            nh = page.height;
                        }
                        pagebounds.height = nh;
                        pagecount++;
                        if (pagecount > lastpage) break;
                        if (pagecount >= firstpage) {
                            Graphics g = job.getGraphics();
                            if (g != null) {
                                g.setClip(0, 0, pagebounds.width, pagebounds.height);
                                g.translate(0, -pagebounds.y);
                                docpane.printAll(g);
                                g.dispose();
                            }
                        }
                        pagebounds.y += pagebounds.height;
                    }
                }
                job.end();
                docpaneScroller.getViewport().setView(docpane);
            }
        }
    }

    /**
     * Saves the document in JGloss XML format.
     *
     * @return <CODE>true</CODE> if the document was successfully saved.
     */
    private boolean saveDocument() {
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(model.getDocumentPath()));
            if (jglossWriterTransformer == null) jglossWriterTransformer = TransformerFactory.newInstance().newTransformer();
            jglossWriterTransformer.transform(new DOMSource(model.getDocument().getDOMDocument()), new StreamResult(out));
            out.close();
            model.setDocumentChanged(false);
            saveAction.setEnabled(false);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog(this, JGloss.messages.getString("error.save.exception", new Object[] { model.getDocumentPath(), ex.getClass().getName(), ex.getLocalizedMessage() }), JGloss.messages.getString("error.save.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Runs a file chooser dialog, and if the user accepts saves the document under the new
     * filename.
     */
    private void saveDocumentAs() {
        String path;
        if (model.getDocumentPath() == null) path = JGloss.getCurrentDir(); else path = new File(model.getDocumentPath()).getPath();
        JFileChooser f = new SaveFileChooser(path);
        f.setFileHidingEnabled(true);
        f.addChoosableFileFilter(jglossFileFilter);
        f.setFileView(CustomFileView.getFileView());
        int r = f.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            model.setDocumentPath(f.getSelectedFile().getAbsolutePath());
            model.setDocumentName(f.getSelectedFile().getName());
            JGloss.setCurrentDir(f.getCurrentDirectory().getAbsolutePath());
            updateTitle();
            if (saveDocument()) OPEN_RECENT.addDocument(f.getSelectedFile());
        }
    }

    /**
     * Marks the document as changed and updates the save action accordingly.
     */
    protected void markChanged() {
        if (!model.isDocumentChanged()) {
            model.setDocumentChanged(true);
            if (model.getDocumentPath() != null) saveAction.setEnabled(true);
        }
    }

    /**
     * Update the document window title.
     */
    protected void updateTitle() {
        frame.setTitle(model.getDocumentName() + ":" + JGloss.messages.getString("main.title"));
    }

    protected void annotateDocumentSelection() {
        int selectionStart = docpane.getSelectionStart();
        int selectionEnd = docpane.getSelectionEnd();
        if (selectionStart == selectionEnd) return;
        annotationList.clearSelection();
        model.getHTMLDocument().addAnnotation(selectionStart, selectionEnd, kit);
        annotationList.setSelectedIndex(model.getAnnotationListModel().findAnnotationIndex(selectionStart, AnnotationListModel.BIAS_NONE));
        DictionaryEntry entry = firstEntryCache.getEntry();
        if (entry != null) {
            Annotation anno = (Annotation) annotationList.getSelectedValue();
            anno.setReading(entry.getReading(0));
            anno.setTranslation(entry.getTranslation(0, 0, 0));
        }
    }

    /**
     * Dispose resources associated with the JGloss document.
     */
    public synchronized void dispose() {
        jglossFrames.remove(this);
        JGloss.prefs.removePropertyChangeListener(prefsListener);
        if (model.getDocument() != null) DocumentStyleDialog.getDocumentStyleDialog().removeStyleSheet(model.getHTMLDocument().getStyleSheet());
        docpane.dispose();
        OPEN_RECENT.removeMenu(openRecentMenu);
        preferencesItem.setAction(null);
        aboutItem.setAction(null);
        UIUtilities.dismantleHierarchy(frame.getJMenuBar());
        frame.setJMenuBar(null);
        frame.removeComponentListener(componentListener);
        frame.removeWindowListener(windowListener);
        frame.setContentPane(new JPanel());
        frame.getContentPane().requestFocusInWindow();
        frame.dispose();
        model = null;
        splitPanes = null;
        exportMenu.setContext(null);
        exportMenu = null;
        docpane.removeCaretListener(this);
        docpane = null;
        docpaneScroller = null;
        kit = null;
        annotationList.removeListSelectionListener(this);
        annotationList = null;
        annotationEditor = null;
        lookupPanel.removeHyperlinkListener(this);
        lookupPanel = null;
        lastSelectionStart = null;
        lastSelectionEnd = null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }
}
