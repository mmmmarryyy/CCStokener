package net.sf.vorg.wizards;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import net.sf.gef.core.wizards.AbstractWizard;
import net.sf.vorg.core.enums.InputTypes;
import net.sf.vorg.core.models.VORGURLRequest;
import net.sf.vorg.factories.ImageFactory;
import net.sf.vorg.vorgautopilot.models.wizards.PilotStoreWizardModel;

public class StoreConfigurationWizard extends AbstractWizard {

    /**
	 * Reference to the model container that contains a copy of the model data and methods to mode this
	 * information to and from the original model.
	 */
    private PilotStoreWizardModel model = null;

    /** Reference to the workbench used to display some elements. */
    protected IWorkbench workbench;

    /**
	 * The workbench selection when this wizard was started. Not used internally because we use the model to
	 * access the data.
	 */
    protected IStructuredSelection selection;

    /**
	 * The page where the properties components will be drawn and operated. Can not be initialized to an empty
	 * element to avoid a failure of the wizard but in that case the output will be lost. If this field is not
	 * received at the creation of this instance then we should return an exception.
	 */
    private StorePropertiesPage newStorePage;

    public StoreConfigurationWizard() {
    }

    public StoreConfigurationWizard(final PilotStoreWizardModel newWizardModel) {
        super();
        if (null == newWizardModel) throw new NullPointerException("The wizards should receive a valid WizardModel.");
        model = newWizardModel;
    }

    @Override
    public void addPages() {
        newStorePage = new StorePropertiesPage(model, workbench, selection);
        addPage(newStorePage);
    }

    @Override
    public boolean canFinish() {
        final IStatus status = newStorePage.getStatus();
        if (status.getSeverity() == IStatus.ERROR) return false; else return true;
    }

    public PilotStoreWizardModel getModel() {
        return model;
    }

    @Override
    public void init(final IWorkbench workbench, final IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
    }

    @Override
    public boolean performFinish() {
        newStorePage.saveDataToModel();
        return true;
    }
}

class StorePropertiesPage extends WizardPage implements Listener {

    private final PilotStoreWizardModel model;

    protected IWorkbench workbench;

    protected IStructuredSelection selection;

    protected IStatus pageStatus;

    private Text inputLocationPath;

    private Spinner refrestMinutes;

    private Spinner timeSeconds;

    private InputTypes inputType = InputTypes.NONE;

    public StorePropertiesPage(final PilotStoreWizardModel modelRef, final IWorkbench workbench, final IStructuredSelection selection) {
        super("Page1");
        setTitle("Configure Autopilot parameters.");
        setDescription("Set the location of the input file that contains the Routes. The Input File may be a VRTool .NAV file\n or a user configured .XML file. Other configuration parameters are the refresh attributes for the Autopilot operation.");
        setImageDescriptor(ImageFactory.getImageDescriptor("icons/configurestore_wiz.png"));
        model = modelRef;
        this.workbench = workbench;
        this.selection = selection;
        pageStatus = new Status(IStatus.OK, "not_used", 0, "", null);
    }

    @Override
    public boolean canFlipToNextPage() {
        return false;
    }

