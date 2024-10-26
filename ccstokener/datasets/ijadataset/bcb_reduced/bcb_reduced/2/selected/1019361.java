package com.robrohan.editorkit;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.xml.parsers.SAXParserFactory;
import org.syntax.jedit.DefaultInputHandler;
import org.syntax.jedit.InputHandler;
import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.SyntaxDocument;
import org.syntax.jedit.SyntaxStyle;
import org.syntax.jedit.tokenmarker.JavaTokenMarker;
import org.syntax.jedit.tokenmarker.Token;
import org.syntax.jedit.tokenmarker.XMLTokenMarker;
import org.syntax.jedit.tokenmarker.XQueryTokenMarker;
import org.syntax.jedit.tokenmarker.XSLTokenMarker;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import com.robrohan.fangorn.EntFileChooser;
import com.robrohan.fangorn.IFileLoadable;
import com.robrohan.fangorn.LoadFile;
import com.robrohan.fangorn.Settings;
import com.robrohan.tools.AshpoolDB;
import com.robrohan.tools.Globals;
import com.robrohan.tools.ImageManager;

/**
 * this is the main component for entering text (xml or xslt)
 * @author  rob
 */
public class XMLEditorPane extends JEditTextArea implements UndoableEditListener, IFileLoadable {

    /** the right click menu (context menu) */
    protected JPopupMenu rmenu;

    /** handle to the file loader (used to get files from the web or disk) */
    protected LoadFile fileLoader;

    /** the file chooser */
    protected EntFileChooser fileChooser;

    /** for pane undos */
    private UndoManager undo = new UndoManager();

    /** the file this pane has loaded */
    private File currentFile;

    /** the current type */
    private String mimetype;

    /** any validation errors will be bubbled up to the container */
    private String valErrors = "";

    /** flag to say if the xml file is in the pane or just referenced on disk */
    private boolean fileReferenced = false;

    /** encoding type for this pane */
    private String encodingtype = "UTF-8";

    /** pointer to this pane (used in inner classes) */
    private final XMLEditorPane panePtr;

    /** the analyze timer */
    private Timer timer;

    /** if this is true, analyze etc is turned off */
    private boolean viewonly = false;

    /** the insight window */
    private InsightWindow insight;

    /** used to lookup insight items */
    private static Map namespaceToURI = null;

    /** the added tags so far */
    private Set usedTags;

    /** the added attributes */
    private Set usedAttr;

    /** little line number shower */
    private JLabel status;

    /** handle to the frame - used with the insight window */
    private JFrame roothndl;

    /** time between analyzes */
    private int analyzetime = 3000;

    protected String begin_comment = "<!-- ";

    protected String end_comment = " -->";

    /** 
	 * Creates a new instance of XMLEditorPane
	 * 
	 * @param type The type of syntax highlighting to use (what kind of document this is)
	 */
    public XMLEditorPane(JFrame roothndl, String type) {
        panePtr = this;
        this.roothndl = roothndl;
        createNewDocument(type);
        createMenusAndKeys(type);
    }

