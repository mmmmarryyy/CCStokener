package com.ignis.eclipse.plugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.wizard.WizardPage;
import org.osgi.framework.Bundle;
import com.ignis.eclipse.plugin.internal.ui.elements.DefaultContentProvider;
import com.ignis.eclipse.plugin.internal.ui.parts.FormBrowser;
import com.ignis.eclipse.plugin.internal.ui.parts.WizardCheckboxTablePart;

public class ignisProjectCreationWizardPage extends WizardPage {

    private IStatus fCurrStatus;

    private boolean fPageVisible;

    private IConfigurationElement fConfigurationElement;

    private String fNameLabel;

    private String fProjectName;

    private String fOutDir;

    private Text fTextControl;

    private LinkedHashMap<String, ignisProjectJARBundle> fBundles;

    private ArrayList<String> fBundlesNames = new ArrayList<String>();

    private WizardCheckboxTablePart fTablePart;

    private FormBrowser fDescriptionBrowser;

    private ImageDescriptor defaultIcon;

    class ListContentProvider extends DefaultContentProvider implements IStructuredContentProvider {

        public Object[] getElements(Object parent) {
            Object[] list = fBundlesNames.toArray();
            Arrays.sort(list);
            return list;
        }
    }

    private void initializeDefaultIconImageDescriptor(IMemento bundleFile) throws CoreException {
        String defaultIcon = bundleFile.getString("defaultbundleicon");
        if (defaultIcon != null) {
            Bundle bundle = ignisProjectsPlugin.getDefault().getBundle();
            setDefaultIconImageDescriptor(ignisProjectsPlugin.createImageDescriptor(bundle, new Path(defaultIcon)));
        }
    }

    public void setDefaultIconImageDescriptor(ImageDescriptor imageDescriptor) {
        defaultIcon = imageDescriptor;
    }

    class ListLabelProvider extends LabelProvider implements ITableLabelProvider {

        public String getColumnText(Object obj, int index) {
            ignisProjectJARBundle section = fBundles.get((String) obj);
            if (index == 0) return section.getName();
            return section.getShortDescription();
        }

        public Image getColumnImage(Object obj, int index) {
            ignisProjectJARBundle bundle = fBundles.get((String) obj);
            ImageDescriptor icon = bundle.getIcon();
            if (icon.equals(ImageDescriptor.getMissingImageDescriptor())) {
                icon = defaultIcon;
            }
            return icon.createImage();
        }
    }

    class TablePart extends WizardCheckboxTablePart {

        public TablePart(String mainLabel) {
            super(mainLabel);
        }

        protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
            return super.createStructuredViewer(parent, style | SWT.FULL_SELECTION, toolkit);
        }

