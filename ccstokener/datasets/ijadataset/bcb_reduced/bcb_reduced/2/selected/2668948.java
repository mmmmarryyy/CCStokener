package net.sourceforge.eclipsetrader.briter.preferences;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.eclipsetrader.briter.BriterPlugin;
import net.sourceforge.eclipsetrader.briter.parser.EaToolParse;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import com.eat.client.model.EatoolModel;
import com.eat.client.model.SymbolListItem;

public class SelectSymbolPage extends PreferencePage implements IWorkbenchPreferencePage {

    private Button leftButton;

    private Button rightButton;

    private TableViewer rightViewer;

    private TableViewer leftViewer;

    public SelectSymbolPage() {
    }

    public SelectSymbolPage(String title) {
        super(title);
    }

    public SelectSymbolPage(String title, ImageDescriptor image) {
        super(title, image);
    }

    private void initialize() {
        EaToolParse.getInstance().loadSelectedSymbolsFromFile();
    }

    public List<SymbolListItem> getSelectedSymbols() {
        List<SymbolListItem> symbols = new ArrayList<SymbolListItem>();
        int size = rightViewer.getTable().getItemCount();
        for (int i = 0; i < size; i++) {
            symbols.add((SymbolListItem) rightViewer.getElementAt(i));
        }
        return symbols;
    }

    @Override
    protected void performApply() {
        saveSelectedSymbols();
        super.performApply();
    }

    @Override
    public boolean performOk() {
        saveSelectedSymbols();
        return super.performOk();
    }

