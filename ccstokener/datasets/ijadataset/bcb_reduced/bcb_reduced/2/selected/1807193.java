package webuilder.webx;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.text.*;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.webx.project.Files;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import webuilder.webx.component.DesignEditor;
import webuilder.webx.component.Editor;
import webuilder.webx.dialogs.BuildSqlDialog;
import webuilder.webx.dialogs.BuildSqlDialogDescriptor;
import webuilder.webx.dialogs.DataTableProperties;
import webuilder.webx.dialogs.DataTablePropertiesDecriptor;
import webuilder.webx.test.TestPanel1Descriptor;
import webuilder.webx.util.CobraConfig;
import webuilder.webx.util.FilesUtil;
import webuilder.webx.util.WebXConfig;
import webuilder.webx.util.XslUtil;
import webuilder.webx.wizardapi.Wizard;
import webuilder.webx.wizardapi.WizardPanelDescriptor;
import com.Ostermiller.Syntax.HighlightedDocument;

public class WebPageEditor extends JPanel {

    private Editor editXSLT;

    private Editor editXML;

    private Editor editPHP;

    private DesignEditor DesignView;

    mainWin MyWin;

    private JTabbedPane tabbedPane;

    private JPanel tabXSLT;

    private JPanel tabXML;

    private JPanel tabPHP;

    private Document xsltDocument;

    private HtmlPanel htmlPanel;

    private InputSource xsltInputSource;

    private DefaultStyledDocument codeDocument;

    private String documentHolder;

    public Files toOpen;

    public Wizard wizard;

    public Map<String, ActionListener> DesignActionsMap;

    private SwingWorker updateViewJob = new SwingWorker() {

        @Override
        protected Object doInBackground() throws Exception {
            savePage(false);
            buildDocument(toOpen);
            return null;
        }
    };

    public WebPageEditor(mainWin frame, Files toOpen) {
        super();
        MyWin = frame;
        this.toOpen = toOpen;
        htmlPanel = new HtmlPanel();
        initDesignActions();
        setLayout(new BorderLayout());
        initTextTabs();
        buildDocument(toOpen);
        loadPage(toOpen);
        updateDocument(editXSLT.MyText.getText());
        tabbedPane.setSelectedIndex(3);
        add(tabbedPane, "Center");
        setVisible(true);
    }

    public WebPageEditor(mainWin frame) {
        super();
        MyWin = frame;
        htmlPanel = new HtmlPanel();
        initDesignActions();
        setLayout(new BorderLayout());
        initTextTabs();
        add(tabbedPane, "Center");
        setVisible(true);
    }

