package com.ivis.xprocess.ui.perspectives.homepages;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.part.EditorPart;
import com.ivis.xprocess.ui.UIConstants;
import com.ivis.xprocess.ui.UIPlugin;
import com.ivis.xprocess.ui.UIType;
import com.ivis.xprocess.ui.datawrappers.IElementWrapper;
import com.ivis.xprocess.ui.editors.util.EditorUtil;
import com.ivis.xprocess.ui.listeners.IListenToDataSourceChange;
import com.ivis.xprocess.ui.managementbar.util.ActionManager;
import com.ivis.xprocess.ui.perspectives.homepages.model.ActionElement;
import com.ivis.xprocess.ui.perspectives.homepages.model.HeaderElement;
import com.ivis.xprocess.ui.perspectives.manager.IManagePerspective;
import com.ivis.xprocess.ui.perspectives.manager.PerspectiveFactory;
import com.ivis.xprocess.ui.properties.ActionMessages;
import com.ivis.xprocess.ui.properties.HelpMessages;
import com.ivis.xprocess.ui.util.ElementUtil;
import com.ivis.xprocess.ui.util.FontAndColorManager;
import com.ivis.xprocess.ui.util.IManagementBarActionProvider;
import com.ivis.xprocess.ui.util.MainToolbarButtonManager;
import com.ivis.xprocess.ui.util.ProgressMonitorManager;
import com.ivis.xprocess.ui.util.URLUtil;
import com.ivis.xprocess.ui.util.ViewUtil;
import com.ivis.xprocess.ui.util.WorkbenchUtil;
import com.ivis.xprocess.ui.util.FontAndColorManager.FontStyle;
import com.ivis.xprocess.ui.widgets.HyperlinkWithImage;

/**
 * Each perspective has a home page that outlines the basic functionality in the
 * Perspective.
 *
 * The editor contents are generated through Jelly and homepage schemas.
 *
 */
public class HomePageEditor extends EditorPart implements IListenToDataSourceChange {

    private static final Logger logger = Logger.getLogger(HomePageEditor.class);

    private String schemaDir = "com/ivis/xprocess/ui/perspectives/homepages/";

    private String schemaName;

    private String type;

    private Composite pageComposite;

    private HomePageHelper homePageHelper;

    private HeaderElement headerElement;

    private ArrayList<ActionElement> actions = new ArrayList<ActionElement>();

    private ArrayList<IManagementBarActionProvider> actionsFromExtensions = new ArrayList<IManagementBarActionProvider>();

    private ArrayList<HyperlinkWithImage> column1Links = new ArrayList<HyperlinkWithImage>();

    private ArrayList<HyperlinkWithImage> column2Links = new ArrayList<HyperlinkWithImage>();

    private ArrayList<Image> images = new ArrayList<Image>();

    private Composite headerComposite;

    private Composite actionComposite;

    private Browser browser;

    private Button doItButton;

    private String baseDoItButtonText = "Do it >>";

    private Button helpButton;

    private Font headerFont = FontAndColorManager.getInstance().getFont("arial:12:" + FontStyle.BOLD.getStyle());

    private Font actionFont = FontAndColorManager.getInstance().getFont("arial:11:" + FontStyle.BOLD.getStyle());

    private Font perspectiveLabelFont = FontAndColorManager.getInstance().getFont("arial:10:" + FontStyle.NORMAL.getStyle());

    private Color actionBackground = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

    private String resource = null;

    private GridData doItButtonLayoutData;

    private Composite buttonComposite;

    private GridData helpButtonLayoutData;

