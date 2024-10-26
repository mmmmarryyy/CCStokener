package com.reactiveplot.programs;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import com.reactiveplot.library.ScriptRunner;
import com.reactiveplot.library.interpreter.BadConversationScriptException;
import com.reactiveplot.library.scripts.ScriptInfo;

public class ScriptBrowser extends JFrame implements ActionListener, ListSelectionListener {

    URL global_scriptDirectoryURL;

    final String global_applicationName = "Script Browser";

    final String global_applicationNameWhenUpdating = "Updating Script Browser... ";

    private static final long serialVersionUID = -2652404238269168133L;

    AbstractScriptOpener scriptPlayer;

    String global_openButtonText;

    JScrollPane global_scriptInfoPane = null;

    StyledDocument global_infoPaneDocument = null;

    private int infoPaneMinX = 300;

    private int infoPaneMinY = 200;

    boolean global_isLoading = false;

    public ScriptBrowser(URL scriptDirectoryURL, AbstractScriptOpener scriptPlayer, String openButtonText) {
        this.global_scriptDirectoryURL = scriptDirectoryURL;
        this.scriptPlayer = scriptPlayer;
        this.global_openButtonText = openButtonText;
        this.setIconImage(ReactivePlotUtils.getReactivePlotIcon());
        this.setTitle(global_applicationName);
        Container contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.add(Box.createRigidArea(new Dimension(0, 10)));
        {
            JPanel scriptBoxes = new JPanel();
            scriptBoxes.setLayout(new BoxLayout(scriptBoxes, BoxLayout.LINE_AXIS));
            {
                JScrollPane listPane = new JScrollPane(getScriptsList(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                scriptBoxes.add(listPane);
            }
            {
                scriptBoxes.add(getScriptInfoPane());
            }
            contentPane.add(scriptBoxes);
        }
        {
            JPanel buttons = new JPanel();
            FlowLayout flowLayout = new FlowLayout(FlowLayout.RIGHT);
            flowLayout.setHgap(20);
            buttons.setLayout(flowLayout);
            {
                JButton playButton = new JButton(global_openButtonText);
                playButton.setActionCommand("play");
                playButton.addActionListener(this);
                playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                buttons.add(playButton);
            }
            {
                JButton closeButton = new JButton("Close");
                closeButton.setActionCommand("close");
                closeButton.addActionListener(this);
                closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                buttons.add(closeButton);
            }
            contentPane.add(buttons);
        }
        this.pack();
    }

    JList global_scriptsList = null;

    JList getScriptsList() {
        if (global_scriptsList != null) return global_scriptsList;
        global_scriptsList = new JList();
        global_scriptsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        global_scriptsList.addListSelectionListener(this);
        return global_scriptsList;
    }

    JScrollPane getScriptInfoPane() {
        if (global_scriptInfoPane != null) return global_scriptInfoPane;
        JTextPane infoPane = new JTextPane();
        infoPane.setEditable(false);
        infoPane.setMinimumSize(new Dimension(infoPaneMinX, infoPaneMinY));
        infoPane.setPreferredSize(new Dimension(infoPaneMinX, infoPaneMinY));
        global_infoPaneDocument = infoPane.getStyledDocument();
        global_scriptInfoPane = new JScrollPane(infoPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return global_scriptInfoPane;
    }

    Script getCurrentlySelectedScript() {
        int selected = getScriptsList().getSelectedIndex();
        if (selected != -1) return global_repositoryScripts.get(selected); else return null;
    }

    int global_timerFrame = 0;

    public void actionPerformed(ActionEvent e) {
        if ("play".equals(e.getActionCommand())) {
            Script script = getCurrentlySelectedScript();
            if (script != null) {
                scriptPlayer.openScript(script.scriptURL);
                this.setVisible(false);
            }
        } else if ("close".equals(e.getActionCommand())) {
            this.setVisible(false);
        } else if ("loadingTimer".equals(e.getActionCommand())) {
            if (global_timerFrame == 0) this.setTitle(global_applicationNameWhenUpdating + "|"); else if (global_timerFrame == 1) this.setTitle(global_applicationNameWhenUpdating + "/"); else if (global_timerFrame == 2) this.setTitle(global_applicationNameWhenUpdating + "-"); else if (global_timerFrame == 3) this.setTitle(global_applicationNameWhenUpdating + "\\");
            global_timerFrame = (global_timerFrame + 1) % 4;
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        int optionSelected = getScriptsList().getSelectedIndex();
        if (optionSelected != -1 && !event.getValueIsAdjusting()) {
            Script script = getCurrentlySelectedScript();
            StyledDocument doc = global_infoPaneDocument;
            try {
                doc.remove(0, doc.getLength());
                ReactivePlotUtils.addScriptInformationToDocument(script.scriptInfo, doc);
            } catch (BadLocationException e) {
                ReactivePlotUtils.displayExceptionErrorBox(e, this);
            }
        }
    }

    static ArrayList<URL> loadScriptDirectory(URL directory) throws IOException {
        URLConnection urlConn = directory.openConnection();
        InputStreamReader directoryInput = new InputStreamReader(urlConn.getInputStream());
        BufferedReader directoryReader = new BufferedReader(directoryInput);
        ArrayList<URL> scriptURLs = new ArrayList<URL>();
        String line = null;
        while (true) {
            line = directoryReader.readLine();
            if (line == null) return scriptURLs;
            line = line.trim();
            if (line.isEmpty()) return scriptURLs;
            if (line.endsWith(".dex")) {
                URL dex = new URL(line);
                ArrayList<URL> moreScripts = loadScriptDirectory(dex);
                scriptURLs.addAll(moreScripts);
            } else if (line.endsWith(".xml")) {
                URL url = new URL(line);
                scriptURLs.add(url);
            } else throw new IOException("Dex \"" + directory + "\" contained bad line \"" + line + "\"");
        }
    }

    public static class Script {

        URL scriptURL;

        ScriptInfo scriptInfo;
    }

    Timer global_loadingTimer = new Timer(200, this);

    ArrayList<Script> global_repositoryScripts = new ArrayList<Script>();

    public void ShowScriptBrowser() {
        this.setVisible(true);
        if (!global_isLoading) {
            global_isLoading = true;
            global_loadingTimer.setActionCommand("loadingTimer");
            global_loadingTimer.start();
            SwingWorker<ArrayList<Script>, Void> worker = new SwingWorker<ArrayList<Script>, Void>() {

                @Override
                public ArrayList<Script> doInBackground() {
                    return updateScriptBrowser(global_scriptDirectoryURL, null);
                }

                @Override
                public void done() {
                    try {
                        global_repositoryScripts = get();
                    } catch (Exception e) {
                        ReactivePlotUtils.displayExceptionErrorBox(e, null);
                    }
                    Vector<String> listLabels = new Vector<String>();
                    for (Script script : global_repositoryScripts) {
                        String label = null;
                        if (script.scriptInfo != null) {
                            label = script.scriptInfo.getTitle();
                        } else label = script.scriptURL.getFile();
                        listLabels.add(label);
                    }
                    getScriptsList().setListData(listLabels);
                    setTitle(global_applicationName);
                    if (global_loadingTimer.isRunning()) global_loadingTimer.stop();
                    global_isLoading = false;
                }
            };
            worker.execute();
        }
    }

    static ArrayList<Script> updateScriptBrowser(URL directoryURL, Component frame) {
        ArrayList<Script> scripts = new ArrayList<Script>();
        ArrayList<URL> scriptURLs = null;
        try {
            scriptURLs = loadScriptDirectory(directoryURL);
        } catch (Exception e) {
            ReactivePlotUtils.displayExceptionErrorBox(e, frame);
            return null;
        }
        for (URL scriptURL : scriptURLs) {
            Script script = new Script();
            script.scriptURL = scriptURL;
            try {
                ScriptRunner runner = new ScriptRunner(scriptURL, null, false);
                script.scriptInfo = runner.getScriptInfo();
            } catch (BadConversationScriptException e) {
                ReactivePlotUtils.displayBadScriptErrorBox(e, frame);
            } catch (Exception e) {
                ReactivePlotUtils.displayExceptionErrorBox(e, frame);
            }
            scripts.add(script);
        }
        return scripts;
    }
}