    private void saveSelectedSymbols() {
        List<SymbolListItem> selectedSymbols = getSelectedSymbols();
        if (selectedSymbols == null) {
            return;
        }
        PrintWriter writer = null;
        try {
            URL url = FileLocator.find(BriterPlugin.getDefault().getBundle(), new Path(EaToolParse.SELECT_SYMBOLS_FILE_PATH), null);
            URL path = FileLocator.resolve(url);
            writer = new PrintWriter(new FileWriter(new File(path.getFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (SymbolListItem symbol : selectedSymbols) {
            writer.write(symbol.toString());
            writer.write("\n");
        }
        if (writer != null) {
            writer.close();
        }
    }

    @Override
    protected Control createContents(Composite ancestor) {
        Composite parent = new Composite(ancestor, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 0;
        parent.setLayout(layout);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        parent.setLayoutData(gridData);
        leftViewer = createTableViewer(parent);
        createMiddelComposite(parent);
        rightViewer = createTableViewer(parent);
        leftViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                rightButton.setEnabled(!event.getSelection().isEmpty());
            }
        });
        rightViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                leftButton.setEnabled(!event.getSelection().isEmpty());
            }
        });
        List<SymbolListItem> symbols = (List<SymbolListItem>) EaToolParse.getInstance().getAvailableSymbols();
        initialize();
        List<SymbolListItem> selectedSymbols = EatoolModel.getInstance().getSelectedSymbols();
        if (selectedSymbols != null) {
            rightViewer.setInput(selectedSymbols);
        }
        if (symbols != null) {
            symbols.removeAll(selectedSymbols);
            leftViewer.setInput(symbols);
        }
        Dialog.applyDialogFont(parent);
        return parent;
    }

    private void refreshViewer() {
        leftViewer.refresh();
        rightViewer.refresh();
    }

    private TableViewer createTableViewer(Composite parent) {
        Composite tableComposite = new Composite(parent, SWT.NONE);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.heightHint = 500;
        tableComposite.setLayoutData(data);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 0;
        tableComposite.setLayout(layout);
        TableViewer tableViewer = new TableViewer(tableComposite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        TableColumn currentColumn = new TableColumn(table, SWT.NONE);
        currentColumn.setText("Symbol");
        currentColumn.setWidth(100);
        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText("Name");
        column.setWidth(200);
        tableViewer.setLabelProvider(new TableLabelProvider());
        tableViewer.setContentProvider(new ContentProvider());
        tableViewer.setSorter(new ViewerSorter());
        return tableViewer;
    }

    private void createMiddelComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.horizontalSpacing = 0;
        composite.setLayout(layout);
        GridData gd = new GridData();
        gd.grabExcessVerticalSpace = true;
        composite.setLayoutData(gd);
        Composite innerComposite = new Composite(composite, SWT.NONE);
        innerComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 0;
        innerComposite.setLayout(layout);
        leftButton = new Button(innerComposite, SWT.NONE);
        Image leftImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_BACK);
        leftButton.setImage(leftImage);
        rightButton = new Button(innerComposite, SWT.NONE);
        Image rightImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_FORWARD);
        rightButton.setImage(rightImage);
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.CENTER;
        leftButton.setLayoutData(gridData);
        gridData = new GridData();
        gridData.verticalAlignment = GridData.CENTER;
        rightButton.setLayoutData(gridData);
        leftButton.setEnabled(false);
        rightButton.setEnabled(false);
        rightButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                move(leftViewer, rightViewer);
            }
        });
        leftButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                move(rightViewer, leftViewer);
            }
        });
    }

    protected void move(TableViewer srcViewer, TableViewer destViewer) {
        ISelection selection = srcViewer.getSelection();
        IStructuredSelection sel = (IStructuredSelection) selection;
        Object[] array = sel.toArray();
        srcViewer.remove(array);
        destViewer.add(array);
    }

    public void init(IWorkbench workbench) {
    }

    static class TableLabelProvider extends LabelProvider implements ITableLabelProvider {

        public String getColumnText(Object element, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return ((SymbolListItem) element).getSymbol();
                case 1:
                    return ((SymbolListItem) element).getName();
            }
            return "";
        }

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
    }

    static class ContentProvider implements IStructuredContentProvider {

        public Object[] getElements(Object inputElement) {
            return ((List) inputElement).toArray();
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    /**
	 * Retuns an image based on a plugin and file path
	 * 
	 * @param plugin
	 *            Object The plugin containing the image
	 * @param name
	 *            String The path to th eimage within the plugin
	 * @return Image The image stored in the file at the specified path
	 */
    public static Image getPluginImage(Object plugin, String name) {
        try {
            URL url = getPluginImageURL(plugin, name);
            InputStream is = url.openStream();
            Image image;
            try {
                image = getImage(is);
            } finally {
                is.close();
            }
            return image;
        } catch (Throwable e) {
        }
        return null;
    }

    /**
	 * Returns an image encoded by the specified input stream
	 * 
	 * @param is
	 *            InputStream The input stream encoding the image data
	 * @return Image The image encoded by the specified input stream
	 */
    protected static Image getImage(InputStream is) {
        Display display = Display.getCurrent();
        ImageData data = new ImageData(is);
        if (data.transparentPixel > 0) {
            return new Image(display, data, data.getTransparencyMask());
        }
        return new Image(display, data);
    }

    /**
	 * Retuns an URL based on a plugin and file path
	 * 
	 * @param plugin
	 *            Object The plugin containing the file path
	 * @param name
	 *            String The file path
	 * @return URL The URL representing the file at the specified path
	 * @throws Exception
	 */
    private static URL getPluginImageURL(Object plugin, String name) throws Exception {
        try {
            Class<?> bundleClass = Class.forName("org.osgi.framework.Bundle");
            Class<?> bundleContextClass = Class.forName("org.osgi.framework.BundleContext");
            if (bundleContextClass.isAssignableFrom(plugin.getClass())) {
                Method getBundleMethod = bundleContextClass.getMethod("getBundle", new Class[0]);
                Object bundle = getBundleMethod.invoke(plugin, new Object[0]);
                Class<?> ipathClass = Class.forName("org.eclipse.core.runtime.IPath");
                Class<?> pathClass = Class.forName("org.eclipse.core.runtime.Path");
                Constructor<?> pathConstructor = pathClass.getConstructor(new Class[] { String.class });
                Object path = pathConstructor.newInstance(new Object[] { name });
                Class<?> platformClass = Class.forName("org.eclipse.core.runtime.Platform");
                Method findMethod = platformClass.getMethod("find", new Class[] { bundleClass, ipathClass });
                return (URL) findMethod.invoke(null, new Object[] { bundle, path });
            }
        } catch (Throwable e) {
        }
        {
            Class<?> pluginClass = Class.forName("org.eclipse.core.runtime.Plugin");
            if (pluginClass.isAssignableFrom(plugin.getClass())) {
                Class<?> ipathClass = Class.forName("org.eclipse.core.runtime.IPath");
                Class<?> pathClass = Class.forName("org.eclipse.core.runtime.Path");
                Constructor<?> pathConstructor = pathClass.getConstructor(new Class[] { String.class });
                Object path = pathConstructor.newInstance(new Object[] { name });
                Method findMethod = pluginClass.getMethod("find", new Class[] { ipathClass });
                return (URL) findMethod.invoke(plugin, new Object[] { path });
            }
        }
        return null;
    }
}