    /**
	 * Create all the menus and keybindings for this mime type
	 *  
	 * @param type
	 */
    public void createMenusAndKeys(String type) {
        setMimeType(type);
        if (type.equals("text/xsl") || type.equals("text/xml") || type.equals("text/java") || type.equals("text/xquery")) {
            if (insight == null) insight = new InsightWindow(roothndl, this);
        } else {
            viewonly = true;
        }
        rmenu = new JPopupMenu();
        JMenuItem tmp;
        rmenu.add(new XMLEditorPane.CutAction());
        rmenu.add(new XMLEditorPane.CopyAction());
        rmenu.add(new XMLEditorPane.PasteAction());
        if (Globals.isMacOSX()) {
            getInputHandler().addKeyBinding("M+C", DefaultInputHandler.CLIP_COPY);
            getInputHandler().addKeyBinding("M+V", DefaultInputHandler.CLIP_PASTE);
            getInputHandler().addKeyBinding("M+X", DefaultInputHandler.CLIP_CUT);
        } else {
            getInputHandler().addKeyBinding("C+C", DefaultInputHandler.CLIP_COPY);
            getInputHandler().addKeyBinding("C+V", DefaultInputHandler.CLIP_PASTE);
            getInputHandler().addKeyBinding("C+X", DefaultInputHandler.CLIP_CUT);
        }
        if (!viewonly) {
            if (type.equals("text/xsl")) {
                JMenu jmu = new JMenu(Globals.getMenuLabel("pasteas", Globals.WINDOW_BUNDLE));
                jmu.add(new XMLEditorPane.PasteAsVariable());
                jmu.add(new XMLEditorPane.PasteAsValueOfV());
                jmu.add(new XMLEditorPane.PasteAsValueOfP());
                rmenu.add(jmu);
                jmu = new JMenu(Globals.getMenuLabel("quick", Globals.WINDOW_BUNDLE));
                jmu.add(new XMLEditorPane.QuickValue0f());
                jmu.add(new XMLEditorPane.QuickCopy0f());
                jmu.add(new XMLEditorPane.QuickMessage());
                jmu.add(new XMLEditorPane.QuickBreak());
                rmenu.add(jmu);
            }
            rmenu.add(new JSeparator());
            ActionListener ACT_UNDO = new XMLEditorPane.UndoAction();
            ActionListener ACT_REDO = new XMLEditorPane.RedoAction();
            rmenu.add((AbstractAction) ACT_UNDO);
            rmenu.add((AbstractAction) ACT_REDO);
            if (Globals.isMacOSX()) {
                getInputHandler().addKeyBinding("M+Z", ACT_UNDO);
                getInputHandler().addKeyBinding("M+Y", ACT_REDO);
            } else {
                getInputHandler().addKeyBinding("C+Z", ACT_UNDO);
                getInputHandler().addKeyBinding("C+Y", ACT_REDO);
            }
            rmenu.add(new JSeparator());
            ActionListener ACT_CMMT = new XMLEditorPane.CommentSection();
            ActionListener ACT_GOTL = new XMLEditorPane.GoToLine();
            rmenu.add((AbstractAction) ACT_CMMT);
            rmenu.add((AbstractAction) ACT_GOTL);
            getInputHandler().addKeyBinding("CS+c", ACT_CMMT);
            getInputHandler().addKeyBinding("C+G", ACT_GOTL);
            rmenu.add(Globals.getScriptsMenu("scripts" + "/" + type.replace('/', '_')));
        }
        rmenu.add(new JSeparator());
        JMenu enctypes = new JMenu(Globals.getMenuLabel("encoding", Globals.WINDOW_BUNDLE));
        ArrayList encs = (ArrayList) Globals.getEncodings();
        Iterator itr = encs.iterator();
        while (itr.hasNext()) {
            final String enc = (String) itr.next();
            tmp = new JMenuItem(enc);
            enctypes.add(tmp);
            tmp.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ev) {
                    setEncoding(enc);
                }
            });
        }
        rmenu.add(enctypes);
        tmp = new JMenuItem(Globals.getMenuLabel("save", Globals.WINDOW_BUNDLE));
        rmenu.add(tmp).setAccelerator(KeyStroke.getKeyStroke(Globals.getOSKeyStroke("save")));
        ActionListener ACT_XSAVE = new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                save();
            }
        };
        tmp.addActionListener(ACT_XSAVE);
        if (Globals.isMacOSX()) {
            getInputHandler().addKeyBinding("M+S", ACT_XSAVE);
        } else {
            getInputHandler().addKeyBinding("C+S", ACT_XSAVE);
        }
        rmenu.add(new JSeparator());
        rmenu.add(new XMLEditorPane.FileProperties());
        if (type.equals("text/xsl") || type.equals("text/xml")) {
            ActionListener ACT_VALIDATE = new XMLEditorPane.ValidateAction();
            rmenu.add((AbstractAction) ACT_VALIDATE);
            getInputHandler().addKeyBinding("CS+V", ACT_VALIDATE);
        }
        rmenu.add(new javax.swing.JSeparator());
        rmenu.add(new XMLEditorPane.LoadFileAction());
        setRightClickPopup(rmenu);
        if (fileChooser == null) {
            fileChooser = (EntFileChooser) Globals.getWindow("File Chooser");
        }
        getDocument().addUndoableEditListener(this);
        this.putClientProperty("InputHandler.homeEnd", Boolean.TRUE);
        ActionListener ACT_DWI = new XMLEditorPane.DownWithInsight();
        getInputHandler().addKeyBinding("DOWN", ACT_DWI);
        getInputHandler().addKeyBinding("C+n", ACT_DWI);
        ActionListener ACT_UWI = new XMLEditorPane.UpWithInsight();
        getInputHandler().addKeyBinding("UP", ACT_UWI);
        getInputHandler().addKeyBinding("C+p", ACT_UWI);
        ActionListener ACT_ENTER = new XMLEditorPane.EnterWithInsight();
        getInputHandler().addKeyBinding("ENTER", ACT_ENTER);
        ActionListener ACT_TAGINSIGHT = new XMLEditorPane.TagInsight();
        getInputHandler().addKeyBinding("C+t", ACT_TAGINSIGHT);
        ActionListener ACT_ATTINSIGHT = new XMLEditorPane.AttrInsight();
        getInputHandler().addKeyBinding("C+u", ACT_ATTINSIGHT);
        ActionListener ACT_LEFT = new XMLEditorPane.LeftWithLineNumbers();
        getInputHandler().addKeyBinding("C+b", ACT_LEFT);
        getInputHandler().addKeyBinding("LEFT", ACT_LEFT);
        ActionListener ACT_RIGHT = new XMLEditorPane.RightWithLineNumbers();
        getInputHandler().addKeyBinding("C+f", ACT_RIGHT);
        getInputHandler().addKeyBinding("RIGHT", ACT_RIGHT);
        addFocusListener(new FocusHandler());
    }

    public void setEncoding(String to) {
        encodingtype = to;
    }

    public void setFileChooser(EntFileChooser efc) {
        fileChooser = efc;
    }

    public void setFileLoader(LoadFile lf) {
        fileLoader = lf;
    }

    /** Creates a new document and sets a syntax highlighting type
	 * @param contenttype the type of document to create
	 */
    public void createNewDocument(String contenttype) {
        this.createNewDocument(contenttype, "");
    }

    /** 
	 * Creates a new document, sets a syntax highlighting type, and adds default text
	 * This mostly just sets up the color and token marker, and starts up some timers
	 * in xml mode for the given content type
	 * 
	 * @param contenttype the type of document to create
	 * @param defaultText the default text for this document
	 */
    public void createNewDocument(String contenttype, String defaultText) {
        SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];
        setDocument(new SyntaxDocument());
        int dbtype = 0;
        if (contenttype.equals("text/xsl")) {
            setTokenMarker(new XSLTokenMarker());
            dbtype = 1;
            try {
                ResultSet spad = AshpoolDB.executeQuery("select element from elements order by element;");
                while (spad.next()) {
                    this.addKeyword(spad.getString("element"), Token.KEYWORD1);
                }
                spad = AshpoolDB.executeQuery("select distinct parameter from parameters order by parameter;");
                while (spad.next()) {
                    this.addKeyword(spad.getString("parameter"), Token.KEYWORD2);
                }
            } catch (Exception e) {
                System.err.println("Syntax highlighting library blew up " + e.toString());
            }
            ActionListener ACT_I = new XMLEditorPane.ViewInsight();
            getInputHandler().addKeyBinding("C+i", ACT_I);
            begin_comment = "<!-- ";
            end_comment = " -->";
        } else if (contenttype.equals("text/xml") || contenttype.equals("text/svg")) {
            setTokenMarker(new XMLTokenMarker());
            ActionListener ACT_I = new XMLEditorPane.ViewInsight();
            getInputHandler().addKeyBinding("C+i", ACT_I);
            dbtype = 2;
            begin_comment = "<!-- ";
            end_comment = " -->";
        } else if (contenttype.equals("text/java")) {
            setTokenMarker(new JavaTokenMarker());
            dbtype = 3;
            begin_comment = "/* ";
            end_comment = " */";
        } else if (contenttype.equals("text/xquery")) {
            setTokenMarker(new XQueryTokenMarker());
            dbtype = 4;
            begin_comment = "(: ";
            end_comment = " :)";
        }
        if (dbtype != 0) {
            try {
                ResultSet stylemaster = AshpoolDB.executeQuery("select id, name, invalidlines, linehighlight, eolmarkers " + " from editorschemes where id = " + dbtype + ";");
                stylemaster.next();
                getPainter().setInvalidLinesPainted(stylemaster.getBoolean("invalidlines"));
                getPainter().setLineHighlightEnabled(stylemaster.getBoolean("linehighlight"));
                getPainter().setEOLMarkersPainted(stylemaster.getBoolean("eolmarkers"));
                ResultSet styleelements = AshpoolDB.executeQuery("select * from editorelements where schemeid = " + stylemaster.getString("id") + " order by elementname asc;");
                int sc = 1;
                while (styleelements.next()) {
                    styles[sc] = new SyntaxStyle(new Color(styleelements.getInt("e"), styleelements.getInt("g"), styleelements.getInt("b")), styleelements.getBoolean("italic"), styleelements.getBoolean("bold"));
                    sc++;
                }
                getPainter().setStyles(styles);
                getPainter().setLineHighlightColor(new Color(181, 213, 255));
            } catch (Exception e) {
                System.err.println("Style Loader blew up " + e.toString());
            }
            if (dbtype == 1) {
                if (timer == null) {
                    timer = new Timer(analyzetime, new ActionListener() {

                        public void actionPerformed(ActionEvent evt) {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    doAnalyze();
                                }
                            });
                        }
                    });
                    timer.start();
                }
            } else {
                if (timer != null) {
                    timer.stop();
                    timer = null;
                }
            }
        } else {
            setTokenMarker(new JavaTokenMarker());
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        }
        getPainter().setEOLMarkerColor(new Color(0x007265));
        setText(defaultText);
        currentFile = null;
        undo.discardAllEdits();
        getDocument().addUndoableEditListener(this);
        setCaretPosition(0);
        setEnabled(true);
        fileReferenced = false;
        if (dbtype == 1 || (dbtype == 2 && !contenttype.equals("text/svg"))) {
            usedTags = new HashSet();
            usedAttr = new HashSet();
            doAnalyze();
        }
    }

    /**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 *
	 * this is overloaded to pass in word fragments to the
	 * insight window
	 */
    private String limiter = "";

    public void processKeyEvent(KeyEvent evt) {
        if (inputHandler == null) return;
        switch(evt.getID()) {
            case KeyEvent.KEY_TYPED:
                if (insight != null && insight.isVisible()) {
                    insight.calculateLocation();
                    limiter = limiter.trim();
                    switch(evt.getKeyChar()) {
                        case '\b':
                            if (limiter.length() > 0) {
                                limiter = limiter.substring(0, limiter.length() - 1);
                            } else {
                                insight.setVisible(false);
                            }
                            break;
                        default:
                            if (evt.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                                limiter += evt.getKeyChar();
                            }
                            break;
                    }
                    insight.limitSet(limiter);
                } else {
                    limiter = "";
                }
                inputHandler.keyTyped(evt);
                break;
            case KeyEvent.KEY_PRESSED:
                inputHandler.keyPressed(evt);
                break;
            case KeyEvent.KEY_RELEASED:
                inputHandler.keyReleased(evt);
                break;
        }
    }

    public void xsltInsight() {
        doAnalyze();
        String linetext = "";
        if (insight == null) insight = new InsightWindow(roothndl, this);
        try {
            insight.setControler(this);
            linetext = getLineText(getCaretLine());
            org.apache.regexp.RE namespace = new org.apache.regexp.RE("[\\<][a-zA-Z0-9_]*[\\:]");
            org.apache.regexp.RE element = new org.apache.regexp.RE("([\\<][a-zA-Z0-9_]*[\\:])([a-zA-Z0-9_\\-]+[ ]+)");
            namespace.match(linetext);
            element.match(linetext);
            insight.calculateLocation();
            String nsreq = "";
            if (namespace.getParen(0) != null) {
                nsreq = namespace.getParen(0).toString().trim();
                nsreq = nsreq.substring(1, (nsreq.length() - 1));
            }
            if (namespaceToURI != null && namespaceToURI.get(nsreq) != null) {
                if (namespace.getParen(0) != null && element.getParen(0) == null) {
                    insight.useLibrary(namespaceToURI.get(nsreq).toString(), InsightWindow.ELEMENT);
                    insight.setVisible(true);
                } else if (element.getParen(0) != null && element.getParen(2) != null) {
                    insight.useLibrary(element.getParen(2).toString().trim(), InsightWindow.PARAM);
                    insight.setVisible(true);
                } else {
                    insight.setVisible(false);
                }
            }
        } catch (Exception e) {
            System.err.println("Can't find namespace? " + e.toString());
            e.printStackTrace(System.err);
        }
    }

    /** show insight using the documents tags */
    public void tagInsight() {
        if (insight == null) insight = new InsightWindow(roothndl, this);
        insight.setControler(this);
        doAnalyze();
        insight.setInsightItems(usedTags);
        insight.calculateLocation();
        insight.setVisible(true);
    }

    /** show insight using the documents attributes */
    public void attrInsight() {
        if (insight == null) insight = new InsightWindow(roothndl, this);
        insight.setControler(this);
        doAnalyze();
        insight.setInsightItems(usedAttr);
        insight.calculateLocation();
        insight.setVisible(true);
    }

    /** Set this panes text from an input stream
	 * @param iso the stream that has the text to load
	 */
    public void setText(java.io.InputStream iso) {
        StringBuffer sb = new StringBuffer();
        try {
            java.io.BufferedReader bufr = new java.io.BufferedReader(new java.io.InputStreamReader(iso));
            String middle = "";
            while ((middle = bufr.readLine()) != null) {
                sb.append(middle + "\n");
            }
            bufr.close();
            iso.close();
            middle = null;
        } catch (Exception e) {
            System.err.println("EditorPane::setText(iso): " + e);
        }
        setText(sb.toString());
        sb = null;
    }

    /** gets the current file
	 * @return the current file object
	 */
    public java.io.File getCurrentFile() {
        return currentFile;
    }

    /** Adds an undo event to the undo tracker
	 * @param a the event that happened
	 */
    public void undoableEditHappened(javax.swing.event.UndoableEditEvent a) {
        undo.addEdit(a.getEdit());
    }

    /** Gets a handle to the undo action to envoke undos from outside the class
	 * @return the undo Action object
	 */
    public Action getUndoAction() {
        return new XMLEditorPane.UndoAction();
    }

    /** Gets a handle to the redo action to envoke redos from outside the class
	 * @return the undo Action object
	 */
    public Action getRedoAction() {
        return new XMLEditorPane.RedoAction();
    }

    class CutAction extends AbstractAction {

        public CutAction() {
            super(Globals.getMenuLabel("cut", Globals.WINDOW_BUNDLE), ImageManager.getImage("fan.20.cut"));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(Globals.getOSKeyStroke("cut")));
        }

        public void actionPerformed(ActionEvent e) {
            cut();
        }
    }

    class CopyAction extends AbstractAction {

        public CopyAction() {
            super(Globals.getMenuLabel("copy", Globals.WINDOW_BUNDLE), ImageManager.getImage("fan.20.copy"));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(Globals.getOSKeyStroke("copy")));
        }

        public void actionPerformed(ActionEvent e) {
            copy();
        }
    }

    class PasteAction extends AbstractAction {

        public PasteAction() {
            super(Globals.getMenuLabel("paste", Globals.WINDOW_BUNDLE), ImageManager.getImage("fan.20.paste"));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(Globals.getOSKeyStroke("paste")));
        }

        public void actionPerformed(ActionEvent e) {
            paste();
        }
    }

    class FocusHandler implements FocusListener {

        public void focusGained(FocusEvent evt) {
        }

        public void focusLost(FocusEvent evt) {
            if (insight != null && insight.isVisible() && !insight.hasFocus()) {
            }
        }
    }

    class ViewInsight extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            xsltInsight();
        }
    }

    class TagInsight extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            tagInsight();
        }
    }

    class AttrInsight extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            attrInsight();
        }
    }

    /** flag to tell if insight just closed - helpful with "enter" */
    boolean insightJustClosed = false;

    class EnterWithInsight extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            if (insight != null && (insight.isVisible() || insightJustClosed)) {
                insightJustClosed = false;
            } else {
                InputHandler.INSERT_BREAK.actionPerformed(e);
            }
        }
    }

    class UpWithInsight extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            if (insight != null && insight.isVisible()) {
                insight.selectPreviousItem().toString();
            } else {
                InputHandler.PREV_LINE.actionPerformed(e);
            }
        }
    }

    class DownWithInsight extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            if (insight != null && insight.isVisible()) {
                insight.selectNextItem().toString();
            } else {
                InputHandler.NEXT_LINE.actionPerformed(e);
            }
        }
    }

    class LeftWithLineNumbers extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            InputHandler.PREV_CHAR.actionPerformed(e);
        }
    }

    class RightWithLineNumbers extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            InputHandler.NEXT_CHAR.actionPerformed(e);
        }
    }

    class UndoAction extends AbstractAction {

        public UndoAction() {
            super(Globals.getMenuLabel("undo", Globals.WINDOW_BUNDLE), ImageManager.getImage("fan.20.undo"));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(Globals.getOSKeyStroke("undo")));
            putValue(Action.SHORT_DESCRIPTION, Globals.getTooltipLabel("undo", Globals.WINDOW_BUNDLE));
        }

        public void actionPerformed(ActionEvent evt) {
            try {
                if (undo.canUndo()) undo.undo();
            } catch (javax.swing.undo.CannotUndoException e) {
                System.err.println("XMLEditorPane::undoAction: " + e);
                e.printStackTrace(System.err);
            }
        }
    }

    class RedoAction extends AbstractAction {

        public RedoAction() {
            super(Globals.getMenuLabel("redo", Globals.WINDOW_BUNDLE), ImageManager.getImage("fan.20.redo"));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(Globals.getOSKeyStroke("redo")));
            putValue(Action.SHORT_DESCRIPTION, Globals.getTooltipLabel("redo", Globals.WINDOW_BUNDLE));
        }

        public void actionPerformed(ActionEvent evt) {
            try {
                if (undo.canRedo()) undo.redo();
            } catch (javax.swing.undo.CannotUndoException e) {
                System.err.println("XMLEditorPane::redoAction: " + e);
                e.printStackTrace(System.err);
            }
        }
    }

    class CommentSection extends AbstractAction {

        public CommentSection() {
            super(Globals.getMenuLabel("comment", Globals.WINDOW_BUNDLE), ImageManager.getImage("fan.20.about"));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift C"));
        }

        public void actionPerformed(ActionEvent a) {
            try {
                String txt = getSelectedText();
                if (txt == null) txt = "";
                setSelectedText(begin_comment + txt + end_comment);
            } catch (Exception e) {
                System.err.println("XMLEditorPane::commentSection: " + e);
            }
        }
    }

    class QuickValue0f extends AbstractAction {

        public QuickValue0f() {
            super("value-of");
        }

        public void actionPerformed(ActionEvent a) {
            try {
                String txt = getSelectedText();
                if (txt == null) txt = "";
                setSelectedText("<xsl:value-of select=\"" + txt + "\" />");
            } catch (Exception e) {
                System.err.println("XMLEditorPane::commentSection: " + e);
            }
        }
    }

    class QuickCopy0f extends AbstractAction {

        public QuickCopy0f() {
            super("copy-of");
        }

        public void actionPerformed(ActionEvent a) {
            try {
                String txt = getSelectedText();
                if (txt == null) txt = "";
                setSelectedText("<xsl:copy-of select=\"" + txt + "\" />");
            } catch (Exception e) {
                System.err.println("XMLEditorPane::commentSection: " + e);
            }
        }
    }

    class QuickMessage extends AbstractAction {

        public QuickMessage() {
            super("message");
        }

        public void actionPerformed(ActionEvent a) {
            try {
                String txt = getSelectedText();
                if (txt == null) txt = "";
                setSelectedText("<xsl:message>" + txt + "</xsl:message>");
            } catch (Exception e) {
                System.err.println("XMLEditorPane::commentSection: " + e);
            }
        }
    }

    class QuickBreak extends AbstractAction {

        public QuickBreak() {
            super("break");
        }

        public void actionPerformed(ActionEvent a) {
            try {
                String txt = getSelectedText();
                if (txt == null) txt = "";
                setSelectedText("<xsl:message terminate=\"yes\">" + txt + "</xsl:message>");
            } catch (Exception e) {
                System.err.println("XMLEditorPane::commentSection: " + e);
            }
        }
    }

    class PasteAsVariable extends AbstractAction {

        public PasteAsVariable() {
            super("Variable");
        }

        public void actionPerformed(ActionEvent e) {
            setSelectedText("$");
            paste();
        }
    }

    class PasteAsValueOfV extends AbstractAction {

        public PasteAsValueOfV() {
            super("Value-of (variable)");
        }

        public void actionPerformed(ActionEvent e) {
            setSelectedText("<xsl:value-of select=\"$");
            paste();
            setSelectedText("\" />");
        }
    }

    class PasteAsValueOfP extends AbstractAction {

        public PasteAsValueOfP() {
            super("Value-of (path)");
        }

        public void actionPerformed(ActionEvent e) {
            setSelectedText("<xsl:value-of select=\"");
            paste();
            setSelectedText("\" />");
        }
    }

    class GoToLine extends AbstractAction {

        public GoToLine() {
            super(Globals.getMenuLabel("goto", Globals.WINDOW_BUNDLE), ImageManager.getImage("fan.20.down"));
            putValue(Action.ACCELERATOR_KEY, javax.swing.KeyStroke.getKeyStroke("control G"));
        }

        public void actionPerformed(ActionEvent ae) {
            String resp = javax.swing.JOptionPane.showInputDialog(panePtr, Globals.getDialogLabel("goto", Globals.WINDOW_BUNDLE));
            if (resp != null && Integer.parseInt(resp) > 0) {
                try {
                    int newline = Integer.parseInt(resp);
                    if (newline > getLineCount()) newline = getLineCount();
                    scrollTo((newline - 1), 0);
                    setCaretPosition(getLineStartOffset((newline - 1)));
                } catch (Exception e) {
                    System.err.println("XMLEditorPane::goToLine: " + e);
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    class FileProperties extends AbstractAction {

        public FileProperties() {
            super(Globals.getMenuLabel("properties", Globals.WINDOW_BUNDLE));
            putValue(AbstractAction.SMALL_ICON, ImageManager.getImage("fan.20.info"));
        }

        public void actionPerformed(ActionEvent ae) {
            String info = "";
            if (currentFile != null) {
                info += "Name: " + currentFile.getName() + "\n";
                info += "Path: " + currentFile.getAbsolutePath() + "\n";
                info += "Writeable: " + currentFile.canWrite() + "\n";
                info += "Still Exists: " + currentFile.exists() + "\n";
                info += "LastModified: " + new java.util.Date(currentFile.lastModified()) + "\n";
            } else {
                info += "File not saved locally.\n";
            }
            if (!isReferenced()) {
                info += "Number of lines: " + (getLineCount() - 1) + "\n";
            } else {
                info += "Referenced Source\n";
            }
            info += "Encoding: " + encodingtype + "\n";
            javax.swing.JOptionPane.showMessageDialog(panePtr, info);
        }
    }

    class LoadFileAction extends AbstractAction {

        public LoadFileAction() {
            super(Globals.getMenuLabel("load", Globals.WINDOW_BUNDLE), ImageManager.getImage("fan.20.diskget"));
            putValue(Action.SHORT_DESCRIPTION, Globals.getTooltipLabel("load", Globals.WINDOW_BUNDLE));
        }

        public void actionPerformed(ActionEvent actionEvent) {
            showOpenFileDialog(Globals.getLabelLabel("loadfile", Globals.WINDOW_BUNDLE));
        }
    }

    class SaveAsFileAction extends AbstractAction {

        public SaveAsFileAction() {
            super(Globals.getMenuLabel("saveas", Globals.WINDOW_BUNDLE), ImageManager.getImage("fan.20.diskput"));
        }

        public void actionPerformed(ActionEvent actionEvent) {
            saveAs();
        }
    }

    class SaveFileAction extends AbstractAction {

        public SaveFileAction() {
            super(Globals.getMenuLabel("save", Globals.WINDOW_BUNDLE), ImageManager.getImage("fan.20.diskput"));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control S"));
            putValue(Action.SHORT_DESCRIPTION, Globals.getTooltipLabel("save", Globals.WINDOW_BUNDLE));
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if (currentFile == null) {
                saveAs();
            } else {
                save();
            }
        }
    }

    class ValidateAction extends AbstractAction {

        public ValidateAction() {
            super(Globals.getMenuLabel("validate", Globals.WINDOW_BUNDLE));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift V"));
        }

        public void actionPerformed(ActionEvent actionEvent) {
            valErrors = "";
            doValidate();
            if (valErrors == "") {
                javax.swing.JOptionPane.showMessageDialog(panePtr, Globals.getDialogLabel("docvalid", Globals.WINDOW_BUNDLE));
            } else {
                javax.swing.JOptionPane.showMessageDialog(panePtr, Globals.getDialogLabel("docinvalid", Globals.WINDOW_BUNDLE));
                tellMessage(valErrors);
            }
        }
    }

    /** Gets a handle to the save action to envoke saves from outside the class
	 * @return the save Action
	 */
    public Action getSaveAction() {
        return new XMLEditorPane.SaveFileAction();
    }

    /** Gets a handle to the saveas action to envoke saveas' from outside the class
	 * @return the saveas Action object
	 */
    public Action getSaveAsAction() {
        return new XMLEditorPane.SaveAsFileAction();
    }

    /** Gets a handle to the load action to envoke loads from outside the class
	 * @return the load Action object
	 */
    public Action getLoadAction() {
        return new XMLEditorPane.LoadFileAction();
    }

    private void tellMessage(String message) {
        System.err.println(message);
    }

    /** envokes the right click menu (context menu)
	 * @param e the mouse event to check for right clicks
	 */
    public void processMouseEvent(MouseEvent e) {
        if (e.isPopupTrigger()) {
            rmenu.show(this, e.getX(), e.getY());
        } else {
            super.processMouseEvent(e);
        }
    }

    /**
	 * top the analyzer from parsing the textArea
	 * (looking for namespaces and such)
	 */
    public void stopAnalyzer() {
        if (timer != null) {
            while (timer.isRunning()) {
                timer.stop();
            }
        }
    }

    /**
	 * kill the analyzer (looking for namespaces and such)
	 */
    public void killAnalyzer() {
        stopAnalyzer();
        timer = null;
    }

    /** Validates this panes contents. This function uses the currently select
	 * xml parser so if it does not do validation, this will not do anything.
	 * It will write any errors to the valErrors String
	 */
    public void doValidate() {
        try {
            XMLReader xmlReader = null;
            xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/validation", true);
            xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
            xmlReader.setErrorHandler(new XMLEditorPane.ErrorHandlerLocal());
            InputSource input = new org.xml.sax.InputSource(new java.io.BufferedInputStream(new ByteArrayInputStream(getText().getBytes(encodingtype))));
            xmlReader.parse(input);
        } catch (Exception e) {
            valErrors += e.toString();
        }
    }

    class ErrorHandlerLocal implements ErrorHandler {

        public void error(SAXParseException sAXParseException) throws SAXException {
            valErrors += "error: " + sAXParseException.toString() + "\n";
        }

        public void fatalError(SAXParseException sAXParseException) throws SAXException {
            valErrors += "fatal: " + sAXParseException.toString() + "\n";
        }

        public void warning(SAXParseException sAXParseException) throws SAXException {
            valErrors += "warning: " + sAXParseException.toString() + "\n";
        }
    }

    /**
	 * parse the textAreas contents and look for namespaces. This will be used
	 * in codecompletion / insite stuff eventually 
	 */
    private XMLEditorPane.AnalyzeParser xep = new XMLEditorPane.AnalyzeParser();

    private XMLEditorPane.AnalyzeHandler hdnl = new XMLEditorPane.AnalyzeHandler();

    private XMLReader xmlReader = null;

    private SAXParserFactory factory = null;

    private InputSource input = null;

    public void doAnalyze() {
        if (System.getProperty("org.xml.sax.driver") == null || System.getProperty("org.xml.sax.driver").equals("")) {
            System.setProperty("org.xml.sax.driver", new Settings().SAXFactory);
        }
        if (getText().length() <= 0 || !(this.getTokenMarker() instanceof XSLTokenMarker || this.getTokenMarker() instanceof XMLTokenMarker)) return;
        try {
            if (xmlReader == null) {
                xmlReader = XMLReaderFactory.createXMLReader();
                xmlReader.setFeature("http://xml.org/sax/features/validation", false);
                xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
                xmlReader.setErrorHandler(hdnl);
                xmlReader.setContentHandler(xep);
            }
            input = new InputSource(new BufferedInputStream(new ByteArrayInputStream(getText().getBytes(encodingtype))));
            Thread t = new Thread() {

                public void run() {
                    try {
                        xmlReader.parse(input);
                    } catch (Exception e) {
                    }
                }
            };
            t.setPriority(Thread.MAX_PRIORITY);
            t.run();
        } catch (Exception e) {
            System.err.println("XMLEditorPane::doAnalyze: " + e.toString());
            e.printStackTrace(System.err);
            this.stopAnalyzer();
        }
    }

    class AnalyzeParser implements ContentHandler {

        public void characters(char[] ch, int start, int length) throws SAXException {
            ;
        }

        public void endDocument() throws SAXException {
            ;
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            ;
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            ;
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            ;
        }

        public void processingInstruction(String target, String data) throws SAXException {
            ;
        }

        public void setDocumentLocator(Locator locator) {
            ;
        }

        public void skippedEntity(String name) throws SAXException {
            ;
        }

        public void startDocument() throws SAXException {
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            usedTags.add(qName);
            for (int x = atts.getLength() - 1; x >= 0; x--) {
                usedAttr.add(atts.getQName(x));
            }
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (prefix.length() > 0 && !isKeywordAdded(prefix)) {
                addKeyword(prefix, org.syntax.jedit.tokenmarker.Token.LABEL);
                if (namespaceToURI == null) namespaceToURI = new java.util.HashMap();
                namespaceToURI.put(prefix, uri);
            }
        }
    }

    class AnalyzeHandler implements ErrorHandler {

        public void error(SAXParseException sAXParseException) throws SAXException {
            ;
        }

        public void fatalError(SAXParseException sAXParseException) throws SAXException {
            ;
        }

        public void warning(SAXParseException sAXParseException) throws SAXException {
            ;
        }
    }

    /** checks to see if a keyword is already listed in the TokenMarker
	 * this is crazy, but it works
	 */
    public boolean isKeywordAdded(String keyword) {
        byte isthere = XSLTokenMarker.getKeywords().lookup(new javax.swing.text.Segment(keyword.toCharArray(), 0, keyword.length()), 0, keyword.length());
        if (isthere == org.syntax.jedit.tokenmarker.Token.NULL) {
            return false;
        } else {
            return true;
        }
    }

    /** add a keyword to the keyword list (in tokenmarker) and set the type 
	 * heavly tied to jedit
	 */
    public void addKeyword(String keyword, byte type) {
        XSLTokenMarker.getKeywords().add(keyword, type);
    }

    /** Envokes the loadFile dialog and then trys to open the file from the
	 * fileLoader dialog box. Can load either a file off disk or from an
	 * http(s) address.
	 * @param title the file loader dialog title (i.e. "select xml document to open")
	 */
    public void showOpenFileDialog(String title) {
        if (fileLoader == null) fileLoader = (LoadFile) Globals.getWindow("Load File");
        fileLoader.setFileName("");
        fileLoader.setTitle(title);
        fileLoader.setDisplayer(this);
        if (fileLoader.isVisible()) {
            fileLoader.toFront();
        } else {
            fileLoader.show();
        }
    }

    public void openFile(String fileURI) {
        boolean ref = fileLoader.isReference();
        if (fileURI != null && !fileURI.equals("") && !ref) {
            try {
                if (fileURI.startsWith("http://") || fileURI.startsWith("https://")) {
                    final String filename = fileURI;
                    Thread w = new Thread() {

                        public void run() {
                            try {
                                setPage(filename);
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                                System.err.println("Couldn't load URL because: " + e.toString());
                            }
                        }
                    };
                    w.start();
                } else {
                    final String filename = fileURI;
                    Thread w = new Thread() {

                        public void run() {
                            loadFile(new java.io.File(filename));
                        }
                    };
                    w.start();
                }
            } catch (Exception e) {
                System.err.println("*** Couldn't load file because: " + e + " ***");
                fileURI = "Error.";
                tellMessage(e.toString());
            }
        } else if (fileURI != null && !fileURI.equals("") && ref) {
            if (fileURI.startsWith("http://") || fileURI.startsWith("https://")) {
            } else {
                createNewDocument("text/xml", "");
                currentFile = new File(fileURI);
                this.add(new JLabel(ImageManager.getImage("fan.20.mime.xml")));
                fileReferenced = true;
                setEnabled(false);
            }
        }
    }

    /** to tell if the file is loaded into this pane, or just referenced.
	 * @return true if the file is referenced
	 */
    public boolean isReferenced() {
        return fileReferenced;
    }

    public void setPage(String URI) {
        try {
            URL url = new URL(URI);
            URLConnection site = url.openConnection();
            BufferedReader bin = new BufferedReader(new InputStreamReader(site.getInputStream()));
            StringBuffer filecontents = new StringBuffer();
            String boxfill;
            while ((boxfill = bin.readLine()) != null) {
                filecontents.append(boxfill + "\n");
            }
            createNewDocument(guessFileType(URI), filecontents.toString());
            setCaretPosition(0);
        } catch (Exception e) {
            System.err.println("Could not load file because: " + e);
            tellMessage(e.toString());
        }
    }

    public void loadFile(File file) {
        try {
            BufferedReader bin = new BufferedReader(new InputStreamReader(new FileInputStream(file), encodingtype));
            StringBuffer filecontents = new StringBuffer();
            String boxfill;
            while ((boxfill = bin.readLine()) != null) {
                filecontents.append(boxfill + "\n");
            }
            createMenusAndKeys(guessFileType(file.getName()));
            createNewDocument(guessFileType(file.getName()), filecontents.toString());
            setCaretPosition(0);
            currentFile = file;
            bin.close();
            boxfill = null;
            filecontents = null;
        } catch (Exception e) {
            System.err.println("Could not load file because: " + e);
            tellMessage(e.toString());
        }
    }

    private String guessFileType(String filename) {
        if (filename.toString().toLowerCase().endsWith(".html") || filename.toString().toLowerCase().endsWith(".htm")) {
            return "text/html";
        } else if (filename.toString().toLowerCase().endsWith(".xsl") || filename.toString().toLowerCase().endsWith(".xslt") || filename.toString().toLowerCase().endsWith(".thx") || filename.toString().toLowerCase().endsWith(".xth") || filename.toString().toLowerCase().endsWith(".fo")) {
            return "text/xsl";
        } else if (filename.toString().toLowerCase().endsWith(".bsh") || filename.toString().toLowerCase().endsWith(".java") || filename.toString().toLowerCase().endsWith(".js")) {
            return "text/java";
        } else if (filename.toString().toLowerCase().endsWith(".xq")) {
            return "text/xquery";
        } else {
            return "text/xml";
        }
    }

    private void save() {
        if (currentFile == null) saveAs(); else saveFile(currentFile);
    }

    /** 
	 * Envokes the saveas dialog to save this panes contents to disk 
	 */
    public void saveAs() {
        if (fileChooser == null) fileChooser = (EntFileChooser) Globals.getWindow("Choose File");
        fileChooser.setName("");
        fileChooser.setDialogTitle(Globals.getMenuLabel("saveas", Globals.WINDOW_BUNDLE));
        fileChooser.setDisplayer(this);
        fileChooser.showSaveDialog();
    }

    /** 
	 * saves the current text area to the passed file, using the encoding
	 * that this area is set for (in String encoding) 
	 */
    public void saveFile(File file) {
        try {
            OutputStreamWriter bout = new OutputStreamWriter(new FileOutputStream(file), encodingtype);
            bout.write(this.getText().toString());
            bout.flush();
            bout.close();
            currentFile = file;
        } catch (Exception e) {
            System.err.println("Could not save file because: " + e);
            e.printStackTrace(System.err);
        }
    }

    /**
	 * @return Returns the mimetype.
	 */
    public String getMimeType() {
        return mimetype;
    }

    /**
	 * @param mimetype The mimetype to set.
	 */
    public void setMimeType(String mimetype) {
        this.mimetype = mimetype;
    }
}
