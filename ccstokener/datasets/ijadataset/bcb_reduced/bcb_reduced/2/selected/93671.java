package de.psisystems.dmachinery.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.psisystems.dmachinery.core.exeptions.PrintException;

public class Factory {

    private static final Log log = LogFactory.getLog(Factory.class);

    boolean init = false;

    protected void registerClasses() throws PrintException {
        if (!init) {
            try {
                Enumeration<URL> somethingToRegister = this.getClass().getClassLoader().getResources("META-INF/" + getClass().getSimpleName() + ".properties");
                while (somethingToRegister.hasMoreElements()) {
                    URL url = (URL) somethingToRegister.nextElement();
                    InputStream in = url.openStream();
                    BufferedReader buff = new BufferedReader(new InputStreamReader(in));
                    String line = buff.readLine();
                    while (line != null) {
                        log.debug(line);
                        try {
                            Class cls = Class.forName(line);
                            cls.newInstance();
                            log.debug("class " + line + " registered " + url);
                        } catch (ClassNotFoundException e) {
                            log.error("class " + line + " not found " + url, e);
                        } catch (InstantiationException e) {
                            log.error("class " + line + " not found " + url, e);
                        } catch (IllegalAccessException e) {
                            log.error("class " + line + " not found " + url, e);
                        }
                        line = buff.readLine();
                    }
                    buff.close();
                    in.close();
                }
            } catch (IOException e) {
                throw new PrintException(e.getMessage(), e);
            }
            init = true;
        }
    }
}
