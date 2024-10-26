package org.gbif.namefinder.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class GuiceModule extends AbstractModule {

    private InputStream classpathStream(String path) {
        InputStream in = null;
        URL url = getClass().getClassLoader().getResource(path);
        if (url != null) {
            try {
                in = url.openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return in;
    }

    @Override
    protected void configure() {
        Names.bindProperties(binder(), getProperties());
    }

    private Properties getProperties() {
        Properties props = new Properties();
        try {
            props.load(classpathStream("application.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }
}