        protected void updateCounter(int amount) {
            super.updateCounter(amount);
            if (getContainer() != null) getContainer().updateButtons();
        }
    }

    private XMLMemento loadBundlesFile() throws CoreException {
        XMLMemento bundles = null;
        FileReader file = null;
        IPath bundleFile = null;
        try {
            bundleFile = new Path("$nl$/bundles.xml");
            URL url = FileLocator.find(ignisProjectsPlugin.getDefault().getBundle(), bundleFile, null);
            Reader br = new BufferedReader(new InputStreamReader(url.openStream()));
            bundles = XMLMemento.createReadRoot(br);
        } catch (FileNotFoundException e) {
            ignisProjectUtils.throwCoreException("Bundle file " + bundleFile.toOSString() + " not found.", e);
        } catch (NullPointerException e) {
            ignisProjectUtils.throwCoreException("Bundle file " + bundleFile.toOSString() + " not found.", e);
        } catch (Exception e) {
            ignisProjectUtils.throwCoreException("Fail to handle Bundle file", e);
        } finally {
            try {
                if (file != null) file.close();
            } catch (IOException e) {
                ignisProjectsPlugin.log("Fail to close Bundle file");
            }
        }
        return bundles;
    }

    public ignisProjectCreationWizardPage(int pageNumber, IConfigurationElement elem, String type) throws CoreException {
        super("page" + pageNumber);
        fCurrStatus = createStatus(IStatus.OK, "");
        fConfigurationElement = elem;
        setTitle(getAttribute(elem, "pagetitle"));
        setDescription(getAttribute(elem, "pagedescription"));
        fNameLabel = getAttribute(elem, "label");
        fProjectName = getAttribute(elem, "name");
        fOutDir = getAttribute(elem, "out");
        IMemento bundleFile = loadBundlesFile();
        getBundles(bundleFile, type);
        initializeDefaultIconImageDescriptor(bundleFile);
        initializeTablePart();
    }

    private void initializeTablePart() {
        IConfigurationElement elem = getConfigurationElement();
        fTablePart = new TablePart(getAttribute(elem, "tabletitle"));
        fDescriptionBrowser = new FormBrowser(SWT.BORDER | SWT.V_SCROLL);
        fDescriptionBrowser.setText("first version...");
    }

    private String getAttribute(IConfigurationElement elem, String tag) {
        String res = elem.getAttribute(tag);
        if (res == null) {
            return '!' + tag + '!';
        }
        return res;
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gd = new GridLayout();
        gd.numColumns = 2;
        composite.setLayout(gd);
        Label label = new Label(composite, SWT.LEFT);
        label.setText(fNameLabel);
        label.setLayoutData(new GridData());
        fTextControl = new Text(composite, SWT.SINGLE | SWT.BORDER);
        fTextControl.setText(fProjectName);
        fTextControl.setSelection(fProjectName.length());
        fTextControl.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                if (!fTextControl.isDisposed()) {
                    validateText(fTextControl.getText());
                }
            }
        });
        fTextControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fTextControl.setFocus();
        setControl(composite);
        Dialog.applyDialogFont(composite);
        if (fCurrStatus.matches(IStatus.OK)) {
            validateText(fProjectName);
        }
        Composite bundler = new Composite(composite, SWT.NONE);
        GridLayout gdbudnler = new GridLayout();
        gdbudnler.numColumns = 2;
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gridData.horizontalSpan = 2;
        bundler.setLayoutData(gridData);
        gdbudnler.marginWidth = 0;
        bundler.setLayout(gdbudnler);
        fTablePart.createControl(bundler);
        CheckboxTableViewer viewer = fTablePart.getTableViewer();
        viewer.addCheckStateListener(new ICheckStateListener() {

            public void checkStateChanged(CheckStateChangedEvent event) {
                String bundleName = (String) event.getElement();
                ignisProjectJARBundle bundle = fBundles.get(bundleName);
                if (event.getChecked()) {
                    if (!bundle.isRunTimeRequired()) {
                        bundle.setRunTimeRequired(true);
                        try {
                            recursiveSign(bundle);
                        } catch (CoreException e) {
                        }
                    }
                    CheckboxTableViewer viewer = fTablePart.getTableViewer();
                    TableItem[] list = viewer.getTable().getItems();
                    for (TableItem listItem : list) {
                        if (!listItem.getChecked()) {
                            String name = (String) listItem.getData();
                            ignisProjectJARBundle listBundle = fBundles.get(name);
                            if (listBundle.isRunTimeRequired()) {
                                listItem.setChecked(true);
                            }
                        }
                    }
                } else {
                    bundle.setRunTimeRequired(false);
                }
            }

            public void recursiveSign(ignisProjectJARBundle bundle) throws CoreException {
                for (String depend : bundle.getDependencies()) {
                    ignisProjectJARBundle dependBundle = fBundles.get(depend);
                    if (dependBundle == null) {
                        throw new CoreException(new Status(IStatus.ERROR, ignisProjectsPlugin.getPluginId(), IStatus.ERROR, "Bundle " + depend + " Does not exist", null));
                    } else if (!dependBundle.isRunTimeRequired()) {
                        dependBundle.setRunTimeRequired(true);
                        recursiveSign(dependBundle);
                    }
                }
            }
        });
        viewer.setContentProvider(new ListContentProvider());
        viewer.setLabelProvider(new ListLabelProvider());
        initializeTable(viewer.getTable());
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                handleSelectionChanged((String) sel.getFirstElement());
            }
        });
        fDescriptionBrowser.createControl(bundler);
        Control c = fDescriptionBrowser.getControl();
        GridData gd2 = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
        gd2.heightHint = 100;
        c.setLayoutData(gd2);
        viewer.setInput(ignisProjectsPlugin.getDefault());
        fTablePart.selectAll(false);
        setControl(bundler);
        Dialog.applyDialogFont(bundler);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(bundler, "IHelpContextIds.TEMPLATE_SELECTION");
    }

    private void handleSelectionChanged(String section) {
        ignisProjectJARBundle bundle = fBundles.get(section);
        String text = bundle != null ? bundle.getDescription() : "";
        if (text.length() > 0) text = "<p>" + text + "</p>";
        fDescriptionBrowser.setText(text);
    }

    private void initializeTable(Table table) {
        IConfigurationElement elem = getConfigurationElement();
        table.setHeaderVisible(true);
        TableColumn column = new TableColumn(table, SWT.NULL);
        column.setText(getAttribute(elem, "tablenamecolumn"));
        column.setResizable(true);
        column = new TableColumn(table, SWT.NULL);
        column.setText(getAttribute(elem, "tabledescriptioncolumn"));
        column.setResizable(true);
        TableLayout layout = new TableLayout();
        layout.addColumnData(new ColumnWeightData(50));
        layout.addColumnData(new ColumnWeightData(50));
        table.setLayout(layout);
    }

    private void validateText(String text) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IStatus status = workspace.validateName(text, IResource.PROJECT);
        if (status.isOK()) {
            if (workspace.getRoot().getProject(text).exists()) {
                status = createStatus(IStatus.ERROR, ignisProjectMessages.ignisProjectCreationWizardPage_error_alreadyexists);
            }
        }
        updateStatus(status);
        fProjectName = text;
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        fPageVisible = visible;
        if (visible && fCurrStatus.matches(IStatus.ERROR)) {
            fCurrStatus = createStatus(IStatus.ERROR, "");
        }
        updateStatus(fCurrStatus);
    }

    private void updateStatus(IStatus status) {
        fCurrStatus = status;
        setPageComplete(!status.matches(IStatus.ERROR));
        if (fPageVisible) {
            applyToStatusLine(this, status);
        }
    }

    private static void applyToStatusLine(DialogPage page, IStatus status) {
        String errorMessage = null;
        String warningMessage = null;
        String statusMessage = status.getMessage();
        if (statusMessage.length() > 0) {
            if (status.matches(IStatus.ERROR)) {
                errorMessage = statusMessage;
            } else if (!status.isOK()) {
                warningMessage = statusMessage;
            }
        }
        page.setErrorMessage(errorMessage);
        page.setMessage(warningMessage);
    }

    private static IStatus createStatus(int severity, String message) {
        return new Status(severity, ignisProjectsPlugin.getPluginId(), severity, message, null);
    }

    /**
	 * @return Returns the name entered by the user
	 */
    public String getName() {
        return fProjectName;
    }

    public String getOutDir() {
        return fOutDir;
    }

    /**
	 * Returns the configuration element of this page.
	 * @return Returns a IConfigurationElement
	 */
    public IConfigurationElement getConfigurationElement() {
        return fConfigurationElement;
    }

    private void getBundles(IMemento bundlesFile, String type) throws CoreException {
        fBundles = new LinkedHashMap<String, ignisProjectJARBundle>();
        IMemento[] bundles = bundlesFile.getChildren("bundle");
        if (bundles != null) {
            for (IMemento mementoBundle : bundles) {
                ignisProjectJARBundle bundle = new ignisProjectJARBundle(getConfigurationElement(), mementoBundle);
                fBundles.put(bundle.getName(), bundle);
                if (mementoBundle.getString("type") == null || mementoBundle.getString("type").equals(type)) {
                    fBundlesNames.add(bundle.getName());
                }
            }
            for (ignisProjectJARBundle bundle : fBundles.values()) {
                for (String depend : bundle.getDependencies()) {
                    if (!fBundles.containsKey(depend)) {
                        throw new CoreException(new Status(IStatus.ERROR, ignisProjectsPlugin.getPluginId(), IStatus.ERROR, "Error: Bundle " + depend + " does not exist, while it is defined as a dependency of bundle " + bundle.getName(), null));
                    }
                }
            }
        }
    }

    public ArrayList<IClasspathEntry> getJarsForClasspath() {
        ArrayList<IClasspathEntry> classpaths = new ArrayList<IClasspathEntry>();
        LinkedHashMap<String, ignisProjectJARBundle> selectionAndDependecies = new LinkedHashMap<String, ignisProjectJARBundle>();
        for (Object tableBundleName : fTablePart.getSelection()) {
            ignisProjectJARBundle bundle = fBundles.get(tableBundleName);
            selectionAndDependecies.put((String) tableBundleName, bundle);
            for (String dependency : bundle.getDependencies()) {
                selectionAndDependecies.put(dependency, fBundles.get(dependency));
            }
        }
        for (ignisProjectJARBundle bundle : selectionAndDependecies.values()) {
            ArrayList<IClasspathEntry> jars = bundle.getClasspathEntries();
            classpaths.addAll(jars);
        }
        return classpaths;
    }

    public ArrayList<IMemento> getBundlesImports() {
        ArrayList<IMemento> importsList = new ArrayList<IMemento>();
        LinkedHashMap<String, ignisProjectJARBundle> selectionAndDependecies = new LinkedHashMap<String, ignisProjectJARBundle>();
        for (Object tableBundleName : fTablePart.getSelection()) {
            ignisProjectJARBundle bundle = fBundles.get(tableBundleName);
            selectionAndDependecies.put((String) tableBundleName, bundle);
            for (String dependency : bundle.getDependencies()) {
                selectionAndDependecies.put(dependency, fBundles.get(dependency));
            }
        }
        for (ignisProjectJARBundle bundle : selectionAndDependecies.values()) {
            ArrayList<IMemento> imports = bundle.getBundleImports();
            if (!imports.isEmpty()) {
                importsList.addAll(imports);
            }
        }
        return importsList;
    }
}
