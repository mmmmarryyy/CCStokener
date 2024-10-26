package net.sf.escripts.views;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import net.sf.escripts.EscriptsPlugin;
import net.sf.escripts.utilities.WidgetUtilities;
import net.sf.escripts.utilities.configuration.ConfigurationElementConstants;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

/**
* The class {@link ElementExplorerLabelProvider} is the {@link ILabelProvider} for displaying
* Eclipse commands, actions, and wizards in the {@link ElementExplorerView}.
*
* @author raner
* @version $Revision: 39 $
**/
public class ElementExplorerLabelProvider implements ILabelProvider, ConfigurationElementConstants {

    private static final String COMMAND_ICON = "command.gif";

    private static final String ACTION_ICON = "action.gif";

    private static final String WIZARD_ICON = "wizard.gif";

    private static final String NL = "$nl$/";

    private Image commandIcon;

    private Image actionIcon;

    private Image wizardIcon;

    /**
    * Creates a new {@link ElementExplorerLabelProvider}.
    *
    * @author raner
    **/
    public ElementExplorerLabelProvider() {
        super();
    }

    /**
    * Returns the image for the label of the given element. The image is owned by the
    * {@link EscriptsPlugin}'s {@link ImageRegistry}. Image disposal will be handled automatically
    * by the registry.
    *
    * @param element the element for which to provide the label image
    * @return the image used to label the element, or <code>null</code> if there is no image for the
    * given object
    * @see ILabelProvider#getImage(Object)
    *
    * @author raner
    **/
    public Image getImage(Object element) {
        if (ElementExplorerType.COMMAND.equals(element)) {
            return commandIcon == null ? commandIcon = getImage(COMMAND_ICON) : commandIcon;
        }
        if (ElementExplorerType.ACTION.equals(element)) {
            return actionIcon == null ? actionIcon = getImage(ACTION_ICON) : actionIcon;
        }
        if (ElementExplorerType.WIZARD.equals(element)) {
            return wizardIcon == null ? wizardIcon = getImage(WIZARD_ICON) : wizardIcon;
        }
        if (element instanceof IConfigurationElement) {
            return getImage((IConfigurationElement) element);
        }
        return null;
    }

    /**
    * Returns the text for the label of the given element.
    *
    * @param element the element for which to provide the label text
    * @return the text string used to label the element, or <code>null</code> if there is no text
    * label for the given object
    * @see ILabelProvider#getText(Object)
    *
    * @author raner
    **/
    public String getText(Object element) {
        if (ElementExplorerType.COMMAND.equals(element)) {
            return EscriptsPlugin.getResourceString("escripts.commands");
        }
        if (ElementExplorerType.ACTION.equals(element)) {
            return EscriptsPlugin.getResourceString("escripts.actions");
        }
        if (ElementExplorerType.WIZARD.equals(element)) {
            return EscriptsPlugin.getResourceString("escripts.wizards");
        }
        if (element instanceof IConfigurationElement) {
            IConfigurationElement configurationElement = (IConfigurationElement) element;
            String elementName = configurationElement.getName();
            StringBuffer text = new StringBuffer(configurationElement.getAttribute(ID)).append(' ');
            String label = null;
            if (elementName.equals(ACTION)) {
                label = WidgetUtilities.getWidgetLabel(configurationElement.getAttribute(LABEL));
            } else {
                label = configurationElement.getAttribute(NAME);
            }
            if (label != null) {
                text.append('(').append(label).append(')');
            }
            return text.toString();
        }
        return null;
    }

    /**
    * Adds an {@link ILabelProviderListener} - this method is currently not implemented because
    * {@link ElementExplorerLabelProvider} does not support any listeners at this point.
    *
    * @param listener the listener to be added
    * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(ILabelProviderListener)
    *
    * @author raner
    **/
    public void addListener(ILabelProviderListener listener) {
    }

    /**
    * Disposes of the {@link ElementExplorerLabelProvider}. This method is currently empty, as there
    * is no need for specific disposal actions. {@link Image}s that were created by the
    * {@link ElementExplorerLabelProvider} are managed by the {@link EscriptsPlugin}
    * {@link ImageRegistry} and will be disposed automatically.
    *
    * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
    *
    * @author raner
    **/
    public void dispose() {
    }

    /**
    * Returns whether the label would be affected by a change to the given property of the given
    * element. Generally, elements in the {@link ElementExplorerView} never change and there is no
    * possibility of such a property change.
    *
    * @param element the element
    * @param property the property
    * @return always <code>false</code>
    * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(Object, String)
    *
    * @author raner
    **/
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    /**
    * Removes am {@link ILabelProviderListener}; this method actually does nothing.
    *
    * @param listener the listener to be removed
    * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(ILabelProviderListener)
    * @see #addListener(ILabelProviderListener)
    *
    * @author raner
    **/
    public void removeListener(ILabelProviderListener listener) {
    }

    private Image getDefaultImage(String elementType) {
        if (COMMAND.equals(elementType)) {
            return getImage(ElementExplorerType.COMMAND);
        }
        if (ACTION.equals(elementType)) {
            return getImage(ElementExplorerType.ACTION);
        }
        if (WIZARD.equals(elementType)) {
            return getImage(ElementExplorerType.WIZARD);
        }
        return null;
    }

    private Image getImage(String imageName) {
        return EscriptsPlugin.getImage(imageName);
    }

    private Image getImage(IConfigurationElement configurationElement) {
        String name = configurationElement.getName();
        String icon = configurationElement.getAttribute(ICON);
        String id = configurationElement.getAttribute(ID);
        if (icon == null) {
            return getDefaultImage(name);
        }
        if (icon.startsWith(NL)) {
            icon = icon.substring(NL.length());
        }
        ImageRegistry imageRegistry = EscriptsPlugin.getDefault().getImageRegistry();
        if (imageRegistry.getDescriptor(name + ':' + id) == null) {
            Bundle bundle = Platform.getBundle(configurationElement.getNamespace());
            URL url = bundle.getEntry(icon);
            if (url != null) {
                InputStream data = null;
                try {
                    data = url.openStream();
                    Image image = new Image(Display.getDefault(), data);
                    imageRegistry.put(name + ':' + id, image);
                    return image;
                } catch (IOException exception) {
                    return getDefaultImage(name);
                } finally {
                    if (data != null) {
                        try {
                            data.close();
                        } catch (IOException couldNotClose) {
                        }
                    }
                }
            }
        }
        return getImage(name + ':' + id);
    }
}
