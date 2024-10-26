package net.hussnain.beans.text;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit.Parser;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoManager;
import net.hussnain.io.Converter;
import net.hussnain.io.Utilities;
import net.hussnain.text.RabtStyleContext;
import net.hussnain.text.RabtStyleSheet;
import net.hussnain.text.RabtHTMLDocument;
import net.hussnain.text.RabtStyledDocument;

/**
 *
 * @author  hussnain
 */
public class RabtTextPane extends JPanel implements UndoableEditListener {

    private static Logger logger = Logger.getLogger("RabtPad");

    private static Hashtable<String, Action> actions;

    private static boolean actionsCreated;

    private static StyleContext styleContext;

    private static StyleSheet styleSheet;

    private static TreeMap<String, Font> fontTable;

    private static Parser parserHTML = null;

    private Action currAction;

    private UndoManager undoMng;

    private UndoAction undoAction;

    private RedoAction redoAction;

    static {
        actionsCreated = false;
        styleContext = new RabtStyleContext();
        styleSheet = new RabtStyleSheet();
        fontTable = new TreeMap<String, Font>();
        try {
            Class c = Class.forName("javax.swing.text.html.parser.ParserDelegator");
            parserHTML = (Parser) c.newInstance();
        } catch (Throwable e) {
            logger.warning("couldnt write create HTML Parser for javax.swing.text.html.parser.ParserDelegator");
        }
    }

    /** Creates new form RabtTextPane */
    public RabtTextPane(JComponent p_owner, String p_contentType) {
        initComponents();
        Document doc = null;
        EditorKit kit = null;
        if (p_contentType.equals(ContentType.html.toString())) {
            doc = new RabtHTMLDocument(styleSheet);
            kit = new HTMLEditorKit();
            ((HTMLDocument) doc).setParser(getParserHTML());
        } else {
            if (p_contentType.equals(ContentType.rtf.toString())) {
                doc = new RabtStyledDocument(styleContext);
                kit = new RTFEditorKit();
            } else {
                if (p_contentType.equals(ContentType.text.toString())) {
                    doc = new RabtStyledDocument(styleContext);
                    kit = new StyledEditorKit();
                }
            }
        }
        textPane.setEditorKit(kit);
        textPane.setDocument(doc);
        doc.addUndoableEditListener(this);
        textPane.setMargin(new Insets(3, 3, 3, 3));
        textPane.setCaretPosition(0);
        textPane.setDragEnabled(true);
        createActionTable(textPane);
        undoMng = new UndoManager();
        undoAction = new UndoAction();
        redoAction = new RedoAction();
    }

    private void initComponents() {
        scrollPane = new javax.swing.JScrollPane();
        textPane = new javax.swing.JTextPane();
        setLayout(new java.awt.BorderLayout());
        scrollPane.setViewportView(textPane);
        add(scrollPane, java.awt.BorderLayout.CENTER);
    }

    private javax.swing.JScrollPane scrollPane;

    private javax.swing.JTextPane textPane;

    /**
     * Only gets file location for a file to be opened. Afterwards you have
     * to use @see openFileFromStream to read the file
     * should be used for internal use when you dont need a custom dialog
     * for opening file.
     * @param p_workingDirectory
     * @return
     */
    public java.io.File getFileLocation(java.io.File p_workingDirectory) {
        return net.hussnain.io.Utilities.openFileDialog(this, p_workingDirectory, javax.swing.JFileChooser.OPEN_DIALOG);
    }

    /**
     * good for external use
     * @param openAsText if true any file in html or rtf will be opened as text
     * @param p_url
     * @param p_workingDirectory
     */
    public void openFileFromStream(java.io.File p_file, boolean openAsText) {
        Document doc = null;
        EditorKit kit = null;
        boolean extentionOkay = false;
        String extension = "txt";
        if ((p_file != null) && p_file.isFile()) {
            try {
                java.net.URL url = p_file.toURI().toURL();
                if (!openAsText) extension = net.hussnain.io.Utilities.parseExtension(url.getPath());
                if (extension.equals("rtf")) {
                    kit = new RTFEditorKit();
                    doc = new RabtStyledDocument(styleContext);
                    extentionOkay = true;
                } else {
                    if (extension.equals("txt")) {
                        kit = new StyledEditorKit();
                        doc = new RabtStyledDocument(styleContext);
                        extentionOkay = true;
                    } else {
                        if (extension.equals("htm") || extension.equals("html")) {
                            doc = new RabtHTMLDocument(styleSheet);
                            kit = new HTMLEditorKit();
                            extentionOkay = true;
                            ((HTMLDocument) doc).setParser(getParserHTML());
                        }
                    }
                }
                if (extentionOkay) {
                    InputStream input = url.openStream();
                    textPane.setEditorKit(kit);
                    kit.read(input, doc, 0);
                    textPane.setDocument(doc);
                    input.close();
                } else textPane.setPage(url);
                textPane.getDocument().addUndoableEditListener(this);
            } catch (java.io.IOException exp) {
                logger.warning("couldnt open file at " + p_file.toString());
            } catch (javax.swing.text.BadLocationException exp) {
                logger.warning("couldnt open file at " + p_file.toString());
            }
        }
    }

