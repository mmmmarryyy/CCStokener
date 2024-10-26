package com.onyourmind.tra;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import com.onyourmind.awt.App;
import com.onyourmind.awt.OymFrame;
import com.onyourmind.awt.PagePanel;
import com.onyourmind.awt.TabPanel;
import com.onyourmind.awt.dialog.DialogBox;
import com.onyourmind.awt.dialog.InfoBox;
import com.onyourmind.dnote.Node;
import com.onyourmind.dnote.OptionsDialog;
import com.onyourmind.tools.Date;

class FrameTra extends OymFrame implements ConstantsTra {

    private static final long serialVersionUID = 4864018818388550765L;

    public String[][] windowStrings = { { WINDOW_START, "com.onyourmind.tra.PanelTraMain" }, { WINDOW_STRUCTURING, "com.onyourmind.tra.StructuringPanel" }, { WINDOW_DATA_ASSESSMENTS, "com.onyourmind.tra.DataAssessmentPanel" }, { WINDOW_DATA_ASSESSMENTS_DETAILED, "com.onyourmind.tra.AssessmentPanel" }, { WINDOW_HOLIDAY, "com.onyourmind.tra.HolidayPanel" }, { WINDOW_TORNADO_CHART, "com.onyourmind.tra.TornadoChartPanel" }, { WINDOW_RISK_PROFILE_CHART, "com.onyourmind.tra.RiskProfileChartPanel" }, { WINDOW_DATA_SUMMARY, "com.onyourmind.tra.DataSummaryPanel" }, { WINDOW_RISK_MITIGATION_PLAN, "com.onyourmind.tra.RiskMitigationPanel" }, { WINDOW_NOTES, "com.onyourmind.tra.NotesPanel" } };

    public String[] fileMenu = { "New", "Open...", "Save", "Save As...", "-", "Export...", "-" };

    public String[] appletFileMenu = { "New", "Open Example File", "-" };

    public String[] helpMenu = { "Contents", "About" };

    public TabPanel tabbedPane = null;

    public String[] excludedNames = { WINDOW_DATA_ASSESSMENTS_DETAILED, WINDOW_HOLIDAY };

    public String tabbedPaneName = "TabbedPane";

    public FrameTra(String str, App pApp) {
        super(str, pApp);
        setFileMenu(fileMenu);
        setHelpMenu(helpMenu);
        if (!getApp().isStandAlone) setFileMenu(appletFileMenu);
    }

    private Map<String, Node> getNodeMap() {
        StructuringPanel pPanel = (StructuringPanel) selectWindow(WINDOW_STRUCTURING, false);
        if (pPanel == null || pPanel.doc == null) return new HashMap<String, Node>();
        return pPanel.doc.nodeMap;
    }

    public String getTitleString() {
        return ((PanelTraMain) selectWindow(WINDOW_START, false)).getProjectName() + " - " + ((PanelTraMain) selectWindow(WINDOW_START, false)).getApplicationName();
    }

    public TraNode getNodeFromLabel(String strNodeLabel) {
        if (strNodeLabel == null || strNodeLabel.length() == 0) return null;
        for (Node node : getNodeMap().values()) {
            TraNode pNode = (TraNode) node;
            String strStripped = getStrippedString(pNode.nodeLabel);
            if (strStripped.equals(strNodeLabel)) return pNode;
        }
        return null;
    }

    public String getStrippedString(String strOriginal) {
        if (strOriginal == null) return "";
        char[] charArray = strOriginal.toCharArray();
        for (int nChar = 0; nChar < charArray.length; nChar++) {
            char cCurrent = charArray[nChar];
            if (cCurrent == '\r' || cCurrent == '\n' || cCurrent == '\t') charArray[nChar] = ' ';
        }
        return new String(charArray);
    }

    public java.util.List<Node> getValidNodes() {
        return getValidNodes(false);
    }

    public java.util.List<Node> getValidNodes(boolean bSort) {
        java.util.List<Node> nodes = new ArrayList<Node>();
        for (Node node : getNodeMap().values()) {
            TraNode pNextNode = (TraNode) node;
            if (!pNextNode.isValid()) continue;
            nodes.add(pNextNode);
        }
        if (bSort) nodes = sortNodeListByPosition(nodes);
        return nodes;
    }

