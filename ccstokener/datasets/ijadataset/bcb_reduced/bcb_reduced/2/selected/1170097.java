package org.nexopenframework.ide.eclipse.jee.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.nexopenframework.ide.eclipse.commons.log.Logger;
import org.nexopenframework.ide.eclipse.commons.util.StringUtils;
import org.nexopenframework.ide.eclipse.commons.xml.XMLUtils;
import org.nexopenframework.ide.eclipse.jee.DuplicateFileException;
import org.nexopenframework.ide.eclipse.jee.JeeServiceComponentUIPlugin;
import org.nexopenframework.ide.eclipse.jee.Messages;
import org.nexopenframework.ide.eclipse.xsd.SchemaGenerator;
import org.nexopenframework.ide.eclipse.xsd.SchemaGeneratorContext;
import org.nexopenframework.ide.eclipse.xsd.SchemaGeneratorException;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>A {@link WizardPage} for generate a schema from a XSD file</p>
 * 
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public class NewSchemaGeneratorWizardPage extends NewTypeWizardPage {

    private IStructuredSelection selection;

    /**path location in workspace*/
    private StringButtonDialogField fPathDialogField;

    /**Add java.io.Serializable at all classes generated*/
    private SelectionButtonDialogFieldGroup fSerializableExtensionButtons;

    public NewSchemaGeneratorWizardPage() {
        super(false, "wizardSchemaPage");
        setTitle("Model generation from a XSD");
        setDescription(Messages.getString("wizard.xsd.description"));
        IStringButtonAdapter adapter = new IStringButtonAdapter() {

            public void changeControlPressed(DialogField field) {
                if (field == fPathDialogField) {
                    NewSchemaGeneratorWizardPage.this.xsdSelectionBrowser();
                }
            }
        };
        fPathDialogField = new StringButtonDialogField(adapter);
        fPathDialogField.setButtonLabel(Messages.getString("wizard.xsd.path.button"));
        fPathDialogField.setLabelText(Messages.getString("wizard.xsd.path.text"));
        fSerializableExtensionButtons = new SelectionButtonDialogFieldGroup(SWT.CHECK, new String[] { "" }, 1);
        fSerializableExtensionButtons.setLabelText("Add Serializable support");
        fSerializableExtensionButtons.setSelection(0, false);
    }

    /**
	 * The wizard owning this page is responsible for calling this method with
	 * the current selection. The selection is used to initialize the fields of
	 * the wizard page.
	 * 
	 * @param selection
	 *            used to initialize the fields
	 */
    public void init(IStructuredSelection selection) {
        this.selection = selection;
        IJavaElement element = getInitialJavaElement(this.selection);
        initContainerPage(element);
        initTypePage(element);
    }

    /**
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        final int nColumns = 4;
        GridLayout layout = new GridLayout();
        layout.numColumns = nColumns;
        composite.setLayout(layout);
        createPathNameControls(composite, nColumns);
        createContainerControls(composite, nColumns - 1);
        Group profiles = new Group(composite, SWT.NONE);
        profiles.setText("Optional properties");
        profiles.setLayout(layout);
        GridData fourColumnSpan = new GridData();
        fourColumnSpan.grabExcessHorizontalSpace = true;
        fourColumnSpan.horizontalAlignment = 4;
        fourColumnSpan.horizontalSpan = 4;
        profiles.setLayoutData(fourColumnSpan);
        Composite infoContainer = new Composite(profiles, 0);
        infoContainer.setLayoutData(fourColumnSpan);
        GridLayout infoLayout = new GridLayout();
        infoLayout.numColumns = 2;
        infoContainer.setLayout(infoLayout);
        final Label info = new Label(infoContainer, SWT.NONE);
        info.setImage(getShell().getDisplay().getSystemImage(SWT.ICON_INFORMATION));
        info.pack();
        Label infoText = new Label(infoContainer, SWT.NONE);
        infoText.setText("If package name is empty, it will be generated one from the schema target namespace");
        createPackageControls(profiles, nColumns);
        this.createSerializableControls(profiles, nColumns);
        infoContainer.pack();
        composite.pack();
        setControl(composite);
        Dialog.applyDialogFont(composite);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.NEW_CLASS_WIZARD_PAGE);
    }

    public void createType(IProgressMonitor monitor) throws CoreException, InterruptedException {
        final String path = this.fPathDialogField.getText();
        if (path != null && !path.equals("")) {
            IPackageFragmentRoot root = getPackageFragmentRoot();
            IPackageFragment pack = getPackageFragment();
            if (pack == null) {
                pack = root.getPackageFragment("");
            }
            if (!pack.exists()) {
                String packName = pack.getElementName();
                pack = root.createPackageFragment(packName, true, new SubProgressMonitor(monitor, 1));
            }
            try {
                SchemaGeneratorContext ctx = new SchemaGeneratorContext();
                if (pack.getElementName() != null && pack.getElementName().trim().length() > 0) {
                    ctx.setPackageName(pack.getElementName());
                }
                handlePath(path, ctx, monitor);
                populateContext(ctx);
                if (fSerializableExtensionButtons.isSelected(0)) {
                    ctx.setExtension(true);
                    addJaxb2SerializableSupport(ctx, monitor);
                }
                SchemaGenerator.generateFromSchema(ctx);
            } catch (SchemaGeneratorException e) {
                IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "Schema Generator Exception", e);
                throw new CoreException(status);
            } catch (DuplicateFileException e) {
                Logger.log(Logger.ERROR, e.getMessage());
            }
        }
    }

    /**
	 * <p></p>
	 * 
	 * @param ctx
	 * @param monitor 
	 */
    protected void addJaxb2SerializableSupport(SchemaGeneratorContext ctx, IProgressMonitor monitor) {
        String file = ctx.getSchemaFiles();
        InputStream is = null;
        OutputStream out = null;
        try {
            is = new FileInputStream(file);
            IFolder xsdFolder = getXsdFolder();
            String[] contents = StringUtils.tokenizeToStringArray(file, File.separator);
            String xsdFile = contents[contents.length - 1];
            IFile f = xsdFolder.getFile(xsdFile);
            String schemaFile = f.getLocation().toFile().getAbsolutePath();
            if (!f.exists()) {
                f.create(is, true, monitor);
                ctx.setSchemaFiles(schemaFile);
                is.close();
                is = f.getContents();
            }
            Document doc = XMLUtils.getDocument(is);
            Element xsd = doc.getDocumentElement();
            if (xsd.getAttribute("xmlns:jaxb") == null || xsd.getAttribute("xmlns:jaxb").length() == 0) {
                Comment comment = doc.createComment("Extension of JAXB 2.0 for addition of java.io.Serializable interface");
                xsd.appendChild(comment);
                xsd.setAttribute("xmlns:jaxb", "http://java.sun.com/xml/ns/jaxb");
                xsd.setAttribute("xmlns:xjc", "http://java.sun.com/xml/ns/jaxb/xjc");
                xsd.setAttribute("jaxb:version", "2.0");
                xsd.setAttribute("jaxb:extensionBindingPrefixes", "xjc");
                Element annotation = doc.createElement("xsd:annotation");
                annotation.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
                Element appinfo = doc.createElement("xsd:appinfo");
                Element globalBindings = doc.createElement("jaxb:globalBindings");
                Element serializable = doc.createElement("jaxb:serializable");
                serializable.setAttribute("uid", "1");
                globalBindings.appendChild(serializable);
                appinfo.appendChild(globalBindings);
                annotation.appendChild(appinfo);
                xsd.appendChild(annotation);
                out = new FileOutputStream(schemaFile);
                XMLUtils.serialize(doc, out);
            }
        } catch (IOException e) {
            Logger.log(Logger.ERROR, e.getMessage());
        } catch (ParserConfigurationException e) {
            Logger.log(Logger.ERROR, e.getMessage());
        } catch (SAXException e) {
            Logger.log(Logger.ERROR, e.getMessage());
        } catch (CoreException e) {
            Logger.log(Logger.ERROR, e.getMessage());
        } finally {
            try {
                if (out != null) {
                    out.flush();
                    out.close();
                    out = null;
                }
            } catch (IOException e) {
            }
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
            }
        }
    }

    /**
	 * @param ctx
	 */
    protected void populateContext(SchemaGeneratorContext ctx) {
        JavaProject project = (JavaProject) this.getPackageFragmentRoot().getParent();
        IProject pj = project.getProject();
        String name = pj.getName();
        String output = this.getPackageFragmentRootText();
        if (output.indexOf(name) > -1) {
            output = output.substring(output.indexOf(name) + name.length());
        }
        IFolder folder = pj.getFolder(output);
        File outputDirectory = new File(folder.getLocationURI().getPath());
        ctx.setOutputDirectory(outputDirectory);
    }

    /**
	 * @param path
	 * @param ctx
	 * @param monitor
	 * @throws CoreException
	 * @throws DuplicateFileException
	 */
    protected void handlePath(String path, SchemaGeneratorContext ctx, IProgressMonitor monitor) throws CoreException, DuplicateFileException {
        if (path.startsWith("http")) {
            handleHttp(path, monitor, ctx);
        } else if (isFile(path)) {
            ctx.setSchemaFiles(path);
        } else {
            File f = new File(path);
            if (f.isDirectory()) {
                ctx.setSchemaDirectory(f);
            }
        }
    }

    /**
	 * @param path
	 * @param monitor
	 * @param ctx
	 * @throws CoreException
	 * @throws DuplicateFileException
	 */
    protected void handleHttp(String path, IProgressMonitor monitor, SchemaGeneratorContext ctx) throws CoreException, DuplicateFileException {
        InputStream is = null;
        try {
            URL url = new URL(path);
            is = url.openStream();
            IFolder folder = getXsdFolder();
            String _path = url.getPath();
            String[] contents = StringUtils.tokenizeToStringArray(_path, "/");
            String file = contents[contents.length - 1];
            if (file.indexOf(".") > -1) {
                IFile f = folder.getFile(file);
                if (!f.exists()) {
                    f.create(is, false, monitor);
                    String schemaFile = f.getLocation().toFile().getAbsolutePath();
                    ctx.setSchemaFiles(schemaFile);
                    return;
                }
                throw new DuplicateFileException("File " + file + " already exists");
            }
            IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "I/O Exception", new FileNotFoundException("No file associated to " + url));
            throw new CoreException(status);
        } catch (MalformedURLException e) {
            IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "Malformed URL Exception", e);
            throw new CoreException(status);
        } catch (IOException e) {
            IStatus status = new Status(IStatus.ERROR, JeeServiceComponentUIPlugin.PLUGIN_ID, IStatus.OK, "I/O Exception", e);
            throw new CoreException(status);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
	 * @return
	 * @throws CoreException
	 */
    protected final IFolder getXsdFolder() throws CoreException {
        JavaProject project = (JavaProject) this.getPackageFragmentRoot().getParent();
        IProject pj = project.getProject();
        IFolder folder = pj.getFolder("src/main/xsd");
        if (!folder.exists()) {
            folder.create(true, true, null);
        }
        return folder;
    }

    protected boolean isFile(String path) {
        File f = new File(path);
        return f.isFile();
    }

    protected String getContainerLabel() {
        return "Output directory";
    }

    /**
	 * Creates the controls for the type Spring controller class name field. Expects a
	 * <code>GridLayout</code> with at least 2 columns.
	 * 
	 * @param composite
	 *            the parent composite
	 * @param nColumns
	 *            number of columns to span
	 */
    protected void createPathNameControls(final Composite composite, int nColumns) {
        fPathDialogField.doFillIntoGrid(composite, nColumns - 1);
        DialogField.createEmptySpace(composite);
        Text text = fPathDialogField.getTextControl(null);
        LayoutUtil.setWidthHint(text, getMaxFieldWidth());
        TextFieldNavigationHandler.install(text);
    }

    /**
	 * Creates the controls for Inheritance strategies. Expects a
	 * <code>GridLayout</code> with at least 3 columns.
	 * 
	 * @param composite
	 *            the parent composite
	 * @param nColumns
	 *            number of columns to span
	 */
    protected void createSerializableControls(Composite composite, int nColumns) {
        LayoutUtil.setHorizontalSpan(fSerializableExtensionButtons.getLabelControl(composite), 1);
        Control control = fSerializableExtensionButtons.getSelectionButtonsGroup(composite);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = nColumns - 2;
        control.setLayoutData(gd);
        DialogField.createEmptySpace(composite);
    }

    /**
	 * 
	 */
    private void xsdSelectionBrowser() {
        FileDialog dialog = new FileDialog(getShell(), 4096);
        dialog.setFilterNames(new String[] { Messages.getString("wizard.xsd.extension.xsd"), Messages.getString("wizard.xsd.extension.all") });
        dialog.setFilterExtensions(new String[] { "*.xsd", "*.*" });
        String result = dialog.open();
        if (result != null) {
            fPathDialogField.setText(result);
        }
    }
}