    /**
     *TODO move to another location
     * the returned file will be closed.
     */
    public static void saveFile(java.io.File p_file, JTextPane textPane) {
        java.io.File file = p_file;
        if (file != null) {
            try {
                java.io.OutputStream fos = new java.io.BufferedOutputStream(new java.io.FileOutputStream(file));
                textPane.getEditorKit().write(fos, textPane.getDocument(), 0, textPane.getDocument().getLength());
                fos.flush();
                fos.close();
                logger.info("file saved to " + file.toString());
            } catch (javax.swing.text.BadLocationException exp) {
                logger.warning("couldnt write file at " + file.toString());
            } catch (java.io.IOException exp) {
                logger.warning("couldnt write file at " + file.toString());
            }
        }
    }

    /**
     * TODO simplify
     * Saves the current document under the given name in the file chooser.
     * File chooser will show the content of the defined working direcory initially.
     * File is saved with @see saveFile
     * @param initial directory for the file chooser
     * @return
     */
    public java.io.File saveFileAs(java.io.File p_workingDirectory, String p_addTitle) {
        java.io.File file;
        javax.swing.JFileChooser fd = new javax.swing.JFileChooser();
        fd.setCurrentDirectory(p_workingDirectory);
        fd.setDialogTitle(p_addTitle);
        net.hussnain.io.RabtFileFilter filter = new net.hussnain.io.RabtFileFilter("Accepted Documents");
        filter.addFileExtention(getDocFormat().toString(), getDocDescription());
        fd.addChoosableFileFilter(filter);
        net.hussnain.utils.UIUtil.setLocationToMid(fd);
        while (true) {
            int action = fd.showSaveDialog(null);
            file = fd.getSelectedFile();
            if (file == null) {
                if (action == fd.APPROVE_OPTION) JOptionPane.showMessageDialog(null, "There is a problem with the target file, please try again later.", "Saving File", JOptionPane.WARNING_MESSAGE);
                break;
            }
            file = Utilities.assignFileExtension(file, getDocFormat().toString());
            if (!(action == fd.APPROVE_OPTION)) {
                break;
            } else {
                if (!file.exists()) {
                    saveFile(file, textPane);
                    logger.info("file saved to " + file.getAbsolutePath());
                    break;
                } else {
                    int value = JOptionPane.showConfirmDialog(this, "Another file with the same name already exists.\nDo you want to overwrite this existing one?", "Saveing the File", JOptionPane.YES_NO_OPTION);
                    if (value == JOptionPane.YES_OPTION) {
                        saveFile(file, textPane);
                        logger.info("file saved to " + file.getAbsolutePath());
                        break;
                    }
                }
            }
        }
        return file;
    }