    public java.util.List<Node> sortNodeListByPosition(java.util.List<Node> nodes) {
        java.util.List<Node> result = new ArrayList<Node>();
        while (nodes.size() > 0) {
            int nMinX = 3000;
            TraNode pNodeMin = null;
            for (Node node : nodes) {
                TraNode pNode = (TraNode) node;
                if (pNode.nodeRect.x <= nMinX) {
                    pNodeMin = pNode;
                    nMinX = pNode.nodeRect.x;
                }
            }
            TraNode pNodeLast = null;
            if (result.size() > 0) pNodeLast = (TraNode) (result.get(result.size() - 1));
            if (pNodeLast != null && pNodeLast.nodeRect.x == nMinX) {
                int nMinY = 3000;
                for (Node node : nodes) {
                    TraNode pNode = (TraNode) node;
                    if (pNode.nodeRect.x == nMinX && pNode.nodeRect.y < nMinY) {
                        pNodeMin = pNode;
                        nMinY = pNode.nodeRect.y;
                    }
                }
            }
            if (pNodeMin == null) break;
            nodes.remove(pNodeMin);
            result.add(pNodeMin);
        }
        return result;
    }

    public void onFileOpen() {
        if (getApp().isStandAlone) {
            super.onFileOpen();
            return;
        }
        String strURL = null;
        try {
            strURL = getApp().getCodeBase().toString() + "example.tra";
            System.out.println(strURL);
            URL urlFile = new URL(strURL);
            InputStream isNew = urlFile.openStream();
            DataInputStream disNew = new DataInputStream(isNew);
            openFileShared("Example", disNew);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(strURL);
        }
    }

    public void openFileShared(String strFileName, DataInputStream disCurrent) {
        onFileClose();
        super.openFileShared(strFileName, disCurrent);
    }

    public void updateMenu() {
        enableMenuItems((getActiveDocument() != null), getDisabledItems());
        MenuBar mb = getMenuBar();
        if (mb == null) return;
        boolean bEnable = getCurrentPage() instanceof StructuringPanel;
        for (int nMenu = 0; nMenu < mb.getMenuCount(); nMenu++) {
            String strMenu = mb.getMenu(nMenu).getLabel();
            if (strMenu.equals("File") || strMenu.equals("Screen") || strMenu.equals("Window") || strMenu.equals("Help")) continue;
            mb.getMenu(nMenu).setEnabled(bEnable);
        }
    }

