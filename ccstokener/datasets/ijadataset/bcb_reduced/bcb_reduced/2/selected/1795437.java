package de.cenit.eb.sm.tools.eclipse.projectfromtemplate.wizards;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.SAXException;
import de.cenit.eb.sm.tools.eclipse.projectfromtemplate.Activator;
import de.cenit.eb.sm.tools.eclipse.projectfromtemplate.data.Template;
import de.cenit.eb.sm.tools.eclipse.projectfromtemplate.data.TemplateContainer;
import de.cenit.eb.sm.tools.eclipse.projectfromtemplate.data.Variable;
import de.cenit.eb.sm.tools.eclipse.projectfromtemplate.preferences.PreferenceConstants;

/**
 * WizardPage.
 */
public class WizardPage1 extends WizardPage implements IWizardPage {

    /** available templates */
    private List lboTemplates;

    /** composite for other controls */
    private Composite composite;

    /** template description */
    private Text txaTemplateDescription;

    /** variable description */
    private Text txaVariableDescription;

    /** available variables for selected template */
    private Table tblVars;

    /** parent composite */
    private Composite parent;

    /** editor to edit the variables */
    private TableEditor editVariables;

    /** combobox for available templates */
    private Combo cboTemplateIndex;

    /** template that is currently being shown */
    private Template templ;

    /** available templates */
    private TemplateContainer templContainer;

    /** page that follows after this wizard page */
    private IWizardPage nextPage;

    /**
    * Creates a new WizardPage1 object.
    *
    */
    protected WizardPage1() {
        super("");
    }

    /**
    * Creates top level control for this dialog.
    *
    * @param parent [in] parent composite
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
    */
    public void createControl(Composite parent) {
        this.parent = parent;
        this.composite = getComposite();
        setControl(this.composite);
    }

