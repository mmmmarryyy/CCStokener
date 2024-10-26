package org.xngr.browser.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Element;
import javax.swing.text.Keymap;
import javax.swing.undo.UndoManager;
import org.bounce.event.PopupListener;
import org.bounce.image.ImageLoader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xngr.browser.ExchangerDocument;
import org.xngr.browser.ExchangerDocumentEvent;
import org.xngr.browser.ExchangerDocumentListener;
import org.xngr.browser.document.DocumentPropertiesAction;
import org.xngr.browser.explorer.Explorer;
import org.xngr.browser.properties.EditorProperties;
import org.xngr.browser.ui.CheckForUpdateAction;
import org.xngr.browser.ui.HelpAction;
import org.xngr.browser.ui.ShowAboutDialogAction;
import org.xngr.browser.util.CommonEntities;
import org.xngr.browser.util.DocumentUtilities;

/**
 * The editor for an eXchaNGeR document.
 *
 * @version	$Revision: 1.17 $, $Date: 2007/06/28 13:14:10 $
 * @author Edwin Dankert <edankert@cladonia.com>
 */
public class Editor extends JFrame implements CaretListener, UndoableEditListener, ExchangerDocumentListener {

    private static final long serialVersionUID = 8554059554604880384L;

    private static final ImageIcon ICON = ImageLoader.get().getImage("/org/xngr/browser/icons/EditorIcon.gif");

    private static final Border BEVEL_BORDER = new CompoundBorder(new BevelBorder(BevelBorder.LOWERED, Color.white, new Color(204, 204, 204), new Color(204, 204, 204), new Color(102, 102, 102)), new EmptyBorder(0, 2, 0, 0));

    private String title = null;

    private boolean changed = false;

    private int discardNextEvent = 0;

    private UndoAction undo = null;

    private RedoAction redo = null;

    private FindNextAction findNext = null;

    private JCheckBoxMenuItem validateItem = null;

    private UndoManager undoManager = null;

    private ExchangerDocument document = null;

    private XmlEditorPane editor = null;

    private JTextField errorLabel = null;

    private JLabel positionLabel = null;

    private EditorProperties props = null;

    private JPopupMenu popup = null;

    private Explorer explorer = null;

    private WaitGlassPane waitPane = null;