    private Action refreshAction = new Action(ActionMessages.action_refresh_homepage) {

        @Override
        public void run() {
            recreatePage();
        }
    };

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (input instanceof HomePageEditorInput) {
            HomePageEditorInput homePageEditorInput = (HomePageEditorInput) input;
            setPartName(PerspectiveFactory.getHomePageTitle(homePageEditorInput.getName()));
            setTitleImage(PerspectiveFactory.getHomePageImage(homePageEditorInput.getName()));
            this.schemaName = homePageEditorInput.getSchemaName();
            this.type = homePageEditorInput.getType();
        }
        setSite(site);
        setInput(input);
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite parent) {
        FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
        fillLayout.marginHeight = 5;
        fillLayout.marginWidth = 5;
        parent.setLayout(new GridLayout());
        parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
        pageComposite = new Composite(parent, SWT.NONE);
        pageComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        GridLayout gridLayout = new GridLayout();
        gridLayout.marginLeft = 0;
        gridLayout.marginRight = 0;
        gridLayout.marginTop = 0;
        gridLayout.marginBottom = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 0;
        pageComposite.setLayout(gridLayout);
        pageComposite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
        createPage();
    }

    private void createPage() {
        initializeJelly();
        homePageHelper.initialize();
        createHeaderComposite(pageComposite);
        buttonComposite = new Composite(pageComposite, SWT.NONE);
        buttonComposite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
        buttonComposite.setLayout(new GridLayout(2, false));
        GridData layoutData = new GridData();
        layoutData.horizontalAlignment = SWT.END;
        buttonComposite.setLayoutData(layoutData);
        helpButton = new Button(buttonComposite, SWT.PUSH);
        helpButton.setImage(UIType.help.image);
        helpButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                resource = null;
                if (helpButton.getData() instanceof ActionElement) {
                    ActionElement actionElement = (ActionElement) helpButton.getData();
                    if ((actionElement != null) && (actionElement.getHelpUrl() != null)) {
                        resource = "/com.ivis.xprocess.doc/html/" + actionElement.getHelpUrl();
                    }
                }
                if (helpButton.getData() instanceof IManagePerspective) {
                    IManagePerspective managePerspective = (IManagePerspective) helpButton.getData();
                    if ((managePerspective != null) && (managePerspective.getHelpUrl() != null)) {
                        resource = "/com.ivis.xprocess.doc/" + managePerspective.getHelpUrl();
                    }
                }
                if ((doItButton.getData() != null) && doItButton.getData() instanceof HyperlinkWithImage) {
                    HyperlinkWithImage link = (HyperlinkWithImage) doItButton.getData();
                    if (link.getData() instanceof IManagementBarActionProvider) {
                        IManagementBarActionProvider managementBarActionProvider = (IManagementBarActionProvider) link.getData();
                        resource = managementBarActionProvider.getHelpURL();
                    }
                }
                if ((resource != null) && (resource.length() != 0)) {
                    ProgressMonitorManager progressMonitorManager = new ProgressMonitorManager();
                    ProgressMonitorDialog progressMonitorDialog = progressMonitorManager.create();
                    progressMonitorDialog.setCancelable(true);
                    progressMonitorDialog.open();
                    progressMonitorDialog.getProgressMonitor().beginTask("Opening Help System", IProgressMonitor.UNKNOWN);
                    progressMonitorDialog.getProgressMonitor().setTaskName("Opening Help System");
                    IRunnableWithProgress run = new IRunnableWithProgress() {

                        public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                            Display display = ViewUtil.getDisplay();
                            display.asyncExec(new Runnable() {

                                public void run() {
                                    UIPlugin.openHelpContents(resource);
                                }
                            });
                        }
                    };
                    try {
                        progressMonitorDialog.run(true, false, run);
                    } catch (InvocationTargetException invocationTargetException) {
                        UIPlugin.log("Helper Button Error", IStatus.ERROR, invocationTargetException);
                    } catch (InterruptedException interruptedException) {
                        UIPlugin.log("Helper Button Error", IStatus.ERROR, interruptedException);
                    } finally {
                        progressMonitorDialog.getProgressMonitor().done();
                        progressMonitorDialog.close();
                    }
                }
            }
        });
        helpButtonLayoutData = new GridData();
        helpButtonLayoutData.horizontalAlignment = SWT.END;
        helpButton.setLayoutData(helpButtonLayoutData);
        helpButton.setEnabled(false);
        doItButton = new Button(buttonComposite, SWT.PUSH);
        doItButton.setText(baseDoItButtonText);
        doItButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                doIt(doItButton.getData());
            }
        });
        doItButton.setEnabled(false);
        doItButtonLayoutData = new GridData(GridData.FILL_VERTICAL);
        doItButtonLayoutData.horizontalAlignment = SWT.END;
        doItButton.setLayoutData(doItButtonLayoutData);
        createBrowserComposite();
        showHeaderPage();
        hookContextMenu();
    }

    private void doIt(Object object) {
        if (object instanceof ActionElement) {
            ActionElement actionElement = (ActionElement) object;
            triggerAction(actionElement);
        }
        if (object instanceof HyperlinkWithImage) {
            HyperlinkWithImage hyperlinkWithImage = (HyperlinkWithImage) object;
            if (hyperlinkWithImage.getData() instanceof IManagementBarActionProvider) {
                IManagementBarActionProvider managementBarActionProvider = (IManagementBarActionProvider) hyperlinkWithImage.getData();
                List<Object> list = new ArrayList<Object>();
                String perspectiveId = PerspectiveFactory.getInstance().getCurrentPerspectiveId();
                if (PerspectiveFactory.getPerspectiveManager(perspectiveId).getCurrentElementWrapper() != null) {
                    list.add(PerspectiveFactory.getPerspectiveManager(perspectiveId).getCurrentElementWrapper());
                    managementBarActionProvider.doAction(list);
                }
            }
        }
    }

    private void createHeaderComposite(Composite parent) {
        headerComposite = new Composite(parent, SWT.NONE);
        GridLayout headerGridLayout = new GridLayout();
        headerGridLayout.marginTop = 0;
        headerGridLayout.verticalSpacing = 0;
        headerComposite.setLayout(headerGridLayout);
        headerComposite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
        headerComposite.setLayoutData(layoutData);
        createHeader(headerComposite);
        actionComposite = new Composite(headerComposite, SWT.NONE);
        actionComposite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        GridLayout actionCompositeGridLayout = new GridLayout(2, false);
        actionCompositeGridLayout.marginLeft = 5;
        actionCompositeGridLayout.marginRight = 5;
        actionComposite.setLayout(actionCompositeGridLayout);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        actionComposite.setLayoutData(layoutData);
        Iterator<?> actionIterator = actions.iterator();
        column1Links = new ArrayList<HyperlinkWithImage>();
        column2Links = new ArrayList<HyperlinkWithImage>();
        while (actionIterator.hasNext()) {
            ActionElement column1ActionElement = (ActionElement) actionIterator.next();
            HyperlinkWithImage link = createAction(actionComposite, column1ActionElement);
            column1Links.add(link);
            if (actionIterator.hasNext()) {
                ActionElement column2ActionElement = (ActionElement) actionIterator.next();
                link = createAction(actionComposite, column2ActionElement);
                column2Links.add(link);
            } else {
                Label blankLabel = new Label(actionComposite, SWT.NONE);
                blankLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
                layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
                blankLabel.setLayoutData(layoutData);
            }
        }
        Iterator<?> actionExtensionIterator = actionsFromExtensions.iterator();
        while (actionExtensionIterator.hasNext()) {
            IManagementBarActionProvider column1ActionElement = (IManagementBarActionProvider) actionExtensionIterator.next();
            HyperlinkWithImage link = createExtensionAction(actionComposite, column1ActionElement);
            if (link != null) {
                column1Links.add(link);
            }
            if (actionExtensionIterator.hasNext()) {
                IManagementBarActionProvider column2ActionElement = (IManagementBarActionProvider) actionExtensionIterator.next();
                link = createExtensionAction(actionComposite, column2ActionElement);
                if (link != null) {
                    column2Links.add(link);
                }
            } else {
                Label blankLabel = new Label(actionComposite, SWT.NONE);
                blankLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
                layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
                blankLabel.setLayoutData(layoutData);
            }
        }
        addNumbersToActions();
        int numberOfOtherPerspectives = 0;
        if (PerspectiveFactory.getValidPerspectives() != null) {
            numberOfOtherPerspectives = PerspectiveFactory.getValidPerspectives().length - 1;
        }
        if (numberOfOtherPerspectives > 0) {
            Label perspectiveActionsLabel = new Label(actionComposite, SWT.WRAP);
            perspectiveActionsLabel.setText(getPerspectiveActionsLabel());
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                perspectiveActionsLabel.setFont(perspectiveLabelFont);
            }
            perspectiveActionsLabel.setBackground(actionBackground);
            layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
            layoutData.horizontalSpan = 2;
            perspectiveActionsLabel.setLayoutData(layoutData);
            createPerspectiveActions(actionComposite);
        }
    }

    private String getPerspectiveActionsLabel() {
        String labelText = "";
        int numberOfOtherPerspectives = PerspectiveFactory.getValidPerspectives().length - 1;
        if (numberOfOtherPerspectives > 0) {
            if (numberOfOtherPerspectives > 1) {
                labelText = HelpMessages.homepage_switch_perspective_prefix + " " + numberOfOtherPerspectives + " " + HelpMessages.homepage_switch_perspective_multi_prefix;
            } else {
                labelText = HelpMessages.homepage_switch_perspective_prefix + " " + numberOfOtherPerspectives + " " + HelpMessages.homepage_switch_perspective_single_prefix;
            }
        }
        return labelText;
    }

    private HyperlinkWithImage createAction(Composite parent, final ActionElement actionElement) {
        Image image = UIType.logo16.image;
        if (actionElement.getImage() != null) {
            String imageLocation = createImageLocation("html/images/", actionElement.getImage());
            File imageFile = new File(imageLocation);
            if (imageFile.exists()) {
                imageLocation = URLUtil.convertURL(getHead() + imageLocation);
                URL url;
                try {
                    url = new URL(imageLocation);
                    image = ImageDescriptor.createFromURL(url).createImage();
                    images.add(image);
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
            } else {
                logger.error("Unable to find the image - " + actionElement.getImage());
            }
        }
        HyperlinkWithImage hyperlink = createBasicHyperlinkWithImage(parent, image, actionElement.getName());
        hyperlink.setData(actionElement);
        hyperlink.addHyperlinkListener(new IHyperlinkListener() {

            public void linkActivated(HyperlinkEvent e) {
                showLink(actionElement);
            }

            public void linkEntered(HyperlinkEvent e) {
            }

            public void linkExited(HyperlinkEvent e) {
            }
        });
        return hyperlink;
    }

    private void showHeaderPage() {
        String perspectiveId = PerspectiveFactory.getInstance().getCurrentPerspectiveId();
        IManagePerspective managePerspective = PerspectiveFactory.getPerspectiveManager(perspectiveId);
        if (managePerspective != null) {
            setUrl(managePerspective.getUrl());
            helpButton.setEnabled(true);
            helpButton.setData(managePerspective);
            doItButtonLayoutData.widthHint = 0;
            helpButtonLayoutData.horizontalSpan = 1;
            pageComposite.layout(true, true);
        }
    }

    private void showLink(ActionElement actionElement) {
        if ((actionElement.getUrl() == null) || (actionElement.getUrl().length() == 0)) {
            doIt(actionElement);
            return;
        }
        HomePageEditor.this.setUrl(actionElement.getUrl());
        if (!actionElement.isDisable() || hasValidCurrentElementWrapper()) {
            if ((actionElement.getActionToCall() != null) && (actionElement.getActionToCall().length() != 0)) {
                doItButtonLayoutData.widthHint = SWT.DEFAULT;
                doItButton.setEnabled(true);
                doItButton.setText(actionElement.getName() + "...");
                doItButton.setData(actionElement);
                helpButtonLayoutData.horizontalSpan = 1;
            } else {
                doItButtonLayoutData.widthHint = 0;
                helpButtonLayoutData.horizontalSpan = 1;
            }
        } else {
            doItButtonLayoutData.widthHint = 0;
            helpButtonLayoutData.horizontalSpan = 1;
        }
        if ((actionElement.getHelpUrl() != null) && (actionElement.getHelpUrl().length() != 0)) {
            helpButton.setEnabled(true);
            helpButton.setData(actionElement);
        } else {
            helpButton.setEnabled(false);
        }
        pageComposite.layout(true, true);
    }

    private HyperlinkWithImage createBasicHyperlinkWithImage(Composite parent, Image image, String actionName) {
        HyperlinkWithImage hyperlink = new HyperlinkWithImage(parent, SWT.NONE, image);
        hyperlink.setText(actionName);
        GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
        hyperlink.setLayoutData(layoutData);
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            hyperlink.setFont(actionFont);
        }
        hyperlink.setBackground(actionBackground);
        return hyperlink;
    }

    private HyperlinkWithImage createExtensionAction(Composite parent, final IManagementBarActionProvider managementBarActionProvider) {
        Class<?> clazz = ElementUtil.getClassofType(type);
        if ((clazz != null) && managementBarActionProvider.canShow(clazz)) {
            Image image = managementBarActionProvider.getUIType(clazz).image;
            if (image == null) {
                logger.error("Unable to find image for action - " + managementBarActionProvider);
            }
            final HyperlinkWithImage link = createBasicHyperlinkWithImage(parent, image, managementBarActionProvider.getActionName(clazz));
            link.setData(managementBarActionProvider);
            if (!hasValidCurrentElementWrapper()) {
                link.setEnabled(false);
                link.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
            }
            link.addHyperlinkListener(new IHyperlinkListener() {

                public void linkEntered(HyperlinkEvent e) {
                }

                public void linkExited(HyperlinkEvent e) {
                }

                public void linkActivated(HyperlinkEvent e) {
                    if ((managementBarActionProvider.getHelpURL() != null) && (managementBarActionProvider.getHelpURL().length() != 0)) {
                        helpButton.setEnabled(true);
                        helpButton.setData(managementBarActionProvider);
                    } else {
                        helpButton.setEnabled(false);
                    }
                    doIt(link);
                    doItButton.setEnabled(false);
                    doItButton.setText(baseDoItButtonText);
                    doItButton.setData(null);
                    pageComposite.layout(true, true);
                }
            });
            return link;
        }
        return null;
    }

    private void createPerspectiveActions(Composite parent) {
        for (String perspectiveId : PerspectiveFactory.getValidPerspectives()) {
            if (getEditorInput() instanceof HomePageEditorInput) {
                HomePageEditorInput homePageEditorInput = (HomePageEditorInput) getEditorInput();
                if (!perspectiveId.equals(homePageEditorInput.getName())) {
                    final IManagePerspective managePerspective = PerspectiveFactory.getPerspectiveManager(perspectiveId);
                    if (managePerspective != null) {
                        createPerspectiveAction(parent, perspectiveId, managePerspective);
                    }
                }
            }
        }
    }

    private HyperlinkWithImage createPerspectiveAction(Composite parent, final String perspectiveId, final IManagePerspective managePerspective) {
        final HyperlinkWithImage link = createBasicHyperlinkWithImage(parent, managePerspective.getImage(), managePerspective.getHomePageTitle());
        link.setData(perspectiveId);
        link.addHyperlinkListener(new IHyperlinkListener() {

            public void linkEntered(HyperlinkEvent e) {
            }

            public void linkExited(HyperlinkEvent e) {
            }

            public void linkActivated(HyperlinkEvent e) {
                if (managePerspective != null) {
                    try {
                        if (link.isFocusControl()) {
                            PlatformUI.getWorkbench().showPerspective(perspectiveId, WorkbenchUtil.getActiveWorkbenchWindow());
                            MainToolbarButtonManager.updateSelectProfileButtonAsync();
                        }
                    } catch (WorkbenchException e1) {
                        e1.printStackTrace();
                    }
                }
                if (!pageComposite.isDisposed()) {
                    pageComposite.layout(true, true);
                }
            }
        });
        return link;
    }

    private String getHead() {
        String head = "file:///";
        if (System.getProperty("os.name").toLowerCase().startsWith("linux") || System.getProperty("os.name").toLowerCase().startsWith("mac")) {
            head = "file://";
        }
        return head;
    }

    private String createImageLocation(String relativeLocation, String imageLocation) {
        String fullImageLocation = UIPlugin.getDefault().getPluginLocation();
        fullImageLocation = fullImageLocation.replace("com.ivis.xprocess.ui", "com.ivis.xprocess.doc");
        fullImageLocation += (relativeLocation + imageLocation);
        return fullImageLocation;
    }

    private void createHeader(Composite parent) {
        if (headerElement != null) {
            Image image = null;
            if (headerElement.getImage() != null) {
                String imageLocation = createImageLocation("html/", headerElement.getImage());
                File imageFile = new File(imageLocation);
                if (imageFile.exists()) {
                    imageLocation = URLUtil.convertURL(imageLocation);
                    URL url;
                    try {
                        url = new URL(getHead() + imageLocation);
                        image = ImageDescriptor.createFromURL(url).createImage();
                        images.add(image);
                    } catch (MalformedURLException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    logger.error("Unable to find the image - " + headerElement.getImage() + " at location - " + imageLocation);
                }
            }
            if (image != null) {
                Label imageLabel = new Label(parent, SWT.NONE);
                imageLabel.setImage(image);
            } else {
                IWorkbenchPage activePage = WorkbenchUtil.getActiveWorkbenchWindow().getActivePage();
                if ((activePage != null) && (activePage.getPerspective() != null)) {
                    final String perspectiveId = PerspectiveFactory.getInstance().getCurrentPerspectiveId();
                    final IManagePerspective managePerspective = PerspectiveFactory.getPerspectiveManager(perspectiveId);
                    if (managePerspective != null) {
                        HyperlinkWithImage headerLink = createBasicHyperlinkWithImage(parent, managePerspective.getImage(), headerElement.getTitle());
                        headerLink.setFont(headerFont);
                        headerLink.addHyperlinkListener(new IHyperlinkListener() {

                            public void linkActivated(HyperlinkEvent e) {
                                showHeaderPage();
                                doItButtonLayoutData.widthHint = 0;
                                helpButtonLayoutData.horizontalSpan = 1;
                                if ((managePerspective.getHelpUrl() != null) && (managePerspective.getHelpUrl().length() != 0)) {
                                    helpButton.setEnabled(true);
                                    helpButton.setData(managePerspective);
                                } else {
                                    helpButton.setEnabled(false);
                                }
                                pageComposite.layout(true, true);
                            }

                            public void linkEntered(HyperlinkEvent e) {
                            }

                            public void linkExited(HyperlinkEvent e) {
                            }
                        });
                        GridData layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
                        headerLink.setLayoutData(layoutData);
                        Label separatorLine = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
                        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
                        separatorLine.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
                        layoutData.horizontalSpan = 2;
                        layoutData.heightHint = 3;
                        separatorLine.setLayoutData(layoutData);
                    }
                } else {
                    EditorUtil.closeEditors(this);
                }
            }
        }
    }

    private void createBrowserComposite() {
        browser = new Browser(pageComposite.getParent(), SWT.BORDER);
        GridData layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        browser.setLayoutData(layoutData);
    }

    private void addNumbersToActions() {
        int counter = 1;
        for (HyperlinkWithImage hyperlinkWithImage : column1Links) {
            hyperlinkWithImage.setText(addNumberToActioName(hyperlinkWithImage.getText(), counter));
            counter++;
        }
        for (HyperlinkWithImage hyperlinkWithImage : column2Links) {
            hyperlinkWithImage.setText(addNumberToActioName(hyperlinkWithImage.getText(), counter));
            counter++;
        }
    }

    private String addNumberToActioName(String actionText, int actionNumber) {
        String numberedText = actionNumber + ". " + actionText;
        return numberedText;
    }

    private void setUrl(String url) {
        if ((browser != null) && !browser.isDisposed()) {
            if (!browser.getUrl().equals(url)) {
                browser.setUrl(url);
            }
        }
    }

    @Override
    public void setFocus() {
    }

    private void initializeJelly() {
        URL url = this.getClass().getClassLoader().getResource(schemaDir + schemaName);
        try {
            if ((url == null) || (url.openConnection() == null)) {
                logger.error("Unable to locate the schema file @ - " + (schemaDir + schemaName));
            } else {
                url.openConnection().connect();
            }
        } catch (IOException ioException) {
            logger.error("Unable to locate the schema file @ - " + url);
        }
        homePageHelper = new HomePageHelper(url, this);
    }

    /**
     * Add a action to the home page.
     *
     * @param actionElement
     */
    public void addAction(ActionElement actionElement) {
        actions.add(actionElement);
    }

    /**
     * Set the homepage header.
     *
     * @param headerElement
     */
    public void setHeader(HeaderElement headerElement) {
        this.headerElement = headerElement;
    }

    private void triggerAction(ActionElement actionElement) {
        if ((actionElement.getActionToCall() == null) || (actionElement.getActionToCall().length() == 0)) {
            return;
        }
        try {
            Class<?> clazz;
            clazz = ActionManager.getActionType(UIType.valueOf(actionElement.getType()));
            if (clazz == null) {
                logger.error("Unable to retrieve Action Manager for - " + actionElement.getType());
                return;
            }
            Object specificActionManager = clazz.newInstance();
            Method actionMethod = specificActionManager.getClass().getMethod(actionElement.getActionToCall());
            actionMethod.invoke(specificActionManager);
        } catch (InstantiationException instantiationException) {
            logger.error(instantiationException);
        } catch (IllegalAccessException illegalAccessException) {
            logger.error(illegalAccessException);
        } catch (SecurityException securityException) {
            logger.error(securityException);
        } catch (NoSuchMethodException noSuchMethodException) {
            logger.error(noSuchMethodException);
        } catch (IllegalArgumentException illegalArgumentException) {
            logger.error(illegalArgumentException);
        } catch (InvocationTargetException invocationTargetException) {
            invocationTargetException.printStackTrace();
            logger.error(invocationTargetException.getMessage());
        }
    }

    private boolean hasValidCurrentElementWrapper() {
        String perspectiveId = PerspectiveFactory.getInstance().getCurrentPerspectiveId();
        IManagePerspective managePerspective = PerspectiveFactory.getPerspectiveManager(perspectiveId);
        if (managePerspective == null) {
            return false;
        }
        if (managePerspective.getCurrentElementWrapper() == null) {
            return false;
        }
        if (managePerspective.getCurrentElementWrapper().isGhost()) {
            return false;
        }
        return true;
    }

    public void inputHasChanged(IElementWrapper newBaseElementWrapper) {
    }

    public void newDatasourceEvent() {
        recreatePage();
    }

    public void profileChangeEvent() {
    }

    @Override
    public void dispose() {
        UIPlugin.getDefault().removeDataSourceListener(this);
        if (getEditorInput() instanceof HomePageEditorInput) {
            HomePageEditorInput homePageEditorInput = (HomePageEditorInput) getEditorInput();
            EditorUtil.getInstance().removeHomePage(homePageEditorInput.getName());
        }
        for (Image image : images) {
            image.dispose();
        }
        super.dispose();
    }

    protected void fillContextMenu(IMenuManager manager) {
        manager.add(new GroupMarker("other_actions"));
        manager.appendToGroup("other_actions", refreshAction);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void recreatePage() {
        Display display = ViewUtil.getDisplay();
        display.asyncExec(new Runnable() {

            public void run() {
                for (Control childControl : pageComposite.getChildren()) {
                    childControl.dispose();
                }
                browser.dispose();
                actions = new ArrayList<ActionElement>();
                actionsFromExtensions = new ArrayList<IManagementBarActionProvider>();
                column1Links = new ArrayList<HyperlinkWithImage>();
                column2Links = new ArrayList<HyperlinkWithImage>();
                createPage();
                pageComposite.getParent().layout(true, true);
            }
        });
    }

    protected void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu", UIConstants.editor_menu_id);
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        addMenuToChildrenControls(menuMgr, pageComposite);
        getSite().registerContextMenu(menuMgr, this.getSite().getSelectionProvider());
    }

    private void addMenuToChildrenControls(MenuManager menuManager, Control parentControl) {
        createContextMenuFor(menuManager, parentControl);
        if ((parentControl instanceof Composite)) {
            Composite composite = (Composite) parentControl;
            Control[] children = composite.getChildren();
            for (int i = 0; i < children.length; i++) {
                addMenuToChildrenControls(menuManager, children[i]);
            }
        }
    }

    private void createContextMenuFor(MenuManager menuManager, Control control) {
        if (!control.isDisposed()) {
            Menu menu = menuManager.createContextMenu(control);
            control.setMenu(menu);
        }
    }
}
