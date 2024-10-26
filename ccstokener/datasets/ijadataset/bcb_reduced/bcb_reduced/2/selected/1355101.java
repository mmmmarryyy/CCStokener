package com.reactiveplot.programs.editor;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.xml.stream.XMLStreamException;
import com.reactiveplot.library.editor.nodes.Node;
import com.reactiveplot.library.editor.views.NodeList;
import com.reactiveplot.library.events.InterpreterEvent;
import com.reactiveplot.library.interpreter.BadConversationScriptException;
import com.reactiveplot.library.scripts.Script;
import com.reactiveplot.programs.AboutDialog;
import com.reactiveplot.programs.AbstractScriptOpener;
import com.reactiveplot.programs.ReactivePlotUtils;
import com.reactiveplot.programs.ScriptBrowser;
import com.reactiveplot.programs.ScriptPlayer;

public class ScriptEditor extends JFrame implements AbstractScriptOpener, ActionListener, AbstractScriptEditor, EditorClickHandler {

    private static final long serialVersionUID = 2528490746316660930L;

    Script global_currentScript = null;

    private String global_applicationName = "(Incomplete and unstable) Script Editor";

    URL global_dex = null;

    URL global_currentScriptURL = null;

    JDialog global_aboutDialog = null;

    ScriptBrowser global_scriptBrowser = null;

    ScriptPlayer global_scriptPlayer = null;

    StructureViewer global_structureViewer = null;

    TextEditor global_textEditor = null;

    public static void main(String[] args) throws XMLStreamException, IOException, BadConversationScriptException, BadLocationException {
        URL dex = new URL("http://www.reactiveplot.com/scripts/directory.dex");
        ScriptEditor editor = new ScriptEditor(dex, true);
        editor.setVisible(true);
    }

    @Override
    public void openScript(URL url) {
        try {
            URLConnection urlConn = url.openConnection();
            InputStream input = new BufferedInputStream(urlConn.getInputStream());
            global_currentScript = Script.loadFromXML(input);
        } catch (Exception e) {
            ReactivePlotUtils.displayExceptionErrorBox(e, this);
            return;
        }
        String scriptName = global_currentScript.info.getTitle();
        this.setTitle(scriptName + " - " + global_applicationName);
        global_currentScriptURL = url;
        getCurrentScriptMenu().setEnabled(true);
        global_structureViewer.setScript(global_currentScript);
        Node start = global_structureViewer.getCurrentNodeList().getStartNode();
        global_textEditor.setScript(global_currentScript, start);
        updateDisplayForChangedSelectedRoute();
        scrollToNode(start, start);
    }

    JMenu global_currentScriptMenu = null;

    JMenu getCurrentScriptMenu() {
        if (global_currentScriptMenu != null) return global_currentScriptMenu;
        JMenu scriptMenu = new JMenu("This script");
        scriptMenu.setMnemonic(KeyEvent.VK_T);
        {
            JMenuItem menuItem = new JMenuItem("Save As...", KeyEvent.VK_S);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
            menuItem.setActionCommand("save");
            menuItem.addActionListener(this);
            scriptMenu.add(menuItem);
        }
        {
            JMenuItem menuItem = new JMenuItem("Play script", KeyEvent.VK_P);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
            menuItem.setActionCommand("play");
            menuItem.addActionListener(this);
            scriptMenu.add(menuItem);
        }
        {
            JMenuItem menuItem = new JMenuItem("Toggle endings", KeyEvent.VK_E);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
            menuItem.setActionCommand("toggle endings");
            menuItem.addActionListener(this);
            scriptMenu.add(menuItem);
        }
        global_currentScriptMenu = scriptMenu;
        return global_currentScriptMenu;
    }