    public void addCustomMenus() {
        MenuBar mb = getMenuBar();
        if (mb == null) return;
        Menu menuEdit = new Menu("Edit");
        mb.add(menuEdit);
        addMenuItem(menuEdit, new MenuItem("Cut"));
        addMenuItem(menuEdit, new MenuItem("Copy"));
        addMenuItem(menuEdit, new MenuItem("Paste"));
        addMenuItem(menuEdit, new MenuItem("-"));
        addMenuItem(menuEdit, new MenuItem("Delete"));
        addMenuItem(menuEdit, new MenuItem("Select All"));
        addMenuItem(menuEdit, new MenuItem("-"));
        addMenuItem(menuEdit, new MenuItem("Toggle Delay"));
        Menu menuView = new Menu("View");
        mb.add(menuView);
        addMenuItem(menuView, new MenuItem("Scale to Window"));
        addMenuItem(menuView, new MenuItem("Normal"));
        addMenuItem(menuView, new MenuItem("-"));
        addMenuItem(menuView, new MenuItem("Options"));
        Menu menuBorder = new Menu("Border");
        mb.add(menuBorder);
        addMenuItem(menuBorder, new MenuItem("None"));
        addMenuItem(menuBorder, new MenuItem("Rectangle"));
        addMenuItem(menuBorder, new MenuItem("Diamond"));
        addMenuItem(menuBorder, new MenuItem("Oval"));
        addMenuItem(menuBorder, new MenuItem("Octagon"));
        Menu menuFont = new Menu("Font");
        mb.add(menuFont);
        String[] strFontArray = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (int i = 0; i < strFontArray.length; i++) addMenuItem(menuFont, new MenuItem(strFontArray[i]));
        Menu menuSize = new Menu("Size");
        mb.add(menuSize);
        String[] strSizeArray = { "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "26", "28", "30", "32" };
        for (int i = 0; i < strSizeArray.length; i++) addMenuItem(menuSize, new MenuItem(strSizeArray[i]));
        Menu menuStyle = new Menu("Style");
        mb.add(menuStyle);
        addMenuItem(menuStyle, new MenuItem("Plain"));
        addMenuItem(menuStyle, new MenuItem("Bold"));
        addMenuItem(menuStyle, new MenuItem("Italic"));
    }

    public void menuAction(ActionEvent e) {
        super.menuAction(e);
        if (!(e.getSource() instanceof MenuItem)) return;
        MenuItem mi = (MenuItem) e.getSource();
        Menu menuSelected = (Menu) mi.getParent();
        String strParentMenu = menuSelected.getLabel();
        String strMenuText = mi.getLabel();
        if (strParentMenu.equals("File") || strParentMenu.equals("Window") || strParentMenu.equals("Help")) return;
        StructuringPanel pPanel = (StructuringPanel) selectWindow(WINDOW_STRUCTURING, false);
        if (pPanel == null) return;
        DnoteTraDoc pDoc = pPanel.doc;
        if (pDoc == null || pDoc.activeView == null) return;
        if (strParentMenu.equals("Font")) {
            pPanel.doc.setFontName(strMenuText);
        } else if (strMenuText.equals("Options")) {
            DoOptionsDialogBox(pPanel);
        } else if (strMenuText.equals("Scale to Window")) {
            pDoc.textToLabel();
            pDoc.activeView.resizeNode = null;
            pDoc.activeView.isViewEntireMap = true;
        } else if (strMenuText.equals("Normal")) {
            pDoc.activeView.isViewEntireMap = false;
        } else if (strMenuText.equals("Toggle Delay")) {
            pDoc.toggleDelay();
        }
        pPanel.doc.menuSelection(strParentMenu, strMenuText);
        pPanel.doc.activeView.repaint();
    }

    public void DoOptionsDialogBox(StructuringPanel pPanel) {
        OptionsDialog dlg = new OptionsDialog(this, (int) pPanel.doc.arrowHeadWidth, (int) pPanel.doc.arrowHeadLength);
        dlg.setVisible(true);
        int nWidth = dlg.getWidth();
        if (nWidth != (int) pPanel.doc.arrowHeadWidth) {
            pPanel.doc.arrowHeadWidth = nWidth;
            setModified(true);
        }
        int nLength = dlg.getLength();
        if (nLength != (int) pPanel.doc.arrowHeadLength) {
            pPanel.doc.arrowHeadLength = nLength;
            setModified(true);
        }
    }

    public void onHelpContents() {
        try {
            if (getApp().isStandAlone) {
                HelpSet hs = new HelpSet(null, HelpSet.findHelpSet(getClass().getClassLoader(), "help/TRA.hs"));
                HelpBroker hb = hs.createHelpBroker();
                hb.setFont(new Font("Arial", Font.PLAIN, 12));
                hb.setSize(new Dimension(800, 600));
                hb.setDisplayed(true);
            } else browserShow("help/WebHelp/start.htm");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getFooter() {
        return getTitleString();
    }

    public int getLatestFileVersion() {
        return 101;
    }

    public void sreateControls() {
        setWindowStrings(windowStrings);
        disableMultiDoc();
        setupWindowList();
        setLayout(new CardLayout());
        setBackground(Color.lightGray);
        buildMenu();
        TRACustomStuff();
        selectWindow(null);
        pack();
    }

    public void TRACustomStuff() {
        tabbedPane = new TabPanel(this);
        super.add(tabbedPaneName, tabbedPane);
        initializeAllWindows();
    }

    public Component add(String name, Component comp) {
        for (int i = 0; i < excludedNames.length; i++) {
            if (excludedNames[i].equals(name)) {
                return super.add(name, comp);
            }
        }
        tabbedPane.addItem(name, comp);
        return comp;
    }

    public void showPagePanel(PagePanel page, String strName) {
        for (int i = 0; i < excludedNames.length; i++) {
            if (excludedNames[i].equals(strName)) {
                super.showPagePanel(null, strName);
                return;
            }
        }
        super.showPagePanel(null, tabbedPaneName);
        tabbedPane.select(strName);
    }

    public String getFilenameExtension() {
        return ".tra";
    }

    public void onFileNew() {
        onFileClose();
        super.onFileNew();
    }

    public void onExportDialog() {
        ExportDialogTra dlg = new ExportDialogTra(this);
        dlg.setVisible(true);
        int nExportType = dlg.getExportType();
        if (dlg.getStatus() != DialogBox.ACTION_OK || nExportType == -1) return;
        PagePanel page = dlg.isCurrentPageOnly() ? getCurrentPage() : null;
        if (nExportType == ExportDialogTra.EXPORT_PROJECT_98) doProject98Export(); else onExport(page, nExportType);
    }

    public void doProject98Export() {
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            ResourceBundle rb = ResourceBundle.getBundle("i18n.LabelBundle", getLocale());
            FileDialog dlg = new FileDialog((OymFrame) null, rb.getString("Save_File"), FileDialog.SAVE);
            dlg.setVisible(true);
            String strFileOnly = dlg.getFile();
            if (strFileOnly == null || strFileOnly == "") return;
            String strFileName = addFileExtension(dlg.getDirectory() + strFileOnly, ".txt");
            FileOutputStream fosCurrent = new FileOutputStream(strFileName);
            PrintWriter pw = new PrintWriter(fosCurrent);
            buildProject98File(pw);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    public String addFileExtension(String strFilename, String strExtension) {
        if (!strFilename.toLowerCase().endsWith(strExtension)) return strFilename + strExtension;
        return strFilename;
    }

    public void buildProject98File(PrintWriter pw) {
        try {
            pw.println("ID  Unique_ID   Task_Name   Duration    Type    Outline_Level   Baseline_Duration   Predecessors    Start_Date  Finish_Date Early_Start Early_Finish    Late_Start  Late_Finish Free_Slack  Total_Slack Leveling_Delay  Percent_Complete    Actual_Start    Actual_Finish   Baseline_Start  Baseline_Finish Constraint_Type Constraint_Date Stop    Resume  Created Scheduled_Work  Baseline_Work   Actual_Work Cost    Fixed_Cost  Baseline_Cost   Actual_Cost Remaining_Cost  WBS Priority    Milestone   Summary Rollup  Text1   Text2   Text3   Text4   Text5   Text6   Text7   Text8   Text9   Text10  Cost1   Cost2   Cost3   Duration1   Duration2   Duration3   Flag1   Flag2   Flag3   Flag4   Flag5   Flag6   Flag7   Flag8   Flag9   Flag10  Marked  Number1 Number2 Number3 Number4 Number5 Subproject_File");
            TraModel pModel = new TraModel((StructuringPanel) selectWindow(WINDOW_STRUCTURING, false));
            double daysPerTimeUnit = ((PanelTraMain) (selectWindow(WINDOW_START, false))).getDaysPerTimeUnit();
            for (Node node : getValidNodes()) {
                TraNode pNode = (TraNode) node;
                String strDateNow = Date.getDateStringFromDate(new java.util.Date(System.currentTimeMillis()));
                String strFinishDate = null;
                for (int nType = 0; nType < 2; nType++) {
                    pModel.setPredecessors(nType);
                    boolean bMitigation = (nType > 0);
                    double fStart = pNode.getStartTimeUnits(bMitigation, false);
                    String strStartDate = Date.getDateStringFromDouble(fStart, daysPerTimeUnit);
                    boolean bZeroStart = (fStart == 0.0);
                    if (bZeroStart) strStartDate = "";
                    int nID = pNode.getUniqueID() * 2 + nType;
                    pw.print(nID + "\t");
                    pw.print(nID + "\t");
                    pw.print(getStrippedString(pNode.nodeLabel) + (nType == 1 ? " (with mitigation)" : "") + "\t");
                    int nDays = (int) (pNode.getTimeUnitValue(1, bMitigation) * 7.0);
                    nDays = (nDays < 0) ? 0 : nDays;
                    pw.print(nDays + " days\t");
                    pw.print("Fixed Duration\t");
                    pw.print("1\t");
                    pw.print(nDays + " days\t");
                    pw.print(pNode.getPredecessors() + "\t");
                    pw.print(strStartDate + "\t");
                    strFinishDate = bZeroStart ? "" : Date.getDateStringFromDouble(fStart + pNode.getTimeUnitValue(1, bMitigation), daysPerTimeUnit);
                    pw.print(strFinishDate + "\t");
                    pw.print(strStartDate + "\t");
                    strFinishDate = bZeroStart ? "" : Date.getDateStringFromDouble(fStart + pNode.getTimeUnitValue(0, bMitigation), daysPerTimeUnit);
                    pw.print(strFinishDate + "\t");
                    pw.print(strStartDate + "\t");
                    strFinishDate = bZeroStart ? "" : Date.getDateStringFromDouble(fStart + pNode.getTimeUnitValue(2, bMitigation), daysPerTimeUnit);
                    pw.print(strFinishDate + "\t");
                    pw.print("0 days\t");
                    pw.print("0 days\t");
                    pw.print("0 edays\t");
                    pw.print("0%\t");
                    pw.print("NA\t");
                    pw.print("NA\t");
                    pw.print(strStartDate + "\t");
                    strFinishDate = bZeroStart ? "" : Date.getDateStringFromDouble(fStart + pNode.getTimeUnitValue(1, bMitigation), daysPerTimeUnit);
                    pw.print(strFinishDate + "\t");
                    pw.print("As Soon As Possible\t");
                    pw.print("NA\t");
                    pw.print("NA\t");
                    pw.print("NA\t");
                    pw.print(strDateNow);
                    pw.println();
                }
            }
        } catch (Exception e) {
            new InfoBox(this, "An error occurred while creating the Microsoft Project 98 export file.");
            e.printStackTrace();
        }
    }
}
