package org.eclipse.help.internal.base.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.help.AbstractContentExtensionProvider;
import org.eclipse.help.IContentExtension;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.dynamic.DocumentReader;

public class RemoteExtensionProvider extends AbstractContentExtensionProvider {

    private static final String PATH_EXTENSIONS = "/extension";

    private DocumentReader reader;

    public RemoteExtensionProvider() {
        RemoteHelp.addPreferenceChangeListener(new IPreferenceChangeListener() {

            public void preferenceChange(PreferenceChangeEvent event) {
                contentChanged();
            }
        });
    }

    public IContentExtension[] getContentExtensions(String locale) {
        if (RemoteHelp.isEnabled()) {
            List contributions = new ArrayList();
            PreferenceFileHandler handler = new PreferenceFileHandler();
            String isEnabled[] = handler.isEnabled();
            for (int ic = 0; ic < handler.getTotalRemoteInfocenters(); ic++) {
                if (isEnabled[ic].equalsIgnoreCase("true")) {
                    InputStream in = null;
                    try {
                        URL url = RemoteHelp.getURL(ic, PATH_EXTENSIONS);
                        in = url.openStream();
                        if (reader == null) {
                            reader = new DocumentReader();
                        }
                        UAElement element = reader.read(in);
                        IContentExtension[] children = (IContentExtension[]) element.getChildren(IContentExtension.class);
                        for (int contrib = 0; contrib < children.length; contrib++) {
                            contributions.add(children[contrib]);
                        }
                    } catch (IOException e) {
                        String msg = "I/O error while trying to contact the remote help server";
                        HelpBasePlugin.logError(msg, e);
                    } catch (Throwable t) {
                        String msg = "Internal error while reading topic extensions from remote server";
                        HelpBasePlugin.logError(msg, t);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
            return (IContentExtension[]) contributions.toArray(new IContentExtension[contributions.size()]);
        }
        return new IContentExtension[0];
    }
}
