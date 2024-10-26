package com.tensegrity.palobrowser.editors.subset2editor.filtertabs;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.palo.api.Dimension;
import org.palo.api.subsets.Subset2;
import org.palo.api.subsets.SubsetFilter;
import com.tensegrity.palobrowser.PalobrowserPlugin;
import com.tensegrity.palobrowser.editors.subset2editor.ISubsetChangedListener;

/**
 * <code>SubsetFilterControl</code>
 * <p>
 * This class provides a <code>{@link CTabFolder}</code> control which 
 * contains a <code>{@link CTabItem}</code> for each existing subset filter.
 * </p>
 *
 * @author ArndHouben
 * @version $Id: SubsetFilterControl.java,v 1.10 2008/05/16 12:30:41 ArndHouben Exp $
 **/
public class SubsetFilterControl implements IFilterControlListener {

    private Subset2 subset;

    private Image activeTabImg;

    private final Dimension dimension;

    private final FormToolkit toolkit;

    private final CTabFolder tabFolder;

    private final HashSet<ISubsetChangedListener> listeners;

    private final Map<Integer, AbstractFilterTab> filterTabs;

    public SubsetFilterControl(Composite parent, FormToolkit toolkit, Dimension dimension) {
        this.toolkit = toolkit;
        this.dimension = dimension;
        this.listeners = new HashSet<ISubsetChangedListener>();
        this.filterTabs = new LinkedHashMap<Integer, AbstractFilterTab>();
        activeTabImg = createActiveTabImage(parent.getDisplay());
        tabFolder = createControl(parent);
    }

    public final void addSubsetChangedListener(ISubsetChangedListener listener) {
        listeners.add(listener);
    }

    public final void removeSubsetChangedListener(ISubsetChangedListener listener) {
        listeners.remove(listener);
    }

    public void activated(SubsetFilter filter) {
        subset.add(filter);
        notifyChanged();
    }

    public void changed(SubsetFilter filter) {
        notifyChanged();
    }

    public void deActivated(SubsetFilter filter) {
        subset.remove(filter);
        notifyChanged();
    }

    /**
	 * Updates all filter tabs to reflect general subset changes like altering
	 * the indent value...
	 */
    public final void updateTabs() {
        for (AbstractFilterTab filterTab : filterTabs.values()) {
            filterTab.update();
        }
    }

    public final void setInput(Subset2 subset) {
        this.subset = subset;
        SubsetFilter[] filters = subset.getFilters();
        for (AbstractFilterTab filterTab : filterTabs.values()) {
            filterTab.setInput(subset);
        }
        if (filters.length == 0) tabFolder.setSelection(0); else {
            AbstractFilterTab filterTab = filterTabs.get(filters[filters.length - 1].getType());
            tabFolder.setSelection(filterTab.getTabItem());
        }
    }

    public final Dimension getDimension() {
        return dimension;
    }

    public final FormToolkit getToolkit() {
        return toolkit;
    }

    public final Image getActiveTabImage() {
        return activeTabImg;
    }

    public final void dispose() {
        if (activeTabImg != null) activeTabImg.dispose();
        activeTabImg = null;
    }

    private final void notifyChanged() {
        if (subset == null) return;
        for (ISubsetChangedListener listener : listeners) {
            listener.changed(subset);
        }
    }

    private final CTabFolder createControl(Composite parent) {
        final CTabFolder tabFolder = new CTabFolder(parent, SWT.TOP | SWT.FLAT);
        toolkit.adapt(tabFolder);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        tabFolder.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        tabFolder.setSelectionForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        filterTabs.put(SubsetFilter.TYPE_ALIAS, new AliasFilterTab(tabFolder, this));
        filterTabs.put(SubsetFilter.TYPE_HIERARCHICAL, new HierarchicalFilterTab(tabFolder, this));
        filterTabs.put(SubsetFilter.TYPE_TEXT, new TextFilterTab(tabFolder, this));
        filterTabs.put(SubsetFilter.TYPE_PICKLIST, new PicklistTab(tabFolder, this));
        filterTabs.put(SubsetFilter.TYPE_ATTRIBUTE, new AttributeFilterTab(tabFolder, this));
        filterTabs.put(SubsetFilter.TYPE_DATA, new DataFilterTab(tabFolder, this));
        filterTabs.put(SubsetFilter.TYPE_SORTING, new SortingFilterTab(tabFolder, this));
        for (AbstractFilterTab tab : filterTabs.values()) tab.addFilterListener(this);
        SortingFilterTab sfTab = (SortingFilterTab) filterTabs.get(SubsetFilter.TYPE_SORTING);
        filterTabs.get(SubsetFilter.TYPE_ALIAS).addFilterListener(sfTab);
        filterTabs.get(SubsetFilter.TYPE_DATA).addFilterListener(sfTab);
        return tabFolder;
    }

    private final Image createActiveTabImage(Display dpl) {
        Image img = null;
        URL url = PalobrowserPlugin.getDefault().getBundle().getResource("icons/check_simple.gif");
        if (url != null) {
            try {
                ImageData imgData = new ImageData(url.openStream());
                RGB newRgb = toolkit.getColors().getColor(IFormColors.TITLE).getRGB();
                if (!imgData.palette.isDirect) {
                    RGB[] rgbs = imgData.palette.colors;
                    for (int i = 0; i < rgbs.length; i++) {
                        RGB rgb = rgbs[i];
                        if (rgb.blue < 10 && rgb.red < 10 && rgb.green < 10) {
                            rgbs[i] = newRgb;
                        }
                    }
                }
                img = new Image(dpl, imgData);
            } catch (IOException e) {
            }
        }
        if (img == null) img = PalobrowserPlugin.getDefault().getImageRegistry().get("icons/check_simple.gif");
        return img;
    }
}