    public void createControl(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NULL);
        final GridLayout grid = new GridLayout();
        grid.numColumns = 2;
        grid.makeColumnsEqualWidth = false;
        composite.setLayout(grid);
        new Label(composite, SWT.NONE).setText("Input File Location:");
        final Composite pathSelection = new Composite(composite, SWT.NULL);
        final GridLayout gridPath = new GridLayout();
        gridPath.numColumns = 2;
        gridPath.makeColumnsEqualWidth = false;
        pathSelection.setLayout(grid);
        inputLocationPath = new Text(pathSelection, SWT.BORDER);
        if (null != model.getInputPath()) {
            inputLocationPath.setText(model.getInputPath());
        }
        final GridData grdata = new GridData(GridData.FILL_HORIZONTAL);
        grdata.widthHint = 300;
        inputLocationPath.setLayoutData(grdata);
        inputLocationPath.addListener(SWT.KeyUp, this);
        final Button clear = new Button(pathSelection, SWT.NONE);
        clear.setText("...");
        clear.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDown(final MouseEvent e) {
                inputLocationPath.setText(StorePropertiesPage.this.selectFile());
                if (null == inputLocationPath.getText()) {
                    pageStatus = new Status(IStatus.WARNING, "not_used", 0, "If the Configuration File is empty the Autopilot can not run.", null);
                } else {
                    try {
                        pageStatus = new Status(IStatus.OK, "not_used", 0, "", null);
                        @SuppressWarnings("unused") final FileInputStream testFile = new FileInputStream(inputLocationPath.getText());
                        StorePropertiesPage.this.updateFileType(inputLocationPath.getText());
                    } catch (final FileNotFoundException fnfe) {
                        pageStatus = new Status(IStatus.ERROR, "not_used", 0, "The Configuration File does not exist.", null);
                    }
                }
                StorePropertiesPage.this.applyToStatusLine(pageStatus);
                StorePropertiesPage.this.getWizard().getContainer().updateButtons();
            }
        });
        new Label(composite, SWT.NONE).setText("Refresh in minutes:");
        refrestMinutes = new Spinner(composite, SWT.NONE);
        refrestMinutes.setFont(new Font(Display.getDefault(), "Tahoma", 9, SWT.BOLD));
        refrestMinutes.setMaximum(90);
        refrestMinutes.setMinimum(1);
        refrestMinutes.setIncrement(1);
        refrestMinutes.setSelection(model.getRefreshInterval());
        refrestMinutes.setEnabled(false);
        new Label(composite, SWT.NONE).setText("Delay in seconds:");
        timeSeconds = new Spinner(composite, SWT.NONE);
        timeSeconds.setFont(new Font(Display.getDefault(), "Tahoma", 9, SWT.BOLD));
        timeSeconds.setMaximum(90);
        timeSeconds.setMinimum(0);
        timeSeconds.setIncrement(1);
        timeSeconds.setSelection(model.getTimeDeviation());
        timeSeconds.setEnabled(false);
        setControl(composite);
    }

    @Override
    public IWizardPage getNextPage() {
        return null;
    }

    public IStatus getStatus() {
        return pageStatus;
    }

    public void handleEvent(final Event event) {
        pageStatus = new Status(IStatus.OK, "not_used", 0, "", null);
        if (event.widget == inputLocationPath) if (null == inputLocationPath.getText()) {
            pageStatus = new Status(IStatus.WARNING, "not_used", 0, "If the Configuration File is empty the Autopilot can not run.", null);
        } else {
            try {
                final String fileName = inputLocationPath.getText();
                if (fileName.toLowerCase().startsWith("http")) {
                    final URL url = new URL(fileName);
                    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    VORGURLRequest.getResourceData(conn);
                } else {
                    @SuppressWarnings("unused") final FileInputStream testFile = new FileInputStream(fileName);
                }
                updateFileType(fileName);
            } catch (final FileNotFoundException fnfe) {
                pageStatus = new Status(IStatus.ERROR, "not_used", 0, "The Configuration File does not exist. Key a valid .NAV or .XML file. Or a valid URL", null);
            } catch (final MalformedURLException e) {
                pageStatus = new Status(IStatus.ERROR, "not_used", 0, "The URL is not valid or not well constructed", null);
            } catch (final IOException e) {
                pageStatus = new Status(IStatus.ERROR, "not_used", 0, "The URL destination can not be read.", null);
            }
        }
        applyToStatusLine(pageStatus);
        getWizard().getContainer().updateButtons();
    }

    public void saveDataToModel() {
        model.setInputPath(inputLocationPath.getText());
        model.setInputType(inputType);
        model.setRefreshInterval(refrestMinutes.getSelection());
        model.setTimeDeviation(timeSeconds.getSelection());
    }

    /**
	 * Applies the status to the status line of a dialog page.
	 */
    protected void applyToStatusLine(final IStatus status) {
        String message = status.getMessage();
        if (message.length() == 0) {
            message = null;
        }
        switch(status.getSeverity()) {
            case IStatus.OK:
                setErrorMessage(null);
                this.setMessage(message);
                break;
            case IStatus.WARNING:
                setErrorMessage(null);
                this.setMessage(message, WizardPage.WARNING);
                break;
            case IStatus.INFO:
                setErrorMessage(null);
                this.setMessage(message, WizardPage.INFORMATION);
                break;
            default:
                setErrorMessage(message);
                this.setMessage(null);
                break;
        }
    }

    private String selectFile() {
        final FileDialog dialog = new FileDialog(workbench.getActiveWorkbenchWindow().getShell(), SWT.OPEN);
        dialog.setFilterNames(new String[] { "VRTool Nav files", "XML Configuration files" });
        dialog.setFilterExtensions(new String[] { "*.nav", "*.xml" });
        if (null != inputLocationPath.getText()) {
            dialog.setFileName(inputLocationPath.getText());
        }
        return dialog.open();
    }

    private void updateFileType(final String testFile) {
        if (testFile.toLowerCase().startsWith("http")) {
            inputType = InputTypes.HTTP;
        } else if (testFile.toLowerCase().endsWith(".nav")) {
            inputType = InputTypes.NAV;
        } else if (testFile.toLowerCase().endsWith(".xml")) {
            inputType = InputTypes.XML;
        } else {
            inputType = InputTypes.NONE;
            pageStatus = new Status(IStatus.WARNING, "not_used", 0, "If the Configuration File is not of the right type.", null);
            applyToStatusLine(pageStatus);
            getWizard().getContainer().updateButtons();
        }
    }
}
