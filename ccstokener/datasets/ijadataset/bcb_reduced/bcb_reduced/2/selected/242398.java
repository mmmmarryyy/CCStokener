package org.pentaho.PentahoAdmin.panels;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jdom.Document;
import org.pentaho.PentahoAdmin.Exception.PentahoAdminXMLException;
import org.pentaho.PentahoAdmin.config.ConfigLoader;
import org.pentaho.PentahoAdmin.util.FileUtils;
import org.pentaho.PentahoAdmin.util.JdomReader;
import org.pentaho.PentahoAdmin.util.JdomWriter;
import com.swtdesigner.SWTResourceManager;

public class HostConfiguration extends Composite {

    private Combo languageCombo;

    private String seperator;

    private String filename;

    private Text solutionPathText;

    private Text encodingText;

    private Combo countryCombo;

    private Text hostNametext;

    private Button saveButton;

    private Button copyOriginalButton;

    private List<Object> elements;

    private Document doc;

    /**
	 * @param parent
	 * @param style
	 */
    public HostConfiguration(Composite parent, int style) throws PentahoAdminXMLException {
        super(parent, style);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        setLayout(gridLayout);
        final Label solutionPathLabel = new Label(this, SWT.NONE);
        solutionPathLabel.setText(Messages.getString("HostConfiguration.solutionpath"));
        solutionPathText = new Text(this, SWT.BORDER);
        final GridData gd_solutionPathText = new GridData(SWT.FILL, SWT.CENTER, true, false);
        solutionPathText.setLayoutData(gd_solutionPathText);
        final Button solutionBrowseButton = new Button(this, SWT.NONE);
        solutionBrowseButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                DirectoryDialog dlg = new DirectoryDialog(getShell());
                dlg.setFilterPath(getSolutionPathText());
                dlg.setText(Messages.getString("HostConfiguration.selectlocation"));
                dlg.setMessage(Messages.getString("HostConfiguration.selectdirectory"));
                String dir = dlg.open();
                if (dir != null) {
                    setSolutionPathText(dir);
                }
            }
        });
        solutionBrowseButton.setText(Messages.getString("HostConfiguration.selectbrowse"));
        final Label hostNameLabel = new Label(this, SWT.NONE);
        hostNameLabel.setText(Messages.getString("HostConfiguration.hostname"));
        hostNametext = new Text(this, SWT.BORDER);
        hostNametext.setBackground(SWTResourceManager.getColor(255, 0, 0));
        final GridData gd_hostNametext = new GridData(SWT.FILL, SWT.CENTER, true, false);
        hostNametext.setLayoutData(gd_hostNametext);
        new Label(this, SWT.NONE);
        final Label localeLanguageLabel = new Label(this, SWT.NONE);
        localeLanguageLabel.setText(Messages.getString("HostConfiguration.localelanguage"));
        languageCombo = new Combo(this, SWT.NONE);
        final GridData gd_languageCombo = new GridData(SWT.FILL, SWT.CENTER, true, false);
        languageCombo.setLayoutData(gd_languageCombo);
        languageCombo.setItems(getLocaleLanguage());
        new Label(this, SWT.NONE);
        final Label localeCountryLabel = new Label(this, SWT.NONE);
        localeCountryLabel.setText(Messages.getString("HostConfiguration.localecountry"));
        countryCombo = new Combo(this, SWT.NONE);
        final GridData gd_countryCombo = new GridData(SWT.FILL, SWT.CENTER, true, false);
        countryCombo.setLayoutData(gd_countryCombo);
        countryCombo.setItems(getLocaleCountry());
        new Label(this, SWT.NONE);
        final Label encodingLabel = new Label(this, SWT.NONE);
        encodingLabel.setText(Messages.getString("HostConfiguration.encoding"));
        encodingText = new Text(this, SWT.BORDER);
        final GridData gd_encodingText = new GridData(SWT.FILL, SWT.CENTER, true, false);
        encodingText.setLayoutData(gd_encodingText);
        new Label(this, SWT.NONE);
        final Label authenticationMethodLabel = new Label(this, SWT.NONE);
        authenticationMethodLabel.setText(Messages.getString("HostConfiguration.authmethod"));
        final Composite composite = new Composite(this, SWT.NONE);
        composite.setLayout(new FillLayout());
        final Button simpleButton = new Button(composite, SWT.RADIO);
        simpleButton.setText(Messages.getString("HostConfiguration.authsimple"));
        final Button jndiButton = new Button(composite, SWT.RADIO);
        jndiButton.setText(Messages.getString("HostConfiguration.authjndi"));
        final Button ldapButton = new Button(composite, SWT.RADIO);
        ldapButton.setText(Messages.getString("HostConfiguration.authldap"));
        new Label(this, SWT.NONE);
        final Composite composite_1 = new Composite(this, SWT.NONE);
        composite_1.setLayout(new FillLayout());
        saveButton = new Button(composite_1, SWT.BUTTON1);
        saveButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                saveFile();
            }
        });
        saveButton.setEnabled(false);
        saveButton.setText(Messages.getString("HostConfiguration.savefile"));
        copyOriginalButton = new Button(composite_1, SWT.BUTTON1);
        copyOriginalButton.setEnabled(true);
        copyOriginalButton.setText(Messages.getString("HostConfiguration.revertfile"));
        copyOriginalButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                FileUtils.copyFile("web.xml", filename);
            }
        });
        solutionPathText.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent arg0) {
                setSaveButton(true);
            }
        });
        hostNametext.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent arg0) {
                setSaveButton(true);
                Boolean ret = checkUrl(getHostNameText());
                if (ret == true) {
                    setHostBackgroundColor(255, 255, 255);
                } else setHostBackgroundColor(255, 0, 0);
            }
        });
        countryCombo.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent arg0) {
                setSaveButton(true);
            }
        });
        encodingText.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent arg0) {
                setSaveButton(true);
            }
        });
        new Label(this, SWT.NONE);
        init();
    }

    /**
	 * @throws PentahoAdminXMLException 
	 * 
	 */
    private void init() throws PentahoAdminXMLException {
        loadFile();
        elements = JdomReader.getChildElements(doc);
        setHostNameText(JdomReader.getNextNodeWithTagValue(elements, "base-url"));
        setLocaleLanguageText(JdomReader.getNextNodeWithTagValue(elements, "locale-language"));
        setLocaleCountryText(JdomReader.getNextNodeWithTagValue(elements, "locale-country"));
        setEncodingText(JdomReader.getNextNodeWithTagValue(elements, "encoding"));
        setSolutionPathText(JdomReader.getNextNodeWithTagValue(elements, "solution-path"));
        Boolean ret = checkUrl(getHostNameText());
        if (ret == true) {
            setHostBackgroundColor(255, 255, 255);
        } else setHostBackgroundColor(255, 0, 0);
    }

    private void loadFile() throws PentahoAdminXMLException {
        seperator = System.getProperty("file.separator");
        filename = ConfigLoader.props.get(0) + seperator + "WEB-INF" + seperator + "web.xml";
        try {
            doc = JdomReader.loadXMLFile(filename);
        } catch (Exception e) {
            throw new PentahoAdminXMLException(Messages.getString("JobMeta.Exception.UnableToLoadJobFromXMLFile") + filename + "]", e);
        }
    }

    private void saveFile() {
        JdomWriter.setNextNodeWithTagValue(elements, "base-url", getHostNameText());
        JdomWriter.setNextNodeWithTagValue(elements, "locale-language", getLocaleLanguageText());
        JdomWriter.setNextNodeWithTagValue(elements, "locale-country", getLocaleCountryText());
        JdomWriter.setNextNodeWithTagValue(elements, "encoding", getEncodingText());
        JdomWriter.setNextNodeWithTagValue(elements, "solution-path", getSolutionPathText());
        JdomWriter.saveXML("c:\\web.xml", doc);
    }

    public void setEncodingText(String encoding) {
        encodingText.setText(encoding);
    }

    public void setHostNameText(String hostname) {
        hostNametext.setText(hostname);
    }

    public void setLocaleCountryText(String locale) {
        countryCombo.setText(locale);
    }

    public void setLocaleLanguageText(String language) {
        languageCombo.setText(language);
    }

    public void setSolutionPathText(String solution) {
        solutionPathText.setText(solution);
    }

    public String getEncodingText() {
        return encodingText.getText();
    }

    public String getHostNameText() {
        return hostNametext.getText();
    }

    public String getLocaleLanguageText() {
        return languageCombo.getText();
    }

    public String getLocaleCountryText() {
        return countryCombo.getText();
    }

    public String getSolutionPathText() {
        return solutionPathText.getText();
    }

    public void setSaveButton(Boolean saveme) {
        saveButton.setEnabled(saveme);
    }

    public void setHostBackgroundColor(int r, int g, int b) {
        hostNametext.setBackground(SWTResourceManager.getColor(r, g, b));
    }

    public String[] getLocaleLanguage() {
        String[] languages = Locale.getISOLanguages();
        return languages;
    }

    public String[] getLocaleCountry() {
        String[] countries = Locale.getISOCountries();
        return countries;
    }

    private boolean checkUrl(String arg) {
        HttpURLConnection.setFollowRedirects(true);
        String urlString = arg;
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                httpConnection.connect();
                int response = httpConnection.getResponseCode();
                String s1 = Integer.toString(response);
                if (s1.charAt(0) == '2') {
                    return true;
                }
                InputStream is = httpConnection.getInputStream();
                byte[] buffer = new byte[256];
                while (is.read(buffer) != -1) {
                }
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
