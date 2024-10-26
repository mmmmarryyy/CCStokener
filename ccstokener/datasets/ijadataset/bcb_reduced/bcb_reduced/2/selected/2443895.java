package freestyleLearning.homeCore.helpManager.dialogs;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import freestyleLearning.homeCore.helpManager.*;
import freestyleLearning.homeCore.learningUnitsManager.*;
import freestyleLearning.homeCore.programConfigurationManager.event.*;
import freestyleLearning.learningUnitViewAPI.*;
import freestyleLearningGroup.independent.gui.*;
import freestyleLearningGroup.independent.util.*;

public class FSLHelpManagerDialog extends JFrame implements FSLProgramConfigurationListener {

    private FLGHtmlPane helpContentPane;

    private FLGInternationalization internationalization;

    private FLGTextButton3D buttonOK;

    private FSLHelpManager helpManager;

    private HyperlinkListener hyperlinkListener;

    private FSLHelpHistoryManager historyManager;

    private FSLLearningUnitViewsManager viewsManager;

    private FSLLearningUnitsManager learningUnitsManager;

    private String programDirectoryPath;

    private URL helpUrl;

    private String fileSeparator;

    private static FSLLearningUnitViewManager memManager;

    public FSLHelpManagerDialog(FSLProgramConfigurationEventGenerator programConfigurationEventGenerator, String title, FSLLearningUnitsManager learningUnitsManager, String programDirectoryPath) {
        super(title);
        fileSeparator = System.getProperty("file.separator");
        this.learningUnitsManager = learningUnitsManager;
        this.programDirectoryPath = programDirectoryPath;
        programConfigurationEventGenerator.addProgramConfigurationListener(this);
        internationalization = new FLGInternationalization("freestyleLearning.homeCore.helpManager.dialogs.internationalization", FSLHelpManagerDialog.class.getClassLoader());
        java.awt.Dimension screenDimension = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int pos_x = (int) (screenDimension.width / 3. / 2);
        int pos_y = (int) (screenDimension.height / 3. / 2);
        setSize(screenDimension.width * 2 / 3, screenDimension.height * 2 / 3);
        setLocation((int) (screenDimension.width - getWidth()) / 2, (int) (screenDimension.height - getHeight()) / 2);
    }

