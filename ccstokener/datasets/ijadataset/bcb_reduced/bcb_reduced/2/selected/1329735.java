package org.eclipse.help.internal.base.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.help.AbstractIndexProvider;
import org.eclipse.help.IIndexContribution;
import org.eclipse.help.internal.base.HelpBasePlugin;

public class RemoteIndexProvider extends AbstractIndexProvider {

    private static final String PATH_INDEX = "/index";

    private static final String PARAM_LANG = "lang";

    public RemoteIndexProvider() {
        RemoteHelp.addPreferenceChangeListener(new IPreferenceChangeListener() {

            public void preferenceChange(PreferenceChangeEvent event) {
                contentChanged();
            }
        });
    }

    public IIndexContribution[] getIndexContributions(String locale) {
        if (RemoteHelp.isEnabled()) {
            List contributions = new ArrayList();
            PreferenceFileHandler handler = new PreferenceFileHandler();
            String isEnabled[] = handler.isEnabled();
            for (int ic = 0; ic < handler.getTotalRemoteInfocenters(); ic++) {
                if (isEnabled[ic].equalsIgnoreCase("true")) {
                    InputStream in = null;
                    try {
                        URL url = RemoteHelp.getURL(ic, PATH_INDEX + '?' + PARAM_LANG + '=' + locale);
                        in = url.openStream();
                        RemoteIndexParser parser = new RemoteIndexParser();
                        IIndexContribution[] result = parser.parse(in);
                        for (int contrib = 0; contrib < result.length; contrib++) {
                            contributions.add(result[contrib]);
                        }
                    } catch (IOException e) {
                        String msg = "I/O error while trying to contact the remote help server";
                        HelpBasePlugin.logError(msg, e);
                    } catch (Throwable t) {
                        String msg = "Internal error while reading index contents from remote server";
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
            return (IIndexContribution[]) contributions.toArray(new IIndexContribution[contributions.size()]);
        }
        return new IIndexContribution[0];
    }
}
