package net.sf.janos.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.sbbi.upnp.devices.DeviceIcon;
import net.sf.janos.ApplicationContext;
import net.sf.janos.ZoneNotAvailableException;
import net.sf.janos.control.SonosController;
import net.sf.janos.control.ZoneListSelectionListener;
import net.sf.janos.control.ZonePlayer;
import net.sf.janos.model.ZoneGroup;
import net.sf.janos.model.ZoneGroupStateModel;
import net.sf.janos.model.ZoneGroupStateModelListener;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;

public class ZoneControlList implements ExpandListener, ZoneGroupStateModelListener {

    private final List<ZoneListSelectionListener> selectionListeners = new ArrayList<ZoneListSelectionListener>();

    private ExpandBar bar;

    private ZonePlayer currentZone;

    public ZoneControlList(ExpandBar bar, SonosController controller) {
        this.bar = bar;
        bar.addExpandListener(this);
        controller.getZoneGroupStateModel().addListener(this);
        addSearchingItem();
    }

    public void itemCollapsed(ExpandEvent arg0) {
    }

    public void itemExpanded(ExpandEvent arg0) {
        for (ExpandItem i : bar.getItems()) {
            if (i.getExpanded()) {
                i.setExpanded(false);
            }
        }
        ExpandItem item = (ExpandItem) arg0.item;
        ZoneControl zc = (ZoneControl) item.getControl();
        if (zc != null) {
            refreshItem(item);
            setCurrentZone(zc.getZonePlayer());
        }
    }

