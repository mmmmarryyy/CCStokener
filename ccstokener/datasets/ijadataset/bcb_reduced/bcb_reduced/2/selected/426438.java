package org.pachyderm.foundation;

import java.net.URL;
import java.util.Locale;
import org.apache.log4j.Logger;
import org.pachyderm.apollo.core.UTType;
import org.pachyderm.apollo.data.CXManagedObject;
import org.pachyderm.apollo.data.CXURLObject;
import org.pachyderm.apollo.data.MD;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSError;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSPathUtilities;
import com.webobjects.foundation.NSSize;

public class PXPrepareManifest extends PXBuildPhase {

    private static Logger LOG = Logger.getLogger(PXPrepareManifest.class.getName());

    @SuppressWarnings("unused")
    private static final String ResourceType = "pachyderm.resource";

    public PXPrepareManifest() {
        super();
    }

    public void executeInContext(PXBuildContext context) {
        PXPresentationDocument presentation = context.getPresentation();
        LOG.info("executeInContext: PXPresentationDocument presentation = " + presentation);
        NSArray<PXScreen> screens = (NSArray<PXScreen>) context.getBuildJob().jobParameters().objectForKey("Screens");
        if (screens == null) {
            screens = presentation.getScreenModel().screens();
        }
        LOG.info("executeInContext: NSArray screens = " + screens);
        context.setObjectForKey(screens, "PXBScreens");
        int count = screens.count();
        LOG.info("executeInContext: number of screens = " + count);
        PXBundle bundle = context.getBundle();
        LOG.info("executeInContext: PXBundle bundle = " + bundle);
        for (int i = 0; i < count; i++) {
            PXScreen screen = screens.objectAtIndex(i);
            LOG.info("executeInContext: PXScreen screen = " + screen);
            PXComponent root = screen.getRootComponent();
            LOG.info("executeInContext: root component = " + root);
            noteResourceReferencesInComponent(root, bundle, context);
        }
    }

    private void noteResourceReferencesInComponent(PXComponent component, PXBundle bundle, PXBuildContext context) {
        LOG.info("noteResourceReferencesInComponent ... [ ENTRY ]");
        if (component == null) {
            LOG.warn("noteResourceReferencesInComponent Warning: null component encountered");
            NSDictionary<String, ?> info = new NSDictionary("<" + getClass().getName() + "> Warning: null component encountered", NSError.LocalizedDescriptionKey);
            NSError message = new NSError("Insert domain here", 12345, info);
            context.appendBuildMessage(message);
            return;
        }
        LOG.info("noteResourceReferencesInComponent ... component: " + component);
        PXComponentDescription desc = component.componentDescription();
        PXBindingValues values = component.bindingValues();
        for (String bindingKey : desc.bindingKeys()) {
            PXBindingDescription binding = desc.bindingForKey(bindingKey);
            Object container = values.storedLocalizedValueForKey(bindingKey, Locale.getDefault());
            boolean isMultiValue = (binding.containerType() == PXBindingDescription.ArrayContainer);
            LOG.info("noteResourceReferencesInComponent ... for (bindingKey=" + bindingKey + ")");
            LOG.info("                                  ...         binding=" + binding);
            LOG.info("                                  ...       container=" + ((container == null) ? "EMPTY" : container.toString()));
            int max = (container == null) ? 0 : ((isMultiValue) ? ((NSArray<?>) container).count() : 1);
            for (int i = 0; i < max; i++) {
                PXAssociation association = (isMultiValue) ? (PXAssociation) ((NSArray<?>) container).objectAtIndex(i) : (PXAssociation) container;
                Object value = association.valueInContext(NSDictionary.EmptyDictionary);
                LOG.info("                                  ...     association=" + association);
                LOG.info("                                  ...           value=" + value);
                if (value instanceof PXComponent) {
                    LOG.info("noteResourceReferencesInComponent ... RECURSE");
                    noteResourceReferencesInComponent((PXComponent) value, bundle, context);
                } else if (value instanceof CXManagedObject) {
                    LOG.info("noteResourceReferencesInComponent ... value is a 'CXManagedObject'");
                    CXManagedObject mo = (CXManagedObject) value;
                    Boolean exists = (Boolean) mo.getValueForAttribute(MD.FSExists);
                    if (exists != null && exists.booleanValue()) {
                        NSDictionary<String, String> rctx = _contextForResourceWithBinding(mo, binding);
                        LOG.info("                                  ...              mo=" + mo);
                        LOG.info("                                  ...          exists=" + exists);
                        LOG.info("                                  ...            rctx=" + rctx);
                        bundle.includeObjectInContext(mo, rctx);
                    } else {
                        NSDictionary<String, ?> info = new NSDictionary<String, Object>("The object at URL " + mo.url() + " is missing.", NSError.LocalizedDescriptionKey);
                        NSError message = new NSError("Insert domain here", 12345, info);
                        context.appendBuildMessage(message);
                    }
                } else if (value != null) {
                    LOG.info("noteResourceReferencesInComponent ... value is " + value.getClass().getName() + " (value=" + value + ")");
                }
            }
        }
    }

