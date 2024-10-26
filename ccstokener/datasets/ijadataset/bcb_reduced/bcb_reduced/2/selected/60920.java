package net.sourceforge.eclipastie.provider.lodgeit.internal.preferences;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import net.sourceforge.eclipastie.core.preferences.fieldeditors.UrlFieldEditor;
import net.sourceforge.eclipastie.provider.lodgeit.LodgeItProvider;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Defines the main preference page for the LodgeIt provider
 *
 * @author Kay Huber, local.ch AG
 * @version $Id: LodgeItPreferencePage.java 36 2009-05-13 10:01:43Z quorg $
 * @since 11.05.2009
 */
public class LodgeItPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public LodgeItPreferencePage() {
        super(GRID);
        init(null);
    }

    @Override
    protected void createFieldEditors() {
        addField(new UrlFieldEditor(ILodgeItPreferenceConstants.LODGEIT_API_URL_PREFERENCE, Messages.LodgeItPreferencePage_APIUrl, getFieldEditorParent(), new UrlFieldEditor.Validator() {

            public void validate(String uri) throws UrlFieldEditor.ValidatorException {
                try {
                    URL url = URI.create(uri + "/xmlrpc/").toURL();
                    URLConnection connection = url.openConnection();
                    if (!(connection instanceof HttpURLConnection)) {
                        throw new UrlFieldEditor.ValidatorException(Messages.LodgeItPreferencePage_InvalidURLNotHTTP);
                    }
                    HttpURLConnection httpConnection = (HttpURLConnection) connection;
                    int responseCode = httpConnection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new UrlFieldEditor.ValidatorException(String.format(Messages.LodgeItPreferencePage_InvalidServerResponseCode, responseCode));
                    }
                } catch (IllegalArgumentException e) {
                    throw new UrlFieldEditor.ValidatorException(Messages.LodgeItPreferencePage_InvalidURL);
                } catch (MalformedURLException e) {
                    throw new UrlFieldEditor.ValidatorException(Messages.LodgeItPreferencePage_InvalidURL);
                } catch (IOException e) {
                    throw new UrlFieldEditor.ValidatorException(Messages.LodgeItPreferencePage_UnableToConnect);
                }
            }
        }));
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return LodgeItProvider.getDefault().getPreferenceStore();
    }

    public void init(IWorkbench workbench) {
        setPreferenceStore(doGetPreferenceStore());
        setDescription(Messages.LodgeItPreferencePage_PreferencesTitle);
    }
}