    /**
     * Exports the current content to the path p_file and save the file
     * in format p_tagetFormat
     */
    public java.io.File exportFile(java.io.File p_file, FileFormat p_tagetFormat) {
        logger.info("starting export now ..");
        return Converter.exportFile(p_file, p_tagetFormat, textPane);
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JTextPane getTextPane() {
        return textPane;
    }

    public void setBold(ActionEvent p_evt) {
        currAction = getActionByName("font-bold");
        currAction.actionPerformed(p_evt);
    }

    public void setItalic(ActionEvent p_evt) {
        currAction = getActionByName("font-italic");
        currAction.actionPerformed(p_evt);
    }

    public void setUnderline(ActionEvent p_evt) {
        currAction = getActionByName("font-underline");
        currAction.actionPerformed(p_evt);
    }

    public static void changeFont(JEditorPane p_editor, String p_fontName) {
        logger.info("changing the font to " + p_fontName);
        if (p_editor != null) {
            DefaultStyledDocument doc = (DefaultStyledDocument) p_editor.getDocument();
            int start = p_editor.getSelectionStart();
            int end = p_editor.getSelectionEnd();
            StyledEditorKit kit = (StyledEditorKit) p_editor.getEditorKit();
            if (start != end) {
                SimpleAttributeSet sty = new SimpleAttributeSet();
                StyleConstants.setFontFamily(sty, p_fontName);
                doc.setCharacterAttributes(start, end - start, sty, false);
                StyleConstants.setBidiLevel(sty, 10);
            }
            StyleConstants.setFontFamily(kit.getInputAttributes(), p_fontName);
        }
    }

    public static void changeFontSize(JEditorPane p_editor, int p_fontSize) {
        logger.info("changing the font size to " + p_fontSize);
        if (p_editor != null) {
            int start = p_editor.getSelectionStart();
            int end = p_editor.getSelectionEnd();
            StyledEditorKit kit = (StyledEditorKit) p_editor.getEditorKit();
            if (start != end) {
                DefaultStyledDocument doc = (DefaultStyledDocument) p_editor.getDocument();
                SimpleAttributeSet sty = new SimpleAttributeSet();
                StyleConstants.setFontSize(sty, p_fontSize);
                doc.setCharacterAttributes(start, end - start, sty, false);
            }
            StyleConstants.setFontSize(kit.getInputAttributes(), p_fontSize);
        }
    }

    public static void changeFontSize(int p_fontSize, ActionEvent p_evt) {
        String actionName = "font-size-" + p_fontSize;
        Action action = getActionByName(actionName);
        if (action != null) {
            logger.info(">>> performing action " + actionName);
            action.actionPerformed(p_evt);
        } else {
            logger.info(">>> creating  action " + actionName);
            action = new StyledEditorKit.FontSizeAction(actionName, p_fontSize);
            action.actionPerformed(p_evt);
            putActionByName(actionName, action);
        }
    }

    public void setAlignmentRight(ActionEvent p_evt) {
        currAction = getActionByName("right-justify");
        currAction.actionPerformed(p_evt);
    }

    public void setAlignmentCenter(ActionEvent p_evt) {
        currAction = getActionByName("center-justify");
        currAction.actionPerformed(p_evt);
    }

    public void setAlignmentLeft(ActionEvent p_evt) {
        currAction = getActionByName("left-justify");
        currAction.actionPerformed(p_evt);
    }

    /**
     * TODO move
     * image inserted in a rtf or txt file will be ignored for saving....
     */
    public static void insertImage(JEditorPane p_editor, String p_imagePath) {
        logger.info("inserting image " + p_imagePath);
        Document doc = p_editor.getDocument();
        if (p_editor != null) {
            int end = p_editor.getSelectionEnd();
            SimpleAttributeSet sty = new SimpleAttributeSet();
            try {
                if (doc instanceof HTMLDocument) {
                    HTMLDocument htmdoc = (HTMLDocument) doc;
                    URL imgUrl = new File(p_imagePath).toURI().toURL();
                    String link = "<img src=\"" + imgUrl.toString() + "\" alt=\"" + imgUrl.toString() + "\"></img>";
                    htmdoc.insertAfterEnd(htmdoc.getCharacterElement(end), link);
                } else {
                    StyleConstants.setIcon(sty, new ImageIcon(p_imagePath));
                    doc.insertString(end, " ", sty);
                }
            } catch (BadLocationException exp) {
                logger.warning("image location is not correct at " + p_imagePath);
            } catch (java.io.IOException exp) {
                logger.warning("problem inserting image at " + p_imagePath);
            }
        }
    }

    /**
     * Returns the current document format from the text panel.
     */
    public FileFormat getDocFormat() {
        FileFormat type = FileFormat.text;
        String cType = textPane.getEditorKit().getContentType();
        if (cType.equals(ContentType.html.toString())) {
            type = FileFormat.html;
        } else {
            if (cType.equals(ContentType.rtf.toString())) {
                type = FileFormat.rtf;
            }
        }
        return type;
    }

    /**
     * Returns description for current document type from the text panel
     * similar to @see getDocType.
     */
    public String getDocDescription() {
        String desc = "plain text file";
        String cType = textPane.getEditorKit().getContentType();
        if (cType.equals(ContentType.html.toString())) {
            desc = "html file";
        } else {
            if (cType.equals(ContentType.rtf.toString())) {
                desc = "richt text file";
            }
        }
        return desc;
    }

    public void cutSelection(ActionEvent p_evt) {
        currAction = getActionByName("cut-to-clipboard");
        currAction.actionPerformed(p_evt);
    }

    public void copySelection(ActionEvent p_evt) {
        currAction = getActionByName("copy-to-clipboard");
        currAction.actionPerformed(p_evt);
    }

    public void pasteSelection(ActionEvent p_evt) {
        currAction = getActionByName("paste-from-clipboard");
        currAction.actionPerformed(p_evt);
    }

    public void selectAll(ActionEvent p_evt) {
        currAction = getActionByName(DefaultEditorKit.selectAllAction);
        currAction.actionPerformed(p_evt);
    }

    public void undo(ActionEvent p_evt) {
        undoAction.actionPerformed(p_evt);
    }

    public void redo(ActionEvent p_evt) {
        redoAction.actionPerformed(p_evt);
    }

    private static synchronized void createActionTable(JTextPane p_textPane) {
        if (!actionsCreated) {
            actions = new Hashtable<String, Action>();
            Action[] actionsArray = p_textPane.getActions();
            Action a;
            for (int i = 0; i < actionsArray.length; i++) {
                a = actionsArray[i];
                actions.put(a.getValue(Action.NAME).toString(), a);
            }
        }
    }

    public static Action getActionByName(String p_name) {
        return (Action) (actions.get(p_name));
    }

    public static synchronized void putActionByName(String p_name, Action a) {
        actions.put(p_name, a);
    }

    public static void heighlightText(JTextComponent textComponent, int startPosition, int endPosition) {
        textComponent.setCaretPosition(startPosition);
        textComponent.moveCaretPosition(endPosition);
        textComponent.getCaret().setSelectionVisible(true);
    }

    public static void heighlightText(JTextComponent textComponent, Element el) {
        heighlightText(textComponent, el.getStartOffset(), el.getEndOffset());
    }

    public void heighlightText(Element el) {
        heighlightText(getTextPane(), el.getStartOffset(), el.getEndOffset());
    }

    public void heighlightText(int startPosition, int endPosition) {
        heighlightText(getTextPane(), startPosition, endPosition);
    }

    /** An undoable edit happened
     *
     */
    public void undoableEditHappened(UndoableEditEvent e) {
        undoMng.addEdit(e.getEdit());
        undoAction.updateUndoState();
        redoAction.updateRedoState();
    }

    public class UndoAction extends javax.swing.AbstractAction {

        public UndoAction() {
            super("Undo");
            setEnabled(false);
        }

        /** Invoked when an action occurs.
	 *
	 */
        public void actionPerformed(ActionEvent e) {
            try {
                undoMng.undo();
            } catch (CannotUndoException ex) {
                System.out.println("Unable to undo: " + ex);
                ex.printStackTrace();
            }
            updateUndoState();
            redoAction.updateRedoState();
        }

        protected void updateUndoState() {
            if (undoMng.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undoMng.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }

    public class RedoAction extends javax.swing.AbstractAction {

        public RedoAction() {
            super("Redo");
            setEnabled(false);
        }

        /** Invoked when an action occurs.
	 *
	 */
        public void actionPerformed(ActionEvent e) {
            try {
                undoMng.redo();
            } catch (CannotRedoException ex) {
                System.out.println("Unable to redo: " + ex);
                ex.printStackTrace();
            }
            updateRedoState();
            undoAction.updateUndoState();
        }

        protected void updateRedoState() {
            if (undoMng.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undoMng.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }

    public static TreeMap<String, Font> getFontTable() {
        return fontTable;
    }

    public static void setFontTable(TreeMap<String, Font> aFontTable) {
        fontTable = aFontTable;
        try {
            ((RabtStyleSheet) styleSheet).setFontTable(aFontTable);
            ((RabtStyleContext) styleContext).setFontTable(aFontTable);
        } catch (ClassCastException exp) {
            logger.severe("Could not set font table.");
            logger.severe(exp.toString());
        }
    }

    public static Parser getParserHTML() {
        return parserHTML;
    }

    public static void setParserHTML(Parser aParserHTML) {
        parserHTML = aParserHTML;
    }

    /**
     * Similiar to to method in SimpleAttributeSet, extened to print in html.
     *
     */
    public static String convertAttributes(AttributeSet attributes, boolean html) {
        String separator = " ";
        String s = "";
        java.util.Enumeration names = attributes.getAttributeNames();
        if (html) separator = "<br>";
        while (names.hasMoreElements()) {
            Object key = names.nextElement();
            Object value = attributes.getAttribute(key);
            if (value instanceof AttributeSet) {
                s = s + key + "=**AttributeSet** ";
            } else {
                s = s + key + "=" + value + separator;
            }
        }
        return s;
    }

    public static enum FileFormat {

        text, html, rtf, pdf, xml
    }

    public static enum ContentType {

        text, html, rtf;

        @Override
        public String toString() {
            return toString(this);
        }

        public static String toString(ContentType type) {
            String ret = "?";
            switch(type) {
                case text:
                    ret = "text/plain";
                    break;
                case html:
                    ret = "text/html";
                    break;
                case rtf:
                    ret = "text/rtf";
                    break;
            }
            return ret;
        }
    }
}