    /**
	 * Constructs an editor for the document.
	 *
	 * @param explorer the documents explorer.
	 * @param props the properties for the editor.
	 * @param document the document to be edited.
	 */
    public Editor(Explorer explorer, EditorProperties props, ExchangerDocument document) {
        setIconImage(ICON.getImage());
        title = "eXchaNGeR - " + document.getName();
        setTitle(title);
        this.props = props;
        this.document = document;
        this.explorer = explorer;
        undoManager = new UndoManager();
        undoManager.setLimit(100);
        editor = new XmlEditor();
        popup = new JPopupMenu();
        editor.addMouseListener(new PopupListener() {

            public void popupTriggered(MouseEvent e) {
                popup.show(editor, e.getX(), e.getY());
            }
        });
        editor.setCaret(new XmlCaret());
        editor.addCaretListener(this);
        if (document.isReadOnly()) {
            editor.setEditable(false);
            editor.setBackground(new Color(204, 204, 204));
            editor.setSelectionColor(new Color(153, 153, 153));
        } else {
            editor.setSelectionColor(new Color(204, 204, 204));
        }
        Keymap keymap = editor.getKeymap();
        Action action = keymap.getAction(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0, false));
        if (action == null || !(action instanceof IndentAction)) {
            keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0, false), new AbstractAction() {

                private static final long serialVersionUID = -4888979080303795037L;

                public void actionPerformed(ActionEvent event) {
                    Object source = event.getSource();
                    if (source instanceof XmlEditor) {
                        ((XmlEditor) source).indentSelectedText();
                    }
                }
            });
        }
        action = keymap.getAction(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK, false));
        if (action == null || !(action instanceof UnindentAction)) {
            keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK, false), new AbstractAction() {

                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent event) {
                    Object source = event.getSource();
                    if (source instanceof XmlEditor) {
                        ((XmlEditor) source).unindentSelectedText();
                    }
                }
            });
        }
        JScrollPane scroller = new JScrollPane(editor);
        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(new EmptyBorder(0, 0, 0, 0));
        main.add(scroller, BorderLayout.CENTER);
        JMenuBar menu = new JMenuBar();
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        SaveAction save = new SaveAction(this);
        JMenuItem saveItem = new JMenuItem(save);
        saveItem.setAccelerator((KeyStroke) save.getValue(SaveAction.ACCELERATOR_KEY));
        fileMenu.add(saveItem);
        toolbar.add(save);
        save.setEnabled(!document.isReadOnly());
        SaveAsAction saveAs = new SaveAsAction(this);
        JMenuItem saveAsItem = new JMenuItem(saveAs);
        saveAsItem.setAccelerator((KeyStroke) saveAs.getValue(SaveAction.ACCELERATOR_KEY));
        fileMenu.add(saveAsItem);
        toolbar.add(saveAs);
        LoadAction load = new LoadAction(this);
        JMenuItem loadItem = new JMenuItem(load);
        loadItem.setAccelerator((KeyStroke) load.getValue(LoadAction.ACCELERATOR_KEY));
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        PageSetupAction pageSetup = new PageSetupAction();
        JMenuItem pageSetupItem = new JMenuItem(pageSetup);
        pageSetupItem.setAccelerator((KeyStroke) pageSetup.getValue(PageSetupAction.ACCELERATOR_KEY));
        fileMenu.add(pageSetupItem);
        PrintAction print = new PrintAction(this);
        JMenuItem printItem = new JMenuItem(print);
        printItem.setAccelerator((KeyStroke) print.getValue(PrintAction.ACCELERATOR_KEY));
        fileMenu.add(printItem);
        toolbar.addSeparator();
        toolbar.add(print);
        toolbar.addSeparator();
        fileMenu.addSeparator();
        CloseAction close = new CloseAction(this);
        JMenuItem closeItem = new JMenuItem(close);
        fileMenu.add(closeItem);
        menu.add(fileMenu);
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        undo = new UndoAction(this);
        JMenuItem undoItem = new JMenuItem(undo);
        undoItem.setAccelerator((KeyStroke) undo.getValue(UndoAction.ACCELERATOR_KEY));
        editMenu.add(undoItem);
        undo.setEnabled(!document.isReadOnly());
        redo = new RedoAction(this);
        JMenuItem redoItem = new JMenuItem(redo);
        redoItem.setAccelerator((KeyStroke) redo.getValue(RedoAction.ACCELERATOR_KEY));
        editMenu.add(redoItem);
        redo.setEnabled(!document.isReadOnly());
        toolbar.addSeparator();
        toolbar.add(undo);
        toolbar.add(redo);
        undo.setEnabled(false);
        redo.setEnabled(false);
        editMenu.addSeparator();
        CutAction cut = new CutAction(editor);
        JMenuItem cutItem = new JMenuItem(cut);
        cutItem.setAccelerator((KeyStroke) cut.getValue(CutAction.ACCELERATOR_KEY));
        editMenu.add(cutItem);
        cut.setEnabled(!document.isReadOnly());
        CopyAction copy = new CopyAction(editor);
        JMenuItem copyItem = new JMenuItem(copy);
        copyItem.setAccelerator((KeyStroke) copy.getValue(CopyAction.ACCELERATOR_KEY));
        editMenu.add(copyItem);
        PasteAction paste = new PasteAction(editor);
        JMenuItem pasteItem = new JMenuItem(paste);
        pasteItem.setAccelerator((KeyStroke) paste.getValue(PasteAction.ACCELERATOR_KEY));
        editMenu.add(pasteItem);
        paste.setEnabled(!document.isReadOnly());
        toolbar.addSeparator();
        toolbar.add(cut);
        toolbar.add(copy);
        toolbar.add(paste);
        editMenu.addSeparator();
        TagAction tagAction = new TagAction(this);
        JMenuItem tagItem = new JMenuItem(tagAction);
        tagItem.setAccelerator((KeyStroke) tagAction.getValue(TagAction.ACCELERATOR_KEY));
        editMenu.add(tagItem);
        tagAction.setEnabled(!document.isReadOnly());
        CommentAction commentAction = new CommentAction(this);
        JMenuItem commentItem = new JMenuItem(commentAction);
        commentItem.setAccelerator((KeyStroke) commentAction.getValue(CommentAction.ACCELERATOR_KEY));
        editMenu.add(commentItem);
        commentAction.setEnabled(!document.isReadOnly());
        IndentAction indent = new IndentAction(this);
        UnindentAction unindent = new UnindentAction(this);
        indent.setEnabled(!document.isReadOnly());
        unindent.setEnabled(!document.isReadOnly());
        toolbar.addSeparator();
        toolbar.add(tagAction);
        toolbar.add(commentAction);
        toolbar.add(indent);
        toolbar.add(unindent);
        editMenu.addSeparator();
        FindAction find = new FindAction(this);
        JMenuItem findItem = new JMenuItem(find);
        findItem.setAccelerator((KeyStroke) find.getValue(FindAction.ACCELERATOR_KEY));
        editMenu.add(findItem);
        findNext = new FindNextAction(this);
        JMenuItem findNextItem = new JMenuItem(findNext);
        findNextItem.setAccelerator((KeyStroke) findNext.getValue(FindNextAction.ACCELERATOR_KEY));
        editMenu.add(findNextItem);
        GotoAction gotoAction = new GotoAction(this);
        JMenuItem gotoItem = new JMenuItem(gotoAction);
        gotoItem.setAccelerator((KeyStroke) gotoAction.getValue(GotoAction.ACCELERATOR_KEY));
        editMenu.add(gotoItem);
        toolbar.addSeparator();
        toolbar.add(find);
        toolbar.add(findNext);
        toolbar.add(gotoAction);
        menu.add(editMenu);
        JMenu documentMenu = new JMenu("Document");
        documentMenu.setMnemonic('D');
        ValidateAction validate = new ValidateAction(this);
        JMenuItem parseItem = new JMenuItem(validate);
        parseItem.setAccelerator((KeyStroke) validate.getValue(ValidateAction.ACCELERATOR_KEY));
        documentMenu.add(parseItem);
        validateItem = new JCheckBoxMenuItem("Validation", document.getProperties().validate());
        documentMenu.add(validateItem);
        documentMenu.addSeparator();
        documentMenu.setEnabled(!document.isReadOnly());
        FormatAction format = new FormatAction(this);
        JMenuItem formatItem = new JMenuItem(format);
        formatItem.setAccelerator((KeyStroke) format.getValue(FormatAction.ACCELERATOR_KEY));
        documentMenu.add(formatItem);
        format.setEnabled(!document.isReadOnly());
        documentMenu.addSeparator();
        DocumentPropertiesAction properties = new DocumentPropertiesAction(this);
        properties.setDocument(document);
        documentMenu.add(properties);
        menu.add(documentMenu);
        JMenu customizeMenu = new JMenu("Customize");
        customizeMenu.setMnemonic('C');
        PreferencesAction preferences = new PreferencesAction(this);
        customizeMenu.add(preferences);
        menu.add(customizeMenu);
        toolbar.addSeparator();
        toolbar.add(format);
        JMenu help = new JMenu("Help");
        help.setMnemonic('H');
        EditorHelpAction helpAction = new EditorHelpAction();
        JMenuItem helpItem = new JMenuItem(helpAction);
        helpItem.setAccelerator((KeyStroke) helpAction.getValue(HelpAction.ACCELERATOR_KEY));
        help.add(helpItem);
        toolbar.addSeparator();
        toolbar.add(helpAction);
        help.add(new HelpAction());
        help.addSeparator();
        CheckForUpdateAction check = new CheckForUpdateAction(this);
        help.add(check);
        toolbar.add(check);
        ShowAboutDialogAction about = new ShowAboutDialogAction(this);
        help.add(about);
        toolbar.add(about);
        menu.add(help);
        popup.add(validate);
        popup.addSeparator();
        popup.add(cut);
        popup.add(copy);
        popup.add(paste);
        popup.addSeparator();
        popup.add(undo);
        popup.add(redo);
        popup.addSeparator();
        popup.add(save);
        JPanel status = new JPanel(new BorderLayout(2, 0));
        errorLabel = new JTextField();
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN));
        errorLabel.setBorder(null);
        errorLabel.setEditable(false);
        errorLabel.setBackground(status.getBackground());
        positionLabel = new JLabel("Ln 1 Col 1");
        positionLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN));
        positionLabel.setForeground(Color.black);
        positionLabel.setPreferredSize(new Dimension(100, 16));
        positionLabel.setBorder(BEVEL_BORDER);
        status.add(errorLabel, BorderLayout.CENTER);
        status.add(positionLabel, BorderLayout.EAST);
        status.setBorder(new EmptyBorder(2, 2, 0, 0));
        main.add(toolbar, BorderLayout.NORTH);
        main.add(status, BorderLayout.SOUTH);
        setJMenuBar(menu);
        setContentPane(main);
        waitPane = new WaitGlassPane();
        setGlassPane(waitPane);
        editor.getDocument().addUndoableEditListener(this);
        document.addListener(this);
        File file = new File(document.getURL().getPath());
        save.setEnabled(file.canWrite());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        setLocation(props.getPosition());
        setSize(props.getDimension());
        try {
            load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        explorer.addEditor(this);
    }

    public void setPreferredFont(Font font) {
        editor.setFont(font);
        errorLabel.setFont(font);
        this.getRootPane().revalidate();
        this.repaint();
        editor.updateUI();
    }

    /**
	 * Should the validation happen against the DTD as referenced 
	 * in the DOCTYPE element.
	 *
	 * @return true when the DTD should be used.
	 */
    public boolean isValidate() {
        return validateItem.getState();
    }

    /**
	 * Returns the editor pane.
	 *
	 * @return the editor pane.
	 */
    public XmlEditorPane getEditor() {
        return editor;
    }

    /**
	 * Do a search in the XML text.
	 *
	 * @param search the text to search for.
	 * @param matchcase should the search match the case.
	 * @param search down/upward from caret the position.
	 */
    public void search(String search, boolean matchCase, boolean down) {
        findNext.setValues(search, matchCase, down);
        props.addSearch(search);
        props.setMatchCase(matchCase);
        props.setDirectionDown(down);
        String newSearch = search;
        try {
            String text = editor.getText(0, editor.getDocument().getLength());
            if (!matchCase) {
                text = text.toLowerCase();
                newSearch = search.toLowerCase();
            }
            int index = 0;
            if (down) {
                index = text.indexOf(newSearch, editor.getCaretPosition());
            } else {
                int pos = editor.getCaretPosition() - (search.length() + 1);
                if (pos > 0) {
                    index = text.lastIndexOf(newSearch, pos);
                } else {
                    index = text.lastIndexOf(newSearch, editor.getCaretPosition());
                }
            }
            if (index != -1) {
                editor.select(index, index + search.length());
                setStatus(-1, "Done");
            } else {
                setStatus(-1, "String \"" + search + "\" not found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Close the editor....
	 *
	 * @param search the text to search for.
	 * @param matchcase should the search match the case.
	 * @param search down/upward from caret the position.
	 */
    public boolean close() {
        boolean result = true;
        if (isChanged()) {
            int value = JOptionPane.showConfirmDialog(this, "Do you want to save the changes you made to " + document.getName() + "?", "Please confirm", JOptionPane.YES_NO_CANCEL_OPTION);
            if (value == JOptionPane.YES_OPTION) {
                Runnable runner = new Runnable() {

                    public void run() {
                        try {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    setWait(true);
                                    setStatus(-1, "Saving...");
                                }
                            });
                            save();
                        } catch (final Throwable t) {
                        } finally {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    setWait(false);
                                    finish();
                                }
                            });
                        }
                    }
                };
                Thread thread = new Thread(runner);
                thread.start();
            } else if (value == JOptionPane.CANCEL_OPTION) {
                result = false;
            } else if (value == JOptionPane.NO_OPTION) {
                finish();
            }
        } else {
            finish();
        }
        return result;
    }

    /**
	 * Save the information in the editor to a file....
	 */
    public synchronized void save() throws IOException, SAXParseException {
        try {
            FileOutputStream out = new FileOutputStream(document.getURL().getFile());
            out.write(editor.getText(0, editor.getDocument().getLength()).getBytes("UTF-8"));
            out.close();
            discardNextEvent++;
            document.load();
            setChanged(false);
            undoManager.discardAllEdits();
            undo.setEnabled(undoManager.canUndo());
            redo.setEnabled(undoManager.canRedo());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Returns the document.
	 *
	 * @return the document.
	 */
    public ExchangerDocument getDocument() {
        return document;
    }

    /**
	 * Save the information in the editor to a different file....
	 *
	 * @param url the url for the document.
	 */
    public synchronized void saveAs(File file) throws IOException {
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(editor.getText(0, editor.getDocument().getLength()).getBytes("UTF-8"));
            out.close();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Goto the start of a specific line in the document.
	 *
	 * @param line the line to go to.
	 */
    public void gotoLine(int line) {
        if (line > 0) {
            Element root = editor.getDocument().getDefaultRootElement();
            if (line > root.getElementCount()) {
                line = root.getElementCount();
            }
            Element elem = root.getElement(line - 1);
            editor.setCaretPosition(elem.getStartOffset());
        }
    }

    /**
	 * Tags the selected text.
	 *
	 * @param name the name of the tag.
	 */
    public void tagSelectedText(String name) {
        String selection = editor.getSelectedText();
        StringBuffer buffer = new StringBuffer("<");
        buffer.append(name);
        buffer.append(">");
        if (selection != null) {
            buffer.append(selection);
        }
        buffer.append("</");
        buffer.append(name);
        buffer.append(">");
        editor.replaceSelection(buffer.toString());
    }

    /**
	 * Comments the selected text.
	 */
    public void commentSelectedText() {
        String selection = editor.getSelectedText();
        StringBuffer buffer = new StringBuffer("<!-- ");
        if (selection != null) {
            buffer.append(selection);
        }
        buffer.append(" -->");
        editor.replaceSelection(buffer.toString());
    }

    /**
	 * Unindents the selected text.
	 */
    public void unindentSelectedText() {
        int start = Math.min(editor.getSelectionStart(), editor.getSelectionEnd());
        int end = Math.max(editor.getSelectionStart(), editor.getSelectionEnd());
        ;
        XmlDocument document = (XmlDocument) editor.getDocument();
        Element root = document.getDefaultRootElement();
        int startElementIndex = root.getElementIndex(start);
        int endElementIndex = root.getElementIndex(end > start ? end - 1 : end);
        for (int i = startElementIndex; i < endElementIndex + 1; i++) {
            Element element = root.getElement(i);
            int off = element.getStartOffset();
            try {
                String text = editor.getText(off, getSpacesForTab());
                int spaces = 0;
                while (spaces < getSpacesForTab()) {
                    if (text.charAt(spaces) != ' ') {
                        break;
                    }
                    spaces++;
                }
                if (spaces > 0) {
                    document.remove(off, spaces);
                }
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Indents the selected text.
	 *
	 * @param tab when true the tab key is used as indentation.
	 */
    public void indentSelectedText(boolean tab) {
        int start = Math.min(editor.getSelectionStart(), editor.getSelectionEnd());
        int end = Math.max(editor.getSelectionStart(), editor.getSelectionEnd());
        ;
        if (tab) {
            if (start != end) {
                XmlDocument document = (XmlDocument) editor.getDocument();
                Element root = document.getDefaultRootElement();
                int startElementIndex = root.getElementIndex(start);
                int endElementIndex = root.getElementIndex(end);
                if (startElementIndex != endElementIndex) {
                    endElementIndex = root.getElementIndex(end - 1);
                    for (int i = startElementIndex; i < endElementIndex + 1; i++) {
                        Element element = root.getElement(i);
                        try {
                            document.insertString(element.getStartOffset(), getTabString(), null);
                        } catch (Exception e) {
                        }
                    }
                } else {
                    editor.replaceSelection(getTabString());
                }
            } else {
                editor.replaceSelection(getTabString());
            }
        } else {
            XmlDocument document = (XmlDocument) editor.getDocument();
            Element root = document.getDefaultRootElement();
            int startElementIndex = root.getElementIndex(start);
            int endElementIndex = root.getElementIndex(end > start ? end - 1 : end);
            for (int i = startElementIndex; i < endElementIndex + 1; i++) {
                Element element = root.getElement(i);
                try {
                    document.insertString(element.getStartOffset(), getTabString(), null);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * Validates the XML in this editor.
	 *
	 * @return true if the editor contains valid XML.
	 */
    public void validateXml() throws SAXParseException, IOException {
        try {
            InputSource source = new InputSource(new StringReader(editor.getText(0, editor.getDocument().getLength())));
            source.setSystemId(document.getURL().toString());
            DocumentUtilities.readDocument(source, isValidate());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Undos a previous undoable edit.
	 */
    public void undo() {
        undoManager.undo();
        redo.setEnabled(undoManager.canRedo());
        undo.setEnabled(undoManager.canUndo());
        if (!undoManager.canUndo()) {
            setChanged(false);
        }
    }

    /**
	 * Redos a previous undo.
	 */
    public void redo() {
        undoManager.redo();
        redo.setEnabled(undoManager.canRedo());
        undo.setEnabled(undoManager.canUndo());
        if (undoManager.canUndo()) {
            setChanged(true);
        }
    }

    /**
	 * Returns the editor properties.
	 *
	 * @return the editor properties.
	 */
    public EditorProperties getProperties() {
        return props;
    }

    /**
	 * returns the string representation of the tab character.
	 *
	 * @return the String the tab should be replaced with.
	 */
    public String getTabString() {
        byte[] tab = new byte[getSpacesForTab()];
        for (int i = 0; i < getSpacesForTab(); i++) {
            tab[i] = ' ';
        }
        return new String(tab);
    }

    /**
	 * Sets the wait cursor on the editor frame.
	 *
	 * @param enabled true when wait is enabled.
	 */
    public void setWait(boolean enabled) {
        waitPane.setVisible(enabled);
    }

    /**
	 * returns the number of spaces used to substitute the 
	 * Tab character.
	 *
	 * @return the String the tab should be replaced with.
	 */
    public int getSpacesForTab() {
        return props.getSpaces();
    }

    public void format() throws SAXParseException, IOException {
        try {
            String text = editor.getText(0, editor.getDocument().getLength());
            boolean allEntitiesCommon = scanCommonEntities(text);
            if (isValidate() || (!isValidate() && allEntitiesCommon)) {
                InputSource source = new InputSource(new StringReader(text));
                source.setSystemId(document.getURL().toString());
                Document doc = DocumentUtilities.readDocument(source, isValidate());
                int pos = editor.getCaretPosition();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DocumentUtilities.writeDocument(out, doc);
                String output = out.toString("UTF-8");
                if (output.length() < pos) {
                    pos = output.length();
                }
                editor.setText(output);
                editor.setCaretPosition(pos);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static synchronized String getText(URL url) throws IOException {
        BufferedInputStream stream = new BufferedInputStream(url.openStream());
        Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        CharArrayWriter writer = new CharArrayWriter();
        int ch = reader.read();
        while (ch != -1) {
            writer.write(ch);
            ch = reader.read();
        }
        return writer.toString();
    }

    public synchronized void load() throws IOException {
        String value = null;
        value = getText(document.getURL());
        editor.setText(replaceTabsWithSpaces(value));
        editor.setCaretPosition(0);
        setChanged(false);
        undoManager.discardAllEdits();
        undo.setEnabled(undoManager.canUndo());
        redo.setEnabled(undoManager.canRedo());
    }

    private String replaceTabsWithSpaces(String text) {
        StringBuffer buffer = new StringBuffer();
        int prevTab = 0;
        int nextTab = text.indexOf('\t', prevTab);
        while (nextTab != -1) {
            buffer.append(text.substring(prevTab, nextTab));
            buffer.append(getTabString());
            prevTab = nextTab + 1;
            nextTab = text.indexOf('\t', prevTab);
        }
        buffer.append(text.substring(prevTab, text.length()));
        return buffer.toString();
    }

    public void setStatus(int line, String error) {
        if (line == 0) {
            errorLabel.setForeground(Color.red);
            errorLabel.setText(error);
            errorLabel.setCaretPosition(0);
        } else if (line > 0) {
            Element root = editor.getDocument().getDefaultRootElement();
            Element elem = root.getElement(line - 1);
            int start = elem.getStartOffset();
            int end = elem.getEndOffset() - 1;
            editor.select(start, end);
            errorLabel.setForeground(Color.red);
            errorLabel.setText("Ln " + line + ": " + error);
            errorLabel.setCaretPosition(0);
        } else if (line == -100) {
            errorLabel.setForeground(Color.red);
            errorLabel.setText(error);
            errorLabel.setCaretPosition(0);
        } else {
            errorLabel.setForeground(Color.black);
            errorLabel.setText(error);
            errorLabel.setCaretPosition(0);
        }
    }

    private void finish() {
        props.setDimension(getSize());
        props.setPosition(getLocation());
        editor.getDocument().removeUndoableEditListener(this);
        document.removeListener(this);
        explorer.removeEditor(this);
        dispose();
    }

    private void setChanged(boolean changed) {
        this.changed = changed;
        if (changed) {
            setTitle(title + "*");
        } else {
            setTitle(title);
        }
    }

    private boolean isChanged() {
        return changed;
    }

    private boolean scanCommonEntities(String text) {
        boolean result = true;
        int entityEnd = 0;
        int entityStart = text.indexOf('&', entityEnd);
        while (entityStart != -1) {
            entityEnd = text.indexOf(';', entityStart);
            boolean inCDATA = false;
            int cdataStart = text.lastIndexOf("<![CDATA[", entityStart);
            if (cdataStart != -1) {
                int cdataEnd = text.indexOf("]]>", cdataStart);
                if (cdataEnd > entityStart) {
                    inCDATA = true;
                }
            }
            if (!inCDATA) {
                boolean inComment = false;
                int commentStart = text.lastIndexOf("<!--", entityStart);
                if (commentStart != -1) {
                    int commentEnd = text.indexOf("-->", commentStart);
                    if (commentEnd > entityStart) {
                        inComment = true;
                    }
                }
                String entity = text.substring(entityStart + 1, entityEnd);
                if (!inComment && !entity.startsWith("#")) {
                    if (!CommonEntities.isCommonEntity(entity)) {
                        editor.select(entityStart, entityEnd + 1);
                        JOptionPane.showMessageDialog(this, "The \"" + entity + "\" Entity is not part of the set of common entities and cannot be resolved!\n" + "Use the validation option to format this XML document.", "\"" + entity + "\" Not Common Entity", JOptionPane.INFORMATION_MESSAGE);
                        result = false;
                        break;
                    }
                }
            }
            entityStart = text.indexOf('&', entityEnd);
        }
        return result;
    }

    /**
     * Messaged when the Document has created an edit, the edit is
     * added to <code>undo</code>, an instance of UndoManager.
     */
    public void undoableEditHappened(UndoableEditEvent e) {
        undoManager.addEdit(e.getEdit());
        redo.setEnabled(undoManager.canRedo());
        undo.setEnabled(undoManager.canUndo());
        setChanged(true);
        editor.repaint();
    }

    /**
     * Messaged when the selection in the editor has changed. Will update
     * the selection in the tree.
     */
    public void caretUpdate(CaretEvent e) {
        Element root = editor.getDocument().getDefaultRootElement();
        int line = root.getElementIndex(e.getDot());
        int col = e.getDot() - root.getElement(line).getStartOffset();
        positionLabel.setText("Ln " + (line + 1) + " Col " + (col + 1));
    }

    public synchronized void documentUpdated(ExchangerDocumentEvent event) {
        if (discardNextEvent == 0) {
            if (isChanged()) {
                int value = JOptionPane.showConfirmDialog(null, "An external process has changed the document, do you want to discard the changes?", "Please confirm", JOptionPane.YES_NO_CANCEL_OPTION);
                if (value == JOptionPane.YES_OPTION) {
                    try {
                        load();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "The document has been changed by an external process, reloading document.", "Document Changed", JOptionPane.INFORMATION_MESSAGE);
                try {
                    load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (getState() == ICONIFIED) {
                setState(NORMAL);
                setVisible(true);
            } else {
                setVisible(true);
            }
        } else {
            discardNextEvent--;
        }
    }

    public void documentDeleted(ExchangerDocumentEvent event) {
    }

    private class XmlCaret extends DefaultCaret {

        private static final long serialVersionUID = -2906548287883247075L;

        boolean focus = false;

        public XmlCaret() {
            setBlinkRate(500);
        }

        public void setSelectionVisible(boolean whatever) {
            super.setSelectionVisible(true);
        }

        public boolean isVisible() {
            boolean result = true;
            if (focus) {
                result = super.isVisible();
            }
            return result;
        }

        /**
		 * Called when the component containing the caret gains
		 * focus.  This is implemented to set the caret to visible
		 * if the component is editable.
		 *
		 * @param e the focus event
		 * @see FocusListener#focusGained
		 */
        public void focusGained(FocusEvent e) {
            focus = true;
            super.focusGained(e);
        }

        /**
		 * Called when the component containing the caret loses
		 * focus.  This is implemented to set the caret to visibility
		 * to false.
		 *
		 * @param e the focus event
		 * @see FocusListener#focusLost
		 */
        public void focusLost(FocusEvent e) {
            focus = false;
        }
    }

    public class XmlEditor extends XmlEditorPane {

        private static final long serialVersionUID = 2029210431563834727L;

        public int getSpaces() {
            return props.getSpaces();
        }

        public boolean isTagCompletion() {
            return props.isTagCompletion();
        }

        public void indentSelectedText() {
            Editor.this.indentSelectedText(true);
        }

        public void unindentSelectedText() {
            Editor.this.unindentSelectedText();
        }
    }

    private class WaitGlassPane extends JPanel {

        private static final long serialVersionUID = -1132502764441687632L;

        public WaitGlassPane() {
            setOpaque(false);
            addKeyListener(new KeyAdapter() {
            });
            addMouseListener(new MouseAdapter() {
            });
            super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
    }
}
