package com.vbSwing.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import com.vbSwing.model.Content;

/**
 * @author yavajo
 * classe permettant l'utilisation des fichiers images
 */
public class URLContent implements Content {

    /**
	 * adresse de l'objet
	 */
    private URL url;

    /**
	 * Constructeur
	 * @param u	=>	url de l'objet
	 */
    public URLContent(URL u) {
        url = u;
    }

    /**
	 * @return l'adresse de l'objet
	 */
    public URL getURL() {
        return url;
    }

    public InputStream openStream() throws IOException {
        return this.url.openStream();
    }
}