    static String _fixType(CXManagedObject object, String type) {
        String uti = null;
        String location = null;
        LOG.info("_fixType(): object = " + object);
        try {
            if (object.getClass().equals(Class.forName("org.pachyderm.apollo.data.CXURLObject"))) {
                LOG.info("it's a CXURLObject");
                location = ((CXURLObject) object).url().toString();
                if (location == null) {
                    location = ((CXURLObject) object).identifier();
                }
            } else {
                LOG.info("not a CXURLObject");
                location = (String) object.getValueForAttribute("location");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        LOG.info("fixType(): uti = " + uti);
        LOG.info("fixType(): url string = " + location);
        if (type == null) {
            try {
                URL url = new URL(location);
                LOG.info("url = " + url);
                type = url.openConnection().getContentType();
            } catch (java.io.IOException e) {
            }
        }
        if (type != null) {
            uti = UTType.preferredIdentifierForTag(UTType.MIMETypeTagClass, type, null);
        }
        if (uti == null) {
            String file = location;
            if (file != null) {
                uti = UTType.preferredIdentifierForTag(UTType.FilenameExtensionTagClass, (NSPathUtilities.pathExtension(file)).toLowerCase(), null);
            }
            if (uti == null) {
                uti = UTType.Item;
            }
        }
        return uti;
    }

    static NSDictionary<String, String> _contextForResourceWithBinding(CXManagedObject object, PXBindingDescription binding) {
        NSMutableDictionary<String, String> rctx = new NSMutableDictionary<String, String>(5);
        String type = (String) object.getValueForAttribute(MD.ContentType);
        if (type == null) {
            type = PXPrepareManifest._fixType(object, type);
        }
        LOG.info("_contextForResourceWithBinding(): type == " + type);
        NSDictionary<?, ?> limits;
        if ((limits = binding.limitsForContentType("pachyderm.resource")) == null) if ((limits = binding.limitsForContentType("public.image")) == null) limits = NSDictionary.EmptyDictionary;
        Object maxsize = limits.objectForKey("max-size");
        if (maxsize != null && maxsize instanceof NSSize) {
            NSSize size = (NSSize) maxsize;
            rctx.setObjectForKey(Integer.toString((int) size.width()), MD.PixelWidth);
            rctx.setObjectForKey(Integer.toString((int) size.height()), MD.PixelHeight);
        }
        if (UTType.typeConformsTo(type, UTType.Video)) {
            rctx.setObjectForKey(UTType.Video, MD.Kind);
        } else if (UTType.typeConformsTo(type, UTType.AudiovisualContent)) {
            rctx.setObjectForKey(UTType.AudiovisualContent, MD.Kind);
        } else if (UTType.typeConformsTo(type, UTType.Audio)) {
            rctx.setObjectForKey(UTType.Audio, MD.Kind);
        } else if (UTType.typeConformsTo(type, UTType.Image)) {
            LOG.info("It's an image!");
            rctx.setObjectForKey(UTType.Image, MD.Kind);
        } else if ((UTType.typeConformsTo(type, UTType.JPEG)) || (UTType.typeConformsTo(type, UTType.JPEG2000))) {
            rctx.setObjectForKey(UTType.Image, MD.Kind);
        } else {
            rctx.setObjectForKey(UTType.Data, MD.Kind);
        }
        return rctx;
    }
}