    public ScriptEditor(URL dex, boolean isStandalone) throws XMLStreamException, IOException, BadConversationScriptException, BadLocationException {
        global_dex = dex;
        global_aboutDialog = new AboutDialog(this, "About " + global_applicationName);
        global_scriptBrowser = new ScriptBrowser(global_dex, this, "Edit Script");
        global_scriptPlayer = new ScriptPlayer(null, false);
        setIconImage(ReactivePlotUtils.getReactivePlotIcon());
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        {
            JMenuBar topPanel = new JMenuBar();
            topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            {
                JMenu scriptsMenu = new JMenu("All scripts");
                scriptsMenu.setMnemonic(KeyEvent.VK_A);
                topPanel.add(scriptsMenu);
                {
                    JMenuItem menuItem = new JMenuItem("Browse internet scripts repository", KeyEvent.VK_B);
                    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
                    menuItem.setActionCommand("browse");
                    menuItem.addActionListener(this);
                    scriptsMenu.add(menuItem);
                }
                {
                    JMenuItem menuItem = new JMenuItem("Open a script from a file", KeyEvent.VK_O);
                    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
                    menuItem.setActionCommand("open");
                    menuItem.addActionListener(this);
                    scriptsMenu.add(menuItem);
                }
                {
                    JMenuItem menuItem = new JMenuItem("Open a script from an internet URL", KeyEvent.VK_U);
                    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.CTRL_MASK));
                    menuItem.setActionCommand("open_url");
                    menuItem.addActionListener(this);
                    scriptsMenu.add(menuItem);
                }
            }
            topPanel.add(getCurrentScriptMenu());
            getCurrentScriptMenu().setEnabled(false);
            {
                JMenuItem aboutItem = new JMenuItem("About", KeyEvent.VK_A);
                aboutItem.setMnemonic(KeyEvent.VK_A);
                aboutItem.setActionCommand("about");
                aboutItem.addActionListener(this);
                topPanel.add(aboutItem);
            }
            int maxWidth = topPanel.getMaximumSize().width;
            int maxHeight = topPanel.getPreferredSize().height;
            topPanel.setMaximumSize(new Dimension(maxWidth, maxHeight));
            topPanel.setMinimumSize(new Dimension(maxWidth, maxHeight));
            contentPane.add(topPanel);
        }
        {
            JPanel panePanel = new JPanel();
            panePanel.setLayout(new BoxLayout(panePanel, BoxLayout.LINE_AXIS));
            global_structureViewer = new StructureViewer(this, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            global_textEditor = new TextEditor(this, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, global_structureViewer, global_textEditor);
            splitPane.setOneTouchExpandable(true);
            splitPane.setDividerLocation(0.3);
            splitPane.setResizeWeight(0.3);
            panePanel.add(splitPane);
            contentPane.add(panePanel);
        }
        this.setContentPane(contentPane);
        this.setTitle(global_applicationName);
        this.pack();
        this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        if (isStandalone) this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent e) {
        if ("browse".equals(e.getActionCommand())) {
            global_scriptBrowser.ShowScriptBrowser();
        } else if ("open_url".equals(e.getActionCommand())) {
            String s = (String) JOptionPane.showInputDialog(this, "Enter the URL of a Reactive Plot script:", "Open script from URL", JOptionPane.PLAIN_MESSAGE, null, null, "http://");
            if ((s != null) && (s.length() > 0)) {
                try {
                    URL url = new URL(s);
                    openScript(url);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        } else if ("save".equals(e.getActionCommand())) {
            try {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                FileFilter filter = new FileNameExtensionFilter("XML file", "xml");
                fc.addChoosableFileFilter(filter);
                int returnVal = fc.showSaveDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    FileWriter writer = new FileWriter(file);
                    global_currentScript.writeScriptAsXML(writer);
                }
            } catch (Exception exception) {
                ReactivePlotUtils.displayExceptionErrorBox(exception, this);
            }
        } else if ("open".equals(e.getActionCommand())) {
            try {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                FileFilter filter = new FileNameExtensionFilter("XML file", "xml");
                fc.addChoosableFileFilter(filter);
                int returnVal = fc.showOpenDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    URL url = file.toURI().toURL();
                    openScript(url);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        } else if ("toggle endings".equals(e.getActionCommand())) {
            global_structureViewer.setShowEndings(!global_structureViewer.getShowEndings());
        } else if ("about".equals(e.getActionCommand())) {
            global_aboutDialog.setVisible(true);
        } else if ("play".equals(e.getActionCommand())) {
            global_scriptPlayer.setVisible(true);
            global_scriptPlayer.openScript(global_currentScriptURL);
        }
    }

    public void setZoomLevel(boolean closeUp) {
        global_structureViewer.setZoomLevel(closeUp);
    }

    @Override
    public void scrollToNode(Node startNode, Node n) {
        global_textEditor.centreScrollingAroundNode(n);
        global_structureViewer.centreScrollingAroundNode(n);
    }

    public void scrollTextToCentreOnNode(Node n) {
        global_textEditor.centreScrollingAroundNode(n);
    }

    public void scrollGraphToCentreOnNode(Node n) {
        global_structureViewer.centreScrollingAroundNode(n);
    }

    @Override
    public void updateForChangedScriptStructure() {
        Node oldNode = global_textEditor.currentStartNode;
        InterpreterEvent event = oldNode.getEventFromNode();
        global_structureViewer.updateForChangedScriptStructure();
        NodeList nodeList = global_structureViewer.getCurrentNodeList();
        if (event != null) {
            Node newNode = nodeList.findNodeFor(event);
            global_textEditor.createEditorsFromStartNode(newNode);
        } else global_textEditor.createEditorsFromStartNode(nodeList.get(0));
        updateDisplayForChangedSelectedRoute();
    }

    @Override
    public void updateForChangedScriptText() {
        global_structureViewer.updateForChangedScriptText();
        global_textEditor.updateForChangedScriptText();
    }

    @Override
    public void updateDisplayForChangedSelectedRoute() {
        global_structureViewer.updateForChangedSelectedRoute();
        global_textEditor.updateForChangedSelectedRoute();
    }
}