    public FSLHelpManagerDialog(String title) {
        super(title);
        internationalization = new FLGInternationalization("freestyleLearning.homeCore.helpManager.dialogs.internationalization", FSLHelpManagerDialog.class.getClassLoader());
        java.awt.Dimension screenDimension = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int pos_x = (int) (screenDimension.width / 5. / 4);
        int pos_y = (int) (screenDimension.height / 5. / 4);
        setSize(screenDimension.width * 2 / 3, screenDimension.height * 4 / 5);
        setLocation((int) (screenDimension.width - getWidth()) / 2, (int) (screenDimension.height - getHeight()) / 2);
        helpContentPane = new FLGHtmlPane();
        helpContentPane.setEditable(false);
        helpContentPane.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    FLGPlatformSpecifics.startExternalApplication(e.getURL().toString());
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(helpContentPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollPane.setOpaque(false);
        JPanel helpButtonPanel = createButtonPanel();
        JPanel mainPanel = new FLGEffectPanel("FLGDialog.background", false);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(helpButtonPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    public void init(FSLHelpHistoryManager historyManager, FSLLearningUnitViewsManager viewsManager) {
        this.viewsManager = viewsManager;
        this.historyManager = historyManager;
        buildIndependentUI();
        setMainHelpText();
    }

    public void buildIndependentUI() {
        JPanel navigationButtonPanel = createNavigationButtonPanel();
        helpContentPane = new FLGHtmlPane();
        helpContentPane.addHyperlinkListener(new FSLHelpManagerDialog_HyperlinkListener());
        helpContentPane.addHyperlinkListener(historyManager);
        helpContentPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(helpContentPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollPane.setOpaque(false);
        JPanel helpButtonPanel = createButtonPanel();
        JPanel mainPanel = new FLGEffectPanel("FLGDialog.background", false);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(navigationButtonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(helpButtonPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createNavigationButtonPanel() {
        JPanel buttonPanel = new FLGEffectPanel("FSLMainFrameColor1", "FLGDialog.background", true);
        buttonPanel.setLayout(new FLGLeftToRightLayout(5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        FLGMainFrameToolBarButton[] toolBarButtons = historyManager.getEditToolBarButtons();
        for (int i = 0; i < toolBarButtons.length; i++) {
            buttonPanel.add(toolBarButtons[i]);
        }
        return buttonPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new FLGEffectPanel("FLGDialog.background", false);
        buttonPanel.setOpaque(false);
        buttonOK = new FLGTextButton3D(internationalization.getString("button.ok.label"), (Color) UIManager.get("FLGDialog.background"));
        buttonOK.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        buttonPanel.add(buttonOK);
        buttonOK.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        buttonOK.getActionMap().put("cancel", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        return buttonPanel;
    }

    public void showPage(String htmlFileName, java.net.URL base) {
        String text = readHtmlFile(htmlFileName);
        helpContentPane.setText(readHtmlFile(htmlFileName));
        helpContentPane.setBase(base);
    }

    private String readHtmlFile(String htmlFileName) {
        StringBuffer buffer = new StringBuffer();
        java.net.URL url = getClass().getClassLoader().getResource("freestyleLearning/homeCore/help/" + htmlFileName);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String string = " ";
            while (string != null) {
                string = reader.readLine();
                if (string != null) buffer.append(string);
            }
        } catch (Exception exc) {
            System.out.println(exc);
        }
        return new String(buffer);
    }

    public void setMainHelpText() {
        String targetFileName = "helpMainTop_" + java.util.Locale.getDefault().getLanguage() + ".html";
        StringBuffer helpContent = new StringBuffer();
        helpContent.append(readHtmlFile(targetFileName));
        helpContent.append("<p>" + internationalization.getString("help.content.viewInfo") + "<ul>");
        String[][] installedLearningUnitViewManagerIds = learningUnitsManager.getInstalledLearningUnitViewManagersIdsAndTitles();
        for (int i = 0; i < installedLearningUnitViewManagerIds.length; i++) {
            String installedViewManagerId = installedLearningUnitViewManagerIds[i][0];
            String installedViewManagerTitle = installedLearningUnitViewManagerIds[i][1];
            FSLLearningUnitViewManager viewManager = viewsManager.getLearningUnitViewManager(installedViewManagerId);
            if (viewManager != null) {
                helpUrl = viewManager.getMainHelpPageUrl();
                helpContent.append("<li>" + internationalization.getString("help.content.gettingStarted") + " <a href=\"" + installedViewManagerTitle + "_" + java.util.Locale.getDefault().getLanguage() + ".html\">" + installedViewManagerTitle + "</a>");
            }
        }
        helpContent.append("</ul></body></html>");
        helpContentPane.setText(new String(helpContent));
        helpUrl = getClass().getClassLoader().getResource("freestyleLearning/homeCore/help/");
        helpContentPane.setBase(helpUrl);
    }

    public void setTargetText(String hyperlinkTarget) {
        String targetFileName = FLGUtilities.removeSpacesFromString(hyperlinkTarget);
        String[][] installedLearningUnitViewManagerIds = learningUnitsManager.getInstalledLearningUnitViewManagersIdsAndTitles();
        try {
            boolean moreViewSpecific = true;
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < 8; i++) {
                char c = targetFileName.charAt(i);
                buffer.append(c);
            }
            if ((buffer.toString()).equals("helpMain")) {
                helpUrl = getClass().getClassLoader().getResource("freestyleLearning/homeCore/help/");
                helpContentPane.setPage(helpUrl + targetFileName);
                helpContentPane.setBase(helpUrl);
                moreViewSpecific = false;
            }
            if (moreViewSpecific) {
                for (int i = 0; i < installedLearningUnitViewManagerIds.length; i++) {
                    if (hyperlinkTarget.equals(installedLearningUnitViewManagerIds[i][1] + "_" + java.util.Locale.getDefault().getLanguage() + ".html")) {
                        FSLLearningUnitViewManager viewManager = viewsManager.getLearningUnitViewManager(installedLearningUnitViewManagerIds[i][0]);
                        helpUrl = viewManager.getMainHelpPageUrl();
                        helpContentPane.setPage_superVersion(helpUrl + "/index" + "_" + java.util.Locale.getDefault().getLanguage() + ".html");
                        helpContentPane.setBase(helpUrl);
                        memManager = viewManager;
                        moreViewSpecific = false;
                        break;
                    }
                }
            }
            if (moreViewSpecific) {
                helpUrl = memManager.getMainHelpPageUrl();
                helpContentPane.setPage_superVersion(helpUrl + "/" + targetFileName);
                helpContentPane.setBase(helpUrl);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        helpContentPane.setStyleSheet(getClass().getClassLoader().getResource("freestyleLearning/homeCore/help/helpStyleSheet.css"));
    }

    public void configurationChanged(FSLProgramConfigurationEvent event) {
        repaint();
    }

    class FSLHelpManagerDialog_HyperlinkListener implements HyperlinkListener {

        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (e.getDescription().equals(("main"))) {
                    setMainHelpText();
                } else {
                    setTargetText(e.getDescription());
                }
            }
        }
    }
}