    /**
    * Creates the composite for all other controls.
    *
    * @return [Composite] composite with other controls
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public Composite getComposite() {
        if (null == this.composite) {
            this.composite = new Composite(this.parent, SWT.NONE);
            GridLayout gl = new GridLayout();
            gl.numColumns = 1;
            this.composite.setLayout(gl);
            getCboTemplateIndex();
            getLboTemplates();
            getTxaTemplateDescription();
            getTblVars();
            getTxaVariableDescription();
            if (0 != getCboTemplateIndex().getItemCount()) {
                getCboTemplateIndex().select(0);
                cboTemplateIndexSelectionChanged();
            }
            this.composite.pack();
        }
        return (this.composite);
    }

    /**
    * Adds all variables from template to the table.
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    protected void fillTableFromTemplate() {
        getTblVars().removeAll();
        for (Variable var : this.getSelectedTempl().getVariables()) {
            TableItem item = new TableItem(getTblVars(), SWT.NONE);
            item.setText(0, var.getKey());
            item.setText(1, var.getValue());
            item.setGrayed(var.getReadonly());
            if (var.getReadonly()) {
                item.setForeground(1, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
            }
        }
        getTblVars().redraw();
    }

    /**
    * Creates a table for all variables in the template.
    *
    * @return [Table] table with all available variables
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public Table getTblVars() {
        if (null == this.tblVars) {
            this.tblVars = new Table(getComposite(), SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
            this.tblVars.setLinesVisible(true);
            this.tblVars.setHeaderVisible(true);
            GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
            gd.heightHint = 200;
            this.tblVars.setLayoutData(gd);
            String[] titles = { "Variable", "Value" };
            for (int i = 0; i < titles.length; i++) {
                TableColumn column = new TableColumn(this.tblVars, SWT.NONE);
                column.setText(titles[i]);
            }
            this.tblVars.setLinesVisible(true);
            this.tblVars.setHeaderVisible(true);
            this.editVariables = new TableEditor(this.tblVars);
            this.editVariables.horizontalAlignment = SWT.LEFT;
            this.editVariables.grabHorizontal = true;
            this.tblVars.addListener(SWT.MouseDown, new Listener() {

                public void updateDerivedValues() {
                    for (int i = 0; i < WizardPage1.this.tblVars.getItemCount(); i++) {
                        TableItem item = WizardPage1.this.tblVars.getItem(i);
                        Variable var = WizardPage1.this.templ.getVariable(item.getText(0));
                        if (var.getReadonly()) {
                            var.updateValue();
                            item.setText(1, var.getValue());
                        }
                    }
                }

                public void handleEvent(Event event) {
                    Table myTblVars = WizardPage1.this.tblVars;
                    Rectangle clientArea = myTblVars.getClientArea();
                    Point pt = new Point(event.x, event.y);
                    int index = myTblVars.getTopIndex();
                    outerLoop: while (index < myTblVars.getItemCount()) {
                        boolean visible = false;
                        final TableItem item = myTblVars.getItem(index);
                        for (int i = 0; i < myTblVars.getColumnCount(); i++) {
                            Rectangle rect = item.getBounds(i);
                            if (rect.contains(pt)) {
                                final int column = i;
                                final Text text = new Text(myTblVars, SWT.NONE);
                                final Variable var = WizardPage1.this.templ.getVariable(item.getText(0));
                                getTxaVariableDescription().setText(var.getDesc());
                                if ((1 != column) || (var.getReadonly())) {
                                    break outerLoop;
                                }
                                Listener textListener = new Listener() {

                                    public void handleEvent(final Event e) {
                                        switch(e.type) {
                                            case SWT.FocusOut:
                                                item.setText(column, text.getText());
                                                var.setValue(text.getText());
                                                updateDerivedValues();
                                                text.dispose();
                                                break;
                                            case SWT.Traverse:
                                                switch(e.detail) {
                                                    case SWT.TRAVERSE_RETURN:
                                                        item.setText(column, text.getText());
                                                        var.setValue(text.getText());
                                                        updateDerivedValues();
                                                    case SWT.TRAVERSE_ESCAPE:
                                                        text.dispose();
                                                        e.doit = false;
                                                        break;
                                                    default:
                                                        break;
                                                }
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                };
                                text.addListener(SWT.FocusOut, textListener);
                                text.addListener(SWT.Traverse, textListener);
                                WizardPage1.this.editVariables.setEditor(text, item, i);
                                text.setText(item.getText(i));
                                text.selectAll();
                                text.setFocus();
                                return;
                            }
                            if (!visible && rect.intersects(clientArea)) {
                                visible = true;
                            }
                        }
                        if (!visible) {
                            return;
                        }
                        index++;
                    }
                }
            });
            for (int i = 0; i < this.tblVars.getColumnCount(); i++) {
                this.tblVars.getColumn(i).pack();
                this.tblVars.getColumn(i).setWidth(250);
            }
            this.tblVars.pack();
        }
        return this.tblVars;
    }

    /**
    * Creates a textfield for the template description.
    *
    * @return [Text] textfield with template description
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public Text getTxaTemplateDescription() {
        if (null == this.txaTemplateDescription) {
            this.txaTemplateDescription = new Text(getComposite(), SWT.MULTI | SWT.LEFT | SWT.H_SCROLL | SWT.V_SCROLL);
            GridData gd = new GridData();
            gd.horizontalAlignment = GridData.FILL;
            gd.grabExcessHorizontalSpace = true;
            gd.verticalAlignment = GridData.CENTER;
            gd.verticalSpan = 5;
            gd.minimumHeight = 100;
            gd.grabExcessVerticalSpace = true;
            this.txaTemplateDescription.setLayoutData(gd);
            this.txaTemplateDescription.setEditable(false);
            this.txaTemplateDescription.setText("Select a template.");
            this.txaTemplateDescription.pack();
        }
        return this.txaTemplateDescription;
    }

    /**
    * Creates a textfield for the variable description.
    *
    * @return [Text] textfield with variable description
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public Text getTxaVariableDescription() {
        if (null == this.txaVariableDescription) {
            this.txaVariableDescription = new Text(getComposite(), SWT.MULTI | SWT.LEFT | SWT.H_SCROLL | SWT.V_SCROLL);
            GridData gd = new GridData();
            gd.horizontalAlignment = GridData.FILL;
            gd.grabExcessHorizontalSpace = true;
            gd.verticalAlignment = GridData.CENTER;
            gd.verticalSpan = 5;
            gd.minimumHeight = 100;
            gd.grabExcessVerticalSpace = true;
            this.txaVariableDescription.setLayoutData(gd);
            this.txaVariableDescription.setEditable(false);
            this.txaVariableDescription.setText("Select a variable.");
            this.txaVariableDescription.pack();
        }
        return this.txaVariableDescription;
    }

    /**
    * Create combobox for available templates.
    *
    * @return [Combo] combobox with available templates
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public Combo getCboTemplateIndex() {
        if (null == this.cboTemplateIndex) {
            this.cboTemplateIndex = new Combo(getComposite(), SWT.READ_ONLY);
            GridData gd = new GridData();
            gd.horizontalAlignment = GridData.FILL;
            gd.grabExcessHorizontalSpace = true;
            gd.verticalAlignment = GridData.CENTER;
            gd.verticalSpan = 5;
            gd.minimumHeight = 30;
            gd.grabExcessVerticalSpace = true;
            this.cboTemplateIndex.setItems(getConfiguredTemplateIndizes());
            this.cboTemplateIndex.setLayoutData(gd);
            this.cboTemplateIndex.pack();
            this.cboTemplateIndex.addSelectionListener(new SelectionListener() {

                public void widgetDefaultSelected(SelectionEvent e) {
                    widgetSelected(e);
                }

                public void widgetSelected(SelectionEvent e) {
                    if (WizardPage1.this.cboTemplateIndex == e.getSource()) {
                        cboTemplateIndexSelectionChanged();
                    }
                }
            });
        }
        return this.cboTemplateIndex;
    }

    /**
    * Callback mathod if another template is selected.
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    protected void cboTemplateIndexSelectionChanged() {
        int idx = WizardPage1.this.cboTemplateIndex.getSelectionIndex();
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        String prefnamePrefix = PreferenceConstants.TEMPLATE_PREFIX + idx;
        String baseUrl = store.getString(prefnamePrefix + PreferenceConstants.BASEURL_POSTFIX);
        String descfile = store.getString(prefnamePrefix + PreferenceConstants.DESCFILE_POSTFIX);
        try {
            URL url = new URL(baseUrl + descfile);
            URLConnection con = url.openConnection();
            WizardPage1.this.setTemplContainer(new TemplateContainer(con.getInputStream(), baseUrl));
            fillListboxFromTemplateContainer();
        } catch (MalformedURLException e1) {
            getTxaTemplateDescription().setText(e1.getMessage());
            e1.printStackTrace();
        } catch (IOException e2) {
            getTxaTemplateDescription().setText(e2.getMessage());
            e2.printStackTrace();
        } catch (XPathExpressionException e3) {
            getTxaTemplateDescription().setText(e3.getMessage());
            e3.printStackTrace();
        } catch (ParserConfigurationException e4) {
            getTxaTemplateDescription().setText(e4.getMessage());
            e4.printStackTrace();
        } catch (SAXException e5) {
            getTxaTemplateDescription().setText(e5.getMessage());
            e5.printStackTrace();
        }
    }

    /**
    * Adds available templates to listbox.
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    protected void fillListboxFromTemplateContainer() {
        this.lboTemplates.removeAll();
        for (String key : getTemplContainer().getTemplates().keySet()) {
            this.lboTemplates.add(key);
        }
    }

    /**
    * Returns the listbox of templates.
    *
    * @return [List] listbox with templates
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public List getLboTemplates() {
        if (null == this.lboTemplates) {
            this.lboTemplates = new List(getComposite(), SWT.SINGLE | SWT.V_SCROLL);
            GridData gd = new GridData();
            gd.horizontalAlignment = GridData.FILL;
            gd.grabExcessHorizontalSpace = true;
            gd.verticalAlignment = GridData.CENTER;
            gd.verticalSpan = 5;
            gd.minimumHeight = 30;
            gd.grabExcessVerticalSpace = true;
            this.lboTemplates.setLayoutData(gd);
            this.lboTemplates.pack();
            this.lboTemplates.setSize(this.lboTemplates.getSize().x, 100);
            this.lboTemplates.addSelectionListener(new SelectionListener() {

                public void widgetDefaultSelected(SelectionEvent e) {
                    widgetSelected(e);
                }

                public void widgetSelected(SelectionEvent e) {
                    if (getLboTemplates() == e.getSource()) {
                        String[] sel = getLboTemplates().getSelection();
                        if (1 != sel.length) {
                            return;
                        }
                        Template myTempl = WizardPage1.this.templContainer.getTemplates().get(sel[0]);
                        setSelectedTempl(myTempl);
                        getTxaTemplateDescription().setText(myTempl.getDescription());
                        fillTableFromTemplate();
                    }
                }
            });
        }
        return this.lboTemplates;
    }

    /**
    * Checks which templates are available.
    *
    * @return [String[]] array of template names
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    protected String[] getConfiguredTemplateIndizes() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        java.util.List<String> templatedIndizes = new LinkedList<String>();
        for (int i = 0; ; i++) {
            if (store.contains(PreferenceConstants.TEMPLATE_PREFIX + i + PreferenceConstants.NAME_POSTFIX)) {
                String name = store.getString(PreferenceConstants.TEMPLATE_PREFIX + i + PreferenceConstants.NAME_POSTFIX);
                templatedIndizes.add(name);
            } else {
                break;
            }
        }
        String[] ret = new String[templatedIndizes.size()];
        templatedIndizes.toArray(ret);
        return ret;
    }

    /**
    * Executed if the dialog is disposed.
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
    */
    public void dispose() {
    }

