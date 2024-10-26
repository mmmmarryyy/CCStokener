package it.could.confluence.autoexport;

import it.could.confluence.autoexport.templates.TemplatesAware;
import it.could.confluence.localization.LocalizedComponent;
import it.could.confluence.localization.LocalizedException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.commons.io.FileUtils;
import com.opensymphony.webwork.views.velocity.VelocityManager;
import com.atlassian.confluence.util.velocity.ConfluenceVelocityResourceCache;
import com.atlassian.plugin.PluginAccessor;

/**
 * <p>The {@link TemplatesManager} provides a single access point for all
 * Velocity templates operations tied to the AutoExport plugin.</p>
 */
public class TemplatesManager extends LocalizedComponent implements TemplatesAware {

    /** <p>The encoding used to load, save, and parse templates.</p> */
    public static final String ENCODING = "UTF-8";

    /** <p>The {@link ConfigurationManager} used to locate templates.</p> */
    private final PluginBuilder pluginBuilder;

    private final PluginAccessor pluginAccessor;

    private final ConfigurationManager configurationManager;

    /** <p>Create a new {TemplatesManager} instance.</p> */
    public TemplatesManager(ConfigurationManager configurationManager, PluginBuilder pluginBuilder, PluginAccessor pluginAccessor) {
        this.pluginBuilder = pluginBuilder;
        this.pluginAccessor = pluginAccessor;
        this.configurationManager = configurationManager;
        this.log.info("Instance created");
    }

    /**
     * <p>Read bytes from an {@link InputStream} encoding HTML characters.</p>
     */
    private String read(InputStream bytes) throws IOException {
        Reader input = null;
        try {
            input = new InputStreamReader(bytes, ENCODING);
            final StringBuffer buffer = new StringBuffer();
            int character = -1;
            while ((character = input.read()) >= 0) {
                switch(character) {
                    case '<':
                        buffer.append("&lt;");
                        break;
                    case '>':
                        buffer.append("&gt;");
                        break;
                    case '&':
                        buffer.append("&amp;");
                        break;
                    case '"':
                        buffer.append("&quot;");
                        break;
                    default:
                        buffer.append((char) character);
                }
            }
            return buffer.toString();
        } finally {
            try {
                if (input != null) input.close();
            } finally {
                bytes.close();
            }
        }
    }

    /**
     * <p>Return the {@link File} associated with a space template.</p>
     */
    private URL findTemplate(String spaceKey) {
        ClassLoader cl = pluginAccessor.getClassLoader();
        String templateName = spaceKey == null ? "autoexport.vm" : "autoexport." + spaceKey + ".vm";
        URL template = cl.getResource(templateName);
        if (template == null) {
            File confHome = new File(configurationManager.getConfluenceHome());
            File velDir = new File(confHome, "velocity");
            File tempFile = new File(velDir, templateName);
            if (tempFile.exists()) {
                try {
                    pluginBuilder.add(templateName, FileUtils.readFileToString(tempFile, "UTF-8"));
                    tempFile.delete();
                    template = cl.getResource(templateName);
                } catch (IOException e) {
                    log.error("Unable to migrate autoexport template: " + templateName, e);
                }
            }
        }
        return template;
    }

    /**
     * <p>Parse and return the Velocity {@link Template} associated with the
     * specified space key.</p>
     */
    public Template getTemplate(String spaceKey) throws LocalizedException {
        String template = DEFAULT_TEMPLATE;
        if (this.hasCustomTemplate(spaceKey)) {
            template = createTemplateName(spaceKey);
        } else if (this.hasCustomTemplate(null)) {
            template = "autoexport.vm";
        }
        final VelocityManager manager = VelocityManager.getInstance();
        final VelocityEngine engine = manager.getVelocityEngine();
        try {
            return engine.getTemplate(template, ENCODING);
        } catch (ResourceNotFoundException exception) {
            throw new LocalizedException(this, "template.notfound", template, exception);
        } catch (ParseErrorException exception) {
            throw new LocalizedException(this, "template.cantparse", template, exception);
        } catch (Exception exception) {
            throw new LocalizedException(this, "template.initerror", template, exception);
        }
    }

    private String createTemplateName(String spaceKey) {
        String template;
        if (spaceKey == null) template = "autoexport.vm"; else template = "autoexport." + spaceKey + ".vm";
        return template;
    }

    /**
     * <p>Read the default template shipped with this plugin.</p>
     * 
     * @return a <b>non-null</b> string with the template contents (escaped). 
     */
    public String readDefaultTemplate() throws LocalizedException {
        ClassLoader loader = this.getClass().getClassLoader();
        InputStream input = loader.getResourceAsStream(DEFAULT_TEMPLATE);
        if (input == null) {
            throw new LocalizedException(this, "reading.nodefault", "Can't find default template");
        } else try {
            return (this.read(input));
        } catch (IOException exception) {
            throw new LocalizedException(this, "reading.default", "Error read" + "ing default template", exception);
        }
    }

    /**
     * <p>Read the custom template associated with the space identified by the
     * specified key.</p>
     * 
     * @return the template contents (encoded) or <b>null</b> if no custom
     *         template was found. 
     */
    public String readCustomTemplate(String spaceKey) throws LocalizedException {
        final URL url = this.findTemplate(spaceKey);
        if (url == null) {
            return spaceKey == null ? this.readDefaultTemplate() : this.readCustomTemplate(null);
        } else try {
            return read(url.openStream());
        } catch (IOException exception) {
            throw new LocalizedException(this, "reading.custom", spaceKey, exception);
        }
    }

    /**
     * <p>Check wether the space identified by the specifed key has a custom
     * template or not.</p>
     */
    public boolean hasCustomTemplate(String spaceKey) {
        return findTemplate(spaceKey) != null;
    }

    /**
     * <p>Remove the custom template associated with the space identified by the
     * specified key.</p>
     * 
     * @return <b>true</b> if the custom template was removed successfully,
     *         <b>false</b> otherwise. 
     */
    public boolean removeCustomTemplate(String spaceKey) throws IOException {
        return pluginBuilder.remove(createTemplateName(spaceKey));
    }

    /**
     * <p>Write the contents of the specified {@link String} as a custom
     * template for the space identified by the specified key.</p>
     *
     * @param spaceKey the key identifying the space.
     * @param template the contents of the template to write.
     */
    public void writeCustomTemplate(String spaceKey, String template) throws LocalizedException {
        String templateName = createTemplateName(spaceKey);
        pluginBuilder.add(templateName, template);
        this.log.info(this.localizeMessage("template.flushing", new Object[] { templateName }));
        ConfluenceVelocityResourceCache.removeFromCaches(templateName);
        this.getTemplate(spaceKey);
    }
}
