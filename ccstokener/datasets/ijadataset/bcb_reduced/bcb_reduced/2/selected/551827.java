package net.sf.opentranquera.xkins.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import net.sf.opentranquera.xkins.core.loaders.XkinsXMLLoader;

/**
 * Esta clase representa el contenido de un template
 * @author Guillermo Meyer
 */
public class Content {

    private String url = null;

    private String data = null;

    private Template template = null;

    public Content() {
        super();
    }

    /**
	 * receive a string with the content or the url where the template content is
	 * @param data
	 */
    public Content(String data) {
        this();
        this.setData(data);
    }

    /**
	 * @return
	 */
    public String getUrl() {
        return url;
    }

    /**
	 * @param string
	 */
    public void setUrl(String string) {
        url = string;
    }

    /**
	 * Devuelve el contenido del template.
	 * Si est� vacio y tiene url, levanda de la misma el contenido y lo devuelve.
	 * @return
	 */
    public InputStream getInputStream() {
        if ((this.data == null || this.data.equals("")) && this.url != null) {
            URL urlToOpen = null;
            try {
                urlToOpen = this.getURLContent();
                if (urlToOpen != null) {
                    XkinsXMLLoader.addConfigFilesTimeStamp(urlToOpen.getFile());
                    return urlToOpen.openStream();
                }
            } catch (MalformedURLException mue) {
                XkinsLogger.getLogger().error("Error getting content input Stream (MalformedURLException)", mue);
            } catch (IOException io) {
                XkinsLogger.getLogger().error("Error getting content input Stream (IOException)", io);
            }
            return null;
        } else {
            return new ByteArrayInputStream(this.data.getBytes());
        }
    }

    private URL getURLContent() throws MalformedURLException {
        URL urlToOpen = null;
        if (this.url.startsWith("jndi://")) {
            String path = "/" + this.url.substring(7);
            urlToOpen = this.getClass().getResource(path);
        } else if (this.url.startsWith("http://") || this.url.startsWith("https://")) {
            urlToOpen = new URL(this.url);
        } else {
            String path = this.getTemplate().getSkin().getUrl() + this.url;
            urlToOpen = new URL("file://" + XkinsXMLLoader.getRealWebPath() + path);
        }
        if (urlToOpen == null) urlToOpen = new URL(this.url);
        return urlToOpen;
    }

    /**
	 * Carga el String con los datos del contenido del template
	 * @param string
	 */
    public void setData(String string) {
        this.data = string;
    }

    /**
	 * @return
	 */
    public Template getTemplate() {
        return template;
    }

    /**
	 * @param template
	 */
    public void setTemplate(Template template) {
        this.template = template;
    }
}