    /**
    * Returns the top level control for this dialog.
    *
    * @return [Control] top level control
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see org.eclipse.jface.dialogs.IDialogPage#getControl()
    */
    public Control getControl() {
        return this.composite;
    }

    /**
    * Returns the error message for this dialog.
    *
    * @return [String] always returns null (no error message available)
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see org.eclipse.jface.dialogs.IDialogPage#getErrorMessage()
    */
    public String getErrorMessage() {
        return null;
    }

    /**
    * Returns the image for this dialog.
    *
    * @return [Image] always returns null (no image available)
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see org.eclipse.jface.dialogs.IDialogPage#getImage()
    */
    public Image getImage() {
        return null;
    }

    /**
    * Sets the composite to visible or invisible.
    *
    * @param visible [in] indicates if composite should be visible or invisible
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
    */
    public void setVisible(boolean visible) {
        this.composite.setVisible(visible);
    }

    /**
    * Checks if the next page can be displayed.
    *
    * @return [boolean] true if the next page can be displayed
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * @see org.eclipse.jface.wizard.IWizardPage#canFlipToNextPage()
    */
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    /**
    * Checks if a template has been selected.
    *
    * @return [boolean] true if a template has been selected
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public boolean isPageComplete() {
        return null != getSelectedTempl();
    }

    /**
    * Sets templ.
    *
    * @param templ [in] The templ to set.
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public void setSelectedTempl(Template templ) {
        setTempl(templ);
        if (null != getTempl()) {
            setPageComplete(true);
        }
    }

    /**
    * Getter for templ.
    *
    * @return [Template] Returns the templ.
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public Template getSelectedTempl() {
        return getTempl();
    }

    /**
    * Sets the values for the variables from the template.
    *
    * <p> For read-only variables, the values are derived from the variables
    * defined as source. For all other variables, the values are copied from
    * the table in the wizard page. </p>
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public void setMapFromTable() {
        for (TableItem item : getTblVars().getItems()) {
            Variable var = getTempl().getVariable(item.getText(0));
            if (var.getReadonly()) {
                var.updateValue();
            } else {
                var.setValue(item.getText(1));
            }
        }
    }

    /**
    * Returns the page that follows after this wizard page.
    *
    * @return [IWizardPage] Java build path configuration page
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public IWizardPage getNextPage() {
        return this.nextPage;
    }

    /** Sets the page that follows after this wizard page.
    *
    * @param javaPage [in] Java build path configuration page
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    public void setNextPage(IWizardPage javaPage) {
        this.nextPage = javaPage;
    }

    /**
    * Sets templ.
    *
    * @param templ [in] The templ to set.
    */
    protected void setTempl(Template templ) {
        this.templ = templ;
    }

    /**
    * Getter for templ.
    *
    * @return [Template] Returns the templ.
    */
    protected Template getTempl() {
        return this.templ;
    }

    /**
    * Sets templContainer.
    *
    * @param templContainer [in] The templContainer to set.
    */
    protected void setTemplContainer(TemplateContainer templContainer) {
        this.templContainer = templContainer;
    }

    /**
    * Getter for templContainer.
    *
    * @return [TemplateContainer] Returns the templContainer.
    */
    protected TemplateContainer getTemplContainer() {
        return this.templContainer;
    }
}
