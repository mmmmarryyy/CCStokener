package de.byteholder.geoclipse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.geoclipse.swt.MapPainter;
import de.byteholder.gpx.GeoPosition;

public class GeoclipseExtensions {

    private static GeoclipseExtensions fInstance;

    private GeoPosition startPosition = new GeoPosition(0, 0);

    private int startZoom = 0;

    private GeoclipseExtensions() {
    }

    public static GeoclipseExtensions getInstance() {
        if (fInstance == null) {
            fInstance = new GeoclipseExtensions();
        }
        return fInstance;
    }

    private void makeErrorImage(Map map) {
        try {
            URL url = this.getClass().getResource("mapviewer/resources/failed.png");
            map.setFailedImage(new Image(Display.getCurrent(), url.openStream()));
        } catch (Exception ex) {
            int tileSize = map.getTileFactory().getTileSize();
            map.setFailedImage(new Image(Display.getCurrent(), tileSize, tileSize));
            GC gc = new GC(map.getFailedImage());
            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            gc.drawString(Messages.geoclipse_extensions_loading_failed, 5, 5);
            gc.dispose();
        }
    }

    public void makeLoadingImage(Map map) {
        try {
            URL url = this.getClass().getResource("mapviewer/resources/loading.png");
            map.setLoadingImage(new Image(Display.getCurrent(), url.openStream()));
        } catch (Exception ex) {
            int tileSize = map.getTileFactory().getTileSize();
            Image img = new Image(Display.getCurrent(), tileSize, tileSize);
            GC gc = new GC(img);
            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            gc.drawString(Messages.geoclipse_extensions_loading, 5, 5);
            gc.dispose();
            map.setLoadingImage(img);
        }
    }

    public List<TileFactory> readExtensions(Map map) {
        IExtensionRegistry registry = RegistryFactory.getRegistry();
        IExtensionPoint point = registry.getExtensionPoint("de.byteholder.geoclipse.tilefactory");
        IExtension[] extensions = point.getExtensions();
        TileFactory tf = null;
        List<TileFactory> factories = new ArrayList<TileFactory>();
        for (IExtension extension : extensions) {
            IConfigurationElement[] elements = extension.getConfigurationElements();
            IConfigurationElement element = elements[elements.length - 1];
            Object o = null;
            try {
                o = element.createExecutableExtension("class");
            } catch (CoreException e) {
                e.printStackTrace();
            }
            if (o != null && o instanceof TileFactory) {
                factories.add((TileFactory) o);
                tf = (TileFactory) o;
            }
        }
        if (tf != null) {
            map.setTileFactory(tf);
            map.setCenterPosition(startPosition);
            map.setZoom(startZoom);
            makeErrorImage(map);
            makeLoadingImage(map);
        }
        registerOverlays(map);
        return factories;
    }

    private void registerOverlays(Map map) {
        IExtensionRegistry registry = RegistryFactory.getRegistry();
        IExtensionPoint point = registry.getExtensionPoint("de.byteholder.geoclipse.mapOverlay");
        IExtension[] extensions = point.getExtensions();
        for (IExtension extension : extensions) {
            IConfigurationElement[] elements = extension.getConfigurationElements();
            IConfigurationElement element = elements[elements.length - 1];
            Object o = null;
            try {
                o = element.createExecutableExtension("class");
            } catch (CoreException e) {
                e.printStackTrace();
            }
            if (o != null && o instanceof MapPainter) {
                map.addOverlayPainter((MapPainter) o);
            }
        }
    }
}