    private void setIcon(final ExpandItem newItem, final ZonePlayer dev) {
        Image oldImage = newItem.getImage();
        if (oldImage != null) {
            oldImage.dispose();
        }
        List<?> icons = dev.getMediaRendererDevice().getUPNPDevice().getDeviceIcons();
        if (icons == null || icons.isEmpty()) {
            newItem.setImage((Image) null);
            return;
        }
        final DeviceIcon deviceIcon = (DeviceIcon) icons.get(0);
        SonosController.getInstance().getWorkerExecutor().execute(new Runnable() {

            public void run() {
                InputStream is = null;
                URL url = deviceIcon.getUrl();
                try {
                    is = url.openStream();
                    final ImageData[] images = new ImageLoader().load(is);
                    if (images != null && images.length > 0) {
                        newItem.getDisplay().asyncExec(new Runnable() {

                            public void run() {
                                if (!newItem.isDisposed()) {
                                    newItem.setImage(new Image(newItem.getDisplay(), images[0]));
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ex1) {
                        }
                    }
                }
            }
        });
    }

    public ZonePlayer getSelectedZone() {
        return currentZone;
    }

    private void setCurrentZone(ZonePlayer zp) {
        currentZone = zp;
        fireZoneSelectionChanged(zp);
    }

    public void addSelectionListener(ZoneListSelectionListener l) {
        this.selectionListeners.add(l);
        if (getSelectedZone() != null) {
            l.zoneSelectionChangedTo(getSelectedZone());
        }
    }

    public void removeSelectionListener(ZoneListSelectionListener l) {
        this.selectionListeners.remove(l);
    }

    protected void fireZoneSelectionChanged(ZonePlayer newSelection) {
        for (ZoneListSelectionListener l : this.selectionListeners) {
            l.zoneSelectionChangedTo(newSelection);
        }
    }

    public void zoneGroupAdded(final ZoneGroup group, final ZoneGroupStateModel source) {
        bar.getDisplay().asyncExec(new Runnable() {

            public void run() {
                try {
                    addZone(group, source);
                } catch (ZoneNotAvailableException e) {
                    LogFactory.getLog(getClass()).error("Could not add zone group", e);
                }
            }
        });
    }

    public void zoneGroupMembersChanged(final ZoneGroup group, final ZoneGroupStateModel source) {
        bar.getDisplay().asyncExec(new Runnable() {

            public void run() {
                changeZone(group, source);
            }
        });
    }

    public void zoneGroupRemoved(final ZoneGroup group, final ZoneGroupStateModel source) {
        bar.getDisplay().asyncExec(new Runnable() {

            public void run() {
                removeZone(group, source);
            }
        });
    }

    protected ExpandItem addZone(ZoneGroup group, ZoneGroupStateModel source) throws ZoneNotAvailableException {
        removeSearchingItem();
        ZonePlayer coordinator = ApplicationContext.getInstance().getController().getZonePlayerModel().getById(group.getCoordinator());
        if (coordinator == null) {
            throw new ZoneNotAvailableException("Can't locate zone with id " + group.getCoordinator());
        }
        String coordinatorName = coordinator.getDevicePropertiesService().getZoneAttributes().getName();
        ZoneControl zoneControl = new ZoneControl(bar, group);
        zoneControl.addControlListener(new ControlListener() {

            public void controlMoved(ControlEvent arg0) {
            }

            public void controlResized(ControlEvent arg0) {
                ZoneControl zoneControl = (ZoneControl) arg0.widget;
                ExpandItem i = findItemByControl(zoneControl);
                i.setHeight(zoneControl.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).y);
                bar.layout(true);
            }
        });
        int index = 0;
        for (ExpandItem i : bar.getItems()) {
            String thisName = (String) i.getData("SORT_KEY");
            if (coordinatorName.compareToIgnoreCase(thisName) > 0) {
                index++;
            } else {
                break;
            }
        }
        boolean expand = false;
        if (bar.getItemCount() == 0) {
            setCurrentZone(coordinator);
            expand = true;
        }
        ExpandItem item = new ExpandItem(bar, 0, index);
        LinkedList<String> names = new LinkedList<String>();
        for (String zoneId : group.getMembers()) {
            ZonePlayer zp = ApplicationContext.getInstance().getController().getZonePlayerModel().getById(zoneId);
            names.add(zp.getDevicePropertiesService().getZoneAttributes().getName());
        }
        Collections.sort(names);
        String title = new String("");
        for (String name : names) {
            if (title.compareTo("") != 0) {
                title += ", ";
            }
            title += name;
        }
        item.setText(title);
        item.setData("SORT_KEY", title);
        item.setData("GROUP_ID", group.getId());
        setIcon(item, coordinator);
        item.setControl(zoneControl);
        item.setExpanded(expand);
        if (expand) {
            refreshItem(item);
        }
        int maxWidth = 0;
        for (ExpandItem expandItem : bar.getItems()) {
            Control control = expandItem.getControl();
            if (control != null) {
                maxWidth = Math.max(maxWidth, control.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
            }
        }
        ((GridData) bar.getLayoutData()).widthHint = Math.max(maxWidth, bar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
        bar.getParent().layout(true);
        return item;
    }

    protected void removeZone(ZoneGroup group, ZoneGroupStateModel source) {
        ExpandItem i;
        while ((i = findItemByZoneGroup(group)) != null) {
            Control c = i.getControl();
            i.setControl(null);
            c.dispose();
            i.dispose();
        }
    }

    protected void changeZone(ZoneGroup group, ZoneGroupStateModel source) {
        ExpandItem i = findItemByZoneGroup(group);
        boolean expanded = i.getExpanded();
        removeZone(group, source);
        try {
            i = addZone(group, source);
            refreshItem(i);
            i.setExpanded(expanded);
        } catch (ZoneNotAvailableException e) {
            LogFactory.getLog(getClass()).error("Could not replace group" + group.getId(), e);
        }
    }

    protected ExpandItem findItemByZoneGroup(ZoneGroup group) {
        String ID = group.getId();
        for (ExpandItem i : bar.getItems()) {
            String thisID = (String) i.getData("GROUP_ID");
            if (thisID.compareToIgnoreCase(ID) == 0) {
                return i;
            }
        }
        return null;
    }

    protected ExpandItem findItemByControl(Control c) {
        for (ExpandItem i : bar.getItems()) {
            if (c.equals(i.getControl())) {
                return i;
            }
        }
        return null;
    }

    protected static String searchingKey = new String("Searching");

    protected void addSearchingItem() {
        ExpandItem item = new ExpandItem(bar, SWT.NONE, 0);
        item.setText("Searching For Zone Players...");
        item.setData(searchingKey, "1");
        item.setControl(null);
        item.setExpanded(false);
    }

    protected void removeSearchingItem() {
        for (ExpandItem i : bar.getItems()) {
            String val = (String) i.getData(searchingKey);
            if (val != null) {
                i.dispose();
            }
        }
    }

    private void refreshItem(ExpandItem item) {
        ZoneControl zc = (ZoneControl) item.getControl();
        zc.getNowPlaying().showNowPlaying();
        zc.getQueue().showNowPlaying();
    }
}