    public void buildDocument(Files page) {
        String uri = constructFileUrlString(page, true);
        URL url;
        try {
            url = new URL(uri);
            URLConnection connection = url.openConnection();
            InputStream in = connection.getInputStream();
            Reader reader = new InputStreamReader(in, "UTF8");
            xsltInputSource = new InputSourceImpl(reader, uri);
            xsltInputSource.setEncoding("utf-8");
            UserAgentContext ucontext = new CobraConfig.LocalUserAgentContext();
            HtmlRendererContext rendererContext = new CobraConfig.LocalHtmlRendererContext(htmlPanel, ucontext);
            DocumentBuilderImpl builder = new DocumentBuilderImpl(rendererContext.getUserAgentContext(), rendererContext);
            xsltDocument = builder.parse(xsltInputSource);
            htmlPanel.setDocument(xsltDocument, rendererContext);
            documentHolder = xsltDocument.toString();
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    public void updateDocument(String page) {
        savePage(true);
        buildDocument(toOpen);
        savePage(false);
    }

    private String constructFileUrlString(Files page, boolean isLocal) {
        String fileURL;
        if (isLocal) fileURL = "file:///"; else fileURL = "http://";
        fileURL += MyWin.getProjectLocation().replace('\\', '/') + "/";
        fileURL += page.getXsltFile();
        return fileURL;
    }

    public WebPageEditor getWebPageEditor() {
        return this;
    }

    private void initDesignActions() {
        DesignActionsMap = new HashMap<String, ActionListener>(20);
        ActionListener addDataTable = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("executing data table action");
                Node selection = htmlPanel.getSelectionNode();
                NodeImpl selctionNode = null;
                if (selection == null) {
                    JOptionPane.showMessageDialog(getWebPageEditor(), "You need to select a teble design in a '<div>'", "Add data table", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                selctionNode = (NodeImpl) ((NodeImpl) selection).getParentNode();
                if (selctionNode == null) selctionNode = ((NodeImpl) selection);
                if (selection != null && selctionNode.getParentNode().getNodeName().equals("div") && selctionNode.getNodeName().equals("table")) {
                    String stringArray[] = { "Build SQL", "Use stored proc.", "Extract from template" };
                    int ans = JOptionPane.showOptionDialog(((Component) (e.getSource())).getParent(), "How to get the data?", "How to get the data?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, stringArray, stringArray[0]);
                    if (ans == 0) {
                        wizard = new Wizard();
                        wizard.getDialog().setTitle("Test Wizard Dialog");
                        WizardPanelDescriptor descriptor1 = new BuildSqlDialogDescriptor(MyWin);
                        wizard.registerWizardPanel(TestPanel1Descriptor.IDENTIFIER, descriptor1);
                        WizardPanelDescriptor descriptor2 = new DataTablePropertiesDecriptor(((BuildSqlDialog) descriptor1.getPanelComponent()));
                        wizard.registerWizardPanel(DataTablePropertiesDecriptor.IDENTIFIER, descriptor2);
                        wizard.setCurrentPanel(TestPanel1Descriptor.IDENTIFIER);
                        int ret = wizard.showModalDialog();
                        if (ret == 0) {
                            System.out.println("option 0" + ret);
                            HashMap<String, String> params = (((DataTableProperties) descriptor2.getPanelComponent()).getXslParams());
                            String[] designData = new String[4];
                            designData = XslUtil.extractDesignData(htmlPanel);
                            designData[3] = (((DataTableProperties) descriptor2.getPanelComponent()).getObjName());
                            String newHtml = XslUtil.insertXslObject(htmlPanel, "data_table", params);
                            FilesUtil.createCustomFile("code_core\\xslt\\" + "data_table" + ".xsl", MyWin.getProjectLocation() + "\\xslt\\" + "data_table" + ".xsl", new String[] { WebXConfig.XSL_ITEM_COLLECTION_ROOT, WebXConfig.XSL_ITEM_COLLECTION_SET, WebXConfig.XSL_ITEM_COLLECTION_ITEM, WebXConfig.XSL_XML_SOURCE }, designData);
                            editXSLT.setText(newHtml);
                            String PhpInsertCode = (((DataTableProperties) descriptor2.getPanelComponent()).getPhpDataCode());
                            System.out.println(PhpInsertCode);
                            editPHP.setText(editPHP.getText().replace(WebXConfig.PHP_NEWLINE_PLACEHOLDER, PhpInsertCode + "\n" + WebXConfig.PHP_NEWLINE_PLACEHOLDER));
                        }
                    }
                    updateDocument(editXSLT.MyText.getText());
                } else JOptionPane.showMessageDialog(getWebPageEditor(), "You need to select a teble design in a '<div>'", "Add data table", JOptionPane.ERROR_MESSAGE);
            }
        };
        DesignActionsMap.put(WebXConfig.TABLE_DESIGN_ACTION, addDataTable);
    }

    private void initTextTabs() {
        editXSLT = new Editor(Color.LIGHT_GRAY, new Font("MS Sans Serif", 1, 14), null, HighlightedDocument.HTML_KEY_STYLE);
        editXSLT.setSize(590, 700);
        editXML = new Editor(Color.PINK, new Font("MS Sans Serif", 1, 14), null, HighlightedDocument.HTML_KEY_STYLE);
        editXML.setSize(590, 700);
        editPHP = new Editor(Color.CYAN, new Font("MS Sans Serif", 1, 14), null, HighlightedDocument.C_STYLE);
        editPHP.setSize(590, 700);
        DesignView = new DesignEditor(htmlPanel, DesignActionsMap);
        editXSLT.MyText.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                updateDocument(editXSLT.MyText.getText());
            }
        });
        tabXSLT = new JPanel();
        tabXSLT.setLayout(new BorderLayout());
        tabXSLT.add(editXSLT, "Center");
        tabXML = new JPanel();
        tabXML.setLayout(new BorderLayout());
        tabXML.add(editXML, "Center");
        tabPHP = new JPanel();
        tabPHP.setLayout(new BorderLayout());
        tabPHP.add(editPHP, "Center");
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("XSLT", tabXSLT);
        tabbedPane.addTab("XML", tabXML);
        tabbedPane.addTab("PHP", tabPHP);
        tabbedPane.addTab("Design view", DesignView);
    }

    public void savePage(boolean isForDesign) {
        System.out.println("tring to save page");
        try {
            File file = new File(mainWin.ProjectLocation + "\\" + toOpen.getXsltFile());
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            String contents = editXSLT.MyText.getText();
            if (isForDesign) contents = contents.replace("<xsl:with-param select=\"" + WebXConfig.SYS_IMAGE_PLACEHOLDER + "\" " + "name=\"" + WebXConfig.SYS_IMAGE_PLACEHOLDER + "\"/>", "<img src=\"images/generic_template.jpg\" height='80' width='150' alt='generic_template'/>"); else contents = contents.replace("<img src=\"images/generic_template.jpg\" height='80' width='150'/>", "<xsl:with-param select=\"" + WebXConfig.SYS_IMAGE_PLACEHOLDER + "\" " + "name=\"" + WebXConfig.SYS_IMAGE_PLACEHOLDER + "\"/>");
            out.write(contents);
            out.close();
            file = new File(mainWin.ProjectLocation + "\\" + toOpen.getPhpFiles());
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            contents = editPHP.MyText.getText();
            out.write(contents);
            out.close();
        } catch (IOException e) {
        }
    }

    public void loadPage(Files page) {
        System.out.println("creating files and temp files");
        editXSLT.setFileSource(new File(mainWin.ProjectLocation + "\\" + page.getXsltFile()));
        editXML.setFileSource(new File(mainWin.ProjectLocation + "\\" + page.getXsltFile()));
        editPHP.setFileSource(new File(mainWin.ProjectLocation + "\\" + page.getPhpFiles()));
        DesignView.setDocumentSource(xsltDocument);
    }
}
